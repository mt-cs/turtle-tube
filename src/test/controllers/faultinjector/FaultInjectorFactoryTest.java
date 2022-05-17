package controllers.faultinjector;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Test controllers.fault injector
 *
 * @author marisatania
 */
class FaultInjectorFactoryTest {

  /**
   * Test controllers.fault injector factory default.
   */
  @Test
  public void testCreateFaultInjector() {
    FaultInjectorFactory faultInjectorFactory = new FaultInjectorFactory(0);
    assertNotNull(faultInjectorFactory.getChaos());
  }

  /**
   * Test delay injector.
   */
  @Test
  public void testCreateDelayInjector() {
    DelayInjector delayInjector = new DelayInjector(1000);
    assertTrue(delayInjector.shouldFail());
  }

  /**
   * Test default should fail.
   */
  @Test
  public void testCreateDefaultInjectorShouldFail() {
    FaultInjectorFactory faultInjectorFactory = new FaultInjectorFactory(0);
    assertFalse(faultInjectorFactory.getChaos().shouldFail());
  }

}