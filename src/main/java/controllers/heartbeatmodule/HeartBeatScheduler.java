package controllers.heartbeatmodule;
import controllers.electionmodule.BullyElectionManager;
import controllers.failuredetector.FailureDetector;
import controllers.membershipmodule.MembershipTable;
import controllers.messagingframework.ConnectionHandler;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import model.HeartBeatInfo;
import model.Membership;
import model.Membership.MemberInfo;
import util.Constant;

/**
 * Heartbeat scheduler class, reference:
 * https://martinfowler.com/articles/patterns-of-distributed-systems/heartbeat.html
 *
 * @author marisatania
 */
public class HeartBeatScheduler {
  private final int brokerId;
  private final MembershipTable membershipTable;
  private final Long heartBeatInterval;
  private final FailureDetector failureDetector;
  private final Map<Integer, HeartBeatInfo> heartbeatInfoMap;
  private final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
  private final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

  /**
   * Constructor for Heartbeat scheduler
   *
   * @param brokerId          current broker's ID
   * @param membershipTable   current broker's membership table
   * @param heartBeatInterval interval of heartbeat
   * @param bullyElection     bully election manager
   */
  public HeartBeatScheduler(int brokerId, MembershipTable membershipTable,
      Long heartBeatInterval, BullyElectionManager bullyElection) {
    this.brokerId = brokerId;
    this.membershipTable = membershipTable;
    this.heartBeatInterval = heartBeatInterval;
    this.heartbeatInfoMap = new ConcurrentHashMap<>();
    this.failureDetector = new FailureDetector(heartbeatInfoMap, membershipTable,
        bullyElection, getHeartBeatTimeout(heartBeatInterval));
  }

  /**
   * Send and check heartbeat periodically
   */
  public void start() {
    Runnable task = () -> {
        sendHeartBeat();
        failureDetector.checkHeartBeat();
    };

    ScheduledFuture<?> scheduledTask;
    scheduledTask = executor.scheduleWithFixedDelay(task,
        heartBeatInterval, heartBeatInterval, TimeUnit.MILLISECONDS);
  }

  /**
   * Send heartbeat to other members in membership table
   */
  public void sendHeartBeat() {
    for (var member : membershipTable) {
//      LOGGER.info(membershipTable.toString());
      if (member.getValue().getBrokerId() == brokerId) {
        continue;
      }
//      LOGGER.info("Sending heartbeat to broker: "
//          + member.getValue().getBrokerId() + " | "
//          + member.getValue().getLeaderBasedLocation());

      ConnectionHandler connection = member.getValue().getLeaderBasedConnection();
      Membership.MemberInfo memberInfo = MemberInfo.newBuilder()
          .setTypeValue(1)
          .setIsAlive(true)
          .setState(Constant.ALIVE)
          .setId(brokerId)
          .setHost(membershipTable.get(brokerId).getHost())
          .setPort(membershipTable.get(brokerId).getPort())
          .setLeaderPort(membershipTable.get(brokerId).getLeaderBasedPort())
          .putAllMembershipTable(membershipTable.getProtoMap())
          .putAllReplicationTable(membershipTable.getReplicationMap())
          .build();
      connection.send(memberInfo.toByteArray());
//      LOGGER.info(membershipTable.getReplicationMap().toString());
    }
  }

  /**
   * Handle heartbeat request
   */
  public synchronized void handleHeartBeatRequest(int id) {
    heartbeatInfoMap.put(id, new HeartBeatInfo(System.nanoTime(), 0));
//    LOGGER.info("Receive heartbeat from broker " + id);
  }

  /**
   * A helper method to get a heartbeat timeout
   *
   * @param heartBeatInterval interval of heartbeat in ms
   * @return heartbeat timeout in second
   */
  private Long getHeartBeatTimeout(Long heartBeatInterval) {
    return (heartBeatInterval + Constant.RT) * Constant.SECOND;
  }

  /**
   * Wrapper class
   *
   * @param heartBeatInterval interval of heartbeat in ms
   * @return heartbeat timeout in second
   */
  public Long getHeartBeatTimeoutPublic(Long heartBeatInterval) {
    return getHeartBeatTimeout(heartBeatInterval);
  }
}