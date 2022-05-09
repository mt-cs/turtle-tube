package controllers.faultinjector;

import interfaces.FaultInjector;

/**
 * A default injector
 * that doesn't return any fault
 * for a non-faulty default connection
 *
 * @author marisatania
 */
public class DefaultInjector implements FaultInjector {

  /**
   * No fault
   */
  @Override
  public void injectFailure() {}

  /**
   * Should fail is false
   *
   * @return false
   */
  @Override
  public boolean shouldFail() {
    return false;
  }

}
