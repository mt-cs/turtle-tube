package controllers.failuredetector;

import controllers.electionmodule.BullyElectionManager;
import controllers.membershipmodule.MembershipTable;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import model.HeartBeatInfo;
import util.Constant;

/**
 * Heartbeat failure detector class using a timeout system
 * Reference: https://martinfowler.com/articles/patterns-of-distributed-systems/heartbeat.html
 *
 * @author marisatania
 */
public class FailureDetector {
  private final Map<Integer, HeartBeatInfo> heartBeatInfoMap;
  private final MembershipTable membershipTable;
  private BullyElectionManager bullyElection;
  private final long heartBeatTimeout;
  private final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

  /**
   * Constructor for failure detector
   *
   * @param heartbeatReceivedTimes map of heartbeat received time
   * @param membershipTable        membership table
   * @param bullyElection          bully election
   * @param heartBeatTimeout       heartbeat timout time
   */
  public FailureDetector(Map<Integer, HeartBeatInfo> heartbeatReceivedTimes,
      MembershipTable membershipTable, BullyElectionManager bullyElection,
      Long heartBeatTimeout) {
    this.heartBeatInfoMap = heartbeatReceivedTimes;
    this.heartBeatTimeout = heartBeatTimeout;
    this.membershipTable = membershipTable;
    this.bullyElection = bullyElection;
  }

  /**
   * Perform heartbeat check
   */
  public void checkHeartBeat() {
    Long now = System.nanoTime();
    Set<Integer> brokerIds = heartBeatInfoMap.keySet();

    for (Integer brokerId : brokerIds) {
      HeartBeatInfo heartBeatInfo = heartBeatInfoMap.get(brokerId);
      Long lastHeartbeatReceivedTime = heartBeatInfo.getHeartBeatReceivedTime();
      long timeSinceLastHeartbeat = now - lastHeartbeatReceivedTime;

      if (timeSinceLastHeartbeat >= heartBeatTimeout) {
        int currMissedHeartBeat = heartBeatInfo.getMissedHeartbeat();
        heartBeatInfo.setMissedHeartbeat(currMissedHeartBeat + 1);
        LOGGER.warning(heartBeatInfo.getMissedHeartbeat()
            + " missed heartbeat for broker: " + brokerId);
        if (heartBeatInfo.getMissedHeartbeat() == Constant.MAX_BOUND) {
          boolean isLeaderFailed = membershipTable.isLeader(brokerId);
          removeFailedBroker(brokerId);
          if (isLeaderFailed) {
            LOGGER.warning("Detected failed leader: " + brokerId);
            bullyElection.start();
          }
        }
        break;
      }
      LOGGER.info("heartbeat checked from: " + brokerId);
      membershipTable.setFailure(false);
    }
  }


  /**
   * A helper class to delete death node
   *
   * @param brokerId failed-broker's ID
   */
  private void removeFailedBroker(int brokerId) {
    membershipTable.setFailure(true);
    membershipTable.remove(brokerId);
    heartBeatInfoMap.remove(brokerId);
    LOGGER.warning("Broker " + brokerId + " has failed!");
    LOGGER.info(membershipTable.toString());
  }

  /**
   * Public wrapper for testing
   *
   * @param brokerId failed-broker's ID
   */
  public void removeFailedBrokerPublic(int brokerId) {
    removeFailedBroker(brokerId);
  }
}