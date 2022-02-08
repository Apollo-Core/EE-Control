package at.uibk.dps.ee.control.init;

import org.opt4j.core.start.Constant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.inject.Inject;
import at.uibk.dps.ee.core.Initializer;
import at.uibk.dps.ee.guice.starter.VertxProvider;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;

/**
 * The {@link InitializerDelayedExecution} introduces a temporal offset between
 * running an orchestration and the actual start of the orchestration done by
 * Apollo (Scheduling multiple enactments could be a use case, but this is
 * mainly here as an example initializer).
 * 
 * @author Fedor Smirnov
 */
public class InitializerDelayedExecution implements Initializer {

  protected final int delayInSeconds;
  protected final Vertx vertX;

  protected final Logger logger = LoggerFactory.getLogger(InitializerDelayedExecution.class);

  @Inject
  public InitializerDelayedExecution(VertxProvider vProv,
      @Constant(namespace = InitializerDelayedExecution.class,
          value = "delayInSeconds") int delayInSeconds) {
    this.delayInSeconds = delayInSeconds;
    this.vertX = vProv.getVertx();
  }

  @Override
  public Future<String> initialize() {
    Promise<String> resultPromise = Promise.promise();
    if (delayInSeconds > 0) {
      logger.info("Enactment delayed by {} seconds.", delayInSeconds);
      vertX.setTimer(delayInSeconds * 1000, asyncRes -> {
        logger.info("Starting enactment after delay.");
        resultPromise.complete();
      });
    } else {
      resultPromise.complete();
    }
    return resultPromise.future();
  }
}
