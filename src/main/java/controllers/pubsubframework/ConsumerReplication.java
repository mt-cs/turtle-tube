package controllers.pubsubframework;

import com.google.protobuf.InvalidProtocolBufferException;
import controllers.faultinjector.FaultInjectorFactory;
import controllers.messagingframework.ConnectionHandler;
import controllers.replicationmodule.ReplicationUtils;
import java.io.IOException;
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

/**
 * Connect and retrieve data from a broker
 *
 * @author marisatania
 */
public class ConsumerReplication {
  private final int id;
  private final String loadBalancerLocation;
  private String leaderLocation;
  private String followerLocation;
  private ConnectionHandler loadBalancerConnection;
  private ConnectionHandler connection;
  private final BlockingQueue<byte[]> blockingQueue;
  private final String topic;
  private final String model;
  private final String read;
  private int startingPosition;
  private volatile boolean isRunning;
  private volatile boolean isUpdatingMembership;
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
                             int startingPosition, String model,
                             String read, int id) {
    this.blockingQueue = new BlockingQueue<>(Constant.NUM_QUEUE);
    this.loadBalancerLocation = loadBalancerLocation;
    this.leaderLocation = "";
    this.followerLocation = "";
    this.topic = topic;
    this.model = model;
    this.read = read;
    this.id = id;
    this.startingPosition = startingPosition;
    this.isRunning = true;
    this.isUpdatingMembership = false;
    connectToLoadBalancer();

    if (read.equals(Constant.LEADER)) {
      getLeaderAddress();
      connectToBroker(leaderLocation);
    } else if (read.equals(Constant.FOLLOWER)) {
      getFollowerAddress();
      connectToBroker(followerLocation);
    }

    sendToBroker(topic, startingPosition, model);
  }

  /**
   * Handle pull and push model request
   *
   * @param topic             msg topic
   * @param startingPosition  offset
   * @param model             push or pull model
   */
  private void sendToBroker(String topic, int startingPosition, String model) {
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
  public void connectToBroker(String brokerLocation) {
    connection = PubSubUtils.connectToBroker(brokerLocation,
        new FaultInjectorFactory(0).getChaos());
    LOGGER.info("Consumer is connected to broker: " + brokerLocation);
  }

  /**
   * Introducing self to broker
   *
   * @param topic String topic
   */
  public void sendPushBasedRequestToBroker (String topic, int startingOffset) {
    Message msgInfo = Message.newBuilder()
        .setTypeValue(Constant.SUBSCRIBER)
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
        .setTypeValue(Constant.CONSUMER_TYPE)
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
      if (isUpdatingMembership) {
        reconnectConsumer();
        isUpdatingMembership = false;
      }
    }
  }

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
        isUpdatingMembership = true;
        LOGGER.info("Reconnecting...");
        reconnectPushBasedConsumer();
        break;
      }

      if (msgByte != null) {
        Message msgFromBroker = null;
        try {
          msgFromBroker = Message.parseFrom(msgByte);
        } catch (InvalidProtocolBufferException e) {
          LOGGER.warning("Error in getting msg from broker: " + e.getMessage());
          reconnectConsumer();
          sendToBroker(topic, startingPosition, model);
        }
        if (msgFromBroker != null && msgFromBroker.getTypeValue() == Constant.BROKER_TYPE) {
          if (read.equals(Constant.FOLLOWER) &&
              msgFromBroker.getTopic().equals(Constant.FOLLOWER)) {
            PubSubUtils.wait(Constant.TIMER_COUNT);
            followerLocation = msgFromBroker.getSrcId();
            LOGGER.info("Receive follower info: " + msgFromBroker.getId() + " | " + followerLocation);
            connectToBroker(msgFromBroker.getSrcId());
            sendToBroker(topic, startingPosition, model);
          }
          if (msgFromBroker.getTopic().equals(Constant.CLOSE)) {
             startingPosition = offsetCount;
             LOGGER.info("Current starting offset: " + startingPosition);
             isRunning = false;
             continue;
          }
          offsetCount += msgFromBroker.getData().size();
          startingPosition += msgFromBroker.getData().size();
          byte[] data = msgFromBroker.getData().toByteArray();
          LOGGER.info("Received from broker message topic: "
              + msgFromBroker.getTopic() + ". Data: " + msgFromBroker.getData().toStringUtf8());
          blockingQueue.put(data);

          if (model.equals(Constant.PUSH)) {
            PubSubUtils.flushToFile(data);
          }
        }
      }
    }
  }

  /**
   * Send request to loadBalancer
   *
   * @param type consumer type
   * @return brokerLocation
   */
  private String sendRequestToLoadBalancer(int type) {
    ReplicationUtils.sendAddressRequest(loadBalancerConnection, type, id);

    FutureTask<MemberInfo> future = new FutureTask<>(() ->
        MemberInfo.parseFrom(loadBalancerConnection.receive()));
    executor.execute(future);

    String host = "";
    int port = 0;
    try {
      host = future.get().getHost();
      port = future.get().getPort();
    } catch (ExecutionException | InterruptedException e) {
      LOGGER.warning("Error in getting address" + e.getMessage());
    }
    return PubSubUtils.getBrokerLocation(host, port);
  }

  /**
   * Get leader pubsub location
   */
  public String getFollowerAddress() {
    LOGGER.info("Sending follower address request...");
    String brokerLocation = sendRequestToLoadBalancer(Constant.PUSH_BASED_CONSUMER_TYPE);
    if (!brokerLocation.equals(followerLocation)) {
      followerLocation = brokerLocation;
    } else {
      followerLocation = "";
    }
    LOGGER.info("Follower location: " + followerLocation);
    return followerLocation;
  }

  /**
   * Get leader pubsub location
   */
  public String getLeaderAddress() {
    String brokerLocation = sendRequestToLoadBalancer(Constant.CONSUMER_TYPE);
    if (!brokerLocation.equals(leaderLocation)) {
      leaderLocation = brokerLocation;
    } else {
      leaderLocation = "";
    }
    LOGGER.info("Leader location: " + leaderLocation);
    return leaderLocation;
  }

  /**
   * Reconnect consumer
   */
  private void reconnectConsumer() {
    LOGGER.info("Reconnecting...");
    PubSubUtils.wait(Constant.TIMER_COUNT);
    if (read.equals(Constant.LEADER)) {
      leaderLocation = getLeaderAddress();
      connectToBroker(leaderLocation);
    } else if (read.equals(Constant.FOLLOWER)) {
      followerLocation = getFollowerAddress();
      connectToBroker(followerLocation);
    }
  }

  /**
   * Reconnect the push based consumer upon failure
   */
  private void reconnectPushBasedConsumer() {
    if (model.equals(Constant.PUSH)) {
      PubSubUtils.wait(Constant.TIMER_COUNT);
      if (read.equals(Constant.LEADER)) {
        leaderLocation = getLeaderAddress();
        if(!leaderLocation.equals("")) {
          connectToBroker(leaderLocation);
        }
      } else if (read.equals(Constant.FOLLOWER)) {
        followerLocation = getFollowerAddress();
        connectToBroker(followerLocation);
      }
      isUpdatingMembership = false;
      sendRequestToBroker(topic, startingPosition);
    }
  }

}