package at.uibk.dps.ee.control.verticles;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import at.uibk.dps.ee.guice.starter.VertxProvider;
import at.uibk.dps.ee.model.graph.EnactmentGraph;
import at.uibk.dps.ee.model.graph.EnactmentGraphProvider;
import at.uibk.dps.ee.model.properties.PropertyServiceData;
import at.uibk.dps.ee.model.properties.PropertyServiceData.NodeType;
import io.vertx.core.eventbus.EventBus;
import net.sf.opendse.model.Task;
import net.sf.opendse.model.properties.TaskPropertyService;

/**
 * The {@link InputDataHandler} processes the input JSON by annotating the root
 * nodes of the graph and broadcasting the data-available events on the event
 * bus.
 * 
 * @author Fedor Smirnov
 */
public class InputDataHandler {

  protected final EnactmentGraph eGraph;
  protected final EventBus eBus;

  @Inject
  public InputDataHandler(EnactmentGraphProvider eGraphProvider, VertxProvider vertxProvider) {
    this.eGraph = eGraphProvider.getEnactmentGraph();
    this.eBus = vertxProvider.geteBus();
  }

  public void processInput(JsonObject input) {
    getRootDataNodes().forEach(rootNode -> processRootNode(rootNode, input));
    getConstantDataNodes().forEach(
        constantNode -> eBus.send(ConstantsEventBus.addressDataAvailable, constantNode.getId()));
  }

  /**
   * Processes the given root node by annotating it with the entry from the json
   * input
   * 
   * @param rootNode the given root node
   */
  protected void processRootNode(final Task rootNode, final JsonObject jsonInput) {
    final String jsonKey = PropertyServiceData.getJsonKey(rootNode);
    final JsonElement content =
        Optional.ofNullable(jsonInput.get(jsonKey)).orElseThrow(() -> new IllegalArgumentException(
            "No entry with the key " + jsonKey + " in the WF input."));
    PropertyServiceData.setContent(rootNode, content);
    eBus.send(ConstantsEventBus.addressDataAvailable, rootNode.getId());
  }

  // should go into "GraphOperations" or sth

  protected Set<Task> getConstantDataNodes() {
    return eGraph.getVertices().stream().filter(task -> TaskPropertyService.isCommunication(task))
        .filter(dataNode -> PropertyServiceData.getNodeType(dataNode).equals(NodeType.Constant))
        .collect(Collectors.toSet());
  }

  protected Set<Task> getRootDataNodes() {
    final Set<Task> result =
        eGraph.getVertices().stream().filter(task -> eGraph.getInEdges(task).size() == 0)
            .filter(task -> !PropertyServiceData.getNodeType(task).equals(NodeType.Constant))
            .collect(Collectors.toSet());
    if (result.stream().anyMatch(task -> !PropertyServiceData.isRoot(task))) {
      throw new IllegalStateException("Non-root nodes without in edges present.");
    }
    return result;
  }
}
