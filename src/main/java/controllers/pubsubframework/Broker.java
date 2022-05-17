package controllers.pubsubframework;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import controllers.electionmodule.BullyElectionManager;
import controllers.heartbeatmodule.HeartBeatScheduler;
import controllers.membershipmodule.MembershipTable;
import controllers.membershipmodule.MembershipUtils;
import controllers.messagingframework.ConnectionHandler;
import controllers.messagingframework.Listener;
import controllers.faultinjector.FaultInjectorFactory;
import controllers.replicationmodule.ReplicationHandler;
import controllers.replicationmodule.ReplicationUtils;
import interfaces.FaultInjector;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import model.MemberAccount;
import model.Membership;
import model.Membership.BrokerInfo;
import model.Membership.BrokerList;
import model.Membership.MemberInfo;
import model.MsgInfo;
import model.MsgInfo.Message;
import util.Constant;
import util.ReplicationAppUtils;

/**
 * Accept an unlimited number of connection requests
 * from producers and consumers.
 *
 * @author marisatania
 */
public class Broker {
  private final ExecutorService threadPool;
  private String host;
  private String leaderLocation;
  private final int port;
  private int leaderBasedPort;
  private int brokerId;
  private int offsetVersionCount;

  private volatile boolean isLeader;
  private volatile boolean isSyncUp;
  private volatile boolean isSnapshot;
  private boolean isRf;
  private ConnectionHandler leaderConnection;
  private MembershipTable membershipTable;
  private HeartBeatScheduler heartBeatScheduler;
  private BullyElectionManager bullyElection;
  private ReplicationHandler replicationHandler;
  private FaultInjector faultInjector;
  private final ConcurrentHashMap<Path, List<Integer>> offsetIndexMap;
  private final ConcurrentHashMap<String, List<Message>> topicMap;
  private final ConcurrentHashMap<String, List<Message>> topicMapReplicationSyncUp;
  private volatile boolean isRunning = true;
  private final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

  /**
   * Constructor for basic broker
   *
   * @param port broker port number
   */
  public Broker(int port) {
    this.port = port;
    this.topicMap = new ConcurrentHashMap<>();
    this.topicMapReplicationSyncUp = new ConcurrentHashMap<>();
    this.offsetIndexMap = new ConcurrentHashMap<>();
    this.threadPool
        = Executors.newFixedThreadPool(Constant.NUM_THREADS);
    this.offsetVersionCount = 0;
  }

  /**
   * Constructor for leader based broker
   *
   * @param host            broker's host
   * @param pubSubPort      broker's pubsub port
   * @param leaderBasedPort broker's leaderBased port
   * @param isLeader        broker's leader status
   * @param brokerId        broker's leader ID
   * @param faultType       fault injector type
   */
  public Broker(String host, int pubSubPort, int leaderBasedPort,
      boolean isLeader, int brokerId, int faultType, boolean isRf) {
    this.host = host;
    this.port = pubSubPort;
    this.leaderBasedPort = leaderBasedPort;
    this.brokerId = brokerId;
    this.isLeader = isLeader;
    this.isRf = isRf;
    this.isSyncUp = true;
    this.isSnapshot = false;
    this.topicMap = new ConcurrentHashMap<>();
    this.topicMapReplicationSyncUp = new ConcurrentHashMap<>();
    this.offsetIndexMap = new ConcurrentHashMap<>();
    this.offsetVersionCount = 0;
    this.threadPool = Executors.newFixedThreadPool(Constant.NUM_THREADS);
    this.faultInjector = new FaultInjectorFactory(faultType).getChaos();
    this.membershipTable = new MembershipTable();

    ConnectionHandler loadBalancerConnection =
        new ConnectionHandler(Constant.LOCALHOST, Constant.LB_PORT, faultInjector);
    MembershipUtils.addSelfToMembershipTable(host, port,
        leaderBasedPort, isLeader, brokerId, membershipTable);
    MembershipUtils.sendBrokerLocation(loadBalancerConnection, brokerId, host, port, isLeader);
    this.bullyElection = new BullyElectionManager(brokerId, membershipTable, loadBalancerConnection);
    this.replicationHandler = new ReplicationHandler(membershipTable, host, port, brokerId, topicMap);
    this.heartBeatScheduler = new HeartBeatScheduler(brokerId,
        membershipTable, 1000L, bullyElection);
    this.heartBeatScheduler.start();
  }

