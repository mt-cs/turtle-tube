//package controllers.pubsubframework;
//
//import static org.junit.jupiter.api.Assertions.*;
//
//import com.google.protobuf.ByteString;
//import constant.TestConstants;
//import controllers.pubsubframework.Producer;
//import model.MsgInfo.Message;
//import org.junit.jupiter.api.Test;
//
//class ProducerTest {
//  Producer producer = new Producer("localhost:1031");
//
//  @Test
//  public void testConnectBroker() {
//    producer.connectToBroker();
//  }
//
//  @Test
//  public void testSend() {
//    producer.connectToBroker();
//    Message msg = Message.newBuilder()
//        .setTypeValue(0)
//        .setSrcId("producer1")
//        .setTopic("image")
//        .setOffset(1)
//        .setData(ByteString.copyFromUtf8(TestConstants.SAMPLE_LOG))
//        .build();
//    producer.send(TestConstants.TOPIC, msg.toByteArray());
//  }
//
//}