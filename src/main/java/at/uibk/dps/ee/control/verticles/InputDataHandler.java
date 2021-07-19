package at.uibk.dps.ee.control.verticles;

import java.util.Optional;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import at.uibk.dps.ee.guice.starter.VertxProvider;
import at.uibk.dps.ee.model.graph.EnactmentGraph;
import at.uibk.dps.ee.model.graph.EnactmentGraphProvider;
import at.uibk.dps.ee.model.properties.PropertyServiceData;
import at.uibk.dps.ee.model.utils.EnactmentGraphUtils;
import io.vertx.core.eventbus.EventBus;
import net.sf.opendse.model.Task;

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
    EnactmentGraphUtils.getNonConstRootNodes(eGraph)
        .forEach(rootNode -> processRootNode(rootNode, input));
    EnactmentGraphUtils.getConstantDataNodes(eGraph).forEach(
        constantNode -> eBus.send(ConstantsVertX.addressDataAvailable, constantNode.getId()));
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
    eBus.send(ConstantsVertX.addressDataAvailable, rootNode.getId());
  }
}