  /**
   * Listening to connections
   */
  public void listenToConnections() {
    Thread pubSubServer = new Thread( () -> {
      Listener pubSubListener = new Listener(port, faultInjector);
      while (isRunning) {
        LOGGER.info("Listening for pubSub connection on port: " + port);
        ConnectionHandler pubSubConnection = pubSubListener.nextConnection();
        threadPool.execute(() -> receiveMsg(pubSubConnection, true));
      }
    });

    Thread leaderBasedServer = new Thread( () -> {
      Listener leaderBasedListener = new Listener(leaderBasedPort, faultInjector);
      while (isRunning) {
        LOGGER.info("Listening for leaderBased connection on port: " + leaderBasedPort);
        ConnectionHandler leaderBasedConnection = leaderBasedListener.nextConnection();
        threadPool.execute(() -> receiveFromBroker(leaderBasedConnection));
      }
    });

    pubSubServer.start();
    leaderBasedServer.start();
  }

  /**
   * Receive messages from connection
   *
   * @param connection  ConnectionHandler
   * @param isReceiving boolean true
   */
  public void receiveMsg(ConnectionHandler connection, boolean isReceiving) {
    while (isReceiving) {
      byte[] msgByte;
      try {
        msgByte = connection.receive();
      } catch (IOException ioe) {
        LOGGER.warning("IOException in broker receive Msg: " + ioe.getMessage());
        connection.close();
        break;
      }

      if (msgByte != null) {
        MsgInfo.Message msg;
        try {
          msg = Message.parseFrom(msgByte);
        } catch (InvalidProtocolBufferException e) {
          LOGGER.warning("Error in getting msg: " + e.getMessage());
          if (isSyncUp) {
            LOGGER.warning("Sync up fail!");
            topicMap.clear();
            topicMapReplicationSyncUp.clear();
            for (Path filePath : offsetIndexMap.keySet()) {
              try {
                Files.delete(filePath);
              } catch (IOException ex) {
                ex.printStackTrace();
              }
            }
            leaderConnection = PubSubUtils.connectToBroker(leaderLocation, faultInjector);
            sendSnapshotToBrokerFromOffset(leaderConnection, leaderLocation);
            LOGGER.info("Resent snapshot request");
            threadPool.execute(() -> receiveMsg(leaderConnection, true));
          }
          continue;
        }
        if (msg != null) {
          if (msg.getTypeValue() == 0) {
            if (msg.getTopic().equals(Constant.CLOSE)) {
              isReceiving = false;
              PubSubUtils.wait(10000);
              MsgInfo.Message lastMsg = Message.newBuilder()
                  .setTypeValue(1)
                  .setTopic("close")
                  .build();
              replicationHandler.sendReplicateToAllBrokers(lastMsg, faultInjector);
              flushAllToDisk();
            } else {
              receiveFromProducer(connection, msg);
            }
          } else if (msg.getTypeValue() == 1) {
            Path filePathSave = null;
            if (msg.getTopic().equals(Constant.CLOSE)) {
              PubSubUtils.wait(10000);
              flushAllToDisk();
              continue;
            }
            if (!msg.getTopic().equals(Constant.LAST_SNAPSHOT)) {
              filePathSave = Path.of(ReplicationAppUtils.getTopicFile(msg.getTopic()));
            }
            if (!msg.getIsSnapshot()) {

              if (isSyncUp) {
                // Continue getting replication during sync up
                LOGGER.info(msg.getMsgId() + " | Sync Up! Received msgInfo replicate from broker: " + msg.getSrcId());
                replicationHandler.storeMsgToTopicMap(msg, topicMapReplicationSyncUp);
              } else {
                // Normal replication
                LOGGER.info(msg.getMsgId() + " | Received msgInfo replicate from broker: " + msg.getSrcId());
                replicationHandler.storeMsgToTopicMap(msg, topicMap);
                if (topicMap.get(msg.getTopic()).size() > Constant.MAX_OFFSET_SIZE) {
                  flushToDisk(topicMap.get(msg.getTopic()), filePathSave);
                }
              }
            } else {
              // SYNC UP
              isSyncUp = true;
              if (msg.getTopic().equals(Constant.LAST_SNAPSHOT)) {
                isSyncUp = false;
                // End of snapshot merge topic map of snapshot and replication
                LOGGER.info("Merging topic map catch up...");
                replicationHandler.copyToTopicMap(topicMapReplicationSyncUp);
                topicMapReplicationSyncUp.clear();
                LOGGER.info("Sync up completed!");
                flushAllToDisk();
                continue;
              }
              // Getting snapshot to catch up
              LOGGER.info(msg.getMsgId() + " | Sync Up! Received msgInfo snapshot from broker: " + msg.getSrcId());
              flushEachMsgToFile(msg);
            }
            replicationHandler.sendAck(connection, PubSubUtils.getBrokerLocation(host, port),
                msg.getOffset(), msg.getSrcId(), msg.getMsgId());
            membershipTable.updateBrokerVersion(brokerId, offsetVersionCount);
          } else if (msg.getTypeValue() == 2) {
            LOGGER.info("Received request from customer for message topic/offset: "
                + msg.getTopic() + "/ " + msg.getStartingPosition());
            sendToConsumerFromOffset(connection, msg);
          } else if (msg.getTypeValue() == 3) {
            LOGGER.info("Received ACK from: " + msg.getSrcId() + " for msgId: " + msg.getMsgId());
          } else if (msg.getTypeValue() == 4) {
            LOGGER.info("Received snapshot request from broker: " + msg.getSrcId());
            MembershipUtils.updatePubSubConnection(membershipTable, msg.getSrcId(), connection);
            sendSnapshotToBrokerFromOffset(connection, msg.getSrcId());
          } else if (msg.getTypeValue() == 5) {
            LOGGER.info("Received request from Push-Based customer for message topic/offset: "
                + msg.getTopic() + "/ " + msg.getStartingPosition());
            sendToPushBasedConsumer(msg.getStartingPosition(), msg.getTopic(), connection,
                ReplicationAppUtils.getTopicFile(msg.getTopic()));
          }
        }
      }
    }
  }



