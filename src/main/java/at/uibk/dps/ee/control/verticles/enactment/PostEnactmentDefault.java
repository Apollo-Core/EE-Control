package at.uibk.dps.ee.control.verticles.enactment;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import at.uibk.dps.ee.control.enactment.PostEnactment;
import at.uibk.dps.ee.control.verticles.ConstantsEventBus;
import at.uibk.dps.ee.guice.starter.VertxProvider;
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

  protected final EventBus eBus;

  @Inject
  public PostEnactmentDefault(VertxProvider vertxProvider) {
    this.eBus = vertxProvider.geteBus();
  }

  @Override
  public void postEnactmentTreatment(Task enactedTask) {
    if (requiresTransformation(enactedTask)) {
      eBus.publish(ConstantsEventBus.addressRequiredTransformation, enactedTask.getId());
    } else {
      System.out.println("enacted");
      eBus.publish(ConstantsEventBus.addressEnactmentFinished, enactedTask.getId());
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
