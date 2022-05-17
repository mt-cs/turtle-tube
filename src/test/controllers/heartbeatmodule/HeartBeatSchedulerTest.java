package controllers.heartbeatmodule;

import static org.junit.jupiter.api.Assertions.*;

import controllers.electionmodule.BullyElectionManager;
import controllers.faultinjector.FaultInjectorFactory;
import controllers.membershipmodule.MembershipTable;
import controllers.messagingframework.ConnectionHandler;
import controllers.replicationmodule.ReplicationFactor;
import model.MemberAccount;
import org.junit.jupiter.api.Test;

/**
 * Test heartbeat scheduler
 *
 * @author marisatania
 */
class HeartBeatSchedulerTest {

  /**
   * Test handle heartbeat request
   */
  @Test
  public void testHandleHeartBeatRequest() {

    MembershipTable membershipTable = new MembershipTable(false);
    MemberAccount memberAccount = new MemberAccount("Localhost", 1050, false, 1, 1069);
    membershipTable.add(1, memberAccount);
    System.out.println(membershipTable);

    BullyElectionManager bullyElectionManager = new BullyElectionManager(1, membershipTable,
        new ConnectionHandler("Localhost", 1500, new FaultInjectorFactory(0).getChaos()));

    ReplicationFactor replicationFactor = new ReplicationFactor(membershipTable);

    HeartBeatScheduler heartBeatScheduler =
        new HeartBeatScheduler(1, membershipTable, 10000L, bullyElectionManager, replicationFactor, false);
    heartBeatScheduler.handleHeartBeatRequest(1);
  }

  /**
   * Test get Heattbeat timenout
   */
  @Test
  public void testGetHeartBeatTimeout() {
    MembershipTable membershipTable = new MembershipTable(false);
    MemberAccount memberAccount = new MemberAccount("Localhost", 1050, false, 1, 1069);
    membershipTable.add(1, memberAccount);
    System.out.println(membershipTable);

    BullyElectionManager bullyElectionManager = new BullyElectionManager(1, membershipTable,
        new ConnectionHandler("Localhost", 1500, new FaultInjectorFactory(0).getChaos()));

    ReplicationFactor replicationFactor = new ReplicationFactor(membershipTable);

    HeartBeatScheduler heartBeatScheduler =
        new HeartBeatScheduler(1, membershipTable, 10000L, bullyElectionManager, replicationFactor, false);
    assertEquals(heartBeatScheduler.getHeartBeatTimeoutPublic(20000L), 23000000000L);
  }

}