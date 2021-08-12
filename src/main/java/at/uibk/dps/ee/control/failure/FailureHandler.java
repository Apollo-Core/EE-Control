package at.uibk.dps.ee.control.failure;

import com.google.gson.JsonObject;
import com.google.inject.ImplementedBy;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;

/**
 * Parent class for all {@link FailureHandler}s.
 * 
 * @author Fedor Smirnov
 */
@ImplementedBy(FailureHandlerShutdown.class)
public abstract class FailureHandler {

  protected final String triggerAddress;

  /**
   * Default constructor.
   * 
   * @param triggerAddress the address the handler reacts to.
   */
  public FailureHandler(final String triggerAddress) {
    this.triggerAddress = triggerAddress;
  }

  /**
   * Handles the failure described by the given message
   * 
   * @param failureMessage the failure message
   * @param resultPromise the current result promise
   * @param vertx the VertX context
   */
  public abstract void handleFailure(String failureMessage, Promise<JsonObject> resultPromise,
      Vertx vertx);

  public String getTriggerAddress() {
    return triggerAddress;
  }
}
