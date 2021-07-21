package at.uibk.dps.ee.control.agents;

import static org.junit.Assert.*;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;
import at.uibk.dps.ee.control.graph.GraphAccess;
import at.uibk.dps.ee.control.management.EnactmentQueues;
import at.uibk.dps.ee.control.verticles.transformation.GraphTransform;
import at.uibk.dps.ee.core.ModelModificationListener;
import net.sf.opendse.model.Task;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AgentTransformTest {

  @Test
  public void test() {
    Task taskNode = new Task("task");
    GraphTransform modification = mock(GraphTransform.class);
    String modName = "modification";
    when(modification.getTransformName()).thenReturn(modName);
    GraphAccess gAccess = mock(GraphAccess.class);
    EnactmentQueues eState = mock(EnactmentQueues.class);
    ModelModificationListener modificationListener = mock(ModelModificationListener.class);
    Set<ModelModificationListener> modificationListeners = new HashSet<>();
    modificationListeners.add(modificationListener);
    AgentTransform tested =
        new AgentTransform(new HashSet<>(), gAccess, modification, taskNode, eState, modificationListeners);

    String expectedMessage = ConstantsAgents.ExcMessageTransformPrefix + modName
        + ConstantsAgents.ExcMessageTransformSuffix + taskNode.getId();
    assertEquals(expectedMessage, tested.formulateExceptionMessage());

    try {
      assertTrue(tested.actualCall());
      verify(modification).modifyEnactmentGraph(gAccess, taskNode);
      verify(eState).putFinishedTask(taskNode);
      verify(modificationListener).reactToModelModification();
    } catch (Exception e) {
      fail();
    }
  }
}
