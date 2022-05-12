package controllers.replicationmodule;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import model.MsgInfo.Message;
import org.junit.jupiter.api.Test;

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
}