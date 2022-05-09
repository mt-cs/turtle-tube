package model.config;

/**
 * Producer instance for config gson class
 *
 * @author marisatania
 */
public class ProducerConfig extends ReplicationConfig{
  private String loadBalancerLocation;
  private String filename;
  private boolean isFullSpeed;

  /**
   * Constructor
   */
  public ProducerConfig(String type, int id, String loadBalancerLocation,
                        String filename, boolean isFullSpeed) {
    super(type, id);
    this.loadBalancerLocation = loadBalancerLocation;
    this.filename = filename;
    this.isFullSpeed = isFullSpeed;
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
   * Getter for filename
   */
  public String getFilename() {
    return filename;
  }

  /**
   * Setter for filename
   */
  public void setFilename(String filename) {
    this.filename = filename;
  }

  /**
   * Getter for isFullSpeed
   */
  public boolean isFullSpeed() {
    return isFullSpeed;
  }

  /**
   * Setter for isFullSpeed
   */
  public void setFullSpeed(boolean fullSpeed) {
    isFullSpeed = fullSpeed;
  }
}
