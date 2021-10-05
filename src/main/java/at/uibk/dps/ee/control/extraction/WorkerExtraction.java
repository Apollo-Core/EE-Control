package at.uibk.dps.ee.control.extraction;

import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.inject.Inject;
import at.uibk.dps.ee.control.verticles.ConstantsVertX;
import at.uibk.dps.ee.control.verticles.VerticleApollo;
import at.uibk.dps.ee.control.verticles.WorkerException;
import at.uibk.dps.ee.model.graph.EnactmentGraphProvider;
import at.uibk.dps.ee.model.properties.PropertyServiceData;
import at.uibk.dps.ee.model.properties.PropertyServiceDependency;
import at.uibk.dps.ee.model.properties.PropertyServiceFunction;
import at.uibk.dps.ee.model.properties.PropertyServiceData.NodeType;
import net.sf.opendse.model.Dependency;
import net.sf.opendse.model.Task;
import net.sf.opendse.model.properties.TaskPropertyService;

/**
 * The {@link WorkerExtraction} is responsible for the extraction of the data
 * from the finished function nodes.
 * 
 * @author Fedor Smirnov
 *
 */
public class WorkerExtraction extends VerticleApollo {

  protected final Set<Task> leafNodes;

  protected final Logger logger = LoggerFactory.getLogger(WorkerExtraction.class);

  /**
   * The injection constructor.
   * 
   * @param eGraphProvider provider of the enactment graph
   */
  @Inject
  public WorkerExtraction(final EnactmentGraphProvider eGraphProvider) {
    super(ConstantsVertX.addressEnactmentFinished, ConstantsVertX.addressDataAvailable,
        ConstantsVertX.addressFailureAbort, eGraphProvider);
    this.leafNodes = eGraph.getVertices().stream()
        .filter(
            node -> TaskPropertyService.isCommunication(node) && PropertyServiceData.isLeaf(node))
        .collect(Collectors.toSet());
  }

  @Override
  protected void work(final Task finishedTask) throws WorkerException {
    for (final Dependency outEdge : eGraph.getOutEdges(finishedTask)) {
      processOutEdge(outEdge);
    }
  }

  /**
   * Processes the given out edge.
   * 
   * @param outEdge the given out edge
   * @throws WorkerException exception thrown in case the extraction fails
   */
  protected void processOutEdge(final Dependency outEdge) throws WorkerException {
    final Task dataNode = eGraph.getDest(outEdge);
    final boolean dataNodeModelsSequentiality =
        PropertyServiceData.getNodeType(dataNode).equals(NodeType.Sequentiality);
    if (dataNodeModelsSequentiality & PropertyServiceData.isWhileCounter(dataNode)) {
      this.vertx.eventBus().send(successAddress, dataNode.getId());
      return;
    }
    final Task finishedFunction = eGraph.getSource(outEdge);
    final JsonObject enactmentResult = PropertyServiceFunction.getOutput(finishedFunction);
    final String key = PropertyServiceDependency.getJsonKey(outEdge);
    if (!enactmentResult.has(key) && !dataNodeModelsSequentiality) {
      throw new WorkerException("The execution of the task " + finishedFunction.getId()
          + " did not produce an entry named " + key + " instead, we got "
          + enactmentResult.toString());
    }
    final JsonElement data =
        dataNodeModelsSequentiality ? new JsonPrimitive(true) : enactmentResult.get(key);
    PropertyServiceData.setContent(dataNode, data);
    //annotateExtractionEdge(outEdge);
    logger.debug("Thread {}; Data on node {} available.", Thread.currentThread().getId(),
        dataNode.getId());
    this.vertx.eventBus().send(successAddress, dataNode.getId());
    // check whether done
    checkOverallResult();
  }

  /**
   * Check whether the overall result of the WF execution is complete. In case the
   * WF execution is complete, a corresponding message is published on the event
   * bus.
   */
  protected void checkOverallResult() {
    if (leafNodes.stream().allMatch(leafNode -> PropertyServiceData.isDataAvailable(leafNode))) {
      final JsonObject result = new JsonObject();
      leafNodes.forEach(leafNode -> {
        final JsonElement content = PropertyServiceData.getContent(leafNode);
        final String key = PropertyServiceData.getJsonKey(leafNode);
        result.add(key, content);
      });
      this.vertx.eventBus().publish(ConstantsVertX.addressWorkflowResultAvailable,
          result.toString());
    }
  }
}
