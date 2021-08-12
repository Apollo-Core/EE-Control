package at.uibk.dps.ee.control.failure;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.google.gson.JsonObject;
import at.uibk.dps.ee.control.verticles.ConstantsVertX;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class FailureHandlerShutdownTest {

  FailureHandlerShutdown tested;

  Promise<JsonObject> promise;
  Vertx vertx;
  String message;

  @Test
  void test() {
    assertEquals(ConstantsVertX.addressFailureAbort, tested.getTriggerAddress());
    tested.handleFailure(message, promise, vertx);
    verify(promise).tryFail(message);
    verify(vertx).close();
  }

  @SuppressWarnings("unchecked")
  @BeforeEach
  void setup() {
    tested = new FailureHandlerShutdown();
    promise = mock(Promise.class);
    vertx = mock(Vertx.class);
    message = "stuff failed";
  }
}
