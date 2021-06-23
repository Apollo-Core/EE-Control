package at.uibk.dps.ee.control.verticles.scheduling;

import at.uibk.dps.ee.control.verticles.ConstantsEventBus;
import at.uibk.dps.ee.control.verticles.HandlerApollo;
import at.uibk.dps.ee.control.verticles.WorkerException;
import at.uibk.dps.ee.model.graph.EnactmentGraph;
import at.uibk.dps.sc.core.ScheduleModel;
import io.vertx.core.eventbus.EventBus;
import net.sf.opendse.model.Task;

public class ScheduleReset extends HandlerApollo<Task> {

  protected final ScheduleModel schedule;

  public ScheduleReset(EventBus eBus, EnactmentGraph eGraph, ScheduleModel schedule) {
    super(ConstantsEventBus.addressResetScheduleTask, ConstantsEventBus.addressPlaceholder,
        ConstantsEventBus.addressFailureAbort, eBus, eGraph);
    this.schedule = schedule;
  }

  @Override
  protected Task readMessage(String message) {
    return eGraph.getVertex(message);
  }

  @Override
  protected void work(Task graphElement) throws WorkerException {
    if (schedule.isScheduled(graphElement)) {
      schedule.resetTaskSchedule(graphElement);
    }else {
      throw new WorkerException("Schedule of Task " + graphElement.getId() + " not reset since not scheduled.");
    }
  }
}
