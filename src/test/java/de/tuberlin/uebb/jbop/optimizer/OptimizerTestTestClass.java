/*
 * Copyright (C) 2013 uebb.tu-berlin.de.
 * 
 * This file is part of JBOP (Java Bytecode OPtimizer).
 * 
 * JBOP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * JBOP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with JBOP. If not, see <http://www.gnu.org/licenses/>.
 */
package de.tuberlin.uebb.jbop.optimizer;

import de.tuberlin.uebb.jbop.optimizer.annotations.AdditionalSteps;
import de.tuberlin.uebb.jbop.optimizer.annotations.Optimizable;
import de.tuberlin.uebb.jbop.optimizer.annotations.StrictLoops;
import de.tuberlin.uebb.jbop.optimizer.loop.ForLoopUnroller;

/**
 * The Class OptimizerTestTestClass.
 * This is a Testclass used in {@link de.tuberlin.uebb.jbop.optimizer.OptimizerTest}.
 * 
 * @author Christopher Ewest
 */
class OptimizerTestTestClass {
  
  /**
   * Unmodified.
   */
  public void unmodified() {
    // this method should not be optimized
  }
  
  /**
   * Simple optimization.
   */
  @Optimizable
  public void simpleOptimization() {
    // this method should be optimized
    // with default steps
  }
  
  /**
   * Simple optimization with loops.
   */
  @Optimizable
  @StrictLoops
  public void simpleOptimizationWithLoops() {
    // this method should be optimized
    // with default steps
    // additionally loops are unrolled
  }
  
  /**
   * Additional optimization.
   */
  @Optimizable
  @AdditionalSteps(steps = ForLoopUnroller.class)
  public void additionalOptimization() {
    // this method should be optimized
    // with default steps
    // additionally loops are unrolled
  }
  
}
