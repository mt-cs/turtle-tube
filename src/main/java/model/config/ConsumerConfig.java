package model.config;

/**
 * Consumer instance for config gson class
 *
 * @author marisatania
 */
public class ConsumerConfig extends ReplicationConfig {
  private String loadBalancerLocation;
  private String topic;
  private int startingPosition;
  private String model;

  /**
   * Constructor
   */
  public ConsumerConfig(String type, int id, String loadBalancerLocation,
                        String topic, int startingPosition, String model) {
    super(type, id);
    this.loadBalancerLocation = loadBalancerLocation;
    this.topic = topic;
    this.startingPosition = startingPosition;
    this.model = model;
  }

  /**
   * Getter for loadBalancerLocation
   */
  public String getLoadBalancerLocation() {
    return loadBalancerLocation;
  }

  /**
   * Setter for loadBalancerLocation
   */
  public void setLoadBalancerLocation(String loadBalancerLocation) {
    this.loadBalancerLocation = loadBalancerLocation;
  }

  /**
   * Getter for topic
   */
  public String getTopic() {
    return topic;
  }

  /**
   * Setter for topic
   */
  public void setTopic(String topic) {
    this.topic = topic;
  }

  /**
   * Getter for startingPosition
   */
  public int getStartingPosition() {
    return startingPosition;
  }

  /**
   * Setter for startingPosition
   */
  public void setStartingPosition(int startingPosition) {
    this.startingPosition = startingPosition;
  }

  /**
   * Getter for model
   */
  public String getModel() {
    return model;
  }

  /**
   * Setter for model
   */
  public void setModel(String model) {
    this.model = model;
  }
}
