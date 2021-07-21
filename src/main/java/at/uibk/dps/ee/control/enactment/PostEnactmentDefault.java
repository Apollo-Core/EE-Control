package at.uibk.dps.ee.control.enactment;

import com.google.inject.Singleton;
import at.uibk.dps.ee.control.verticles.ConstantsVertX;
import at.uibk.dps.ee.model.properties.PropertyServiceFunction;
import at.uibk.dps.ee.model.properties.PropertyServiceFunctionDataFlow;
import at.uibk.dps.ee.model.properties.PropertyServiceFunction.UsageType;
import at.uibk.dps.ee.model.properties.PropertyServiceFunctionDataFlow.DataFlowType;
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
  public void postEnactmentTreatment(Task enactedTask, EventBus eBus) {
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
    return PropertyServiceFunction.getUsageType(task).equals(UsageType.DataFlow)
        && PropertyServiceFunctionDataFlow.getDataFlowType(task).equals(DataFlowType.Collections);
  }
}
