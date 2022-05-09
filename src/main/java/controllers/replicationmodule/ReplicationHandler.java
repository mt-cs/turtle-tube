package controllers.replicationmodule;

import controllers.membershipmodule.MembershipTable;
import controllers.membershipmodule.MembershipUtils;
import controllers.messagingframework.ConnectionHandler;
import controllers.pubsubframework.PubSubUtils;
import interfaces.FaultInjector;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import model.MsgInfo;
import model.MsgInfo.Message;
import util.Constant;

/**
 * Handle data replication
 *
 * @author marisatania
 */
public class ReplicationHandler {
  private final MembershipTable membershipTable;
  private final String host;
  private final int port;
  private final int brokerId;
  private final ConcurrentHashMap<String, List<Message>> topicMap;
  private final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

  /**
   * Constructor
   *
   * @param membershipTable MembershipTable instance
   * @param host            broker's host
   * @param pubSubPort      broker's port
   * @param brokerId        broker's ID
   * @param topicMap        topic concurrent map
   */
  public ReplicationHandler(MembershipTable membershipTable,
      String host, int pubSubPort, int brokerId,
      ConcurrentHashMap<String, List<Message>> topicMap) {
    this.host = host;
    this.port = pubSubPort;
    this.brokerId = brokerId;
    this.topicMap = topicMap;
    this.membershipTable = membershipTable;
  }

  /**
   * Send snapshot request at join
   *
   * @param connection            PubSub Connection
   * @param targetBrokerLocation  target host and port
   */
  public void sendSnapshotRequest(ConnectionHandler connection, String targetBrokerLocation) {
    MsgInfo.Message msgInfo = Message.newBuilder()
        .setTypeValue(4)
        .setSrcId(PubSubUtils.getBrokerLocation(host, port))
        .setTopic(Constant.SNAPSHOT)
        .build();
    LOGGER.info("Sent request snapshot to broker: " + targetBrokerLocation);
    connection.send(msgInfo.toByteArray());
  }

  /**
   * Send data replication to another broker
   *
   * @param connectionToPeer connection
   * @param msgFromProducer  MsgInfo.Message
   */
  public boolean sendReplicateToBroker(ConnectionHandler connectionToPeer,
      MsgInfo.Message msgFromProducer) {

    Message msgInfoReplicate = Message.newBuilder()
        .setSrcId(PubSubUtils.getBrokerLocation(host, port))
        .setTopic(msgFromProducer.getTopic())
        .setOffset(msgFromProducer.getOffset())
        .setData(msgFromProducer.getData())
        .setMsgId(msgFromProducer.getMsgId())
        .setTypeValue(1)
        .build();

    boolean isSent = connectionToPeer.send(msgInfoReplicate.toByteArray());
    if (isSent) {
      LOGGER.info("Sent replication for: " + msgFromProducer.getMsgId());
    } else {
      LOGGER.warning("Failed sending replication for: " + msgFromProducer.getMsgId());
    }
    return isSent;
  }

  /**
   * Send ACK to broker
   *
   * @param connection Connection Handler
   * @param srcId      Broker ID
   * @param length     msg length
   * @param targetId   target ID
   * @param msgId      message ID
   * @return true if connection send
   */
  public boolean sendAck(ConnectionHandler connection, String srcId,
      int length, String targetId, int msgId) {
    Message ack = Message.newBuilder()
        .setTypeValue(3)
        .setSrcId(srcId)
        .setOffset(length)
        .setMsgId(msgId)
        .build();
    boolean isAckSent = connection.send(ack.toByteArray());
    if (isAckSent) {
      LOGGER.info("Sent ACK to: " + targetId + " | " + isAckSent
          + " | source: " + srcId +" | msgId: " + msgId);
    } else {
      LOGGER.warning("FAIL sending ACK to: " + targetId + " | " +
          isAckSent + " | source: " + srcId + " | msgId: " + msgId);
    }
    return isAckSent;
  }

