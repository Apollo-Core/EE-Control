package at.uibk.dps.ee.control.verticles;

/**
 * Static container for the constants used to communicate over the event bus.
 * 
 * @author Fedor Smirnov
 */
public final class ConstantsVertX {

  // Apollo Verticle logging ID

  // still have to figure our more sensible settings for these
  public static final String typeId = "Apollo Instance";
  public static final String functionId = "Apollo WF Verticle";
  public static final String enactmentMode = "enactment mode";
  public static final String implId = "implId";

  // Locks
  public static final String transformTransmitLock = "transformTransmit";
  public static final String waitingListLock = "waitingList";

  // Event bus messages
  public static final String messagePause = "pause";
  public static final String messageResume = "resume";
  
  public static final String messageInitDelay = "init delay";

  // Event bus addresses

  // Control Addresses
  public static final String addressControlPause = "PAUSE_ADDRESS";
  public static final String addressControlResume = "RESUME_ADDRESS";
  
  // Init Addresses
  public static final String addressInitDelay = "INIT_DELAY_ADDRESS";

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

  // cast when a task results in a graph transformation
  public static final String addressRequiredTransformation = "TRANSFORMATION_REQUIRED";

  // cast when the result of the wf processing is available. Has the result JSON
  // as message string
  public static final String addressWorkflowResultAvailable = "WORKFLOW_RESULT";

  // cast when a task on a resource finished execution. Has the resource ID
  // as message string
  public static final String addressResourceFreed = "RES_FREED";


  // failures

  public static final String addressFailureAbort = "FAILURE_ABORT";


  /**
   * No constructor.
   */
  private ConstantsVertX() {}


}
