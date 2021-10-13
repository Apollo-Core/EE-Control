package at.uibk.dps.ee.control.transformation;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import at.uibk.dps.ee.control.testconstants.ConstantsControlTest;
import at.uibk.dps.ee.io.afcl.AfclReader;
import at.uibk.dps.ee.model.graph.EnactmentGraph;
import at.uibk.dps.ee.model.properties.PropertyServiceFunction;
import net.sf.opendse.model.Task;

class GraphTransformerWhileWhileTest {

  EnactmentGraph input;
  GraphTransformWhile tested;
  GraphTransformWhileCollapse testedCollapse;

  Task whileEndOuter;
  Task whileEndInner;

  @Test
  void test() {
    // check the initial element numbers
    assertEquals(20, input.getVertexCount());
    assertEquals(26, input.getEdgeCount());

    
    // build up one inner construct, collapse it, and check that the next iteration
    // of the outer loop is created correctly
    tested.modifyEnactmentGraph(input, whileEndInner);
    Task whileEndReplicaInner = input.getVertex("innerWhile--whileEnd+innerWhile");
    JsonObject testContent = new JsonObject();
    testContent.add("prop", new JsonPrimitive(42));
    PropertyServiceFunction.setOutput(whileEndReplicaInner, testContent);
    testedCollapse.modifyEnactmentGraph(input, whileEndReplicaInner);
    tested.modifyEnactmentGraph(input, whileEndOuter);
    
    assertEquals(32, input.getVertexCount());
    assertEquals(52, input.getEdgeCount());
  }


  @BeforeEach
  void setup() {
    AfclReader reader = new AfclReader(ConstantsControlTest.filePathYamlNestedWhile);
    input = reader.getEnactmentGraph();
    tested = new GraphTransformWhile();
    testedCollapse = new GraphTransformWhileCollapse();
    whileEndOuter = input.getVertex("outerWhile--whileEnd");
    whileEndInner = input.getVertex("innerWhile--whileEnd");
  }
}
