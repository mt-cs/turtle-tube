package model;

/**
 * LogStream model
 *
 * @author marisatania
 */
public class LogStream {
  private String topic;
  private String key;
  byte[] data;

  /**
   * Constructor
   *
   * @param topic  String topic
   * @param key    String key
   * @param data   Data array
   */
  public LogStream(String topic, String key, byte[] data) {
    this.topic = topic;
    this.key = key;
    this.data = data;
  }

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
   * Getter for data
   *
   * @return data
   */
  public byte[] getData() {
    return data;
  }
}
