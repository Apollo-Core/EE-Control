package at.uibk.dps.ee.control.transformation;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import at.uibk.dps.ee.io.afcl.AfclReader;
import at.uibk.dps.ee.model.graph.EnactmentGraph;
import at.uibk.dps.ee.model.properties.PropertyServiceFunction;
import at.uibk.dps.ee.model.properties.PropertyServiceFunctionUtility.UtilityType;
import net.sf.opendse.model.Task;
import net.sf.opendse.model.properties.TaskPropertyService;

class GraphTransformWhileTest {

  EnactmentGraph input;

  GraphTransformWhile tested;
  GraphTransformWhileCollapse testedCollapse;
  Task whileEndTask;

  /**
   * Tests that the graph part for the next while iteration is built correctly.
   */
  @Test
  void testGraphModification() {

    int numFunctionNodes = (int) input.getVertices().stream()
        .filter(node -> TaskPropertyService.isProcess(node)).count();
    int numDataNodes = input.getVertices().size() - numFunctionNodes;
    int numEdges = input.getEdgeCount();

    assertEquals(3, numFunctionNodes);
    assertEquals(9, numDataNodes);
    assertEquals(13, numEdges);

    tested.modifyEnactmentGraph(input, whileEndTask);
    

    numFunctionNodes = (int) input.getVertices().stream()
        .filter(node -> TaskPropertyService.isProcess(node)).count();
    numDataNodes = input.getVertices().size() - numFunctionNodes;
    numEdges = input.getEdgeCount();

    assertEquals(6, numFunctionNodes);
    assertEquals(12, numDataNodes);
    assertEquals(26, numEdges);

    Task whileEndReplica = input.getVertex("while--whileEnd+while");
    
    JsonObject testContent = new JsonObject();
    testContent.add("prop", new JsonPrimitive(42));
    PropertyServiceFunction.setOutput(whileEndReplica, testContent);
    
    testedCollapse.modifyEnactmentGraph(input, whileEndReplica);

    numFunctionNodes = (int) input.getVertices().stream()
        .filter(node -> TaskPropertyService.isProcess(node)).count();
    numDataNodes = input.getVertices().size() - numFunctionNodes;
    numEdges = input.getEdgeCount();

    assertEquals(3, numFunctionNodes);
    assertEquals(9, numDataNodes);
    assertEquals(13, numEdges);
    
    assertEquals(testContent, PropertyServiceFunction.getOutput(whileEndTask));
  }

  /**
   * Tests the correct transformation name string
   */
  @Test
  void testTransName() {
    assertEquals(UtilityType.While.name(), tested.getTransformName());
  }

  @BeforeEach
  void setup() {
    AfclReader reader = new AfclReader("src/test/resources/simpleWhile.yaml");
    input = reader.getEnactmentGraph();
    tested = new GraphTransformWhile();
    testedCollapse = new GraphTransformWhileCollapse();
    whileEndTask = input.getVertex("while--whileEnd");
  }
}
