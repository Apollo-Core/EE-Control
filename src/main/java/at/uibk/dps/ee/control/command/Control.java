package at.uibk.dps.ee.control.command;

import java.util.HashSet;
import java.util.Set;
import org.opt4j.core.start.Constant;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import at.uibk.dps.ee.core.ControlStateListener;
import at.uibk.dps.ee.core.EnactmentState;
import at.uibk.dps.ee.core.exception.StopException;
import at.uibk.dps.ee.core.function.EnactmentStateListener;

/**
 * Class implementing the reaction to external triggers, enabling a dynamic
 * control of the enactment process.
 * 
 * @author Fedor Smirnov
 */
@Singleton
public class Control implements EnactmentStateListener {

  protected EnactmentState enactmentState = EnactmentState.PAUSED;
  protected final Set<ControlStateListener> listeners = new HashSet<>();
  protected boolean init;

  protected final boolean pauseOnStart;

  /**
   * Injection constructor
   * 
   * @param pauseOnStart boolean set in the GUI. If true, the enactment will start
   *        in the paused state.
   */
  @Inject
  public Control(
      @Constant(namespace = Control.class, value = "pauseOnStart") final boolean pauseOnStart) {
    this.pauseOnStart = pauseOnStart;
  }

  /**
   * Adds a {@link ControlStateListener}.
   * 
   * @param listener the listener to add
   */
  public void addListener(final ControlStateListener listener) {
    listeners.add(listener);
  }

  /**
   * Run if paused. Otherwise this does nothing.
   */
  public void play() {
    if (!init) {
      throw new IllegalStateException("Control play triggerred before control initialization.");
    }
    if (enactmentState.equals(EnactmentState.PAUSED)) {
      setState(EnactmentState.RUNNING);
    }
  }

  /**
   * Pause if running. Otherwise nothing.
   */
  public void pause() {
    if (enactmentState.equals(EnactmentState.RUNNING)) {
      setState(EnactmentState.PAUSED);
    }
  }

  /**
   * Terminate the enactment.
   */
  public void stop() {
    setState(EnactmentState.STOPPED);
  }

  public boolean isInit() {
    return init;
  }

  /**
   * Sets the current state and notifies all listeners.
   * 
   * @param stateToSet the state to set
   */
  protected void setState(final EnactmentState stateToSet) {
    final EnactmentState previous = enactmentState;
    final EnactmentState current = stateToSet;
    this.enactmentState = stateToSet;
    for (final ControlStateListener listener : listeners) {
      try {
        listener.reactToStateChange(previous, current);
      } catch (StopException stopExc) {
        throw new IllegalStateException("Stop exception when changing the control state.", stopExc);
      }
    }
  }

  @Override
  public void enactmentStarted() {
    enactmentState = EnactmentState.RUNNING;
    init = true;
    if (pauseOnStart) {
      setState(EnactmentState.PAUSED);
    }
  }

  public EnactmentState getEnactmentState() {
    return enactmentState;
  }

  @Override
  public void enactmentTerminated() {
    // Nothing to do here
  }
}
