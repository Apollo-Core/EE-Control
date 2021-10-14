package at.uibk.dps.ee.control.transformation;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import com.google.gson.JsonObject;
import at.uibk.dps.ee.model.graph.EnactmentGraph;
import at.uibk.dps.ee.model.properties.PropertyServiceData;
import at.uibk.dps.ee.model.properties.PropertyServiceDependency;
import at.uibk.dps.ee.model.properties.PropertyServiceFunction;
import at.uibk.dps.ee.model.properties.PropertyServiceFunctionUtility.UtilityType;
import at.uibk.dps.ee.model.properties.PropertyServiceFunctionUtilityWhile;
import at.uibk.dps.ee.model.properties.PropertyServiceReproduction;
import net.sf.opendse.model.Dependency;
import net.sf.opendse.model.Task;
import net.sf.opendse.model.properties.TaskPropertyService;

/**
 * The {@link GraphTransformWhileCollapse} is a transformation operation to
 * collapse the graph replicas created as part of a while loop at the end of the
 * sequential iteration.
 * 
 * @author Fedor Smirnov
 *
 */
public class GraphTransformWhileCollapse implements GraphTransform {

  @Override
  public void modifyEnactmentGraph(final EnactmentGraph graph, final Task whileEnd) {
    // get the reference to the original while end
    final Task dataOut = graph.getSuccessors(whileEnd).iterator().next();
    final String originalWhileEndRef = PropertyServiceData.getOriginalWhileEndReference(dataOut);
    if (originalWhileEndRef.equals(whileEnd.getId())) {
      // finished after first iteration -> no collapsing
      return;
    }
    final Task originalWhileEnd = graph.getVertex(originalWhileEndRef);
    // transfer the output from the replica to the original while end
    transferOutput(originalWhileEnd, whileEnd);
    // remove all edges currently at the original while end
    graph.getOutEdges(originalWhileEnd).forEach(edge -> graph.removeEdge(edge));
    // transfer all edges to the data outs to the original while end
    graph.getOutEdges(whileEnd)
        .forEach(outEdge -> transferDataOutEdge(outEdge, whileEnd, originalWhileEnd, graph));
    // gather and remove all function nodes pointing to the original while end
    final Set<Task> functionsToRemove =
        graph.getVertices().stream().filter(task -> TaskPropertyService.isProcess(task))
            .filter(function -> PropertyServiceReproduction.isWhileReplica(function)
                && PropertyServiceReproduction.getOriginalWhileEndReference(function)
                    .equals(originalWhileEnd.getId()))
            .collect(Collectors.toSet());
    functionsToRemove.forEach(nodeToRemove -> removeReplicatedFunctionNode(nodeToRemove, graph));
    // remove the now disconnected data nodes
    final Set<Task> disconnected = graph.getVertices().stream().filter(
        vertex -> graph.getSuccessorCount(vertex) == 0 && graph.getPredecessorCount(vertex) == 0)
        .collect(Collectors.toSet());
    disconnected.forEach(disconnectedNode -> graph.removeVertex(disconnectedNode));
    graph.getVertices().stream().filter(task -> TaskPropertyService.isCommunication(task))
        .filter(dataNode -> graph.getIncidentEdges(dataOut).size() == 0)
        .forEach(toRemove -> graph.removeVertex(toRemove));
    // reset the while counter
    final String whileCounterReference =
        PropertyServiceFunctionUtilityWhile.getWhileCounterReference(originalWhileEnd);
    final Task whileCounter = graph.getVertex(whileCounterReference);
    PropertyServiceData.resetWhileCounter(whileCounter);
  }

  /**
   * Transfers the output of the replica while end to the original while end.
   * 
   * @param originalWhileEnd the original while end
   * @param whileEndReplica the while end replica
   */
  protected void transferOutput(final Task originalWhileEnd, final Task whileEndReplica) {
    final JsonObject outputReplica = PropertyServiceFunction.getOutput(whileEndReplica);
    PropertyServiceFunction.resetOutput(originalWhileEnd);
    PropertyServiceFunction.setOutput(originalWhileEnd, outputReplica);
  }

  /**
   * Removes the given node from the graph, as well as all of its in- and
   * out-edges
   * 
   * @param functionNode the given function node
   * @param graph the enactment graph
   */
  protected void removeReplicatedFunctionNode(final Task functionNode, final EnactmentGraph graph) {
    final Set<Dependency> edgesToRemove = new HashSet<>(graph.getIncidentEdges(functionNode));
    edgesToRemove.forEach(edge -> graph.removeEdge(edge));
    graph.removeVertex(functionNode);
  }

  /**
   * Transfers the given edge back to the original while end and removes it from
   * the replica.
   * 
   * @param dataOutEdge the given edge
   * @param replicatedWhileEnd the replicated while end
   * @param originalWhileEnd the original while end
   * @param graph the enactment graph
   */
  protected void transferDataOutEdge(final Dependency dataOutEdge, final Task replicatedWhileEnd,
      final Task originalWhileEnd, final EnactmentGraph graph) {
    final Task dataOut = graph.getDest(dataOutEdge);
    final Dependency fromOriginal = PropertyServiceDependency.addDataDependency(originalWhileEnd,
        dataOut, PropertyServiceDependency.getJsonKey(dataOutEdge), graph);
    dataOutEdge.getAttributeNames().forEach(
        attrName -> fromOriginal.setAttribute(attrName, dataOutEdge.getAttribute(attrName)));
    if (dataOutEdge.getParent() != null) {
      fromOriginal.setParent(dataOutEdge.getParent());
    }
    graph.removeEdge(dataOutEdge);
  }

  @Override
  public String getTransformName() {
    return UtilityType.While.name() + "-Collapse";
  }
}
