package at.uibk.dps.ee.control.agents;

import java.util.Set;
import com.google.inject.Inject;
import at.uibk.dps.ee.control.graph.GraphAccess;
import at.uibk.dps.ee.control.management.EnactmentQueues;
import at.uibk.dps.ee.control.verticles.transformation.GraphTransformAggregation;
import at.uibk.dps.ee.control.verticles.transformation.GraphTransformDistribution;
import at.uibk.dps.ee.core.ModelModificationListener;
import at.uibk.dps.ee.enactables.EnactableFactory;
import at.uibk.dps.ee.model.properties.PropertyServiceFunctionDataFlowCollections;
import at.uibk.dps.ee.model.properties.PropertyServiceFunctionDataFlowCollections.OperationType;
import net.sf.opendse.model.Task;

/**
 * The {@link AgentFactoryTransform} is used for the creation of
 * {@link AgentTransform}s.
 * 
 * @author Fedor Smirnov
 */
public class AgentFactoryTransform {

  protected final GraphAccess graphAccess;
  protected final EnactableFactory enactableFactory;
  protected final EnactmentQueues enactmentState;
  protected final Set<ModelModificationListener> modificationListeners;

  /**
   * The injection constructor.
   * 
   * @param graphAccess the access to the graph
   * @param enactableFactory the factory for the enactables (needed during node
   *        reproduction)
   * @param enactmentState the state of the enactment (for the access to the
   *        queues)
   */
  @Inject
  public AgentFactoryTransform(final GraphAccess graphAccess,
      final EnactableFactory enactableFactory, final EnactmentQueues enactmentState,
      final Set<ModelModificationListener> modificationListeners) {
    this.graphAccess = graphAccess;
    this.enactableFactory = enactableFactory;
    this.enactmentState = enactmentState;
    this.modificationListeners = modificationListeners;
  }

  /**
   * Creates a transformation agent for the provided task from the transformation
   * queue.
   * 
   * @param taskNode the task triggering the transformation
   * @param listeners the {@link AgentTaskListener}s
   * @return a transformation agent for the provided task from the transformation
   *         queue
   */
  public AgentTransform createTransformAgent(final Task taskNode,
      final Set<AgentTaskListener> listeners) {
    if (PropertyServiceFunctionDataFlowCollections.getOperationType(taskNode)
        .equals(OperationType.Distribution)) {
      return new AgentTransform(listeners, graphAccess,
          new GraphTransformDistribution(enactableFactory), taskNode, enactmentState,
          modificationListeners);
    } else if (PropertyServiceFunctionDataFlowCollections.getOperationType(taskNode)
        .equals(OperationType.Aggregation)) {
      return new AgentTransform(listeners, graphAccess, new GraphTransformAggregation(), taskNode,
          enactmentState, modificationListeners);
    } else {
      throw new IllegalArgumentException("Unknown type of data flow operation.");
    }
  }
}
