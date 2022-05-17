package util;

import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;
import model.config.BrokerConfig;
import model.config.ConsumerConfig;
import model.config.LoadBalancerConfig;
import model.config.ProducerConfig;
import model.config.ReplicationConfig;

/**
 * Utils class to run config file
 *
 * @author marisatania
 */
public class ReplicationAppUtils {
  private static ReplicationConfig config;
  private static final ReplicationCmdLineParser cmdLineParser = new ReplicationCmdLineParser();
  private static final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

  /**
   * Calls parse command line to get hosts
   *
   * @param args command line arguments
   * @return hosts ArrayList
   */
  public static ReplicationConfig runConfig (String[] args) {
    if (!cmdLineParser.parseCmdLine(args)) {
      System.out.println("ParseCmdLine: invalid command line input. Exiting... Please try again!");
      System.out.println("Usage: -type <sourceType> -config <config json file> -log <output log file>");
      System.exit(0);
    }
    runConfig(cmdLineParser.getConfigFile());
    return config;
  }

  /**
   * Run configuration
   *
   */
  public static void runConfig(String fileName) {
    config = null;
    try (BufferedReader reader = Files.newBufferedReader
        (Paths.get(fileName), StandardCharsets.ISO_8859_1)) {
      String line;
      Gson gson = new Gson();

      if ((line = reader.readLine()) != null) {
        switch (cmdLineParser.getType()) {
          case Constant.BROKER -> config = gson.fromJson(line, BrokerConfig.class);
          case Constant.PRODUCER -> config = gson.fromJson(line, ProducerConfig.class);
          case Constant.CONSUMER -> config = gson.fromJson(line, ConsumerConfig.class);
          case Constant.LOADBALANCER -> config = gson.fromJson(line, LoadBalancerConfig.class);
        }
      }
    } catch(IOException e) {
      LOGGER.log(Level.SEVERE,"\nInvalid file input. Please try again.");
      System.exit(0);
    } catch (com.google.gson.JsonSyntaxException e) {
      LOGGER.log(Level.INFO,"Gson exception: " + e.getMessage());
    }
  }

  /**
   * Getter for app configuration
   *
   * @return config
   */
  public static ReplicationConfig getConfig() {
    return config;
  }

  /**
   * Get host ID
   *
   * @return host ID
   */
  public static int getId() {
    return config.getId();
  }

  /**
   * Get host type
   *
   * @return host type
   */
  public static String getType() {
    return config.getType();
  }

  /**
   * Get log File
   *
   * @return logFile
   */
  public static String getOffsetFile() {
    return cmdLineParser.getOffsetFile();
  }

  /**
   * Get topic file
   *
   * @param topic msg topic
   * @return topic
   */
  public static String getTopicFile(String topic) {
    return cmdLineParser.getOffsetHeader() + Constant.UNDERSCORE + topic + Constant.OFFSET_LOG;
  }

}