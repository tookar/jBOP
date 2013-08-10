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
package de.tuberlin.uebb.jbop.optimizer.arithmetic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import de.tuberlin.uebb.jbop.optimizer.ClassNodeBuilder;

/**
 * Tests for {@link ArithmeticExpressionInterpreter}.
 * 
 * @author Christopher Ewest
 */
public class ArithmeticExpressionInterpreterTest {
  
  private final ArithmeticExpressionInterpreter interpreter = new ArithmeticExpressionInterpreter();
  private ClassNodeBuilder builder;
  
  @Before
  public void before() {
    builder = ClassNodeBuilder.createClass("TestClass").addMethod("testMethod", "()V");
  }
  
  /**
   * Tests that arithmeticExpressionInterpreter() of the Testobject is working correctly
   * if nothing is todo (empty amethod).
   */
  @Test
  public void testArithmeticExpressionInterpreterEmptyMethod() {
    // INIT
    
    // RUN
    final InsnList optimized = interpreter.optimize(builder.getMethod("testMethod").instructions,
        builder.getMethod("testMethod"));
    
    // ASSERT
    assertFalse(interpreter.isOptimized());
    assertEquals(0, optimized.size());
  }
  
  /**
   * Tests that arithmeticExpressionInterpreter() of the Testobject is working correctly
   * if nothing is todo (no arithmetic Expression occur in instructions).
   */
  @Test
  public void testArithmeticExpressionInterpreterNoArithmeticInstructions() {
    // INIT
    builder.addInsn(new InsnNode(Opcodes.ICONST_1)).//
        addInsn(new VarInsnNode(Opcodes.ISTORE, 1)).//
        addInsn(new InsnNode(Opcodes.ICONST_2)).//
        addInsn(new VarInsnNode(Opcodes.ISTORE, 2)).//
        addInsn(new InsnNode(Opcodes.ICONST_3)).//
        addInsn(new VarInsnNode(Opcodes.ISTORE, 3)).//
        addInsn(new InsnNode(Opcodes.RETURN));
    // RUN
    final InsnList optimized = interpreter.optimize(builder.getMethod("testMethod").instructions,
        builder.getMethod("testMethod"));
    
    // ASSERT
    assertFalse(interpreter.isOptimized());
    assertEquals(7, optimized.size());
  }
  
  /**
   * Tests that arithmeticExpressionInterpreter() of the Testobject is working correctly
   * if arithmetic Expression occur in instructions.
   */
  @Test
  public void testArithmeticExpressionInterpreter() {
    // INIT
    builder.addInsn(new InsnNode(Opcodes.ICONST_1)).//
        addInsn(new InsnNode(Opcodes.ICONST_2)).//
        addInsn(new InsnNode(Opcodes.IADD)).//
        addInsn(new InsnNode(Opcodes.ICONST_1)).//
        addInsn(new InsnNode(Opcodes.ICONST_1)).//
        addInsn(new InsnNode(Opcodes.IADD)).//
        addInsn(new InsnNode(Opcodes.IADD)).//
        addInsn(new InsnNode(Opcodes.RETURN));
    
    // RUN STEP1
    final InsnList optimized = interpreter.optimize(builder.getMethod("testMethod").instructions,
        builder.getMethod("testMethod"));
    
    // ASSERT STEP 1
    assertTrue(interpreter.isOptimized());
    assertEquals(4, optimized.size());
    assertEquals(Opcodes.ICONST_3, optimized.getFirst().getOpcode());
    assertEquals(Opcodes.ICONST_2, optimized.getFirst().getNext().getOpcode());
    
    // RUN STEP 2
    final InsnList optimized2 = interpreter.optimize(builder.getMethod("testMethod").instructions,
        builder.getMethod("testMethod"));
    
    // ASSERT STEP 2
    assertTrue(interpreter.isOptimized());
    assertEquals(2, optimized2.size());
    assertEquals(Opcodes.ICONST_5, optimized2.getFirst().getOpcode());
    
    // RUN STEP 3
    final InsnList optimized3 = interpreter.optimize(builder.getMethod("testMethod").instructions,
        builder.getMethod("testMethod"));
    
    // ASSERT STEP 3
    assertFalse(interpreter.isOptimized());
    assertEquals(2, optimized3.size());
    assertEquals(Opcodes.ICONST_5, optimized2.getFirst().getOpcode());
  }
}
