package at.uibk.dps.ee.control.agents;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import at.uibk.dps.ee.control.management.EnactmentState;
import net.sf.opendse.model.Task;

/**
 * The default factory for the creation of {@link AgentEnactment}s.
 * 
 * @author Fedor Smirnov
 */
@Singleton
public class AgentFactoryEnactment {
	
	protected final EnactmentState enactmentState;
	
	@Inject
	public AgentFactoryEnactment(EnactmentState enactmentState) {
		this.enactmentState = enactmentState;
	}

	/**
	 * Creates an agent for the enactment of the given function task.
	 * 
	 * @param task the given function task
	 * @return an agent for the enactment of the given function task
	 */
	public AgentEnactment createAgentEnactment(Task task) {
		return new AgentEnactment(enactmentState, task);
	}
}