  /**
   * Handle messages from broker
   *
   * @param connection leader based connection
   */
  private void receiveFromBroker(ConnectionHandler connection) {
    while (isRunning) {
      byte[] brokerInfo = connection.receiveInfo();

      if (brokerInfo != null) {
        try {
          MemberInfo memberInfo = MemberInfo.parseFrom(brokerInfo);
          String brokerLocation =
              PubSubUtils.getBrokerLocation(memberInfo.getHost(), memberInfo.getLeaderPort());

          if (memberInfo.getState().equals(Constant.ALIVE)) {
            updateMembershipTable(memberInfo.getMembershipTableMap());
//            LOGGER.info(membershipTable.toString());
            if (membershipTable.isLeader(memberInfo.getId())) {
              updateMembershipTableRf(memberInfo.getReplicationTableMap());
//            LOGGER.info(memberInfo.getReplicationTableMap().toString());
//              membershipTable.rfToString();
            }
            heartBeatScheduler.handleHeartBeatRequest(memberInfo.getId());
          } else if (memberInfo.getState().equals(Constant.CONNECT))  {
            if (isSnapshot && !memberInfo.getIsLeader()) {
              MembershipUtils.addToMembershipTable(connection, memberInfo, membershipTable);
            }
          } else if (memberInfo.getState().equals(Constant.ELECTION)) {
            bullyElection.handleElectionRequest(connection, memberInfo.getId());
          } else if (memberInfo.getState().equals(Constant.CANDIDATE)) {
            bullyElection.handleCandidateRequest();
          } else if (memberInfo.getState().equals(Constant.VICTORY)) {
            bullyElection.handleVictoryRequest(memberInfo.getId());
          }
//          LOGGER.info("Received " + memberInfo.getState()
//              + " from broker: " + memberInfo.getId() + " | " + brokerLocation);

        } catch (InvalidProtocolBufferException e) {
          LOGGER.info("Protobuf exception: " + e.getMessage());
        }
      } else {
        connection.close();
        break;
      }
    }
  }

