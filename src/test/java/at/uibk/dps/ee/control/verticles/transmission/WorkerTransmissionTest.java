package at.uibk.dps.ee.control.verticles.transmission;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import at.uibk.dps.ee.control.verticles.ConstantsVertX;
import at.uibk.dps.ee.model.graph.EnactmentGraph;
import at.uibk.dps.ee.model.graph.EnactmentGraphProvider;
import at.uibk.dps.ee.model.properties.PropertyServiceData;
import at.uibk.dps.ee.model.properties.PropertyServiceDependency;
import at.uibk.dps.ee.model.properties.PropertyServiceDependency.TypeDependency;
import at.uibk.dps.ee.model.properties.PropertyServiceFunction;
import edu.uci.ics.jung.graph.util.EdgeType;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import net.sf.opendse.model.Communication;
import net.sf.opendse.model.Dependency;
import net.sf.opendse.model.Task;

class WorkerTransmissionTest {

  protected class MockWorker extends WorkerTransmission {

    public MockWorker(EnactmentGraphProvider eGraphProvider,
        SchedulabilityCheck schedulabilityCheck) {
      super(eGraphProvider, schedulabilityCheck);
    }

    public void setVertx(Vertx vertx) {
      this.vertx = vertx;
    }
  }

  EventBus eBus;
  EnactmentGraph graph;
  EnactmentGraphProvider eProv;
  SchedulabilityCheck schedCheck;
  MockWorker tested;

  /**
   * Case where the other one has already transmitted.
   */
  @Test
  public void testTransmitMultiOtherDone() {
    Communication dataNode = new Communication("data");
    JsonElement content = new JsonPrimitive(42);
    PropertyServiceData.setContent(dataNode, content);
    Dependency edge = new Dependency("dep");
    PropertyServiceDependency.setType(edge, TypeDependency.Data);
    Task function = new Task("function");
    String key = "key";
    PropertyServiceDependency.setJsonKey(edge, key);
    graph.addEdge(edge, dataNode, function, EdgeType.DIRECTED);

    Communication secondData = new Communication("secondData");
    JsonElement content2 = new JsonPrimitive("we");
    PropertyServiceData.setContent(secondData, content2);
    Dependency otherEdge = new Dependency("dep2");
    String key2 = "key2";
    PropertyServiceDependency.setJsonKey(otherEdge, key2);
    PropertyServiceDependency.setType(otherEdge, TypeDependency.Data);
    graph.addEdge(otherEdge, secondData, function, EdgeType.DIRECTED);
    PropertyServiceDependency.annotateFinishedTransmission(otherEdge);

    when(schedCheck.isTargetSchedulable(function, graph)).thenReturn(true);
    tested.annotateTransmission(edge);
    assertFalse(PropertyServiceDependency.isTransmissionDone(edge));
    verify(eBus, times(1)).send(ConstantsVertX.addressTaskSchedulable, function.getId());
    assertTrue(PropertyServiceFunction.isInputSet(function));
    assertEquals(content, PropertyServiceFunction.getInput(function).get(key));
    assertEquals(content2, PropertyServiceFunction.getInput(function).get(key2));
  }

  /**
   * Case where is an other edge which has not yet transmitted.
   */
  @Test
  public void testTransmitDataMultipleEdgesOtherNotReady() {
    Communication dataNode = new Communication("data");
    JsonElement content = new JsonPrimitive(42);
    PropertyServiceData.setContent(dataNode, content);
    Dependency edge = new Dependency("dep");
    PropertyServiceDependency.setType(edge, TypeDependency.Data);
    Task function = new Task("function");
    String key = "key";
    PropertyServiceDependency.setJsonKey(edge, key);
    graph.addEdge(edge, dataNode, function, EdgeType.DIRECTED);

    Communication secondData = new Communication("secondData");
    JsonElement content2 = new JsonPrimitive("we");
    PropertyServiceData.setContent(secondData, content2);
    Dependency otherEdge = new Dependency("dep2");
    PropertyServiceDependency.setType(otherEdge, TypeDependency.Data);
    graph.addEdge(otherEdge, secondData, function, EdgeType.DIRECTED);

    when(schedCheck.isTargetSchedulable(function, graph)).thenReturn(false);
    tested.annotateTransmission(edge);
    assertTrue(PropertyServiceDependency.isTransmissionDone(edge));
    verify(eBus, never()).send(ConstantsVertX.addressTaskSchedulable, function.getId());
    assertFalse(PropertyServiceFunction.isInputSet(function));
  }

  /**
   * Transmitting data from dataNode to Function
   */
  @Test
  public void testTransmitDataOneEdge() {
    Communication dataNode = new Communication("data");
    JsonElement content = new JsonPrimitive(42);
    PropertyServiceData.setContent(dataNode, content);
    Dependency edge = new Dependency("dep");
    PropertyServiceDependency.setType(edge, TypeDependency.Data);
    Task function = new Task("function");
    String key = "key";
    PropertyServiceDependency.setJsonKey(edge, key);
    graph.addEdge(edge, dataNode, function, EdgeType.DIRECTED);
    when(schedCheck.isTargetSchedulable(function, graph)).thenReturn(true);
    tested.annotateTransmission(edge);
    assertFalse(PropertyServiceDependency.isTransmissionDone(edge));
    verify(eBus, times(1)).send(ConstantsVertX.addressTaskSchedulable, function.getId());
    assertEquals(content, PropertyServiceFunction.getInput(function).get(key));
  }

  @BeforeEach
  void setUp() {
    Vertx vMock = mock(Vertx.class);
    eBus = mock(EventBus.class);
    when(vMock.eventBus()).thenReturn(eBus);
    eProv = mock(EnactmentGraphProvider.class);
    graph = new EnactmentGraph();
    when(eProv.getEnactmentGraph()).thenReturn(graph);
    schedCheck = mock(SchedulabilityCheck.class);
    tested = new MockWorker(eProv, schedCheck);
    tested.setVertx(vMock);
  }

}
