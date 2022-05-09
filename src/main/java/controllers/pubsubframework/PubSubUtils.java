package controllers.pubsubframework;

import controllers.messagingframework.ConnectionHandler;
import interfaces.FaultInjector;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;
import model.MsgInfo;
import model.MsgInfo.Message;
import util.Utils;

/**
 * PubSub Framework Util class
 *
 * @author marisatania
 */
public class PubSubUtils {
  private static final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

  /**
   * Create connection to broker
   * @param brokerLocation broker host:id
   * @return connection
   */
  public static ConnectionHandler connectToBroker(String brokerLocation, FaultInjector faultInjector) {
    String[] brokerInfo = Utils.getBrokerInfo(brokerLocation);
    if (brokerInfo.length != 2) {
      LOGGER.warning("Broker location is in incorrect format: " + brokerLocation);
    }
    String hostName = brokerInfo[0];
    int port = Integer.parseInt(brokerInfo[1]);

    return new ConnectionHandler(hostName, port, faultInjector);
  }

  /**
   * Get port of broker
   *
   * @param brokerLocation broker location
   * @return port
   */
  public static int getPort(String brokerLocation) {
    String[] brokerInfo = Utils.getBrokerInfo(brokerLocation);
    if (brokerInfo.length != 2) {
      LOGGER.warning("Broker location is in incorrect format: " + brokerLocation);
    }
    return Integer.parseInt(brokerInfo[1]);
  }

  /**
   * Get host of broker
   *
   * @param brokerLocation broker location
   * @return host
   */
  public static String getHost(String brokerLocation) {
    String[] brokerInfo = Utils.getBrokerInfo(brokerLocation);
    if (brokerInfo.length != 2) {
      LOGGER.warning("Broker location is in incorrect format: " + brokerLocation);
    }
    return brokerInfo[0];
  }

  /**
   * Prompt hit enter to send the next message
   */
  public static void promptEnterKey(){
    System.out.println("Hit \"ENTER\" to continue...");
    Scanner scanner = new Scanner(System.in);
    scanner.nextLine();
  }

  /**
   * Delay timer task
   *
   * @param task  TimerTask object
   * @param delay delay length
   */
  public static void timerTask (TimerTask task, long delay) {
    Timer timer = new Timer();
    timer.schedule(task, delay);
  }

  /**
   * Wait by calling thread sleep
   *
   * @param timeout wait length
   */
  public static void wait(int timeout) {
    try {
      Thread.sleep(timeout);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  /**
   * Get broker location
   *
   * @param host broker's host
   * @param port broker's port
   * @return broker location
   */
  public static String getBrokerLocation(String host, int port) {
    return host + ":" + port;
  }


  public static String getMsgTopicInfo(MsgInfo.Message msg) {
    return "\n--> Topic: " + msg.getTopic()
        + ". Data: " + msg.getData()
        + " added to broker's messages list";
  }

  /**
   * Log message info
   *
   * @param msgInfo Message object
   */
  public static void logMsgInfo(Message msgInfo) {
    LOGGER.info(msgInfo.getMsgId()+ " | Sent message topic: " + msgInfo.getTopic() +
        ". Size: " + msgInfo.getOffset() + ". Data: " + msgInfo.getData());
  }

  /**
   * Check if broker is the first leader
   *
   * @param brokerId broker Id
   * @return true if it's the first leader
   */
  public static boolean getIsFirstLeader(int brokerId) {
    return brokerId == 1;
  }
}
