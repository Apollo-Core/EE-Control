package at.uibk.dps.ee.control.verticles.transmission;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import at.uibk.dps.ee.control.verticles.ConstantsVertX;
import at.uibk.dps.ee.control.verticles.VerticleApollo;
import at.uibk.dps.ee.control.verticles.WorkerException;
import at.uibk.dps.ee.model.graph.EnactmentGraphProvider;
import at.uibk.dps.ee.model.properties.PropertyServiceData;
import at.uibk.dps.ee.model.properties.PropertyServiceDependency;
import at.uibk.dps.ee.model.properties.PropertyServiceFunction;
import io.vertx.core.shareddata.Lock;
import at.uibk.dps.ee.model.properties.PropertyServiceData.NodeType;
import at.uibk.dps.ee.model.properties.PropertyServiceDependency.TypeDependency;
import net.sf.opendse.model.Dependency;
import net.sf.opendse.model.Task;

/**
 * Worker transmitting from a data node to function nodes.
 * 
 * @author Fedor Smirnov
 */
public class WorkerTransmission extends VerticleApollo {

  protected final SchedulabilityCheck schedulabilityCheck;

  protected final Logger logger = LoggerFactory.getLogger(WorkerTransmission.class);

  /**
   * Injection constructor
   * 
   * @param eGraphProvider provides the enactment graph
   * @param schedulabilityCheck checks the schedulability of functions
   */
  @Inject
  public WorkerTransmission(EnactmentGraphProvider eGraphProvider,
      SchedulabilityCheck schedulabilityCheck) {
    super(ConstantsVertX.addressDataAvailable, ConstantsVertX.addressTaskSchedulable,
        ConstantsVertX.addressFailureAbort, eGraphProvider);
    this.schedulabilityCheck = schedulabilityCheck;
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
    // annotate the edges
    this.vertx.sharedData().getLock("transmission annotation lock", lockRes -> {
      if (lockRes.succeeded()) {
        Lock lock = lockRes.result();
        annotateTransmission(transmissionEdge);
        lock.release();
      } else {
        throw new IllegalStateException("Failed getting transmission annotation lock");
      }
    });
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
      JsonObject functionInput = new JsonObject();
      // for all in-edges of the node as processed
      eGraph.getInEdges(functionNode).forEach(inEdge -> {
        if (!inEdge.equals(transmissionEdge)
            && PropertyServiceDependency.getType(inEdge).equals(TypeDependency.ControlIf)) {
          // the case of the other if edge which is not active and, therefore, ignored
        } else {
          final Task src = eGraph.getSource(inEdge);
          JsonElement content = PropertyServiceData.getContent(src);
          final String key = PropertyServiceDependency.getJsonKey(inEdge);
          functionInput.add(key, content);
          PropertyServiceDependency.setDataConsumed(inEdge);
          if (!PropertyServiceData.getNodeType(src).equals(NodeType.Constant)
              && eGraph.getOutEdges(src).stream()
                  .allMatch(outEdgeSrc -> PropertyServiceDependency.isDataConsumed(outEdgeSrc))) {
            PropertyServiceData.resetContent(src);
            eGraph.getOutEdges(src)
                .forEach(outEdgeSrc -> PropertyServiceDependency.resetTransmission(outEdgeSrc));
          }
        }
      });
      PropertyServiceFunction.setInput(functionNode, functionInput);
      logger.debug("Thread {}; Task {} is schedulable.", Thread.currentThread().getId(),
          functionNode.getId());
      this.vertx.eventBus().send(successAddress, functionNode.getId());
    }
  }
}
