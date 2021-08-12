package at.uibk.dps.ee.control.verticles;

import java.util.AbstractMap.SimpleEntry;
import java.util.HashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.inject.Inject;
import at.uibk.dps.ee.control.failure.FailureHandler;
import at.uibk.dps.ee.core.CoreFunction;
import at.uibk.dps.ee.guice.starter.VertxProvider;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;

/**
 * Intermediate class to get the VertX-based procedure going. To be removed soon
 * thereafter.
 * 
 * @author Fedor Smirnov
 *
 */
public class VerticleFunction implements CoreFunction {

  protected final VerticleManager vManager;
  protected final InputDataHandler dataHandler;
  protected final FailureHandler failureHandler;
  protected final PromiseProvider promiseProvider;
  protected Promise<JsonObject> currentPromise;
  protected final EventBus eBus;
  protected final Vertx vertx;

  protected final Logger logger = LoggerFactory.getLogger(VerticleFunction.class);

  /**
   * Injection constructor.
   * 
   * @param dataHandler the handler to inject input data into the eBus
   * @param vManager the manager deploying the verticles
   * @param vProv vertX provider
   */
  @Inject
  public VerticleFunction(final InputDataHandler dataHandler, final VerticleManager vManager,
      final VertxProvider vProv, final PromiseProvider pProv, final FailureHandler failureHandler) {
    super();
    this.vManager = vManager;
    this.dataHandler = dataHandler;
    this.vertx = vProv.getVertx();
    this.eBus = vProv.geteBus();
    this.promiseProvider = pProv;
    this.failureHandler = failureHandler;
    eBus.consumer(ConstantsVertX.addressWorkflowResultAvailable, this::handleResult);
    eBus.consumer(failureHandler.getTriggerAddress(), this::handleFailure);
    vManager.deployVerticles();
  }

  /**
   * Handler for the message signaling a critical failure during the enactment.
   * 
   * @param failureMessage
   */
  protected void handleFailure(final Message<String> failureMessage) {
    logger.error("Handling failure with message {}", failureMessage.body());
    failureHandler.handleFailure(failureMessage.body(), currentPromise, vertx);
  }

  /**
   * Handler for the message sent when the enactment of the application is
   * finished.
   * 
   * @param resultMessage the result message.
   */
  protected void handleResult(final Message<String> resultMessage) {
    final JsonObject resultObject = JsonParser.parseString(resultMessage.body()).getAsJsonObject();
    currentPromise.complete(resultObject);
  }

  @Override
  public Future<JsonObject> processInput(final JsonObject input) {
    dataHandler.processInput(input);
    currentPromise = promiseProvider.getJsonPromise();
    return currentPromise.future();
  }

  @Override
  public String getTypeId() {
    return ConstantsVertX.typeId;
  }

  @Override
  public String getEnactmentMode() {
    return ConstantsVertX.enactmentMode;
  }

  @Override
  public String getImplementationId() {
    return ConstantsVertX.implId;
  }

  @Override
  public Set<SimpleEntry<String, String>> getAdditionalAttributes() {
    return new HashSet<>();
  }
}
