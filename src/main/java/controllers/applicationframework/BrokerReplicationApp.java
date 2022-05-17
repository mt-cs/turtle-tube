package controllers.applicationframework;

import controllers.pubsubframework.Broker;
import model.config.BrokerConfig;

/**
 * Broker runner app
 *
 * -type broker -config config/configBrokerReplication.json -log broker1.log
 */
public class BrokerReplicationApp {

  public void runBrokerApplication (BrokerConfig brokerConfig) {
    Broker broker = new Broker(brokerConfig.getHost(), brokerConfig.getPort(),
                               brokerConfig.getLeaderBasedPort(), brokerConfig.isLeader(),
                               brokerConfig.getId(), brokerConfig.getFaultType(), brokerConfig.isRf());

    broker.listenToConnections();
    if (!brokerConfig.isLeader()) {
      broker.connectToPeer(brokerConfig.getTargetBrokerLocation(),
                           brokerConfig.getTargetLeaderBasedLocation(),
                           brokerConfig.getTargetId());
    }
  }
}
