package controllers.heartbeatmodule;
import controllers.electionmodule.BullyElectionManager;
import controllers.failuredetector.FailureDetector;
import controllers.membershipmodule.MembershipTable;
import controllers.messagingframework.ConnectionHandler;
import controllers.replicationmodule.ReplicationFactor;
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
  private final boolean isRf;
  private final MembershipTable membershipTable;
  private final Long heartBeatInterval;
  private final FailureDetector failureDetector;
  private final Map<Integer, HeartBeatInfo> heartbeatInfoMap;
  private final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);

  /**
   * Constructor for Heartbeat scheduler
   *
   * @param brokerId          current broker's ID
   * @param membershipTable   current broker's membership table
   * @param heartBeatInterval interval of heartbeat
   * @param bullyElection     bully election manager
   */
  public HeartBeatScheduler(int brokerId, MembershipTable membershipTable,
      Long heartBeatInterval, BullyElectionManager bullyElection,
      ReplicationFactor replicationFactor, boolean isRf) {
    this.brokerId = brokerId;
    this.isRf = isRf;
    this.membershipTable = membershipTable;
    this.heartBeatInterval = heartBeatInterval;
    this.heartbeatInfoMap = new ConcurrentHashMap<>();
    this.failureDetector = new FailureDetector(heartbeatInfoMap, membershipTable,
        bullyElection, getHeartBeatTimeout(heartBeatInterval), replicationFactor, isRf);
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
      if (member.getValue().getBrokerId() == brokerId) {
        continue;
      }
      ConnectionHandler connection = member.getValue().getLeaderBasedConnection();
      MemberInfo memberInfo;
      if (isRf) {
        memberInfo = HeartBeatUtils.getHeartBeatRfInfo(brokerId, membershipTable);
      } else {
        memberInfo = HeartBeatUtils.getHeartBeatMemberInfo(brokerId, membershipTable);
      }
      connection.send(memberInfo.toByteArray());
    }
  }

  /**
   * Handle heartbeat request
   */
  public synchronized void handleHeartBeatRequest(int id) {
    heartbeatInfoMap.put(id, new HeartBeatInfo(System.nanoTime(), 0));
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