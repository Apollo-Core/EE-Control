package at.uibk.dps.ee.control.scheduling;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import at.uibk.dps.ee.control.verticles.ConstantsVertX;
import at.uibk.dps.ee.control.verticles.WorkerException;
import at.uibk.dps.ee.model.graph.MappingsConcurrent;
import at.uibk.dps.ee.model.graph.SpecificationProvider;
import at.uibk.dps.sc.core.ScheduleModel;
import at.uibk.dps.sc.core.arbitration.ResourceArbiter;
import at.uibk.dps.sc.core.capacity.CapacityLimitException;
import at.uibk.dps.sc.core.scheduler.Scheduler;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import net.sf.opendse.model.Resource;
import net.sf.opendse.model.Mapping;
import net.sf.opendse.model.Task;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.doNothing;

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

  Vertx vMock;
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

  @Test
  void testCapacityLimitationReaction() {
    Task notScheduled = new Task("notScheduled");
    CapacityLimitException limiExc = new CapacityLimitException(notScheduled);

    assertTrue(tested.waitingTasks.isEmpty());
    Future<Set<Mapping<Task, Resource>>> failedFuture = Future.failedFuture(limiExc);
    when(scheduler.scheduleTask(notScheduled)).thenReturn(failedFuture);

    try {
      tested.work(notScheduled);
      assertTrue(tested.waitingTasks.contains(notScheduled));
    } catch (WorkerException e) {
      fail();
    }
  }

  @Test
  void testConsiderWaiting() throws WorkerException {
    Task t1 = new Task("t1");
    Task t2 = new Task("t2");
    Task t3 = new Task("t3");

    Resource res1 = new Resource("res1");
    Resource res2 = new Resource("res2");

    Mapping<Task, Resource> m1 = new Mapping<Task, Resource>("m1", t1, res1);
    Mapping<Task, Resource> m2 = new Mapping<Task, Resource>("m2", t2, res1);
    Mapping<Task, Resource> m3 = new Mapping<Task, Resource>("m3", t1, res2);
    Mapping<Task, Resource> m4 = new Mapping<Task, Resource>("m4", t2, res2);
    Mapping<Task, Resource> m5 = new Mapping<Task, Resource>("m5", t3, res2);
    MappingsConcurrent mappings = new MappingsConcurrent();
    mappings.addMapping(m1);
    mappings.addMapping(m2);
    mappings.addMapping(m3);
    mappings.addMapping(m4);
    mappings.addMapping(m5);

    SpecificationProvider specProv = mock(SpecificationProvider.class);
    when(specProv.getMappings()).thenReturn(mappings);

    List<Task> valid = new ArrayList<>();
    valid.add(t1);
    valid.add(t2);
    List<Task> arbited = new ArrayList<>();
    arbited.add(t2);
    arbited.add(t1);

    ResourceArbiter arbiter = mock(ResourceArbiter.class);
    when(arbiter.prioritizeTasks(valid, res1)).thenReturn(arbited);

    Scheduler schedMock = mock(Scheduler.class);

    MockWorker waitTest = new MockWorker(specProv, scheduleModel, schedMock, arbiter);
    MockWorker waitSpy = spy(waitTest);
    waitSpy.waitingTasks.add(t1);
    waitSpy.waitingTasks.add(t2);
    waitSpy.waitingTasks.add(t3);

    doNothing().when(waitSpy).work(t1);
    doNothing().when(waitSpy).work(t2);

    waitSpy.considerWaiting(res1);

    assertFalse(waitSpy.waitingTasks.contains(t1));
    assertFalse(waitSpy.waitingTasks.contains(t2));
    verify(waitSpy).work(t1);
    verify(waitSpy).work(t2);
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
    vMock = mock(Vertx.class);
    eBus = mock(EventBus.class);
    when(vMock.eventBus()).thenReturn(eBus);
    tested.setVertX(vMock);
  }
}
