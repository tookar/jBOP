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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with JBOP. If not, see <http://www.gnu.org/licenses/>.
 */
package de.tuberlin.uebb.jbop.optimizer.utils.predicates;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.commons.collections15.Predicate;
import org.junit.Test;
import org.objectweb.asm.tree.MethodNode;

import de.tuberlin.uebb.jbop.optimizer.ClassNodeBuilder;
import de.tuberlin.uebb.jbop.optimizer.annotations.Optimizable;

/**
 * Tests for {@link OptimizablePredicate}.
 * 
 * @author Christopher Ewest
 */
public class OptimizablePredicateTest {
  
  private final ClassNodeBuilder classBuilder = ClassNodeBuilder.//
      createClass("de.tuberlin.uebb.jbop.optimizer.utils.predicates.TestClass").//
      addEmptyMethod("method");
  
  private final Predicate<MethodNode> predicate = new OptimizablePredicate();
  
  /**
   * Tests that the Predicate returns false, if no Optimizable-Annotation is present.
   */
  @Test
  public void testEvaluateNotOptimizable() {
    // INIT
    
    // RUN
    final boolean isOptimizable = predicate.evaluate(classBuilder.getMethod("method"));
    
    // ASSERT
    assertFalse(isOptimizable);
  }
  
  /**
   * Tests that the Predicate returns true, if an Optimizable-Annotation is present.
   */
  @Test
  public void testEvaluateOptimizable() {
    // INIT
    classBuilder.withAnnotation(Optimizable.class);
    
    // RUN
    final boolean isOptimizable = predicate.evaluate(classBuilder.getMethod("method"));
    
    // ASSERT
    assertTrue(isOptimizable);
  }
  
  /**
   * Tests that the Predicate returns false, if there is an Annotation present, but not {@link Optimizable}.
   */
  @Test
  public void testEvaluateOtherAnnotation() {
    // INIT
    classBuilder.withAnnotation(Test.class);
    
    // RUN
    final boolean isOptimizable = predicate.evaluate(classBuilder.getMethod("method"));
    
    // ASSERT
    assertFalse(isOptimizable);
  }
  
  /**
   * Tests that the Predicate returns false, if there aren't any annotations.
   */
  @Test
  public void testEvaluateNullAnnotations() {
    // INIT
    
    // RUN
    final boolean isOptimizable = predicate.evaluate(new MethodNode());
    
    // ASSERT
    assertFalse(isOptimizable);
  }
  
}
