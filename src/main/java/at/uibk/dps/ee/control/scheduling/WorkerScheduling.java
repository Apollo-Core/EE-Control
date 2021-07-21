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
  public WorkerScheduling(EnactmentGraphProvider graphProvider, ScheduleModel schedule,
      Scheduler scheduler) {
    super(ConstantsVertX.addressTaskSchedulable, ConstantsVertX.addressTaskLaunchable,
        ConstantsVertX.addressFailureAbort, graphProvider);
    this.schedule = schedule;
    this.scheduler = scheduler;
  }

  @Override
  protected void work(Task schedulableTask) throws WorkerException {
    if (schedule.isScheduled(schedulableTask)) {
      throw new WorkerException("Task " + schedulableTask.getId() + " already scheduled.");
    } else {
      final Set<Mapping<Task, Resource>> taskSchedule = scheduler.scheduleTask(schedulableTask);
      schedule.setTaskSchedule(schedulableTask, taskSchedule);
    }
    logger.debug("Thread {}; Task {} scheduled.", Thread.currentThread().getId(),
        schedulableTask.getId());
    this.vertx.eventBus().send(successAddress, schedulableTask.getId());
  }
}
