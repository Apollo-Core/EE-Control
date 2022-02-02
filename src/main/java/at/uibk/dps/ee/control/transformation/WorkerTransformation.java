package at.uibk.dps.ee.control.transformation;

import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.inject.Inject;
import at.uibk.dps.ee.control.verticles.ConstantsVertX;
import at.uibk.dps.ee.control.verticles.VerticleApollo;
import at.uibk.dps.ee.control.verticles.WorkerException;
import at.uibk.dps.ee.core.ModelModificationListener;
import at.uibk.dps.ee.model.graph.SpecificationProvider;
import io.vertx.core.shareddata.Lock;
import net.sf.opendse.model.Task;

/**
 * Worker responsible for run-time graph transformations.
 * 
 * @author Fedor Smirnov
 */
public class WorkerTransformation extends VerticleApollo {

  protected final GraphTransformer transformer;
  protected final PostTransformation postTransformation;
  protected final Set<ModelModificationListener> listeners;

  protected final Logger logger = LoggerFactory.getLogger(WorkerTransformation.class);

  /**
   * Injection constructor
   * 
   * @param specProv provides the spec
   * @param transformer the class with the transform operations for different
   *        tasks
   * @param listeners the transformation listeners
   * @param postTransformation the object performing the action to trigger the
   *        next step of the orchestration
   */
  @Inject
  public WorkerTransformation(final SpecificationProvider specProv,
      final GraphTransformer transformer, final Set<ModelModificationListener> listeners,
      final PostTransformation postTransformation) {
    super(ConstantsVertX.addressRequiredTransformation, ConstantsVertX.addressEnactmentFinished,
        ConstantsVertX.addressFailureAbort, specProv);
    this.transformer = transformer;
    this.listeners = listeners;
    this.postTransformation = postTransformation;
  }

  @Override
  protected void work(final Task transformNode) throws WorkerException {
    this.vertx.sharedData().getLock(ConstantsVertX.transformTransmitLock, lockRes -> {
      if (lockRes.succeeded()) {
        final Lock lock = lockRes.result();
        performTransformation(transformNode);
        lock.release();
      } else {
        throw new IllegalStateException("Failed getting transformation annotation lock");
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
    postTransformation.postTransformationTreatment(transformNode, this.vertx.eventBus());
  }
}
