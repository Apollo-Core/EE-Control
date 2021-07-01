package at.uibk.dps.ee.control.agents;

import static org.junit.Assert.*;
import org.junit.Test;
import at.uibk.dps.ee.control.management.EnactmentQueues;
import at.uibk.dps.ee.core.function.Enactable;
import at.uibk.dps.ee.core.function.EnactmentFunction;
import at.uibk.dps.ee.model.properties.PropertyServiceFunction;
import at.uibk.dps.sc.core.ScheduleModel;
import at.uibk.dps.sc.core.interpreter.ScheduleInterpreter;
import at.uibk.dps.sc.core.scheduler.Scheduler;
import net.sf.opendse.model.Mapping;
import net.sf.opendse.model.Resource;
import net.sf.opendse.model.Task;
import static org.mockito.Mockito.mock;

import static org.mockito.Mockito.when;
import java.util.HashSet;
import java.util.Set;
import static org.mockito.Mockito.verify;

public class AgentSchedulingTest {

  @Test
  public void test() {

    ScheduleModel scheduleMock = mock(ScheduleModel.class);
    Scheduler schedulerMock = mock(Scheduler.class);
    Task task = new Task("task");
    EnactmentQueues stateMock = mock(EnactmentQueues.class);
    ScheduleInterpreter interpreterMock = mock(ScheduleInterpreter.class);
    Set<AgentTaskListener> listeners = new HashSet<>();
    Enactable enactableMock = mock(Enactable.class);
    PropertyServiceFunction.setEnactable(task, enactableMock);
    Set<Mapping<Task, Resource>> schedule = new HashSet<>();
    when(schedulerMock.scheduleTask(task)).thenReturn(schedule);
    EnactmentFunction functionMock = mock(EnactmentFunction.class);
    when(interpreterMock.interpretSchedule(task, schedule)).thenReturn(functionMock);

    AgentScheduling tested = new AgentScheduling(scheduleMock, schedulerMock, task, stateMock,
        interpreterMock, listeners);
    assertEquals(ConstantsAgents.ExcMessageScheduling + task.getId(),
        tested.formulateExceptionMessage());

    try {
      tested.actualCall();
      verify(stateMock).putLaunchableTask(task);
      verify(enactableMock).schedule(functionMock);
    } catch (Exception e) {
      fail();
    }
  }

  @Test
  public void testAlreadyScheduled() {
    ScheduleModel scheduleMock = mock(ScheduleModel.class);
    Scheduler schedulerMock = mock(Scheduler.class);
    Task task = new Task("task");
    EnactmentQueues stateMock = mock(EnactmentQueues.class);
    ScheduleInterpreter interpreterMock = mock(ScheduleInterpreter.class);
    Set<AgentTaskListener> listeners = new HashSet<>();
    Enactable enactableMock = mock(Enactable.class);
    PropertyServiceFunction.setEnactable(task, enactableMock);
    Set<Mapping<Task, Resource>> schedule = new HashSet<>();
    when(scheduleMock.isScheduled(task)).thenReturn(true);
    when(schedulerMock.scheduleTask(task)).thenReturn(schedule);
    EnactmentFunction functionMock = mock(EnactmentFunction.class);
    when(interpreterMock.interpretSchedule(task, schedule)).thenReturn(functionMock);

    AgentScheduling tested = new AgentScheduling(scheduleMock, schedulerMock, task, stateMock,
        interpreterMock, listeners);
    try {
      tested.actualCall();
    } catch (IllegalStateException e) {
    } catch (Exception e) {
      fail();
    }
  }
}
