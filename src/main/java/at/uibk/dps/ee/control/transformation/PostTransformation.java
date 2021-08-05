package at.uibk.dps.ee.control.transformation;

import com.google.inject.ImplementedBy;
import io.vertx.core.eventbus.EventBus;
import net.sf.opendse.model.Task;

/**
 * Interface for the operations which transformation workers perform after a
 * graph transformation is finished. The operations include, e.g., sending
 * messages to the event bus to mark the completion of the transformation work.
 * 
 * @author Fedor Smirnov
 */
@ImplementedBy(PostTransformationDefault.class)
public interface PostTransformation {

  /**
   * Performs the action required after the transformation operation.
   * 
   * @param transformationTriggerTask the task which triggerred the transformation
   *        operation
   * @param eventBus the vertX event bus
   */
  void postTransformationTreatment(Task transformationTriggerTask, EventBus eventBus);
}
