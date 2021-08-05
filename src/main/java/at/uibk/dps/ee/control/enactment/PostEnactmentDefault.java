package at.uibk.dps.ee.control.enactment;

import com.google.inject.Singleton;
import at.uibk.dps.ee.control.verticles.ConstantsVertX;
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
        || PropertyServiceFunctionUtilityWhile.isWhileEndTask(task);
  }
}
