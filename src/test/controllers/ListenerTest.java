package controllers;

import static org.junit.jupiter.api.Assertions.*;

import constant.TestConstants;
import controllers.faultinjector.FaultInjectorFactory;
import controllers.messagingframework.Listener;
import java.io.IOException;
import java.net.Socket;
import org.junit.jupiter.api.Test;

/**
 * Test listener class
 *
 * @author marisatania
 */
class ListenerTest {

  /**
   * Run listener server socket
   */
  @Test
  public void runListener() {
    FaultInjectorFactory faultInjectorFactory = new FaultInjectorFactory(0);
    Listener listener = new Listener(TestConstants.port, faultInjectorFactory.getChaos());
    listener.nextConnection();
  }

  /**
   * Validate listener accepting client
   */
  @Test
  public void testListenerAcceptsConnection() {
    try(Socket initiator = new Socket("localhost", TestConstants.port)) {
      assertTrue(initiator.isConnected());
    } catch (IOException e) {
      System.out.println(e.getMessage());
    }
  }

  /**
   * Validate listener refuse connection
   */
  @Test
  public void testListenerClosed() {
    try {
      new Socket("localhost", TestConstants.port);
      fail("Cannot connect if listener is closed");
    } catch (Exception e) {
      assertEquals("Connection refused", e.getMessage().trim());
    }
  }
}