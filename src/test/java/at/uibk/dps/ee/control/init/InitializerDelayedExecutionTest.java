package at.uibk.dps.ee.control.init;

import static org.junit.jupiter.api.Assertions.*;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import at.uibk.dps.ee.guice.starter.VertxProvider;
import io.vertx.core.Vertx;

class InitializerDelayedExecutionTest {

  VertxProvider vProv;

  @Test
  void testNoDelay() {
    InitializerDelayedExecution tested = new InitializerDelayedExecution(vProv, 0);
    Instant start = Instant.now();
    CountDownLatch latch = new CountDownLatch(1);
    tested.initialize().onComplete(asyncRes -> {
      latch.countDown();
    });
    try {
      latch.await();
      Instant end = Instant.now();
      long duration = Duration.between(start, end).getSeconds();
      assertEquals(0, duration);
    } catch (InterruptedException e) {
      fail();
    }
  }

  @Test
  void testDelay() {
    InitializerDelayedExecution tested = new InitializerDelayedExecution(vProv, 1);
    Instant start = Instant.now();
    CountDownLatch latch = new CountDownLatch(1);
    tested.initialize().onComplete(asyncRes -> {
      latch.countDown();
    });
    try {
      latch.await();
      Instant end = Instant.now();
      long duration = Duration.between(start, end).getSeconds();
      assertEquals(1, duration);
    } catch (InterruptedException e) {
      fail();
    }
  }

  @BeforeEach
  void setup() {
    Vertx vertx = Vertx.vertx();
    vProv = new VertxProvider(vertx);
  }

}
