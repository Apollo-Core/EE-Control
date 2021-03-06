package at.uibk.dps.ee.control.agents;

import static org.junit.Assert.*;
import org.junit.Test;
import at.uibk.dps.ee.control.graph.GraphAccess;
import at.uibk.dps.ee.control.management.EnactmentAgent;
import at.uibk.dps.ee.control.management.EnactmentQueues;
import at.uibk.dps.ee.control.management.ExecutorProvider;
import static org.mockito.Mockito.mock;

public class AgentFactoryActivationTest {

  @Test
  public void test() {
    EnactmentQueues state = mock(EnactmentQueues.class);
    ExecutorProvider execProvider = mock(ExecutorProvider.class);
    AgentFactoryScheduling schedMock = mock(AgentFactoryScheduling.class);
    AgentFactoryEnactment enactMock = mock(AgentFactoryEnactment.class);
    AgentFactoryTransmission transMock = mock(AgentFactoryTransmission.class);
    AgentFactoryExtraction extractMock = mock(AgentFactoryExtraction.class);
    AgentFactoryTransform transformMock = mock(AgentFactoryTransform.class);
    GraphAccess graphMock = mock(GraphAccess.class);
    AgentFactoryActivation tested = new AgentFactoryActivation(state, execProvider, schedMock,
        transMock, enactMock, extractMock, transformMock, graphMock);

    EnactmentAgent agentMock = mock(EnactmentAgent.class);
    assertTrue(tested.createSchedulingActivationAgent() instanceof AgentActivationScheduling);
    assertTrue(tested.createExtractionActivationAgent() instanceof AgentActivationExtraction);
    assertTrue(tested.createEnactmentActivationAgent() instanceof AgentActivationEnactment);
    assertTrue(
        tested.createTransmissionActivationAgent(agentMock) instanceof AgentActivationTransmission);
    assertTrue(tested.createTransformActicationAgent() instanceof AgentActivationTransform);
  }
}
