package at.uibk.dps.ee.control.agents;

import static org.junit.Assert.*;
import org.junit.Test;
import at.uibk.dps.ee.control.graph.GraphAccess;
import at.uibk.dps.ee.control.graph.GraphAccess.EdgeTupleAppl;
import at.uibk.dps.ee.control.management.EnactmentQueues;
import net.sf.opendse.model.Dependency;
import net.sf.opendse.model.Task;
import static org.mockito.Mockito.mock;
import java.util.HashSet;
import java.util.Set;

public class AgentFactoryExtractionTest {

  @Test
  public void test() {
    EnactmentQueues stateMock = mock(EnactmentQueues.class);
    Task src = new Task("src");
    Task dst = new Task("dst");
    Dependency edge = new Dependency("dep");
    EdgeTupleAppl tuple = new EdgeTupleAppl(src, dst, edge);
    Set<AgentTaskListener> listeners = new HashSet<>();
    GraphAccess mockAccess = mock(GraphAccess.class);

    AgentFactoryExtraction tested = new AgentFactoryExtraction(stateMock, mockAccess);
    AgentExtraction result = tested.createExtractionAgent(tuple, listeners);
    assertEquals(src, result.finishedFunction);
    assertEquals(dst, result.dataNode);
    assertEquals(edge, result.edge);
    assertEquals(stateMock, result.enactmentState);
    assertEquals(mockAccess, result.graphAccess);
  }
}
