package at.uibk.dps.ee.control.verticles.extraction;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import at.uibk.dps.ee.control.graph.GraphAccess;
import at.uibk.dps.ee.control.verticles.ConstantsEventBus;
import at.uibk.dps.ee.core.enactable.Enactable;
import at.uibk.dps.ee.core.enactable.Enactable.State;
import at.uibk.dps.ee.model.graph.EnactmentGraph;
import at.uibk.dps.ee.model.properties.PropertyServiceData;
import at.uibk.dps.ee.model.properties.PropertyServiceDependency;
import at.uibk.dps.ee.model.properties.PropertyServiceFunction;
import io.vertx.core.eventbus.EventBus;
import at.uibk.dps.ee.model.properties.PropertyServiceData.NodeType;
import net.sf.opendse.model.Dependency;
import net.sf.opendse.model.Task;

/**
 * The {@link AgentExtractionLocal} is used in cases where the data is already
 * present on the local machine and just needs to be written into the data node.
 * 
 * @author Fedor Smirnov
 *
 */
public class AgentExtractionLocal extends AgentExtraction {

  public AgentExtractionLocal(Task finishedTask, Dependency outEdge, Task dataNode,
      GraphAccess graphAccess, EventBus eventBus) {
    super(finishedTask, outEdge, dataNode, graphAccess, eventBus);
  }

  @Override
  protected void extract() {
    final boolean dataNodeModelsSequentiality =
        PropertyServiceData.getNodeType(dataNode).equals(NodeType.Sequentiality);
    final Enactable finishedEnactable = PropertyServiceFunction.getEnactable(finishedTask);
    final JsonObject enactmentResult = finishedEnactable.getResult();
    final String key = PropertyServiceDependency.getJsonKey(outEdge);
    if (!enactmentResult.has(key) && !dataNodeModelsSequentiality) {
      throw new IllegalStateException(
          "The execution of the task " + finishedTask.getId() + " did not produce an entry named "
              + key + " instead, we got " + enactmentResult.toString());
    }
    final JsonElement data =
        dataNodeModelsSequentiality ? new JsonPrimitive(true) : enactmentResult.get(key);
    PropertyServiceData.setContent(dataNode, data);
    graphAccess.writeOperationEdge(this::annotateExtractionEdge, outEdge);
    eventBus.publish(ConstantsEventBus.addressDataAvailable, dataNode.getId());
  }

  /**
   * Annotates that the extraction modeled by the given edge is finished.
   * 
   * @param graph the enactment graph
   * @param extractionEdge the given edge
   */
  protected void annotateExtractionEdge(final EnactmentGraph graph,
      final Dependency extractionEdge) {
    PropertyServiceDependency.setExtractionDone(extractionEdge);
    final Task process = graph.getSource(extractionEdge);
    // check if extraction done for all out edges
    if (graph.getOutEdges(process).stream()
        .allMatch(outEdge -> PropertyServiceDependency.isExtractionDone(outEdge))) {
      // reset the edge annotation
      graph.getOutEdges(process)
          .forEach(outEdge -> PropertyServiceDependency.resetExtractionDone(outEdge));
      // reset the enactable state
      PropertyServiceFunction.getEnactable(process).setState(State.WAITING);
      eventBus.publish(ConstantsEventBus.addressResetScheduleTask, process.getId());
    }
  }
}
