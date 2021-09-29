package at.uibk.dps.ee.control.transformation;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
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
import net.sf.opendse.model.Dependency;
import net.sf.opendse.model.Task;

public class GraphTransformDistributionAggregationTest {

  @Test
  public void test() {
    // create the input graph
    String scopeName = "scope";
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

    Task function = new Task(funcName);
    PropertyServiceFunction.setUsageType(UsageType.User, function);

    Task aggregation = PropertyServiceFunctionDataFlowCollections
        .createCollectionDataFlowTask("aggregation", OperationType.Aggregation, scopeName);

    String funcInDataName = "distributedData";
    String funcOutDataName = "funcResult";
    Communication distributedData = new Communication(funcInDataName);
    Communication functionResult = new Communication(funcOutDataName);
    PropertyServiceDependency.addDataDependency(wfInput, distributionNode, collNameIn, testInput);
    PropertyServiceDependency.addDataDependency(distributionNode, distributedData, collNameIn,
        testInput);
    PropertyServiceDependency.addDataDependency(distributedData, function, funcInName, testInput);
    PropertyServiceDependency.addDataDependency(function, functionResult, funcOutName, testInput);
    PropertyServiceDependency.addDataDependency(functionResult, aggregation,
        ConstantsEEModel.JsonKeyAggregation, testInput);
    PropertyServiceDependency.addDataDependency(aggregation, wfOutput,
        ConstantsEEModel.JsonKeyAggregation, testInput);
    Communication outsideInput = new Communication("outsideIn");
    String jsonKeyOutside = "outside";
    PropertyServiceDependency.addDataDependency(outsideInput, function, jsonKeyOutside, testInput);
    GraphTransformDistribution tested = new GraphTransformDistribution();

    // run the operation
    tested.modifyEnactmentGraph(testInput, distributionNode);
    GraphTransformAggregation testedAggregation = new GraphTransformAggregation();
    assertEquals(OperationType.Distribution.name(), tested.getTransformName());
    assertEquals(OperationType.Aggregation.name(), testedAggregation.getTransformName());

    // do the tests
    // element numbers
    assertEquals(14, testInput.getVertexCount());
    assertEquals(17, testInput.getEdgeCount());
    Task funcIn2 = testInput.getVertex(tested.getReproducedId(funcInDataName, 2));
    assertNotNull(funcIn2);
    Task func2 = testInput.getVertex(tested.getReproducedId(funcName, 2));
    assertNotNull(func2);
    Task funcOut2 = testInput.getVertex(tested.getReproducedId(funcOutDataName, 2));
    assertNotNull(funcOut2);
    Dependency distributionEdge = testInput.getInEdges(funcIn2).iterator().next();
    Dependency aggregationEdge = testInput.getOutEdges(funcOut2).iterator().next();
    Dependency funcOutEdge = testInput.getOutEdges(func2).iterator().next();

    // check the JSON keys
    String expectedDistJson = ConstantsEEModel.getCollectionElementKey(collNameIn, 2);
    String expectedAggrJson =
        ConstantsEEModel.getCollectionElementKey(ConstantsEEModel.JsonKeyAggregation, 2);
    String expectedFuncOutJson = funcOutName;
    assertEquals(expectedDistJson, PropertyServiceDependency.getJsonKey(distributionEdge));
    assertEquals(expectedAggrJson, PropertyServiceDependency.getJsonKey(aggregationEdge));
    assertEquals(expectedFuncOutJson, PropertyServiceDependency.getJsonKey(funcOutEdge));
    assertEquals(function, func2.getParent());
    assertEquals(distributedData, funcIn2.getParent());
    assertEquals(functionResult, funcOut2.getParent());

    // test the reverse transformation

    // run the tests
    // enactable not finished
    
    
    
    testedAggregation.modifyEnactmentGraph(testInput, aggregation);
    
    assertEquals(14, testInput.getVertexCount());
    assertEquals(17, testInput.getEdgeCount());
    // test the reverse operation when enactable finished
    PropertyServiceFunction.setInput(aggregation, new JsonObject());
    testedAggregation.modifyEnactmentGraph(testInput, aggregation);
    
    //EnactmentGraphViewer.view(testInput);
    assertEquals(8, testInput.getVertexCount());
    assertEquals(7, testInput.getEdgeCount());
  }
}
