package util;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

class ReplicationCmdLineParserTest {

  /**
   * Validates getHosts
   */
  @Test
  public void testGetOffsetFile() {
    String logFile = "consumer1.log";
    String[] logArr = logFile.split("\\.");
    String offsetFile = logArr[0] + "_offset.log";
    assertEquals("consumer1_offset.log", offsetFile);
  }

  /**
   * Validates getHosts
   */
  @Test
  public void testGetTopicFromFile() {
    String logFile = "broker2_image_offset.log";
    String result = logFile.substring(logFile.lastIndexOf('_') + 1, logFile.lastIndexOf('.'));
    assertEquals("offset", result);
  }

}