package model;

import controllers.messagingframework.ConnectionHandler;
import controllers.pubsubframework.PubSubUtils;

/**
 * Member account info
 *
 * @author marisatania
 */
public class MemberAccount {
  private String host;
  private int port;
  private final int leaderBasedPort;
  private final int brokerId;
  private int version;
  private final String pubSubLocation;
  private ConnectionHandler pubSubConnection;
  private ConnectionHandler leaderBasedConnection;
  private boolean isLeader;

  /**
   * Constructor for member Account with connection
   *
   * @param host                  Host name
   * @param port                  pubSub port
   * @param leaderBasedConnection leaderBasedConnection
   * @param isLeader              isLeader
   * @param brokerId              memberId
   * @param leaderBasedPort       leaderBased port
   */
  public MemberAccount(String host, int port,
      ConnectionHandler leaderBasedConnection,
      boolean isLeader, int brokerId, int leaderBasedPort) {
    this.host = host;
    this.port = port;
    this.leaderBasedConnection = leaderBasedConnection;
    this.leaderBasedPort = leaderBasedPort;
    this.isLeader = isLeader;
    this.pubSubLocation = PubSubUtils.getBrokerLocation(host, port);
    this.brokerId = brokerId;
  }

  /**
   * Constructor for member Account
   *
   * @param host                  Host name
   * @param port                  pubSub port
   * @param isLeader              isLeader
   * @param brokerId              memberId
   * @param leaderBasedPort       leaderBased port
   */
  public MemberAccount(String host, int port, boolean isLeader,
      int brokerId, int leaderBasedPort) {
    this.host = host;
    this.port = port;
    this.pubSubLocation = PubSubUtils.getBrokerLocation(host, port);
    this.isLeader = isLeader;
    this.brokerId = brokerId;
    this.leaderBasedPort = leaderBasedPort;
  }

  /**
   * Get broker Id
   *
   * @return brokerId
   */
  public int getBrokerId() {
    return brokerId;
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
   * Setter for host
   *
   * @param host host
   */
  public void setHost(String host) {
    this.host = host;
  }

  /**
   * Getter for port
   *
   * @return port
   */
  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }

  /**
   * Getter for connection
   *
   * @return pubSub connection
   */
  public ConnectionHandler getPubSubConnection() {
    return pubSubConnection;
  }

  /**
   * Setter for pubsub connection
   *
   * @param pubSubConnection Connection handler
   */
  public void setPubSubConnection(ConnectionHandler pubSubConnection) {
    this.pubSubConnection = pubSubConnection;
  }

  /**
   * Getter for leader based connection
   *
   * @return leaderBasedConnection
   */
  public ConnectionHandler getLeaderBasedConnection() {
    return leaderBasedConnection;
  }

  /**
   * Check if member is the leader
   *
   * @return isLeader
   */
  public boolean isLeader() {
    return isLeader;
  }

  /**
   * Set isLeader
   *
   * @param leader boolean
   */
  public void setLeader(boolean leader) {
    isLeader = leader;
  }

  /**
   * Get leader based location
   *
   * @return host and port
   */
  public String getLeaderBasedLocation() {
    return PubSubUtils.getBrokerLocation(getHost(), leaderBasedPort);
  }

  /**
   * Getter for location
   *
   * @return pubSub location
   */
  public String getPubSubLocation() {
    return pubSubLocation;
  }

  /**
   * Getter for port
   *
   * @return leaderBasedPort
   */
  public int getLeaderBasedPort() {
    return leaderBasedPort;
  }

  /**
   * Get broker's version
   *
   * @return version
   */
  public int getVersion() {
    return version;
  }

  /**
   * Setter for version
   *
   * @param version int data version
   */
  public void setVersion(int version) {
    this.version = version;
  }
}