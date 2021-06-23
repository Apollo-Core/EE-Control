package at.uibk.dps.ee.control.verticles;

import java.util.Set;
import net.sf.opendse.model.Element;

/**
 * Parent class of all worker factories.
 * 
 * @author Fedor Smirnov
 *
 * @param <E> type of element that the produced worker operates on
 */
public abstract class VerticleFactory<E extends Element> {

  /**
   * Creates the workers to process the provided graph element.
   * 
   * @param message the event bus message received by the supervisor
   * @return the workers to process the provided graph element
   */
  public Set<Worker<E>> getWorkers(String message) {
    return getWorkers(getElementFromMessage(message));
  }

  /**
   * Creates the workers to process the provided graph element.
   * 
   * @param graphElement the provided graph element
   * @return the workers to process the provided graph element
   */
  protected abstract Set<Worker<E>> getWorkers(E graphElement);

  /**
   * Parses the triggering element from the event bus string.
   * 
   * @param message the event bus string
   * @return the triggering element
   */
  protected abstract E getElementFromMessage(String message);

}
