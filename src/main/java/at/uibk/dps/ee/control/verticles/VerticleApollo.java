
package at.uibk.dps.ee.control.verticles;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import at.uibk.dps.ee.model.graph.EnactmentGraph;
import at.uibk.dps.ee.model.graph.MappingsConcurrent;
import at.uibk.dps.ee.model.graph.ResourceGraph;
import at.uibk.dps.ee.model.graph.SpecificationProvider;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.Message;
import net.sf.opendse.model.Task;

/**
 * Parent class for the verticles which perform (effectively) non-blocking
 * operations as part of the vertX event loop.
 * 
 * @author Fedor Smirnov
 *
 */
public abstract class VerticleApollo extends AbstractVerticle {

  protected final String triggerAddress;
  protected final String successAddress;
  protected final String failureAddress;

  protected final EnactmentGraph eGraph;
  protected final ResourceGraph rGraph;
  protected final MappingsConcurrent mappings;

  protected final Logger logger = LoggerFactory.getLogger(VerticleApollo.class);

  protected boolean paused;
  protected final List<Task> queue = new ArrayList<>();

  /**
   * Parent constructor
   * 
   * @param triggerAddress the address on the event bus to listen to
   * @param successAddress the address to write on in case of success
   * @param failureAddress the address to write to in case of failure
   * @param eProvider the enactment graph provider
   */
  public VerticleApollo(final String triggerAddress, final String successAddress,
      final String failureAddress, final SpecificationProvider specProv) {
    this.triggerAddress = triggerAddress;
    this.successAddress = successAddress;
    this.failureAddress = failureAddress;
    this.eGraph = specProv.getEnactmentGraph();
    this.rGraph = specProv.getResourceGraph();
    this.mappings = specProv.getMappings();
  }


  @Override
  public void start() throws Exception {
    this.vertx.eventBus().consumer(ConstantsVertX.addressControlPause, this::pauseHandler);
    this.vertx.eventBus().consumer(ConstantsVertX.addressControlResume, this::resumeHandler);
    this.vertx.eventBus().consumer(triggerAddress, this::processTaskTrigger);
  }

  /**
   * Processes the task trigger received via the event bus. Stores trigger in
   * queue if currently paused.
   * 
   * @param taskMessage the message containing the task ID
   */
  protected void processTaskTrigger(final Message<String> taskMessage) {
    final Task triggerTask = eGraph.getVertex(taskMessage.body());
    if (paused) {
      queue.add(triggerTask);
    } else {
      processTask(triggerTask);
    }
  }

  /**
   * Processes the trigger task, reporting failures if they occur.
   * 
   * @param triggerTask the trigger task
   */
  protected void processTask(final Task triggerTask) {
    try {
      work(triggerTask);
    } catch (WorkerException wExc) {
      logger.error("Worker Exception Encountered.", wExc);
      failureHandler(wExc);
    }
  }

  /**
   * Write a failure message to the event bus so that the verticle responsible to
   * failure treatment can take care of it.
   * 
   * @param cause the cause of the failure.
   */
  protected void failureHandler(final Throwable cause) {
    this.vertx.eventBus().publish(failureAddress, cause.getMessage());
  }

  /**
   * Resume handler
   * 
   * @param message empty message
   */
  protected void resumeHandler(final Message<String> message) {
    // process the triggers stored during the pause
    while (!queue.isEmpty()) {
      processTask(queue.remove(0));
    }
    paused = false;
  }

  /**
   * Pause handler
   * 
   * @param message emtpy message
   */
  protected void pauseHandler(final Message<String> message) {
    this.paused = true;
  }



  /**
   * Performs the work on the provided task
   * 
   * @param triggeringTask the task triggering the verticle action
   * @throws WorkerException
   */
  protected abstract void work(Task triggeringTask) throws WorkerException;

}
