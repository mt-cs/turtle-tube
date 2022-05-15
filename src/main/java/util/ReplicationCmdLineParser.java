package util;

/**
 * CmdLineParser - a class that parse command line argument
 *
 * @author marisatania
 */
public class ReplicationCmdLineParser {
  private String configFile;
  private String logFile;
  private String type;

  /**
   * Constructor for CmdLineParser
   */
  public ReplicationCmdLineParser() {
    this.configFile = "";
  }

  /**
   * Get config File;
   *
   * @return configFile
   */
  public String getConfigFile() {
    return configFile;
  }

  /**
   * Get log File;
   *
   * @return logFile
   */
  public String getLogFile() {
    return logFile;
  }

  /**
   * Get offset File header;
   *
   * @return offset file header
   */
  public String getOffsetHeader() {
    String[] logArr = logFile.split("\\.");
    return logArr[0];
  }

  /**
   *Get offset File;
   *
   * @return offset file
   */
  public String getOffsetFile() {
    String[] logArr = logFile.split("\\.");
    return logArr[0] + Constant.OFFSET_LOG;
  }

  /**
   * Call parseCmdLineArgs private method
   *
   * @param args command line args
   */
  public boolean parseCmdLine(String[] args) {
    return parseCmdLineArgs(args);
  }

  /**
   * Modular parser command line arguments to get filenames
   *
   * -type producer -config <config json file>
   * @param args command line args
   */
  private boolean parseCmdLineArgs(String[] args) {
    if (args.length != Constant.CONFIG_LENGTH
        || !args[0].equals(Constant.TYPE_FLAG)
        || !args[2].equals(Constant.CONFIG_FLAG)) {
      return false;
    }

    type = args[1];
    configFile = args[3];
    logFile= args[5];
    return true;
  }

  /**
   * Get type
   *
   * @return type
   */
  public String getType() {
    return type;
  }

}