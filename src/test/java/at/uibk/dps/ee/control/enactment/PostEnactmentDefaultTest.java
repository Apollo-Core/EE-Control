package at.uibk.dps.ee.control.enactment;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import at.uibk.dps.ee.control.verticles.ConstantsVertX;
import at.uibk.dps.ee.guice.starter.VertxProvider;
import at.uibk.dps.ee.model.properties.PropertyServiceFunction;
import at.uibk.dps.ee.model.properties.PropertyServiceFunction.UsageType;
import at.uibk.dps.ee.model.properties.PropertyServiceFunctionDataFlowCollections;
import at.uibk.dps.ee.model.properties.PropertyServiceFunctionDataFlowCollections.OperationType;
import at.uibk.dps.sc.core.ScheduleModel;
import io.vertx.core.AsyncResult;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.shareddata.Lock;
import net.sf.opendse.model.Task;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

class PostEnactmentDefaultTest {

  protected static Vertx vertx;

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
  @SuppressWarnings({"unchecked", "rawtypes"})
  @Test
  void testAsyncLockHandler() {
    AsyncResult asynRes = mock(AsyncResult.class);
    Lock mockLock = mock(Lock.class);
    when(asynRes.result()).thenReturn(mockLock);
    when(asynRes.succeeded()).thenReturn(true);
    tested.lockResHandler(asynRes, requiresNoTrans, eBus);
    verify(eBus).send(ConstantsVertX.addressEnactmentFinished, requiresNoTrans.getId());
    verify(mockLock).release();
  }

  /**
   * Tests the transformation requirement check.
   */
  @Test
  void testRequiresTransformation() {
    assertTrue(tested.requiresTransformation(requiresTrans));
    assertFalse(tested.requiresTransformation(requiresNoTrans));
  }

  @BeforeAll
  static void init() {
    vertx = Vertx.vertx();
  }

  @BeforeEach
  void setup() {
    requiresNoTrans = new Task("task");
    PropertyServiceFunction.setUsageType(UsageType.User, requiresNoTrans);
    requiresTrans = PropertyServiceFunctionDataFlowCollections.createCollectionDataFlowTask("task2",
        OperationType.Aggregation, "scope");
    ScheduleModel mockSchedule = mock(ScheduleModel.class);
    tested = new PostEnactmentDefault(mockSchedule, new VertxProvider(vertx));
    eBus = mock(EventBus.class);
  }
}
