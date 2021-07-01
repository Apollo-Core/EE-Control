package at.uibk.dps.ee.control.modules;

import at.uibk.dps.ee.control.verticles.OutputDataHandler;
import at.uibk.dps.ee.control.verticles.VerticleFunction;
import at.uibk.dps.ee.control.verticles.enactment.WorkerEnactment;
import at.uibk.dps.ee.control.verticles.extraction.WorkerExtraction;
import at.uibk.dps.ee.control.verticles.scheduling.WorkerScheduling;
import at.uibk.dps.ee.control.verticles.transmission.WorkerTransmission;
import at.uibk.dps.ee.core.CoreFunction;
import at.uibk.dps.ee.guice.modules.HandlerModule;

/**
 * The {@link EnactmentVerticleModule} is used to configure the binding of the
 * Apollo handlers used to process the messages on the VertX event bus.
 * 
 * @author Fedor Smirnov
 */
public class EnactmentVerticleModule extends HandlerModule {

  @Override
  protected void config() {

    bind(CoreFunction.class).to(VerticleFunction.class);

    // worker handlers
    addEBusMessageHandler(WorkerTransmission.class);
    addEBusMessageHandler(WorkerScheduling.class);
    addEBusMessageHandler(WorkerEnactment.class);
    addEBusMessageHandler(WorkerExtraction.class);

    // infrastructure handlers
    addEBusMessageHandler(OutputDataHandler.class);

  }
}