  /**
   * Connect to another broker
   *
   * @param targetBrokerLocation        pubsub host:port
   * @param targetLeaderBasedConnection leader based host:port
   * @param targetId                    target broker's ID
   */
  public void connectToPeer(String targetBrokerLocation,
      String targetLeaderBasedConnection, int targetId) {

    ConnectionHandler connectionToPeer = PubSubUtils.connectToBroker
        (targetLeaderBasedConnection, faultInjector);
    LOGGER.info("Connected to " + targetLeaderBasedConnection);

    MemberInfo memberInfo = MemberInfo.newBuilder()
        .setTypeValue(1)
        .setId(brokerId)
        .setHost(host)
        .setPort(port)
        .setLeaderPort(leaderBasedPort)
        .setIsAlive(true)
        .setIsLeader(isLeader)
        .setState(Constant.CONNECT)
        .build();
    connectionToPeer.send(memberInfo.toByteArray());
    LOGGER.info("My info: " +
        PubSubUtils.getBrokerLocation(memberInfo.getHost(), memberInfo.getLeaderPort()));
    LOGGER.info("Sending connection member info to " + targetLeaderBasedConnection);

    MembershipUtils.addTargetToMembershipTable(targetBrokerLocation, targetId,
        targetLeaderBasedConnection, membershipTable, connectionToPeer);
    threadPool.execute(() -> receiveFromBroker(connectionToPeer));

    if (!isSnapshot && membershipTable.get(targetId).isLeader()) {
      isSnapshot = true;
      leaderLocation = targetBrokerLocation;
      LOGGER.info("Leader location: " + leaderLocation);
      this.leaderConnection = PubSubUtils.connectToBroker(leaderLocation, faultInjector);
      replicationHandler.sendSnapshotRequest(leaderConnection, leaderLocation);
      threadPool.execute(() -> receiveMsg(leaderConnection, true));
    }
  }

  /**
   * Receive message from producer
   *
   * @param connection      Pubsub connection
   * @param msgFromProducer MsgInfo.Message
   */
  public void receiveFromProducer(ConnectionHandler connection, Message msgFromProducer) {
    Path filePathSave = Path.of(ReplicationAppUtils.getTopicFile(msgFromProducer.getTopic()));
    if (!topicMap.containsKey(msgFromProducer.getTopic())) {
      List <MsgInfo.Message> msgList = Collections.synchronizedList(new ArrayList<>());
      msgList.add(msgFromProducer);
      LOGGER.info(PubSubUtils.getMsgTopicInfo(msgFromProducer));
      topicMap.put(msgFromProducer.getTopic(), msgList);
      LOGGER.info("New Topic List: " + msgFromProducer.getTopic() + " added to broker's topicMap");

      // Set up replication rf broker List
      List<MemberAccount> rfBrokerList = replicationHandler.getRfBrokerList();

      // update membership replication map
      membershipTable.addRfBrokerList(msgFromProducer.getTopic(), rfBrokerList);
//      membershipTable.rfToString();

    } else {
      topicMap.get(msgFromProducer.getTopic()).add(msgFromProducer);
      LOGGER.info(PubSubUtils.getMsgTopicInfo(msgFromProducer));
    }

    if (topicMap.get(msgFromProducer.getTopic()).size() > Constant.MAX_OFFSET_SIZE) {
      flushToDisk(topicMap.get(msgFromProducer.getTopic()), filePathSave);
    }

    boolean isAckSent = false;
    if (!isRf) {
      if (replicationHandler.sendReplicateToAllBrokers(msgFromProducer, faultInjector)) {
        isAckSent = replicationHandler.sendAck(connection, PubSubUtils.getBrokerLocation(host, port),
            msgFromProducer.getOffset(), msgFromProducer.getSrcId(), msgFromProducer.getMsgId());
      }
    } else {
      if (replicationHandler.sendReplicateToRFFollowers(msgFromProducer, faultInjector,
          membershipTable.getRfMap().get(msgFromProducer.getTopic()))) {
        isAckSent = replicationHandler.sendAck(connection,
            PubSubUtils.getBrokerLocation(host, port),
            msgFromProducer.getOffset(), msgFromProducer.getSrcId(), msgFromProducer.getMsgId());
      }
    }

    if (isAckSent) {
      LOGGER.info("Broker received msgId: " + msgFromProducer.getMsgId());
    }
  }

