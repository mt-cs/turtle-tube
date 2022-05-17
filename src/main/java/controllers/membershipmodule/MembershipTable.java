package controllers.membershipmodule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;
import model.MemberAccount;
import model.Membership;
import model.Membership.BrokerInfo;
import model.Membership.BrokerList;
import util.Constant;

/**
 * Class for membership table instance
 *
 * @author marisatania
 */
public class MembershipTable implements Iterable<Map.Entry<Integer, MemberAccount>> {
  private final ConcurrentMap<Integer, MemberAccount> membershipMap;
  private final ConcurrentMap<Integer, Membership.BrokerInfo> protoMap;
  private final ConcurrentHashMap<String, List<MemberAccount>> rfMap;
  private final ConcurrentMap<String, BrokerList> replicationMap;
  private volatile boolean isFailure;
  private volatile boolean isFollowerFail;
  private final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

  /**
   * Constructor
   */
  public MembershipTable() {
    this.membershipMap = new ConcurrentHashMap<>();
    this.protoMap = new ConcurrentHashMap<>();
    this.replicationMap = new ConcurrentHashMap<>();
    this.rfMap = new ConcurrentHashMap<>();
  }

  @Override
  public Iterator<ConcurrentMap.Entry<Integer, MemberAccount>> iterator() {
    return membershipMap.entrySet().iterator();
  }

  /**
   * Key id, value brokerInfo to the map and protomap
   *
   * @param id         broker ID
   * @param brokerInfo Member account
   */
  public synchronized void add(int id, MemberAccount brokerInfo) {
    membershipMap.computeIfAbsent(id, v -> brokerInfo);
    protoMap.computeIfAbsent(id, v -> getProtoInfo(id, brokerInfo));
  }

  /**
   * Key id, value brokerInfo to the map and protomap
   *
   * @param topic      String msg topic
   * @param memberList Member account list
   */
  public synchronized void addRfBrokerList(String topic, List<MemberAccount> memberList) {
    LOGGER.info("Adding topic to replication map: " + topic);
    rfMap.computeIfAbsent(topic, v -> memberList);
    replicationMap.computeIfAbsent(topic, v -> getRfBrokerInfoList(memberList));
  }

  /**
   * Remove member from map
   *
   * @param id broker ID
   */
  public void remove(int id) {
    membershipMap.remove(id);
    protoMap.remove(id);
    removeBrokerRfMap(id);
    removeBrokerReplicationMap(id);
  }

  /**
   * Get membership map
   *
   * @return membership map
   */
  public ConcurrentMap<Integer, MemberAccount> getMembershipMap() {
    return membershipMap;
  }

  /**
   * Get protobuf map
   *
   * @return proto<ap
   */
  public ConcurrentMap<Integer, Membership.BrokerInfo> getProtoMap() {
    return protoMap;
  }

  /**
   * Get brokerInfo from id
   *
   * @param id brokerID
   * @return Member account
   */
  public MemberAccount get(int id) {
    return membershipMap.getOrDefault(id, null);
  }

  /**
   * Check if particular broker is a leader
   */
  public boolean isLeader(int id) {
    return membershipMap.get(id).isLeader();
  }

  /**
   * Set broker as the leader
   *
   * @param id       broker's ID
   * @param isLeader boolean
   */
  public void setLeader(int id, boolean isLeader) {
    membershipMap.get(id).setLeader(isLeader);
    protoMap.get(id).newBuilderForType().setIsLeader(isLeader);
    LOGGER.info("Found new leader: " + isLeader + ". Update leader to: " + id);
  }

  /**
   * Update version during receive
   *
   * @param brokerId broker ID
   * @param version  data version
   */
  public void updateBrokerVersion(int brokerId, int version) {
    membershipMap.get(brokerId).setVersion(version);
    protoMap.get(brokerId).newBuilderForType().setVersion(version);
  }

  /**
   * Get size of the map
   *
   * @return membership map size
   */
  public int size() {
    return membershipMap.size();
  }

  /**
   * Check if map contains member
   *
   * @param id brokerID
   * @return true if exist
   */
  public boolean notContainsMember(int id) {
    return !membershipMap.containsKey(id);
  }

  /**
   * Set failure if member failed
   *
   * @param isFailure boolean
   */
  public void setFailure(boolean isFailure) {
    this.isFailure = isFailure;
  }

  /**
   * Get if detected member failed
   *
   * @return isFailure
   */
  public boolean getFailure (){
    return isFailure;
  }

  public boolean isFollowerFail() {
    return isFollowerFail;
  }

  /**
   * Get replication factor protobuf map
   *
   * @return replicationMap
   */
  public ConcurrentMap<String, BrokerList> getReplicationMap() {
    return replicationMap;
  }

  /**
   * Create protobuf broker info
   *
   * @param id          broker's ID
   * @param brokerInfo  Member Account
   * @return proto obj
   */
  public synchronized Membership.BrokerInfo getProtoInfo (int id, MemberAccount brokerInfo) {
    return Membership.BrokerInfo.newBuilder()
        .setId(id)
        .setHost(brokerInfo.getHost())
        .setPort(brokerInfo.getPort())
        .setLeaderPort(brokerInfo.getLeaderBasedPort())
        .setIsLeader(brokerInfo.isLeader())
        .setIsAlive(true)
        .build();
  }



  /**
   * Getter for replication factor topic map
   *
   * @return rfMap
   */
  public ConcurrentHashMap<String, List<MemberAccount>> getRfMap() {
    return rfMap;
  }

