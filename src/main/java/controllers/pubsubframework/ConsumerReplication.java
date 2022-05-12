package controllers.pubsubframework;

import com.google.protobuf.InvalidProtocolBufferException;
import controllers.faultinjector.FaultInjectorFactory;
import controllers.messagingframework.ConnectionHandler;
import controllers.replicationmodule.ReplicationUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.logging.Logger;
import model.Membership.MemberInfo;
import model.MsgInfo.Message;
import util.BlockingQueue;
import util.Constant;
import util.ReplicationAppUtils;

/**
 * Connect and retrieve data from a broker
 *
 * @author marisatania
 */
public class ConsumerReplication {
  private final String loadBalancerLocation;
  private String leaderLocation;
  private ConnectionHandler loadBalancerConnection;
  private ConnectionHandler connection;
  private final BlockingQueue<byte[]> blockingQueue;
  private final String topic;
  private final String model;
  private int startingPosition;
  private volatile boolean isRunning;
  private volatile boolean isElecting;
  private ExecutorService executor;
  private final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

  /**
   * Constructor for consumer
   *
   * @param loadBalancerLocation  String broker
   * @param topic                 String topi
   * @param startingPosition      int poll starting position
   */
  public ConsumerReplication(String loadBalancerLocation, String topic,
                             int startingPosition, String model) {
    this.blockingQueue = new BlockingQueue<>(Constant.NUM_QUEUE);
    this.loadBalancerLocation = loadBalancerLocation;
    this.leaderLocation = "";
    this.topic = topic;
    this.model = model;
    this.startingPosition = startingPosition;
    this.isRunning = true;
    this.isElecting = false;
    connectToLoadBalancer();
    getLeaderAddress();
    connectToBroker();
    if (model.equals(Constant.PULL)) {
      pollFromBroker(Constant.POLL_FREQ);
    } else {
      sendPushBasedRequestToBroker(topic, startingPosition);
    }
  }

  /**
   * Create connection to LoadBalancer
   */
  public void connectToLoadBalancer() {
    this.executor = Executors.newSingleThreadExecutor();
    this.loadBalancerConnection
        = ReplicationUtils.connectToLoadBalancer(loadBalancerLocation);
  }

  /**
   * Connect to broker
   */
  public void connectToBroker() {
    connection = PubSubUtils.connectToBroker(leaderLocation,
        new FaultInjectorFactory(0).getChaos());
    LOGGER.info("Consumer is connected to broker: " + leaderLocation);
  }

  /**
   * Introducing self to broker
   *
   * @param topic String topic
   */
  public void sendPushBasedRequestToBroker (String topic, int startingOffset) {
    Message msgInfo = Message.newBuilder()
        .setTypeValue(5)
        .setTopic(topic)
        .setStartingPosition(startingOffset)
        .build();
    connection.send(msgInfo.toByteArray());
    LOGGER.info("Sending push-based request for topic: " + msgInfo.getTopic() +
        ", starting position: " + msgInfo.getStartingPosition());
    receiveFromBroker();
  }

  /**
   * Introducing self to broker
   *
   * @param topic String topic
   */
  public void sendRequestToBroker (String topic, int startingPosition) {
    Message msgInfo = Message.newBuilder()
        .setTypeValue(2)
        .setTopic(topic)
        .setStartingPosition(startingPosition)
        .build();
    connection.send(msgInfo.toByteArray());
    LOGGER.info("Sending request for topic: " + msgInfo.getTopic() +
        ", starting position: " + msgInfo.getStartingPosition());
    receiveFromBroker();
  }

  /**
   * Pull message from a broker, behaves like a blocking queue
   * Put them in a queue
   *
   * @param timeout The maximum amount of time to wait (in ms)
   * @return data byte array
   */
  public byte[] poll(Duration timeout) {
    return blockingQueue.poll(timeout.toMillis());
  }

