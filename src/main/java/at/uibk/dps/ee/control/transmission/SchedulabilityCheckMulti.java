package at.uibk.dps.ee.control.transmission;

import at.uibk.dps.ee.model.graph.EnactmentGraph;
import at.uibk.dps.ee.model.properties.PropertyServiceFunctionDataFlow;
import net.sf.opendse.model.Task;

/**
 * The {@link SchedulabilityCheckMulti} picks the right check for the provided task.
 * 
 * @author Fedor Smirnov
 *
 */
public class SchedulabilityCheckMulti implements SchedulabilityCheck{

  protected SchedulabilityCheckDefault checkDefault = new SchedulabilityCheckDefault();
  protected SchedulabilityCheckMuxer checkMuxer = new SchedulabilityCheckMuxer();
  
  @Override
  public boolean isTargetSchedulable(Task target, EnactmentGraph graph) {
    if (PropertyServiceFunctionDataFlow.isMultiplexerNode(target)) {
      return checkMuxer.isTargetSchedulable(target, graph);
    }else {
      return checkDefault.isTargetSchedulable(target, graph);
    }
  }
}
