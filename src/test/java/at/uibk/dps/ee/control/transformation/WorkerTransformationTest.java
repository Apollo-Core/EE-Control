package at.uibk.dps.ee.control.transformation;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import at.uibk.dps.ee.core.ModelModificationListener;
import at.uibk.dps.ee.model.graph.EnactmentGraph;
import at.uibk.dps.ee.model.graph.SpecificationProvider;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import net.sf.opendse.model.Task;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

class WorkerTransformationTest {

  protected class MockWorker extends WorkerTransformation {
    public MockWorker(SpecificationProvider specProv, GraphTransformer transformer,
        Set<ModelModificationListener> listeners, PostTransformation postTransformation) {
      super(specProv, transformer, listeners, postTransformation);
    }

    public void setVertX(Vertx vertx) {
      this.vertx = vertx;
    }
  }

  GraphTransformer transformer;
  GraphTransform operation;
  PostTransformation postTransformation;
  ModelModificationListener listener;
  EventBus eBus;
  Task input;
  EnactmentGraph graph;

  MockWorker tested;

  /**
   * Tests the correct call of the transform operation.
   */
  @Test
  void test() {
    tested.performTransformation(input);
    verify(operation).modifyEnactmentGraph(graph, input);
    verify(listener).reactToModelModification();
    verify(postTransformation).postTransformationTreatment(input, eBus);
  }

  @BeforeEach
  void setup() {
    input = new Task("task");
    transformer = mock(GraphTransformer.class);
    operation = mock(GraphTransform.class);
    postTransformation = mock(PostTransformation.class);
    when(transformer.getTransformOperation(input)).thenReturn(operation);
    Vertx vertx = mock(Vertx.class);
    eBus = mock(EventBus.class);
    when(vertx.eventBus()).thenReturn(eBus);
    graph = new EnactmentGraph();
    SpecificationProvider specProv = mock(SpecificationProvider.class);
    when(specProv.getEnactmentGraph()).thenReturn(graph);
    listener = mock(ModelModificationListener.class);
    Set<ModelModificationListener> listeners = new HashSet<>();
    listeners.add(listener);
    tested = new MockWorker(specProv, transformer, listeners, postTransformation);
    tested.setVertX(vertx);
  }
}
