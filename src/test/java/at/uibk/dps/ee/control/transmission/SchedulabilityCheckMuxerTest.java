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

public class SchedulabilityCheckMuxerTest {

  @Test
  public void test() {
    Task src1 = new Communication("comm1");
    Task src2 = new Communication("comm2");
    Task dest = new Task("task");
    EnactmentGraph graph = new EnactmentGraph();
    PropertyServiceDependencyControlIf.addIfDependency(src1, dest, "key1", false, graph);
    PropertyServiceDependencyControlIf.addIfDependency(src2, dest, "key2", true, graph);
    Dependency falseDep = graph.getOutEdges(src1).iterator().next();
    Dependency trueDep = graph.getOutEdges(src2).iterator().next();
    Task decSrc = new Communication("decSrc");
    PropertyServiceData.setContent(decSrc, new JsonPrimitive(true));
    PropertyServiceDependency.addDataDependency(decSrc, dest, "key", graph);
    Dependency ifDep = graph.getOutEdges(decSrc).iterator().next();
    SchedulabilityCheckMuxer tested = new SchedulabilityCheckMuxer();
    // none are active
    assertFalse(tested.isTargetSchedulable(dest, graph));
    // if is active
    PropertyServiceDependency.annotateFinishedTransmission(ifDep);
    assertFalse(tested.isTargetSchedulable(dest, graph));
    // if and the right edge are active
    PropertyServiceDependency.annotateFinishedTransmission(trueDep);
    assertTrue(tested.isTargetSchedulable(dest, graph));
    // now the other one
    PropertyServiceData.setContent(decSrc, new JsonPrimitive(false));
    assertFalse(tested.isTargetSchedulable(dest, graph));
    PropertyServiceDependency.annotateFinishedTransmission(falseDep);
    assertTrue(tested.isTargetSchedulable(dest, graph));
  }

  @Test
  public void testExcNotTwo() {
    assertThrows(IllegalArgumentException.class, () -> {
      Task src1 = new Communication("comm1");
      Task src2 = new Communication("comm2");
      Task dest = new Task("task");
      EnactmentGraph graph = new EnactmentGraph();
      Task decSrc = new Communication("decSrc");
      PropertyServiceData.setContent(decSrc, new JsonPrimitive(true));
      PropertyServiceDependency.addDataDependency(decSrc, dest, "key", graph);
      PropertyServiceDependency.addDataDependency(src2, dest, "key", graph);
      PropertyServiceDependencyControlIf.addIfDependency(src1, dest, "key1", false, graph);
      SchedulabilityCheckMuxer tested = new SchedulabilityCheckMuxer();
      tested.isTargetSchedulable(dest, graph);
    });
  }

  @Test
  public void testExcBothTrue() {
    assertThrows(IllegalArgumentException.class, () -> {
      Task src1 = new Communication("comm1");
      Task src2 = new Communication("comm2");
      Task dest = new Task("task");
      EnactmentGraph graph = new EnactmentGraph();
      Task decSrc = new Communication("decSrc");
      PropertyServiceData.setContent(decSrc, new JsonPrimitive(true));
      PropertyServiceDependency.addDataDependency(decSrc, dest, "key", graph);
      PropertyServiceDependencyControlIf.addIfDependency(src1, dest, "key1", true, graph);
      PropertyServiceDependencyControlIf.addIfDependency(src2, dest, "key2", true, graph);
      SchedulabilityCheckMuxer tested = new SchedulabilityCheckMuxer();
      tested.isTargetSchedulable(dest, graph);
    });
  }

  @Test
  public void testExcEdgeNum() {
    assertThrows(IllegalArgumentException.class, () -> {
      Task src1 = new Communication("comm1");
      Task src2 = new Communication("comm2");
      Task dest = new Task("task");
      EnactmentGraph graph = new EnactmentGraph();
      PropertyServiceDependencyControlIf.addIfDependency(src1, dest, "key1", false, graph);
      PropertyServiceDependencyControlIf.addIfDependency(src2, dest, "key2", true, graph);
      SchedulabilityCheckMuxer tested = new SchedulabilityCheckMuxer();
      tested.isTargetSchedulable(dest, graph);
    });
  }
}
