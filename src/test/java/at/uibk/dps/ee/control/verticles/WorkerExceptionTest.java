package at.uibk.dps.ee.control.verticles;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class WorkerExceptionTest {

  @Test
  void test() {
    String myMessage = "message";
    WorkerException tested = new WorkerException(myMessage);
    assertEquals(myMessage, tested.getMessage());
  }

}
