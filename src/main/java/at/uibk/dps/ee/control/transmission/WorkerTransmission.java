package at.uibk.dps.ee.control.transmission;

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
  public WorkerTransmission(final EnactmentGraphProvider eGraphProvider,
      final SchedulabilityCheck schedulabilityCheck) {
    super(ConstantsVertX.addressDataAvailable, ConstantsVertX.addressTaskSchedulable,
        ConstantsVertX.addressFailureAbort, eGraphProvider);
    this.schedulabilityCheck = schedulabilityCheck;
  }

  @Override
  protected void work(final Task dataNode) throws WorkerException {
    this.vertx.sharedData().getLock(ConstantsVertX.transformTransmitLock, lockRes -> {
      if (lockRes.succeeded()) {
        final Lock lock = lockRes.result();
        for (final Dependency transmissionEdge : eGraph.getOutEdges(dataNode)) {
          processTransmissionEdge(transmissionEdge);
        }
        lock.release();
      } else {
        throw new IllegalStateException("Failed getting transmission annotation lock");
      }
    });
  }

  /**
   * Processes a transmission edge which goes out from the data node being
   * processed.
   * 
   * @param transmissionEdge out edge of the data node being processed
   */
  protected void processTransmissionEdge(final Dependency transmissionEdge) {
    final boolean inputCompletelyPresent = annotateAndCheck(transmissionEdge);
    if (inputCompletelyPresent) {
      annotateFunctionInput(transmissionEdge);
    }
  }

  /**
   * Annotates the given edge as processed. Returns true iff all in-edges of the
   * dest function are processed thereafter.
   * 
   * @param transmissionEdge the processed edge
   * @return true iff all the in-edges of the function dest are processed
   */
  protected boolean annotateAndCheck(final Dependency transmissionEdge) {
    PropertyServiceDependency.annotateFinishedTransmission(transmissionEdge);
    final Task functionNode = eGraph.getDest(transmissionEdge);
    return schedulabilityCheck.isTargetSchedulable(functionNode, eGraph);
  }

  /**
   * Annotates the inputs for the function destination of the given edge and
   * advertizes that the function dest can be scheduled.
   * 
   * @param transmissionEdge the given edge
   */
  protected void annotateFunctionInput(final Dependency transmissionEdge) {
    final Task functionNode = eGraph.getDest(transmissionEdge);
    final JsonObject functionInput = new JsonObject();
    // for all in-edges of the node as processed
    eGraph.getInEdges(functionNode).forEach(inEdge -> {
      if (inEdge.equals(transmissionEdge)
          || !PropertyServiceDependency.getType(inEdge).equals(TypeDependency.ControlIf)) {
        final Task src = eGraph.getSource(inEdge);
        final JsonElement content = PropertyServiceData.getContent(src);
        final String key = PropertyServiceDependency.getJsonKey(inEdge);
        functionInput.add(key, content);
      }
    });
    PropertyServiceFunction.setInput(functionNode, functionInput);
    logger.debug("Thread {}; Task {} is schedulable.", Thread.currentThread().getId(),
        functionNode.getId());
    this.vertx.eventBus().send(successAddress, functionNode.getId());
  }
}
