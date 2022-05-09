package controllers.faultinjector;

import java.util.Random;
import java.util.logging.Logger;

/**
 * Fault Injector utilities
 *
 * @author marisatania
 */
public class FaultUtils {
  private static final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

  /**
   * Add delay
   *
   * @param delay delay bound
   */
  public static void injectDelay(int delay) {
    if (delay != 0) {
      try {
        int delayTime = new Random().nextInt(delay);
        Thread.sleep(delayTime);
        LOGGER.info("Delayed for: " + delayTime);
      } catch (InterruptedException e) {
        LOGGER.warning("Error injecting delay: " + e.getMessage());
      }
    }
  }

  /**
   * Inject loss
   *
   * @param lossRate loss rate
   * @return true if loss, false otherwise
   */
  public static boolean injectLoss(double lossRate) {
    if (new Random().nextDouble() <= lossRate) {
      LOGGER.info("Loss injector failure.");
      return true;
    } else {
      return false;
    }
  }
}
