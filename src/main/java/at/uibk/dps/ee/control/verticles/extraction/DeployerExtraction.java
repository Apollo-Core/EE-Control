package at.uibk.dps.ee.control.verticles.extraction;

import at.uibk.dps.ee.control.graph.GraphAccess;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import net.sf.opendse.model.Task;

/**
 * The {@link DeployerExtraction} listens to the event bus and reacts to
 * finished tasks by creating extraction agents to retrieve the data.
 * 
 * @author Fedor Smirnov
 */
public class DeployerExtraction extends AbstractVerticle{

  protected final EventBus eBus;
  protected final FactoryExtraction factory;
  protected final GraphAccess graphAccess;
  
  /**
   * Standard constructor.
   * 
   * @param eBus the vertX eventBus
   * @param factory the verticle factory
   */
  public DeployerExtraction(EventBus eBus, FactoryExtraction factory, GraphAccess graphAccess) {
    this.eBus = eBus;
    this.factory = factory;
    this.graphAccess = graphAccess;
  }
  
  @Override
  public void start() throws Exception {
    super.start();
    
  }
  
  /**
   * Extracts a data from the given finished task by deploying the corresponding verticles.
   * 
   * @param finishedTask
   */
  protected void extractData(Task finishedTask) {
    
    // iterate over the out edges
    
    // this is what we do for each of them
    
    AgentExtraction verticle = factory.getAgent(finishedTask, outEdge, dataNode);
    this.vertx.deployVerticle(verticle, res -> {
      
    });
    
  }
  
}
