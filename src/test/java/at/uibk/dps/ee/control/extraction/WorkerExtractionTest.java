package at.uibk.dps.ee.control.extraction;

import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.Test;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import at.uibk.dps.ee.control.verticles.ConstantsVertX;
import at.uibk.dps.ee.control.verticles.WorkerException;
import at.uibk.dps.ee.model.constants.ConstantsEEModel;
import at.uibk.dps.ee.model.graph.EnactmentGraph;
import at.uibk.dps.ee.model.graph.EnactmentGraphProvider;
import at.uibk.dps.ee.model.properties.PropertyServiceData;
import at.uibk.dps.ee.model.properties.PropertyServiceDependency;
import at.uibk.dps.ee.model.properties.PropertyServiceFunction;
import edu.uci.ics.jung.graph.util.EdgeType;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import net.sf.opendse.model.Communication;
import net.sf.opendse.model.Dependency;
import net.sf.opendse.model.Task;

public class WorkerExtractionTest {

  protected class ExtractionMock extends WorkerExtraction {
    public ExtractionMock(EnactmentGraphProvider eGraphProvider) {
      super(eGraphProvider);
    }
    public void setVertX(Vertx vertx) {
      this.vertx = vertx;
    }
  }
  
  @Test
  public void testNoKey() {
    Task finished = new Task("finished");
    Dependency dep = new Dependency("dep");
    Communication dataNode = new Communication("data");
    PropertyServiceDependency.setJsonKey(dep, "key");
    EnactmentGraph eGraph = new EnactmentGraph();
    eGraph.addEdge(dep, finished, dataNode, EdgeType.DIRECTED);
    EnactmentGraphProvider eProv = mock(EnactmentGraphProvider.class);
    when(eProv.getEnactmentGraph()).thenReturn(eGraph);
    WorkerExtraction tested = new WorkerExtraction(eProv);
    PropertyServiceFunction.setOutput(finished, new JsonObject());
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
    PropertyServiceDependency.setJsonKey(dep, ConstantsEEModel.JsonKeySequentiality);
    EnactmentGraph eGraph = new EnactmentGraph();
    eGraph.addEdge(dep, finished, dataNode, EdgeType.DIRECTED);
    EnactmentGraphProvider eProv = mock(EnactmentGraphProvider.class);
    when(eProv.getEnactmentGraph()).thenReturn(eGraph);
    ExtractionMock tested = new ExtractionMock(eProv);

    EventBus busMock = mock(EventBus.class);
    Vertx mockV = mock(Vertx.class);
    when(mockV.eventBus()).thenReturn(busMock);
    tested.setVertX(mockV);
    
    JsonObject result = new JsonObject();
    result.add(ConstantsEEModel.JsonKeySequentiality, new JsonPrimitive(true));
    PropertyServiceFunction.setOutput(finished, result);
    
    try {
      tested.work(finished);
    } catch (WorkerException e) {
      fail();
    }
    assertEquals(true, PropertyServiceData.getContent(dataNode).getAsBoolean());
    verify(busMock).send(ConstantsVertX.addressDataAvailable, dataNode.getId());
  }

  @Test
  void testWorkLeafNode() {
    Task finished = new Task("finished");
    Dependency dep = new Dependency("dep");
    Communication dataNode = new Communication("data");
    
    PropertyServiceData.makeLeaf(dataNode);
    PropertyServiceData.setJsonKey(dataNode, "key");
    JsonObject result = new JsonObject();
    JsonElement content = new JsonPrimitive(42);
    result.add("key", content);
    PropertyServiceDependency.setJsonKey(dep, "key");
    EnactmentGraph eGraph = new EnactmentGraph();
    eGraph.addEdge(dep, finished, dataNode, EdgeType.DIRECTED);
    PropertyServiceFunction.setOutput(finished, result);
    EnactmentGraphProvider eProv = mock(EnactmentGraphProvider.class);
    when(eProv.getEnactmentGraph()).thenReturn(eGraph);
    ExtractionMock tested = new ExtractionMock(eProv);

    EventBus busMock = mock(EventBus.class);
    Vertx mockV = mock(Vertx.class);
    when(mockV.eventBus()).thenReturn(busMock);
    tested.setVertX(mockV);

    try {
      tested.work(finished);
    } catch (WorkerException e) {
      fail();
    }
    assertEquals(content, PropertyServiceData.getContent(dataNode));
    verify(busMock).send(ConstantsVertX.addressDataAvailable, dataNode.getId());
    verify(busMock).publish(ConstantsVertX.addressWorkflowResultAvailable, result.toString());
  }
  
  @Test
  public void testWork() {
    Task finished = new Task("finished");
    Dependency dep = new Dependency("dep");
    Communication dataNode = new Communication("data");
    JsonObject result = new JsonObject();
    JsonElement content = new JsonPrimitive(42);
    result.add("key", content);
    PropertyServiceDependency.setJsonKey(dep, "key");
    EnactmentGraph eGraph = new EnactmentGraph();
    eGraph.addEdge(dep, finished, dataNode, EdgeType.DIRECTED);
    PropertyServiceFunction.setOutput(finished, result);
    EnactmentGraphProvider eProv = mock(EnactmentGraphProvider.class);
    when(eProv.getEnactmentGraph()).thenReturn(eGraph);
    ExtractionMock tested = new ExtractionMock(eProv);

    EventBus busMock = mock(EventBus.class);
    Vertx mockV = mock(Vertx.class);
    when(mockV.eventBus()).thenReturn(busMock);
    tested.setVertX(mockV);

    try {
      tested.work(finished);
    } catch (WorkerException e) {
      fail();
    }
    assertEquals(content, PropertyServiceData.getContent(dataNode));
    verify(busMock).send(ConstantsVertX.addressDataAvailable, dataNode.getId());
  }
}
