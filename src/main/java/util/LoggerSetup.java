package util;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Code example from CS601
 *
 * src: https://www.vogella.com/tutorials/Logging/article.html
 */
public class LoggerSetup {

  public static void setup(String fileName) {
    Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    logger.setLevel(Level.INFO);

    FileHandler fileOut;
    try {
      fileOut = new FileHandler(fileName);
      fileOut.setFormatter(new SimpleFormatter());
      logger.addHandler(fileOut);
    } catch (SecurityException | IOException e) {
      logger.warning(e.getMessage());
    }
  }
}
