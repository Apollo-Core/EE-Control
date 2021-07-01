package at.uibk.dps.ee.control.verticles.transformation;

import at.uibk.dps.ee.control.graph.GraphTransform;
import at.uibk.dps.ee.control.graph.GraphTransformer;
import at.uibk.dps.ee.control.verticles.ConstantsEventBus;
import at.uibk.dps.ee.control.verticles.HandlerApollo;
import at.uibk.dps.ee.control.verticles.WorkerException;
import at.uibk.dps.ee.model.graph.EnactmentGraph;
import io.vertx.core.eventbus.EventBus;
import net.sf.opendse.model.Task;

public class WorkerTransformation extends HandlerApollo<Task> {

  protected final GraphTransformer transformer;
  
  public WorkerTransformation(EventBus eBus, EnactmentGraph eGraph, GraphTransformer transformer) {
    super(ConstantsEventBus.addressRequiredTransformation,
        ConstantsEventBus.addressEnactmentFinished, ConstantsEventBus.addressFailureAbort, eBus,
        eGraph);
    this.transformer = transformer;
  }

  @Override
  protected Task readMessage(String message) {
    return eGraph.getVertex(message);
  }

  @Override
  protected void work(Task transformNode) throws WorkerException {
    GraphTransform transformOperation = transformer.getTransformOperation(transformNode);
    transformOperation.modifyEnactmentGraph(eGraph, transformNode);
    eBus.publish(successAddress, transformNode.getId());
  }
}
