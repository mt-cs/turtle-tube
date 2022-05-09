package controllers.applicationframework;

import model.config.BrokerConfig;
import model.config.ConsumerConfig;
import model.config.LoadBalancerConfig;
import model.config.ProducerConfig;
import model.config.ReplicationConfig;
import util.Constant;

/**
 * Manage Publish/Subscribe system application
 *
 * @author marisatania
 */
public class ReplicationApp {
  private final String hostType;
  private final ReplicationConfig host;

  /**
   * Constructor
   *
   * @param hostType broker/ producer/ consumer
   * @param host     node configInfo
   */
  public ReplicationApp(String hostType, ReplicationConfig host) {
    this.hostType = hostType;
    this.host = host;
  }

  /**
   * Run application
   */
  public void runApplication() {
    switch (hostType) {
      case Constant.PRODUCER -> new ProducerReplicationApp().runProducerApplication((ProducerConfig) host);
      case Constant.CONSUMER -> new ConsumerReplicationApp().runConsumerApplication((ConsumerConfig) host);
      case Constant.BROKER -> new BrokerReplicationApp().runBrokerApplication((BrokerConfig) host);
      case Constant.LOADBALANCER -> new LoadBalancerApp().runLoadBalancerApplication((LoadBalancerConfig) host);
    }
  }
}
