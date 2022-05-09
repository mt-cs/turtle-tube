package interfaces;

import java.util.logging.Logger;

/**
 * Fault Injector interface
 *
 * @author marisatania
 */
public interface FaultInjector {
  Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

  void injectFailure();
  boolean shouldFail();
}
