package controllers.applicationframework;

import controllers.pubsubframework.ConsumerReplication;
import controllers.pubsubframework.PubSubUtils;
import java.time.Duration;
import java.util.logging.Logger;
import model.config.ConsumerConfig;
import util.Constant;

/**
 * An application running on any host
 * that consumes messages from a broker.
 *
 * -type consumer -config config/ConfigConsumerReplication.json -log consumer1.log
 *
 * @author marisatania
 */
public class ConsumerReplicationApp {
  private ConsumerReplication consumer;
  private static final Logger LOGGER
      = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

  /**
   * Run consumer application
   */
  public void runConsumerApplication (ConsumerConfig consumerConfig) {
    consumer = new ConsumerReplication(consumerConfig.getLoadBalancerLocation(),
                                       consumerConfig.getTopic(),
                                       consumerConfig.getStartingPosition(),
                                       consumerConfig.getModel(),
                                       consumerConfig.getRead());

    if (consumerConfig.getModel().equals(Constant.PULL)) {
      Thread consumerAppThread = new Thread(new ApplicationWrite());
      consumerAppThread.start();
      try {
        consumerAppThread.join();
      } catch (InterruptedException e) {
        LOGGER.warning("Consumer app file thread join error: " + e.getMessage());
      }
    }
  }


  /**
   * Save the data consumed to a file
   */
  private class ApplicationWrite implements Runnable{
    @Override
    public void run() {
      LOGGER.info("Consumer application write... ");
      while (true) {
        byte[] message = consumer.poll(Duration.ofMillis(Constant.POLL_TIMEOUT));
        PubSubUtils.flushToFile(message);
      }
    }
  }
}
