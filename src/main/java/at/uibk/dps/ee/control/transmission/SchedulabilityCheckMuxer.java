package at.uibk.dps.ee.control.transmission;

import java.util.HashSet;
import java.util.Set;
import at.uibk.dps.ee.model.graph.EnactmentGraph;
import at.uibk.dps.ee.model.properties.PropertyServiceData;
import at.uibk.dps.ee.model.properties.PropertyServiceDependency;
import at.uibk.dps.ee.model.properties.PropertyServiceDependency.TypeDependency;
import at.uibk.dps.ee.model.properties.PropertyServiceDependencyControlIf;
import net.sf.opendse.model.Dependency;
import net.sf.opendse.model.Task;

/**
 * The {@link SchedulabilityCheck} used for multiplexing nodes.
 * 
 * @author Fedor Smirnov
 */
public class SchedulabilityCheckMuxer implements SchedulabilityCheck {

  private static final int expectedInEdgeNumber = 3;
  private static final int expectedIfEdgeNumber = 2;
  private static final int expectedTrueEdgeNumber = 1;

  @Override
  public boolean isTargetSchedulable(final Task target, final EnactmentGraph graph) {
    checkInEdges(new HashSet<>(graph.getInEdges(target)), target);
    Dependency decVarDep = null;
    Dependency trueDep = null;
    Dependency falseDep = null;
    for (final Dependency dep : graph.getInEdges(target)) {
      if (PropertyServiceDependency.getType(dep).equals(TypeDependency.ControlIf)) {
        if (PropertyServiceDependencyControlIf.getActivation(dep)) {
          trueDep = dep;
        } else {
          falseDep = dep;
        }
      } else {
        decVarDep = dep;
      }
    }
    if (!PropertyServiceDependency.isTransmissionDone(decVarDep)) {
      return false;
    }
    final boolean decVar =
        PropertyServiceData.getContent(graph.getSource(decVarDep)).getAsBoolean();
    if (decVar) {
      return PropertyServiceDependency.isTransmissionDone(trueDep);
    } else {
      return PropertyServiceDependency.isTransmissionDone(falseDep);
    }
  }

  /**
   * Checks that the in edges fulfill the expectations towards a muxer node.
   * Throws an exception otherwise.
   * 
   * @param inEdges the in edges of the muxer node.
   * @param muxer the muxer node
   */
  protected void checkInEdges(final Set<Dependency> inEdges, final Task muxer) {
    if (inEdges.size() != expectedInEdgeNumber) {
      throw new IllegalArgumentException(
          "The muxer node " + muxer.getId() + " does not have 3 in edges.");
    }
    if (inEdges.stream()
        .filter(dep -> PropertyServiceDependency.getType(dep).equals(TypeDependency.ControlIf))
        .count() != expectedIfEdgeNumber) {
      throw new IllegalArgumentException(
          "The muxer node " + muxer.getId() + " does not have 2 if edges.");
    }
    if (inEdges.stream()
        .filter(dep -> PropertyServiceDependency.getType(dep).equals(TypeDependency.ControlIf))
        .filter(dep -> PropertyServiceDependencyControlIf.getActivation(dep))
        .count() != expectedTrueEdgeNumber) {
      throw new IllegalArgumentException(
          "Two if edges annotated as true for node " + muxer.getId());
    }
    if (inEdges.stream()
        .filter(dep -> PropertyServiceDependency.getType(dep).equals(TypeDependency.ControlIf))
        .filter(dep -> !PropertyServiceDependencyControlIf.getActivation(dep))
        .count() != expectedTrueEdgeNumber) {
      throw new IllegalArgumentException(
          "Two if edges annotated as false for node " + muxer.getId());
    }
  }
}
