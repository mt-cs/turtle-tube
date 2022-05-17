package util;

import static org.junit.jupiter.api.Assertions.*;
import constant.TestConstants;
import java.util.ArrayList;
import java.util.Arrays;
import model.MemberAccount;
import model.config.Config;
import org.junit.jupiter.api.Test;

/**
 * Test getting the right hosts list
 *
 * @author marisatania
 */
public class UtilsTest {
  private static ArrayList<MemberAccount> hosts;

  /**
   * Validates getHosts
   */
  @Test
  public void testGetHosts() {
    assertEquals(
        TestConstants.hostList,
        Arrays.toString(hosts.toArray())
    );
  }

  /**
   * Validates isInteger
   */
  @Test
  public void testIsInteger() {
    assertTrue(Utils.isInteger(String.valueOf(TestConstants.port)));
  }

  @Test
  public void getBrokerHostName() {
    String[] brokerInfo = Utils.getBrokerInfo("localhost:2022");
    assertEquals("localhost", brokerInfo[0]);
  }

  @Test
  public void getBrokerPort() {
    String[] brokerInfo = Utils.getBrokerInfo("localhost:2022");
    assertEquals("2022", brokerInfo[1]);
  }

  @Test
  public void getConfigId() {
    Config config = Utils.runConfig("configBroker.json");
    assertEquals("broker1", config.getId());
  }

  @Test
  public void getConfigHost() {
    Config config = Utils.runConfig("configBroker.json");
    assertEquals("localhost", config.getHost());
  }

  @Test
  public void getConfigType() {
    Config config = Utils.runConfig("configConsumer.json");
    assertEquals("consumer", config.getType());
  }

  @Test
  public void getConfigStartingPosition() {
    Config config = Utils.runConfig("configConsumer.json");
    assertEquals(20, config.getStartingPosition());
  }

}