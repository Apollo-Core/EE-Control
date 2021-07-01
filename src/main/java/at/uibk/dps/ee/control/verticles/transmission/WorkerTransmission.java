package at.uibk.dps.ee.control.verticles.transmission;

import com.google.gson.JsonElement;
import com.google.inject.Inject;
import at.uibk.dps.ee.control.transmission.SchedulabilityCheck;
import at.uibk.dps.ee.control.verticles.ConstantsEventBus;
import at.uibk.dps.ee.control.verticles.HandlerApollo;
import at.uibk.dps.ee.control.verticles.WorkerException;
import at.uibk.dps.ee.guice.starter.VertxProvider;
import at.uibk.dps.ee.model.graph.EnactmentGraphProvider;
import at.uibk.dps.ee.model.properties.PropertyServiceData;
import at.uibk.dps.ee.model.properties.PropertyServiceDependency;
import at.uibk.dps.ee.model.properties.PropertyServiceFunction;
import at.uibk.dps.ee.model.properties.PropertyServiceData.NodeType;
import at.uibk.dps.ee.model.properties.PropertyServiceDependency.TypeDependency;
import net.sf.opendse.model.Dependency;
import net.sf.opendse.model.Task;

public class WorkerTransmission extends HandlerApollo<Task> {

  protected final SchedulabilityCheck schedulabilityCheck;

  @Inject
  public WorkerTransmission(VertxProvider vertxProvider, EnactmentGraphProvider eGraphProvider,
      SchedulabilityCheck schedulabilityCheck) {
    super(ConstantsEventBus.addressDataAvailable, ConstantsEventBus.addressTaskSchedulable,
        ConstantsEventBus.addressFailureAbort, vertxProvider.geteBus(), eGraphProvider);
    this.schedulabilityCheck = schedulabilityCheck;
  }

  @Override
  protected Task readMessage(String message) {
    return eGraph.getVertex(message);
  }

  @Override
  protected void work(Task dataNode) throws WorkerException {
    for (Dependency transmissionEdge : eGraph.getOutEdges(dataNode)) {
      processTransmissionEdge(transmissionEdge);
    }
  }

  /**
   * Processes a transmission edge which goes out from the data node being
   * processed.
   * 
   * @param transmissionEdge out edge of the data node being processed
   */
  protected void processTransmissionEdge(Dependency transmissionEdge) {
    Task dataNode = eGraph.getSource(transmissionEdge);
    Task functionNode = eGraph.getDest(transmissionEdge);
    // set the enactable data
    final JsonElement content = PropertyServiceData.getContent(dataNode);
    final String key = PropertyServiceDependency.getJsonKey(transmissionEdge);
    PropertyServiceFunction.setInput(functionNode, key, content);
    // annotate the edges
    annotateTransmission(transmissionEdge);
  }

  /**
   * Annotates a completed transmission on the corresponding edge. In case that
   * all in edges of the node are annotated as completed, the node is put into the
   * schedulable queue.
   * 
   * @param transmissionEdge the transmission edge to annotate
   */
  protected void annotateTransmission(final Dependency transmissionEdge) {
    Task functionNode = eGraph.getDest(transmissionEdge);
    // annotate the dependency
    PropertyServiceDependency.annotateFinishedTransmission(transmissionEdge);
    // if all edges done with transmitting
    if (schedulabilityCheck.isTargetSchedulable(functionNode, eGraph)) {
      System.out.println("target schedulable");
      eBus.publish(successAddress, functionNode.getId());
      // for all in-edges of the node as processed
      eGraph.getInEdges(functionNode).forEach(inEdge -> {
        if (!inEdge.equals(transmissionEdge)
            && PropertyServiceDependency.getType(inEdge).equals(TypeDependency.ControlIf)) {
          // the case of the other if edge which is not active and, therefore, ignored
        } else {
          PropertyServiceDependency.setDataConsumed(inEdge);
          final Task src = eGraph.getSource(inEdge);
          if (!PropertyServiceData.getNodeType(src).equals(NodeType.Constant)
              && eGraph.getOutEdges(src).stream()
                  .allMatch(outEdgeSrc -> PropertyServiceDependency.isDataConsumed(outEdgeSrc))) {
            PropertyServiceData.resetContent(src);
            eGraph.getOutEdges(src)
                .forEach(outEdgeSrc -> PropertyServiceDependency.resetTransmission(outEdgeSrc));
          }
        }
      });
    }
  }
}
