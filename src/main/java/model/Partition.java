package model;

/**
 * Model for partition
 */
public class Partition {
  private String topic;
  private String key;
  private byte[]keyBytes;
  private String brokerLocation;

  /**
   * Getter for topic
   *
   * @return topic
   */
  public String getTopic() {
    return topic;
  }

  /**
   * Getter for key
   *
   * @return key
   */
  public String getKey() {
    return key;
  }

  /**
   * Setter for key
   */
  public void setKey(String key) {
    this.key = key;
  }

  /**
   * Get broker location
   *
   * @return broker location
   */
  public String getBrokerLocation() {
    return brokerLocation;
  }

  /**
   * Set broker location
   *
   * @param brokerLocation broker location
   */
  public void setBrokerLocation(String brokerLocation) {
    this.brokerLocation = brokerLocation;
  }
}
