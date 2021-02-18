package at.uibk.dps.ee.control.graph;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import at.uibk.dps.ee.core.enactable.Enactable.State;
import at.uibk.dps.ee.model.graph.EnactmentGraph;
import at.uibk.dps.ee.model.properties.PropertyServiceFunction;
import at.uibk.dps.ee.model.properties.PropertyServiceFunctionDataFlowCollections;
import at.uibk.dps.ee.model.properties.PropertyServiceReproduction;
import at.uibk.dps.ee.model.properties.PropertyServiceFunctionDataFlowCollections.OperationType;
import edu.uci.ics.jung.graph.util.EdgeType;
import net.sf.opendse.model.Dependency;
import net.sf.opendse.model.Task;

/**
 * The {@link GraphTransformAggregation} collapses the graph to revert the
 * reproduction performed by the {@link GraphTransformDistribution}. It is used
 * after all aggregation operations of a parallel for scope have been completed.
 * 
 * @author Fedor Smirnov
 */
public class GraphTransformAggregation implements GraphTransform {

  @Override
  public void modifyEnactmentGraph(final GraphAccess graphAccess, final Task taskNode) {
    graphAccess.writeOperationTask(this::revertDistributionReproduction, taskNode);
  }

  /**
   * Checks whether the completion of the provided aggregation node finishes up
   * the operations within the corresponding reproduction scope. Collapses the
   * graph if it does.
   * 
   * @param graph the enactment graph
   * @param aggregationNode the finished aggregation node
   */
  public void revertDistributionReproduction(final EnactmentGraph graph,
      final Task aggregationNode) {
    final String scope = PropertyServiceFunctionDataFlowCollections.getScope(aggregationNode);
    if (!readyForRevert(graph, scope)) {
      return;
    }
    // find the distribution node
    final Set<Task> dNodes = graph.getVertices().stream()
        .filter(task -> PropertyServiceFunctionDataFlowCollections.isDistributionNode(task)
            && PropertyServiceFunctionDataFlowCollections.getScope(task).equals(scope))
        .collect(Collectors.toSet());
    if (dNodes.size() > 1) {
      throw new IllegalArgumentException("Multiple distribution nodes with the scope " + scope);
    }

    // sweep the graph to find the reproduced and the original elements
    final Set<Task> offspringTasks = new HashSet<>();
    final Set<Dependency> offspringDependencies = new HashSet<>();
    final Task distributionNode = dNodes.iterator().next();
    final Task startNode = distributionNode;
    recSweepReproducedGraphSection(graph, startNode, offspringTasks, offspringDependencies, scope);
    // add the original edges (vertices added automatically)
    offspringDependencies
        .forEach(dependency -> addOriginalEdge(graph, dependency, scope, distributionNode));
    // remove the offsprings
    offspringDependencies.forEach(dependency -> graph.removeEdge(dependency));
    offspringTasks.forEach(task -> graph.removeVertex(task));
  }

  /**
   * Finds the original edge and the original end points corresponding to the
   * given offspring edge and adds them to the graph.
   * 
   * @param graph the enactment graph
   * @param offspringEdge the offspring edge
   * @param scope the reproduction scope
   * @param distributionNode the distribution node responsible for the
   *        reproduction which is being reverted
   */
  protected void addOriginalEdge(final EnactmentGraph graph, final Dependency offspringEdge,
      final String scope, final Task distributionNode) {
    if (graph.containsEdge((Dependency) offspringEdge.getParent())) {
      return;
    }
    final Task offspringSrc = graph.getSource(offspringEdge);
    final Task offspringDst = graph.getDest(offspringEdge);
    final Task originalSrc = !wasReproduced(offspringSrc, scope, distributionNode) ? offspringSrc
        : (Task) offspringSrc.getParent();
    if (originalSrc == null) {
      throw new IllegalStateException("The offspring " + offspringSrc + " has no parent.");
    }
    final Task originalDst = !wasReproduced(offspringDst, scope, distributionNode) ? offspringDst
        : (Task) offspringDst.getParent();
    if (originalDst == null) {
      throw new IllegalStateException("The offspring " + offspringDst + " has no parent.");
    }
    final Dependency originalEdge = (Dependency) offspringEdge.getParent();
    graph.addEdge(originalEdge, originalSrc, originalDst, EdgeType.DIRECTED);
  }

