package at.uibk.dps.ee.control.verticles.transmission;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import at.uibk.dps.ee.model.graph.EnactmentGraph;
import at.uibk.dps.ee.model.properties.PropertyServiceFunction;
import at.uibk.dps.ee.model.properties.PropertyServiceFunctionDataFlow;
import at.uibk.dps.ee.model.properties.PropertyServiceFunction.UsageType;
import at.uibk.dps.ee.model.properties.PropertyServiceFunctionDataFlow.DataFlowType;
import net.sf.opendse.model.Task;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.doReturn;

class SchedulabilityCheckMultiTest {

  SchedulabilityCheckDefault defaultCheck;
  SchedulabilityCheckMuxer muxCheck;
  
  Task muxTask;
  Task nonMuxTask;
  
  EnactmentGraph graph;
  SchedulabilityCheckMulti tested;
  
  /**
   * MuxTask
   */
  @Test
  void testMux() {
    assertTrue(tested.isTargetSchedulable(muxTask, graph));
    verify(defaultCheck, never()).isTargetSchedulable(muxTask, graph);
    verify(muxCheck).isTargetSchedulable(muxTask, graph);
  }
  
  /**
   * Normal task
   */
  @Test
  void testNonMux() {
    assertTrue(tested.isTargetSchedulable(nonMuxTask, graph));
    verify(defaultCheck).isTargetSchedulable(nonMuxTask, graph);
    verify(muxCheck, never()).isTargetSchedulable(nonMuxTask, graph);
  }
  
  @BeforeEach
  void setUp() {
    tested = new SchedulabilityCheckMulti();
    
    defaultCheck = spy(tested.checkDefault);
    muxCheck = spy(tested.checkMuxer);
    
    tested.checkDefault = defaultCheck;
    tested.checkMuxer = muxCheck;
    
    muxTask = PropertyServiceFunctionDataFlow.createDataFlowFunction("mux", DataFlowType.Multiplexer);
    nonMuxTask = new Task("nonMux");
    PropertyServiceFunction.setUsageType(UsageType.User, nonMuxTask);
    graph = new EnactmentGraph();

    doReturn(true).when(defaultCheck).isTargetSchedulable(nonMuxTask, graph);
    doReturn(true).when(muxCheck).isTargetSchedulable(muxTask, graph);
  }
}
