package util;

import static org.junit.jupiter.api.Assertions.*;
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

}