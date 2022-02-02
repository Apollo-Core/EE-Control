package at.uibk.dps.ee.control.enactment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import at.uibk.dps.ee.control.verticles.ConstantsVertX;
import at.uibk.dps.ee.control.verticles.VerticleApollo;
import at.uibk.dps.ee.control.verticles.WorkerException;
import at.uibk.dps.ee.core.function.EnactmentFunction;
import at.uibk.dps.ee.model.graph.SpecificationProvider;
import at.uibk.dps.ee.model.properties.PropertyServiceFunction;
import at.uibk.dps.sc.core.ScheduleModel;
import at.uibk.dps.sc.core.interpreter.ScheduleInterpreter;
import net.sf.opendse.model.Task;

/**
 * The worker responsible for the function enactment, i.e., the triggerring of
 * the underlying serverless function interface.
 * 
 * @author Fedor Smirnov
 */
public class WorkerEnactment extends VerticleApollo {

  protected final PostEnactment postEnactment;
  protected final ScheduleModel scheduleModel;
  protected final ScheduleInterpreter interpreter;

  protected final Logger logger = LoggerFactory.getLogger(WorkerEnactment.class);

  /**
   * The injection constructor.
   * 
   * @param specProvider provides the sepc
   * @param postEnactment defines what to do after the enactment is finished
   * @param scheduleModel the schedule (maps tasks to mapping sets)
   * @param interpreter the interpreter (maps mapping sets to functions)
   */
  @Inject
  public WorkerEnactment(final SpecificationProvider specProvider,
      final PostEnactment postEnactment, final ScheduleModel scheduleModel,
      final ScheduleInterpreter interpreter) {
    super(ConstantsVertX.addressTaskLaunchable, ConstantsVertX.addressEnactmentFinished,
        ConstantsVertX.addressFailureAbort, specProvider);
    this.postEnactment = postEnactment;
    this.scheduleModel = scheduleModel;
    this.interpreter = interpreter;
  }

  @Override
  protected void work(final Task functionNode) throws WorkerException {
    logger.debug("Enacting Task {}", functionNode.getId());
    final EnactmentFunction function =
        interpreter.interpretSchedule(functionNode, scheduleModel.getTaskSchedule(functionNode));
    function.processInput(PropertyServiceFunction.getInput(functionNode)).onComplete(asyncRes -> {
      if (asyncRes.succeeded()) {
        processResult(asyncRes.result(), functionNode);
      } else {
        logger.error("Exception during enactment {}", asyncRes.cause());
        failureHandler(asyncRes.cause());
      }
    });
  }

  /**
   * Process the result by calling the postEnactment and annotating the task
   * output.
   * 
   * @param result the operation result
   * @param functionNode the function node
   */
  protected void processResult(final JsonObject result, final Task functionNode) {
    logger.debug("Enactment finished for task {}.", functionNode.getId());
    PropertyServiceFunction.setOutput(functionNode, result);
    postEnactment.postEnactmentTreatment(functionNode, this.vertx.eventBus());
  }
}