  /**
   * Poll from broker continuously
   *
   * @param pollFrequency poll time in milliseconds
   */
  public void pollFromBroker(int pollFrequency) {
    Timer timer = new Timer();
    timer.schedule(new fetchBroker(), 0, pollFrequency);
  }

  /**
   * Fetch from broker
   */
  private class fetchBroker extends TimerTask {
    public void run() {
      sendRequestToBroker(topic, startingPosition);
      LOGGER.info("Fetching from broker " + leaderLocation + "...");
      if (isElecting) {
        PubSubUtils.wait(30000);
        leaderLocation = getLeaderAddress();
        connectToBroker();
        isElecting = false;
      }
    }
  }

//   PUSH BASE
//   consumer connect I am a push based subscribe to this topic
//   while loop trying to receive msg
//   broker get topic request
//   get message
//   save to file
//   send to consumer

  /**
   * Receive data from Broker
   */
  public void receiveFromBroker() {
    int offsetCount = startingPosition;
    isRunning = true;
    while (isRunning) {
      byte[] msgByte;
      try {
        msgByte = connection.receive();
      } catch (IOException ioe) {
        LOGGER.warning("IOException in consumer receive Msg: " + ioe.getMessage());
        connection.close();
        isElecting = true;
        break;
      }

      if (msgByte != null) {
        Message msgFromBroker = null;
        try {
          msgFromBroker = Message.parseFrom(msgByte);
        } catch (InvalidProtocolBufferException e) {
          LOGGER.warning("Error in getting msg from broker: " + e.getMessage());
        }
        if (msgFromBroker != null && msgFromBroker.getTypeValue() == 1) {
          if (msgFromBroker.getTopic().equals(Constant.CLOSE)) {
             startingPosition = offsetCount;
            isRunning = false;
            continue;
          }
          offsetCount += msgFromBroker.getData().size();
          byte[] data = msgFromBroker.getData().toByteArray();
          LOGGER.info("Received from broker message topic: "
              + msgFromBroker.getTopic() + ". Data: " + msgFromBroker.getData().toStringUtf8());
          blockingQueue.put(data);

          if (model.equals(Constant.PUSH)) {
            flushToFile(data, LOGGER);
          }
        }
      }
    }
  }

  public static void flushToFile(byte[] data, Logger logger) {
    if (data != null) {
      Path filePathSave = Path.of(ReplicationAppUtils.getOffsetFile());
      if (!Files.exists(filePathSave)) {
        try {
          Files.write(filePathSave, data);
        } catch (IOException e) {
          logger.warning("Exception during consumer application write: " + e.getMessage());
        }
        logger.info("Creating consumer application file path: " + filePathSave);
      } else {
        try {
          Files.write(filePathSave, data, StandardOpenOption.APPEND);
        } catch (IOException e) {
          logger.warning("Consumer app file write exception: " + e.getMessage());
        }
      }
    }
  }

  /**
   * Get leader pubsub location
   */
  public String getLeaderAddress() {
    ReplicationUtils.sendAddressRequest(loadBalancerConnection);

    FutureTask<MemberInfo> future = new FutureTask<>(() ->
        MemberInfo.parseFrom(loadBalancerConnection.receive()));
    executor.execute(future);

    String leaderHost = "";
    int leaderPort = 0;
    try {
      leaderHost = future.get().getHost();
      leaderPort = future.get().getPort();
    } catch (ExecutionException | InterruptedException e) {
      e.printStackTrace();
    }

    String brokerLocation
        = PubSubUtils.getBrokerLocation(leaderHost, leaderPort);
    if (!brokerLocation.equals(leaderLocation)) {
      leaderLocation = brokerLocation;
    } else {
      leaderLocation = "";
    }
    LOGGER.info("Leader location: " + leaderLocation);
    return leaderLocation;
  }

  /**
   * Close connection to broker
   */
  public void close() {
    connection.close();
  }

}