  private void flushAllToDisk() {
    for (Entry<String, List<Message>> topic : topicMap.entrySet()) {
      flushToDisk(topicMap.get(topic.getKey()),
          Path.of(ReplicationAppUtils.getTopicFile(topic.getKey())));
    }
  }

  /**
   * Flush message to disk
   *
   * @param topicList list of topics
   */
  public synchronized void flushToDisk(List<MsgInfo.Message> topicList, Path filePathSave) {
    LOGGER.info("Flushing to disk...");
    for (int i = 0; i < topicList.size(); i++) {
      Message msg = topicList.get(i);
      byte[] msgArr = msg.getData().toByteArray();
      if (!Files.exists(filePathSave)) {
        try {
          Files.write(filePathSave, msgArr);
          incrementOffset(msg, filePathSave);
          topicMap.get(msg.getTopic()).remove(msg);
        } catch (IOException e) {
          LOGGER.warning("Error while flushing to disk: " + e.getMessage());
        }
      } else {
        try {
          Files.write(filePathSave, msgArr, StandardOpenOption.APPEND);
          incrementOffset(msg, filePathSave);
          topicMap.get(msg.getTopic()).remove(msg);
        } catch (IOException e) {
          LOGGER.warning("Error while flushing to disk: " + e.getMessage());
        }
      }
    }
  }

  public void flushEachMsgToFile(Message msg) {
    byte[] data = msg.getData().toByteArray();
    if (data != null) {
      Path filePathSave = Path.of(ReplicationAppUtils.getTopicFile(msg.getTopic()));
      if (!Files.exists(filePathSave)) {
        try {
          LOGGER.info("Creating topic file path: " + filePathSave);
          Files.write(filePathSave, data);
          incrementOffset(msg, filePathSave);
        } catch (IOException e) {
          LOGGER.warning("Exception during topic replication write: " + e.getMessage());
        }
      } else {
        try {
          Files.write(filePathSave, data, StandardOpenOption.APPEND);
          incrementOffset(msg, filePathSave);
        } catch (IOException e) {
          LOGGER.warning("Topic file write exception: " + e.getMessage());
        }
      }
    }
  }

  private void incrementOffset(Message msg, Path filePath) {
    int currOffset;
    offsetVersionCount += msg.getOffset();
    if (!offsetIndexMap.containsKey(filePath)) {
      List <Integer> offsetIndexList = Collections.synchronizedList(new ArrayList<>());
      offsetIndexList.add(0);
      currOffset = msg.getOffset();
      offsetIndexList.add(currOffset);
      offsetIndexMap.put(filePath, offsetIndexList);
    } else {
      List <Integer> offsetIndexList = offsetIndexMap.get(filePath);
      currOffset = offsetIndexList.get(offsetIndexList.size() - 1) + msg.getOffset();
      offsetIndexMap.get(filePath).add(currOffset);
    }
  }

  /**
   * Send message to consumer using offset
   *
   * @param msg protobuf message
   */
  public void sendToConsumerFromOffset(ConnectionHandler connection, Message msg) {
    int startingOffset = msg.getStartingPosition();
    int countOffsetSent = 0, offset;
    while (countOffsetSent <= Constant.MAX_BYTES) {
      String fileName = ReplicationAppUtils.getTopicFile(msg.getTopic());
      byte[] data = getBytes(startingOffset, fileName,
          offsetIndexMap.get(Path.of(fileName)));
      offset = data.length;
      startingOffset += offset;
      countOffsetSent += offset;
      MsgInfo.Message msgInfo = MsgInfo.Message.newBuilder()
          .setTypeValue(1)
          .setTopic(msg.getTopic())
          .setData(ByteString.copyFrom(data))
          .setOffset(offset)
          .build();
      if (msgInfo.getData().startsWith(ByteString.copyFromUtf8("\0"))) {
        return;
      }
      LOGGER.info("Sending data to consumer: " + ByteString.copyFrom(data));
      connection.send(msgInfo.toByteArray());
    }
    sendClose(connection);
  }

