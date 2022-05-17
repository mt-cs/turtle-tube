package controllers.pubsubframework;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import controllers.faultinjector.FaultInjectorFactory;
import controllers.messagingframework.ConnectionHandler;
import controllers.replicationmodule.ReplicationUtils;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.logging.Logger;
import model.Membership.MemberInfo;
import model.MsgInfo;
import model.MsgInfo.Message;
import util.Constant;

/**
 * Connect and publishes messages to a Broker
 *
 * @author marisatania
 */
public class ProducerReplication {
  private final String loadBalancerLocation;
  private String leaderLocation;
  private ConnectionHandler loadBalancerConnection;
  private ConnectionHandler leaderConnection;
  private final ExecutorService executor;
  private final int id;
  private int msgId;
  private volatile boolean isElecting;
  private final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

  public ProducerReplication(String loadBalancerLocation, int id) {
    this.loadBalancerLocation = loadBalancerLocation;
    this.executor = Executors.newSingleThreadExecutor();
    this.id = id;
    this.msgId = 1;
  }

  /**
   * Create connection to broker
   */
  public void connectToBroker() {
    leaderConnection = PubSubUtils.connectToBroker(leaderLocation,
        new FaultInjectorFactory(0).getChaos());
    LOGGER.info("Producer is connected to broker: " + leaderLocation);
  }

  /**
   * Create connection to LoadBalancer
   */
  public void connectToLoadBalancer() {
    this.loadBalancerConnection
        = ReplicationUtils.connectToLoadBalancer(loadBalancerLocation);
  }

  /**
   * Send data and topic
   *
   * @param topic String topic
   * @param data  byte[] data
   */
  public void send (String topic, byte[] data) {

    MsgInfo.Message msgInfo = MsgInfo.Message.newBuilder()
        .setTypeValue(Constant.PRODUCER_TYPE)
        .setSrcId(Constant.PRODUCER + id)
        .setTopic(topic)
        .setOffset(data.length)
        .setMsgId(msgId)
        .setData(ByteString.copyFrom(data))
        .build();
    if (leaderConnection.send(msgInfo.toByteArray())) {
      if (msgInfo.getTopic().equals(Constant.CLOSE)) {
        msgId--;
        return;
      }
      PubSubUtils.logMsgInfo(msgInfo);
    }

    if (msgInfo.getTopic().equals(Constant.CLOSE)) {
      return;
    }
    if (getIsReceivedAck(leaderConnection, data.length)) {
      LOGGER.info("Producer received ACK for data: " + msgId);
      msgId++;
    } else {
      LOGGER.warning("Missing ACK for data: " + msgId);
      handleElection(msgInfo);
    }
  }

  /**
   * Send log to data to load balancer with no key specified
   */
  public String getLeaderAddress() {

    ReplicationUtils.sendAddressRequest(loadBalancerConnection,
                                        Constant.PRODUCER_TYPE, id);

    FutureTask<MemberInfo> future =
        new FutureTask<>(() -> MemberInfo.parseFrom(loadBalancerConnection.receive()));
    executor.execute(future);

    String leaderHost = "";
    int leaderPort = 0;
    try {
      leaderHost = future.get().getHost();
      leaderPort = future.get().getPort();
    } catch (ExecutionException | InterruptedException e) {
      e.printStackTrace();
    }

    String brokerLocation = PubSubUtils.getBrokerLocation(leaderHost, leaderPort);

    if (!brokerLocation.equals(leaderLocation)) {
      leaderLocation = brokerLocation;
    } else {
      leaderLocation = "";
    }
    LOGGER.info("Leader location: " + leaderLocation);
    return leaderLocation;
  }

  /**
   * Get ACK from broker
   *
   * @param connection ConnectionHandler
   * @param length     msg length
   * @return true if ACK is received
   */
  public boolean getIsReceivedAck(ConnectionHandler connection, int length) {

    byte[] ackArr = new byte[0];
    try {
      ackArr = connection.receive();
    } catch (IOException ioe) {
      LOGGER.warning("IOException in broker receivingAck: " + ioe.getMessage());
      connection.close();
      isElecting = true;
    }

    MsgInfo.Message ack = null;
    try {
      ack = Message.parseFrom(ackArr);
    } catch (InvalidProtocolBufferException e) {
      LOGGER.warning("Error in getting ACK: " + e.getMessage());
    }
    return ack != null &&
        ack.getTypeValue() == 3 &&
        ack.getMsgId() == msgId &&
        ack.getOffset() == length;
  }

  /**
   * Handle election
   *
   * @param msgInfo Message Info
   */
  private void handleElection(Message msgInfo) {
    if (!isElecting)  {
      PubSubUtils.wait(Constant.PRODUCER_TIMER_COUNTER);
      if (leaderConnection.send(msgInfo.toByteArray())) {
        PubSubUtils.logMsgInfo(msgInfo);
      }
    } else {
      PubSubUtils.wait(Constant.PRODUCER_TIMER_COUNTER);
      leaderLocation = getLeaderAddress();
      connectToBroker();
      isElecting = false;
    }
  }

  /**
   * Close connection
   */
  public void close () {
    send(Constant.CLOSE, Constant.CLOSE.getBytes(StandardCharsets.UTF_8));
  }

}