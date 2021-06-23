package at.uibk.dps.ee.control.verticles;

/**
 * Static container for the constants used to communicate over the event bus.
 * 
 * @author Fedor Smirnov
 */
public class ConstantsEventBus {


  // Event bus addresses

  // requests that the schedule of a task is reset, has task id as message body
  public static final String addressResetScheduleTask = "RESET_SCHEDULE_TASK";


  // worker addresses

  // normal procedures

  // cast when the task data becomes available so that the task can be scheduled
  public static final String addressTaskSchedulable = "TASK_SCHEDULABLE";

  // cast when the task has been scheduled and can be executed, has task id as
  // message body
  public static final String addressTaskLaunchable = "TASK_LAUNCHABLE";

  // cast when the data of a data node is available, has data node id as message
  // body
  public static final String addressDataAvailable = "DATA_AVAILABLE";

  // cast when the execution of a function node is finished, has function node as
  // message body
  public static final String addressEnactmentFinished = "ENACTMENT_FINISHED";


  // failures

  public static final String addressFailureAbort = "FAILURE_ABORT";

  
  // placeholder for handlers not using certain addresses
  public static final String addressPlaceholder = "404";
  
  
  /**
   * No constructor.
   */
  private ConstantsEventBus() {}


}
