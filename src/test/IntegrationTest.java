//import controllers.applicationframework.replicationapp.BrokerReplicationApp;
//import controllers.applicationframework.replicationapp.LoadBalancerApp;
//import controllers.applicationframework.replicationapp.ProducerReplicationApp;
//import controllers.pubsubframework.ProducerReplication;
//import model.config.BrokerConfig;
//import model.config.LoadBalancerConfig;
//import model.config.ReplicationConfig;
//import org.junit.jupiter.api.Assertions;
//import org.junit.jupiter.api.Test;
//import util.Constant;
//
//public class IntegrationTest {
//  @Test
//  /**
//   * producer able to get the correct address
//   */
//  public void producerGetLeaderAddress() throws InterruptedException {
//    String loadBalancerAddress = "localhost:8090";
//    int loadBalancerPort = 8090;
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
//      Broker broker = new Broker(1, 8091, loadBalancerAddress, true);
//      broker.start();
//    });
//
//    brokerThread.start();
//
//    Thread.sleep(2000);
//
//    Producer producer = new Producer(loadBalancerAddress);
//    String leaderAddress = producer.getLeaderAddress();
//    Server server = new Server(8092);
//    String expected = server.getHostname().split(":")[0] + ":8091";
//    System.out.println(leaderAddress);
//
//    Assertions.assertEquals(expected, leaderAddress);
//  }
//
//  /**
//   * connect broker to load balancer
//   */
//  @Test
//  public void connectBrokerToLoadBalancerThroughApp() throws InterruptedException {
//    String loadBalancerAddress = "localhost:8092";
//
//    Thread loadBalancerThread = new Thread( () -> {
//      String[] args = new String[] {"-port:", "8092"};
//      LoadBalancerApp loadBalancerApp = new LoadBalancerApp();
//      loadBalancerApp.startApp(args);
//    });
//
//    loadBalancerThread.start();
//
//    Thread.sleep(2000);
//
//    String[] args = new String[] {"-address:", loadBalancerAddress, "-port:", "8093"};
//    BrokerApp brokerApp = new BrokerApp();
//
//    // Broker successfully executed and connected to load balancer
//    Assertions.assertTrue(brokerApp.startApp(args));
//  }
//
//  /**
//   * connect producer to load balancer
//   */
//  @Test
//  public void connectProducerToLoadBalancerThroughApp() throws InterruptedException {
//    String loadBalancerAddress = "localhost:8094";
//
//    Thread loadBalancerThread = new Thread( () -> {
//      String[] args = new String[] {"-port:", "8094"};
//      LoadBalancerApp loadBalancerApp = new LoadBalancerApp();
//      loadBalancerApp.startApp(args);
//    });
//
//    loadBalancerThread.start();
//
//    Thread.sleep(2000);
//
//    String[] args = new String[] {"-address:", loadBalancerAddress, "-file:", "system_test.txt"};
//    ProducerApp producerApp = new ProducerApp();
//
//    // Producer successfully executed and connected to load balancer
//    Assertions.assertTrue(producerApp.startApp(args));
//  }
//
//  @Test
//  /**
//   * connect consumer to load balancer
//   */
//  public void connectConsumerToLoadBalancerThroughApp() throws InterruptedException {
//    String loadBalancerAddress = "localhost:1234";
//
//    Thread loadBalancerThread = new Thread( () -> {
//      String[] arr = new String[] {"port:", "1234"};
//      LoadBalancerApp loadBalancerApp = new LoadBalancerApp();
//      loadBalancerApp.runLoadBalancerApplication(new LoadBalancerConfig(Constant.LOADBALANCER, 1234));
//    });
//
//    loadBalancerThread.start();
//
//    Thread.sleep(2000);
//
//    Thread brokerThread = new Thread( () -> {
//      String[] args = new String[] {"-address:", loadBalancerAddress, "-port:", "8096"};
//      BrokerReplicationApp brokerApp = new BrokerReplicationApp();
//      brokerApp.runBrokerApplication(new BrokerConfig());
//    });
//
//    brokerThread.start();
//
//    Thread.sleep(2000);
//
//    Thread producerThread = new Thread( () -> {
//      String[] args = new String[] {"-address:", loadBalancerAddress, "-file:", "system_test.txt"};
//      ProducerReplicationApp producerApp = new ProducerReplicationApp());
//      producerApp.runProducerApplication(args);
//    });
//
//    producerThread.start();
//
//    Thread.sleep(2000);
//
//    String[] args = new String[] {"-address:", loadBalancerAddress, "-file:",
//        "test.txt", "-topic:", "image", "-position:", "0"};
//    ConsumerApp consumerApp = new ConsumerApp();
//    boolean isValid = consumerApp.startApp(args);
//    System.out.println("isValid: " + isValid);
//
//    Assertions.assertTrue(isValid);
//  }
//
//}