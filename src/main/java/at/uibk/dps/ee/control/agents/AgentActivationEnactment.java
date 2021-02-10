package at.uibk.dps.ee.control.agents;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import at.uibk.dps.ee.control.management.EnactmentState;
import at.uibk.dps.ee.control.management.ExecutorProvider;
import net.sf.opendse.model.Task;

/**
 * The ActivationEnactment is responsible for the activation of the {@link AgentEnactment}s to
 * process the tasks in the ready queue.
 * 
 * @author Fedor Smirnov
 */
public class AgentActivationEnactment extends AgentContinuous implements AgentTaskCreator {

  protected final EnactmentState enactmentState;
  protected final ExecutorService executor;
  protected final AgentFactoryEnactment agentFactory;
  protected final Set<AgentTaskListener> listeners = new HashSet<>();

  public AgentActivationEnactment(EnactmentState enactmentState, ExecutorProvider executorProvider,
      AgentFactoryEnactment agentFactory) {
    this.enactmentState = enactmentState;
    this.executor = executorProvider.getExecutorService();
    this.agentFactory = agentFactory;
  }

  @Override
  protected void operationOnTask(Task launchableTask) {
    AgentEnactment enacterAgent =
        agentFactory.createAgentEnactment(launchableTask, getAgentTaskListeners());
    executor.submit(enacterAgent);
  }

  /**
   * Adds a listener to the listener list
   * 
   * @param listener the listener to add
   */
  public void addAgentTaskListener(AgentTaskListener listener) {
    listeners.add(listener);
  }

  @Override
  protected Task getTaskFromBlockingQueue() {
    try {
      return enactmentState.takeLaunchableTask();
    } catch (InterruptedException e) {
      throw new IllegalStateException("Enactment Activation agent interrupted.", e);
    }
  }

  @Override
  public Set<AgentTaskListener> getAgentTaskListeners() {
    return listeners;
  }
}
