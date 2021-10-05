package at.uibk.dps.ee.control.transformation;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import at.uibk.dps.ee.model.constants.ConstantsEEModel;
import at.uibk.dps.ee.model.graph.EnactmentGraph;
import at.uibk.dps.ee.model.properties.PropertyServiceData;
import at.uibk.dps.ee.model.properties.PropertyServiceDependency;
import at.uibk.dps.ee.model.properties.PropertyServiceFunction;
import at.uibk.dps.ee.model.properties.PropertyServiceFunctionUtility.UtilityType;
import at.uibk.dps.ee.model.properties.PropertyServiceFunctionUtilityWhile;
import at.uibk.dps.ee.model.properties.PropertyServiceReproduction;
import at.uibk.dps.ee.model.properties.PropertyServiceFunction.UsageType;
import at.uibk.dps.ee.model.properties.PropertyServiceFunctionUser;
import net.sf.opendse.model.Communication;
import net.sf.opendse.model.Dependency;
import net.sf.opendse.model.Task;

/**
 * Performs the graph transformation creating the graph part for the next
 * iteration of a while compound.
 * 
 * @author Fedor Smirnov
 */
public class GraphTransformWhile implements GraphTransform {

  @Override
  public void modifyEnactmentGraph(final EnactmentGraph graph, final Task whileEnd) {
    // Get the reference to the originalWhileEnd
    final Task dataOutCompound = graph.getSuccessors(whileEnd).iterator().next();
    final String referenceOriginalWhileEnd =
        PropertyServiceData.getOriginalWhileEndReference(dataOutCompound);
    final String originalWhileStartRef = PropertyServiceFunctionUtilityWhile
        .getWhileStart(graph.getVertex(referenceOriginalWhileEnd));
    // get all the functions which have to be replicated
    final String whileStartRef = PropertyServiceFunctionUtilityWhile.getWhileStart(whileEnd);
    final Task whileStartTask = graph.getVertex(whileStartRef);
    // increment the while counter
    final Task whileCounter =
        graph.getVertex(PropertyServiceFunctionUtilityWhile.getWhileCounterReference(whileEnd));
    PropertyServiceData.incrementWhileCounter(whileCounter);
    // integrate replica of while end
    final Task whileEndRep =
        replicateWhileEnd(graph, whileEnd, referenceOriginalWhileEnd, originalWhileStartRef);
    // integrate replica of while start
    final Task whileStartRep =
        replicateWhileStart(graph, whileStartTask, whileEnd, originalWhileStartRef);
    PropertyServiceFunctionUtilityWhile.setWhileStart(whileEndRep, whileStartRep.getId());
    // replicate all functions, add them as successors of the whileEnd, and process
    // their outEdges (contained within the while) and in-edges (potentially
    // pointing to things outside the while body)
    final Set<Task> whileBody = new HashSet<>(graph.getSuccessors(whileStartTask));
    whileBody.forEach(toReplicate -> graph
        .addVertex(replicateTask(toReplicate, referenceOriginalWhileEnd, originalWhileStartRef)));
    // process the out edges (add the data node replicas)
    final String whileRef = PropertyServiceFunctionUtilityWhile.getWhileStart(whileEnd);
    whileBody.forEach(toReplicate -> graph.getOutEdges(toReplicate)
        .forEach(outEdge -> processOutEdge(outEdge, graph, whileRef, originalWhileStartRef)));
    // process the in edges (connect to the predecessor data nodes)
    whileBody.add(whileEnd);
    whileBody.forEach(toReplicate -> graph.getInEdges(toReplicate)
        .forEach(inEdge -> processInEdge(inEdge, graph, whileRef, originalWhileStartRef)));
  }

  /**
   * Integrates and returns the replica of the while end node.
   * 
   * @param graph the enactment graph
   * @param whileEnd the while end node
   * @return the replica of the while end
   */
  protected Task replicateWhileEnd(final EnactmentGraph graph, final Task whileEnd,
      final String originalWhileEndRef, final String originalWhileStart) {
    final Task result = replicateTask(whileEnd, originalWhileEndRef, originalWhileStart);
    // attach all successors of the original to the replica
    graph.getOutEdges(whileEnd)
        .forEach(outEdge -> transferOutputEdge(graph, whileEnd, result, outEdge));
    return result;
  }

