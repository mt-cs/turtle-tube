package controllers.membershipmodule;

import static org.junit.jupiter.api.Assertions.*;

import controllers.faultinjector.FaultInjectorFactory;
import controllers.messagingframework.ConnectionHandler;
import model.Membership.MemberInfo;
import org.junit.jupiter.api.Test;
import util.Constant;

class MembershipUtilsTest {

  ConnectionHandler connectionHandler = new ConnectionHandler("localhost", 1237,
      new FaultInjectorFactory(0).getChaos());
  MembershipTable membershipTable = new MembershipTable(false);

  @Test
  void testAddSelfToMembershipTable() {
    MembershipUtils.addSelfToMembershipTable("localhost",
        1027, 1013, false, 20, membershipTable );
    assertFalse(membershipTable.notContainsMember(20));
  }

  @Test
  void testAddTargetToMembershipTable() {
    MembershipUtils.addTargetToMembershipTable("localhost:1234", 1,
        "localhost:1235", membershipTable, connectionHandler);
    assertEquals(membershipTable.get(1).getLeaderBasedLocation(), "localhost:1235");

  }

  @Test
  void testAddNewMemberToMembershipTable() {
    MemberInfo memberInfo = MemberInfo.newBuilder()
        .setHost("localhost")
        .setId(1203)
        .setState(Constant.CONNECT)
        .setLeaderPort(1900)
        .setTypeValue(1)
        .setVersion(200)
        .build();
    MembershipUtils.addToMembershipTable(connectionHandler, memberInfo, membershipTable);
    assertEquals(membershipTable.get(1203).getHost(), "localhost");
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