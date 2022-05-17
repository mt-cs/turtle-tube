//package controllers.pubsubframework;
//
//import static org.junit.jupiter.api.Assertions.*;
//
//import com.google.protobuf.ByteString;
//import constant.TestConstants;
//import java.nio.charset.StandardCharsets;
//import java.time.Duration;
//import model.MsgInfo.Message;
//import org.junit.jupiter.api.Test;
//import util.Constant;
//
//class ConsumerTest {
//
//  Consumer consumer = new Consumer("localhost:1031", TestConstants.TOPIC, 20);
//
//  @Test
//  public void testPoll() {
//    consumer.connectToBroker();
//    Message msg = Message.newBuilder()
//        .setTypeValue(0)
//        .setSrcId("producer1")
//        .setTopic("image")
//        .setOffset(1)
//        .setData(ByteString.copyFromUtf8(TestConstants.SAMPLE_LOG))
//        .build();
//    assertEquals(msg.getData().toStringUtf8(), new String(consumer.poll(Duration.ofMillis(Constant.POLL_TIMEOUT)), StandardCharsets.UTF_8));
//  }
//
//  @Test
//  public void testReceiveFromBroker() {
//    consumer.connectToBroker();
//    consumer.receiveFromBroker();
//  }
//
//
//
//}