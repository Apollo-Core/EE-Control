package at.uibk.dps.ee.control.verticles.enactment;

import com.google.inject.Inject;
import at.uibk.dps.ee.control.enactment.PostEnactment;
import at.uibk.dps.ee.control.verticles.ConstantsEventBus;
import at.uibk.dps.ee.control.verticles.HandlerApollo;
import at.uibk.dps.ee.control.verticles.WorkerException;
import at.uibk.dps.ee.core.function.EnactmentFunction;
import at.uibk.dps.ee.guice.starter.VertxProvider;
import at.uibk.dps.ee.model.graph.EnactmentGraphProvider;
import at.uibk.dps.ee.model.properties.PropertyServiceFunction;
import at.uibk.dps.sc.core.ScheduleModel;
import at.uibk.dps.sc.core.interpreter.ScheduleInterpreter;
import io.vertx.core.Vertx;
import net.sf.opendse.model.Task;

public class WorkerEnactment extends HandlerApollo<Task> {

  protected final PostEnactment postEnactment;
  protected final Vertx vertx;
  protected final ScheduleModel scheduleModel;
  protected final ScheduleInterpreter interpreter;

  @Inject
  public WorkerEnactment(VertxProvider vertxProvider, EnactmentGraphProvider eGraphProvider,
      PostEnactment postEnactment, ScheduleModel scheduleModel, ScheduleInterpreter interpreter) {
    super(ConstantsEventBus.addressTaskLaunchable, ConstantsEventBus.addressEnactmentFinished,
        ConstantsEventBus.addressFailureAbort, vertxProvider.geteBus(), eGraphProvider);
    this.postEnactment = postEnactment;
    this.vertx = vertxProvider.getVertx();
    this.scheduleModel = scheduleModel;
    this.interpreter = interpreter;
  }

  @Override
  protected Task readMessage(String message) {
    return eGraph.getVertex(message);
  }

  @Override
  protected void work(Task functionNode) throws WorkerException {
    
    EnactmentFunction function = interpreter.interpretSchedule(functionNode, scheduleModel.getTaskSchedule(functionNode));
    
    function.processInput(PropertyServiceFunction.getInput(functionNode)).onComplete(jsonResult ->{
     PropertyServiceFunction.setOutput(functionNode, jsonResult.result());
     postEnactment.postEnactmentTreatment(functionNode);
    });
  }
}
