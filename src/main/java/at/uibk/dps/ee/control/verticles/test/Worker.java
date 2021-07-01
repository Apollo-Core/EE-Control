package at.uibk.dps.ee.control.verticles.test;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;

/**
 * Thingy which performs a long operation.
 * 
 * @author Fedor Smirnov
 */
public class Worker {

  protected final Promise<String> stringPromise = Promise.promise();
  
  public Worker(Vertx vertx) {
    vertx.setTimer(2000, timerId ->{
      stringPromise.complete("Timer fired");
    });
  }
  
  public Future<String> getFutureResult(){
    return stringPromise.future();
  }
  
}
