package at.uibk.dps.ee.control.management;

import static org.junit.Assert.*;
import org.junit.Test;
import at.uibk.dps.ee.core.function.Enactable.State;
import at.uibk.dps.ee.enactables.EnactableAtomic;
import at.uibk.dps.ee.model.constants.ConstantsEEModel;
import at.uibk.dps.ee.model.graph.ResourceGraph;
import at.uibk.dps.ee.model.graph.ResourceGraphProvider;
import at.uibk.dps.ee.model.properties.PropertyServiceFunctionUser;
import at.uibk.dps.ee.model.properties.PropertyServiceFunctionUtilityCollections;
import at.uibk.dps.ee.model.properties.PropertyServiceFunctionUtilityCollections.CollectionOperation;
import at.uibk.dps.ee.model.properties.PropertyServiceResource;
import at.uibk.dps.sc.core.ScheduleModel;
import net.sf.opendse.model.Mapping;
import net.sf.opendse.model.Resource;
import net.sf.opendse.model.Task;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.util.HashSet;
import java.util.Set;

public class ResourceMonitorTest {

  @Test
  public void testStateChange() {

    ScheduleModel scheduleMock = mock(ScheduleModel.class);
    ResourceGraphProvider provMock = mock(ResourceGraphProvider.class);
    ResourceGraph resGraph = new ResourceGraph();
    Resource ee = new Resource(ConstantsEEModel.idLocalResource);
    resGraph.addVertex(ee);
    when(provMock.getResourceGraph()).thenReturn(resGraph);

    Task task = PropertyServiceFunctionUser.createUserTask("tasky", "addition");
    Resource res1 = new Resource("res1");
    Resource res2 = new Resource("res2");
    Mapping<Task, Resource> mapping1 = new Mapping<>("m1", task, res1);
    Mapping<Task, Resource> mapping2 = new Mapping<>("m2", task, res2);
    Set<Mapping<Task, Resource>> schedule = new HashSet<>();
    schedule.add(mapping1);
    schedule.add(mapping2);

    when(scheduleMock.isScheduled(task)).thenReturn(true);
    when(scheduleMock.getTaskSchedule(task)).thenReturn(schedule);
    EnactableAtomic atomic = mock(EnactableAtomic.class);
    when(atomic.getFunctionNode()).thenReturn(task);

    ResourceMonitor tested = new ResourceMonitor(scheduleMock, provMock);
    tested.enactableStateChanged(atomic, State.LAUNCHABLE, State.RUNNING);
    assertEquals(task.getId(), PropertyServiceResource.getUsingTaskIds(res1).iterator().next());
    assertEquals(task.getId(), PropertyServiceResource.getUsingTaskIds(res2).iterator().next());

    tested.enactableStateChanged(atomic, State.RUNNING, State.FINISHED);
    assertTrue(PropertyServiceResource.getUsingTaskIds(res1).isEmpty());
    assertTrue(PropertyServiceResource.getUsingTaskIds(res2).isEmpty());
  }

  @Test
  public void testUser() {
    ScheduleModel scheduleMock = mock(ScheduleModel.class);
    ResourceGraphProvider provMock = mock(ResourceGraphProvider.class);
    ResourceGraph resGraph = new ResourceGraph();
    Resource ee = new Resource(ConstantsEEModel.idLocalResource);
    resGraph.addVertex(ee);
    when(provMock.getResourceGraph()).thenReturn(resGraph);

    Task task = PropertyServiceFunctionUser.createUserTask("tasky", "addition");
    Resource res1 = new Resource("res1");
    Resource res2 = new Resource("res2");
    Mapping<Task, Resource> mapping1 = new Mapping<>("m1", task, res1);
    Mapping<Task, Resource> mapping2 = new Mapping<>("m2", task, res2);
    Set<Mapping<Task, Resource>> schedule = new HashSet<>();
    schedule.add(mapping1);
    schedule.add(mapping2);

    when(scheduleMock.isScheduled(task)).thenReturn(true);
    when(scheduleMock.getTaskSchedule(task)).thenReturn(schedule);
    EnactableAtomic atomic = mock(EnactableAtomic.class);
    when(atomic.getFunctionNode()).thenReturn(task);

    ResourceMonitor tested = new ResourceMonitor(scheduleMock, provMock);
    Set<Resource> result = tested.getResourceOfAtomic(atomic);
    assertEquals(2, result.size());
    assertTrue(result.contains(res1));
    assertTrue(result.contains(res2));
  }

  @Test
  public void testLocal() {
    ScheduleModel scheduleMock = mock(ScheduleModel.class);
    ResourceGraphProvider provMock = mock(ResourceGraphProvider.class);
    ResourceGraph resGraph = new ResourceGraph();
    Resource ee = new Resource(ConstantsEEModel.idLocalResource);
    resGraph.addVertex(ee);
    when(provMock.getResourceGraph()).thenReturn(resGraph);

    Task task = PropertyServiceFunctionUser.createUserTask("tasky", "addition");
    when(scheduleMock.isScheduled(task)).thenReturn(true);
    when(scheduleMock.getTaskSchedule(task)).thenReturn(new HashSet<>());
    EnactableAtomic atomic = mock(EnactableAtomic.class);
    when(atomic.getFunctionNode()).thenReturn(task);

    ResourceMonitor tested = new ResourceMonitor(scheduleMock, provMock);
    Set<Resource> result = tested.getResourceOfAtomic(atomic);
    assertEquals(1, result.size());
    assertTrue(result.contains(ee));
  }

  @Test
  public void testUtility() {
    ScheduleModel scheduleMock = mock(ScheduleModel.class);
    ResourceGraphProvider provMock = mock(ResourceGraphProvider.class);
    ResourceGraph resGraph = new ResourceGraph();
    Resource ee = new Resource(ConstantsEEModel.idLocalResource);
    resGraph.addVertex(ee);
    when(provMock.getResourceGraph()).thenReturn(resGraph);

    Task task = PropertyServiceFunctionUtilityCollections.createCollectionOperation("bla", "bla",
        CollectionOperation.Block);
    when(scheduleMock.isScheduled(task)).thenReturn(false);
    EnactableAtomic atomic = mock(EnactableAtomic.class);
    when(atomic.getFunctionNode()).thenReturn(task);

    ResourceMonitor tested = new ResourceMonitor(scheduleMock, provMock);
    Set<Resource> result = tested.getResourceOfAtomic(atomic);
    assertEquals(1, result.size());
    assertTrue(result.contains(ee));
  }

  @Test(expected = IllegalStateException.class)
  public void testResException() {
    ScheduleModel scheduleMock = mock(ScheduleModel.class);
    ResourceGraphProvider provMock = mock(ResourceGraphProvider.class);
    ResourceGraph resGraph = new ResourceGraph();
    Resource ee = new Resource(ConstantsEEModel.idLocalResource);
    resGraph.addVertex(ee);
    when(provMock.getResourceGraph()).thenReturn(resGraph);

    Task task = PropertyServiceFunctionUser.createUserTask("tasky", "addition");
    when(scheduleMock.isScheduled(task)).thenReturn(false);
    EnactableAtomic atomic = mock(EnactableAtomic.class);
    when(atomic.getFunctionNode()).thenReturn(task);

    ResourceMonitor tested = new ResourceMonitor(scheduleMock, provMock);
    tested.getResourceOfAtomic(atomic);
  }
}
