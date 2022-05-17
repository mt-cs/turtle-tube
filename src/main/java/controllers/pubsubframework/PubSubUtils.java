package controllers.pubsubframework;

import com.google.protobuf.ByteString;
import controllers.messagingframework.ConnectionHandler;
import controllers.replicationmodule.ReplicationUtils;
import interfaces.FaultInjector;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;
import model.MsgInfo;
import model.MsgInfo.Message;
import util.Constant;
import util.ReplicationAppUtils;
import util.Utils;

/**
 * PubSub Framework Util class
 *
 * @author marisatania
 */
public class PubSubUtils {
  private static final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

  /**
   * Get the roundup offset from index
   *
   * @param offsetIndex     list of offset index
   * @param startingOffset  starting request
   * @return offset
   */
  public static int getClosestOffset(List<Integer> offsetIndex, int startingOffset) {
    int diff = Math.abs(offsetIndex.get(0) - startingOffset);
    int idx = 0;
    for (int i = 1; i < offsetIndex.size(); i++) {
      if (offsetIndex.get(i) > startingOffset) {
        break;
      }
      int curr = Math.abs(offsetIndex.get(i) - startingOffset);
      if (curr < diff) {
        idx = i;
        diff = curr;
      }
    }
    return offsetIndex.get(idx);
  }

  /**
   * Get the size of offset file
   *
   * @param filePath path of persistent storage
   * @return size
   */
  public static long getFileSize(Path filePath) {
    try {
      return Files.size(filePath);
    } catch (IOException e) {
      LOGGER.warning("Failed in getting file size: " + e.getMessage());
    }
    return 0;
  }

  /**
   * Flush consumer application to file
   *
   * @param data log data
   */
  public static void flushToFile(byte[] data) {
    if (data != null) {
      Path filePathSave = Path.of(ReplicationAppUtils.getOffsetFile());
      if (!Files.exists(filePathSave)) {
        try {
          Files.write(filePathSave, data);
        } catch (IOException e) {
          LOGGER.warning("Exception during consumer application write: " + e.getMessage());
        }
        LOGGER.info("Creating consumer application file path: " + filePathSave);
      } else {
        try {
          Files.write(filePathSave, data, StandardOpenOption.APPEND);
        } catch (IOException e) {
          LOGGER.warning("Consumer app file write exception: " + e.getMessage());
        }
      }
    }
  }

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
   * Get snapshot msg
   * @param id         msg ID
   * @param offset     msg offset
   * @param data       protobuf data
   * @param log        string log
   * @param host       broker host
   * @param port       broker port
   * @param brokerId   broker ID
   * @return snapshot protobuf
   */
  public static Message getSnapshotMsg(int id, int offset, byte[] data,
                                      String log, String host, int port, int brokerId) {
    return Message.newBuilder()
        .setTypeValue(Constant.BROKER_TYPE)
        .setData(ByteString.copyFrom(data))
        .setOffset(offset)
        .setTopic(ReplicationUtils.getTopic(log))
        .setSrcId(PubSubUtils.getBrokerLocation(host, port))
        .setMsgId(id)
        .setId(brokerId)
        .setIsSnapshot(true)
        .build();
  }

  /**
   * Get last message
   *
   * @return close message
   */
  public static Message getLastMessage() {
    return Message.newBuilder()
        .setTypeValue(Constant.BROKER_TYPE)
        .setTopic(Constant.CLOSE)
        .build();
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
   * Delete file
   *
   * @param filePath fikePath
   */
  public static void deleteFilePath(Path filePath) {
    try {
      Files.delete(filePath);
    } catch (IOException ex) {
      ex.printStackTrace();
    }
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

  /**
   * Msg Topic Info to string
   *
   * @param msg MsgInfo.Message
   * @return string protobuf representation
   */
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

  /**
   * Get topic from offset file
   *
   * @param logFile topic offset file
   * @return topic
   */
  public static String getTopic(String logFile) {
    return logFile.substring(logFile.lastIndexOf('_') + 1, logFile.lastIndexOf('.'));
  }

}


