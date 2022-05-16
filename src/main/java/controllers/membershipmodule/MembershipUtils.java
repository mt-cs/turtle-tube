package controllers.membershipmodule;

import controllers.messagingframework.ConnectionHandler;
import controllers.pubsubframework.PubSubUtils;
import java.util.logging.Logger;
import model.MemberAccount;
import model.Membership.MemberInfo;

/**
 * Membership Module Util class
 *
 * @author marisatania
 */
public class MembershipUtils {
  private static final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

  /**
   * A helper method to add self to the membership table
   *
   * @param host            broker's host name
   * @param leaderBasedPort broker's leader based port
   * @param isLeader        broker's leader status
   * @param brokerId        broker's ID
   * @param membershipTable membership table
   */
  public static void addSelfToMembershipTable(String host, int port, int leaderBasedPort,
      boolean isLeader, int brokerId,
      MembershipTable membershipTable) {
    MemberAccount brokerInfo = new MemberAccount(host, port, isLeader, brokerId, leaderBasedPort);
    membershipTable.add(brokerId, brokerInfo);
    LOGGER.info("Added self to membership table: " + brokerInfo.getLeaderBasedLocation());
    LOGGER.info(membershipTable.toString());
  }

  /**
   * Add target to the membership table
   *
   * @param targetBrokerLocation        pubsub host:port
   * @param targetLeaderBasedLocation   leader based host:port
   * @param targetId                    target broker's ID
   * @param membershipTable             membership table
   * @param connectionToPeer            connection to target
   */
  public static void addTargetToMembershipTable(String targetBrokerLocation, int targetId,
      String targetLeaderBasedLocation,
      MembershipTable membershipTable,
      ConnectionHandler connectionToPeer) {
    boolean targetIsLeader = PubSubUtils.getIsFirstLeader(targetId);
    MemberAccount brokerInfo = new MemberAccount(PubSubUtils.getHost(targetBrokerLocation),
        PubSubUtils.getPort(targetBrokerLocation),
        connectionToPeer, targetIsLeader, targetId,
        PubSubUtils.getPort(targetLeaderBasedLocation));

    membershipTable.add(targetId, brokerInfo);
    LOGGER.info("Added target to membership table: " + brokerInfo.getLeaderBasedLocation());
    LOGGER.info(membershipTable.toString());
  }

  /**
   * Add new member to membership table
   *
   * @param connection leader based connection
   * @param memberInfo Member info protobuf instance
   */
  public static void addToMembershipTable(ConnectionHandler connection,
      MemberInfo memberInfo,
      MembershipTable membershipTable) {
    MemberAccount brokerInfo = new MemberAccount(memberInfo.getHost(),
        memberInfo.getPort(), connection,
        memberInfo.getIsLeader(), memberInfo.getId(),
        memberInfo.getLeaderPort());
    membershipTable.add(memberInfo.getId(), brokerInfo);
    LOGGER.info("Added to membership table: " + brokerInfo.getLeaderBasedLocation());
  }

  /**
   * Helper method to send currentLeader address
   */
  public static void sendLeaderLocation(ConnectionHandler connection, int leaderId,
      String leaderHost, int leaderPort) {
    MemberInfo leaderInfo = MemberInfo.newBuilder()
        .setTypeValue(1)
        .setHost(leaderHost)
        .setPort(leaderPort)
        .setId(leaderId)
        .build();
    LOGGER.info("Sent leader info: " +
        PubSubUtils.getBrokerLocation(leaderHost, leaderPort));

    connection.send(leaderInfo.toByteArray());
  }

  /**
   * Helper method to send broker address
   */
  public static void sendBrokerLocation(ConnectionHandler connection, int brokerId,
      String brokerHost, int brokerPort, boolean isLeader) {
    MemberInfo brokerInfo = MemberInfo.newBuilder()
        .setTypeValue(1)
        .setHost(brokerHost)
        .setPort(brokerPort)
        .setId(brokerId)
        .setIsLeader(isLeader)
        .build();
    LOGGER.info("Sent broker info: " +
        PubSubUtils.getBrokerLocation(brokerHost, brokerPort));

    connection.send(brokerInfo.toByteArray());
  }

  /**
   * Update pubsub connection in table
   *
   * @param membershipTable membership table
   * @param brokerLocation  broker host and port
   * @param connection      connection pubsub
   */
  public static void updatePubSubConnection(MembershipTable membershipTable,
      String brokerLocation, ConnectionHandler connection) {
    for (var member : membershipTable) {
      if (brokerLocation.equals(member.getValue().getPubSubLocation())) {
        member.getValue().setPubSubConnection(connection);
        LOGGER.info("Updated pubsub connection for broker: " + brokerLocation);
        return;
      }
    }
  }
}
