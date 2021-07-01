package at.uibk.dps.ee.control.modules;

import org.opt4j.core.config.annotations.Info;
import org.opt4j.core.config.annotations.Order;
import org.opt4j.core.start.Constant;
import at.uibk.dps.ee.control.command.Control;
import at.uibk.dps.ee.control.management.EnactmentAgentProvider;
import at.uibk.dps.ee.guice.modules.EeModule;

/**
 * The module configuring the main enactment agent.
 * 
 * @author Fedor Smirnov
 *
 */
public class EnactmentAgentModule extends EeModule {

  @Order(1)
  @Info("If checked, the EE will be initially in the PAUSED state.")
  @Constant(namespace = Control.class, value = "pauseOnStart")
  protected boolean pauseOnStart;

  @Override
  protected void config() {
    bind(EnactableProvider.class).to(EnactmentAgentProvider.class);
    addEnactmentStateListener(Control.class);
  }

  public boolean isPauseOnStart() {
    return pauseOnStart;
  }

  public void setPauseOnStart(final boolean pauseOnStart) {
    this.pauseOnStart = pauseOnStart;
  }
}
