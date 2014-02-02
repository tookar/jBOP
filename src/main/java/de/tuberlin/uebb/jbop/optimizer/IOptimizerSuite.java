package de.tuberlin.uebb.jbop.optimizer;

import de.tuberlin.uebb.jbop.exception.JBOPClassException;

/**
 * Interface for optimizerSuits.
 */
public interface IOptimizerSuite {
  
  /**
   * Optimize the given Object.
   */
  <T> T optimize(final T input, String suffix) throws JBOPClassException;
  
  // /**
  // * return a statistic of the used steps
  // */
  // OptimizerStatistic getStats();
}
