package model;

/**
 * Heartbeat info model
 */
public class HeartBeatInfo {
  private final Long heartBeatReceivedTime;
  private int missedHeartbeat;

  /**
   * Constructor
   *
   * @param heartBeatReceivedTime time of receive
   * @param missedHeartbeat       number of missed heartbeat
   */
  public HeartBeatInfo(Long heartBeatReceivedTime, int missedHeartbeat) {
    this.heartBeatReceivedTime = heartBeatReceivedTime;
    this.missedHeartbeat = missedHeartbeat;
  }

  /**
   * Get receive time
   *
   * @return heartBeatReceivedTime
   */
  public Long getHeartBeatReceivedTime() {
    return heartBeatReceivedTime;
  }

  /**
   * Get if heartbeat is missed
   *
   * @return missedHeartbeat
   */
  public int getMissedHeartbeat() {
    return missedHeartbeat;
  }

  /**
   * Set if heartbeat is missed
   * @param missedHeartbeat integer
   */
  public void setMissedHeartbeat(int missedHeartbeat) {
    this.missedHeartbeat = missedHeartbeat;
  }
}
