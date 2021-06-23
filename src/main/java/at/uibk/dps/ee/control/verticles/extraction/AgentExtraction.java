package at.uibk.dps.ee.control.verticles.extraction;

import at.uibk.dps.ee.control.graph.GraphAccess;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import net.sf.opendse.model.Dependency;
import net.sf.opendse.model.Task;

/**
 * Parent class for the agents extracting data from task nodes.
 * 
 * @author Fedor Smirnov
 *
 */
public abstract class AgentExtraction extends AbstractVerticle{

  protected final Task finishedTask;
  protected final Dependency outEdge;
  protected final Task dataNode;
  protected final GraphAccess graphAccess;
  protected final EventBus eventBus;

  /**
   * Standard constructor
   * 
   * @param finishedFunction the finished function
   * @param outEdge the out edge
   * @param dataNode the data node to transmit to
   * @param graphAccess the access to the enactment graph
   * @param eBus the vertX event bus
   */
  public AgentExtraction(Task finishedTask, Dependency outEdge, Task dataNode,
      GraphAccess graphAccess, EventBus eventBus) {
    this.finishedTask = finishedTask;
    this.outEdge = outEdge;
    this.dataNode = dataNode;
    this.graphAccess = graphAccess;
    this.eventBus = eventBus;
  }
  
  @Override
  public void start() throws Exception {
    super.start();
    eventBus.consumer(address)
  }
  
  /**
   * Performs the extraction and undeploys itself.
   */
  public void extractAndUndeploy() {
    extract();
    this.vertx.undeploy(this.deploymentID());
  }

  /**
   * Extracts the data produced by the finished functions and transmits it to the
   * data node on the end of the out dependency.
   */
  protected abstract void extract();
}
