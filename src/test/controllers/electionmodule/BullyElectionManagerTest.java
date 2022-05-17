package controllers.electionmodule;

import static org.junit.jupiter.api.Assertions.*;

import controllers.faultinjector.FaultInjectorFactory;
import controllers.membershipmodule.MembershipTable;
import controllers.messagingframework.ConnectionHandler;
import model.MemberAccount;
import org.junit.jupiter.api.Test;

class BullyElectionManagerTest {

  @Test
  public void testHandleVictoryRequest() {
    MembershipTable membershipTable = new MembershipTable(false);
    MemberAccount memberAccount = new MemberAccount("Localhost", 1050, false, 1, 1069);
    membershipTable.add(1, memberAccount);
    System.out.println(membershipTable);

    BullyElectionManager bullyElectionManager = new BullyElectionManager(1, membershipTable,
        new ConnectionHandler("Localhost", 1500, new FaultInjectorFactory(0).getChaos()));
    bullyElectionManager.handleVictoryRequest(1);

    assertTrue(membershipTable.isLeader(1));
  }

  @Test
  public void testElecting() {

    MembershipTable membershipTable = new MembershipTable(false);
    MemberAccount memberAccount = new MemberAccount("Localhost", 1054, false, 1, 1069);
    membershipTable.add(2, memberAccount);
    System.out.println(membershipTable);

    BullyElectionManager bullyElectionManager = new BullyElectionManager(2, membershipTable,
        new ConnectionHandler("Localhost", 1500, new FaultInjectorFactory(0).getChaos()));
    bullyElectionManager.setElecting(false);
  }

}