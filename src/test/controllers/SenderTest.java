package controllers;

import static org.junit.jupiter.api.Assertions.*;

import constant.TestConstants;
import controllers.faultinjector.FaultInjectorFactory;
import controllers.messagingframework.ConnectionHandler;
import controllers.messagingframework.Listener;
import model.MsgInfo;
import org.junit.jupiter.api.Test;
import util.Constant;

/**
 * Test Sender class
 *
 * @author marisatania
 */
public class SenderTest {

  /**
   * Test sender thread
   */
  @Test
  public void testSender() {
    final ConnectionHandler[] connection = new ConnectionHandler[1];
    FaultInjectorFactory faultInjectorFactory = new FaultInjectorFactory(0);
    Thread listenerThread = new Thread( () -> {
      Listener listener = new Listener(TestConstants.port, faultInjectorFactory.getChaos());
      connection[0] = listener.nextConnection();
    });
    listenerThread.start();
    Thread serverSender = new Thread(() ->
        testInitiator(connection[0], Constant.listener + TestConstants.testHost));
    serverSender.start();
    assertNotNull(serverSender.getState());
  }

  /**
   * Test send protobuf encoding message
   *
   * @param connection ConnectionManager
   * @param topic      String topic
   */
  private void testInitiator(ConnectionHandler connection, String topic) {
    while (true) {
      MsgInfo.Message msgRequest = MsgInfo.Message.newBuilder()
          .setTypeValue(2)
          .setStartingPosition(1)
          .setTopic(topic)
          .build();
      connection.send(msgRequest.toByteArray());
    }
  }
}