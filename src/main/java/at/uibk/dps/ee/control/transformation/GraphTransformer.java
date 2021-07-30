package at.uibk.dps.ee.control.transformation;

import at.uibk.dps.ee.model.properties.PropertyServiceFunction;
import at.uibk.dps.ee.model.properties.PropertyServiceFunctionDataFlowCollections;
import at.uibk.dps.ee.model.properties.PropertyServiceFunction.UsageType;
import at.uibk.dps.ee.model.properties.PropertyServiceFunctionDataFlowCollections.OperationType;
import at.uibk.dps.ee.model.properties.PropertyServiceFunctionUtility.UtilityType;
import at.uibk.dps.ee.model.properties.PropertyServiceFunctionUtility;
import net.sf.opendse.model.Task;

/**
 * The {@link GraphTransformer} maps a given task onto the appropriate
 * {@link GraphTransform}.
 * 
 * @author Fedor Smirnov
 */
public class GraphTransformer {

  /**
   * Returns the appropriate transform operation for the given function node.
   * 
   * @param functionNode the function node causing the transformation
   * @return the appropriate transform operation for the given function node
   */
  public GraphTransform getTransformOperation(final Task functionNode) {
    if (PropertyServiceFunction.getUsageType(functionNode).equals(UsageType.DataFlow)) {
      if (PropertyServiceFunctionDataFlowCollections.getOperationType(functionNode)
          .equals(OperationType.Distribution)) {
        return new GraphTransformDistribution();
      } else {
        return new GraphTransformAggregation();
      }
    } else if (PropertyServiceFunction.getUsageType(functionNode).equals(UsageType.Utility)) {
      if (PropertyServiceFunctionUtility.getUtilityType(functionNode).equals(UtilityType.While)) {
        return new GraphTransformWhile();
      }
    }
    throw new IllegalArgumentException("Unknown type of data flow operation.");
  }
}
