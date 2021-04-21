package at.uibk.dps.ee.control.graph;

import java.util.Set;
import java.util.function.BiConsumer;
import com.google.inject.ImplementedBy;
import at.uibk.dps.ee.model.graph.EnactmentGraph;
import net.sf.opendse.model.Dependency;
import net.sf.opendse.model.Task;

/**
 * Interface for the classes offering access to the enactment graph.
 * 
 * @author Fedor Smirnov
 *
 */
@ImplementedBy(GraphAccessConcurrent.class)
public interface GraphAccess {

  /**
   * Convenience class to handle edge objects
   * 
   * @author Fedor Smirnov
   */
  class EdgeTupleAppl {
    protected final Task src;
    protected final Task dst;
    protected final Dependency edge;

    /**
     * Default constructor.
     * 
     * @param src the source node
     * @param dst the destination node
     * @param edge the edge
     */
    public EdgeTupleAppl(final Task src, final Task dst, final Dependency edge) {
      super();
      this.src = src;
      this.dst = dst;
      this.edge = edge;
    }

    public Task getSrc() {
      return src;
    }

    public Task getDst() {
      return dst;
    }

    public Dependency getEdge() {
      return edge;
    }
  }
  
  /**
   * Returns a copy of the enactment graph which can be used for read purposes.
   * 
   * @return A copy of the enactment graph which can be used for read purposes
   */
  EnactmentGraph getGraphCopy();

  /**
   * Returns the edgeTuples (end nodes + edge) for all out edges of the given node
   * 
   * @param node the given node
   * @return the edgeTuples (end nodes + edge) for all out edges of the given node
   */
  Set<EdgeTupleAppl> getOutEdges(Task node);

  /**
   * Returns the root nodes of the graph (annotated with the WF input).
   * 
   * @return the root nodes of the graph (annotated with the WF input)
   */
  Set<Task> getRootDataNodes();

  /**
   * Returns the leaf nodes of the graph (to be annotated with the WF result).
   * 
   * @return the leaf nodes of the graph (to be annotated with the WF result)
   */
  Set<Task> getLeafDataNodes();

  /**
   * Returns the constant data nodes of the graph.
   * 
   * @return the set of the constant data nodes.
   */
  Set<Task> getConstantDataNodes();

  /**
   * Applies the given write operation (relatively to the given task) in
   * thread-safe way.
   * 
   * @param writeOperation the write operation to apply
   * @param task the given task
   */
  void writeOperationTask(BiConsumer<EnactmentGraph, Task> writeOperation, Task task);
  
  /**
   * Applies the given write operation (relatively to the given edge) in
   * thread-safe way.
   * 
   * @param writeOperation the write operation to apply
   * @param task the given edge
   */
  void writeOperationEdge(BiConsumer<EnactmentGraph, Dependency> writeOperation, Dependency edge);
}
