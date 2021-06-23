package at.uibk.dps.ee.control.verticles.extraction;

import at.uibk.dps.ee.control.graph.GraphAccess;
import io.vertx.core.eventbus.EventBus;
import net.sf.opendse.model.Dependency;
import net.sf.opendse.model.Task;

/**
 * The {@link FactoryExtraction} is used to create the verticles doing the
 * actual extraction.
 * 
 * @author Fedor Smirnov
 */
public class FactoryExtraction {

  protected final GraphAccess graphAccess;
  protected final EventBus eventBus;

  /**
   * Standard constructor.
   * 
   * @param graphAccess the graph access
   * @param eventBus the vertX event bus
   */
  public FactoryExtraction(GraphAccess graphAccess, EventBus eventBus) {
    super();
    this.graphAccess = graphAccess;
    this.eventBus = eventBus;
  }

  /**
   * Returns the extraction agent to extract the data from the finished task.
   * 
   * @param finishedTask the task with the processing results
   * @param outEdge the outgoing edge
   * @param dataNode the data node where the result shall be stored
   * @return the extraction agent to extract the data from the finished task
   */
  public AgentExtraction getAgent(Task finishedTask, Dependency outEdge, Task dataNode) {
    return new AgentExtractionLocal(finishedTask, outEdge, dataNode, graphAccess, eventBus);
  }
}
