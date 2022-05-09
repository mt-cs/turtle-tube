package util;

import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;
import model.config.Config;

/**
 * Utils class to run config file
 *
 * @author marisatania
 */
public class Utils {
  private static Config config;
  private static final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

  /**
   * Get starting position
   *
   * @return starting position
   */
  public static int getStartingPosition() {
    return config.getStartingPosition();
  }

  /**
   * Get topic name
   *
   * @return topic
   */
  public static String getTopic() {
    return config.getTopic();
  }


  /**
   * Get host ID
   *
   * @return host ID
   */
  public static String getHostId() {
    return config.getId();
  }

  /**
   * Get host type
   *
   * @return host type
   */
  public static String getHostType() {
    return config.getType();
  }

  /**
   * Run configuration
   *
   * @return config
   */
  public static Config runConfig(String fileName) {
    config = null;
    try (BufferedReader reader = Files.newBufferedReader
        (Paths.get(fileName), StandardCharsets.ISO_8859_1)) {
      String line;
      Gson gson = new Gson();

      if ((line = reader.readLine()) != null) {
        config = gson.fromJson(line, Config.class);
      }
    } catch(IOException e) {
      LOGGER.log(Level.SEVERE,"\nInvalid file input. Please try again.");
      System.exit(0);
    } catch (com.google.gson.JsonSyntaxException e) {
      LOGGER.log(Level.INFO,"Gson exception: " + e.getMessage());
    }
    return config;
  }

  /**
   * Check if the value is an integer
   *
   * @param input value
   * @return true if it's an integer
   */
  public static boolean isInteger(String input) {
    try {
      Integer.parseInt(input);
      return true;
    }
    catch (NumberFormatException e) {
      LOGGER.warning(input + " is not a valid integer\n" + e.getMessage());
    }
    return false;
  }

  /**
   * Get broker info from location
   *
   * @param brokerLocation broker location
   * @return array of host and port
   */
  public static String[] getBrokerInfo(String brokerLocation) {
    return brokerLocation.split(":");
  }

  /**
   * Get config object
   *
   * @return config
   */
  public static Config getConfig() {
    return config;
  }


  /**
   * This function reads configuration file from the provided path.
   *
   * @param path input path
   * @return String
   */
  public static String readFile(Path path) {
    StringBuilder sb = new StringBuilder();
    try (BufferedReader in = Files.newBufferedReader(path)){
      String line;
      while((line = in.readLine()) != null) {
        sb.append(line);
      }
    } catch (IOException ioe) {
      LOGGER.warning("Check provided file, IOException occurred: " +  ioe);
    }
    return sb.toString();
  }
}