  /**
   * Send snapshot to broker using offset
   */
  public void sendSnapshotToBrokerFromOffset(ConnectionHandler connection, String srcId) {
    Map<String, List<Message>> currentTopicMap = topicMap.entrySet().stream()
        .collect(Collectors.toMap(Entry::getKey, e -> List.copyOf(e.getValue())));

    // Flush current topicMap to Offset
    for (Map.Entry<String, List<Message>> topic : currentTopicMap.entrySet()) {
      for (Message msg : topic.getValue()) {
        flushEachMsgToFile(msg);
        topicMap.get(msg.getTopic()).remove(msg);
      }
    }

    // Copy all current topic files snapshot
    ConcurrentHashMap<Path, List<Integer>> offsetIndexMapCopy = new ConcurrentHashMap<>();
    for (Path filePath : offsetIndexMap.keySet()) {
      Path fileCopyPath = ReplicationUtils.copyTopicFiles(filePath, srcId);
      offsetIndexMapCopy.putIfAbsent(fileCopyPath,
          new CopyOnWriteArrayList<>(offsetIndexMap.get(filePath)));
    }

    LOGGER.info("Sending snapshot... Broker version offset: " + offsetVersionCount);
    int countOffsetSent = 0, id = 1, offset;
    boolean isSent;

    for(Path filePath : offsetIndexMapCopy.keySet()) {
      LOGGER.info("Sending snapshot... " + filePath);
      while (countOffsetSent <= PubSubUtils.getFileSize(filePath)) {

        byte[] data = getBytes(countOffsetSent, filePath.getFileName().toString(),
            offsetIndexMapCopy.get(filePath));
        if (data == null) {
          break;
        }
        String log = ByteString.copyFrom(data).toStringUtf8();
        offset = data.length;
        countOffsetSent += offset;

        MsgInfo.Message msgInfo = MsgInfo.Message.newBuilder()
            .setTypeValue(1)
            .setData(ByteString.copyFrom(data))
            .setOffset(offset)
            .setTopic(ReplicationUtils.getTopic(log))
            .setSrcId(PubSubUtils.getBrokerLocation(host, port))
            .setMsgId(id)
            .setIsSnapshot(true)
            .build();
        if (msgInfo.getData().startsWith(ByteString.copyFromUtf8("\0"))) {
          break;
        }
        isSent = connection.send(msgInfo.toByteArray());
        if (isSent) {
          LOGGER.info(id++ + " | Sent snapshot of: " + new String(data));
        } else {
          LOGGER.warning("Sent failed for msgId: " + id++);
        }
      }
      countOffsetSent = 0;
      try {
        Files.delete(filePath);
      } catch (IOException e) {
        LOGGER.warning(filePath+ " delete fail: " + e.getMessage());
      }
    }
    replicationHandler.sendLastSnapshot(connection);
  }

  /**
   * Get message byte array from starting offset
   *
   * @param startingOffset starting offset
   * @return byte[] messages for consumer
   */
  public byte[] getBytes(int startingOffset, String fileName, List<Integer> offsetIndex) {
    int nextIdx, byteSize;
    byte[] data = new byte[0];
    try (InputStream inputStream = new FileInputStream(fileName)) {
      if (!offsetIndex.contains(startingOffset)) {
        startingOffset = PubSubUtils.getClosestOffset(offsetIndex, startingOffset);
      }
      nextIdx = offsetIndex.indexOf(startingOffset) + 1;
      if (nextIdx >= offsetIndex.size()) {
        return null;
      }
      byteSize = offsetIndex.get(nextIdx) - startingOffset;
      if (byteSize < 0) {
        return null;
      }
      data = new byte[byteSize];
      inputStream.skip(startingOffset);
      inputStream.read(data);
    } catch (IOException e) {
      LOGGER.warning("Error in reading from persistent log: " + e.getMessage());
    }
    return data;
  }

