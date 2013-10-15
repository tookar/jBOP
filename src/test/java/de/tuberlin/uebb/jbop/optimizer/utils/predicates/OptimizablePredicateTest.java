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
