package controllers.membershipmodule;

import static org.junit.jupiter.api.Assertions.*;

import controllers.faultinjector.FaultInjectorFactory;
import controllers.messagingframework.ConnectionHandler;
import model.MemberAccount;
import model.Membership;
import org.junit.jupiter.api.Test;

class MembershipManagerTest {
  MembershipTable membershipTable = new MembershipTable(false);
  MemberAccount memberInfo = new MemberAccount("localhost", 1023,
    new ConnectionHandler("localhost", 1237,
        new FaultInjectorFactory(0).getChaos()), false,5, 1088);


  @Test
  void testConvertToProtoInfo() {
    membershipTable.add(memberInfo.getBrokerId(), memberInfo);
    Membership.BrokerInfo protoInfo = membershipTable.getProtoInfo(memberInfo.getBrokerId(), memberInfo);
    assertEquals(protoInfo.getPort(),1023);
  }

  @Test
  void testUpdateBrokerVersion() {
    membershipTable.add(memberInfo.getBrokerId(), memberInfo);
    membershipTable.updateBrokerVersion(memberInfo.getBrokerId(), 10);
    assertEquals(membershipTable.get(memberInfo.getBrokerId()).getVersion(), 10);
  }

  @Test
  void testAddMember() {
    membershipTable.add(memberInfo.getBrokerId(), memberInfo);
    assertFalse(membershipTable.notContainsMember(5));
  }

  @Test
  void testRemoval() {
    membershipTable.remove(5);
    assertTrue(membershipTable.notContainsMember(5));
  }

  @Test
  void testUpdateMembershipTable() {
    assertFalse(membershipTable.getFailure());
  }

  @Test
  void testMemberNotFound() {
    assertTrue(membershipTable.notContainsMember(200));
  }

  @Test
  void testMemberSize() {
    assertEquals(membershipTable.size(),0);
  }
}