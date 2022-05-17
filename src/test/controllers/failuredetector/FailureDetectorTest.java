package controllers.failuredetector;

import static org.junit.jupiter.api.Assertions.assertTrue;

import controllers.electionmodule.BullyElectionManager;
import controllers.faultinjector.FaultInjectorFactory;
import controllers.membershipmodule.MembershipTable;
import controllers.messagingframework.ConnectionHandler;
import controllers.replicationmodule.ReplicationFactor;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import model.HeartBeatInfo;
import model.MemberAccount;
import org.junit.jupiter.api.Test;

class FailureDetectorTest {

  @Test
  public void testRemoveFailedBroker() {
    MembershipTable membershipTable = new MembershipTable(false);
    MemberAccount memberAccount = new MemberAccount("Localhost", 1050, false, 1, 1069);
    membershipTable.add(1, memberAccount);
    System.out.println(membershipTable);

    Map<Integer, HeartBeatInfo> heartBeatInfoMap = new ConcurrentHashMap<>();
    heartBeatInfoMap.putIfAbsent(1, new HeartBeatInfo(2000L, 1));

    BullyElectionManager bullyElectionManager =
        new BullyElectionManager(1, membershipTable,
        new ConnectionHandler("Localhost", 1500,
            new FaultInjectorFactory(0).getChaos()));

    ReplicationFactor replicationFactor = new ReplicationFactor(membershipTable);
    FailureDetector failureDetector = new FailureDetector(heartBeatInfoMap,
        membershipTable, bullyElectionManager, 20000L, replicationFactor, false);
    failureDetector.removeFailedBrokerPublic(1);

    assertTrue(membershipTable.size() == 0);
  }


}