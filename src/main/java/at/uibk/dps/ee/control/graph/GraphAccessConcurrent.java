package at.uibk.dps.ee.control.graph;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import at.uibk.dps.ee.core.enactable.Enactable;
import at.uibk.dps.ee.model.graph.EnactmentGraph;
import at.uibk.dps.ee.model.properties.PropertyServiceData;
import at.uibk.dps.ee.model.properties.PropertyServiceData.NodeType;
import edu.uci.ics.jung.graph.util.EdgeType;
import net.sf.opendse.model.Communication;
import net.sf.opendse.model.Dependency;
import net.sf.opendse.model.Element;
import net.sf.opendse.model.Task;
import net.sf.opendse.model.properties.TaskPropertyService;

/**
 * Implements a threat-safe run-time access to the enactment graph based on a
 * ReadWriteLock.
 * 
 * @author Fedor Smirnov
 */
@Singleton
public class GraphAccessConcurrent implements GraphAccess {

  protected final EnactmentGraph graph;
  protected final ReadWriteLock readWriteLock;
  protected final Lock readLock;
  protected final Lock writeLock;

  /**
   * The injection constructor.
   * 
   * @param graphProvider the provider for the enactment graph (where the function
   *        nodes are annotated with the corresponding {@link Enactable}s.)
   */
  @Inject
  public GraphAccessConcurrent(final GraphProviderEnactables graphProvider) {
    this.graph = graphProvider.getEnactmentGraph();
    this.readWriteLock = new ReentrantReadWriteLock();
    this.readLock = readWriteLock.readLock();
    this.writeLock = readWriteLock.writeLock();
  }

  @Override
  public Set<EdgeTupleAppl> getOutEdges(final Task node) {
    try {
      readLock.lock();
      return graph.getOutEdges(node).stream().map(edge -> getEdgeTupleForEdge(edge))
          .collect(Collectors.toSet());
    } finally {
      readLock.unlock();
    }
  }

  /**
   * Summarizes the edge to an edgeTuple
   * 
   * @param edge the edge to summarize
   * @return the edgeTuple of the edge
   */
  protected EdgeTupleAppl getEdgeTupleForEdge(final Dependency edge) {
    final Task src = graph.getSource(edge);
    final Task dst = graph.getDest(edge);
    return new EdgeTupleAppl(src, dst, edge);
  }

  @Override
  public void writeOperationTask(final BiConsumer<EnactmentGraph, Task> writeOperation,
      final Task task) {
    try {
      writeLock.lock();
      writeOperation.accept(graph, task);
    } finally {
      writeLock.unlock();
    }
  }

  @Override
  public void writeOperationEdge(final BiConsumer<EnactmentGraph, Dependency> writeOperation,
      final Dependency edge) {
    try {
      writeLock.lock();
      writeOperation.accept(graph, edge);
    } finally {
      writeLock.unlock();
    }
  }

  @Override
  public Set<Task> getRootDataNodes() {
    try {
      readLock.lock();
      final Set<Task> result =
          graph.getVertices().stream().filter(task -> graph.getInEdges(task).size() == 0)
              .filter(task -> !PropertyServiceData.getNodeType(task).equals(NodeType.Constant))
              .collect(Collectors.toSet());
      if (result.stream().anyMatch(task -> !PropertyServiceData.isRoot(task))) {
        throw new IllegalStateException("Non-root nodes without in edges present.");
      }
      return result;
    } finally {
      readLock.unlock();
    }
  }

  @Override
  public Set<Task> getLeafDataNodes() {
    try {
      readLock.lock();
      final Set<Task> result = graph.getVertices().stream()
          .filter(task -> graph.getOutEdges(task).size() == 0).collect(Collectors.toSet());
      if (result.stream().anyMatch(task -> !PropertyServiceData.isLeaf(task))) {
        throw new IllegalStateException("Non-leaf nodes without out edges present.");
      }
      return result;
    } finally {
      readLock.unlock();
    }
  }

  @Override
  public Set<Task> getConstantDataNodes() {
    try {
      readLock.lock();
      return graph.getVertices().stream().filter(task -> TaskPropertyService.isCommunication(task))
          .filter(dataNode -> PropertyServiceData.getNodeType(dataNode).equals(NodeType.Constant))
          .collect(Collectors.toSet());
    } finally {
      readLock.unlock();
    }
  }

  @Override
  public EnactmentGraph getGraphCopy() {
    try {
      readLock.lock();
      return copyGraph();
    } finally {
      readLock.unlock();
    }

  }

  /**
   * Generates a copy of the enactment graph
   * 
   * @return a copy of the enactment graph
   */
  protected EnactmentGraph copyGraph() {
    final EnactmentGraph result = new EnactmentGraph();
    // add all vertices
    for (final Task task : graph) {
      result.addVertex((Task) copy(task));
    }
    // add all edges
    for (final Dependency dep : graph.getEdges()) {
      final Dependency copyDep = (Dependency) copy(dep);
      final Task srcOrigin = graph.getSource(dep);
      final Task dstOrigin = graph.getDest(dep);
      final Task srcCopy = result.getVertex(srcOrigin.getId());
      final Task dstCopy = result.getVertex(dstOrigin.getId());
      result.addEdge(copyDep, srcCopy, dstCopy, EdgeType.DIRECTED);
    }
    return result;
  }

  /**
   * Returns a copy of the given element, identical in everything but the
   * reference.
   * 
   * @param original the original element
   * @return the element copy
   */
  protected Element copy(final Element original) {
    // make the object
    Optional<Element> result;
    // Element result = null;
    if (original instanceof Communication) {
      result = Optional.of(new Communication(original.getId()));
    } else if (original instanceof Task) {
      result = Optional.of(new Task(original.getId()));
    } else if (original instanceof Dependency) {
      result = Optional.of(new Dependency(original.getId()));
    } else {
      throw new IllegalArgumentException("Unknown element type for element " + original.getId());
    }
    final Element resultElement = result.get();
    // copy the attributes
    for (final String attrName : original.getAttributeNames()) {
      resultElement.setAttribute(attrName, original.getAttribute(attrName));
    }
    resultElement.setParent(original);
    return resultElement;
  }
}
