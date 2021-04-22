package at.uibk.dps.ee.control.management;

import static org.junit.Assert.*;

import org.junit.Test;

import net.sf.opendse.model.Task;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class EnactmentQueuesTest {

  protected static final int queueDurationTime = 100;
  
  protected class PutRunnable implements Callable<Boolean> {

    protected final Task task;
    protected final int waitTimeMillis;
    protected final EnactmentQueues state;

    protected PutRunnable(Task task, int waitTime, EnactmentQueues state) {
      this.task = task;
      this.waitTimeMillis = waitTime;
      this.state = state;
    }

    @Override
    public Boolean call() throws Exception {
      try {
        Thread.sleep(waitTimeMillis);
      } catch (InterruptedException e) {
        fail();
      }
      state.putLaunchableTask(task);
      return true;
    }
  }

  protected class TakeRunnable implements Callable<Task> {
    protected final EnactmentQueues state;

    protected TakeRunnable(EnactmentQueues state) {
      this.state = state;
    }

    @Override
    public Task call() throws Exception {
      return state.takeLaunchableTask();
    }
  }

  @Test
  public void testReadyTaskQueueTake() {
    EnactmentQueues tested = new EnactmentQueues();
    assertTrue(tested.launchableTasks.isEmpty());
    ExecutorService executor = Executors.newCachedThreadPool();
    Task task1 = new Task("task1");
    Task task2 = new Task("task2");

    PutRunnable run1 = new PutRunnable(task1, 0, tested);
    PutRunnable run2 = new PutRunnable(task2, 500, tested);
    TakeRunnable take = new TakeRunnable(tested);

    Instant start = Instant.now();
    executor.submit(run1);
    executor.submit(run2);
    Instant finishedSUbmission = Instant.now();
    assertTrue(Duration.between(start, finishedSUbmission).toMillis() < 30);
    Future<Task> f1 = executor.submit(take);
    try {
      Thread.sleep(30);
    } catch (InterruptedException e1) {
      fail();
    }
    Future<Task> f2 = executor.submit(take);
    try {
      Task result1 = f1.get();
      Instant finish1 = Instant.now();
      Task result2 = f2.get();
      Instant finish2 = Instant.now();
      assertEquals(result1, task1);
      assertEquals(result2, task2);

      long duration1 = Duration.between(start, finish1).toMillis();
      long duration2 = Duration.between(start, finish2).toMillis();
      assertTrue(duration1 < queueDurationTime);
      assertTrue(duration2 >= 500);

    } catch (InterruptedException | ExecutionException e) {
      fail();
    }
  }

  @Test
  public void testReadyTaskQueuePut() {
    EnactmentQueues tested = new EnactmentQueues();
    assertTrue(tested.launchableTasks.isEmpty());
    ExecutorService executor = Executors.newCachedThreadPool();
    Task task1 = new Task("task1");
    Task task2 = new Task("task2");
    PutRunnable run1 = new PutRunnable(task1, 20, tested);
    PutRunnable run2 = new PutRunnable(task2, 20, tested);
    Instant start = Instant.now();

    Future<Boolean> future1 = executor.submit(run1);
    Future<Boolean> future2 = executor.submit(run2);

    try {
      future1.get();
      future2.get();
    } catch (InterruptedException | ExecutionException e) {
      fail();
    }

    Instant end = Instant.now();
    long time = Duration.between(start, end).toMillis();
    assertTrue(time < queueDurationTime);
    assertTrue(tested.launchableTasks.size() == 2);
    assertTrue(tested.launchableTasks.contains(task1));
    assertTrue(tested.launchableTasks.contains(task2));
  }

  @Test
  public void testPutTakeTransformTask() {
    EnactmentQueues tested = new EnactmentQueues();
    assertTrue(tested.awaitingTransform.isEmpty());
    Task transformTask = new Task("transform");
    tested.putTransformTask(transformTask);
    assertFalse(tested.awaitingTransform.isEmpty());
    try {
      assertEquals(transformTask, tested.takeTransformTask());
    } catch (InterruptedException e) {
      fail();
    }
  }
}
