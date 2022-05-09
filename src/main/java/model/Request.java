package model;

/**
 * Request model class
 *
 * @author marisatania
 */
public class Request {
  private final String fileName;
  private final String hostId;
  private final String saveAsName;

  /**
   * Constructor
   *
   * @param fileName    requested file name
   * @param hostId      host name
   * @param saveAsName  new file name
   */
  public Request(String fileName, String hostId, String saveAsName) {
    this.fileName = fileName;
    this.hostId = hostId;
    this.saveAsName = saveAsName;
  }

  /**
   * Get new file name
   *
   * @return saveAsName
   */
  public String getSaveAsName() {
    return saveAsName;
  }

  /**
   * Get requested file name
   *
   * @return fileName
   */
  public String getFileName() {
    return fileName;
  }

  /**
   * Get host name
   *
   * @return hostId
   */
  public String getHostId() {
    return hostId;
  }

}
