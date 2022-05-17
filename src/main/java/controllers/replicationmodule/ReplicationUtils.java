package controllers.replicationmodule;

import controllers.faultinjector.FaultInjectorFactory;
import controllers.messagingframework.ConnectionHandler;
import controllers.pubsubframework.PubSubUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;
import model.MemberAccount;
import model.Membership.MemberInfo;
import util.Constant;

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
  public static void sendAddressRequest(ConnectionHandler loadBalancerConnection,
                                        int type, int id) {
    MemberInfo addressRequest = MemberInfo.newBuilder()
        .setTypeValue(type)
        .setId(id)
        .build();
    loadBalancerConnection.send(addressRequest.toByteArray());
  }

  /**
   * Get random member up to rf followers
   *
   * @param brokerAccountList member account list
   * @return rfBrokerList
   */
  public static List<MemberAccount> getRandomMemberAccounts(
      List<MemberAccount> brokerAccountList) {
    List<MemberAccount> rfBrokerList
        = Collections.synchronizedList(new ArrayList<>());

    int randomIndex;
    MemberAccount randomAccount;
    for (int i = 0; i < Constant.RF; i++) {
      randomIndex = new Random().nextInt(brokerAccountList.size());
      randomAccount = brokerAccountList.get(randomIndex);
      brokerAccountList.remove(randomIndex);
      rfBrokerList.add(randomAccount);
    }
    LOGGER.info(rfBrokerList.toString());
    return rfBrokerList;
  }

  /**
   * Copy topic file to a new path
   *
   * @param originalFilePath origin path
   * @param id               target broker Id
   * @return copyFilePath
   */
  public static Path copyTopicFiles(Path originalFilePath, String id) {
    String copyFileName = getCopyFileName(originalFilePath.getFileName().toString(), id);

    Path copyFilePath = Path.of(copyFileName);
    try {
      Files.copy(originalFilePath, copyFilePath);
      LOGGER.info("Copied: " + originalFilePath + " to: " + copyFilePath);
    } catch (IOException e) {
      LOGGER.warning("Error while copying file: " + e.getMessage());
    }
    return copyFilePath;
  }

  /**
   * Get the name of copy file
   *
   * @param line original offset file name
   * @param id   target broker Id
   * @return copyFileName
   */
  public static String getCopyFileName(String line, String id) {
    String[] lineSplit = line.split(Constant.OFFSET_LOG);
    String copyFileName = lineSplit[0] + Constant.UNDERSCORE + PubSubUtils.getPort(id) + Constant.COPY_LOG;
    LOGGER.info("Copy fileName: " + copyFileName);
    return copyFileName;
  }

  /**
   * Get topic name
   *
   * @param line stream
   * @return topic name
   */
  public static String getTopic(String line) {
    String[] lineSplit = line.split(Constant.SPACE);
    if (lineSplit.length < 7) {
      return "";
    }
    String[] urlSplit = lineSplit[6].split(Constant.SLASH);
    if (urlSplit.length <= 1) {
      return "";
    }
    return urlSplit[1];
  }
}
