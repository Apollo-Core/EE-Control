package at.uibk.dps.ee.control.verticles.enactment;

import com.google.inject.Inject;
import at.uibk.dps.ee.control.enactment.PostEnactment;
import at.uibk.dps.ee.control.verticles.ConstantsVertX;
import at.uibk.dps.ee.control.verticles.VerticleApollo;
import at.uibk.dps.ee.control.verticles.WorkerException;
import at.uibk.dps.ee.core.function.EnactmentFunction;
import at.uibk.dps.ee.model.graph.EnactmentGraphProvider;
import at.uibk.dps.ee.model.properties.PropertyServiceFunction;
import at.uibk.dps.sc.core.ScheduleModel;
import at.uibk.dps.sc.core.interpreter.ScheduleInterpreter;
import net.sf.opendse.model.Task;

public class WorkerEnactment extends VerticleApollo {

  protected final PostEnactment postEnactment;
  protected final ScheduleModel scheduleModel;
  protected final ScheduleInterpreter interpreter;

  @Inject
  public WorkerEnactment(EnactmentGraphProvider eGraphProvider, PostEnactment postEnactment,
      ScheduleModel scheduleModel, ScheduleInterpreter interpreter) {
    super(ConstantsVertX.addressTaskLaunchable, ConstantsVertX.addressEnactmentFinished,
        ConstantsVertX.addressFailureAbort, eGraphProvider);
    this.postEnactment = postEnactment;
    this.scheduleModel = scheduleModel;
    this.interpreter = interpreter;
  }

  @Override
  protected void work(Task functionNode) throws WorkerException {
    EnactmentFunction function =
        interpreter.interpretSchedule(functionNode, scheduleModel.getTaskSchedule(functionNode));
    System.out.println("before");
    function.processInput(PropertyServiceFunction.getInput(functionNode)).onComplete(jsonResult -> {
      System.out.println("after enactment");
      PropertyServiceFunction.setOutput(functionNode, jsonResult.result());
      postEnactment.postEnactmentTreatment(functionNode, this.vertx.eventBus());
    });
    System.out.println("after");
  }
}
