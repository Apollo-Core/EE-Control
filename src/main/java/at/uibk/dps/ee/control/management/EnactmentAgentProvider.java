package at.uibk.dps.ee.control.management;

import java.util.Set;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import at.uibk.dps.ee.core.EnactableProvider;
import at.uibk.dps.ee.core.function.EnactableRoot;
import at.uibk.dps.ee.core.function.EnactableStateListener;

/**
 * Guice interface to provide the enactment control classes based on agents to
 * the rest of the EE.
 * 
 * @author Fedor Smirnov
 */
@Singleton
public class EnactmentAgentProvider implements EnactableProvider {

  protected final EnactableRoot rootEnactableAgents;

  /**
   * The injection constructor.
   * 
   * @param enactmentAgents the main agent starting and ending the enactment
   * @param listeners the {@link EnactableStateListener}s.
   */
  @Inject
  public EnactmentAgentProvider(final EnactmentAgent enactmentAgents,
      final Set<EnactableStateListener> listeners) {
    this.rootEnactableAgents = new EnactableRoot(listeners, enactmentAgents);
  }

  @Override
  public EnactableRoot getEnactableApplication() {
    return rootEnactableAgents;
  }
}
