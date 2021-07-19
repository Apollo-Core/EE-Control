package at.uibk.dps.ee.control.verticles;

/**
 * Exception thrown in cases where the verticle workers encounter problems.
 * 
 * @author Fedor Smirnov
 */
public class WorkerException extends Exception{

  private static final long serialVersionUID = 1L;
  
  /**
   * Standard constructor.
   * 
   * @param message the message of the exception
   */
  public WorkerException(String message) {
    super(message);
  }
}