  /**
   * Store message to topic map
   *
   * @param msg       Message Info
   * @param topicMap  topic concurrent map
   */
  public void storeMsgToTopicMap(Message msg,
      ConcurrentHashMap<String, List<Message>> topicMap) {
    if (!topicMap.containsKey(msg.getTopic())) {
      List <Message> msgList = Collections.synchronizedList(new ArrayList<>());
      msgList.add(msg);
      LOGGER.info(msg.getMsgId() + " | " + PubSubUtils.getMsgTopicInfo(msg));
      topicMap.put(msg.getTopic(), msgList);
      LOGGER.info("New Topic List: " + msg.getTopic() + " added to broker's topicMap");
    } else {
      topicMap.get(msg.getTopic()).add(msg);
      LOGGER.info(msg.getMsgId() + " | " + PubSubUtils.getMsgTopicInfo(msg));
    }
  }

  /**
   * Send snapshot from the topic Map
   *
   * @param connection Connection handler
   */
  public synchronized void sendTopicMap(ConnectionHandler connection) {
    LOGGER.info("Sending snapshot of topicMap...");
    boolean isSent;

    for (Map.Entry<String, List<Message>> topic : topicMap.entrySet()) {
      LOGGER.info("Sending topic list: " + topic.getKey());
      List<MsgInfo.Message> msgList = topicMap.get(topic.getKey());
      for (MsgInfo.Message msg : msgList) {
        MsgInfo.Message msgInfo = MsgInfo.Message.newBuilder()
            .setTypeValue(1)
            .setTopic(msg.getTopic())
            .setOffset(msg.getOffset())
            .setSrcId(PubSubUtils.getBrokerLocation(host, port))
            .setData(msg.getData())
            .setMsgId(msg.getMsgId())
            .build();

        isSent = connection.send(msgInfo.toByteArray());
        if (isSent) {
          LOGGER.info(msg.getMsgId() + " | Sent snapshot of: " + msg.getData());
        } else {
          LOGGER.warning("Sent failed for msgId: " + msg.getMsgId());
        }
      }
    }
  }

  /**
   * Send replicate to followers
   *
   * @param msg           Message info
   * @param faultInjector fault injector
   * @return true if sent
   */
  public boolean sendReplicateToAllBrokers(MsgInfo.Message msg, FaultInjector faultInjector) {
    if (membershipTable.size() == 1) {
      LOGGER.info(membershipTable.toString());
      LOGGER.info("No follower connection found.");
      return true;
    }
    updatePubSubConnection(faultInjector);

    boolean isSent = false;

    for (var broker : membershipTable) {
      if (broker.getKey() != brokerId) {
        String targetBroker = broker.getValue().getPubSubLocation();
        LOGGER.info("Sending replicate to broker: " + targetBroker);
        ConnectionHandler connectionToPeer = broker.getValue().getPubSubConnection();
        if (connectionToPeer == null) {
          connectionToPeer = new ConnectionHandler(broker.getValue().getHost(),
              broker.getValue().getPort(), faultInjector);
          MembershipUtils.updatePubSubConnection(membershipTable,
              msg.getSrcId(), connectionToPeer);
        }
        isSent = sendReplicateToBroker(connectionToPeer, msg);
        if (!isSent) {
          LOGGER.warning("Msg lost: " + msg.getMsgId() + " to broker: " + targetBroker);
          PubSubUtils.wait(10000);
          LOGGER.info("Member key " + broker.getKey() + "is failed: "
              + membershipTable.notContainsMember(broker.getKey()));
          return membershipTable.notContainsMember(broker.getKey());
        }
      }
    }
    return isSent;
  }

  /**
   * Update connection for pubsub
   *
   * @param faultInjector fault injector
   */
  public synchronized void updatePubSubConnection(FaultInjector faultInjector) {
    for (var broker : membershipTable) {
      if (broker.getKey() != brokerId) {
        ConnectionHandler connectionToPeer = broker.getValue().getPubSubConnection();
        if (connectionToPeer == null) {
          connectionToPeer = new ConnectionHandler(broker.getValue().getHost(),
              broker.getValue().getPort(), faultInjector);
          MembershipUtils.updatePubSubConnection(membershipTable,
              broker.getValue().getPubSubLocation(), connectionToPeer);
        }
      }
    }
  }
}