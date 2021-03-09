package at.uibk.dps.ee.control.management;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;
import at.uibk.dps.ee.core.exception.StopException;

/**
 * An implementation of the {@link EmergencyManager} which reacts to each
 * exception by ending the enactment process via a
 * 
 * @author Fedor Smirnov
 *
 */
public class EmergencyManagerHardStop implements EmergencyManager {

  protected boolean emergencyState;
  protected Optional<Exception> exc = Optional.empty();
  protected String additionalInformation;
  protected Optional<EnactmentAgent> mainAgent = Optional.empty();

  @Override
  public void registerMain(final EnactmentAgent mainAgent) {
    this.mainAgent = Optional.of(mainAgent);
  }

  @Override
  public void reactToException(final Exception exc, final String additionalInformation) {
    emergencyState = true;
    this.exc = Optional.of(exc);
    this.additionalInformation = additionalInformation;
    mainAgent.get().wakeUp();
  }

  @Override
  public boolean isEmergency() {
    return emergencyState;
  }

  @Override
  public void emergencyProtocol() throws StopException {
    final StringBuffer buffer = new StringBuffer(additionalInformation).append('\n');
    if (exc.get() instanceof StopException) {
      StopException stopExc = (StopException) exc.get();
      buffer.append(stopExc.getStopMessage()).append('\n');
    }
    // convert the exc stack trace to string
    final StringWriter stringWriter = new StringWriter();
    final PrintWriter printWriter = new PrintWriter(stringWriter);
    exc.get().printStackTrace(printWriter);
    buffer.append(stringWriter.toString());
    throw new StopException(buffer.toString(), exc.get());
  }
}
