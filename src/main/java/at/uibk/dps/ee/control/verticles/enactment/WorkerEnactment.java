package at.uibk.dps.ee.control.verticles.enactment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.gson.JsonObject;
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
import io.vertx.core.AsyncResult;
import net.sf.opendse.model.Task;

public class WorkerEnactment extends VerticleApollo {

  protected final PostEnactment postEnactment;
  protected final ScheduleModel scheduleModel;
  protected final ScheduleInterpreter interpreter;

  protected final Logger logger = LoggerFactory.getLogger(WorkerEnactment.class);
  
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
    function.processInput(PropertyServiceFunction.getInput(functionNode))
        .onComplete(jsonResult -> processResult(jsonResult, functionNode));
  }

  /**
   * Process the result by calling the postEnactment and annotating the task
   * output.
   * 
   * @param result the operation result
   * @param functionNode the function node
   */
  protected void processResult(AsyncResult<JsonObject> result, Task functionNode) {
    logger.debug("Enactment finished for task {}.", functionNode.getId());
    PropertyServiceFunction.setOutput(functionNode, result.result());
    postEnactment.postEnactmentTreatment(functionNode, this.vertx.eventBus());
  }
}
