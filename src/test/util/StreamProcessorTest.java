package util;

import static org.junit.jupiter.api.Assertions.*;

import constant.TestConstants;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import model.LogStream;
import org.junit.jupiter.api.Test;

class StreamProcessorTest {
  public StreamProcessor streamProcessor = new StreamProcessor("sample.log");
  public ArrayList<LogStream> logStreams = streamProcessor.processStream();

  /**
   * Validates get process stream key
   */
  @Test
  public void testGetProcessStreamKey() {
    assertEquals("31.56.96.51", logStreams.get(1).getKey());
  }

  /**
   * Validates get process stream key
   */
  @Test
  public void testGetProcessStreamTopic() {
    assertEquals("image", logStreams.get(1).getTopic());
  }

  /**
   * Validates get process stream key
   */
  @Test
  public void testGetProcessStreamData() {
    String data = new String(logStreams.get(1).getData(), StandardCharsets.UTF_8);
    assertEquals(TestConstants.SAMPLE_LOG, data);
  }

}