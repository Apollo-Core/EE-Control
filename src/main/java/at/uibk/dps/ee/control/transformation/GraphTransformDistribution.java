package at.uibk.dps.ee.control.transformation;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import com.google.inject.Inject;
import at.uibk.dps.ee.model.constants.ConstantsEEModel;
import at.uibk.dps.ee.model.graph.EnactmentGraph;
import at.uibk.dps.ee.model.properties.PropertyServiceData.Property;
import at.uibk.dps.ee.model.properties.PropertyServiceDependency;
import at.uibk.dps.ee.model.properties.PropertyServiceFunctionDataFlowCollections;
import at.uibk.dps.ee.model.properties.PropertyServiceReproduction;
import at.uibk.dps.ee.model.properties.PropertyServiceFunctionDataFlowCollections.OperationType;
import at.uibk.dps.ee.model.properties.PropertyServiceFunctionUtilityWhile.Properties;
import net.sf.opendse.model.Communication;
import net.sf.opendse.model.Dependency;
import net.sf.opendse.model.Task;
import net.sf.opendse.model.properties.TaskPropertyService;

/**
 * The {@link GraphTransformDistribution} transforms the graph by reproducing
 * the graph parts which model a parallel processing of collection data.
 * 
 * @author Fedor Smirnov
 */
public class GraphTransformDistribution implements GraphTransform {

  // The set of attributes which relate to other nodes and therefore, must be
  // adjusted during reproduction
  protected final Set<String> nodeReferenceAttributes;
  protected final Set<String> edgeReferenceAttributes;

  /**
   * Default constructor
   */
  @Inject
  public GraphTransformDistribution() {
    this.nodeReferenceAttributes = generateNodeReferenceAttributes();
    this.edgeReferenceAttributes = generateEdgeReferenceAttributes();
  }

  @Override
  public void modifyEnactmentGraph(final EnactmentGraph graph, final Task taskNode) {
    applyDistributionReproduction(graph, taskNode);
  }

  /**
   * Generates the set of edge attributes which have to be updated during
   * reproduction.
   * 
   * @return the set of edge attributes which have to be updated during
   *         reproduction
   */
  protected final Set<String> generateEdgeReferenceAttributes() {
    // No reference attributes atm. Use this method if any such attributes added in
    // the future.
    return new HashSet<>();
  }

  /**
   * Generates the set of node attributes which have to be updated during
   * reproduction.
   * 
   * @return the set of node attributes which have to be updated during
   *         reproduction
   */
  protected final Set<String> generateNodeReferenceAttributes() {
    final Set<String> result = new HashSet<>();
    result.add(Property.OriginalWhileStart.name());
    result.add(Property.OriginalWhileEnd.name());
    result.add(Properties.WhileStartRef.name());
    result.add(Properties.WhileCounterRef.name());
    return result;
  }

  /**
   * Reproduces parts of the graph to model the parallel processing of distributed
   * data. Scans the graph and finds the part between the distribution node and
   * its aggregators. Creates i (i = number of parallel loop iterations) copies of
   * the subgraph in between and adds them to the graph. Each original element of
   * the graph is annotated as parent of each offspring. The parents are removed
   * from the graph.
   * 
   * @param graph the enactment graph
   * @param distributionTask the distribution task
   */
  protected void applyDistributionReproduction(final EnactmentGraph graph,
      final Task distributionTask) {
    final String scope = PropertyServiceFunctionDataFlowCollections.getScope(distributionTask);
    // find all edges which are relevant for the reproduction
    final Set<Dependency> edgesToReproduce = findEdgesToReproduce(graph, distributionTask);

    // reproduce each of the edges, while keeping track of the new nodes in the
    // graph
    for (final Dependency originalEdge : edgesToReproduce) {
      reproduceEdge(graph, originalEdge, distributionTask);
    }

    // remove the original elements
    removeOriginalElements(graph, edgesToReproduce, scope, distributionTask);

    final Set<Task> newTasks = graph.getVertices().stream()
        .filter(task -> TaskPropertyService.isProcess(task)
            && PropertyServiceReproduction.belongsToDistributionNode(task, distributionTask))
        .collect(Collectors.toSet());
    final Set<Task> aggregationNodes = newTasks.stream()
        .filter(task -> PropertyServiceFunctionDataFlowCollections.isAggregationNode(task)
            && scope.equals(PropertyServiceFunctionDataFlowCollections.getScope(task)))
        .collect(Collectors.toSet());
    newTasks.removeAll(aggregationNodes);
  }


