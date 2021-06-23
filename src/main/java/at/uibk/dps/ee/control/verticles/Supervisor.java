package at.uibk.dps.ee.control.verticles;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.eventbus.EventBus;
import net.sf.opendse.model.Element;

public abstract class Supervisor<E extends Element> extends AbstractVerticle {

  protected final VerticleFactory<E> factory;
  protected final EventBus eBus;
  protected final String triggerAddress;
  protected final String successAddress;

  public Supervisor(VerticleFactory<E> factory, EventBus eBus, String triggerAddress,
      String successAddress) {
    this.factory = factory;
    this.eBus = eBus;
    this.triggerAddress = triggerAddress;
    this.successAddress = successAddress;
  }

  @Override
  public void start() throws Exception {
    super.start();
    eBus.consumer(triggerAddress, message -> {
      processTriggerMessage(message.body().toString());
    });

  }

  /**
   * Processes the trigger message.
   * 
   * @param message the trigger message
   */
  protected void processTriggerMessage(String message) {
    for (Worker<E> worker : factory.getWorkers(message)) {
      DeploymentOptions options = new DeploymentOptions().setWorker(worker.isBlocking());
      this.getVertx().deployVerticle(worker, options).onComplete(res -> {
        System.out.println("worker deployed");
        eBus.publish(successAddress, res);
      });
    }
  }
}
