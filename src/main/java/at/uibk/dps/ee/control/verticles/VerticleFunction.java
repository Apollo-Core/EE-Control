package at.uibk.dps.ee.control.verticles;

import java.util.AbstractMap.SimpleEntry;
import java.util.Set;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.inject.Inject;
import at.uibk.dps.ee.core.CoreFunction;
import at.uibk.dps.ee.guice.HandlerString;
import at.uibk.dps.ee.guice.starter.VertxProvider;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;

/**
 * Intermediate class to get the VertX-based procedure going. To be removed soon thereafter.
 * 
 * @author Fedor Smirnov
 *
 */
public class VerticleFunction implements CoreFunction{

  protected final InputDataHandler dataHandler;
  protected final Promise<JsonObject> resultPromise;
  
  protected final EventBus eBus;
  
  @Inject
  public VerticleFunction(InputDataHandler dataHandler, VertxProvider vertxProvider, Set<HandlerString> stringHandlers) {
    super();
    this.dataHandler = dataHandler;
    this.resultPromise = Promise.promise();
    this.eBus = vertxProvider.geteBus();
    eBus.consumer(ConstantsEventBus.addressWorkflowResultAvailable, resultMessage ->{
      String messageString = resultMessage.body().toString();
      JsonObject jsonResult = (JsonObject) JsonParser.parseString(messageString);
      resultPromise.complete(jsonResult);
    });
    
    for (HandlerString stringhandler : stringHandlers) {
      eBus.consumer(stringhandler.getTriggerAddress(), stringhandler);
    }
    
  }

  @Override
  public Future<JsonObject> processInput(JsonObject input) {
    
    dataHandler.processInput(input);
    
    return resultPromise.future();
  }

  @Override
  public String getTypeId() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getEnactmentMode() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getImplementationId() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Set<SimpleEntry<String, String>> getAdditionalAttributes() {
    // TODO Auto-generated method stub
    return null;
  }

  
  
}
