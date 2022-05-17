package controllers.replicationmodule;

import controllers.membershipmodule.MembershipTable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;
import model.MemberAccount;
import model.Membership.BrokerInfo;
import model.Membership.BrokerList;
import util.Constant;

/**
 * Class for replication factor module
 *
 * @author marisatania
 */
public class ReplicationFactor {
  private final MembershipTable membershipTable;
  private final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

  /**
   * Constructor
   */
  public ReplicationFactor(MembershipTable membershipTable) {
    this.membershipTable = membershipTable;
  }

  /**
   * Key id, value brokerInfo to the map and protomap
   *
   * @param topic      String msg topic
   * @param memberList Member account list
   */
  public synchronized void addRfBrokerList(String topic, List<MemberAccount> memberList) {
    LOGGER.info("Adding topic to replication map: " + topic);
    membershipTable.getRfMap().computeIfAbsent(topic, v -> memberList);
    membershipTable.getReplicationMap()
        .computeIfAbsent(topic, v -> getRfBrokerInfoList(memberList));
  }

  /**
   * Set Replication Factor local map
   *
   * @param topic      Message topic
   * @param memberList MemberAccount list
   */
  public synchronized void setRfMap(String topic, List<MemberAccount> memberList) {
    if (!memberList.isEmpty()) {
      List<MemberAccount> memberAccounts = new CopyOnWriteArrayList<>(memberList);
      membershipTable.getRfMap().putIfAbsent(topic, memberAccounts);
      if (membershipTable.getRfMap().containsKey(topic)) {
        membershipTable.getRfMap().replace(topic, memberAccounts);
      }
    }
  }

  /**
   * Remove death node from rf map
   *
   * @param id failed broker id
   */
  public synchronized void removeBrokerRfMap(int id) {
    for (String topicKey : membershipTable.getRfMap().keySet()) {
      membershipTable.getRfMap().get(topicKey)
          .removeIf(memberAccount -> memberAccount.getBrokerId() == id);
    }
    logRf();
  }

  /**
   * Reset replication map to remove death node
   */
  public synchronized void resetReplicationMap() {
    BrokerList brokerList;
    for (String topicKey : membershipTable.getRfMap().keySet()) {
      brokerList = getRfBrokerInfoList(membershipTable.getRfMap().get(topicKey));
      membershipTable.getReplicationMap().replace(topicKey, brokerList);
    }
  }

  /**
   * Call remove rf map and replication map
   *
   * @param id failed broker id
   */
  public synchronized void removeBrokerReplicationMap(int id) {
    LOGGER.info("Removing from replication map ID : " + id);

    for (String topicKey : membershipTable.getReplicationMap().keySet()) {
      LOGGER.info("Deleting from: " + topicKey);
      BrokerList brokerList = membershipTable.getReplicationMap().get(topicKey);
      for (int i = 0; i < brokerList.getBrokerInfoCount(); i++) {
        BrokerInfo brokerInfo = brokerList.getBrokerInfo(i);
        if (brokerInfo.getId() == id) {
          LOGGER.info("Deleting: " + brokerInfo.getId());
          membershipTable.setFollowerFail(true);
          break;
        }
      }
      if(membershipTable.isFollowerFail()) {
        break;
      }
    }
    removeBrokerRfMap(id);
    resetReplicationMap();
    LOGGER.info(membershipTable.getReplicationMap().toString());
  }

