package at.uibk.dps.ee.control.verticles.transmission;

import com.google.inject.ImplementedBy;
import at.uibk.dps.ee.model.graph.EnactmentGraph;
import net.sf.opendse.model.Task;

/**
 * The {@link SchedulabilityCheck} is applied after a transmission to check
 * whether the transmission target is now ready to be scheduled.
 * 
 * @author Fedor Smirnov
 */
@ImplementedBy(SchedulabilityCheckMulti.class)
public interface SchedulabilityCheck {

  /**
   * Returns true if the given task is ready to be scheduled.
   * 
   * @param target the given task
   * @param graph the enactment graph
   * @return true if the given task is ready to be scheduled
   */
  boolean isTargetSchedulable(Task target, EnactmentGraph graph);
}
