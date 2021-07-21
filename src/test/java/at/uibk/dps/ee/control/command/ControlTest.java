package at.uibk.dps.ee.control.command;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import at.uibk.dps.ee.control.verticles.ConstantsVertX;
import at.uibk.dps.ee.core.EnactmentState;
import at.uibk.dps.ee.guice.starter.VertxProvider;
import io.vertx.core.eventbus.EventBus;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.never;

class ControlTest {

  EventBus eBus;
  VertxProvider vProv;

  /**
   * Tests exception if no init.
   */
  @Test
  void testPlayNoInit() {
    Control tested = new Control(false, vProv);
    assertThrows(IllegalStateException.class, () -> {
      tested.play();
    });
  }

  /**
   * Tests play behavior when init done.
   */
  @Test
  void testPlayInit() {
    Control tested = new Control(false, vProv);
    tested.init = true;
    tested.play();
    assertEquals(EnactmentState.RUNNING, tested.getEnactmentState());
    verify(eBus).publish(ConstantsVertX.addressControlResume, ConstantsVertX.messageResume);
  }

  /**
   * Tests correct init (pause)
   */
  @Test
  void testEnactmentStartedPause() {
    Control tested = new Control(true, vProv);
    Control spy = spy(tested);
    spy.enactmentStarted();
    assertEquals(EnactmentState.PAUSED, spy.getEnactmentState());
    assertTrue(spy.init);
    verify(spy).pause();
    verify(eBus).publish(ConstantsVertX.addressControlPause, ConstantsVertX.messagePause);
  }

  /**
   * Tests correct init (no pause)
   */
  @Test
  void testEnactmentStarted() {
    Control tested = new Control(false, vProv);
    Control spy = spy(tested);
    spy.enactmentStarted();
    assertEquals(EnactmentState.RUNNING, spy.getEnactmentState());
    assertTrue(spy.init);
    verify(spy, never()).pause();
  }

  /**
   * Tests that methods correct on construction.
   */
  @Test
  void testConstruction() {
    Control tested = new Control(false, vProv);
    assertFalse(tested.isInit());
    assertEquals(tested.getEnactmentState(), EnactmentState.PAUSED);
    tested.setState(EnactmentState.RUNNING);
    assertEquals(tested.getEnactmentState(), EnactmentState.RUNNING);
  }

  @BeforeEach
  void setup() {
    eBus = mock(EventBus.class);
    vProv = mock(VertxProvider.class);
    when(vProv.geteBus()).thenReturn(eBus);
  }
}