  /**
   * Removes the original elements from the graph.
   * 
   * @param graph the enactment graph
   * @param reproducedEdges the set of original edges between the distribution
   *        node and its aggregators
   * @param scope the reproduction scope
   */
  protected void removeOriginalElements(final EnactmentGraph graph,
      final Set<Dependency> reproducedEdges, final String scope, final Task distributionTask) {
    // gather the vertices to remove
    final Set<Task> verticesToRemove =
        reproducedEdges.stream().map(edge -> graph.getSource(edge)).collect(Collectors.toSet());
    verticesToRemove.addAll(
        reproducedEdges.stream().map(edge -> graph.getDest(edge)).collect(Collectors.toSet()));
    verticesToRemove.removeIf(
        task -> !PropertyServiceReproduction.belongsToDistributionNode(task, distributionTask));
    verticesToRemove
        .removeIf(task -> PropertyServiceFunctionDataFlowCollections.isAggregationNode(task)
            && scope.equals(PropertyServiceFunctionDataFlowCollections.getScope(task)));
    // remove the edges
    reproducedEdges.stream().forEach(dependency -> graph.removeEdge(dependency));
    // remove the vertices
    verticesToRemove.stream().forEach(task -> graph.removeVertex(task));
  }


  /**
   * Reproduces the given edge the requested amount of times. Adds the offspring
   * edge and end points to the graph.
   * 
   * @param originalEdge the original edge
   * @param graph the enactment graph
   */
  protected void reproduceEdge(final EnactmentGraph graph, final Dependency originalEdge,
      final Task distributionNode) {
    final int iterationNum =
        PropertyServiceFunctionDataFlowCollections.getIterationNumber(distributionNode);
    final String scope = PropertyServiceFunctionDataFlowCollections.getScope(distributionNode);
    final Task originalSrc = graph.getSource(originalEdge);
    final Task originalDst = graph.getDest(originalEdge);

    for (int reproductionIdx = 0; reproductionIdx < iterationNum; reproductionIdx++) {

      Optional<Task> offspringSrc;
      String jsonKey = PropertyServiceDependency.getJsonKey(originalEdge);
      Optional<Task> offspringDst;

      // assign src
      if (PropertyServiceReproduction.belongsToDistributionNode(originalSrc, distributionNode)) {
        // src needs to be reproduced
        offspringSrc = reproduceNode(graph, originalSrc, reproductionIdx);
      } else {
        // edge from distribution node
        offspringSrc = Optional.of(originalSrc);
        final String collectionName = PropertyServiceDependency.getJsonKey(originalEdge);
        if (originalSrc.equals(distributionNode)) {
          jsonKey = ConstantsEEModel.getCollectionElementKey(collectionName, reproductionIdx);
        }
      }

      if (PropertyServiceFunctionDataFlowCollections.isAggregationNode(originalDst)
          && PropertyServiceFunctionDataFlowCollections.getScope(originalDst).equals(scope)) {
        // edge to aggregation node
        offspringDst = Optional.of(originalDst);
        jsonKey = ConstantsEEModel.getCollectionElementKey(ConstantsEEModel.JsonKeyAggregation,
            reproductionIdx);
      } else {
        // dst needs to be reproduced
        offspringDst = reproduceNode(graph, originalDst, reproductionIdx);
      }
      final Dependency edgeOffspring = PropertyServiceReproduction.addDataDependencyOffspring(
          offspringSrc.get(), offspringDst.get(), jsonKey, graph, originalEdge, scope);
      final int rpIdx = reproductionIdx;
      originalEdge.getAttributeNames().stream()
          .filter(attrName -> edgeReferenceAttributes.contains(attrName))
          .forEach(attrName -> edgeOffspring.setAttribute(attrName,
              getReproducedId(originalEdge.getAttribute(attrName), rpIdx)));
      // case where the original edge is while annotated -> the data references will
      // point to replicas
      if (PropertyServiceDependency.isWhileAnnotated(originalEdge)) {
        adjustWhileAnnotations(originalEdge, edgeOffspring, rpIdx);
      }
    }
  }

  /**
   * Adjusts the while annotations of an edge which points to different sources
   * between the first and the following while iterations (necessary since the
   * distribution operation changes the node names).
   * 
   * @param originalEdge the original edge
   * @param edgeOffspring the edge created through the distribution operation
   * @param rpIdx the current reproduction index
   */
  protected void adjustWhileAnnotations(final Dependency originalEdge,
      final Dependency edgeOffspring, final int rpIdx) {
    final List<String> whileDataRefsOriginal =
        PropertyServiceDependency.getWhileDataReferences(originalEdge);
    final List<String> whileFuncRefsOriginal =
        PropertyServiceDependency.getWhileFuncReferences(originalEdge);
    PropertyServiceDependency.resetWhileAnnotation(edgeOffspring);
    for (int idx = 0; idx < whileDataRefsOriginal.size(); idx++) {
      final String whileDataRefReplica = getReproducedId(whileDataRefsOriginal.get(idx), rpIdx);
      final String whileFuncRefReplica = getReproducedId(whileFuncRefsOriginal.get(idx), rpIdx);
      PropertyServiceDependency.addWhileInputReference(edgeOffspring, whileDataRefReplica,
          whileFuncRefReplica);
    }
  }



