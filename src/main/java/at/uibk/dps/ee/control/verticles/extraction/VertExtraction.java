package at.uibk.dps.ee.control.verticles.extraction;

import java.util.Map;
import at.uibk.dps.ee.model.graph.EnactmentGraph;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;

public class VertExtraction extends AbstractVerticle {

  protected final EnactmentGraph eGraph;
  protected final EventBus eBus;
  protected final Map<String, Handler<Message<String>>> handlerMap;

  public VertExtraction(EnactmentGraph eGraph, EventBus eBus,
      Map<String, Handler<Message<String>>> handlerMap) {
    this.eGraph = eGraph;
    this.eBus = eBus;
    this.handlerMap = handlerMap;
  }

  @Override
  public void start() throws Exception {
    
    handlerMap.forEach((address, handler) -> {
      eBus.consumer(address, handler);
    });
    
  }

}
