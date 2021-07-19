package at.uibk.dps.ee.control.command;

import org.opt4j.core.start.Constant;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import at.uibk.dps.ee.control.verticles.ConstantsVertX;
import at.uibk.dps.ee.core.EnactmentState;
import at.uibk.dps.ee.core.function.EnactmentStateListener;
import at.uibk.dps.ee.guice.starter.VertxProvider;
import io.vertx.core.eventbus.EventBus;

/**
 * Class implementing the reaction to external triggers, enabling a dynamic
 * control of the enactment process.
 * 
 * @author Fedor Smirnov
 */
@Singleton
public class Control implements EnactmentStateListener {

  protected EnactmentState enactmentState = EnactmentState.PAUSED;
  protected boolean init;

  protected final boolean pauseOnStart;
  protected final EventBus eBus;

  /**
   * Injection constructor
   * 
   * @param pauseOnStart boolean set in the GUI. If true, the enactment will start
   *        in the paused state.
   */
  @Inject
  public Control(@Constant(namespace = Control.class, value = "pauseOnStart") boolean pauseOnStart,
      VertxProvider vertxProvider) {
    this.pauseOnStart = pauseOnStart;
    this.eBus = vertxProvider.geteBus();
  }

  /**
   * Run if paused. Otherwise this does nothing.
   */
  public void play() {
    if (!init) {
      throw new IllegalStateException("Control play triggerred before control initialization.");
    }
    if (enactmentState.equals(EnactmentState.PAUSED)) {
      System.out.println("resume");
      setState(EnactmentState.RUNNING);
      eBus.publish(ConstantsVertX.addressControlResume, "resume");
    }
  }

  /**
   * Pause if running. Otherwise nothing.
   */
  public void pause() {
    System.out.println("pause");
    if (enactmentState.equals(EnactmentState.RUNNING)) {
      setState(EnactmentState.PAUSED);
      eBus.publish(ConstantsVertX.addressControlPause, "pause");
    }
  }

  /**
   * Terminate the enactment.
   */
  public void stop() {
    setState(EnactmentState.STOPPED);
    System.err.println("Stopping not yet properly implemented");
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
    this.enactmentState = stateToSet;
  }

  @Override
  public void enactmentStarted() {
    enactmentState = EnactmentState.RUNNING;
    init = true;
    if (pauseOnStart) {
      pause();
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