  /**
   * Reproduces the given node and returns an optional of the offspring with the
   * given reproduction index.
   * 
   * @param graph the enactment graph
   * @param original the node to reproduce
   * @param reproductionIdx the reproduction index
   * @param graph the enactment graph
   * @return an optional of the offspring with the given reproduction index
   */
  protected Optional<Task> reproduceNode(final EnactmentGraph graph, final Task original,
      final int reproductionIdx) {
    final String offspringId = getReproducedId(original.getId(), reproductionIdx);
    if (graph.containsVertex(offspringId)) {
      return Optional.of(graph.getVertex(offspringId));
    } else {
      final boolean adjustScope =
          PropertyServiceFunctionDataFlowCollections.isAggregationNode(original)
              || PropertyServiceFunctionDataFlowCollections.isDistributionNode(original);
      final Task task =
          TaskPropertyService.isCommunication(original) ? new Communication(offspringId)
              : new Task(offspringId);
      task.setParent(original);

      // copy all standard attributes
      original.getAttributeNames()
          .forEach(attr -> task.setAttribute(attr,
              nodeReferenceAttributes.contains(attr)
                  ? getReproducedId(original.getAttribute(attr), reproductionIdx)
                  : original.getAttribute(attr)));
      if (adjustScope) {
        final String adjustedScope = PropertyServiceFunctionDataFlowCollections.getScope(original)
            + ConstantsEEModel.KeywordSeparator1 + reproductionIdx;
        PropertyServiceFunctionDataFlowCollections.setScope(task, adjustedScope);
      }
      return Optional.of(task);
    }
  }

  /**
   * Generates the id for the offspring with the given idx
   * 
   * @param originalId the id of the parent
   * @param reproductionIdx the idx of the offspring.
   * @return
   */
  protected String getReproducedId(final String originalId, final int reproductionIdx) {
    return originalId + ConstantsEEModel.KeyWordSeparator2 + reproductionIdx;
  }

  /**
   * Returns the edges which are relevant for the reproductions starting from the
   * provided distribution node.
   * 
   * @param graph the enactment graph
   * @param distributionNode the distribution node
   * @return the edges which are relevant for the reproductions starting from the
   *         provided distribution node
   */
  protected Set<Dependency> findEdgesToReproduce(final EnactmentGraph graph,
      final Task distributionNode) {
    final Set<Dependency> result = new HashSet<>();
    final Task curNode = distributionNode;
    final String scope = PropertyServiceFunctionDataFlowCollections.getScope(distributionNode);
    final Set<Task> visited = new HashSet<>();
    recProcessOutEdgesNode(graph, curNode, scope, visited, result, distributionNode);
    return result;
  }

  /**
   * Recursive operation to check a node and gather all of its out edges which are
   * relevant for reproduction.
   * 
   * @param curNode the node to check
   * @param graph the enactment graph
   * @param scope the reproduction scope
   * @param visited the set of visited nodes
   * @param distributionNode the distribution node doing the reproduction
   * @param result the edges gathered so far
   */
  protected void recProcessOutEdgesNode(final EnactmentGraph graph, final Task curNode,
      final String scope, final Set<Task> visited, final Set<Dependency> result,
      final Task distributionNode) {
    visited.add(curNode);
    if (!curNode.equals(distributionNode)) {
      PropertyServiceReproduction.annotateDistributionNode(curNode, distributionNode.getId());
    }
    if (PropertyServiceFunctionDataFlowCollections.isAggregationNode(curNode)
        && PropertyServiceFunctionDataFlowCollections.getScope(curNode).equals(scope)) {
      // recursion base case: arrival at an aggregation node.
      return;
    } else {
      // if the node is not a distribution node with the proper scope, all in edges
      // are also added
      if (!(PropertyServiceFunctionDataFlowCollections.isDistributionNode(curNode)
          && scope.equals(PropertyServiceFunctionDataFlowCollections.getScope(curNode)))) {
        result.addAll(graph.getInEdges(curNode));
      }
      for (final Dependency outEdge : graph.getOutEdges(curNode)) {
        result.add(outEdge);
        final Task dest = graph.getDest(outEdge);
        if (!visited.contains(dest)) {
          recProcessOutEdgesNode(graph, dest, scope, visited, result, distributionNode);
        }
      }
    }
  }

  @Override
  public String getTransformName() {
    return OperationType.Distribution.name();
  }
}
