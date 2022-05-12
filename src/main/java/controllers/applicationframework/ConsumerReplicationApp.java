package controllers.applicationframework;

import controllers.pubsubframework.ConsumerReplication;
import controllers.replicationmodule.ReplicationUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.logging.Logger;
import model.config.ConsumerConfig;
import util.Constant;
import util.ReplicationAppUtils;

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
                                       consumerConfig.getModel());
    Thread consumerAppThread = new Thread(new ApplicationWrite());
    LOGGER.info("Starting consumer app... ");
    consumerAppThread.start();
    try {
      consumerAppThread.join();
    } catch (InterruptedException e) {
      LOGGER.warning("Consumer app file thread join error: " + e.getMessage());
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
        LOGGER.info("Polling from blocking queue... ");
        byte[] message = consumer.poll(Duration.ofMillis(Constant.POLL_TIMEOUT));
        LOGGER.info("Polling from blocking queue... ");
        if (message != null) {
          Path filePathSave = Path.of(ReplicationAppUtils.getOffsetFile());
          if (!Files.exists(filePathSave)) {
            try {
              Files.write(filePathSave, message);
            } catch (IOException e) {
              LOGGER.warning("Exception during consumer application write: " + e.getMessage());
            }
            LOGGER.info("Creating consumer application file path: " + filePathSave);
          } else {
            try {
              Files.write(filePathSave, message, StandardOpenOption.APPEND);
            } catch (IOException e) {
              LOGGER.warning("Consumer app file write exception: " + e.getMessage());
            }
          }
        }
      }
    }
  }
}
