package controllers.faultinjector;

import interfaces.FaultInjector;

/**
 * Delay injector implementation.
 *
 * @author marisatania
 */
public class DelayInjector implements FaultInjector {
  private final int delay;

  /**
   * Constructor.
   *
   * @param delay the delay parameter
   */
  public DelayInjector(int delay) {
    this.delay = delay;
  }

  /**
   * Inject delay time.
   */
  @Override
  public void injectFailure() {
    FaultUtils.injectDelay(delay);
  }

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
