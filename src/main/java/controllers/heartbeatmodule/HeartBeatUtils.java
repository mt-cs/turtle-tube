package controllers.heartbeatmodule;

import controllers.membershipmodule.MembershipTable;
import model.Membership.MemberInfo;
import util.Constant;

/**
 * Heartbeat utility helper class
 */
public class HeartBeatUtils {

  /**
   * Get heartbeat member info
   *
   * @param brokerId        broker's ID
   * @param membershipTable membership table
   * @return MemberInfo
   */
  public static MemberInfo getHeartBeatMemberInfo(int brokerId,
      MembershipTable membershipTable) {
    return MemberInfo.newBuilder()
        .setTypeValue(Constant.BROKER_TYPE)
        .setIsAlive(true)
        .setState(Constant.ALIVE)
        .setId(brokerId)
        .setHost(membershipTable.get(brokerId).getHost())
        .setPort(membershipTable.get(brokerId).getPort())
        .setLeaderPort(membershipTable.get(brokerId).getLeaderBasedPort())
        .putAllMembershipTable(membershipTable.getProtoMap())
        .build();
  }

  /**
   * Get heartbeat member info
   *
   * @param brokerId        broker's ID
   * @param membershipTable membership table
   * @return MemberInfo
   */
  public static MemberInfo getHeartBeatRfInfo(int brokerId,
      MembershipTable membershipTable) {
    return MemberInfo.newBuilder()
        .setTypeValue(Constant.BROKER_TYPE)
        .setIsAlive(true)
        .setState(Constant.ALIVE)
        .setId(brokerId)
        .setHost(membershipTable.get(brokerId).getHost())
        .setPort(membershipTable.get(brokerId).getPort())
        .setLeaderPort(membershipTable.get(brokerId).getLeaderBasedPort())
        .putAllMembershipTable(membershipTable.getProtoMap())
        .putAllReplicationTable(membershipTable.getReplicationMap())
        .build();
  }
}
