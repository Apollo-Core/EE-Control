package at.uibk.dps.ee.control.transmission;

import static org.junit.Assert.*;
import org.junit.jupiter.api.Test;
import com.google.gson.JsonPrimitive;
import at.uibk.dps.ee.model.graph.EnactmentGraph;
import at.uibk.dps.ee.model.properties.PropertyServiceData;
import at.uibk.dps.ee.model.properties.PropertyServiceDependency;
import at.uibk.dps.ee.model.properties.PropertyServiceDependencyControlIf;
import net.sf.opendse.model.Communication;
import net.sf.opendse.model.Dependency;
import net.sf.opendse.model.Task;

public class SchedulabilityCheckDefaultTest {

  @Test
  public void testIfEdgeActive() {
    Task src1 = new Communication("comm1");
    Task src2 = new Communication("comm2");
    Task dest = new Task("task");
    EnactmentGraph graph = new EnactmentGraph();
    PropertyServiceDependencyControlIf.addIfDependency(src1, dest, "string", false, graph);
    PropertyServiceDependencyControlIf.addIfDependency(src2, dest, "string", true, graph);
    Dependency trueDep = graph.getOutEdges(src2).iterator().next();
    Dependency falseDep = graph.getOutEdges(src1).iterator().next();
    SchedulabilityCheckDefault tested = new SchedulabilityCheckDefault();
    
    PropertyServiceData.setContent(src1, new JsonPrimitive(true));
    PropertyServiceData.setContent(src2, new JsonPrimitive(true));
    assertTrue(tested.isIfEdgeActive(graph, trueDep));
    assertFalse(tested.isIfEdgeActive(graph, falseDep));
    
    PropertyServiceData.setContent(src1, new JsonPrimitive(false));
    PropertyServiceData.setContent(src2, new JsonPrimitive(false));
    assertFalse(tested.isIfEdgeActive(graph, trueDep));
    assertTrue(tested.isIfEdgeActive(graph, falseDep));
  }
  
  @Test
  public void testIsTargetSchedulable() {
    // normal dependencies, not all ready
    Task src1 = new Communication("comm1");
    Task src2 = new Communication("comm2");
    Task dest = new Task("task");
    EnactmentGraph graph = new EnactmentGraph();
    PropertyServiceDependency.addDataDependency(src1, dest, "key1", graph);
    PropertyServiceDependency.addDataDependency(src2, dest, "key2", graph);
    Dependency dep1 = graph.getOutEdges(src1).iterator().next();
    Dependency dep2 = graph.getOutEdges(src2).iterator().next();
    PropertyServiceDependency.annotateFinishedTransmission(dep1);
    SchedulabilityCheckDefault tested = new SchedulabilityCheckDefault();
    assertFalse(tested.isTargetSchedulable(dest, graph));
    
    // normal dependencies, all ready
    PropertyServiceDependency.annotateFinishedTransmission(dep2);
    assertTrue(tested.isTargetSchedulable(dest, graph));
    
    // and now with an if edge
    Task decSrc = new Communication("decSrc");
    PropertyServiceData.setContent(decSrc, new JsonPrimitive(true));
    PropertyServiceDependencyControlIf.addIfDependency(decSrc, dest, "ifKey", false, graph);
    Dependency ifDep = graph.getOutEdges(decSrc).iterator().next();
    PropertyServiceDependency.annotateFinishedTransmission(ifDep);
    // variable not set to wanted bool value
    assertFalse(tested.isTargetSchedulable(dest, graph));
    PropertyServiceData.setContent(decSrc, new JsonPrimitive(false));
    assertTrue(tested.isTargetSchedulable(dest, graph));
  }
}
