package controllers.applicationframework;

import controllers.pubsubframework.ProducerReplication;
import controllers.pubsubframework.PubSubUtils;
import java.util.ArrayList;
import model.LogStream;
import model.config.ProducerConfig;
import util.StreamProcessor;

/**
 * Producer application mimics a front-end web server
 * by replaying data from logs with fault-tolerant replication
 *
 * @author marisatania
 */
public class ProducerReplicationApp {

  /**
   * Run producer replication
   *
   * -type producer -config config/configProducerReplication.json -log producer1.log
   *
   * @param producerConfig producer configuration
   */
  public void runProducerApplication(ProducerConfig producerConfig) {

    ProducerReplication producer =
        new ProducerReplication(producerConfig.getLoadBalancerLocation(),
                                producerConfig.getId());
    producer.connectToLoadBalancer();
    producer.getLeaderAddress();
    producer.connectToBroker();

    /* Process data stream */
    StreamProcessor streamProcessor = new StreamProcessor(producerConfig.getFilename());
    ArrayList<LogStream> streamsList = streamProcessor.processStream();

    /* Send data */
    for (LogStream stream : streamsList) {
      if (!producerConfig.isFullSpeed()) {
        PubSubUtils.promptEnterKey();
      }
      producer.send(stream.getTopic(), stream.getData());
    }

    /* Close connection */
    producer.close();
  }
}