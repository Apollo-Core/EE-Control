package at.uibk.dps.ee.control.verticles;

import java.util.Set;
import io.vertx.core.AbstractVerticle;
import net.sf.opendse.model.Element;

/**
 * The Apollo verticles. The agents are implemented as asynchronous handlers of
 * messages exchanged over the event bus.
 * 
 * @author Fedor Smirnov
 */
public class VerticleApollo extends AbstractVerticle {

  protected final Set<HandlerApollo<? extends Element>> handlers;
  
  public VerticleApollo(final Set<HandlerApollo<? extends Element>> handlers) {
    this.handlers = handlers;
  }
  
  @Override
    public void start() throws Exception {
      handlers.forEach(handler -> {
        this.vertx.eventBus().consumer(handler.getTriggerAddress(), handler);
      });
    }
}
