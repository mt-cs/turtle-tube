package model.config;

/**
 * Load balancer config for gson
 *
 * @author marisatania
 */
public class LoadBalancerConfig extends ReplicationConfig {
  private int port;

  /**
   * Constructor
   */
  public LoadBalancerConfig(String type, int id, int port) {
    super(type, id);
    this.port = port;
  }

  /**
   * Getter for port
   */
  public int getPort() {
    return port;
  }

  /**
   * Setter for port
   */
  public void setPort(int port) {
    this.port = port;
  }
}
