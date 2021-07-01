package at.uibk.dps.ee.control.verticles.extraction;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.inject.Inject;
import at.uibk.dps.ee.control.verticles.ConstantsEventBus;
import at.uibk.dps.ee.control.verticles.HandlerApollo;
import at.uibk.dps.ee.control.verticles.WorkerException;
import at.uibk.dps.ee.guice.starter.VertxProvider;
import at.uibk.dps.ee.model.graph.EnactmentGraphProvider;
import at.uibk.dps.ee.model.properties.PropertyServiceData;
import at.uibk.dps.ee.model.properties.PropertyServiceDependency;
import at.uibk.dps.ee.model.properties.PropertyServiceFunction;
import at.uibk.dps.ee.model.properties.PropertyServiceData.NodeType;
import net.sf.opendse.model.Dependency;
import net.sf.opendse.model.Task;

public class WorkerExtraction extends HandlerApollo<Task> {

  @Inject
  public WorkerExtraction(EnactmentGraphProvider eGraphProvider, VertxProvider vertxProvider) {
    super(ConstantsEventBus.addressEnactmentFinished, ConstantsEventBus.addressDataAvailable,
        ConstantsEventBus.addressFailureAbort, vertxProvider.geteBus(), eGraphProvider);
  }

  @Override
  protected void work(Task finishedTask) throws WorkerException {
    for (Dependency outEdge : eGraph.getOutEdges(finishedTask)) {
      processOutEdge(outEdge);
    }
  }

  protected void processOutEdge(Dependency outEdge) throws WorkerException {
    Task finishedFunction = eGraph.getSource(outEdge);
    Task dataNode = eGraph.getDest(outEdge);
    final boolean dataNodeModelsSequentiality =
        PropertyServiceData.getNodeType(dataNode).equals(NodeType.Sequentiality);
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
    annotateExtractionEdge(outEdge);
    System.out.println("extracted");
    eBus.publish(successAddress, dataNode.getId());
  }

  protected void annotateExtractionEdge(Dependency extractionEdge) {
    PropertyServiceDependency.setExtractionDone(extractionEdge);
    final Task process = eGraph.getSource(extractionEdge);
    // check if extraction done for all out edges
    if (eGraph.getOutEdges(process).stream()
        .allMatch(outEdge -> PropertyServiceDependency.isExtractionDone(outEdge))) {
      // reset the edge annotation
      eGraph.getOutEdges(process)
          .forEach(outEdge -> PropertyServiceDependency.resetExtractionDone(outEdge));
      // reset the enactable state
      eBus.publish(ConstantsEventBus.addressResetScheduleTask, process.getId());
    }
  }

  @Override
  protected Task readMessage(String message) {
    return eGraph.getVertex(message);
  }



}
