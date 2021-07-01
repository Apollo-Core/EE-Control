package at.uibk.dps.ee.control.verticles.test;

import io.vertx.core.Vertx;

public class Main {

  public static void main(String[] args) {
    
    Vertx vertx = Vertx.vertx();
    
    Worker worker = new Worker(vertx);
    Waiter waiter = new Waiter(worker);
    
  }
  
}
