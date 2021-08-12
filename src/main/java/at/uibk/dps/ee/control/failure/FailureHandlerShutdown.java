package at.uibk.dps.ee.control.failure;

import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import at.uibk.dps.ee.control.verticles.ConstantsVertX;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;

/**
 * The {@link FailureHandlerShutdown} reacts to failures by shutting down the
 * enactment.
 * 
 * @author Fedor Smirnov
 *
 */
@Singleton
public class FailureHandlerShutdown extends FailureHandler {

  /**
   * Injection constructor.
   */
  @Inject
  public FailureHandlerShutdown() {
    super(ConstantsVertX.addressFailureAbort);
  }

  @Override
  public void handleFailure(String failureMessage, Promise<JsonObject> resultPromise, Vertx vertx) {
    resultPromise.tryFail(failureMessage);
    vertx.close();
  }
}
