package at.uibk.dps.ee.control.agents;

import static org.junit.Assert.*;
import org.junit.Test;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import at.uibk.dps.ee.control.graph.GraphAccess;
import at.uibk.dps.ee.control.management.EnactmentQueues;
import at.uibk.dps.ee.control.transmission.SchedulabilityCheck;
import at.uibk.dps.ee.control.transmission.SchedulabilityCheckDefault;
import at.uibk.dps.ee.core.enactable.Enactable;
import at.uibk.dps.ee.core.enactable.Enactable.State;
import at.uibk.dps.ee.model.graph.EnactmentGraph;
import at.uibk.dps.ee.model.properties.PropertyServiceData;
import at.uibk.dps.ee.model.properties.PropertyServiceDependency;
import at.uibk.dps.ee.model.properties.PropertyServiceFunction;
import at.uibk.dps.ee.model.properties.PropertyServiceData.DataType;
import edu.uci.ics.jung.graph.util.EdgeType;
import net.sf.opendse.model.Communication;
import net.sf.opendse.model.Dependency;
import net.sf.opendse.model.Task;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;
import java.util.HashSet;
import java.util.Set;

public class AgentTransmissionTest {

  @Test
  public void testAnnotateTransmission() {
    EnactmentGraph graph = new EnactmentGraph();

    Task c0 = new Communication("c0");
    JsonObject content0 = new JsonObject();
    PropertyServiceData.setContent(c0, content0);
    JsonObject content1 = new JsonObject();
    Task c1 = PropertyServiceData.createConstantNode("c1", DataType.Number, content1);

    Task t0 = new Task("t0");
    Task t1 = new Task("t1");
    Enactable mockEnactable0 = mock(Enactable.class);
    Enactable mockEnactable1 = mock(Enactable.class);
    PropertyServiceFunction.setEnactable(t0, mockEnactable0);
    PropertyServiceFunction.setEnactable(t1, mockEnactable1);

    String key = "key";
    Dependency d0 = PropertyServiceDependency.addDataDependency(c0, t0, key, graph);
    Dependency d1 = PropertyServiceDependency.addDataDependency(c0, t1, key, graph);
    Dependency d2 = PropertyServiceDependency.addDataDependency(c1, t0, key, graph);
    Dependency d3 = PropertyServiceDependency.addDataDependency(c1, t1, key, graph);

    EnactmentQueues queues = mock(EnactmentQueues.class);
    GraphAccess access = mock(GraphAccess.class);
    AgentTransmission tested0 = new AgentTransmission(queues, c0, d0, t0, access, new HashSet<>(),
        new SchedulabilityCheckDefault());
    AgentTransmission tested1 = new AgentTransmission(queues, c0, d1, t1, access, new HashSet<>(),
        new SchedulabilityCheckDefault());
    AgentTransmission tested2 = new AgentTransmission(queues, c1, d2, t0, access, new HashSet<>(),
        new SchedulabilityCheckDefault());
    AgentTransmission tested3 = new AgentTransmission(queues, c1, d3, t1, access, new HashSet<>(),
        new SchedulabilityCheckDefault());

    assertStatus(graph, true, true, false, false, false, false, false, false, false, false);
    tested0.annotateTransmission(graph, t0);
    assertStatus(graph, true, true, true, false, false, false, false, false, false, false);
    tested2.annotateTransmission(graph, t0);
    verify(mockEnactable0).setState(State.SCHEDULABLE);
    assertStatus(graph, true, true, true, false, true, false, true, false, true, false);
    tested3.annotateTransmission(graph, t1);
    assertStatus(graph, true, true, true, false, true, true, true, false, true, false);
    tested1.annotateTransmission(graph, t1);
    verify(mockEnactable1).setState(State.SCHEDULABLE);
    assertStatus(graph, false, true, false, false, true, true, false, false, true, true);

  }