  /**
   * Returns true if the given task was reproduced by the given distribution node
   * within the given scope.
   * 
   * @param task the given task
   * @param scope the reproduction scope
   * @param distributionNode the distribution node
   * @return true if the given task was reproduced by the given distribution node
   *         within the given scope
   */
  protected boolean wasReproduced(final Task task, final String scope,
      final Task distributionNode) {
    return PropertyServiceReproduction.belongsToDistributionNode(task, distributionNode)
        && !isEndNodeInScope(task, scope);
  }

  /**
   * Recursively applied method realizing a traversal of the graph section created
   * by the reproduction indicated by the provided scope.
   * 
   * @param currentNode the currently processed node
   * @param originals the set of the pre-reproduction elements
   * @param offsprings the set of the reproduction results
   * @param graph the enacment graph (post-reproduction state)
   * @param scope the reproduction scope
   */
  protected void recSweepReproducedGraphSection(final EnactmentGraph graph, final Task currentNode,
      final Set<Task> offspringTasks, final Set<Dependency> offspringDependencies,
      final String scope) {
    if (isEndNodeInScope(currentNode, scope, false)) {
      // aggregation node as the base case
      return;
    } else {
      if (!isEndNodeInScope(currentNode, scope, true)) {
        // anything which is not a distribution node is an offspring
        offspringTasks.add(currentNode);
        // in that case, we also add the in edges
        offspringDependencies.addAll(graph.getInEdges(currentNode));
      }
      // all out edges are offsprings
      for (final Dependency outEdge : graph.getOutEdges(currentNode)) {
        offspringDependencies.add(outEdge);
        final Task nextNode = graph.getDest(outEdge);
        recSweepReproducedGraphSection(graph, nextNode, offspringTasks, offspringDependencies,
            scope);
      }
    }
  }

  /**
   * Returns true if the given task is either a distribution of an aggregation
   * node in the current scope.
   * 
   * @param task the task to check
   * @param scope the considered reproduction scope
   * @return true if the given task is either a distribution of an aggregation
   *         node in the current scope
   */
  protected boolean isEndNodeInScope(final Task task, final String scope) {
    return isEndNodeInScope(task, scope, false) || isEndNodeInScope(task, scope, true);
  }

  /**
   * Returns true if the given task is either a distribution of an aggregation
   * node in the current scope.
   * 
   * @param task the task to check
   * @param scope the considered reproduction scope
   * @param distribution true if we check for distribution nodes, false in case of
   *        aggregation
   * @return true if the given task is either a distribution of an aggregation
   *         node in the current scope
   */
  protected boolean isEndNodeInScope(final Task task, final String scope,
      final boolean distribution) {
    final boolean collectionNode =
        distribution ? PropertyServiceFunctionDataFlowCollections.isDistributionNode(task)
            : PropertyServiceFunctionDataFlowCollections.isAggregationNode(task);
    return collectionNode
        && scope.equals(PropertyServiceFunctionDataFlowCollections.getScope(task));
  }

  /**
   * Returns true if the reproduction indicated by the provided scope is ready to
   * be reverted (which is the case of all of its aggregators have content
   * available).
   * 
   * @param graph the enactment graph
   * @param scope the reproduction scope
   * @return true if the reproduction indicated by the provided scope is ready to
   *         be reverted
   */
  protected boolean readyForRevert(final EnactmentGraph graph, final String scope) {
    // get the aggregators
    final Set<Task> aggregators = graph.getVertices().stream()
        .filter(task -> PropertyServiceFunctionDataFlowCollections.isAggregationNode(task)
            && PropertyServiceFunctionDataFlowCollections.getScope(task).equals(scope))
        .collect(Collectors.toSet());
    return aggregators.stream().allMatch(aggregator -> PropertyServiceFunction
        .getEnactable(aggregator).getState().equals(State.FINISHED));
  }

  @Override
  public String getTransformName() {
    return OperationType.Aggregation.name();
  }
}