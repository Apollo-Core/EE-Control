package at.uibk.dps.ee.control.scheduling;

import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.inject.Inject;
import at.uibk.dps.ee.control.verticles.ConstantsVertX;
import at.uibk.dps.ee.control.verticles.VerticleApollo;
import at.uibk.dps.ee.control.verticles.WorkerException;
import at.uibk.dps.ee.model.graph.EnactmentGraphProvider;
import at.uibk.dps.sc.core.ScheduleModel;
import at.uibk.dps.sc.core.scheduler.Scheduler;
import net.sf.opendse.model.Mapping;
import net.sf.opendse.model.Resource;
import net.sf.opendse.model.Task;

/**
 * Worker scheduling the tasks.
 * 
 * @author Fedor Smirnov
 *
 */
public class WorkerScheduling extends VerticleApollo {

  protected final ScheduleModel schedule;
  protected final Scheduler scheduler;

  protected final Logger logger = LoggerFactory.getLogger(WorkerScheduling.class);

  /**
   * Injection constructor
   * 
   * @param graphProvider e graph provider
   * @param schedule the schedule model
   * @param scheduler the scheduler
   */
  @Inject
  public WorkerScheduling(final EnactmentGraphProvider graphProvider, final ScheduleModel schedule,
      final Scheduler scheduler) {
    super(ConstantsVertX.addressTaskSchedulable, ConstantsVertX.addressTaskLaunchable,
        ConstantsVertX.addressFailureAbort, graphProvider);
    this.schedule = schedule;
    this.scheduler = scheduler;
  }

  @Override
  protected void work(final Task schedulableTask) throws WorkerException {
    if (schedule.isScheduled(schedulableTask)) {
      throw new WorkerException("Task " + schedulableTask.getId() + " already scheduled.");
    }
    scheduler.scheduleTask(schedulableTask).onComplete(asyncRes -> {
      if (asyncRes.succeeded()) {
        processChosenMappings(schedulableTask, asyncRes.result());
      } else {
        throw new IllegalArgumentException("Async scheduling call failed.");
      }
    });
  }

  /**
   * Processes the mappings chosen by the scheduler
   * 
   * @param task the task being scheduled
   * @param chosenMappings the chosen mappings
   */
  protected void processChosenMappings(Task task, Set<Mapping<Task, Resource>> chosenMappings) {
    schedule.setTaskSchedule(task, chosenMappings);
    this.vertx.eventBus().send(successAddress, task.getId());
    if (schedule.isScheduled(task)) {
      logger.debug("Thread {}; Task {} scheduled", Thread.currentThread().getId(), task.getId());
    }
  }
}
