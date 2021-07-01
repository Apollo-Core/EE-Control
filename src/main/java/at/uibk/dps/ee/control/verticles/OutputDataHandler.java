package at.uibk.dps.ee.control.verticles;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import at.uibk.dps.ee.guice.starter.VertxProvider;
import at.uibk.dps.ee.model.graph.EnactmentGraphProvider;
import at.uibk.dps.ee.model.properties.PropertyServiceData;
import net.sf.opendse.model.Task;
import net.sf.opendse.model.properties.TaskPropertyService;

/**
 * The {@link OutputDataHandler} reacts to the messages signalizing available
 * data on the leaf nodes of the graph. As soon as all leaf nodes have annotated
 * data, it formulates a message containing the workflow result.
 * 
 * @author Fedor Smirnov
 *
 */
public class OutputDataHandler extends HandlerApollo<Task> {

  protected final Set<Task> leafNodes;
  protected final Map<Task, JsonElement> leafData = new HashMap<>();

  @Inject
  public OutputDataHandler(EnactmentGraphProvider eGraphProvider, VertxProvider vertxProvider) {
    super(ConstantsEventBus.addressDataAvailable, ConstantsEventBus.addressWorkflowResultAvailable,
        ConstantsEventBus.addressFailureAbort, vertxProvider.geteBus(), eGraphProvider);
    this.leafNodes = eGraphProvider.getEnactmentGraph().getVertices().stream()
        .filter(
            task -> (TaskPropertyService.isCommunication(task) && PropertyServiceData.isLeaf(task)))
        .collect(Collectors.toSet());
  }

  @Override
  protected Task readMessage(String message) {
    return eGraph.getVertex(message);
  }

  @Override
  protected void work(Task dataNode) throws WorkerException {
    if (PropertyServiceData.isLeaf(dataNode)) {
      JsonElement data = PropertyServiceData.getContent(dataNode);
      leafData.put(dataNode, data);
      if (leafData.keySet().containsAll(leafNodes)) {
        JsonObject result = new JsonObject();
        leafData.entrySet().forEach(entry -> {
          result.add(PropertyServiceData.getJsonKey(entry.getKey()), entry.getValue());
        });
        eBus.publish(ConstantsEventBus.addressWorkflowResultAvailable, result.toString());
      }
    }
  }
}
