package at.uibk.dps.ee.control.verticles;

import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

  protected final Logger logger = LoggerFactory.getLogger(InputDataHandler.class);

  /**
   * The injection constructor.
   * 
   * @param eGraphProvider provides the e graph
   * @param vertxProvider provides the vertX instance
   */
  @Inject
  public InputDataHandler(final EnactmentGraphProvider eGraphProvider,
      final VertxProvider vertxProvider) {
    this.eGraph = eGraphProvider.getEnactmentGraph();
    this.eBus = vertxProvider.geteBus();
  }

  /**
   * Processes the workflow input by annotating the corresponding root nodes of
   * the graph and sending event bus messages with the information that the
   * corresponding data is available.
   * 
   * @param input the workflow input
   */
  public void processInput(final JsonObject input) {
    EnactmentGraphUtils.getNonConstRootNodes(eGraph)
        .forEach(rootNode -> processRootNode(rootNode, input));
    EnactmentGraphUtils.getConstantDataNodes(eGraph)
        .forEach(constantNode -> processConstantNode(constantNode));
  }

  /**
   * Processes the given constant node: Sends an event bus message signaling the
   * availability of the node's data.
   * 
   * @param constantNode the constant node to process
   */
  protected void processConstantNode(final Task constantNode) {
    logger.debug("Availability of constant node {} advertized.", constantNode.getId());
    eBus.send(ConstantsVertX.addressDataAvailable, constantNode.getId());
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
    logger.debug("Availability of the input data modeled by node {} advertized.", rootNode.getId());
    eBus.send(ConstantsVertX.addressDataAvailable, rootNode.getId());
  }
}
