package at.uibk.dps.ee.control.verticles.transformation;

import java.util.Set;
import com.google.inject.Inject;
import at.uibk.dps.ee.control.graph.GraphTransform;
import at.uibk.dps.ee.control.graph.GraphTransformer;
import at.uibk.dps.ee.control.verticles.ConstantsVertX;
import at.uibk.dps.ee.control.verticles.VerticleApollo;
import at.uibk.dps.ee.control.verticles.WorkerException;
import at.uibk.dps.ee.core.ModelModificationListener;
import at.uibk.dps.ee.model.graph.EnactmentGraphProvider;
import net.sf.opendse.model.Task;

public class WorkerTransformation extends VerticleApollo {

  protected final GraphTransformer transformer;
  protected final Set<ModelModificationListener> listeners;


  @Inject
  public WorkerTransformation(EnactmentGraphProvider eGraphProv, GraphTransformer transformer,
      Set<ModelModificationListener> listeners) {
    super(ConstantsVertX.addressRequiredTransformation,
        ConstantsVertX.addressEnactmentFinished, ConstantsVertX.addressFailureAbort,
        eGraphProv);
    this.transformer = transformer;
    this.listeners = listeners;
  }

  @Override
  protected void work(Task transformNode) throws WorkerException {
    GraphTransform transformOperation = transformer.getTransformOperation(transformNode);
    transformOperation.modifyEnactmentGraph(eGraph, transformNode);
    System.out.println("Transform complete");
    listeners.forEach(listener -> listener.reactToModelModification());
    this.vertx.eventBus().send(successAddress, transformNode.getId());
  }
}
