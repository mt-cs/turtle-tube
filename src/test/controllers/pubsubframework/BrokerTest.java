//package controllers;
//
//import static org.junit.jupiter.api.Assertions.*;
//
//import com.google.protobuf.ByteString;
//import constant.TestConstants;
//import controllers.pubsubframework.Broker;
//import java.nio.charset.StandardCharsets;
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.List;
//import java.util.Objects;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.concurrent.ConcurrentLinkedQueue;
//import model.MsgInfo;
//import model.MsgInfo.Message;
//import org.junit.jupiter.api.Test;
//
///**
// * Broker Test
// *
// * @author marisatania
// */
//class BrokerTest {
//  Broker broker = new Broker(TestConstants.port);
//
//  @Test
//  public void testCheckTopicMapTrue() {
//    List<Message> topicList = getTopicLists();
//    ConcurrentHashMap<String, List<Message>> topicMap = new ConcurrentHashMap<>();
//    topicMap.put("image", topicList);
//    assertTrue(broker.checkTopicMapContainsRequest(topicMap,2, "image"));
//  }
//
//  @Test
//  public void testCheckTopicMapFalseTopic() {
//    List<Message> topicList = getTopicLists();
//    ConcurrentHashMap<String, List<Message>> topicMap = new ConcurrentHashMap<>();
//    topicMap.put("image", topicList);
//    assertFalse(broker.checkTopicMapContainsRequest(topicMap,2, "product"));
//  }
//
//  @Test
//  public void testCheckTopicMapFalseStartPos() {
//    List<Message> topicList = getTopicLists();
//    ConcurrentHashMap<String, List<Message>> topicMap = new ConcurrentHashMap<>();
//    topicMap.put("image", topicList);
//    assertFalse(broker.checkTopicMapContainsRequest(topicMap,11, "image"));
//  }
//
//  public List<Message> getTopicLists() {
//    List<Message> topicList = Collections.synchronizedList(new ArrayList<>());
//    Message msg;
//    for (int i = 0; i < 10; i++) {
//      msg = Message.newBuilder()
//          .setTypeValue(0)
//          .setSrcId("producer" + i)
//          .setTopic("image")
//          .setOffset(289)
//          .setData(ByteString.copyFromUtf8(TestConstants.SAMPLE_LOG))
//          .build();
//      topicList.add(msg);
//    }
//    return topicList;
//  }
//
//
//}