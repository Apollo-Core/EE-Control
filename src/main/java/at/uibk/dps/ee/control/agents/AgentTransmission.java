package at.uibk.dps.ee.control.agents;

import java.util.Set;

import com.google.gson.JsonElement;

import at.uibk.dps.ee.control.graph.GraphAccess;
import at.uibk.dps.ee.control.management.EnactmentQueues;
import at.uibk.dps.ee.control.transmission.SchedulabilityCheck;
import at.uibk.dps.ee.core.enactable.Enactable;
import at.uibk.dps.ee.core.enactable.Enactable.State;
import at.uibk.dps.ee.model.graph.EnactmentGraph;
import at.uibk.dps.ee.model.properties.PropertyServiceData;
import at.uibk.dps.ee.model.properties.PropertyServiceData.NodeType;
import at.uibk.dps.ee.model.properties.PropertyServiceDependency;
import at.uibk.dps.ee.model.properties.PropertyServiceFunction;
import net.sf.opendse.model.Dependency;
import net.sf.opendse.model.Task;

/**
 * The {@link AgentTransmission} is responsible for (a) transmitting the content
 * of a data node to a dependent function node and (b) checking whether the
 * function node is ready for execution (in which case it is put into the
 * readyQueue).
 * 
 * @author Fedor Smirnov
 */
public class AgentTransmission extends AgentTask {

  protected final EnactmentQueues enactmentState;
  protected final Task dataNode;
  protected final Dependency edge;
  protected final Task functionNode;
  protected final GraphAccess graphAccess;
  protected final SchedulabilityCheck schedulabilityCheck;

  /**
   * The default constructor.
   * 
   * @param enactmentState the state of the enactment (for the access to the
   *        queues)
   * @param dataNode the data node whose data is transmitted
   * @param edge the edge between the data node and the function node
   * @param functionNode the function node
   * @param graphAccess the access to the graph
   * @param listeners the {@link AgentTaskListener}s
   */
  public AgentTransmission(final EnactmentQueues enactmentState, final Task dataNode,
      final Dependency edge, final Task functionNode, final GraphAccess graphAccess,
      final Set<AgentTaskListener> listeners, final SchedulabilityCheck schedulabilityCheck) {
    super(listeners);
    this.enactmentState = enactmentState;
    this.dataNode = dataNode;
    this.edge = edge;
    this.functionNode = functionNode;
    this.graphAccess = graphAccess;
    this.schedulabilityCheck = schedulabilityCheck;
  }

  @Override
  public boolean actualCall() throws Exception {
    // set the enactable data
    final JsonElement content = PropertyServiceData.getContent(dataNode);
    final String key = PropertyServiceDependency.getJsonKey(edge);
    final Enactable enactable = PropertyServiceFunction.getEnactable(functionNode);
    synchronized (enactable) {
      enactable.setInputValue(key, content);
    }
    // annotate the edges
    graphAccess.writeOperationTask(this::annotateTransmission, functionNode);
    return true;
  }

  /**
   * Annotates a completed transmission on the corresponding edge. In case that
   * all in edges of the node are annotated as completed, the node is put into the
   * schedulable queue.
   * 
   * @param graph the enactment graph
   * @param functionNode the function node to which information was transmitted
   */
  protected void annotateTransmission(final EnactmentGraph graph, final Task functionNode) {
    // annotate the dependency
    PropertyServiceDependency.annotateFinishedTransmission(edge);
    // if all edges done with transmitting
    if (schedulabilityCheck.isTargetSchedulable(functionNode, graph)) {
      PropertyServiceFunction.getEnactable(functionNode).setState(State.SCHEDULABLE);
      enactmentState.putSchedulableTask(functionNode);
      // for all in-edges of the node as processed
      graph.getInEdges(functionNode).forEach(inEdge -> {
        PropertyServiceDependency.setDataConsumed(inEdge);
        Task src = graph.getSource(inEdge);
        if (!PropertyServiceData.getNodeType(src).equals(NodeType.Constant)
            && graph.getOutEdges(src).stream()
                .allMatch(outEdgeSrc -> PropertyServiceDependency.isDataConsumed(outEdgeSrc))) {
          PropertyServiceData.resetContent(src);
          graph.getOutEdges(src)
              .forEach(outEdgeSrc -> PropertyServiceDependency.resetTransmission(outEdgeSrc));
        }
      });
    }
  }

  @Override
  protected String formulateExceptionMessage() {
    return ConstantsAgents.ExcMessageTransmissionPrefix + dataNode.getId()
        + ConstantsAgents.ExcMessageTransmissionSuffix + functionNode.getId();
  }
}
