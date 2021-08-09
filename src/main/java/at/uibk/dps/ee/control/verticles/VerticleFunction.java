package at.uibk.dps.ee.control.verticles;

import java.util.AbstractMap.SimpleEntry;
import java.util.HashSet;
import java.util.Set;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.inject.Inject;
import at.uibk.dps.ee.core.CoreFunction;
import at.uibk.dps.ee.guice.starter.VertxProvider;
import io.vertx.core.Future;
import io.vertx.core.Promise;
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
  protected final PromiseProvider promiseProvider;
  protected Promise<JsonObject> currentPromise;
  protected final EventBus eBus;

  /**
   * Injection constructor.
   * 
   * @param dataHandler the handler to inject input data into the eBus
   * @param vManager the manager deploying the verticles
   * @param vProv vertX provider
   */
  @Inject
  public VerticleFunction(final InputDataHandler dataHandler, final VerticleManager vManager,
      final VertxProvider vProv, final PromiseProvider pProv) {
    super();
    this.vManager = vManager;
    this.dataHandler = dataHandler;
    this.eBus = vProv.geteBus();
    this.promiseProvider = pProv;
    eBus.consumer(ConstantsVertX.addressWorkflowResultAvailable, this::resultHandler);
    vManager.deployVerticles();
  }

  /**
   * Handler for the message sent when the enactment of the application is
   * finished.
   * 
   * @param resultMessage the result message.
   */
  protected void resultHandler(final Message<String> resultMessage) {
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
