package controllers.replicationmodule;

import controllers.faultinjector.FaultInjectorFactory;
import controllers.messagingframework.ConnectionHandler;
import controllers.pubsubframework.PubSubUtils;
import java.util.logging.Logger;
import model.Membership.MemberInfo;

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

}
