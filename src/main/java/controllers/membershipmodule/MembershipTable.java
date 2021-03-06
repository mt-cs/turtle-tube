package controllers.membershipmodule;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;
import model.MemberAccount;
import model.Membership;
import model.Membership.BrokerList;

/**
 * Class for membership table instance
 *
 * @author marisatania
 */
public class MembershipTable implements Iterable<Map.Entry<Integer, MemberAccount>> {
  private final ConcurrentMap<Integer, MemberAccount> membershipMap;
  private final ConcurrentMap<Integer, Membership.BrokerInfo> protoMap;
  private ConcurrentHashMap<String, List<MemberAccount>> rfMap;
  private ConcurrentMap<String, BrokerList> replicationMap;
  private volatile boolean isFailure;
  private volatile boolean isFollowerFail;
  private final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

  /**
   * Constructor
   */
  public MembershipTable(boolean isRf) {
    this.membershipMap = new ConcurrentHashMap<>();
    this.protoMap = new ConcurrentHashMap<>();
    if (isRf) {
      this.replicationMap = new ConcurrentHashMap<>();
      this.rfMap = new ConcurrentHashMap<>();
    }
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
   * Remove member from map
   *
   * @param id broker ID
   */
  public void remove(int id) {
    membershipMap.remove(id);
    protoMap.remove(id);
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

  /**
   * Get is follower fail
   *
   * @return isFollowerFail
   */
  public boolean isFollowerFail() {
    return isFollowerFail;
  }

  /**
   * Set is follower fail
   *
   * @param followerFail true if failure
   */
  public void setFollowerFail(boolean followerFail) {
    isFollowerFail = followerFail;
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
