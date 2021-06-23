package at.uibk.dps.ee.control.verticles;

import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import net.sf.opendse.model.Element;

/**
 * Parent class for the handlers used to react to Apollo-internal messages.
 * 
 * @author Fedor Smirnov
 *
 */
public abstract class HandlerApollo<E extends Element> implements Handler<Message<String>> {

  protected final String triggerAddress;
  protected final String successAddress;
  protected final String failureAddress;

  protected final EventBus eBus;

  /**
   * Standard constructor
   * 
   * @param triggerAddress the address triggering the handler.
   * @param successAddress the address used to report success
   * @param failureAddress the address used to report failures
   * @param eBus the vertX event bus
   */
  public HandlerApollo(String triggerAddress, String successAddress, String failureAddress,
      EventBus eBus) {
    this.triggerAddress = triggerAddress;
    this.successAddress = successAddress;
    this.failureAddress = failureAddress;
    this.eBus = eBus;
  }

  @Override
  public void handle(Message<String> event) {
    E graphElement = readMessage(event.body());
    try {
      work(graphElement);
    } catch (WorkerException wExc) {
      eBus.publish(failureAddress, wExc.getMessage());
    }
  }


  public String getTriggerAddress() {
    return triggerAddress;
  }

  /**
   * Parses the event bus message to a processable element
   * 
   * @param message
   * @return the processable element
   */
  protected abstract E readMessage(String message);

  /**
   * Performs the work on the provided graph element
   * 
   * @param graphElement
   * @return
   * @throws WorkerException
   */
  protected abstract void work(E graphElement) throws WorkerException;
}
