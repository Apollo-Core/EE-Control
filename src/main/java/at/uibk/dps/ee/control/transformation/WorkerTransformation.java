package at.uibk.dps.ee.control.transformation;

import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.inject.Inject;
import at.uibk.dps.ee.control.verticles.ConstantsVertX;
import at.uibk.dps.ee.control.verticles.VerticleApollo;
import at.uibk.dps.ee.control.verticles.WorkerException;
import at.uibk.dps.ee.core.ModelModificationListener;
import at.uibk.dps.ee.model.graph.EnactmentGraphProvider;
import io.vertx.core.shareddata.Lock;
import net.sf.opendse.model.Task;

/**
 * Worker responsible for run-time graph transformations.
 * 
 * @author Fedor Smirnov
 */
public class WorkerTransformation extends VerticleApollo {

  protected final GraphTransformer transformer;
  protected final Set<ModelModificationListener> listeners;

  protected final Logger logger = LoggerFactory.getLogger(WorkerTransformation.class);

  /**
   * Injection constructor
   * 
   * @param eGraphProv provides the enactment graph
   * @param transformer the class with the transform operations for different
   *        tasks
   * @param listeners the transformation listeners
   */
  @Inject
  public WorkerTransformation(final EnactmentGraphProvider eGraphProv,
      final GraphTransformer transformer, final Set<ModelModificationListener> listeners) {
    super(ConstantsVertX.addressRequiredTransformation, ConstantsVertX.addressEnactmentFinished,
        ConstantsVertX.addressFailureAbort, eGraphProv);
    this.transformer = transformer;
    this.listeners = listeners;
  }

  @Override
  protected void work(final Task transformNode) throws WorkerException {
    // transformation performed by one verticle at a time
    this.vertx.sharedData().getLock("transformation lock", lockRes -> {
      if (lockRes.succeeded()) {
        final Lock lock = lockRes.result();
        performTransformation(transformNode);
        lock.release();
      } else {
        throw new IllegalStateException("Failed getting the transformation lock.");
      }
    });
  }

  /**
   * Performs the actual transformation.
   * 
   * @param transformNode the node triggerring the transformation
   * @throws WorkerException thrown in case of transformation exceptions
   */
  protected void performTransformation(final Task transformNode) {
    final GraphTransform transformOperation = transformer.getTransformOperation(transformNode);
    transformOperation.modifyEnactmentGraph(eGraph, transformNode);
    logger.debug("Thread {}; Transform operation task {} completed.",
        Thread.currentThread().getId(), transformNode.getId());
    listeners.forEach(listener -> listener.reactToModelModification());
    this.vertx.eventBus().send(successAddress, transformNode.getId());
  }
}
