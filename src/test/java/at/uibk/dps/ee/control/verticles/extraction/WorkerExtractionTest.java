package at.uibk.dps.ee.control.verticles.extraction;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.HashSet;
import org.junit.Test;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import at.uibk.dps.ee.control.verticles.ConstantsEventBus;
import at.uibk.dps.ee.control.verticles.WorkerException;
import at.uibk.dps.ee.core.enactable.Enactable;
import at.uibk.dps.ee.core.enactable.Enactable.State;
import at.uibk.dps.ee.model.constants.ConstantsEEModel;
import at.uibk.dps.ee.model.graph.EnactmentGraph;
import at.uibk.dps.ee.model.properties.PropertyServiceData;
import at.uibk.dps.ee.model.properties.PropertyServiceDependency;
import at.uibk.dps.ee.model.properties.PropertyServiceFunction;
import at.uibk.dps.sc.core.ScheduleModel;
import edu.uci.ics.jung.graph.util.EdgeType;
import io.vertx.core.eventbus.EventBus;
import net.sf.opendse.model.Communication;
import net.sf.opendse.model.Dependency;
import net.sf.opendse.model.Task;

public class WorkerExtractionTest {

  @Test
  public void testNoKey() {
    Task finished = new Task("finished");
    Dependency dep = new Dependency("dep");
    Communication dataNode = new Communication("data");
    Enactable mockEnactable = mock(Enactable.class);
    JsonObject result = new JsonObject();
    when(mockEnactable.getResult()).thenReturn(result);
    PropertyServiceFunction.setEnactable(finished, mockEnactable);
    PropertyServiceDependency.setJsonKey(dep, "key");
    EnactmentGraph eGraph = new EnactmentGraph();
    eGraph.addEdge(dep, finished, dataNode, EdgeType.DIRECTED);
    EventBus mockBus = mock(EventBus.class);
    WorkerExtraction tested = new WorkerExtraction(eGraph, mockBus);
    try {
      tested.work(finished);
      fail();
    } catch (WorkerException e) {
      assertEquals(
          "The execution of the task finished did not produce an entry named key instead, we got {}",
          e.getMessage());
    }
  }

  @Test
  public void testSequentiality() {
    Task finished = new Task("finished");
    Dependency dep = new Dependency("dep");
    Task dataNode = PropertyServiceData.createSequentialityNode("node");
    Enactable mockEnactable = mock(Enactable.class);
    JsonObject result = new JsonObject();
    when(mockEnactable.getResult()).thenReturn(result);
    PropertyServiceFunction.setEnactable(finished, mockEnactable);
    PropertyServiceDependency.setJsonKey(dep, ConstantsEEModel.JsonKeySequentiality);
    EnactmentGraph eGraph = new EnactmentGraph();
    eGraph.addEdge(dep, finished, dataNode, EdgeType.DIRECTED);
    EventBus mockBus = mock(EventBus.class);
    WorkerExtraction tested = new WorkerExtraction(eGraph, mockBus);
    try {
      tested.work(finished);
    } catch (WorkerException e) {
      fail();
    }
    assertEquals(true, PropertyServiceData.getContent(dataNode).getAsBoolean());
    verify(mockBus).publish(ConstantsEventBus.addressDataAvailable, dataNode.getId());
  }

  @Test
  public void testWork() {
    Task finished = new Task("finished");
    Dependency dep = new Dependency("dep");
    Communication dataNode = new Communication("data");
    Enactable mockEnactable = mock(Enactable.class);
    JsonObject result = new JsonObject();
    JsonElement content = new JsonPrimitive(42);
    result.add("key", content);
    when(mockEnactable.getResult()).thenReturn(result);
    PropertyServiceFunction.setEnactable(finished, mockEnactable);
    PropertyServiceDependency.setJsonKey(dep, "key");
    EventBus mockBus = mock(EventBus.class);

    EnactmentGraph eGraph = new EnactmentGraph();
    eGraph.addEdge(dep, finished, dataNode, EdgeType.DIRECTED);
    WorkerExtraction tested = new WorkerExtraction(eGraph, mockBus);
    try {
      tested.work(finished);
    } catch (WorkerException e) {
      fail();
    }
    assertEquals(content, PropertyServiceData.getContent(dataNode));
    verify(mockBus).publish(ConstantsEventBus.addressDataAvailable, dataNode.getId());
  }

  @Test
  public void testReadMessage() {
    Task finished = new Task("t");
    Task c0 = new Communication("c0");
    Task c1 = new Communication("c1");

    Dependency d0 = new Dependency("d0");
    Dependency d1 = new Dependency("d1");

    EnactmentGraph graph = new EnactmentGraph();
    graph.addEdge(d0, finished, c0, EdgeType.DIRECTED);
    graph.addEdge(d1, finished, c1, EdgeType.DIRECTED);

    Enactable mockEnactable = mock(Enactable.class);
    when(mockEnactable.getState()).thenReturn(State.FINISHED);
    PropertyServiceFunction.setEnactable(finished, mockEnactable);

    ScheduleModel schedule = new ScheduleModel();
    schedule.setTaskSchedule(finished, new HashSet<>());
    assertTrue(schedule.isScheduled(finished));

    EventBus mockBus = mock(EventBus.class);

    WorkerExtraction tested0 = new WorkerExtraction(graph, mockBus);
    assertEquals(finished, tested0.readMessage("t"));
  }

  @Test
  public void testReset() {
    Task finished = new Task("t");
    Task c0 = new Communication("c0");
    Task c1 = new Communication("c1");

    Dependency d0 = new Dependency("d0");
    Dependency d1 = new Dependency("d1");

    EnactmentGraph graph = new EnactmentGraph();
    graph.addEdge(d0, finished, c0, EdgeType.DIRECTED);
    graph.addEdge(d1, finished, c1, EdgeType.DIRECTED);

    Enactable mockEnactable = mock(Enactable.class);
    when(mockEnactable.getState()).thenReturn(State.FINISHED);
    PropertyServiceFunction.setEnactable(finished, mockEnactable);

    ScheduleModel schedule = new ScheduleModel();
    schedule.setTaskSchedule(finished, new HashSet<>());
    assertTrue(schedule.isScheduled(finished));

    EventBus mockBus = mock(EventBus.class);

    WorkerExtraction tested0 = new WorkerExtraction(graph, mockBus);

    tested0.annotateExtractionEdge(d0);
    assertTrue(PropertyServiceDependency.isExtractionDone(d0));
    verify(mockEnactable, never()).setState(State.WAITING);
    verify(mockBus, never()).publish(ConstantsEventBus.addressResetScheduleTask, finished.getId());

    tested0.annotateExtractionEdge(d1);
    assertFalse(PropertyServiceDependency.isExtractionDone(d0));
    assertFalse(PropertyServiceDependency.isExtractionDone(d1));
    verify(mockEnactable).setState(State.WAITING);
    verify(mockBus).publish(ConstantsEventBus.addressResetScheduleTask, finished.getId());
  }

}
