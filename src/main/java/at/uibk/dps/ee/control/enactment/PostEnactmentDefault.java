package at.uibk.dps.ee.control.enactment;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import at.uibk.dps.ee.control.verticles.ConstantsVertX;
import at.uibk.dps.ee.guice.starter.VertxProvider;
import at.uibk.dps.ee.model.properties.PropertyServiceFunction;
import at.uibk.dps.ee.model.properties.PropertyServiceFunctionDataFlowCollections;
import at.uibk.dps.ee.model.properties.PropertyServiceFunctionUtilityWhile;
import at.uibk.dps.ee.model.properties.PropertyServiceResource;
import at.uibk.dps.sc.core.ConstantsScheduling;
import at.uibk.dps.sc.core.ScheduleModel;
import at.uibk.dps.ee.model.properties.PropertyServiceFunction.UsageType;
import io.vertx.core.AsyncResult;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.shareddata.Lock;
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

  protected final ScheduleModel schedule;
  protected final Vertx vertx;

  /**
   * Injection constructor
   * 
   * @param schedule the reference to the task schedule
   * @param vProv the vertx provider
   */
  @Inject
  public PostEnactmentDefault(final ScheduleModel schedule, final VertxProvider vProv) {
    this.schedule = schedule;
    this.vertx = vProv.getVertx();
  }

  @Override
  public void postEnactmentTreatment(final Task enactedTask, final EventBus eBus) {
    if (requiresTransformation(enactedTask)) {
      eBus.send(ConstantsVertX.addressRequiredTransformation, enactedTask.getId());
    } else {
      if (PropertyServiceFunction.getUsageType(enactedTask).equals(UsageType.User)) {
        this.vertx.sharedData().getLock(ConstantsScheduling.lockCapacityQuery,
            lockRes -> lockResHandler(lockRes, enactedTask, eBus));
      }
    }
  }

  /**
   * Callback used when lock is acquired.
   * 
   * @param asyncRes the async result containing the lock
   * @param enactedTask the task that was enacted
   * @param eBus reference to the event bus
   */
  protected void lockResHandler(final AsyncResult<Lock> asyncRes, final Task enactedTask,
      final EventBus eBus) {
    if (asyncRes.succeeded()) {
      final Lock lock = asyncRes.result();
      schedule.getTaskSchedule(enactedTask)
          .forEach(m -> PropertyServiceResource.removeUsingTask(enactedTask, m.getTarget()));
      lock.release();
      eBus.send(ConstantsVertX.addressEnactmentFinished, enactedTask.getId());
    } else {
      throw new IllegalStateException("Failed to get res capacity lock.");
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
