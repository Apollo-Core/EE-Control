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
  
  // cast when the data of a data node is available, has data node id as message body
  public static final String addressDataAvailable = "DATA_AVAILABLE";
  
  // cast when the execution of a function node is finished, has function node as message body
  public static final String addressEnactmentFinished = "ENACTMENT_FINISHED";
  
  
  public static final String addressFailureAbort = "FAILURE_ABORT";
  
  /**
   * No constructor.
   */
  private ConstantsEventBus() {
  }
  
  
}
