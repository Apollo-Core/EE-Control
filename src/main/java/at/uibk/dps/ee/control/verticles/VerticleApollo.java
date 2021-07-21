package at.uibk.dps.ee.control.verticles;

import java.util.ArrayList;
import java.util.List;
import at.uibk.dps.ee.model.graph.EnactmentGraph;
import at.uibk.dps.ee.model.graph.EnactmentGraphProvider;
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

  protected boolean paused = false;
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
      final String failureAddress, EnactmentGraphProvider eProvider) {
    this.triggerAddress = triggerAddress;
    this.successAddress = successAddress;
    this.failureAddress = failureAddress;
    this.eGraph = eProvider.getEnactmentGraph();
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
    if (!paused) {
      processTask(triggerTask);
    } else {
      queue.add(triggerTask);
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
      this.vertx.eventBus().publish(failureAddress, wExc.getMessage());
    }
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
