package at.uibk.dps.ee.control.verticles;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import at.uibk.dps.ee.guice.starter.VertxProvider;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.any;

class VerticleFunctionTest {

  VerticleFunction tested;

  InputDataHandler inputHandler;
  VerticleManager verticleManager;
  Vertx vertx;
  EventBus eBus;
  Promise<JsonObject> resultPromise;
  
  JsonObject result;

  @SuppressWarnings("unchecked")
  @BeforeEach
  public void setup() {
    result = new JsonObject();
    result.add("result", new JsonPrimitive(42));
    
    inputHandler = mock(InputDataHandler.class);
    verticleManager = mock(VerticleManager.class);
    vertx = mock(Vertx.class);
    eBus = mock(EventBus.class);

    VertxProvider vProv = mock(VertxProvider.class);
    when(vProv.getVertx()).thenReturn(vertx);
    when(vProv.geteBus()).thenReturn(eBus);

    PromiseProvider pProv = mock(PromiseProvider.class);
    resultPromise = mock(Promise.class);
    when(pProv.getJsonPromise()).thenReturn(resultPromise);

    tested = new VerticleFunction(inputHandler, verticleManager, vProv, pProv);
  }

  /**
   * Test that we use the handler to process input.
   */
  @Test
  void testProcessInput() {
    tested.processInput(result);
    verify(inputHandler).processInput(result);
    verify(resultPromise).future();
  }

  /**
   * Test the result handler.
   */
  @Test
  void testResultHandler() {
    @SuppressWarnings("unchecked")
    Message<String> mockMessage = mock(Message.class);
    when(mockMessage.body()).thenReturn(result.toString());
    tested.currentPromise = tested.promiseProvider.getJsonPromise();
    tested.resultHandler(mockMessage);
    verify(resultPromise).complete(any(JsonObject.class));
  }

  /**
   * Test that the function returns all the right log constants
   */
  @Test
  void testLogConstants() {
    assertEquals(ConstantsVertX.typeId, tested.getTypeId());
    assertEquals(ConstantsVertX.enactmentMode, tested.getEnactmentMode());
    assertEquals(ConstantsVertX.implId, tested.getImplementationId());
    assertTrue(tested.getAdditionalAttributes().isEmpty());
  }
}
