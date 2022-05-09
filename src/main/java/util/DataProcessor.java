package util;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.logging.Logger;

/**
 * A helper class to clean up Kaggle log data
 */
public class DataProcessor {
  private final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
  private String filePath;

  /**
   * Constructor
   *
   * @param filePath file name
   */
  public DataProcessor(String filePath) {
    this.filePath = filePath;
  }

  /**
   * This function split files from the provided path.
   */
  public void readFile() {
    Path path = Paths.get(filePath);
    try (BufferedReader in = Files.newBufferedReader(path)){
      String line;
      while((line = in.readLine()) != null) {
        if (line.contains("GET /image/")) {
          writeToFile("image.log", line);
        } else if (line.contains("GET /product/")) {
          writeToFile("product.log", line);
        } else if (line.contains("GET /filter/")) {
          writeToFile("filter.log", line);
        }
      }
    } catch (IOException ioe) {
      LOGGER.warning("Check provided file, IOException occurred: " +  ioe);
    }
  }

  public void getSelectedTopics(int logSize) {
    Path path = Paths.get(filePath);
    try (BufferedReader in = Files.newBufferedReader(path)){
      String line;
      int count = 0;
      while((line = in.readLine()) != null) {
        if (count == logSize * 3) {
          break;
        }
        if (line.contains("GET /image/") || line.contains("GET /product/") || line.contains("GET /filter/")) {
          if (count < logSize) {
            writeToFile("demo1.log", line);
          } else if (count < logSize * 2) {
            writeToFile("demo2.log", line);
          } else if (count < logSize * 3) {
            writeToFile("demo3.log", line);
          }
          count++;
        }
      }
    } catch (IOException ioe) {
      LOGGER.warning("Check provided file, IOException occurred: " +  ioe);
    }
  }

  /**
   * Write string to file
   *
   * @param fileName filename
   * @param log string line
   */
  public void writeToFile(String fileName, String log) {
    Path filePath = Path.of(fileName);
    if (!Files.exists(filePath)) {
      try {
        Files.writeString(filePath, log);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    try {
      Files.writeString(filePath, log + System.lineSeparator(), StandardOpenOption.APPEND);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
