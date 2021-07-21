package at.uibk.dps.ee.control.agents;

import java.util.Set;
import at.uibk.dps.ee.control.graph.GraphAccess;
import at.uibk.dps.ee.control.management.EnactmentQueues;
import at.uibk.dps.ee.control.verticles.transformation.GraphTransform;
import at.uibk.dps.ee.core.ModelModificationListener;
import net.sf.opendse.model.Task;

/**
 * The {@link AgentTransform} is used in cases where the enactment graph needs
 * to be transformed at run time. This agent should be provided with tasks whose
 * enactables have been executed.
 * 
 * @author Fedor Smirnov
 *
 */
public class AgentTransform extends AgentTask {

  protected final GraphTransform modification;
  protected final GraphAccess graphAccess;
  protected final Task taskNode;
  protected final EnactmentQueues enactmentState;
  protected final Set<ModelModificationListener> modificationListeners;

  /**
   * The default constructor.
   * 
   * @param listeners the {@link AgentTaskListener}s
   * @param graphAccess the graph access
   * @param modification the operation describing the graph modification
   * @param taskNode the task node triggering the transformation
   * @param enactmentState the state of the enactment (for the queue access)
   */
  public AgentTransform(final Set<AgentTaskListener> listeners, final GraphAccess graphAccess,
      final GraphTransform modification, final Task taskNode, final EnactmentQueues enactmentState,
      final Set<ModelModificationListener> modificationListeners) {
    super(listeners);
    this.modification = modification;
    this.graphAccess = graphAccess;
    this.taskNode = taskNode;
    this.enactmentState = enactmentState;
    this.modificationListeners = modificationListeners;
  }

  @Override
  protected boolean actualCall() throws Exception {
    modification.modifyEnactmentGraph(graphAccess, taskNode);
    modificationListeners.forEach(listener -> listener.reactToModelModification());
    enactmentState.putFinishedTask(taskNode);
    return true;
  }

  @Override
  protected String formulateExceptionMessage() {
    return ConstantsAgents.ExcMessageTransformPrefix + modification.getTransformName()
        + ConstantsAgents.ExcMessageTransformSuffix + taskNode.getId();
  }
}
