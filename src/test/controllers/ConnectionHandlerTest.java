package controllers;

import static org.junit.jupiter.api.Assertions.*;

import constant.TestConstants;
import controllers.faultinjector.FaultInjectorFactory;
import controllers.messagingframework.ConnectionHandler;
import controllers.messagingframework.Listener;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/**
 * Test Connection Manager class
 *
 * @author marisatania
 */
public class ConnectionHandlerTest {

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
   * Validates connection send
   */
  @Test
  public void testSendMsg() {
    FaultInjectorFactory faultInjectorFactory = new FaultInjectorFactory(0);
    ConnectionHandler connection = new ConnectionHandler(
        TestConstants.localHost,
        TestConstants.port, faultInjectorFactory.getChaos());
    assertTrue(connection.send((String.format("%d: %s\n", 1, TestConstants.testHost))
        .getBytes(StandardCharsets.UTF_8)));
  }

  /**
   * Validates connection receive
   */
  @Test
  public void testReceiveMsg() throws InterruptedException, IOException {
    FaultInjectorFactory faultInjectorFactory = new FaultInjectorFactory(0);
    ServerSocket serverSocket = new ServerSocket(TestConstants.port);
    Thread listenerThread = new Thread( () -> {
      try {
        Socket socket = serverSocket.accept();
        ConnectionHandler connection
            = new ConnectionHandler(socket, faultInjectorFactory.getChaos());
        byte[] byteArr = TestConstants.testHost.getBytes(StandardCharsets.UTF_8);
        connection.send(byteArr);
      } catch (IOException e) {
        e.printStackTrace();
      }
    });
    listenerThread.start();
    ConnectionHandler connectionMng = new ConnectionHandler(
        TestConstants.localHost,
        TestConstants.port, faultInjectorFactory.getChaos());
    listenerThread.join();
    assertEquals(TestConstants.testHost,
        new String(connectionMng.receive(), StandardCharsets.UTF_8));
  }
}