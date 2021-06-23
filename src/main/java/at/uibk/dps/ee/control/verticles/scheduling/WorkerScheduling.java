package at.uibk.dps.ee.control.verticles.scheduling;

import java.util.Set;
import at.uibk.dps.ee.control.verticles.ConstantsEventBus;
import at.uibk.dps.ee.control.verticles.HandlerApollo;
import at.uibk.dps.ee.control.verticles.WorkerException;
import at.uibk.dps.ee.core.enactable.Enactable;
import at.uibk.dps.ee.core.enactable.EnactmentFunction;
import at.uibk.dps.ee.model.graph.EnactmentGraph;
import at.uibk.dps.ee.model.properties.PropertyServiceFunction;
import at.uibk.dps.sc.core.ScheduleModel;
import at.uibk.dps.sc.core.interpreter.ScheduleInterpreter;
import at.uibk.dps.sc.core.scheduler.Scheduler;
import io.vertx.core.eventbus.EventBus;
import net.sf.opendse.model.Mapping;
import net.sf.opendse.model.Resource;
import net.sf.opendse.model.Task;

public class WorkerScheduling extends HandlerApollo<Task> {

  protected final ScheduleModel schedule;
  protected final Scheduler scheduler;
  protected final ScheduleInterpreter interpreter;

  public WorkerScheduling(EventBus eBus, EnactmentGraph graph, ScheduleModel schedule,
      Scheduler scheduler, ScheduleInterpreter interpreter) {
    super(ConstantsEventBus.addressTaskSchedulable, ConstantsEventBus.addressTaskLaunchable,
        ConstantsEventBus.addressFailureAbort, eBus, graph);
      this.schedule = schedule;
      this.scheduler = scheduler;
      this.interpreter = interpreter;
  }

  @Override
  protected Task readMessage(String message) {
    return eGraph.getVertex(message);
  }

  @Override
  protected void work(Task schedulableTask) throws WorkerException {
    if (schedule.isScheduled(schedulableTask)) {
      throw new WorkerException("Task " + schedulableTask.getId() + " already scheduled.");
    } else {
      final Set<Mapping<Task, Resource>> taskSchedule = scheduler.scheduleTask(schedulableTask);
      schedule.setTaskSchedule(schedulableTask, taskSchedule);
      final Enactable taskEnactable = PropertyServiceFunction.getEnactable(schedulableTask);
      final EnactmentFunction enactmentFunction =
          interpreter.interpretSchedule(schedulableTask, taskSchedule);
      taskEnactable.schedule(enactmentFunction);
    }
    eBus.publish(successAddress, schedulableTask.getId());
  }
}
