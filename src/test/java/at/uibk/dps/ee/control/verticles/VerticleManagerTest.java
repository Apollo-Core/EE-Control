package at.uibk.dps.ee.control.verticles;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import at.uibk.dps.ee.guice.starter.VertxProvider;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import java.util.HashSet;
import java.util.Set;



class VerticleManagerTest {

  VerticleManager tested;

  Vertx vertX;

  VerticleApollo verticle1;
  VerticleApollo verticle2;

  int deploymentNumber = 2;

  @SuppressWarnings("unchecked")
  @Test
  void testCallBack() {
    int count = (int) tested.latch.getCount();
    AsyncResult<String> res = (AsyncResult<String>) mock(AsyncResult.class);
    tested.deployCallBack(res);
    assertEquals(count -1, tested.latch.getCount());
  }
  
  @SuppressWarnings("unchecked")
  @Test
  void testDeploy() {
    tested.deployVerticles();
    verify(vertX, times(deploymentNumber)).deployVerticle(eq(verticle1), any(Handler.class));
    verify(vertX, times(deploymentNumber)).deployVerticle(eq(verticle2), any(Handler.class));
  }

  @SuppressWarnings("unchecked")
  @BeforeEach
  void setup() {
    vertX = mock(Vertx.class);
    VertxProvider vProv = mock(VertxProvider.class);
    when(vProv.getVertx()).thenReturn(vertX);
    verticle1 = mock(VerticleApollo.class);
    verticle2 = mock(VerticleApollo.class);
    Set<VerticleApollo> verticles = new HashSet<>();
    verticles.add(verticle1);
    verticles.add(verticle2);
    tested  = new VerticleManager(verticles, deploymentNumber, vProv);
    doAnswer(new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        tested.latch.countDown();
        return null;
      }
    }).when(vertX).deployVerticle(any(VerticleApollo.class), any(Handler.class));
  }
}