  /**
   * Selecting random broker up to RF
   *
   * @return rf member account list
   */
  public synchronized List<MemberAccount> getRfBrokerMemberAccountList() {
    List<MemberAccount> brokerAccountList = new ArrayList<>();
    for (Integer brokerId : membershipTable.getMembershipMap().keySet()) {
      if (membershipTable.getMembershipMap().get(brokerId).isLeader() ||
          membershipTable.getMembershipMap().get(brokerId).getBrokerId() == brokerId) {
        continue;
      }
      brokerAccountList.add(membershipTable.getMembershipMap().get(brokerId));
    }

    if (brokerAccountList.size() < Constant.RF + 1) {
      LOGGER.info("Membership table size: "
          + membershipTable.getMembershipMap().size());
      return brokerAccountList;
    }

    return ReplicationUtils.getRandomMemberAccounts(brokerAccountList);
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
      BrokerInfo rfBrokerInfo = membershipTable.getProtoInfo(brokerInfo.getBrokerId(), brokerInfo);
      rfBrokerList.add(rfBrokerInfo);
    }
    return BrokerList.newBuilder()
        .addAllBrokerInfo(rfBrokerList)
        .build();
  }

  /**
   * Get a new rf broker after failure
   *
   * @param idList list of remaining broker
   * @return new broker info
   */
  public synchronized BrokerInfo getNewRfBrokerInfo(List<Integer> idList) {
    LOGGER.info("Selecting new random broker list....");
    List<BrokerInfo> brokerList = new ArrayList<>();
    for (Integer brokerId : membershipTable.getProtoMap().keySet()) {
      BrokerInfo brokerInfo = membershipTable.getProtoMap().get(brokerId);
      if (brokerInfo.getIsLeader() ||
          membershipTable.isLeader(brokerInfo.getId()) ||
          membershipTable.getProtoMap().get(brokerInfo.getId()).getIsLeader()) {
        LOGGER.info("Skip leader from random follower list.");
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
    return brokerList.get(randomIndex);
  }

  /**
   * Update new rfMap
   *
   * @param topic      msg topic
   * @param brokerList protobuf BrokerList
   */
  private synchronized void updateNewRfMap(String topic, BrokerList brokerList) {
    BrokerInfo brokerInfo;
    MemberAccount memberAccount;
    List<MemberAccount> brokerAccountList
        = Collections.synchronizedList(new ArrayList<>());

    for (int i = 0; i < brokerList.getBrokerInfoCount(); i++) {
      brokerInfo = brokerList.getBrokerInfo(i);
      memberAccount = getMemberAccount(brokerInfo);
      brokerAccountList.add(memberAccount);
    }

    if (!brokerAccountList.isEmpty()) {
      setRfMap(topic, brokerAccountList);
    }
    logRf();
  }

  /**
   * Update the fail node from rf list to maintain rf
   *
   * @return newRfMemberMap
   */
  public synchronized Map<String, List<MemberAccount>> updateNewRfBrokerList() {
    LOGGER.info("Updating the fail node to maintain rf...");
    LOGGER.info(membershipTable.getReplicationMap().toString());
    List<BrokerInfo> rfBrokerList
        = Collections.synchronizedList(new ArrayList<>());
    List<Integer> existingIdList
        = Collections.synchronizedList(new ArrayList<>());
    List<MemberAccount> memberAccounts;
    BrokerList brokerListNew;
    Map<String, List<MemberAccount>> newRfMemberMap = new ConcurrentHashMap<>();

    for (String topic : membershipTable.getReplicationMap().keySet()) {
      LOGGER.info("Topic RF: " + topic);
      BrokerList brokerList = membershipTable.getReplicationMap().get(topic);
      memberAccounts = Collections.synchronizedList(new ArrayList<>());

      for (BrokerInfo newBrokerInfo : brokerList.getBrokerInfoList()) {
        if (newBrokerInfo.getIsLeader() ||
            membershipTable.isLeader(newBrokerInfo.getId()) ||
            membershipTable.getProtoMap().get(newBrokerInfo.getId()).getIsLeader()) {
          LOGGER.info("Skip leader from follower list.");
          continue;
        }
        rfBrokerList.add(newBrokerInfo);
        LOGGER.info("Existing broker: " + newBrokerInfo.getId());
        existingIdList.add(newBrokerInfo.getId());
      }

      if (membershipTable.getMembershipMap().size() < Constant.RF + 1) {
        LOGGER.info("Membership table size: "
            + membershipTable.getMembershipMap().size());
        memberAccounts = getRfBrokerMemberAccountList();
        brokerListNew = getRfBrokerInfoList(memberAccounts);
      } else {
        while (rfBrokerList.size() < Constant.RF) {
          LOGGER.info("New broker list size: " + rfBrokerList.size());
          BrokerInfo newRandomBroker = getNewRfBrokerInfo(existingIdList);
          rfBrokerList.add(newRandomBroker);
          existingIdList.add(newRandomBroker.getId());
          LOGGER.info("New random broker: " + newRandomBroker.getId());
          memberAccounts.add(getMemberAccount(newRandomBroker));
        }
        if (!memberAccounts.isEmpty()) {
          newRfMemberMap.putIfAbsent(topic, memberAccounts);
        }
        brokerListNew = BrokerList.newBuilder()
            .addAllBrokerInfo(rfBrokerList)
            .build();
      }
      membershipTable.getReplicationMap().replace(topic, brokerListNew);
      rfBrokerList.clear();
      existingIdList.clear();
      updateNewRfMap(topic, brokerListNew);
    }
    membershipTable.setFollowerFail(false);
    LOGGER.info(membershipTable.getReplicationMap().toString());
    return newRfMemberMap;
  }

  /**
   * Update rf Map after election
   *
   * @return newRfMemberMap
   */
  public synchronized void leaderUpdateRfMap() {
    LOGGER.info("Updating rf map...");
    LOGGER.info(membershipTable.getReplicationMap().toString());
    for (String topic : membershipTable.getReplicationMap().keySet()) {
      LOGGER.info("Topic RF: " + topic);
      updateNewRfMap(topic, membershipTable.getReplicationMap().get(topic));
    }
    logRf();
  }

  /**
   * A helper class to get a member account instance
   * from protobuf BrokerInfo
   *
   * @param brokerInfo BrokerInfo protobuf
   * @return memberAccount
   */
  private MemberAccount getMemberAccount(BrokerInfo brokerInfo) {
    MemberAccount memberAccount;
    memberAccount = new MemberAccount(brokerInfo.getHost(), brokerInfo.getPort(),
        brokerInfo.getIsLeader(), brokerInfo.getId(), brokerInfo.getLeaderPort());
    return memberAccount;
  }

  /**
   * Log rfMap
   */
  public synchronized void logRf() {
    for (String topicKey : membershipTable.getRfMap().keySet()) {
      LOGGER.info(topicKey + " | "
          + membershipTable.getRfMap().get(topicKey).toString());
    }
  }
}
