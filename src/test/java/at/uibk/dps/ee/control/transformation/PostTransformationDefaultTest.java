package at.uibk.dps.ee.control.transformation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import at.uibk.dps.ee.control.verticles.ConstantsVertX;
import at.uibk.dps.ee.model.constants.ConstantsEEModel;
import at.uibk.dps.ee.model.properties.PropertyServiceFunction;
import at.uibk.dps.ee.model.properties.PropertyServiceFunctionDataFlowCollections;
import at.uibk.dps.ee.model.properties.PropertyServiceFunctionDataFlowCollections.OperationType;
import at.uibk.dps.ee.model.properties.PropertyServiceFunctionUtilityWhile;
import at.uibk.dps.ee.model.properties.PropertyServiceReproduction;
import io.vertx.core.eventbus.EventBus;
import net.sf.opendse.model.Task;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PostTransformationDefaultTest {

  PostTransformationDefault tested;
  EventBus eBus;

  @Test
  void testWhileBreak() {
    Task whileTask =
        PropertyServiceFunctionUtilityWhile.createWhileEndTask(new Task("t1"), new Task("t2"));
    JsonObject output = new JsonObject();
    output.add(ConstantsEEModel.JsonKeyWhileDecision, new JsonPrimitive(false));
    String originalReference = "original";
    PropertyServiceReproduction.setOriginalWhileEndReference(whileTask, originalReference);
    PropertyServiceFunction.setOutput(whileTask, output);
    tested.postTransformationTreatment(whileTask, eBus);
    verify(eBus).send(ConstantsVertX.addressEnactmentFinished, originalReference);
  }

  @Test
  void testWhileContinue() {
    Task whileTask =
        PropertyServiceFunctionUtilityWhile.createWhileEndTask(new Task("t1"), new Task("t2"));
    JsonObject output = new JsonObject();
    output.add(ConstantsEEModel.JsonKeyWhileDecision, new JsonPrimitive(true));
    PropertyServiceFunction.setOutput(whileTask, output);
    tested.postTransformationTreatment(whileTask, eBus);
    verify(eBus).send(ConstantsVertX.addressEnactmentFinished, whileTask.getId());
  }

  @Test
  void testDistributionNode() {
    Task dist = PropertyServiceFunctionDataFlowCollections.createCollectionDataFlowTask("task",
        OperationType.Distribution, "dist");
    tested.postTransformationTreatment(dist, eBus);
    verify(eBus).send(ConstantsVertX.addressEnactmentFinished, dist.getId());
  }

  @Test
  void testAggregationNode() {
    Task aggro = PropertyServiceFunctionDataFlowCollections.createCollectionDataFlowTask("task",
        OperationType.Aggregation, "aggro");
    tested.postTransformationTreatment(aggro, eBus);
    verify(eBus).send(ConstantsVertX.addressEnactmentFinished, aggro.getId());
  }

  @BeforeEach
  void setup() {
    tested = new PostTransformationDefault();
    eBus = mock(EventBus.class);
  }

}
