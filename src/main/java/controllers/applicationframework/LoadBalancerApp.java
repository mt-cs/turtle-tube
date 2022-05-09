package controllers.applicationframework;

import controllers.pubsubframework.LoadBalancer;
import model.config.LoadBalancerConfig;

/**
 * Load balancer runner:
 * -type loadbalancer -config config/configLoadBalancerReplication.json -log loadbalancer.log
 *
 * @author marisatania
 */
public class LoadBalancerApp {

  /**
   * Run producer replication
   *
   * @param loadBalancerConfigConfig configuration
   */
  public void runLoadBalancerApplication(LoadBalancerConfig loadBalancerConfigConfig) {

    LoadBalancer loadBalancer = new LoadBalancer(loadBalancerConfigConfig.getPort());
    loadBalancer.listenToConnection();
  }
}
