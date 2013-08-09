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
package de.tuberlin.uebb.jdae.optimizer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import de.tuberlin.uebb.jdae.exception.JBOPClassException;
import de.tuberlin.uebb.jdae.optimizer.arithmetic.ArithmeticExpressionInterpreter;
import de.tuberlin.uebb.jdae.optimizer.array.FieldArrayLengthInliner;
import de.tuberlin.uebb.jdae.optimizer.array.FieldArrayValueInliner;
import de.tuberlin.uebb.jdae.optimizer.array.LocalArrayLengthInliner;
import de.tuberlin.uebb.jdae.optimizer.array.LocalArrayValueInliner;
import de.tuberlin.uebb.jdae.optimizer.controlflow.ConstantIfInliner;
import de.tuberlin.uebb.jdae.optimizer.loop.ForLoopUnroller;
import de.tuberlin.uebb.jdae.optimizer.var.LocalVarInliner;
import de.tuberlin.uebb.jdae.optimizer.var.RemoveUnusedLocalVars;

/**
 * Tests for {@link Optimizer}.
 * 
 * @author Christopher Ewest
 */
public class OptimizerTest {
  
  private ClassNode classNode;
  private final Optimizer optimizer = new Optimizer();
  private final OptimizerTestTestClass input = new OptimizerTestTestClass();
  
  private static final List<Class<? extends IOptimizer>> DEFAULT_OPTIMIZER_STEPS;
  static {
    final List<Class<? extends IOptimizer>> optimizers = new ArrayList<>();
    optimizers.add(LocalArrayLengthInliner.class);
    optimizers.add(FieldArrayLengthInliner.class);
    optimizers.add(LocalArrayValueInliner.class);
    optimizers.add(FieldArrayValueInliner.class);
    optimizers.add(LocalVarInliner.class);
    optimizers.add(RemoveUnusedLocalVars.class);
    optimizers.add(ConstantIfInliner.class);
    optimizers.add(ArithmeticExpressionInterpreter.class);
    DEFAULT_OPTIMIZER_STEPS = Collections.unmodifiableList(optimizers);
  }
  
  /**
   * Init for every test.
   * 
   * @throws IOException
   *           Signals that an I/O exception has occurred.
   */
  @Before
  public void before() throws IOException {
    classNode = new ClassNode(Opcodes.ASM4);
    new ClassReader("de.tuberlin.uebb.jdae.optimizer.OptimizerTestTestClass")
        .accept(classNode, ClassReader.SKIP_FRAMES);
  }
  
  /**
   * Tests that all default-Steps are initialized when method is marked with <code>@</code>Optimizable.
   * 
   * @throws Exception
   *           the exception
   */
  @Test
  public void testInitOptimizersDefault() throws Exception {
    // INIT
    final String methodName = "simpleOptimization";
    
    // RUN
    runTest(methodName, DEFAULT_OPTIMIZER_STEPS);
  }
  
  /**
   * Tests that all needed Steps are initialized when method is marked with <code>@</code>Optimizable and <code>@</code>
   * StrictLoop.
   * 
   * @throws Exception
   *           the exception
   */
  @Test
  public void testInitOptimizersStrictLoops() throws Exception {
    // INIT
    final String methodName = "simpleOptimizationWithLoops";
    final List<Class<? extends IOptimizer>> optimizers = new ArrayList<>();
    optimizers.addAll(DEFAULT_OPTIMIZER_STEPS);
    optimizers.add(ForLoopUnroller.class);
    
    // RUN
    runTest(methodName, optimizers);
  }
  
  /**
   * Tests that all needed Steps are initialized when method is marked with <code>@</code>Optimizable and <code>@</code>
   * AdditionalSteps.
   * 
   * @throws Exception
   *           the exception
   */
  @Test
  public void testInitOptimizersSAdditionalSteps() throws Exception {
    // INIT
    final String methodName = "additionalOptimization";
    final List<Class<? extends IOptimizer>> optimizers = new ArrayList<>();
    optimizers.addAll(DEFAULT_OPTIMIZER_STEPS);
    optimizers.add(ForLoopUnroller.class);
    
    // RUN
    runTest(methodName, optimizers);
  }
  
  private void runTest(final String methodName, final List<Class<? extends IOptimizer>> expectedOptimizers)
      throws JBOPClassException {
    // INIT
    final MethodNode methodNode = getMethod(methodName);
    
    // RUN
    final List<IOptimizer> optimizers = optimizer.initOptimizers(classNode, methodNode, input);
    
    // ASSERT
    assertContainsAllAndOnly(expectedOptimizers, optimizers);
  }
  
  private void assertContainsAllAndOnly(final List<Class<? extends IOptimizer>> expected,
      final List<IOptimizer> optimizers) {
    assertEquals(expected.size(), optimizers.size());
    for (final Class<? extends IOptimizer> expectedOptimizerClass : expected) {
      boolean found = false;
      for (final IOptimizer actualOptimizer : optimizers) {
        if (actualOptimizer.getClass().equals(expectedOptimizerClass)) {
          found = true;
          break;
        }
      }
      if (!found) {
        fail(expectedOptimizerClass.getName() + " was not found, but was expected.");
      }
    }
  }
  
  private MethodNode getMethod(final String name) {
    for (final MethodNode method : classNode.methods) {
      if (name.equals(method.name)) {
        return method;
      }
    }
    return null;
  }
  
}
