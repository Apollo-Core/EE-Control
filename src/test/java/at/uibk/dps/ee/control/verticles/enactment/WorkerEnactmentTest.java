package at.uibk.dps.ee.control.verticles.enactment;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.google.gson.JsonObject;
import at.uibk.dps.ee.control.verticles.WorkerException;
import at.uibk.dps.ee.core.function.EnactmentFunction;
import at.uibk.dps.ee.model.graph.EnactmentGraphProvider;
import at.uibk.dps.ee.model.properties.PropertyServiceFunction;
import at.uibk.dps.sc.core.ScheduleModel;
import at.uibk.dps.sc.core.interpreter.ScheduleInterpreter;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import net.sf.opendse.model.Mapping;
import net.sf.opendse.model.Resource;
import net.sf.opendse.model.Task;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import java.util.HashSet;
import java.util.Set;

class WorkerEnactmentTest {

  protected class MockWorker extends WorkerEnactment{

    public MockWorker(EnactmentGraphProvider eGraphProvider, PostEnactment postEnactment,
        ScheduleModel scheduleModel, ScheduleInterpreter interpreter) {
      super(eGraphProvider, postEnactment, scheduleModel, interpreter);
    }
    
    public void setVertx(Vertx vertx) {
      this.vertx = vertx;
    }
    
  }
  
  PostEnactment postEnactment;
  ScheduleModel scheduleModel;
  ScheduleInterpreter scheduleInterpreter;
  
  Task functionTask;
  Set<Mapping<Task, Resource>> schedule;
  EnactmentFunction function;
  
  MockWorker tested;
  EventBus eBus;
  
  JsonObject result;
  JsonObject input;
  
  /**
   * Test the result handler.
   */
  @Test
  void testProcessResult() {
    @SuppressWarnings("unchecked")
    AsyncResult<JsonObject> aRes = mock(AsyncResult.class);
    when(aRes.result()).thenReturn(result);
    
    tested.processResult(aRes, functionTask);
    assertTrue(PropertyServiceFunction.isOutputSet(functionTask));
    assertEquals(result, PropertyServiceFunction.getOutput(functionTask));
    verify(postEnactment).postEnactmentTreatment(functionTask, eBus);
  }
  
  /**
   * Test the normal execution of the work method.
   */
  @Test
  void testWork() {
    try {
      tested.work(functionTask);
      verify(scheduleInterpreter).interpretSchedule(functionTask, schedule);
      verify(function).processInput(input);
    } catch (WorkerException e) {
      fail();
    }
  }

  @BeforeEach
  void setup() {
    postEnactment = mock(PostEnactment.class);
    functionTask = new Task("task");
    schedule = new HashSet<>();
    scheduleModel = mock(ScheduleModel.class);
    when(scheduleModel.getTaskSchedule(functionTask)).thenReturn(schedule);
    function = mock(EnactmentFunction.class);
    scheduleInterpreter = mock(ScheduleInterpreter.class);
    when(scheduleInterpreter.interpretSchedule(functionTask, schedule)).thenReturn(function);
    eBus = mock(EventBus.class);
    Vertx vertx = mock(Vertx.class);
    when(vertx.eventBus()).thenReturn(eBus);
    tested = new MockWorker(mock(EnactmentGraphProvider.class), postEnactment, scheduleModel, scheduleInterpreter);
    tested.setVertx(vertx);
    input = new JsonObject();
    PropertyServiceFunction.setInput(functionTask, input);
    result = new JsonObject();
    when(function.processInput(input)).thenReturn(Future.succeededFuture(result));
  }
  
}
