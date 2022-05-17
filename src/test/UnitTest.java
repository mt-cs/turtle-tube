//package controllers.replicationframework;
//
//import org.junit.jupiter.api.Assertions;
//import org.junit.jupiter.api.Test;
//
//public class UnitTest {
//  @Test
//  public void brokerAppTestIsValidArgs() {
//    String[] args = new String[] {"-address:", "localhost:8080", "-port:", "8081", "-isleader:", "true", "-brokerid:", "3"};
//    BrokerApp brokerApp = new BrokerApp();
//    boolean isValid = brokerApp.isValidArgs(args);
//    Assertions.assertTrue(isValid);
//  }
//
//  /**
//   * Wrong address
//   */
//  @Test
//  public void brokerAppTestIsNotValidArgs() {
//    String[] args = new String[] {"-address:", "localhost", "-port", "8081"};
//    BrokerApp brokerApp = new BrokerApp();
//    boolean isValid = brokerApp.isValidArgs(args);
//    Assertions.assertFalse(isValid);
//  }
//
//  @Test
//  public void consumerAppTestIsValidArgs() {
//    String[] args = new String[] {"-address:", "localhost:8080", "-file:",
//        "testtttt.txt", "-topic:", "topic", "-position:", "3"};
//    ConsumerApp consumerApp = new ConsumerApp();
//    boolean isValid = consumerApp.isValidArgs(args);
//    Assertions.assertTrue(isValid);
//  }
//
//  /**
//   * Output File already exist
//   */
//  @Test
//  public void consumerAppTestIsNotValidArgs() {
//    String[] args = new String[] {"-address:", "localhost:8080", "-file:",
//        "system_test.txt", "-topic:", "topic", "-position:", "3"};
//    ConsumerApp consumerApp = new ConsumerApp();
//    boolean isValid = consumerApp.isValidArgs(args);
//    Assertions.assertFalse(isValid);
//  }
//
//  @Test
//  public void loadBalancerAppTestIsValidArgs() {
//    String[] args = new String[] {"-port:", "8080"};
//    LoadBalancerApp loadBalancerApp = new LoadBalancerApp();
//    boolean isValid = loadBalancerApp.isValidArgs(args);
//    Assertions.assertTrue(isValid);
//  }
//
//  /**
//   * Invalid flag
//   */
//  @Test
//  public void loadBalancerAppTestIsNotValidArgs() {
//    String[] args = new String[] {"-ports:", "8080"};
//    LoadBalancerApp loadBalancerApp = new LoadBalancerApp();
//    boolean isValid = loadBalancerApp.isValidArgs(args);
//    Assertions.assertFalse(isValid);
//  }
//
//  @Test
//  public void producerAppTestIsValidArgs() {
//    String[] args = new String[] {"-address:", "localhost:8080", "-file:", "system_test.txt"};
//    ProducerApp producerApp = new ProducerApp();
//    boolean isValid = producerApp.isValidArgs(args);
//    Assertions.assertTrue(isValid);
//  }
//
//  /**
//   * Missing an argument
//   */
//  @Test
//  public void producerAppTestIsNotValidArgs() {
//    String[] args = new String[] {"-address:", "localhost:8080", "-file:"};
//    ProducerApp producerApp = new ProducerApp();
//    boolean isValid = producerApp.isValidArgs(args);
//    Assertions.assertFalse(isValid);
//  }
//}