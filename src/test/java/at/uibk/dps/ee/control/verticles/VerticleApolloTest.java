package at.uibk.dps.ee.control.verticles;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import at.uibk.dps.ee.model.graph.EnactmentGraph;
import at.uibk.dps.ee.model.graph.EnactmentGraphProvider;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import net.sf.opendse.model.Task;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.never;

public class VerticleApolloTest {

  protected class VerticleMock extends VerticleApollo {

    public VerticleMock(String triggerAddress, String successAddress, String failureAddress,
        EnactmentGraphProvider eProvider) {
      super(triggerAddress, successAddress, failureAddress, eProvider);
    }

    @Override
    protected void work(Task triggeringTask) throws WorkerException {
      if (triggeringTask.equals(task1)) {
        // do nothing
      } else {
        throw new WorkerException("message");
      }
    }

    protected void setVertx(Vertx vertx) {
      this.vertx = vertx;
    }
  }

  String triggerAddress = "trigger";
  String successAddress = "success";
  String failureAddress = "failure";

  EventBus eBus;

  VerticleMock tested;

  Task task1;
  Task task2;

  Message<String> task1Message;
  Message<String> task2Message;

  @SuppressWarnings("unchecked")
  @BeforeEach
  public void setupVerticle() {
    task1 = new Task("task1");
    task2 = new Task("task2");

    task1Message = mock(Message.class);
    task2Message = mock(Message.class);
    when(task1Message.body()).thenReturn("task1");
    when(task2Message.body()).thenReturn("task2");

    EnactmentGraph eGraph = new EnactmentGraph();
    eGraph.addVertex(task1);
    eGraph.addVertex(task2);
    EnactmentGraphProvider eProv = mock(EnactmentGraphProvider.class);
    when(eProv.getEnactmentGraph()).thenReturn(eGraph);
    tested = new VerticleMock(triggerAddress, successAddress, failureAddress, eProv);

    Vertx vertX = mock(Vertx.class);
    eBus = mock(EventBus.class);
    when(vertX.eventBus()).thenReturn(eBus);
    tested.setVertx(vertX);
  }

  /**
   * Test that we process the queue tasks when resuming.
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testResume() {
    VerticleMock spy = spy(tested);
    spy.paused = true;
    spy.queue.add(task1);
    spy.resumeHandler(mock(Message.class));
    assertFalse(spy.paused);
    assertTrue(spy.queue.isEmpty());
    verify(spy).processTask(task1);
  }
  
  /**
   * Test that we don't queue if not paused.
   */
  @Test
  public void testTriggerNoPause() {
    VerticleMock spy = spy(tested);
    spy.processTaskTrigger(task1Message);
    assertTrue(spy.queue.isEmpty());
    verify(spy).processTask(task1);
  }
  
  /**
   * Test the publishing of the failure message.
   */
  @Test
  public void testProcessException() {
    tested.processTask(task2);
    verify(eBus).publish(failureAddress, "message");
  }

  /**
   * Tests the normal task processing
   */
  @Test
  public void testProcessTask() {
    VerticleMock spy = spy(tested);
    spy.processTask(task1);
    try {
      verify(spy).work(task1);
    } catch (WorkerException e) {
      fail("work exception");
    }
  }

  /**
   * Tests that tasks are queued when verticle is paused.
   */
  @Test
  public void testQueuing() {
    VerticleMock spy = spy(tested);
    spy.paused = true;
    spy.processTaskTrigger(task1Message);
    spy.processTaskTrigger(task2Message);
    verify(spy, never()).processTask(task1);
    verify(spy, never()).processTask(task2);
    assertEquals(2, spy.queue.size());
    assertTrue(spy.queue.contains(task1));
    assertTrue(spy.queue.contains(task2));
  }

  /**
   * Test the task pausing.
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testPausing() {
    assertFalse(tested.paused);
    tested.pauseHandler(mock(Message.class));
    assertTrue(tested.paused);
  }

  /**
   * Test the address names.
   */
  @Test
  public void testAddresses() {
    assertEquals(triggerAddress, tested.triggerAddress);
    assertEquals(successAddress, tested.successAddress);
    assertEquals(failureAddress, tested.failureAddress);
  }
}
