package at.uibk.dps.ee.control.modules;

import org.opt4j.core.config.annotations.Category;
import com.google.inject.Binder;
import com.google.inject.multibindings.Multibinder;
import at.uibk.dps.ee.control.verticles.VerticleApollo;
import at.uibk.dps.ee.guice.modules.EeModule;

/**
 * Parent module for the modules used to configure Apollo's verticles.
 * 
 * @author Fedor Smirnov
 */
@Category("Vertex eBus Handlers")
public abstract class VerticleModule extends EeModule {

  /**
   * Adds a scalable verticle processing trigger tasks transmitted over the event
   * bus.
   * 
   * @param functionDecorator the verticle to add
   */
  public void addEBusVerticle(final Class<? extends VerticleApollo> verticle) {
    addEBusVerticle(binder(), verticle);
  }

  /**
   * Adds a scalable verticle processing trigger tasks transmitted over the event
   * bus.
   * 
   * @param binder the binder
   * @param functionDecorator the verticle to add
   */
  public static void addEBusVerticle(final Binder binder,
      final Class<? extends VerticleApollo> verticle) {
    final Multibinder<VerticleApollo> multibinder =
        Multibinder.newSetBinder(binder, VerticleApollo.class);
    multibinder.addBinding().to(verticle);
  }

  
}
