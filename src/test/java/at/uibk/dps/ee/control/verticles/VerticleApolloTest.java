package at.uibk.dps.ee.control.verticles;

import java.util.HashSet;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import at.uibk.dps.ee.model.graph.EnactmentGraph;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import net.sf.opendse.model.Element;
import net.sf.opendse.model.Task;

@RunWith(VertxUnitRunner.class)
public class VerticleApolloTest {

  protected class Trigger extends HandlerApollo<Task> {

    public Trigger(String triggerAddress, String successAddress, String failureAddress,
        EventBus eBus, EnactmentGraph graph) {
      super(triggerAddress, successAddress, failureAddress, eBus, graph);
    }

    @Override
    protected Task readMessage(String message) {
      return new Task(message);
    }

    @Override
    protected void work(Task graphElement) throws WorkerException {
      eBus.publish(successAddress, Response.expectedMessage);
    }
  }

  protected class Response extends HandlerApollo<Task> {

    protected final TestContext context;
    protected static final String expectedMessage = "expected";

    public Response(String triggerAddress, String successAddress, String failureAddress,
        EventBus eBus, TestContext context, EnactmentGraph graph) {
      super(triggerAddress, successAddress, failureAddress, eBus, graph);
      this.context = context;
    }

    @Override
    protected Task readMessage(String message) {
      return new Task(message);
    }

    @Override
    protected void work(Task graphElement) throws WorkerException {
      context.assertEquals(expectedMessage, graphElement.getId());
      eBus.publish(successAddress, graphElement.getId());
    }
  }

  protected class FailingHandler extends HandlerApollo<Task> {

    protected static final String expectedExcMessage = "expected";

    public FailingHandler(String triggerAddress, String successAddress, String failureAddress,
        EventBus eBus, EnactmentGraph graph) {
      super(triggerAddress, successAddress, failureAddress, eBus, graph);
    }

    @Override
    protected Task readMessage(String message) {
      return new Task(message);
    }

    @Override
    protected void work(Task graphElement) throws WorkerException {
      throw new WorkerException(expectedExcMessage);
    }
  }

  @Test
  public void testCorrect(TestContext context) {
    Async async = context.async();

    Vertx vertx = Vertx.vertx();
    EventBus eBus = vertx.eventBus();

    String triggerTrigger = "triggerTrigger";
    String triggerSuccess = "triggerSuccess";
    String responseTrigger = triggerSuccess;
    String responseSuccess = "responseSuccess";

    eBus.consumer(responseSuccess, res -> {
      async.complete();
    });

    Trigger trigger =
        new Trigger(triggerTrigger, triggerSuccess, "none", eBus, new EnactmentGraph());
    Response response =
        new Response(responseTrigger, responseSuccess, "none", eBus, context, new EnactmentGraph());

    Set<HandlerApollo<? extends Element>> handlers = new HashSet<>();
    handlers.add(trigger);
    handlers.add(response);

    VerticleApollo tested = new VerticleApollo(handlers);

    vertx.deployVerticle(tested).onComplete(res -> {
      eBus.publish(triggerTrigger, Response.expectedMessage);
    });
  }

  @Test
  public void testFailure(TestContext context) {
    Async async = context.async();

    Vertx vertx = Vertx.vertx();
    EventBus eBus = vertx.eventBus();

    String failureTrigger = "failureTrigger";
    String failureSuccess = "none";
    String failureFailure = "failureFailure";

    eBus.consumer(failureFailure, message -> {
      String messageString = message.body().toString();
      context.assertEquals(FailingHandler.expectedExcMessage, messageString);
      async.complete();
    });

    FailingHandler failing = new FailingHandler(failureTrigger, failureSuccess, failureFailure,
        eBus, new EnactmentGraph());

    Set<HandlerApollo<? extends Element>> handlers = new HashSet<>();
    handlers.add(failing);

    VerticleApollo tested = new VerticleApollo(handlers);

    vertx.deployVerticle(tested).onComplete(res -> {
      eBus.publish(failureTrigger, "message");
    });
  }
}
