package at.uibk.dps.ee.control.scheduling;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.inject.Inject;
import at.uibk.dps.ee.control.verticles.ConstantsVertX;
import at.uibk.dps.ee.control.verticles.VerticleApollo;
import at.uibk.dps.ee.control.verticles.WorkerException;
import at.uibk.dps.ee.model.graph.SpecificationProvider;
import at.uibk.dps.sc.core.ScheduleModel;
import at.uibk.dps.sc.core.arbitration.ResourceArbiter;
import at.uibk.dps.sc.core.capacity.CapacityLimitException;
import at.uibk.dps.sc.core.scheduler.Scheduler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.shareddata.Lock;
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
  protected final ResourceArbiter arbiter;

  protected final List<Task> waitingTasks;

  protected final Logger logger = LoggerFactory.getLogger(WorkerScheduling.class);

  /**
   * Injection constructor
   * 
   * @param specProvider the specification provider
   * @param schedule the schedule model
   * @param scheduler the scheduler
   */
  @Inject
  public WorkerScheduling(final SpecificationProvider specProvider, final ScheduleModel schedule,
      final Scheduler scheduler, final ResourceArbiter arbiter) {
    super(ConstantsVertX.addressTaskSchedulable, ConstantsVertX.addressTaskLaunchable,
        ConstantsVertX.addressFailureAbort, specProvider);
    this.schedule = schedule;
    this.scheduler = scheduler;
    this.arbiter = arbiter;
    this.waitingTasks = new ArrayList<>();
  }

  @Override
  public void start() throws Exception {
    super.start();
    this.vertx.eventBus().consumer(ConstantsVertX.addressResourceFreed, this::processFreedResource);
  }

  /**
   * Reads the freed resource from the eBus message
   * 
   * @param resMessage the eBus message
   */
  protected void processFreedResource(final Message<String> resMessage) {
    final Resource freedRes = rGraph.getVertex(resMessage.body());
    this.vertx.sharedData().getLock(ConstantsVertX.waitingListLock, lockRes -> {
      if (lockRes.succeeded()) {
        Lock waitingListLock = lockRes.result();
        considerWaiting(freedRes);
        waitingListLock.release();
      } else {
        throw new IllegalStateException("Failed to acquire waiting list lock");
      }
    });
  }

  /**
   * Chooses the waiting task to try to reschedule on the given resource.
   * 
   * @param res the given resource (which was just freed)
   */
  protected void considerWaiting(Resource res) {
    List<Task> relevant = waitingTasks.stream() //
        .filter(t -> mappings.getMappings(t).stream() //
            .anyMatch(m -> m.getTarget().equals(res))) //
        .collect(Collectors.toList());
    if (!relevant.isEmpty()) {
      try {
        List<Task> prioList = arbiter.prioritizeTasks(relevant, res);
        for (Task taskToSchedule : prioList) {
          logger.debug("Attempting to schedule Task {} from the wait list.",
              taskToSchedule.getId());
          waitingTasks.remove(taskToSchedule);
          work(taskToSchedule);
        }
      } catch (WorkerException e) {
        failureHandler(e);
      }
    }
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
        if (asyncRes.cause() instanceof CapacityLimitException) {
          Task waiting = ((CapacityLimitException) asyncRes.cause()).getUnscheduledTask();
          logger.debug("Task {} added to waiting list.", schedulableTask.getId());
          waitingTasks.add(waiting);
        } else {
          throw new IllegalArgumentException("Async scheduling call failed.");
        }
      }
    });
  }

  /**
   * Processes the mappings chosen by the scheduler
   * 
   * @param task the task being scheduled
   * @param chosenMappings the chosen mappings
   */
  protected void processChosenMappings(final Task task,
      final Set<Mapping<Task, Resource>> chosenMappings) {
    schedule.setTaskSchedule(task, chosenMappings);
    this.vertx.eventBus().send(successAddress, task.getId());
    if (schedule.isScheduled(task)) {
      logger.debug("Thread {}; Task {} scheduled", Thread.currentThread().getId(), task.getId());
    }
  }
}
