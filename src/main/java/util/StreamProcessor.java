package util;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.logging.Logger;
import model.LogStream;

/**
 * A helper class to process stream
 * Data set web server access logs
 *
 * @link https://www.kaggle.com/eliasdabbas/web-server-access-logs
 * @author marisatania
 */
public class StreamProcessor {
  private final String streamFile;
  private final ArrayList<LogStream> streamList;
  private static final Logger LOGGER =
      Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

  /**
   * Constructor.
   *
   * @param streamFile file name
   */
  public StreamProcessor(String streamFile) {
    this.streamFile = streamFile;
    this.streamList = new ArrayList<>();
  }

  /**
   * Process log stream file
   */
  public ArrayList<LogStream> processStream() {
    String dir = System.getProperty(Constant.USER_DIR);
    Path path;

    path = Paths.get(dir + Constant.SLASH + streamFile);

    try (BufferedReader br = Files.newBufferedReader(path, StandardCharsets.UTF_8)){
      String line, hostName, topic;
      while ((line = br.readLine()) != null) {
        String[] lineSplit = line.split(Constant.SPACE);
        hostName = lineSplit[0];
        String[] urlSplit = lineSplit[6].split(Constant.SLASH);
        if (urlSplit.length == 0) {
          continue;
        }
        topic = urlSplit[1];
        line += "\n";
        byte[] data = line.getBytes(StandardCharsets.UTF_8);
        streamList.add(new LogStream(topic, hostName, data));
      }
    } catch (IOException exception) {
      LOGGER.warning("Error: exception while processing stream: " + exception.getMessage());
    }

    return streamList;
  }
}
