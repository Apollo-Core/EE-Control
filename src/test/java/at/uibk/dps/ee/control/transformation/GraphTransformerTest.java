package at.uibk.dps.ee.control.transformation;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import at.uibk.dps.ee.model.constants.ConstantsEEModel;
import at.uibk.dps.ee.model.properties.PropertyServiceFunction;
import at.uibk.dps.ee.model.properties.PropertyServiceFunctionDataFlowCollections;
import at.uibk.dps.ee.model.properties.PropertyServiceFunctionUtilityWhile;
import at.uibk.dps.ee.model.properties.PropertyServiceFunctionDataFlowCollections.OperationType;
import net.sf.opendse.model.Task;

class GraphTransformerTest {

  GraphTransformer tested;

  @Test
  void testWhileCollapse() {
    Task whileTask =
        PropertyServiceFunctionUtilityWhile.createWhileEndTask(new Task("t1"), new Task("t2"));
    JsonObject output = new JsonObject();
    output.add(ConstantsEEModel.JsonKeyWhileDecision, new JsonPrimitive(false));
    PropertyServiceFunction.setOutput(whileTask, output);
    GraphTransform result = tested.getTransformOperation(whileTask);
    assertTrue(result instanceof GraphTransformWhileCollapse);
  }

  @Test
  void testWhileContinue() {
    Task whileTask =
        PropertyServiceFunctionUtilityWhile.createWhileEndTask(new Task("t1"), new Task("t2"));
    JsonObject output = new JsonObject();
    output.add(ConstantsEEModel.JsonKeyWhileDecision, new JsonPrimitive(true));
    PropertyServiceFunction.setOutput(whileTask, output);
    GraphTransform result = tested.getTransformOperation(whileTask);
    assertTrue(result instanceof GraphTransformWhile);
  }

  @Test
  void testDistribution() {
    Task dist = PropertyServiceFunctionDataFlowCollections.createCollectionDataFlowTask("task",
        OperationType.Distribution, "dist");
    GraphTransform result = tested.getTransformOperation(dist);
    assertTrue(result instanceof GraphTransformDistribution);
  }

  @Test
  void testAggregation() {
    Task aggro = PropertyServiceFunctionDataFlowCollections.createCollectionDataFlowTask("task",
        OperationType.Aggregation, "aggro");
    GraphTransform result = tested.getTransformOperation(aggro);
    assertTrue(result instanceof GraphTransformAggregation);
  }

  @BeforeEach
  void setup() {
    tested = new GraphTransformer();
  }
}
