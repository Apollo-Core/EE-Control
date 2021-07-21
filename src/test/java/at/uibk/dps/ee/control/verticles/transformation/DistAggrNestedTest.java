package at.uibk.dps.ee.control.verticles.transformation;

import static org.junit.Assert.*;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Test;
import com.google.gson.JsonObject;
import at.uibk.dps.ee.model.constants.ConstantsEEModel;
import at.uibk.dps.ee.model.graph.EnactmentGraph;
import at.uibk.dps.ee.model.properties.PropertyServiceData;
import at.uibk.dps.ee.model.properties.PropertyServiceDependency;
import at.uibk.dps.ee.model.properties.PropertyServiceFunction;
import at.uibk.dps.ee.model.properties.PropertyServiceFunctionDataFlowCollections;
import at.uibk.dps.ee.model.properties.PropertyServiceFunction.UsageType;
import at.uibk.dps.ee.model.properties.PropertyServiceFunctionDataFlowCollections.OperationType;
import net.sf.opendse.model.Communication;
import net.sf.opendse.model.Task;

public class DistAggrNestedTest {

  @Test
  public void test() {
    // create the input graph
    String scopeName = "scope";
    String scopeName2 = "innerScope";
    String collNameIn = "collectionInput";
    String funcInName = "funcIn";
    String funcName = "function";
    String funcOutName = "funcOut";

    EnactmentGraph testInput = new EnactmentGraph();

    Communication wfInput = new Communication("input");
    PropertyServiceData.makeRoot(wfInput);

    Communication wfOutput = new Communication("output");
    PropertyServiceData.makeLeaf(wfOutput);

    Task distributionNode = PropertyServiceFunctionDataFlowCollections
        .createCollectionDataFlowTask("distribution", OperationType.Distribution, scopeName);
    PropertyServiceFunctionDataFlowCollections.setIterationNumber(distributionNode, 3);
    Task distributionNode2 = PropertyServiceFunctionDataFlowCollections
        .createCollectionDataFlowTask("distribution2", OperationType.Distribution, scopeName2);
    PropertyServiceFunctionDataFlowCollections.setIterationNumber(distributionNode, 3);
    PropertyServiceFunctionDataFlowCollections.setIterationNumber(distributionNode2, 3);

    Task function = new Task(funcName);
    PropertyServiceFunction.setUsageType(UsageType.User, function);

    Task aggregation = PropertyServiceFunctionDataFlowCollections
        .createCollectionDataFlowTask("aggregation", OperationType.Aggregation, scopeName);
    Task aggregation2 = PropertyServiceFunctionDataFlowCollections
        .createCollectionDataFlowTask("aggregation2", OperationType.Aggregation, scopeName2);

    String funcInDataName = "distributedData";
    String funcOutDataName = "funcResult";
    Communication distributedData = new Communication(funcInDataName);
    Communication distributedData2 = new Communication(funcInDataName + "2");
    Communication functionResult = new Communication(funcOutDataName);
    Communication functionResult2 = new Communication(funcOutDataName + "2");
    PropertyServiceDependency.addDataDependency(wfInput, distributionNode, collNameIn, testInput);
    PropertyServiceDependency.addDataDependency(distributionNode, distributedData, collNameIn,
        testInput);
    PropertyServiceDependency.addDataDependency(distributedData, distributionNode2, collNameIn,
        testInput);
    PropertyServiceDependency.addDataDependency(distributionNode2, distributedData2, collNameIn,
        testInput);
    PropertyServiceDependency.addDataDependency(distributedData2, function, funcInName, testInput);
    PropertyServiceDependency.addDataDependency(function, functionResult, funcOutName, testInput);
    PropertyServiceDependency.addDataDependency(functionResult, aggregation2,
        ConstantsEEModel.JsonKeyAggregation, testInput);
    PropertyServiceDependency.addDataDependency(aggregation2, functionResult2,
        ConstantsEEModel.JsonKeyAggregation, testInput);
    PropertyServiceDependency.addDataDependency(functionResult2, aggregation,
        ConstantsEEModel.JsonKeyAggregation, testInput);
    PropertyServiceDependency.addDataDependency(aggregation, wfOutput,
        ConstantsEEModel.JsonKeyAggregation, testInput);
    Communication outsideInput = new Communication("outsideIn");
    String jsonKeyOutside = "outside";
    PropertyServiceDependency.addDataDependency(outsideInput, function, jsonKeyOutside, testInput);

    GraphTransformDistribution tested = new GraphTransformDistribution();
    assertEquals(12, testInput.getVertexCount());
    // run the first distribution
    tested.modifyEnactmentGraph(testInput, distributionNode);
    assertEquals(26, testInput.getVertexCount());
    // get the reproduced distributions nodes
    Set<Task> reproduced = testInput.getVertices().stream()
        .filter(task -> task.getParent() != null && task.getParent().equals(distributionNode2))
        .collect(Collectors.toSet());
    Map<String, List<Task>> groupedByScope = reproduced.stream().collect(
        Collectors.groupingBy(task -> PropertyServiceFunctionDataFlowCollections.getScope(task)));
    assertEquals(3, groupedByScope.size());
    Set<Task> reproducedAggr = testInput.getVertices().stream()
        .filter(task -> task.getParent() != null && task.getParent().equals(aggregation2))
        .collect(Collectors.toSet());
    Map<String, List<Task>> aggrByScope = reproducedAggr.stream().collect(
        Collectors.groupingBy(task -> PropertyServiceFunctionDataFlowCollections.getScope(task)));
    assertEquals(3, aggrByScope.size());
    // run the second distribution
    reproduced.forEach(distTask -> tested.modifyEnactmentGraph(testInput, distTask));
    assertEquals(44, testInput.getVertexCount());
    // inner aggregation
    GraphTransformAggregation aggregationOperation = new GraphTransformAggregation();
    reproducedAggr.forEach(repAgg -> PropertyServiceFunction.setInput(repAgg, new JsonObject()));
    reproducedAggr.forEach(aggr -> aggregationOperation.modifyEnactmentGraph(testInput, aggr));
    assertEquals(26, testInput.getVertexCount());
    // outer aggregation
    PropertyServiceFunction.setInput(aggregation, new JsonObject());
    aggregationOperation.modifyEnactmentGraph(testInput, aggregation);
    assertEquals(12, testInput.getVertexCount());
  }
}
