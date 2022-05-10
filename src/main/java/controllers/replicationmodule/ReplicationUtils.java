package controllers.replicationmodule;

import controllers.faultinjector.FaultInjectorFactory;
import controllers.messagingframework.ConnectionHandler;
import controllers.pubsubframework.PubSubUtils;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import model.Membership.MemberInfo;
import model.MsgInfo;
import model.MsgInfo.Message;

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

  public static synchronized ConcurrentHashMap<String, List<Message>> mergeTopicMap(
      ConcurrentHashMap<String, List<Message>> topicMapOri,
      ConcurrentHashMap<String, List<Message>> topicMapCatchUp) {

    for (Map.Entry<String, List<Message>> topic : topicMapOri.entrySet()) {
      List<MsgInfo.Message> msgList = topicMapOri.get(topic.getKey());
      if (topicMapCatchUp.containsKey(topic.getKey())) {
        for (MsgInfo.Message msg : msgList) {
          topicMapCatchUp.get(topic.getKey()).add(msg);
          LOGGER.info("Catch up snapshot merge:" + msg.getMsgId());
        }
      } else {
        topicMapCatchUp.putIfAbsent(topic.getKey(), msgList);
        LOGGER.info("Catch up snapshot merge topic:" + topic.getKey());
      }
    }
    return topicMapCatchUp;
  }

}
