package controllers.applicationframework;

import controllers.pubsubframework.ConsumerReplication;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
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
        consumerConfig.getTopic(), consumerConfig.getStartingPosition());

    Thread consumerAppThread = new Thread(new ApplicationWrite());
    consumerAppThread.start();
    try {
      consumerAppThread.join();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }


  /**
   * Save the data consumed to a file
   */
  private class ApplicationWrite implements Runnable{
    @Override
    public void run() {
      while (true) {
        byte[] message = consumer.poll(Duration.ofMillis(Constant.POLL_TIMEOUT));

        if (message != null) {
          Path filePathSave = Path.of("consumer_replication2.log");
          if (!Files.exists(filePathSave)) {
            try {
              Files.write(filePathSave, message);
            } catch (IOException e) {
              LOGGER.warning("Exception during consumer application write: " + e.getMessage());
            }
            LOGGER.info("Creating consumer application file path: " + filePathSave);
          }
          try {
            Files.write(filePathSave, message, StandardOpenOption.APPEND);
            Files.writeString(filePathSave, "\n", StandardOpenOption.APPEND);
            LOGGER.info("Writing to consumer application file...");
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
      }
    }
  }
}
