package controllers.pubsubframework;

import controllers.messagingframework.ConnectionHandler;
import interfaces.FaultInjector;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.List;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;
import model.MsgInfo;
import model.MsgInfo.Message;
import org.apache.commons.codec.digest.DigestUtils;
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

  public static long getFileSize(String filePath) {
    return new File(filePath).length();
  }

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
   * Get the amount of byte to skip
   * @param offsetIndex     index offset
   * @param startingOffset  starting offset
   *
   * @return number of byte to skip
   */
  public static int getByteToSkip(List<Integer> offsetIndex, int startingOffset) {
    if (startingOffset == 0) {
      return 0;
    }
    int byteToSkip = 0;
    for (int offset : offsetIndex) {
      byteToSkip += offset;
      if (offset == startingOffset) {
        break;
      }
    }
    return byteToSkip - 1;
  }

  public static byte[] getHashBytes(String key) {
    return DigestUtils.sha256("");      // returns byte arrays
  }

  /**
   * Source code: Kafka GitHub apache/kafka
   * Generates 32 bit murmur2 hash from byte array
   *
   * @link org.apache.kafka.common.utils.Utils.jav
   * @param data byte array to hash
   * @return 32-bit hash of the given array
   */
  @SuppressWarnings("fallthrough")
  public static int murmur2(final byte[] data) {
    int length = data.length;
    int seed = 0x9747b28c;
    // 'm' and 'r' are mixing constants generated offline.
    // They're not really 'magic', they just happen to work well.
    final int m = 0x5bd1e995;
    final int r = 24;

    // Initialize the hash to a random value
    int h = seed ^ length;
    int length4 = length / 4;

    for (int i = 0; i < length4; i++) {
      final int i4 = i * 4;
      int k = (data[i4 + 0] & 0xff) + ((data[i4 + 1] & 0xff) << 8) + ((data[i4 + 2] & 0xff) << 16) + ((data[i4 + 3] & 0xff) << 24);
      k *= m;
      k ^= k >>> r;
      k *= m;
      h *= m;
      h ^= k;
    }

    // Handle the last few bytes of the input array
    switch (length % 4) {
      case 3:
        h ^= (data[(length & ~3) + 2] & 0xff) << 16;
      case 2:
        h ^= (data[(length & ~3) + 1] & 0xff) << 8;
      case 1:
        h ^= data[length & ~3] & 0xff;
        h *= m;
    }

    h ^= h >>> 13;
    h *= m;
    h ^= h >>> 15;

    return h;
  }

  /**
   * A cheap way to deterministically convert a number to a positive value. When the input is
   * positive, the original value is returned. When the input number is negative, the returned
   * positive value is the original value bit AND against 0x7fffffff which is not its absolute
   * value.
   *
   * @link org.apache.kafka.common.utils.Utils.java
   * @param number a given number
   * @return a positive number.
   */
  public static int toPositive(int number) {
    return number & 0x7fffffff;
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

  /**
   * Source : https://stackoverflow.com/a/16251508/15987367
   * @param filePath
   * @return
   */
  public static boolean waitService(String filePath) {
    final Path path = FileSystems.getDefault().getPath(System.getProperty("user.home"), "Desktop");
    System.out.println(path);
    try (final WatchService watchService = FileSystems.getDefault().newWatchService()) {
      final WatchKey watchKey = path.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
      while (true) {
        final WatchKey wk = watchService.take();
        for (WatchEvent<?> event : wk.pollEvents()) {
          //we only register "ENTRY_MODIFY" so the context is always a Path.
          final Path changed = (Path) event.context();
          System.out.println(changed);
          if (changed.endsWith(filePath)) {
            LOGGER.info("File offset has changed");
            return true;
          }
        }
        // reset the key
        boolean valid = wk.reset();
        if (!valid) {
          System.out.println("Key has been unregisterede");
          return false;
        }
      }
    } catch (IOException exception) {
      exception.printStackTrace();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    return false;
  }
}


