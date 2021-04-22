package at.uibk.dps.ee.control.agents;

import at.uibk.dps.ee.core.ControlStateListener;
import at.uibk.dps.ee.core.EnactmentState;
import at.uibk.dps.ee.core.exception.StopException;
import net.sf.opendse.model.Task;

/**
 * A continuous agent is an agent which continuously monitors a queue until it
 * receives a {@link PoisonPill}.
 * 
 * @author Fedor Smirnov
 */
public abstract class AgentContinuous implements Agent, ControlStateListener {

  protected boolean stopped;
  protected boolean paused;

  @Override
  public Boolean call() throws Exception {
    while (!stopped) {
      if (paused) {
        // go to sleep
        synchronized (this) {
          try {
            wait();
          } catch (InterruptedException e) {
            throw new IllegalArgumentException("Continuous agent interrupted.", e);
          }
        }
      }
      final Task task = getTaskFromBlockingQueue();
      if (task instanceof PoisonPill) {
        // stopping
        stopped = true;
        continue;
      } else {
        operationOnTask(task);
      }
    }
    // reset the state for a possible reexecution before exiting
    stopped = false;
    paused = false;
    return true;
  }

  /**
   * The task of the continuous agent which is performed on the task retrieved
   * from the blocking queue.
   */
  protected abstract void operationOnTask(Task task) throws StopException;

  /**
   * Retrieves a task from the blocking queue monitored by this agent.
   * 
   * @return a task from the blocking queue monitored by this agent
   */
  protected abstract Task getTaskFromBlockingQueue();

  @Override
  public void reactToStateChange(final EnactmentState previousState,
      final EnactmentState currentState) throws StopException {
    if (previousState.equals(EnactmentState.RUNNING)
        && currentState.equals(EnactmentState.PAUSED)) {
      // pausing
      synchronized (this) {
        this.paused = true;
      }
    } else if (previousState.equals(EnactmentState.PAUSED)
        && currentState.equals(EnactmentState.RUNNING)) {
      // resuming
      synchronized (this) {
        this.paused = false;
        notifyAll();
      }
    }
  }
}
