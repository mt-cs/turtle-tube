package model.config;

/**
 * Configuration file
 *
 * @author marisatania
 */
public class Config {
  private String type;
  private String id;
  private String kind;
  private int port;
  private String host;
  private String topic;
  private String key;
  private int startingPosition;
  private int numBrokers;
  private int numPartitions;

  /**
   * Getter for type
   *
   * @return type
   */
  public String getType() {
    return type;
  }

  /**
   * Getter for id
   *
   * @return id
   */
  public String getId() {
    return id;
  }

  /**
   * Getter for port
   *
   * @return port
   */
  public int getPort() {
    return port;
  }

  /**
   * Getter for host
   *
   * @return host
   */
  public String getHost() {
    return host;
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
   * Getter for starting position
   *
   * @return starting position
   */
  public int getStartingPosition() {
    return startingPosition;
  }
}
