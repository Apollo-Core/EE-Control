package at.uibk.dps.ee.control.transformation;

import com.google.gson.JsonObject;
import at.uibk.dps.ee.model.constants.ConstantsEEModel;
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
    } else if (PropertyServiceFunction.getUsageType(functionNode).equals(UsageType.Utility)
        && PropertyServiceFunctionUtility.getUtilityType(functionNode).equals(UtilityType.While)) {
      // check the while decision
      final JsonObject content = PropertyServiceFunction.getOutput(functionNode);
      if (!content.has(ConstantsEEModel.JsonKeyWhileDecision)) {
        throw new IllegalArgumentException(
            "While decision variable not set in the while end task " + functionNode);
      }
      final boolean whileGoesOn = content.get(ConstantsEEModel.JsonKeyWhileDecision).getAsBoolean();
      return whileGoesOn ? new GraphTransformWhile() : new GraphTransformWhileCollapse();
    }
    throw new IllegalArgumentException("Unknown type of transform operation.");
  }
}
