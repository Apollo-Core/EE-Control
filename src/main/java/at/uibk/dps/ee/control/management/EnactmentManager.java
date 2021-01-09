package at.uibk.dps.ee.control.management;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import at.uibk.dps.ee.control.command.Control;
import at.uibk.dps.ee.control.elemental.AtomicEnactment;
import at.uibk.dps.ee.core.ControlStateListener;
import at.uibk.dps.ee.core.EnactmentState;
import at.uibk.dps.ee.core.enactable.Enactable;
import at.uibk.dps.ee.core.enactable.EnactableRoot;
import at.uibk.dps.ee.core.enactable.EnactableStateListener;
import at.uibk.dps.ee.core.exception.StopException;
import at.uibk.dps.ee.enactables.EnactableAtomic;
import at.uibk.dps.ee.enactables.EnactableFactory;
import at.uibk.dps.ee.model.graph.EnactmentGraph;
import at.uibk.dps.ee.model.graph.EnactmentGraphProvider;
import at.uibk.dps.ee.model.properties.PropertyServiceData;
import at.uibk.dps.ee.model.properties.PropertyServiceData.NodeType;
import at.uibk.dps.ee.model.properties.PropertyServiceDependency;
import at.uibk.dps.ee.model.properties.PropertyServiceDependency.TypeDependency;
import at.uibk.dps.ee.model.properties.PropertyServiceDependencyControlIf;
import net.sf.opendse.model.Dependency;
import net.sf.opendse.model.Task;
import net.sf.opendse.model.properties.TaskPropertyService;

/**
 * The {@link EnactmentManager} maintains the {@link EnactmentGraph}, monitors
 * the state of the elemental enactables, and triggers their execution as soon
 * as their input data is available.
 * 
 * @author Fedor Smirnov
 *
 */
public class EnactmentManager extends EnactableRoot implements ControlStateListener, EnactableStateListener {

	protected final EnactmentGraph graph;
	protected final Map<Task, EnactableAtomic> task2EnactableMap;
	protected final Set<Task> leafNodes;

	// Set of tasks which can be started
	protected final Set<Task> readyTasks = new HashSet<>();

	/**
	 * Default constructor.
	 * 
	 * @param stateListeners the enactable state listeners added via guice
	 * @param graphProvider  the object providing the {@link EnactmentGraph}
	 * @param control        the control object for the implementation of user
	 *                       commands
	 */
	public EnactmentManager(final Set<EnactableStateListener> stateListeners,
			final EnactmentGraphProvider graphProvider, final Control control) {
		super(stateListeners);
		this.graph = graphProvider.getEnactmentGraph();
		EnactableFactory factory = new EnactableFactory(stateListeners);
		factory.addEnactableStateListener(this);
		this.task2EnactableMap = UtilsManagement.generateTask2EnactableMap(graph, factory);
		this.leafNodes = UtilsManagement.getLeafNodes(graph);
		control.addListener(this);
	}

