package model;

/**
 * Lossy Parameter object
 *
 * @author marisatania
 */
public class LossyParam {
  private double lossRate;
  private int delay;

  /**
   * Getter for loss rate
   *
   * @return lossRate
   */
  public double getLossRate() {
    return lossRate;
  }

  /**
   * Getter for delay parameter
   *
   * @return delay
   */
  public int getDelay() {
    return delay;
  }

  /**
   * Lossy parameters as string
   *
   * @return toString
   */
  @Override
  public String toString() {
    return "LossyParam{" +
        "lossRate=" + lossRate +
        ", delay=" + delay +
        '}';
  }
}