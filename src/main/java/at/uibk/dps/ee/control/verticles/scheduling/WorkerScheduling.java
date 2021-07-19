package at.uibk.dps.ee.control.verticles.scheduling;

import java.util.Set;
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

public class WorkerScheduling extends VerticleApollo {

  protected final ScheduleModel schedule;
  protected final Scheduler scheduler;

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
    System.out.println(
        "Thread " + Thread.currentThread().getId() + " " + schedulableTask.getId() + " launchable");
    this.vertx.eventBus().send(successAddress, schedulableTask.getId());
  }
}
