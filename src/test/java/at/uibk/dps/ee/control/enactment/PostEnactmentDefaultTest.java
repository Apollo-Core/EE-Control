package at.uibk.dps.ee.control.enactment;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import at.uibk.dps.ee.control.verticles.ConstantsVertX;
import at.uibk.dps.ee.model.constants.ConstantsEEModel;
import at.uibk.dps.ee.model.properties.PropertyServiceFunction;
import at.uibk.dps.ee.model.properties.PropertyServiceFunction.UsageType;
import at.uibk.dps.ee.model.properties.PropertyServiceFunctionDataFlowCollections;
import at.uibk.dps.ee.model.properties.PropertyServiceFunctionDataFlowCollections.OperationType;
import at.uibk.dps.ee.model.properties.PropertyServiceFunctionUtilityWhile;
import io.vertx.core.eventbus.EventBus;
import net.sf.opendse.model.Task;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PostEnactmentDefaultTest {

  Task requiresTrans;
  Task requiresNoTrans;

  EventBus eBus;

  PostEnactmentDefault tested;

  /**
   * Tests the post enactment with transformation.
   */
  @Test
  void testPostEnactmentTransform() {
    tested.postEnactmentTreatment(requiresTrans, eBus);
    verify(eBus).send(ConstantsVertX.addressRequiredTransformation, requiresTrans.getId());
  }

  /**
   * Tests the enactment without transformation.
   */
  @Test
  void testPostEnactmentFinish() {
    tested.postEnactmentTreatment(requiresNoTrans, eBus);
    verify(eBus).send(ConstantsVertX.addressEnactmentFinished, requiresNoTrans.getId());
  }

  /**
   * Tests the transformation requirement check.
   */
  @Test
  void testRequiresTransformation() {
    assertTrue(tested.requiresTransformation(requiresTrans));
    assertFalse(tested.requiresTransformation(requiresNoTrans));
  }

  /**
   * Tests that the while transform check works
   */
  @Test
  void testReqWhile() {
    Task task = new Task("not while");
    PropertyServiceFunction.setUsageType(UsageType.DataFlow, task);
    assertFalse(tested.whileRequiresTransformation(task));

    Task whileCounter = new Task("whileCounter");
    Task whileStart = new Task("whileStart");
    Task whileTask =
        PropertyServiceFunctionUtilityWhile.createWhileEndTask(whileStart, whileCounter);
    JsonObject jsonObj = new JsonObject();
    PropertyServiceFunction.setOutput(whileTask, jsonObj);
    assertThrows(IllegalArgumentException.class, () -> {
      tested.whileRequiresTransformation(whileTask);
    });
    jsonObj.add(ConstantsEEModel.JsonKeyWhileDecision, new JsonPrimitive(false));
    assertFalse(tested.whileRequiresTransformation(whileTask));
    jsonObj.add(ConstantsEEModel.JsonKeyWhileDecision, new JsonPrimitive(true));
    assertTrue(tested.whileRequiresTransformation(whileTask));
  }

  @BeforeEach
  void setup() {
    requiresNoTrans = new Task("task");
    PropertyServiceFunction.setUsageType(UsageType.User, requiresNoTrans);
    requiresTrans = PropertyServiceFunctionDataFlowCollections.createCollectionDataFlowTask("task2",
        OperationType.Aggregation, "scope");
    tested = new PostEnactmentDefault();
    eBus = mock(EventBus.class);
  }
}
