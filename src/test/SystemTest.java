//package controllers.replicationframework;
//
//import org.junit.jupiter.api.Assertions;
//import org.junit.jupiter.api.Test;
//
//import java.nio.charset.StandardCharsets;
//
//public class SystemTest {
//
//  @Test
//  /**
//   * broker to load balancer
//   */
//  public void runProgram() throws InterruptedException {
//    String loadBalancerAddress = "localhost:8090";
//    int loadBalancerPort = 8090;
//    int brokerPort = 8091;
//
//    Thread loadBalancerThread = new Thread( () -> {
//      LoadBalancer lb = new LoadBalancer(loadBalancerPort);
//      lb.start();
//    });
//
//    loadBalancerThread.start();
//
//    Thread.sleep(2000);
//
//    Thread brokerThread = new Thread( () -> {
//      Broker broker = new Broker(1, brokerPort, loadBalancerAddress, true);
//      broker.start();
//    });
//
//    brokerThread.start();
//
//    Thread.sleep(2000);
//
//    Thread producerThread = new Thread( () -> {
//      Producer producer = new Producer(loadBalancerAddress);
//      producer.send("topic", "test".getBytes(StandardCharsets.UTF_8));
//    });
//
//    producerThread.start();
//
//    Thread.sleep(2000);
//
//    Consumer consumer = new Consumer(loadBalancerAddress, "topic", 0);
//
//    String actual = "";
//    String expected = "test";
//    while (true) {
//      DataContainer.Data data = consumer.poll(1);
//      if (data != null) {
//        actual = data.getData().toStringUtf8();
//        System.out.println(actual);
//        break;
//      }
//    }
//
//    Assertions.assertEquals(expected, actual);
//  }
//
//  @Test
//  /**
//   * get data from follower
//   */
//  public void getDataFromFollower() throws InterruptedException {
//    String loadBalancerAddress = "localhost:8093";
//    int loadBalancerPort = 8093;
//    int brokerPort = 8094;
//    int brokerPortReplication = 8096;
//
//    Thread loadBalancerThread = new Thread( () -> {
//      LoadBalancer lb = new LoadBalancer(loadBalancerPort);
//      lb.start();
//    });
//
//    loadBalancerThread.start();
//
//    Thread.sleep(2000);
//
//    Thread brokerThread = new Thread( () -> {
//      Broker broker = new Broker(1, brokerPort, loadBalancerAddress, true);
//      broker.start();
//    });
//
//    brokerThread.start();
//
//    Thread broker2Thread = new Thread( () -> {
//      Broker broker = new Broker(1, brokerPortReplication, loadBalancerAddress, false);
//      broker.start();
//    });
//
//    broker2Thread.start();
//
//    Thread.sleep(2000);
//
//    Thread producerThread = new Thread( () -> {
//      Producer producer = new Producer(loadBalancerAddress);
//      producer.send("topic", "test".getBytes(StandardCharsets.UTF_8));
//    });
//
//    producerThread.start();
//
//    Thread.sleep(2000);
//
//    Consumer consumer = new Consumer(loadBalancerAddress, "topic", 0);
//
//    String actual = "";
//    String expected = "test";
//    while (true) {
//      DataContainer.Data data = consumer.poll(1);
//      if (data != null) {
//        actual = data.getData().toStringUtf8();
//        System.out.println(actual);
//        break;
//      }
//    }
//
//    Assertions.assertEquals(expected, actual);
//  }
//
//}