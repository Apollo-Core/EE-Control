package at.uibk.dps.ee.control.agents;

import static org.junit.Assert.*;
import org.junit.Test;
import at.uibk.dps.ee.control.enactment.PostEnactment;
import at.uibk.dps.ee.core.function.Enactable;
import at.uibk.dps.ee.model.properties.PropertyServiceFunction;
import net.sf.opendse.model.Task;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import java.util.HashSet;;

public class AgentEnactmentTest {

  @Test
  public void test() {
    Enactable mockEnactable = mock(Enactable.class);
    PostEnactment postMock = mock(PostEnactment.class);
    Task task = new Task("task");
    PropertyServiceFunction.setEnactable(task, mockEnactable);
    AgentEnactment tested = new AgentEnactment(task, postMock, new HashSet<>());
    assertEquals(ConstantsAgents.ExcMessageEnactment + task.getId(),
        tested.formulateExceptionMessage());
    try {
      tested.actualCall();
      verify(mockEnactable).play();
      verify(postMock).postEnactmentTreatment(task);
    } catch (Exception e) {
      fail();
    }
  }
}