  protected static void assertStatus(EnactmentGraph graph, boolean contentC0, boolean contentC1,
      boolean transD0, boolean transD1, boolean transD2, boolean transD3, boolean consumedD0,
      boolean consumedD1, boolean consumedD2, boolean consumedD3) {
    Task c0 = graph.getVertex("c0");
    Task c1 = graph.getVertex("c1");
    Task t0 = graph.getVertex("t0");
    Task t1 = graph.getVertex("t1");

    Dependency d0 = graph.findEdge(c0, t0);
    Dependency d1 = graph.findEdge(c0, t1);
    Dependency d2 = graph.findEdge(c1, t0);
    Dependency d3 = graph.findEdge(c1, t1);

    assertEquals(contentC0, PropertyServiceData.isDataAvailable(c0));
    assertEquals(contentC1, PropertyServiceData.isDataAvailable(c1));

    assertEquals(transD0, PropertyServiceDependency.isTransmissionDone(d0));
    assertEquals(transD1, PropertyServiceDependency.isTransmissionDone(d1));
    assertEquals(transD2, PropertyServiceDependency.isTransmissionDone(d2));
    assertEquals(transD3, PropertyServiceDependency.isTransmissionDone(d3));

    assertEquals(consumedD0, PropertyServiceDependency.isDataConsumed(d0));
    assertEquals(consumedD1, PropertyServiceDependency.isDataConsumed(d1));
    assertEquals(consumedD2, PropertyServiceDependency.isDataConsumed(d2));
    assertEquals(consumedD3, PropertyServiceDependency.isDataConsumed(d3));
  }

  @Test
  public void testActualCall() {
    EnactmentQueues stateMock = mock(EnactmentQueues.class);
    Communication dataNode = new Communication("data");
    JsonElement content = new JsonPrimitive(42);
    PropertyServiceData.setContent(dataNode, content);
    Dependency edge = new Dependency("dep");
    Task function = new Task("function");
    GraphAccess gMock = mock(GraphAccess.class);
    Set<AgentTaskListener> listeners = new HashSet<>();
    String key = "key";
    PropertyServiceDependency.setJsonKey(edge, key);
    Enactable enactableMock = mock(Enactable.class);
    PropertyServiceFunction.setEnactable(function, enactableMock);
    SchedulabilityCheck checkMock = mock(SchedulabilityCheck.class);
    AgentTransmission tested =
        new AgentTransmission(stateMock, dataNode, edge, function, gMock, listeners, checkMock);
    String expected = ConstantsAgents.ExcMessageTransmissionPrefix + dataNode.getId()
        + ConstantsAgents.ExcMessageTransmissionSuffix + function.getId();
    assertEquals(expected, tested.formulateExceptionMessage());
    try {
      tested.actualCall();
      verify(enactableMock).setInputValue(key, content);
    } catch (Exception e) {
      fail();
    }
  }

  @Test
  public void testTransmitData() {
    EnactmentQueues stateMock = mock(EnactmentQueues.class);
    Communication dataNode = new Communication("data");
    JsonElement content = new JsonPrimitive(42);
    PropertyServiceData.setContent(dataNode, content);
    Dependency edge = new Dependency("dep");
    Task function = new Task("function");
    GraphAccess gMock = mock(GraphAccess.class);
    Set<AgentTaskListener> listeners = new HashSet<>();
    String key = "key";
    PropertyServiceDependency.setJsonKey(edge, key);
    Enactable enactableMock = mock(Enactable.class);
    PropertyServiceFunction.setEnactable(function, enactableMock);
    SchedulabilityCheck checkMock = mock(SchedulabilityCheck.class);
    AgentTransmission tested =
        new AgentTransmission(stateMock, dataNode, edge, function, gMock, listeners, checkMock);
    assertFalse(PropertyServiceDependency.isTransmissionDone(edge));

    Dependency otherEdge1 = new Dependency("e1");
    Dependency otherEdge2 = new Dependency("e2");
    Set<Dependency> inEdges = new HashSet<>();
    inEdges.add(otherEdge2);
    inEdges.add(otherEdge1);
    inEdges.add(edge);
    Communication comm1 = new Communication("comm1");
    Communication comm2 = new Communication("comm2");
    Communication comm3 = new Communication("comm3");
    EnactmentGraph graph = new EnactmentGraph();
    graph.addEdge(edge, comm1, function, EdgeType.DIRECTED);
    graph.addEdge(otherEdge1, comm2, function, EdgeType.DIRECTED);
    graph.addEdge(otherEdge2, comm3, function, EdgeType.DIRECTED);

    PropertyServiceDependency.annotateFinishedTransmission(otherEdge1);
    when(checkMock.isTargetSchedulable(function, graph)).thenReturn(false);

    tested.annotateTransmission(graph, function);
    assertTrue(PropertyServiceDependency.isTransmissionDone(edge));
    verify(stateMock, times(0)).putSchedulableTask(function);
    when(checkMock.isTargetSchedulable(function, graph)).thenReturn(true);
    PropertyServiceDependency.annotateFinishedTransmission(otherEdge2);
    tested.annotateTransmission(graph, function);
    assertFalse(PropertyServiceDependency.isTransmissionDone(edge));
    verify(stateMock, times(1)).putSchedulableTask(function);
  }
}
