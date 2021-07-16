package at.uibk.dps.ee.control.verticles;

import java.util.Set;
import java.util.concurrent.CountDownLatch;
import org.opt4j.core.start.Constant;
import com.google.inject.Inject;
import at.uibk.dps.ee.guice.starter.VertxProvider;
import io.vertx.core.Vertx;

/**
 * The {@link VerticleManager} manages the deployment of the verticles used by
 * Apollo.
 * 
 * @author Fedor Smirnov
 */
public class VerticleManager {

  protected final Set<VerticleApollo> eventBusVerticles;
  protected final int deploymentNumber;
  protected final Vertx vertx;


  @Inject
  public VerticleManager(Set<VerticleApollo> eventBusVerticles,
      @Constant(namespace = VerticleManager.class, value = "deploymentNumber") int deploymentNumber,
      VertxProvider vertxProv) {
    this.eventBusVerticles = eventBusVerticles;
    this.deploymentNumber = deploymentNumber;
    this.vertx = vertxProv.getVertx();
  }

  /**
   * Deploys the configured number of the configured verticle types. Synchronous
   * method waiting until the deployment is finished.
   */
  public void deployVerticles() {
    int verticleNumber = eventBusVerticles.size() * deploymentNumber;
    final CountDownLatch latch = new CountDownLatch(verticleNumber);

    for (VerticleApollo verticleType : eventBusVerticles) {
      for (int i = 0; i < deploymentNumber; i++) {
        vertx.deployVerticle(verticleType, completionEvent -> latch.countDown());
      }
    }

    try {
      latch.await();
      System.out.println("Deployed " + verticleNumber + " verticles.");
    } catch (InterruptedException e) {
      throw new IllegalStateException("Interrupted while deploying verticels", e);
    }
  }
}
