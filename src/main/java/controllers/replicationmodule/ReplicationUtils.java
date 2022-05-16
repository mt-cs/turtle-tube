package controllers.replicationmodule;

import controllers.faultinjector.FaultInjectorFactory;
import controllers.messagingframework.ConnectionHandler;
import controllers.pubsubframework.PubSubUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.logging.Logger;
import model.Membership.MemberInfo;
import util.Constant;
import util.ReplicationAppUtils;

/**
 * Helper util for replication
 *
 * @author marisatania
 */
public class ReplicationUtils {
  private static final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

  /**
   * Create connection to LoadBalancer
   */
  public static ConnectionHandler connectToLoadBalancer(String loadBalancerLocation) {
    ConnectionHandler loadBalancerConnection = new ConnectionHandler(
        PubSubUtils.getHost(loadBalancerLocation),
        PubSubUtils.getPort(loadBalancerLocation),
        new FaultInjectorFactory(0).getChaos());
    LOGGER.info("Connected to load balancer: " + loadBalancerLocation);
    return loadBalancerConnection;
  }

  /**
   * Send request for broker location
   *
   * @param loadBalancerConnection Connection handler
   */
  public static void sendAddressRequest(ConnectionHandler loadBalancerConnection) {
    MemberInfo addressRequest = MemberInfo.newBuilder()
        .setTypeValue(0)
        .build();
    loadBalancerConnection.send(addressRequest.toByteArray());
  }

  public static String getTopic(String line) {
    String[] lineSplit = line.split(Constant.SPACE);
    String[] urlSplit = lineSplit[6].split(Constant.SLASH);
    if (urlSplit.length == 0) {
      return "";
    }
    return urlSplit[1];
  }

  public static String getCopyFileName(String line) {
    LOGGER.info("Copy from: " + line);
    String[] lineSplit = line.split(Constant.OFFSET_LOG);
    String copyFileName = lineSplit[0] + Constant.COPY_LOG;
    LOGGER.info("Copy fileName: " + copyFileName);
    return copyFileName;
  }

  public static Path copyTopicFiles(Path originalFilePath) {
    LOGGER.info("Copy topic to files: " + originalFilePath);
    String copyFileName = getCopyFileName(originalFilePath.getFileName().toString());

    Path copyFilePath = Path.of(copyFileName);
    try {
      Files.copy(originalFilePath, copyFilePath);
      LOGGER.info("Copied: " + originalFilePath + " to: " + copyFilePath);
    } catch (IOException e) {
      LOGGER.warning("Error while copying file: " + e.getMessage());
    }
    return copyFilePath;
  }


  public static void flushReplicateToFile(byte[] data, String topic) {
    if (data != null) {
      Path filePathSave = Path.of(ReplicationAppUtils.getTopicFile(topic));
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
}
