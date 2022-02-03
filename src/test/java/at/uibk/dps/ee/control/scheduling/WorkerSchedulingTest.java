package at.uibk.dps.ee.control.scheduling;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import at.uibk.dps.ee.control.verticles.ConstantsVertX;
import at.uibk.dps.ee.control.verticles.WorkerException;
import at.uibk.dps.ee.model.graph.SpecificationProvider;
import at.uibk.dps.sc.core.ScheduleModel;
import at.uibk.dps.sc.core.arbitration.ResourceArbiter;
import at.uibk.dps.sc.core.scheduler.Scheduler;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import net.sf.opendse.model.Resource;
import net.sf.opendse.model.Mapping;
import net.sf.opendse.model.Task;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.util.HashSet;
import java.util.Set;
import static org.mockito.Mockito.verify;;

class WorkerSchedulingTest {

  protected class MockWorker extends WorkerScheduling {

    public MockWorker(SpecificationProvider specProvider, ScheduleModel schedule,
        Scheduler scheduler, ResourceArbiter arbiter) {
      super(specProvider, schedule, scheduler, arbiter);
    }

    public void setVertX(Vertx vertx) {
      this.vertx = vertx;
    }
  }

  EventBus eBus;
  MockWorker tested;

  Scheduler scheduler;
  ScheduleModel scheduleModel;

  Task input;
  Set<Mapping<Task, Resource>> schedule;

  ResourceArbiter arbiter;

  /**
   * Failure since task already scheduled.
   */
  @Test
  void testScheduled() {
    when(scheduleModel.isScheduled(input)).thenReturn(true);
    assertThrows(WorkerException.class, () -> {
      tested.work(input);
    });
  }

  /**
   * Normal scheduling
   */
  @Test
  void testNotScheduled() {
    when(scheduleModel.isScheduled(input)).thenReturn(false);
    try {
      tested.work(input);
      verify(scheduleModel).setTaskSchedule(input, schedule);
      verify(eBus).send(ConstantsVertX.addressTaskLaunchable, input.getId());
    } catch (WorkerException e) {
      fail();
    }
  }

  @BeforeEach
  void setUp() {
    SpecificationProvider specProv = mock(SpecificationProvider.class);
    input = new Task("task");
    schedule = new HashSet<>();
    scheduler = mock(Scheduler.class);
    Future<Set<Mapping<Task, Resource>>> futureSched = Future.succeededFuture(schedule);
    when(scheduler.scheduleTask(input)).thenReturn(futureSched);
    scheduleModel = mock(ScheduleModel.class);
    arbiter = mock(ResourceArbiter.class);
    tested = new MockWorker(specProv, scheduleModel, scheduler, arbiter);
    Vertx vMock = mock(Vertx.class);
    eBus = mock(EventBus.class);
    when(vMock.eventBus()).thenReturn(eBus);
    tested.setVertX(vMock);
  }
}
