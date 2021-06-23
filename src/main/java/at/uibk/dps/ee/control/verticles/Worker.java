package at.uibk.dps.ee.control.verticles;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import net.sf.opendse.model.Element;

public abstract class Worker<E extends Element> extends AbstractVerticle {

  protected final E triggerElement;
  
  public Worker(E triggerElement) {
    this.triggerElement = triggerElement;
  }
  
  /**
   * Returns true if the current worker is blocking.
   * 
   * @return true iff the current worker is blocking
   */
  public abstract boolean isBlocking();
  
  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    
  }
  
  /**
   * Performs the worker operation (asynchronously).
   */
  protected abstract void executeElementOperation() throws Exception;

}
