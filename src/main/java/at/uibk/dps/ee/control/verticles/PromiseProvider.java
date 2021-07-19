package at.uibk.dps.ee.control.verticles;

import com.google.gson.JsonObject;
import com.google.inject.Singleton;
import io.vertx.core.Promise;

/**
 * Class to enable injecting promises into objects.
 * 
 * @author Fedor Smirnov
 *
 */
@Singleton
public class PromiseProvider {

  /**
   * Returns a new promise that has not been completed yet.
   * 
   * @return a new promise that has not been completed yet
   */
  public Promise<JsonObject> getJsonPromise() {
    return Promise.promise();
  }
}
