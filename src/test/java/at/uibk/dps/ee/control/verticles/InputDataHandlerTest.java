package at.uibk.dps.ee.control.verticles;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import at.uibk.dps.ee.guice.starter.VertxProvider;
import at.uibk.dps.ee.model.graph.EnactmentGraph;
import at.uibk.dps.ee.model.graph.EnactmentGraphProvider;
import at.uibk.dps.ee.model.properties.PropertyServiceData;
import at.uibk.dps.ee.model.properties.PropertyServiceData.DataType;
import io.vertx.core.eventbus.EventBus;
import at.uibk.dps.ee.model.properties.PropertyServiceDependency;
import net.sf.opendse.model.Communication;
import net.sf.opendse.model.Task;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

class InputDataHandlerTest {

  EnactmentGraph eGraph;

  Task input1;
  Task input2;
  Task constantInput;

  JsonObject inputObj;

  InputDataHandler tested;
  EventBus eBus;

  /**
   * Test that an event is set for each root node and constant node in the graph.
   */
  @Test
  void testProcessInput() {
    tested.processInput(inputObj);
    verify(eBus).send(ConstantsVertX.addressDataAvailable, input1.getId());
    verify(eBus).send(ConstantsVertX.addressDataAvailable, input2.getId());
    verify(eBus).send(ConstantsVertX.addressDataAvailable, constantInput.getId());
  }

  /**
   * Check the missing entry exception.
   */
  @Test
  void testProcessRootNodeMissingEntry() {
    assertThrows(IllegalArgumentException.class, () -> {
      PropertyServiceData.setJsonKey(input1, "missing");
      tested.processRootNode(input1, inputObj);
    });
  }

  /**
   * Test that we set the content of the root node.
   */
  @Test
  void testProcessRootNode() {
    tested.processRootNode(input1, inputObj);
    assertEquals(1, PropertyServiceData.getContent(input1).getAsInt());
    verify(eBus).send(ConstantsVertX.addressDataAvailable, input1.getId());
  }

  @BeforeEach
  void setup() {
    inputObj = new JsonObject();
    inputObj.add("in1", new JsonPrimitive(1));
    inputObj.add("in2", new JsonPrimitive("two"));

    // create the graph
    eGraph = new EnactmentGraph();

    input1 = new Communication("d1");
    PropertyServiceData.makeRoot(input1);
    PropertyServiceData.setJsonKey(input1, "in1");

    input2 = new Communication("d2");
    PropertyServiceData.makeRoot(input2);
    PropertyServiceData.setJsonKey(input2, "in2");

    constantInput =
        PropertyServiceData.createConstantNode("d3", DataType.Number, new JsonPrimitive(42));

    Task t = new Task("t");
    Communication output = new Communication("out");

    PropertyServiceDependency.addDataDependency(input1, t, "in1", eGraph);
    PropertyServiceDependency.addDataDependency(input2, t, "in2", eGraph);
    PropertyServiceDependency.addDataDependency(constantInput, t, "in3", eGraph);
    PropertyServiceDependency.addDataDependency(t, output, "out", eGraph);

    // set up the tested
    EnactmentGraphProvider eGraphProvider = mock(EnactmentGraphProvider.class);
    when(eGraphProvider.getEnactmentGraph()).thenReturn(eGraph);
    VertxProvider vProv = mock(VertxProvider.class);

    eBus = mock(EventBus.class);
    when(vProv.geteBus()).thenReturn(eBus);

    tested = new InputDataHandler(eGraphProvider, vProv);

  }
}