  /**
   * Sending message to push-based customer
   * from starting position to max pull
   *
   * @param startingOffset initial message offset
   * @param topic          topic
   */
  public void sendToPushBasedConsumer(int startingOffset, String topic,
      ConnectionHandler connection, String fileName) {
    LOGGER.info("Sending data to push-based consumer: ");
    int countOffsetSent = 0, offset;
    while (countOffsetSent <= PubSubUtils.getFileSize(Path.of(fileName))) {
      byte[] data = getBytes(startingOffset, fileName,
          offsetIndexMap.get(Path.of(fileName)));
      if (data == null) {
        return;
      }
      offset = data.length;
      if (offset == 0) {
        return;
      }
      startingOffset += offset;
      countOffsetSent += offset;
      MsgInfo.Message msgInfo = MsgInfo.Message.newBuilder()
          .setTypeValue(1)
          .setTopic(topic)
          .setData(ByteString.copyFrom(data))
          .setOffset(offset)
          .build();
      if (msgInfo.getData().startsWith(ByteString.copyFromUtf8("\0"))) {
        return;
      }
      LOGGER.info("Sent data: " + ByteString.copyFrom(data));
      connection.send(msgInfo.toByteArray());
    }
    sendClose(connection);
  }

  /**
   * Send a closing message
   */
  private void sendClose(ConnectionHandler connection) {
    MsgInfo.Message msgInfo = Message.newBuilder()
        .setTypeValue(1)
        .setTopic("close")
        .build();
    connection.send(msgInfo.toByteArray());
  }

  /**
   * Update membership table based on received heartbeat
   *
   * @param rfBrokerMap membership table instance for protobuf
   */
  private void updateMembershipTableRf(Map<String, BrokerList> rfBrokerMap) {
    List<MemberAccount> brokerAccountList = Collections.synchronizedList(new ArrayList<>());
    Membership.BrokerList brokerList;
    for (String topic : rfBrokerMap.keySet()) {
      brokerList = rfBrokerMap.get(topic);
      if (brokerList == null) {
        LOGGER.info("Broker list is null");
        return;
      }
      membershipTable.getReplicationMap().putIfAbsent(topic, brokerList);
//      membershipTable.getReplicationMap().replace(topic, brokerList);
//      LOGGER.info("TOPIC RF");
//      LOGGER.info(membershipTable.getReplicationMap().toString());
      updateRfMap(brokerAccountList, brokerList, topic);
    }
  }

  private void updateRfMap(List<MemberAccount> brokerAccountList,
      BrokerList brokerList, String topic) {
    BrokerInfo brokerInfo;
    MemberAccount memberAccount;

    for (int i = 0; i < brokerList.getBrokerInfoCount(); i++) {
      brokerInfo = brokerList.getBrokerInfo(i);
      memberAccount = new MemberAccount(brokerInfo.getHost(), brokerInfo.getPort(),
          brokerInfo.getIsLeader(), brokerInfo.getId(), brokerInfo.getLeaderPort());
      brokerAccountList.add(memberAccount);
    }

    if (!brokerAccountList.isEmpty()) {
      membershipTable.setRfMap(topic, brokerAccountList);
    }
    brokerAccountList.clear();
  }

  /**
   * Update membership table based on received heartbeat
   *
   * @param protoMap membership table instance for protobuf
   */
  private void updateMembershipTable(Map<Integer, Membership.BrokerInfo> protoMap) {
    Membership.BrokerInfo protoInfo;
    for (Integer brokerId : protoMap.keySet()) {
      protoInfo = protoMap.get(brokerId);
      String leaderBasedLocation =
          PubSubUtils.getBrokerLocation(protoInfo.getHost(), protoInfo.getLeaderPort());

      if (!membershipTable.getMembershipMap().containsKey(brokerId)
          && !membershipTable.getFailure()) {
        LOGGER.info(leaderBasedLocation + " is leader: " + protoInfo.getIsLeader());
        connectToPeer(PubSubUtils.getBrokerLocation(protoInfo.getHost(),
            protoInfo.getPort()), leaderBasedLocation, protoInfo.getId());

        if (!isSnapshot && protoInfo.getIsLeader()) {
          isSnapshot = true;
          leaderLocation =
              PubSubUtils.getBrokerLocation(protoInfo.getHost(), protoInfo.getPort());
          LOGGER.info("Leader location: " + leaderLocation);
          this.leaderConnection = PubSubUtils.connectToBroker(leaderLocation, faultInjector);
          replicationHandler.sendSnapshotRequest(leaderConnection, leaderLocation);
          threadPool.execute(() -> receiveMsg(leaderConnection, true));
        }
      }
    }
  }
}