  /**
   * Reattaches all out edges from the original to the replica.
   * 
   * @param graph the enactment graph
   * @param original the original node
   * @param replica the replica node
   * @param originalOutEdge the copied edge
   */
  protected void transferOutputEdge(final EnactmentGraph graph, final Task original,
      final Task replica, final Dependency originalOutEdge) {
    final Task dst = graph.getDest(originalOutEdge);
    final String jsonKey = PropertyServiceDependency.getJsonKey(originalOutEdge);
    final Dependency newEdge =
        PropertyServiceDependency.addDataDependency(replica, dst, jsonKey, graph);
    if (originalOutEdge.getParent() != null) {
      newEdge.setParent(originalOutEdge.getParent());
    }
    originalOutEdge.getAttributeNames().forEach(
        attrName -> newEdge.setAttribute(attrName, originalOutEdge.getAttribute(attrName)));
    graph.removeEdge(originalOutEdge);
  }


  /**
   * Replicates the while start node and adds it to the graph.
   * 
   * @param graph the enactment graph
   * @param whileStart the original while start
   * @param whileEnd the original while end
   */
  protected Task replicateWhileStart(final EnactmentGraph graph, final Task whileStart,
      final Task whileEnd, final String originalWhileStart) {
    final Task whileStartRep = replicateDataNode(whileStart, originalWhileStart);
    PropertyServiceDependency.addDataDependency(whileEnd, whileStartRep,
        ConstantsEEModel.JsonKeyWhileDecision, graph);
    return whileStartRep;
  }


  /**
   * Processes the in edges of the original and adds the same in edges to the
   * replicated part of the graph
   * 
   * @param originalInEdge the original in edge (from data to function)
   * @param graph the enactment graph
   */
  protected void processInEdge(final Dependency originalInEdge, final EnactmentGraph graph,
      final String whileId, final String originalWhileStart) {
    final Task originalData = graph.getSource(originalInEdge);
    final boolean dataIsReplicated =
        graph.containsVertex(getReplicaId(originalData, originalWhileStart));
    final Task replicaSrc = getReplicaSrc(originalInEdge, graph, whileId, originalWhileStart);
    final Task replicaDst = findReplica(graph.getDest(originalInEdge), graph, originalWhileStart);
    final Dependency replica =
        addDependencyReplica(replicaSrc, replicaDst, originalInEdge, graph, whileId);
    if (dataIsReplicated && !PropertyServiceDependency.doesPointToPreviousIteration(replica)) {
      PropertyServiceDependency.resetTransmission(replica);
    }
  }

  /**
   * Returns the node which is to be used as the source for the edge created by
   * replicating the given edge.
   * 
   * @param replicatedEdge the edge being replicated
   * @param graph the enactment graph
   * @return the node which is to be used as src
   */
  protected Task getReplicaSrc(final Dependency replicatedEdge, final EnactmentGraph graph,
      final String whileReference, final String originalWhileStart) {
    if (PropertyServiceDependency.isWhileAnnotated(replicatedEdge) && PropertyServiceDependency
        .getReplicaWhileFuncReference(replicatedEdge).equals(whileReference)) {
      // there is a difference between first and further iterations
      return Optional
          .ofNullable(
              graph.getVertex(PropertyServiceDependency.getReplicaSrcReference(replicatedEdge)))
          .orElseThrow(() -> new IllegalStateException("The edge " + replicatedEdge.getId()
              + " points to a non-existant while replica reference."));
    } else {
      // standard dependency
      final Task originalData = graph.getSource(replicatedEdge);
      final boolean dataIsReplicated =
          graph.containsVertex(getReplicaId(originalData, originalWhileStart));
      return dataIsReplicated ? graph.getVertex(getReplicaId(originalData, originalWhileStart))
          : originalData;
    }
  }

  /**
   * Processes the out edge of an original function.
   *
   * @param originalOutEdge the out edge to process
   * @param replicaFunc the replicated function
   * @param graph the enactment graph
   */
  protected void processOutEdge(final Dependency originalOutEdge, final EnactmentGraph graph,
      final String whileId, final String originalWhileId) {
    final Task replicaFunc = findReplica(graph.getSource(originalOutEdge), graph, originalWhileId);
    final Task originalData = graph.getDest(originalOutEdge);
    // create the data node replica
    final Task replicatedDataNode = replicateDataNode(originalData, originalWhileId);
    // connect it to the src replica
    addDependencyReplica(replicaFunc, replicatedDataNode, originalOutEdge, graph, whileId);
  }

  /**
   * Returns the replica for the provided node.
   * 
   * @param original the original node
   * @param graph the enactment graph
   * @return the replica for the provided node
   */
  protected Task findReplica(final Task original, final EnactmentGraph graph,
      final String whileRef) {
    final String replicaId = getReplicaId(original, whileRef);
    if (graph.getVertex(replicaId) == null) {
      throw new IllegalStateException("Replica not in graph for task " + original);
    }
    return graph.getVertex(replicaId);
  }

