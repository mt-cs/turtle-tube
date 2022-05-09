package controllers.faultinjector;

import interfaces.FaultInjector;
import java.util.logging.Logger;
import util.Constant;
import util.Utils;

/**
 * Factory design pattern for connection
 *
 * @author marisatania
 */
public class FaultInjectorFactory {
  private final int chaosId;
  private final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

  /**
   * Constructor.
   *
   * @param chaosId chaos status
   */
  public FaultInjectorFactory(int chaosId) {
    this.chaosId = chaosId;
  }

  /**
   * Instantiate a fault injector
   */
  public FaultInjector getChaos() {
    FaultInjector faultInjector;
    if (chaosId == 0) {
      faultInjector = new DefaultInjector();
      LOGGER.info("Factory created Default Injector.");
    } else if (chaosId == 1) {
      faultInjector = new LossInjector(Constant.LOSS_RT);
      LOGGER.info("Factory created Loss Fault Injector.");
    } else if (chaosId == 2) {
      faultInjector = new DelayInjector(Constant.DELAY);
      LOGGER.info("Factory created Delay Fault Injector.");
    } else if (chaosId == 3) {
      faultInjector = new FaultyInjector(Constant.DELAY, Constant.LOSS_RT);
      LOGGER.info("Factory created Faulty Injector.");
    } else {
      System.out.println("Unknown injector type. Implement default injector");
      faultInjector = new DefaultInjector();
    }
    return faultInjector;
  }
}