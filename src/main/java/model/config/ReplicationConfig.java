package model.config;

/**
 * Replication config superclass
 *
 * @author marisatania
 */
public class ReplicationConfig {
  private String type;
  private int id;

  /**
   * Constructor
   */
  public ReplicationConfig(String type, int id) {
    this.type = type;
    this.id = id;
  }

  /**
   * Getter for id
   */
  public int getId() {
    return id;
  }

  /**
   * Setter for id
   */
  public void setId(int id) {
    this.id = id;
  }

  /**
   * Getter for type
   */
  public String getType() {
    return type;
  }

  /**
   * Setter for type
   */
  public void setType(String type) {
    this.type = type;
  }
}
