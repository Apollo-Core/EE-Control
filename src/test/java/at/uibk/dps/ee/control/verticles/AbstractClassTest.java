package at.uibk.dps.ee.control.verticles;

import static org.junit.Assert.*;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import net.sf.opendse.model.Task;

public class AbstractClassTest {
  
  protected class TestSupervisor extends Supervisor<Task>{
    public TestSupervisor(VerticleFactory<Task> factory, EventBus eBus, String triggerAddress,
        String successAddress) {
      super(factory, eBus, triggerAddress, successAddress);
    }
  }
  
  protected class TestWorker extends Worker<Task>{

    public TestWorker(Task triggerElement) {
      super(triggerElement);
    }

    @Override
    public boolean isBlocking() {
      return false;
    }

    @Override
    protected void executeElementOperation() throws Exception {
    }
  }
  
  protected class TestFactory extends VerticleFactory<Task>{

    @Override
    protected Set<Worker<Task>> getWorkers(Task graphElement) {
      Set<Worker<Task>> result = new HashSet<>();
      result.add(new TestWorker(graphElement));
      return result;
    }

    @Override
    protected Task getElementFromMessage(String message) {
      return new Task(message);
    }
    
  }
  
  protected class ResultListener extends AbstractVerticle{
    protected boolean success = false;
    protected final EventBus eBus;
    protected final String successAddress;
    
    public ResultListener(EventBus eBus, String successAddress) {
      this.eBus = eBus;
      this.successAddress = successAddress;
    }
    
    @Override
    public void start() throws Exception {
      super.start();
      eBus.consumer(successAddress, message -> {
        assertEquals(message, "task1");
        success = true;
      });
    }
    
  }

  @Test
  public void test() throws InterruptedException {
    
    Vertx vertx = Vertx.vertx();
    EventBus eBus = vertx.eventBus();
    
    String triggerAddress = "trigger";
    String successAddress = "success";
    
    ResultListener listener = new ResultListener(eBus, successAddress);
    TestFactory factory = new TestFactory();
    TestSupervisor supervisor = new TestSupervisor(factory, eBus, triggerAddress, successAddress);
    vertx.deployVerticle(listener).onComplete( res ->{
      System.out.println("listener deployed");
      vertx.deployVerticle(supervisor).onComplete(res2 -> {
        System.out.println("supervisor deployed");
        eBus.publish(triggerAddress, "task1");
      });
    });
    
    //eBus.publish(successAddress, "task1");
    
    System.out.println("finishing the rest of the test");
    
    TimeUnit.MILLISECONDS.sleep(500);
    
    System.out.println("waiting done");
    assertTrue(listener.success);
    
  }

}
