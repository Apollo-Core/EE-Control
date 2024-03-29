package at.uibk.dps.ee.control.enactment;

import com.google.inject.ImplementedBy;
import io.vertx.core.eventbus.EventBus;
import net.sf.opendse.model.Task;

/**
 * Interface for the operations which are to be performed after the enactment
 * associated with a task has been completed (such as putting the task into a
 * queue of the {@link EnactmentQueues}).
 * 
 * @author Fedor Smirnov
 */
@ImplementedBy(PostEnactmentDefault.class)
public interface PostEnactment {

  /**
   * Performs the required post enactment operation for the given task.
   * 
   * @param enactedTask the task which was just enacted.
   * @param eBus reference to the event bus used by the correct verticle
   */
  void postEnactmentTreatment(Task enactedTask, EventBus eBus);
}
