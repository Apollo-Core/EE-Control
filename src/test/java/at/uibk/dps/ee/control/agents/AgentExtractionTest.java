package at.uibk.dps.ee.control.agents;

import static org.junit.Assert.*;
import org.junit.Test;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import at.uibk.dps.ee.control.graph.GraphAccess;
import at.uibk.dps.ee.control.management.EnactmentQueues;
import at.uibk.dps.ee.core.function.Enactable;
import at.uibk.dps.ee.core.function.Enactable.State;
import at.uibk.dps.ee.model.constants.ConstantsEEModel;
import at.uibk.dps.ee.model.graph.EnactmentGraph;
import at.uibk.dps.ee.model.properties.PropertyServiceData;
import at.uibk.dps.ee.model.properties.PropertyServiceDependency;
import at.uibk.dps.ee.model.properties.PropertyServiceFunction;
import at.uibk.dps.sc.core.ScheduleModel;
import edu.uci.ics.jung.graph.util.EdgeType;
import net.sf.opendse.model.Communication;
import net.sf.opendse.model.Dependency;
import net.sf.opendse.model.Task;

import static org.mockito.Mockito.mock;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import java.util.HashSet;

public class AgentExtractionTest {

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

    GraphAccess gAccess = mock(GraphAccess.class);
    EnactmentQueues mockQueues = mock(EnactmentQueues.class);
    ScheduleModel schedule = new ScheduleModel();
    schedule.setTaskSchedule(finished, new HashSet<>());
    assertTrue(schedule.isScheduled(finished));
    AgentExtraction tested0 =
        new AgentExtraction(finished, d0, c0, mockQueues, new HashSet<>(), gAccess, schedule);

    tested0.annotateExtractionEdge(graph, d0);
    assertTrue(PropertyServiceDependency.isExtractionDone(d0));
    verify(mockEnactable, never()).setState(State.WAITING);

    tested0.annotateExtractionEdge(graph, d1);
    assertFalse(PropertyServiceDependency.isExtractionDone(d0));
    assertFalse(PropertyServiceDependency.isExtractionDone(d1));
    verify(mockEnactable).setState(State.WAITING);
    assertFalse(schedule.isScheduled(finished));
  }

  @Test
  public void test() {
    Task finished = new Task("finished");
    Dependency dep = new Dependency("dep");
    Communication dataNode = new Communication("data");
    Enactable mockEnactable = mock(Enactable.class);
    JsonObject result = new JsonObject();
    JsonElement content = new JsonPrimitive(42);
    result.add("key", content);
    when(mockEnactable.getResult()).thenReturn(result);
    PropertyServiceFunction.setEnactable(finished, mockEnactable);
    EnactmentQueues mockState = mock(EnactmentQueues.class);
    PropertyServiceDependency.setJsonKey(dep, "key");
    GraphAccess gAccess = mock(GraphAccess.class);
    ScheduleModel schedule = new ScheduleModel();
    AgentExtraction tested =
        new AgentExtraction(finished, dep, dataNode, mockState, new HashSet<>(), gAccess, schedule);
    String expectedMessage = ConstantsAgents.ExcMessageExtractionPrefix + finished.getId()
        + ConstantsAgents.ExcMessageExtractionSuffix + dataNode.getId();
    assertEquals(expectedMessage, tested.formulateExceptionMessage());
    try {
      tested.actualCall();
    } catch (Exception e) {
      fail();
    }
    assertEquals(content, PropertyServiceData.getContent(dataNode));
    verify(mockState).putAvailableData(dataNode);
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
    EnactmentQueues mockState = mock(EnactmentQueues.class);
    GraphAccess gAccess = mock(GraphAccess.class);
    PropertyServiceDependency.setJsonKey(dep, ConstantsEEModel.JsonKeySequentiality);
    ScheduleModel schedule = new ScheduleModel();
    AgentExtraction tested =
        new AgentExtraction(finished, dep, dataNode, mockState, new HashSet<>(), gAccess, schedule);
    try {
      tested.actualCall();
    } catch (Exception e) {
      fail();
    }
    assertEquals(true, PropertyServiceData.getContent(dataNode).getAsBoolean());
    verify(mockState).putAvailableData(dataNode);
  }

  @Test
  public void testNoKey() {
    Task finished = new Task("finished");
    Dependency dep = new Dependency("dep");
    Communication dataNode = new Communication("data");
    Enactable mockEnactable = mock(Enactable.class);
    JsonObject result = new JsonObject();
    when(mockEnactable.getResult()).thenReturn(result);
    PropertyServiceFunction.setEnactable(finished, mockEnactable);
    EnactmentQueues mockState = mock(EnactmentQueues.class);
    PropertyServiceDependency.setJsonKey(dep, "key");
    GraphAccess gAccess = mock(GraphAccess.class);
    ScheduleModel schedule = new ScheduleModel();
    AgentExtraction tested =
        new AgentExtraction(finished, dep, dataNode, mockState, new HashSet<>(), gAccess, schedule);
    String expectedMessage = ConstantsAgents.ExcMessageExtractionPrefix + finished.getId()
        + ConstantsAgents.ExcMessageExtractionSuffix + dataNode.getId();
    assertEquals(expectedMessage, tested.formulateExceptionMessage());
    try {
      tested.actualCall();
      fail();
    } catch (Exception e) {
      assertTrue(e instanceof IllegalStateException);
    }
  }
}
