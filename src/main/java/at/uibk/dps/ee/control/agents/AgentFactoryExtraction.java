package at.uibk.dps.ee.control.agents;

import java.util.Set;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import at.uibk.dps.ee.control.graph.GraphAccess;
import at.uibk.dps.ee.control.graph.GraphAccess.EdgeTupleAppl;
import at.uibk.dps.ee.control.management.EnactmentQueues;

/**
 * The default factory for the creation of {@link AgentExtraction}s.
 * 
 * @author Fedor Smirnov
 */
@Singleton
public class AgentFactoryExtraction {

  protected final EnactmentQueues enactmentState;
  protected final GraphAccess graphAccess;

  /**
   * The injection constructor.
   * 
   * @param enactmentState the state of the enactment (for the access to the
   *        queues)
   */
  @Inject
  public AgentFactoryExtraction(final EnactmentQueues enactmentState,
      final GraphAccess graphAccess) {
    this.enactmentState = enactmentState;
    this.graphAccess = graphAccess;
  }

  /**
   * Creates an {@link AgentExtraction} to extract the data from the finished task
   * (edge src) and write it into the data node (edge destination).
   * 
   * @param edgeTuple the edge tuple with src, dst, and the edge
   * @param listeners the {@link AgentTaskListener}s
   * @return an {@link AgentExtraction} to extract the data from the finished task
   *         (edge src) and write it into the data node (edge destination)
   */
  public AgentExtraction createExtractionAgent(final EdgeTupleAppl edgeTuple,
      final Set<AgentTaskListener> listeners) {
    return new AgentExtraction(edgeTuple.getSrc(), edgeTuple.getEdge(), edgeTuple.getDst(),
        enactmentState, listeners, graphAccess);
  }
}
