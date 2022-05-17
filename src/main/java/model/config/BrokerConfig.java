package model.config;

/**
 * Broker instance for config gson class
 *
 * @author marisatania
 */
public class BrokerConfig extends ReplicationConfig {
  private String host;
  private int port;
  private int leaderBasedPort;
  boolean isLeader;
  private String targetBrokerLocation;
  private String targetLeaderBasedLocation;
  private int targetId;
  private int faultType;
  private String loadBalancerLocation;
  private boolean isRf;

  /**
   * Constructor
   */
  public BrokerConfig(String type, int id, String host, int port, int leaderBasedPort,
      boolean isLeader, String targetBrokerLocation, String targetLeaderBasedLocation, int targetId,
      int faultType, String loadBalancerLocation, boolean isRf) {
    super(type, id);
    this.host = host;
    this.port = port;
    this.leaderBasedPort = leaderBasedPort;
    this.isLeader = isLeader;
    this.targetBrokerLocation = targetBrokerLocation;
    this.targetLeaderBasedLocation = targetLeaderBasedLocation;
    this.targetId = targetId;
    this.faultType = faultType;
    this.loadBalancerLocation = loadBalancerLocation;
    this.isRf = isRf;
  }

  /**
   * Getter for host
   */
  public String getHost() {
    return host;
  }

  /**
   * Setter for host
   */
  public void setHost(String host) {
    this.host = host;
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

  /**
   * Getter for leaderBasedPort
   */
  public int getLeaderBasedPort() {
    return leaderBasedPort;
  }

  /**
   * Getter for leaderBasedPort
   */
  public void setLeaderBasedPort(int leaderBasedPort) {
    this.leaderBasedPort = leaderBasedPort;
  }

  /**
   * Getter for isLeader
   */
  public boolean isLeader() {
    return isLeader;
  }

  /**
   * Setter for isLeader
   */
  public void setLeader(boolean leader) {
    isLeader = leader;
  }

  /**
   * Getter for target pubsub location
   */
  public String getTargetBrokerLocation() {
    return targetBrokerLocation;
  }

  /**
   * Setter for target pubsub location
   */
  public void setTargetBrokerLocation(String targetBrokerLocation) {
    this.targetBrokerLocation = targetBrokerLocation;
  }

  /**
   * Getter for target leader based location
   */
  public String getTargetLeaderBasedLocation() {
    return targetLeaderBasedLocation;
  }

  /**
   * Setter for target leader based location
   */
  public void setTargetLeaderBasedLocation(String targetLeaderBasedLocation) {
    this.targetLeaderBasedLocation = targetLeaderBasedLocation;
  }

  /**
   * Getter for targetID
   */
  public int getTargetId() {
    return targetId;
  }

  /**
   * Setter for targertID
   */
  public void setTargetId(int targetId) {
    this.targetId = targetId;
  }

  /**
   * Getter for fault type
   */
  public int getFaultType() {
    return faultType;
  }

  /**
   * Setter for fault type
   */
  public void setFaultType(int faultType) {
    this.faultType = faultType;
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
   * Getter for rf
   */
  public boolean isRf() {
    return isRf;
  }

  /**
   * Setter for rf
   */
  public void setRf(boolean rf) {
    isRf = rf;
  }
}
