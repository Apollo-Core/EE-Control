package at.uibk.dps.ee.control.verticles;

import java.util.Set;
import java.util.concurrent.CountDownLatch;
import org.opt4j.core.start.Constant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.inject.Inject;
import at.uibk.dps.ee.guice.starter.VertxProvider;
import io.vertx.core.AsyncResult;
import io.vertx.core.Vertx;

/**
 * The {@link VerticleManager} manages the deployment of the verticles used by
 * Apollo.
 * 
 * @author Fedor Smirnov
 */
public class VerticleManager {

  protected final Set<VerticleApollo> eventBusVerticles;
  protected final Vertx vertx;
  protected final int deploymentNumber;
  protected final int verticleNumber;
  protected final CountDownLatch latch;
  
  protected final Logger logger = LoggerFactory.getLogger(VerticleManager.class);

  /**
   * Injection constructor.
   * 
   * @param eventBusVerticles the verticle types which are to be deployed as part
   *        of the current apollo instance
   * @param deploymentNumber the number of verticles which are to be deployed for
   *        each verticle type
   * @param vertxProv the vertX provider
   */
  @Inject
  public VerticleManager(Set<VerticleApollo> eventBusVerticles,
      @Constant(namespace = VerticleManager.class, value = "deploymentNumber") int deploymentNumber,
      VertxProvider vertxProv) {
    this.eventBusVerticles = eventBusVerticles;
    this.deploymentNumber = deploymentNumber;
    this.verticleNumber = eventBusVerticles.size() * deploymentNumber;
    this.vertx = vertxProv.getVertx();
    this.latch = new CountDownLatch(verticleNumber);
  }

  /**
   * Deploys the configured number of the configured verticle types. Synchronous
   * method waiting until the deployment is finished.
   */
  public void deployVerticles() {
    for (VerticleApollo verticleType : eventBusVerticles) {
      for (int i = 0; i < deploymentNumber; i++) {
        vertx.deployVerticle(verticleType, this::deployCallBack);
      }
    }
    try {
      latch.await();
      logger.info("Deployed {} verticles.", verticleNumber);
    } catch (InterruptedException e) {
      throw new IllegalStateException("Interrupted while deploying verticels", e);
    }
  }
  
  /**
   * The latch is decremented once for the deployment of each verticle.
   * @param result not used
   */
  protected void deployCallBack(AsyncResult<String> result) {
    latch.countDown();
  }
}
