package controllers.electionmodule;

import controllers.membershipmodule.MembershipTable;
import controllers.membershipmodule.MembershipUtils;
import controllers.messagingframework.ConnectionHandler;
import controllers.pubsubframework.PubSubUtils;
import java.util.*;
import java.util.logging.Logger;
import model.Membership;
import model.Membership.MemberInfo;
import util.Constant;

/**
 * Implementing bully election algorithm
 *
 * @author marisatania
 */
public class BullyElectionManager {
  private final String pubSubHost;
  private final int pubSubPort;
  private final MembershipTable membershipTable;
  private final ConnectionHandler loadBalancerConnection;
  private final int currBrokerId;
  private volatile boolean isElecting;
  private volatile boolean isLeaderSelected;
  private volatile boolean isCandidateExist;
  private final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

  /**
   * Constructor for BullyElectionManager
   *
   * @param brokerId        broker's ID
   * @param membershipTable membership table instance
   */
  public BullyElectionManager(int brokerId, MembershipTable membershipTable,
      ConnectionHandler loadBalancerConnection) {
    this.currBrokerId = brokerId;
    this.membershipTable = membershipTable;
    this.loadBalancerConnection = loadBalancerConnection;
    this.isElecting = false;
    this.isLeaderSelected = false;
    this.isCandidateExist = false;
    this.pubSubHost = membershipTable.get(currBrokerId).getHost();
    this.pubSubPort = membershipTable.get(currBrokerId).getPort();
  }


  /**
   * Start bully election process
   */
  public synchronized void start() {
    if (!isElecting) {
      isElecting = true;
      LOGGER.info("Broker " + currBrokerId + " is starting new election...");
      sendElectionMsgToLowerIds();
    }
  }

  /**
   * Elect self if there is no candidate response
   */
  public void checkCandidateResponse() {
    LOGGER.info("Waiting for election response...");
    LOGGER.info("candidate exist: " + isCandidateExist);
    LOGGER.info("Leader selected: " + isLeaderSelected);
    TimerTask task = new TimerTask() {
      public void run() {
        if (!isCandidateExist && !isLeaderSelected) {
          LOGGER.warning("No candidate response. Declaring victory...");
          electSelf();
        }
      }
    };
    PubSubUtils.timerTask(task, 5000L);
  }

  /**
   * Elect self and inform other brokers
   */
  private void electSelf() {
    isLeaderSelected = true;
    isCandidateExist = false;
    LOGGER.info("Declare victory: " + currBrokerId);
    handleVictoryRequest(currBrokerId);
    LOGGER.info(membershipTable.toString());
    sendVictoryMsgToHigherIds();
    MembershipUtils.sendLeaderLocation(loadBalancerConnection,
        currBrokerId, pubSubHost, pubSubPort);
  }

  /**
   * Send election to all brokers with lowerIds
   */
  private void sendElectionMsgToLowerIds() {
    LOGGER.info("Sending election to brokers with lowerIDs...");
    isLeaderSelected = false;
    isCandidateExist = false;

    for (var broker : membershipTable) {
      if (broker.getKey() < currBrokerId) {
        ConnectionHandler connection = broker.getValue().getLeaderBasedConnection();

        if (!sendElectionStateMsg(connection, Constant.ELECTION)) {
          LOGGER.info("NOT SENT election msg to inactive broker: " + broker.getKey());
        } else {
          LOGGER.info("Election message sent to broker: " + broker.getKey());
        }
      }
    }
    checkCandidateResponse();
  }

  /**
   * Send victory to all brokers with higher Ids
   */
  private void sendVictoryMsgToHigherIds() {
    LOGGER.info("Sending victory to brokers with higher IDs...");
    LOGGER.info(membershipTable.toString());

    for (var broker : membershipTable) {
      if (broker.getKey() > currBrokerId) {
        ConnectionHandler connection = broker.getValue().getLeaderBasedConnection();
        if (!sendElectionStateMsg(connection, Constant.VICTORY)) {
          LOGGER.info("NOT SENT victory msg to inactive broker: " + broker.getKey());
        } else {
          LOGGER.info("SENT Victory to broker: " + broker.getKey());
        }
      }
    }
    isLeaderSelected = true;
    isElecting = false;
    isCandidateExist = false;
  }

  /**
   * Candidate response to election message request
   *
   * @param connection    leader based connection
   * @param inputBrokerId sender's broker ID
   */
  public void handleElectionRequest(ConnectionHandler connection, int inputBrokerId) {
    LOGGER.info("Input id: " + inputBrokerId + " currId: " + currBrokerId);
    if (currBrokerId < inputBrokerId) {
      LOGGER.info("Received election request from higherIds: " + inputBrokerId + " | currId: "
          + currBrokerId);
      sendElectionStateMsg(connection, Constant.CANDIDATE);
      sendElectionMsgToLowerIds();
    }
  }

  /**
   * Handling candidate response Restart election if timeout
   */
  public void handleCandidateRequest() {
    isCandidateExist = true;
    LOGGER.info("Handling candidate response: " + isCandidateExist);
    LOGGER.info("Leader selected: " + isLeaderSelected);
    TimerTask task = new TimerTask() {
      public void run() {
        if (!isLeaderSelected) {
          LOGGER.warning("No leader selected, restart election.");
          isElecting = false;
          start();
        }
      }
    };
    Timer timer = new Timer();
    long delay = 15000L;
    timer.schedule(task, delay);
  }


  /**
   * Handle victory election state
   *
   * @param newLeaderBrokerId leader's ID
   */
  public void handleVictoryRequest(int newLeaderBrokerId) {
    isLeaderSelected = true;
    LOGGER.info("New leader selected broker: " + newLeaderBrokerId);
    membershipTable.setLeader(newLeaderBrokerId, true);
    LOGGER.info(membershipTable.toString());
    isElecting = false;
    isCandidateExist = false;
  }

  private boolean sendElectionStateMsg(ConnectionHandler connection, String state) {

    Membership.MemberInfo electionMsg = MemberInfo.newBuilder()
        .setTypeValue(1)
        .setIsAlive(true)
        .setState(state)
        .setId(currBrokerId)
        .setHost(pubSubHost)
        .setPort(pubSubPort)
        .setLeaderPort(membershipTable.get(currBrokerId).getLeaderBasedPort())
        .putAllMembershipTable(membershipTable.getProtoMap())
        .build();
    LOGGER.info("Sending election state msg: " + state);
    LOGGER.info(membershipTable.toString());
    return connection.send(electionMsg.toByteArray());
  }

  /**
   * Set election state
   *
   * @param isElecting boolean if electing
   */
  public void setElecting(boolean isElecting) {
    this.isElecting = isElecting;
  }
}