  /**
   * Adds a replica of the provided dependency to the provided graph.
   * 
   * @param replicaSrc the replica src
   * @param replicaDest the replica dst
   * @param originalDep the dependency being replicated
   * @param graph the enactment graph
   * @return the added dependency
   */
  protected Dependency addDependencyReplica(final Task replicaSrc, final Task replicaDest,
      final Dependency originalDep, final EnactmentGraph graph, final String whileNodeId) {
    final String jsonKey = PropertyServiceDependency.getJsonKey(originalDep);
    final Dependency replica =
        PropertyServiceDependency.addDataDependency(replicaSrc, replicaDest, jsonKey, graph);
    originalDep.getAttributeNames()
        .forEach(attrName -> replica.setAttribute(attrName, originalDep.getAttribute(attrName)));
    if (PropertyServiceDependency.isWhileAnnotated(originalDep) && PropertyServiceDependency
        .getReplicaWhileFuncReference(originalDep).equals(whileNodeId)) {
      PropertyServiceDependency.resetWhileAnnotation(replica);
      PropertyServiceDependency.annotatePreviousIterationDependency(replica);
    }
    return replica;
  }

  /**
   * Replicates the given data node.
   * 
   * @param original the given data node
   * @return the replica
   */
  protected Task replicateDataNode(final Task original, final String whileStartRef) {
    final Task result = new Communication(getReplicaId(original, whileStartRef));
    original.getAttributeNames()
        .forEach(attrName -> result.setAttribute(attrName, original.getAttribute(attrName)));
    PropertyServiceData.resetContent(result);

    // check for a reference to the (nested) while start. If there is one, we have
    // to redirect it to the nested while start created during this transformation
    if (PropertyServiceData.isWhileOutput(result)) {
      final String originalReference = PropertyServiceData.getOriginalWhileEndReference(result);
      final String updatedReference = getReplicaId(originalReference, whileStartRef);
      PropertyServiceData.annotateOriginalWhileEnd(result, updatedReference);
    }
    return result;
  }

  /**
   * Replicates the provided task by creating a copy of it, representing the
   * processing in the next iteration.
   * 
   * @param original the original task
   * @param whileReference the reference to the while task
   */
  protected Task replicateTask(final Task original, final String originalWhileEndRef,
      final String whileStarRef) {
    final String replicaId = getReplicaId(original, whileStarRef);
    final Task result = new Task(replicaId);
    result.setParent(original);
    original.getAttributeNames()
        .forEach(attrName -> result.setAttribute(attrName, original.getAttribute(attrName)));
    PropertyServiceFunction.resetInput(result);
    PropertyServiceFunction.resetOutput(result);
    if (PropertyServiceFunction.getUsageType(result).equals(UsageType.User)) {
      final String originalRef = PropertyServiceFunctionUser.isWhileReplica(original)
          ? PropertyServiceFunctionUser.getWhileRef(original)
          : original.getId();
      PropertyServiceFunctionUser.setWhileRef(result, originalRef);
    }
    // set the while end reference
    PropertyServiceReproduction.setOriginalWhileEndReference(result, originalWhileEndRef);

    // the case that we are replicating a nested while end
    if (PropertyServiceFunctionUtilityWhile.isWhileEndTask(result)
        && !PropertyServiceFunctionUtilityWhile.getWhileStart(result).equals(whileStarRef)) {
      final String currentReference = PropertyServiceFunctionUtilityWhile.getWhileStart(result);
      final String updatedReference = getReplicaId(currentReference, whileStarRef);
      PropertyServiceFunctionUtilityWhile.setWhileStart(result, updatedReference);
    }
    return result;
  }

  /**
   * Returns the ID for the replica of an original.
   * 
   * @param originalId the original
   * @param whileRef the reference to the while compound we are in
   * @return the ID for the replica of an original
   */
  protected String getReplicaId(final Task original, final String whileRef) {
    return getReplicaId(original.getId(), whileRef);
  }

  /**
   * Same as above, but based on the original id and not the whole task
   * 
   * @param originalId if od the original task
   * @param whileRef the reference to the while compound we are in
   * @return the ID for the replica of an original
   */
  protected String getReplicaId(final String originalId, final String whileRef) {
    return originalId + ConstantsEEModel.whileReplicaSuffix + whileRef;
  }

  @Override
  public String getTransformName() {
    return UtilityType.While.name();
  }
}
