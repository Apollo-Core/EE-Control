package at.uibk.dps.ee.control.agents;

import java.util.Set;

import at.uibk.dps.ee.control.management.EnactmentQueues;
import at.uibk.dps.ee.core.function.Enactable;
import at.uibk.dps.ee.core.function.EnactmentFunction;
import at.uibk.dps.ee.model.properties.PropertyServiceFunction;
import at.uibk.dps.sc.core.ScheduleModel;
import at.uibk.dps.sc.core.interpreter.ScheduleInterpreter;
import at.uibk.dps.sc.core.scheduler.Scheduler;
import net.sf.opendse.model.Mapping;
import net.sf.opendse.model.Resource;
import net.sf.opendse.model.Task;

/**
 * The {@link AgentScheduling} is used to check whether a launchable task has
 * already been scheduled and to schedule it, if it is not yet scheduled.
 * 
 * @author Fedor Smirnov
 */
public class AgentScheduling extends AgentTask {

  protected final ScheduleModel schedule;
  protected final Scheduler scheduler;
  protected final Task functionNode;
  protected final EnactmentQueues enactmentState;
  protected final ScheduleInterpreter interpreter;

  /**
   * The default constructor.
   * 
   * @param schedule the schedule map
   * @param scheduler the scheduler
   * @param functionNode the task node to schedule
   * @param enactmentState the state of the enactment (for the access to the
   *        queues)
   * @param interpreter the interpreter
   * @param listeners the {@link AgentTaskListener}s
   */
  public AgentScheduling(final ScheduleModel schedule, final Scheduler scheduler,
      final Task functionNode, final EnactmentQueues enactmentState,
      final ScheduleInterpreter interpreter, final Set<AgentTaskListener> listeners) {
    super(listeners);
    this.schedule = schedule;
    this.scheduler = scheduler;
    this.functionNode = functionNode;
    this.enactmentState = enactmentState;
    this.interpreter = interpreter;
  }

  @Override
  public boolean actualCall() throws Exception {
    if (schedule.isScheduled(functionNode)) {
      throw new IllegalStateException("Somehow, the task is already scheduled.");
    } else {
      final Set<Mapping<Task, Resource>> taskSchedule = scheduler.scheduleTask(functionNode);
      schedule.setTaskSchedule(functionNode, taskSchedule);
      final Enactable taskEnactable = PropertyServiceFunction.getEnactable(functionNode);
      final EnactmentFunction enactmentFunction =
          interpreter.interpretSchedule(functionNode, taskSchedule);
      taskEnactable.schedule(enactmentFunction);
    }
    enactmentState.putLaunchableTask(functionNode);
    return true;
  }

  @Override
  protected String formulateExceptionMessage() {
    return ConstantsAgents.ExcMessageScheduling + functionNode.getId();
  }
}
