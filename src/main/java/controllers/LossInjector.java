package controllers.faultinjector;

import interfaces.FaultInjector;

/**
 * Loss fault injector implementation.
 *
 * @author marisatania
 */
public class LossInjector implements FaultInjector {
  private final double lossRate;

  /**
   * Constructor.
   *
   * @param lossRate loss rate
   */
  public LossInjector(double lossRate) {
    this.lossRate = lossRate;
  }

  /**
   * Do nothing.
   */
  @Override
  public void injectFailure() {}

  /**
   * Inject loss
   *
   * @return true if loss, false otherwise
   */
  @Override
  public boolean shouldFail() {
    return FaultUtils.injectLoss(lossRate);
  }
}