  public synchronized void setRfMap(String topic, List<MemberAccount> memberList) {
    if (!memberList.isEmpty()) {
        List<MemberAccount> memberAccounts = new CopyOnWriteArrayList<>(memberList);
        rfMap.putIfAbsent(topic, memberAccounts);

//      else {
//        rfMap.replace(topic, memberList);
//      }
//      LOGGER.info(topic + " | " + rfMap.get(topic).toString());
    }
  }

  public synchronized void rfToString() {
    for (String topicKey : rfMap.keySet()) {
      LOGGER.info(topicKey + " | " + rfMap.get(topicKey).toString());
    }
  }

  public synchronized void removeBrokerRfMap(int id) {
    for (String topicKey : rfMap.keySet()) {
      rfMap.get(topicKey).removeIf(memberAccount -> memberAccount.getBrokerId() == id);
    }
    rfToString();
  }

  public synchronized void removeBrokerReplicationMap(int id) {
    LOGGER.info("Removing from replication map ID : " + id);
    List<BrokerInfo> rfBrokerList = Collections.synchronizedList(new ArrayList<>());
    for (String topicKey : replicationMap.keySet()) {
      BrokerList brokerList = replicationMap.get(topicKey);
      for (int i = 0; i < brokerList.getBrokerInfoCount(); i++) {
        BrokerInfo brokerInfo = brokerList.getBrokerInfo(i);
        if (brokerInfo.getId() == id) {
          LOGGER.info("DELETING: " + id);
          isFollowerFail = true;
        } else {
          rfBrokerList.add(brokerInfo);
        }
        if (isFollowerFail) {
          BrokerList brokerListNew = BrokerList.newBuilder()
              .addAllBrokerInfo(rfBrokerList)
              .build();
          replicationMap.replace(topicKey, brokerListNew);
          rfBrokerList.clear();
          break;
        }
      }
    }
    LOGGER.info(replicationMap.toString());
  }

  public synchronized void updateNewRfBrokerList() {
    LOGGER.info("Updating the fail node to maintain rf...");
    LOGGER.info(replicationMap.toString());
    List<BrokerInfo> rfBrokerList = Collections.synchronizedList(new ArrayList<>());
    List<Integer> existingIdList = Collections.synchronizedList(new ArrayList<>());
    for (String topic : replicationMap.keySet()) {
      LOGGER.info("TOPIC RF: " + topic);
      BrokerList brokerList = replicationMap.get(topic);
      for (BrokerInfo newBrokerInfo : brokerList.getBrokerInfoList()) {
        rfBrokerList.add(newBrokerInfo);
        LOGGER.info("Existing broker: " + newBrokerInfo.getId());
        existingIdList.add(newBrokerInfo.getId());
      }
      while (rfBrokerList.size() < Constant.RF) {
        LOGGER.info("New broker list size: " + rfBrokerList.size());

        rfBrokerList.add(getNewRfBrokerList(existingIdList));
      }
      BrokerList brokerListNew = BrokerList.newBuilder()
          .addAllBrokerInfo(rfBrokerList)
          .build();
      replicationMap.replace(topic, brokerListNew);
      rfBrokerList.clear();
      existingIdList.clear();
    }
    isFollowerFail = false;
    LOGGER.info(replicationMap.toString());
  }

  /**
   * Create protobuf broker info
   *
   * @param brokerInfoList  Member Account list
   * @return proto List
   */
  public synchronized BrokerList getRfBrokerInfoList (List<MemberAccount> brokerInfoList) {
    List<BrokerInfo> rfBrokerList = Collections.synchronizedList(new ArrayList<>());
    for (MemberAccount brokerInfo : brokerInfoList) {
      BrokerInfo rfBrokerInfo = getProtoInfo(brokerInfo.getBrokerId(), brokerInfo);
      rfBrokerList.add(rfBrokerInfo);
//      LOGGER.info(rfBrokerInfo.toString());
    }
    BrokerList brokerList = BrokerList.newBuilder()
        .addAllBrokerInfo(rfBrokerList)
        .build();
//    LOGGER.info(brokerList.toString());
    return brokerList;
  }

  public synchronized BrokerInfo getNewRfBrokerList(List<Integer> idList) {
    LOGGER.info("Selecting new random broker list....");

    List<BrokerInfo> brokerList = new ArrayList<>();
    for (Integer brokerId : protoMap.keySet()) {
      BrokerInfo brokerInfo = protoMap.get(brokerId);
      if (brokerInfo.getIsLeader()) {
        continue;
      }
      if (idList.size() == 0) {
        LOGGER.info("No existing broker found...");
        brokerList.add(brokerInfo);
      } else {
        for (Integer id : idList) {
          if (brokerInfo.getId() != id) {
            brokerList.add(brokerInfo);
          }
        }
      }
    }
    int randomIndex = new Random().nextInt(brokerList.size());
    LOGGER.info("New rf broker: " + brokerList.get(randomIndex).getId());
    return brokerList.get(randomIndex);
  }

  /**
   * toString method
   *
   * @return membership table String value
   */
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("MEMBERSHIP TABLE\n");
    for (Integer brokerId: membershipMap.keySet()) {
      sb.append(brokerId)
          .append(" | ")
          .append(membershipMap.get(brokerId).getLeaderBasedLocation())
          .append(" | ")
          .append(membershipMap.get(brokerId).isLeader())
          .append("\n");
    }
    return sb.toString();
  }
}
