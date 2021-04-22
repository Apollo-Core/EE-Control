package at.uibk.dps.ee.control.management;

import java.util.concurrent.LinkedBlockingQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import net.sf.opendse.model.Task;

/**
 * The {@link EnactmentQueues} captures the current state of the enactment
 * (within queues representing the state of different tasks) and manages the
 * access to the enactment graph.
 * 
 * @author Fedor Smirnov
 */
@Singleton
public class EnactmentQueues {

  protected final Logger queueLogger = LoggerFactory.getLogger(EnactmentQueues.class);

  protected final LinkedBlockingQueue<Task> launchableTasks;
  protected final LinkedBlockingQueue<Task> schedulableTasks;
  protected final LinkedBlockingQueue<Task> finishedTasks;
  protected final LinkedBlockingQueue<Task> availableData;
  protected final LinkedBlockingQueue<Task> awaitingTransform;

  /**
   * The injection constructor
   */
  @Inject
  public EnactmentQueues() {
    this.launchableTasks = new LinkedBlockingQueue<>();
    this.finishedTasks = new LinkedBlockingQueue<>();
    this.availableData = new LinkedBlockingQueue<>();
    this.schedulableTasks = new LinkedBlockingQueue<>();
    this.awaitingTransform = new LinkedBlockingQueue<>();
  }

  /**
   * Returns a function task which requires a graph transformation. Blocks if the
   * queue of transform tasks is empty.
   * 
   * @return a function task which is ready to be launched
   * @throws InterruptedException
   */
  public Task takeTransformTask() throws InterruptedException {
    return takeFromQueue(awaitingTransform);
  }

  /**
   * Puts a function node into the queue of tasks which require a graph
   * transformation.
   * 
   * @param functionTask the task which requires graph transformation
   */
  public void putTransformTask(final Task functionTask) {
    putInQueue(awaitingTransform, functionTask);
  }

  /**
   * Returns a function task which is ready to be launched. Blocks if the queue of
   * launchable tasks is empty.
   * 
   * @return a function task which is ready to be launched
   * @throws InterruptedException
   */
  public Task takeSchedulableTask() throws InterruptedException {
    return takeFromQueue(schedulableTasks);
  }

  /**
   * Puts a function node into the queue of tasks which can be launched.
   * 
   * @param functionTask the task which is ready to be launched
   */
  public void putSchedulableTask(final Task functionTask) {
    putInQueue(schedulableTasks, functionTask);
  }

  /**
   * Returns a data node with available data. This method blocks until data is
   * available.
   * 
   * @return a data node with content.
   * @throws InterruptedException
   */
  public Task takeAvailableData() throws InterruptedException {
    return takeFromQueue(availableData);
  }

  /**
   * Puts a data node into the queue with the available data.
   * 
   * @param dataNode the data node with available content
   */
  public void putAvailableData(final Task dataNode) {
    putInQueue(availableData, dataNode);
  }

  /**
   * Returns a task node which was executed, so that it is annotated with results.
   * 
   * @return a task node which finished its execution.
   * @throws InterruptedException
   */
  public Task takeFinishedTask() throws InterruptedException {
    return takeFromQueue(finishedTasks);
  }

  /**
   * Adds a finished task to the queue (Blocking should never happen here since we
   * do not have a queue capacity limit).
   * 
   * @param readyTask the finished task to be added to the queue.
   */
  public void putFinishedTask(final Task finishedTask) {
    putInQueue(finishedTasks, finishedTask);
  }

  /**
   * Returns a task node which is ready for execution or waits until the queue is
   * non-empty.
   * 
   * @return a task node which is ready to be executed.
   * @throws InterruptedException
   */
  public Task takeLaunchableTask() throws InterruptedException {
    return takeFromQueue(launchableTasks);
  }

  /**
   * Adds a ready task to the queue (Blocking should never happen here since we do
   * not have a queue capacity limit).
   * 
   * @param readyTask the task to be added to the queue.
   */
  public void putLaunchableTask(final Task readyTask) {
    putInQueue(launchableTasks, readyTask);
  }

  /**
   * Returns an element from the blocking queue. Can cause consumers to wait until
   * content is available.
   * 
   * @param <E> task or dependency
   * @param queue the blocking queue
   * @return an element from the blocking queue
   * @throws InterruptedException
   */
  protected Task takeFromQueue(final LinkedBlockingQueue<Task> queue) throws InterruptedException {
    final Task result = queue.take();
    queueLogger.debug("\nTaken\nStatus\n{}", queueStatus());
    return result;
  }

  /**
   * Generates the log string used for the logging of the queue status
   * 
   * @return the log string used for the logging of the queue status
   */
  protected String queueStatus() {
    StringBuffer buffer = new StringBuffer();
    buffer.append("Launchable tasks: ").append(launchableTasks.size()).append('\n');
    buffer.append("Finished tasks: ").append(finishedTasks.size()).append('\n');
    buffer.append("Schedulable tasks: ").append(schedulableTasks.size()).append('\n');
    buffer.append("Available Data: ").append(availableData.size()).append('\n');
    buffer.append("Awaiting transform: ").append(awaitingTransform.size()).append('\n');
    return buffer.toString();
  }

  /**
   * Puts an element into one of the blocking queues. Since none of them have a
   * capacity limit, blocking should never happen.
   * 
   * @param <E> either task or dependency
   * @param queue the queue to put the element in
   * @param element the element to put into the queue
   */
  protected void putInQueue(final LinkedBlockingQueue<Task> queue, final Task element) {
    try {
      queue.put(element);
      queueLogger.debug("\nPut\nStatus\n{}", queueStatus());
    } catch (InterruptedException e) {
      throw new IllegalStateException("Interrupted exception when putting task in queue.", e);
    }
  }
}
