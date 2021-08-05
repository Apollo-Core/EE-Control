package at.uibk.dps.ee.control.transformation;

import com.google.gson.JsonObject;
import at.uibk.dps.ee.control.verticles.ConstantsVertX;
import at.uibk.dps.ee.model.constants.ConstantsEEModel;
import at.uibk.dps.ee.model.properties.PropertyServiceFunction;
import at.uibk.dps.ee.model.properties.PropertyServiceFunctionDataFlowCollections;
import at.uibk.dps.ee.model.properties.PropertyServiceFunctionUtilityWhile;
import at.uibk.dps.ee.model.properties.PropertyServiceReproduction;
import io.vertx.core.eventbus.EventBus;
import net.sf.opendse.model.Task;

/**
 * The default class implementing the post transformation behavior
 * 
 * @author Fedor Smirnov
 *
 */
public class PostTransformationDefault implements PostTransformation {

  protected final String enactAddress = ConstantsVertX.addressEnactmentFinished;

  @Override
  public void postTransformationTreatment(Task transformationTriggerTask, EventBus eventBus) {
    // in case of the parFor transformations, just say that stuff is enacted
    if (PropertyServiceFunctionDataFlowCollections.isAggregationNode(transformationTriggerTask)
        || PropertyServiceFunctionDataFlowCollections
            .isDistributionNode(transformationTriggerTask)) {
      postTransformParFor(transformationTriggerTask, eventBus);
    } else if (PropertyServiceFunctionUtilityWhile.isWhileEndTask(transformationTriggerTask)) {
      postTransformWhile(transformationTriggerTask, eventBus);
    } else {
      throw new IllegalStateException(
          "No post transform behavior defined for task " + transformationTriggerTask);
    }
  }

  /**
   * Post transformation for while transform tasks (potentially considering the
   * graph collapse).
   * 
   * @param whileEndTask the while end task whose transformation was just
   *        executed.
   * @param eBus the vertX event bus
   */
  protected void postTransformWhile(Task whileEndTask, EventBus eBus) {
    JsonObject output = PropertyServiceFunction.getOutput(whileEndTask);
    boolean whileContinued = output.get(ConstantsEEModel.JsonKeyWhileDecision).getAsBoolean();
    if (whileContinued) {
      // just built the next iteration => enact the while end (replica)
      eBus.send(enactAddress, whileEndTask.getId());
    } else {
      // while finished and collapsed => enact the original while end
      String originalWhileEndReference =
          PropertyServiceReproduction.getOriginalWhileEndReference(whileEndTask);
      eBus.send(enactAddress, originalWhileEndReference);
    }
  }

  /**
   * Post transformation for parallelFor transform tasks (just finishes the
   * enactment)
   * 
   * @param parallelForTask distribution/aggregation task of a parallel for
   * @param eBus the vertX event bus
   */
  protected void postTransformParFor(Task parallelForTask, EventBus eBus) {
    eBus.send(enactAddress, parallelForTask.getId());
  }
}
