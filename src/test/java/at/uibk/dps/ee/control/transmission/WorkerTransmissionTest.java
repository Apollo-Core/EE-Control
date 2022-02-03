package at.uibk.dps.ee.control.transmission;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import at.uibk.dps.ee.control.verticles.ConstantsVertX;
import at.uibk.dps.ee.model.graph.EnactmentGraph;
import at.uibk.dps.ee.model.graph.SpecificationProvider;
import at.uibk.dps.ee.model.properties.PropertyServiceData;
import at.uibk.dps.ee.model.properties.PropertyServiceDependency;
import at.uibk.dps.ee.model.properties.PropertyServiceFunction;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import net.sf.opendse.model.Communication;
import net.sf.opendse.model.Dependency;
import net.sf.opendse.model.Task;

class WorkerTransmissionTest {

  protected class MockWorker extends WorkerTransmission {

    public MockWorker(SpecificationProvider specProvider, SchedulabilityCheck schedulabilityCheck) {
      super(specProvider, schedulabilityCheck);
    }

    public void setVertx(Vertx vertx) {
      this.vertx = vertx;
    }
  }

  SpecificationProvider specProv;
  EventBus eBus;
  EnactmentGraph graph;
  SchedulabilityCheck schedCheck;
  MockWorker tested;
  Dependency dep1;
  Dependency dep2;
  Task function;
  JsonObject expectedResult;
  Communication comm1;
  Communication comm2;

  /**
   * Tests that the function input is annotated correctly.
   */
  @Test
  void testAnnotateFunctionInput() {
    tested.annotateFunctionInput(dep1);
    assertEquals(expectedResult, PropertyServiceFunction.getInput(function));
    verify(eBus).send(ConstantsVertX.addressTaskSchedulable, function.getId());
  }

  /**
   * Test that edges are correctly annotated and that we correctly check the data
   * availability.
   */
  @Test
  void testAnnotateAndCheck() {
    assertFalse(PropertyServiceDependency.isTransmissionDone(dep1));
    assertFalse(PropertyServiceDependency.isTransmissionDone(dep2));
    when(schedCheck.isTargetSchedulable(function, graph)).thenReturn(false);
    assertFalse(tested.annotateAndCheck(dep1));
    assertTrue(PropertyServiceDependency.isTransmissionDone(dep1));
    assertFalse(PropertyServiceDependency.isTransmissionDone(dep2));
    when(schedCheck.isTargetSchedulable(function, graph)).thenReturn(true);
    assertTrue(tested.annotateAndCheck(dep2));
    assertTrue(PropertyServiceDependency.isTransmissionDone(dep1));
    assertTrue(PropertyServiceDependency.isTransmissionDone(dep2));
  }

  @BeforeEach
  void setUp() {
    Vertx vMock = mock(Vertx.class);
    eBus = mock(EventBus.class);
    when(vMock.eventBus()).thenReturn(eBus);
    graph = new EnactmentGraph();
    specProv = mock(SpecificationProvider.class);
    when(specProv.getEnactmentGraph()).thenReturn(graph);
    schedCheck = mock(SchedulabilityCheck.class);
    tested = new MockWorker(specProv, schedCheck);
    tested.setVertx(vMock);

    comm1 = new Communication("comm1");
    comm2 = new Communication("comm2");
    function = new Task("task");
    String key1 = "key1";
    String key2 = "key2";
    JsonElement content1 = new JsonPrimitive(true);
    JsonElement content2 = new JsonPrimitive(42);
    expectedResult = new JsonObject();
    expectedResult.add(key1, content1);
    expectedResult.add(key2, content2);
    PropertyServiceData.setContent(comm1, content1);
    PropertyServiceData.setContent(comm2, content2);
    dep1 = PropertyServiceDependency.addDataDependency(comm1, function, key1, graph);
    dep2 = PropertyServiceDependency.addDataDependency(comm2, function, key2, graph);
  }

}
