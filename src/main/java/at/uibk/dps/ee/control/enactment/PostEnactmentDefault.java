package at.uibk.dps.ee.control.enactment;

import com.google.gson.JsonObject;
import com.google.inject.Singleton;
import at.uibk.dps.ee.control.verticles.ConstantsVertX;
import at.uibk.dps.ee.model.constants.ConstantsEEModel;
import at.uibk.dps.ee.model.properties.PropertyServiceFunction;
import at.uibk.dps.ee.model.properties.PropertyServiceFunctionDataFlowCollections;
import at.uibk.dps.ee.model.properties.PropertyServiceFunctionUtilityWhile;
import io.vertx.core.eventbus.EventBus;
import net.sf.opendse.model.Task;

/**
 * Default operation after the enactment. Publishes the message to either the
 * completion or the transformation address.
 * 
 * @author Fedor Smirnov
 *
 */
@Singleton
public class PostEnactmentDefault implements PostEnactment {

  @Override
  public void postEnactmentTreatment(final Task enactedTask, final EventBus eBus) {
    if (requiresTransformation(enactedTask)) {
      eBus.send(ConstantsVertX.addressRequiredTransformation, enactedTask.getId());
    } else {
      eBus.send(ConstantsVertX.addressEnactmentFinished, enactedTask.getId());
    }
  }

  /**
   * Returns true iff the provided task requires a graph transformation after its
   * enactment.
   * 
   * @param task the provided task.
   * @return true iff the provided task requires a graph transformation after its
   *         enactment
   */
  protected boolean requiresTransformation(final Task task) {
    return PropertyServiceFunctionDataFlowCollections.isAggregationNode(task)
        || PropertyServiceFunctionDataFlowCollections.isDistributionNode(task)
        || whileRequiresTransformation(task);
  }

  /**
   * Returns true iff the provided task (a) is a while end and (b) requires a
   * graph transformation (to create the next iteration of the while body).
   * 
   * @param task the provided task
   * @return true iff the provided task (a) is a while end and (b) requires a
   *         graph transformation (to create the next iteration of the while body)
   */
  protected boolean whileRequiresTransformation(Task task) {
    if (!PropertyServiceFunctionUtilityWhile.isWhileEndTask(task)) {
      return false;
    }
    JsonObject content = PropertyServiceFunction.getOutput(task);
    if (!content.has(ConstantsEEModel.JsonKeyWhileDecision)) {
      throw new IllegalArgumentException(
          "While decision variable not set in the while end task " + task);
    }
    return content.get(ConstantsEEModel.JsonKeyWhileDecision).getAsBoolean();
  }
}
