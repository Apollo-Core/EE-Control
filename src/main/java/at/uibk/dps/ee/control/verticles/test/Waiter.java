package at.uibk.dps.ee.control.verticles.test;

import io.vertx.core.Future;

/**
 * Thingy which waits for a long operation.
 * 
 * @author Fedor Smirnov
 *
 */
public class Waiter {

  
  
  public Waiter(Worker worker) {
    Future<String> waitResult = worker.getFutureResult();
    waitResult.onComplete(asyncRes ->{
      System.out.println(asyncRes.result());
    });
  }
}
