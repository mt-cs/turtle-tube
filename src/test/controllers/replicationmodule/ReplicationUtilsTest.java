package controllers.replicationmodule;

import static org.junit.jupiter.api.Assertions.*;

import controllers.pubsubframework.PubSubUtils;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import model.MsgInfo.Message;
import org.junit.jupiter.api.Test;
import util.Constant;

class ReplicationUtilsTest {
  @Test
  public void testMergeTopicMap() {
    Message msg3 = Message.newBuilder()
        .setMsgId(3)
        .build();
    ConcurrentHashMap <String, List<Message>> topicMap = new ConcurrentHashMap<>();
    List<Message> listTopic = new CopyOnWriteArrayList<>();
    listTopic.add(msg3);
    topicMap.put("image", listTopic);

    Message msg1 = Message.newBuilder()
        .setMsgId(1)
        .build();
    Message msg2 = Message.newBuilder()
        .setMsgId(2)
        .build();
    Message msg4 = Message.newBuilder()
        .setMsgId(4)
        .build();
    List<Message> listTopicImg = new CopyOnWriteArrayList<>();
    listTopicImg.add(msg1);
    listTopicImg.add(msg2);

    List<Message> listTopicPrice = new CopyOnWriteArrayList<>();
    listTopicPrice.add(msg4);

    ConcurrentHashMap <String, List<Message>> topicMapCatchUp = new ConcurrentHashMap<>();
    topicMapCatchUp.put("image", listTopicImg);
    topicMapCatchUp.put("price", listTopicPrice);

    StringBuilder sb = new StringBuilder();
    topicMap.forEach((key, value) -> sb.append(key).append(" ").append(value.toString()));
    assertEquals("image [msgId: 1\n"
        + ", msgId: 2\n"
        + ", msgId: 3\n"
        + "]price [msgId: 4\n"
        + "]", sb.toString());
  }

  @Test
  public void testCopyTopicMap() {
    Map<String, List<String>> topicMap = new ConcurrentHashMap<>();
    List<String> listTopic = new CopyOnWriteArrayList<>();
    listTopic.add("image");
    listTopic.add("product");
    listTopic.add("filter");
    listTopic.add("news");
    topicMap.put("first", listTopic);

    Map<String, List<String>> currentTopicMap = topicMap.entrySet().stream()
        .collect(Collectors.toMap(e -> e.getKey(), e -> List.copyOf(e.getValue())));

    StringBuilder sb = new StringBuilder();

    topicMap.get("first").add("hello");
    currentTopicMap.entrySet().forEach(entry -> {
      sb.append(entry.getKey() + " " + entry.getValue());
    });
    assertEquals("first [image, product, filter, news]", sb.toString());
  }

  @Test
  public void testGetFileSize() {
    System.out.println(new File("log/demo1.log").length());
  }

  @Test
  public void testGetCopyFIle() {
    String[] lineSplit = "broker1_image_offset.log".split(Constant.OFFSET_LOG);
    System.out.println(lineSplit[0]);
  }

  @Test
  public void testUrl() {
    String line = "54.36.149.41 - - [22/Jan/2019:03:56:14 +0330] \"GET /filter/27|13%20%D9%85%DA%AF%D8%A7%D9%BE%DB%8C%DA%A9%D8%B3%D9%84,27|%DA%A9%D9%85%D8%AA%D8%B1%20%D8%A7%D8%B2%205%20%D9%85%DA%AF%D8%A7%D9%BE%DB%8C%DA%A9%D8%B3%D9%84,p53 HTTP/1.1\" 200 30577 \"-\" \"Mozilla/5.0 (compatible; AhrefsBot/6.1; +http://ahrefs.com/robot/)\" \"-\"54.36.149.41 - - [22/Jan/2019:03:56:14 +0330] \"GET /filter/27|13%20%D9%85%DA%AF%D8%A7%D9%BE%DB%8C%DA%A9%D8%B3%D9%84,27|%DA%A9%D9%85%D8%AA%D8%B1%20%D8%A7%D8%B2%205%20%D9%85%DA%AF%D8%A7%D9%BE%DB%8C%DA%A9%D8%B3%D9%84,p53 HTTP/1.1\" 200 30577 \"-\" \"Mozilla/5.0 (compatible; AhrefsBot/6.1; +http://ahrefs.com/robot/)\" \"-\"\n";
    String[] lineSplit = line.split(Constant.SPACE);
    String[] urlSplit = lineSplit[6].split(Constant.SLASH);
    System.out.println(Arrays.toString(urlSplit));
    if (urlSplit.length <= 1) {
      System.out.println("ERR");
    }
    System.out.println(urlSplit[1]);
  }
}