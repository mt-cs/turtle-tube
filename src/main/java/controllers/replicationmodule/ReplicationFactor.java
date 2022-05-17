package controllers.replicationmodule;

import controllers.membershipmodule.MembershipTable;
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
import model.Membership.BrokerInfo;
import model.Membership.BrokerList;
import util.Constant;

/**
 * Class for membership table instance
 *
 * @author marisatania
 */
public class ReplicationFactor {
  private MembershipTable membershipTable = new MembershipTable();
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
    membershipTable.getReplicationMap().computeIfAbsent(topic, v -> getRfBrokerInfoList(memberList));
  }

  /**
   * Remove member from map
   *
   * @param id broker ID
   */
  public void removeRfMaps(int id) {
    removeBrokerReplicationMap(id);
  }

  public synchronized void setRfMap(String topic, List<MemberAccount> memberList) {
    if (!memberList.isEmpty()) {
        List<MemberAccount> memberAccounts = new CopyOnWriteArrayList<>(memberList);
//        LOGGER.info(memberAccounts.toString());
        membershipTable.getRfMap().putIfAbsent(topic, memberAccounts);
//        rfToString();
        if (membershipTable.getRfMap().containsKey(topic)) {
          membershipTable.getRfMap().replace(topic, memberAccounts);
//          rfToString();
        }
    }
  }

  public synchronized void rfToString() {
    for (String topicKey : membershipTable.getRfMap().keySet()) {
      LOGGER.info(topicKey + " | " + membershipTable.getRfMap().get(topicKey).toString());
    }
  }

  public synchronized void removeBrokerRfMap(int id) {
    for (String topicKey : membershipTable.getRfMap().keySet()) {
      membershipTable.getRfMap().get(topicKey).removeIf(memberAccount -> memberAccount.getBrokerId() == id);
    }
    rfToString();
  }

  public synchronized void resetReplicationMap() {
    BrokerList brokerList;
    for (String topicKey : membershipTable.getRfMap().keySet()) {
      brokerList = getRfBrokerInfoList(membershipTable.getRfMap().get(topicKey));
      membershipTable.getReplicationMap().replace(topicKey, brokerList);
    }

  }

  public synchronized void removeBrokerReplicationMap(int id) {
    LOGGER.info("Removing from replication map ID : " + id);

    for (String topicKey : membershipTable.getReplicationMap().keySet()) {
      LOGGER.info(" DELETING FROM: " + topicKey);
      BrokerList brokerList = membershipTable.getReplicationMap().get(topicKey);
      for (int i = 0; i < brokerList.getBrokerInfoCount(); i++) {
        BrokerInfo brokerInfo = brokerList.getBrokerInfo(i);
        LOGGER.info("CHECKING FOR: " + brokerInfo.getId() + " | " + brokerInfo.getPort());
        if (brokerInfo.getId() == id) {
          LOGGER.info("DELETING: " + brokerInfo.getId());
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
      LOGGER.info("Membership table size: " + membershipTable.getMembershipMap().size());
      return brokerAccountList;
    }

    List<MemberAccount> rfBrokerList = Collections.synchronizedList(new ArrayList<>());

    // select random broker up to RF
    int randomIndex;
    MemberAccount randomAccount;
    for (int i = 0; i < Constant.RF; i++) {
      randomIndex = new Random().nextInt(brokerAccountList.size());
      randomAccount = brokerAccountList.get(randomIndex);
      brokerAccountList.remove(randomIndex);
      rfBrokerList.add(randomAccount);
    }
    LOGGER.info(rfBrokerList.toString());
    return rfBrokerList;
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
//      LOGGER.info(rfBrokerInfo.toString());
    }
    BrokerList brokerList = BrokerList.newBuilder()
        .addAllBrokerInfo(rfBrokerList)
        .build();
//    LOGGER.info(brokerList.toString());
    return brokerList;
  }

  public synchronized Map<String, List<MemberAccount>> updateNewRfBrokerList() {
    LOGGER.info("Updating the fail node to maintain rf...");
    LOGGER.info(membershipTable.getReplicationMap().toString());
    List<BrokerInfo> rfBrokerList = Collections.synchronizedList(new ArrayList<>());
    List<Integer> existingIdList = Collections.synchronizedList(new ArrayList<>());
    List<MemberAccount> memberAccounts;
    BrokerList brokerListNew;
    Map<String, List<MemberAccount>> newRfMemberMap = new ConcurrentHashMap<>();
    for (String topic : membershipTable.getReplicationMap().keySet()) {
      LOGGER.info("TOPIC RF: " + topic);
      BrokerList brokerList = membershipTable.getReplicationMap().get(topic);
      memberAccounts = Collections.synchronizedList(new ArrayList<>());
      for (BrokerInfo newBrokerInfo : brokerList.getBrokerInfoList()) {
        rfBrokerList.add(newBrokerInfo);
        LOGGER.info("Existing broker: " + newBrokerInfo.getId());
        existingIdList.add(newBrokerInfo.getId());
      }
      if (membershipTable.getMembershipMap().size() < Constant.RF + 1) {
        LOGGER.info("Membership table size: " + membershipTable.getMembershipMap().size());
        memberAccounts = getRfBrokerMemberAccountList();
        brokerListNew = getRfBrokerInfoList(memberAccounts);
      } else {
        while (rfBrokerList.size() < Constant.RF) {
          LOGGER.info("New broker list size: " + rfBrokerList.size());
          BrokerInfo newRandomBroker = getNewRfBrokerList(existingIdList);
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

  public synchronized BrokerInfo getNewRfBrokerList(List<Integer> idList) {
    LOGGER.info("Selecting new random broker list....");

    List<BrokerInfo> brokerList = new ArrayList<>();
    for (Integer brokerId : membershipTable.getProtoMap().keySet()) {
      BrokerInfo brokerInfo = membershipTable.getProtoMap().get(brokerId);
      if (brokerInfo.getIsLeader()) {
        continue;
      }
      if (idList.size() == 0) {
        LOGGER.info("No existing broker found...");
        brokerList.add(brokerInfo);
      } else {
        for (Integer id : idList) {
          // LOGGER.info("Existing ID: " + id);
          if (brokerInfo.getId() != id) {
            brokerList.add(brokerInfo);
            // LOGGER.info("Added to broker list: " + brokerInfo.getId());
          }
        }
      }
    }
    int randomIndex = new Random().nextInt(brokerList.size());
    // LOGGER.info("New rf broker: " + brokerList.get(randomIndex).getId());
    return brokerList.get(randomIndex);
  }

  private synchronized void updateNewRfMap(String topic, BrokerList brokerList) {
    BrokerInfo brokerInfo;
    MemberAccount memberAccount;
    List<MemberAccount> brokerAccountList = Collections.synchronizedList(new ArrayList<>());

    for (int i = 0; i < brokerList.getBrokerInfoCount(); i++) {
      brokerInfo = brokerList.getBrokerInfo(i);
      memberAccount = getMemberAccount(brokerInfo);
      brokerAccountList.add(memberAccount);
    }

    if (!brokerAccountList.isEmpty()) {
      setRfMap(topic, brokerAccountList);
    }
    rfToString();
  }

  private MemberAccount getMemberAccount(BrokerInfo brokerInfo) {
    MemberAccount memberAccount;
    memberAccount = new MemberAccount(brokerInfo.getHost(), brokerInfo.getPort(),
        brokerInfo.getIsLeader(), brokerInfo.getId(), brokerInfo.getLeaderPort());
    return memberAccount;
  }
}