	/**
	 * Returns true if all leaf nodes have been annotated with content.
	 * 
	 * @return true if all leaf nodes have been annotated with content
	 */
	protected boolean wfFinished() {
		for (Task task : leafNodes) {
			if (!PropertyServiceData.isDataAvailable(task)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public synchronized void reactToStateChange(EnactmentState previousState, EnactmentState currentState)
			throws StopException {
		if (previousState.equals(EnactmentState.RUNNING) && currentState.equals(EnactmentState.PAUSED)) {
			// run => pause
			pause();
		} else if (previousState.equals(EnactmentState.PAUSED) && currentState.equals(EnactmentState.RUNNING)) {
			// pause => run
			notifyAll();
			setState(State.RUNNING);
		} else {
			throw new IllegalStateException("Transition from enactment state " + previousState.name() + " to "
					+ currentState.name() + " not yet implemented.");
		}
	}

	@Override
	protected synchronized void myPlay() throws StopException {
		while (!wfFinished()) {
			if (!readyTasks.isEmpty() && !state.equals(State.PAUSED)) {
				// enact an enactable and annotate the results
				enactReadyTasks();
			} else {
				// wait to be woken up by a worker thread
				try {
					wait();
				} catch (InterruptedException e) {
					throw new IllegalStateException("Enactment manager interrupted while waiting.");
				}
			}
		}
	}

	/**
	 * Creates a callable for each task which is ready. The callable triggers the
	 * execution of the enactable and then annotates the output data.
	 * 
	 * @param readyTasks the tasks which are ready
	 */
	protected void enactReadyTasks() {
		Set<Task> handled = new HashSet<>();
		for (Task task : readyTasks) {
			EnactableAtomic enactable = task2EnactableMap.get(task);
			AtomicEnactment atomicEnactment = new AtomicEnactment(enactable, task);
			Thread thread = new Thread(atomicEnactment);
			thread.start();
			handled.add(task);
		}
		synchronized (readyTasks) {
			readyTasks.removeAll(handled);
		}
	}

	@Override
	protected void myPause() {
		// TODO Has to be implemented

	}

	@Override
	protected void myInit() {
		// annotates the data present at the start of the enactment
		for (Task node : graph) {
			if (TaskPropertyService.isCommunication(node)) {
				if (PropertyServiceData.isRoot(node)) {
					// root nodes
					initRootNodeContent(node);
				} else if (PropertyServiceData.getNodeType(node).equals(NodeType.Constant)) {
					// constant nodes
					initConstantDataNode(node);
				}
			}
		}
	}

	/**
	 * Initializes the given constant node by notiying all of its function
	 * successors.
	 * 
	 * @param constantNode the given contant node.
	 */
	protected void initConstantDataNode(Task constantNode) {
		annotateDataConsumers(constantNode);
	}

	/**
	 * Initializes the content of the given root node and sets the input of its
	 * function successors.
	 * 
	 * @param rootNode the given root node
	 */
	protected void initRootNodeContent(Task rootNode) {
		String key = PropertyServiceData.getJsonKey(rootNode);
		if (!wfInput.has(key)) {
			throw new IllegalStateException("The input " + key + " was not provided in the WF input.");
		}
		JsonElement content = wfInput.get(key);
		PropertyServiceData.setContent(rootNode, content);
		annotateDataConsumers(rootNode);
	}

	/**
	 * Annotates the content of the given node in all enactables of its function
	 * successors.
	 * 
	 * @param dataNode the given node
	 */
	protected void annotateDataConsumers(Task dataNode) {
		for (Dependency outEdge : graph.getOutEdges(dataNode)) {
			if (PropertyServiceDependency.getType(outEdge).equals(TypeDependency.Data)) {
				annotateDataConsumer(dataNode, outEdge);
			} else if (PropertyServiceDependency.getType(outEdge).equals(TypeDependency.ControlIf)) {
				boolean decisionVariable = PropertyServiceData.getContent(dataNode).getAsBoolean();
				if (PropertyServiceDependencyControlIf.getActivation(outEdge) == decisionVariable) {
					annotateDataConsumer(dataNode, outEdge);
				}
			} else {
				throw new IllegalStateException("The edge " + outEdge.getId() + " has an unknown type.");
			}
		}
	}

	/**
	 * Annotates the consumer connected to the given data node by the provided edge.
	 * 
	 * @param dataNode       the given data node
	 * @param edgeToConsumer the provided edge
	 */
	protected void annotateDataConsumer(Task dataNode, Dependency edgeToConsumer) {
		Task functionNode = graph.getDest(edgeToConsumer);
		EnactableAtomic enactable = task2EnactableMap.get(functionNode);
		String jsonKey = PropertyServiceDependency.getJsonKey(edgeToConsumer);
		enactable.setInput(jsonKey, PropertyServiceData.getContent(dataNode));
	}

	@Override
	public JsonObject getOutput() {
		JsonObject result = new JsonObject();
		// iterate the leaves
		for (Task task : graph) {
			if (TaskPropertyService.isCommunication(task) && PropertyServiceData.isLeaf(task)) {
				if (!PropertyServiceData.isDataAvailable(task)) {
					throw new IllegalStateException("No data in the leaf node " + task.getId());
				}
				String jsonKey = PropertyServiceData.getJsonKey(task);
				result.add(jsonKey, PropertyServiceData.getContent(task));
			}
		}
		return result;
	}

	/**
	 * Annotates the results of the task execution on the successor data nodes.
	 * 
	 * @param enactable the enactable which is finished with its execution
	 */
	protected void annotateExecutionResults(EnactableAtomic enactable) {
		Task task = enactable.getFunctionNode();
		JsonObject result = enactable.getJsonResult();
		for (Dependency outEdge : graph.getOutEdges(task)) {
			if (PropertyServiceDependency.getType(outEdge).equals(TypeDependency.Data)) {
				Task dataNode = graph.getDest(outEdge);
				String jsonKey = PropertyServiceDependency.getJsonKey(outEdge);
				if (!result.has(jsonKey)) {
					throw new IllegalStateException("The enactment of task " + task.getId()
							+ " did not provide a result for the key " + jsonKey);
				}
				JsonElement content = result.get(jsonKey);
				PropertyServiceData.setContent(dataNode, content);
				annotateDataConsumers(dataNode);
			}
		}
	}

	@Override
	public void enactableStateChanged(Enactable enactable, State previousState, State currentState) {
		if (enactable instanceof EnactableAtomic) {
			if (previousState.equals(State.WAITING) && currentState.equals(State.READY)) {
				synchronized (this) {
					// Enactable is ready => add its task to the ready list
					readyTasks.add(((EnactableAtomic) enactable).getFunctionNode());
					this.notifyAll();
				}
			} else if (enactable instanceof EnactableAtomic && previousState.equals(State.RUNNING)
					&& currentState.equals(State.FINISHED)) {
				// Enactable finished execution => annotate the results
				annotateExecutionResults((EnactableAtomic) enactable);
				if (wfFinished()) {
					synchronized (this) {
						this.notifyAll();
					}
				}
			}
		}
	}

	@Override
	protected void myReset() {
		// TODO Auto-generated method stub
	}
}
