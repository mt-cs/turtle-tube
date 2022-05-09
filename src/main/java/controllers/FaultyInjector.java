package controllers.faultinjector;

import interfaces.FaultInjector;

/**
 * Inject both loss and delay
 *
 * @author marisatania
 */
public class FaultyInjector implements FaultInjector {
  private final int delayBound;
  private final double lossRate;

  /**
   * Constructor
   *
   * @param delayBound the delay parameter
   */
  public FaultyInjector(int delayBound, double lossRate) {
    this.delayBound = delayBound;
    this.lossRate = lossRate;
  }

  /**
   * Inject delay
   */
  @Override
  public void injectFailure() {
    FaultUtils.injectDelay(delayBound);
  }

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