package at.uibk.dps.ee.control.modules;

import org.opt4j.core.config.annotations.Info;
import org.opt4j.core.config.annotations.Order;
import org.opt4j.core.start.Constant;
import at.uibk.dps.ee.control.command.Control;
import at.uibk.dps.ee.control.enactment.WorkerEnactment;
import at.uibk.dps.ee.control.extraction.WorkerExtraction;
import at.uibk.dps.ee.control.scheduling.WorkerScheduling;
import at.uibk.dps.ee.control.transformation.WorkerTransformation;
import at.uibk.dps.ee.control.transmission.WorkerTransmission;
import at.uibk.dps.ee.control.verticles.VerticleFunction;
import at.uibk.dps.ee.control.verticles.VerticleManager;
import at.uibk.dps.ee.core.CoreFunction;
import io.vertx.core.impl.cpu.CpuCoreSensor;

/**
 * The {@link EnactmentVerticleModule} is used to configure the binding of the
 * Apollo handlers used to process the messages on the VertX event bus.
 * 
 * @author Fedor Smirnov
 */
public class EnactmentVerticleModule extends VerticleModule {

  @Order(1)
  @Info("If checked, the EE will be initially in the PAUSED state.")
  @Constant(namespace = Control.class, value = "pauseOnStart")
  protected boolean pauseOnStart;

  @Order(2)
  @Info("Number of verticles deployed for each verticle type.")
  @Constant(namespace = VerticleManager.class, value = "deploymentNumber")
  protected int deploymentNumber = 2 * CpuCoreSensor.availableProcessors();


  @Override
  protected void config() {
    bind(CoreFunction.class).to(VerticleFunction.class);
    // worker handlers
    addEBusVerticle(WorkerTransmission.class);
    addEBusVerticle(WorkerScheduling.class);
    addEBusVerticle(WorkerEnactment.class);
    addEBusVerticle(WorkerExtraction.class);
    addEBusVerticle(WorkerTransformation.class);

    // probably remove this and remove enactment listener
    addEnactmentStateListener(Control.class);
  }

  public boolean isPauseOnStart() {
    return pauseOnStart;
  }

  public void setPauseOnStart(final boolean pauseOnStart) {
    this.pauseOnStart = pauseOnStart;
  }

  public int getDeploymentNumber() {
    return deploymentNumber;
  }

  public void setDeploymentNumber(final int deploymentNumber) {
    this.deploymentNumber = deploymentNumber;
  }
}
