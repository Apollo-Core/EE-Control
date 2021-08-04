package at.uibk.dps.ee.control.transformation;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import com.google.gson.JsonPrimitive;
import at.uibk.dps.ee.model.constants.ConstantsEEModel;
import at.uibk.dps.ee.model.graph.EnactmentGraph;
import at.uibk.dps.ee.model.properties.PropertyServiceData;
import at.uibk.dps.ee.model.properties.PropertyServiceDependency;
import at.uibk.dps.ee.model.properties.PropertyServiceFunction;
import at.uibk.dps.ee.model.properties.PropertyServiceFunctionUtility.UtilityType;
import at.uibk.dps.ee.model.properties.PropertyServiceFunctionUtilityWhile;
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
  public void modifyEnactmentGraph(EnactmentGraph graph, Task whileEnd) {
    // get all the functions which have to be replicated
    String whileStartRef = PropertyServiceFunctionUtilityWhile.getWhileStart(whileEnd);
    Task whileStartTask = graph.getVertex(whileStartRef);
    // increment the while counter
    Task whileCounter =
        graph.getVertex(PropertyServiceFunctionUtilityWhile.getWhileCounterReference(whileEnd));
    PropertyServiceData.incrementWhileCounter(whileCounter);
    // integrate replica of while end
    Task whileEndRep = replicateWhileEnd(graph, whileEnd);
    // integrate replica of while start
    Task whileStartRep = replicateWhileStart(graph, whileStartTask, whileEnd);
    PropertyServiceFunctionUtilityWhile.setWhileStart(whileEndRep, whileStartRep);
    // replicate all functions, add them as successors of the whileEnd, and process
    // their outEdges (contained within the while) and in-edges (potentially
    // pointing to things outside the while body)
    Set<Task> whileBody = new HashSet<>(graph.getSuccessors(whileStartTask));
    whileBody.forEach(toReplicate -> graph.addVertex(replicateTask(toReplicate)));
    // process the out edges (add the data node replicas)
    String whileRef = PropertyServiceFunctionUtilityWhile.getWhileStart(whileEnd);
    whileBody.forEach(toReplicate -> graph.getOutEdges(toReplicate)
        .forEach(outEdge -> processOutEdge(outEdge, graph, whileRef)));
    // process the in edges (connect to the predecessor data nodes)
    whileBody.add(whileEnd);
    whileBody.forEach(toReplicate -> graph.getInEdges(toReplicate)
        .forEach(inEdge -> processInEdge(inEdge, graph, whileRef)));
  }

  /**
   * Increments the while loop counter.
   * 
   * @param whileCounter the node with the loop count
   */
  protected void incrementWhileCounter(Task whileCounter) {
    int count = PropertyServiceData.getContent(whileCounter).getAsInt();
    PropertyServiceData.setContent(whileCounter, new JsonPrimitive(++count));
  }

  /**
   * Integrates and returns the replica of the while end node.
   * 
   * @param graph the enactment graph
   * @param whileEnd the while end node
   * @return the replica of the while end
   */
  protected Task replicateWhileEnd(EnactmentGraph graph, Task whileEnd) {
    Task result = replicateTask(whileEnd);
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
  protected void transferOutputEdge(EnactmentGraph graph, Task original, Task replica,
      Dependency originalOutEdge) {
    Task dst = graph.getDest(originalOutEdge);
    String jsonKey = PropertyServiceDependency.getJsonKey(originalOutEdge);
    Dependency newEdge = PropertyServiceDependency.addDataDependency(replica, dst, jsonKey, graph);
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
  protected Task replicateWhileStart(EnactmentGraph graph, Task whileStart, Task whileEnd) {
    Task whileStartRep = replicateDataNode(whileStart);
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
  protected void processInEdge(Dependency originalInEdge, EnactmentGraph graph, String whileId) {
    Task originalData = graph.getSource(originalInEdge);
    boolean dataIsReplicated = graph.getVertex(getReplicaId(originalData)) != null;
    Task replicaSrc = getReplicaSrc(originalInEdge, graph);
    Task replicaDst = findReplica(graph.getDest(originalInEdge), graph);
    Dependency replica =
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
  protected Task getReplicaSrc(Dependency replicatedEdge, EnactmentGraph graph) {
    if (PropertyServiceDependency.isWhileAnnotated(replicatedEdge)) {
      // there is a difference between first and further iterations
      return Optional
          .ofNullable(
              graph.getVertex(PropertyServiceDependency.getReplicaSrcReference(replicatedEdge)))
          .orElseThrow(() -> new IllegalStateException("The edge " + replicatedEdge.getId()
              + " points to a non-existant while replica reference."));
    } else {
      // standard dependency
      Task originalData = graph.getSource(replicatedEdge);
      boolean dataIsReplicated = graph.getVertex(getReplicaId(originalData)) != null;
      return dataIsReplicated ? graph.getVertex(getReplicaId(originalData)) : originalData;
    }
  }

  /**
   * Processes the out edge of an original function.
   *
   * @param originalOutEdge the out edge to process
   * @param replicaFunc the replicated function
   * @param graph the enactment graph
   */
  protected void processOutEdge(Dependency originalOutEdge, EnactmentGraph graph, String whileId) {
    Task replicaFunc = findReplica(graph.getSource(originalOutEdge), graph);
    Task originalData = graph.getDest(originalOutEdge);
    // create the data node replica
    Task replicatedDataNode = replicateDataNode(originalData);
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
  protected Task findReplica(Task original, EnactmentGraph graph) {
    String replicaId = getReplicaId(original);
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
  protected Dependency addDependencyReplica(Task replicaSrc, Task replicaDest,
      Dependency originalDep, EnactmentGraph graph, String whileNodeId) {
    String jsonKey = PropertyServiceDependency.getJsonKey(originalDep);
    Dependency replica =
        PropertyServiceDependency.addDataDependency(replicaSrc, replicaDest, jsonKey, graph);
    originalDep.getAttributeNames()
        .forEach(attrName -> replica.setAttribute(attrName, originalDep.getAttribute(attrName)));
    if (PropertyServiceDependency.isWhileAnnotated(originalDep)
        && PropertyServiceDependency.getReplicaWhileFuncRefernce(originalDep).equals(whileNodeId)) {
      PropertyServiceDependency.resetWhileAnnotation(replica);
      PropertyServiceDependency.annotatePreviousIterationDependency(replica);
//      PropertyServiceDependency.annotateWhileReplica(replica,
//          graph.getVertex(getReplicaId(
//              graph.getVertex(PropertyServiceDependency.getReplicaSrcReference(originalDep)))),
//          whileNodeId);
    }
    return replica;
  }

  /**
   * Replicates the given data node.
   * 
   * @param original the given data node
   * @return the replica
   */
  protected Task replicateDataNode(Task original) {
    Task result = new Communication(getReplicaId(original));
    original.getAttributeNames()
        .forEach(attrName -> result.setAttribute(attrName, original.getAttribute(attrName)));
    PropertyServiceData.resetContent(result);
    return result;
  }

  /**
   * Replicates the providedtask by creating a copy of it, representing the
   * processing in the next iteration.
   * 
   * @param original
   */
  protected Task replicateTask(Task original) {
    String replicaId = getReplicaId(original);
    Task result = new Task(replicaId);
    original.getAttributeNames()
        .forEach(attrName -> result.setAttribute(attrName, original.getAttribute(attrName)));
    PropertyServiceFunction.resetInput(result);
    PropertyServiceFunction.resetOutput(result);
    if (PropertyServiceFunction.getUsageType(result).equals(UsageType.User)) {
      String originalId = PropertyServiceFunctionUser.isSeqReplica(original)
          ? PropertyServiceFunctionUser.getOriginalRef(original)
          : original.getId();
      PropertyServiceFunctionUser.setOriginalRef(result, originalId);
    }
    return result;
  }

  /**
   * Returns the ID for the replica of an original.
   * 
   * @param originalId the original
   * @return the ID for the replica of an original
   */
  protected String getReplicaId(Task original) {
    return original.getId() + ConstantsEEModel.whileReplicaSuffix;
  }

  @Override
  public String getTransformName() {
    return UtilityType.While.name();
  }

}
