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
package de.tuberlin.uebb.jbop.optimizer.var;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.objectweb.asm.Opcodes.ACONST_NULL;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ASTORE;
import static org.objectweb.asm.Opcodes.DALOAD;
import static org.objectweb.asm.Opcodes.DASTORE;
import static org.objectweb.asm.Opcodes.DCONST_1;
import static org.objectweb.asm.Opcodes.DDIV;
import static org.objectweb.asm.Opcodes.DLOAD;
import static org.objectweb.asm.Opcodes.DREM;
import static org.objectweb.asm.Opcodes.DSTORE;
import static org.objectweb.asm.Opcodes.DSUB;
import static org.objectweb.asm.Opcodes.FCONST_1;
import static org.objectweb.asm.Opcodes.FSTORE;
import static org.objectweb.asm.Opcodes.GOTO;
import static org.objectweb.asm.Opcodes.I2D;
import static org.objectweb.asm.Opcodes.IALOAD;
import static org.objectweb.asm.Opcodes.ICONST_0;
import static org.objectweb.asm.Opcodes.ICONST_1;
import static org.objectweb.asm.Opcodes.ICONST_2;
import static org.objectweb.asm.Opcodes.ICONST_3;
import static org.objectweb.asm.Opcodes.ICONST_4;
import static org.objectweb.asm.Opcodes.ICONST_5;
import static org.objectweb.asm.Opcodes.IF_ICMPGT;
import static org.objectweb.asm.Opcodes.IF_ICMPLT;
import static org.objectweb.asm.Opcodes.IINC;
import static org.objectweb.asm.Opcodes.ILOAD;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.IRETURN;
import static org.objectweb.asm.Opcodes.ISTORE;
import static org.objectweb.asm.Opcodes.LCONST_1;
import static org.objectweb.asm.Opcodes.LSTORE;

import org.junit.Before;
import org.junit.Test;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import de.tuberlin.uebb.jbop.optimizer.ClassNodeBuilder;

/**
 * Tests for {@link RemoveUnusedLocalVars}.
 * 
 * @author Christopher Ewest
 */
public class RemoveUnusedLocalVarsTest {
  
  private ClassNodeBuilder builder;
  private MethodNode methodNode;
  private final RemoveUnusedLocalVars optimizer = new RemoveUnusedLocalVars();
  
  /**
   * Init for every test.
   */
  @Before
  public void before() {
    builder = ClassNodeBuilder.createClass("de.tuberlin.uebb.jbop.optimizer.var.RemoveUnusedTestClass").//
        addMethod("remove", "()I");
    methodNode = builder.getMethod("remove");
  }
  
  /**
   * Tests that RemoveUnusedLocalVars is working correctly.
   */
  @Test
  public void testRemoveUnusedLocalVars() {
    // INIT
    builder.add(ICONST_1).//
        add(ISTORE, 1).//
        add(ICONST_2).//
        add(ISTORE, 2).//
        add(ICONST_3).//
        add(ISTORE, 3).//
        add(ICONST_4).//
        add(ISTORE, 4).//
        add(ICONST_5).//
        add(ISTORE, 5).//
        add(DCONST_1).//
        add(DSTORE, 6).//
        add(ACONST_NULL).//
        add(ASTORE, 7).//
        add(FCONST_1).//
        add(FSTORE, 8).//
        add(LCONST_1).//
        add(LSTORE, 9).//
        add(ILOAD, 1).//
        addReturn();
    
    // RUN
    assertEquals(20, methodNode.instructions.size());
    final InsnList optimized = optimizer.optimize(methodNode.instructions, methodNode);
    
    // ASSERT
    assertEquals(4, optimized.size());
    assertEquals(ICONST_1, optimized.get(0).getOpcode());
    assertEquals(ISTORE, optimized.get(1).getOpcode());
    assertEquals(1, ((VarInsnNode) optimized.get(1)).var);
    assertEquals(ILOAD, optimized.get(2).getOpcode());
    assertEquals(1, ((VarInsnNode) optimized.get(2)).var);
    assertEquals(IRETURN, optimized.get(3).getOpcode());
  }
  
  /**
   * Tests that RemoveUnusedLocalVars is working correctly.
   */
  @Test
  public void testRemoveUnusedLocalVarsWithArray() {
    // INIT
    builder.addField("multiarr", "[[I").//
        selectMethod(methodNode.name, methodNode.desc).//
        loadFieldArrayValue("multiarr", 0).//
        store(Type.getType(Object.class), 1).//
        add(ICONST_1).//
        addReturn();
    // RUN
    assertEquals(7, methodNode.instructions.size());
    final InsnList optimized = optimizer.optimize(methodNode.instructions, methodNode);
    
    // ASSERT
    assertEquals(2, optimized.size());
    assertEquals(ICONST_1, optimized.get(0).getOpcode());
    assertEquals(IRETURN, optimized.get(1).getOpcode());
  }
  
  /**
   * Tests that RemoveUnusedLocalVars is working correctly.
   */
  @Test
  public void testRemoveUnusedLocalVarsNothingToRemove() {
    // INIT
    builder.addField("multiarr", "[[I").//
        selectMethod(methodNode.name, methodNode.desc).//
        loadFieldArrayValue("multiarr", 0).//
        store(Type.getType(Object.class), 1).//
        load(Type.getType(Object.class), 1).//
        add(ICONST_1).//
        add(IALOAD).//
        addReturn();
    
    // RUN
    assertEquals(9, methodNode.instructions.size());
    final InsnList optimized = optimizer.optimize(methodNode.instructions, methodNode);
    
    // ASSERT
    assertEquals(9, optimized.size());
  }
  
  /**
   * Tests that RemoveUnusedLocalVars is working correctly.
   */
  @Test
  public void testRemoveUnusedLocalVarsWithIIncNothingToRemove() {
    // INIT
    builder.add(ICONST_1).//
        add(ISTORE, 1).//
        add(IINC, 1, 2).//
        add(ILOAD, 1).//
        addReturn();
    // RUN
    assertEquals(5, methodNode.instructions.size());
    final InsnList optimized = optimizer.optimize(methodNode.instructions, methodNode);
    
    // ASSERT
    assertEquals(5, optimized.size());
  }
  
  /**
   * Tests that RemoveUnusedLocalVars is working correctly.
   */
  @Test
  public void testRemoveUnusedLocalVarsWithIInc() {
    // INIT
    builder.add(ICONST_1).//
        add(ISTORE, 1).//
        add(IINC, 1, 2).//
        add(ICONST_1).//
        addReturn();
    // RUN
    
    assertEquals(5, methodNode.instructions.size());
    final InsnList optimized = optimizer.optimize(methodNode.instructions, methodNode);
    
    // ASSERT
    assertEquals(2, optimized.size());
  }
  
  /**
   * Tests that RemoveUnusedLocalVars is working correctly.
   */
  @Test
  public void testRemoveUnusedLocalVarsWithLoop() {
    // INIT
    final LabelNode label1 = new LabelNode();
    final LabelNode label2 = new LabelNode();
    builder.add(ICONST_0).//
        add(ISTORE, 1).//
        add(GOTO, label1).//
        addInsn(label2).//
        add(IINC, 1, 1).//
        addInsn(label1).//
        add(ICONST_5).//
        add(ILOAD, 1).//
        add(IF_ICMPLT, label2).//
        addReturn();
    // RUN
    
    assertEquals(10, methodNode.instructions.size());
    optimizer.optimize(methodNode.instructions, methodNode);
    
    // ASSERT
    assertFalse(optimizer.isOptimized());
  }
  
  /**
   * Tests that RemoveUnusedLocalVars is working correctly.
   */
  @Test
  public void testRemoveUnusedLocalVarsWithLoopTypeTwo() {
    // INIT
    final LabelNode check = new LabelNode();
    final LabelNode ende = new LabelNode();
    builder.add(ICONST_0)//
        .add(ISTORE, 1)//
        .addInsn(check)//
        .add(ICONST_5)//
        .add(ILOAD, 1)//
        .add(IF_ICMPGT, ende)//
        .add(IINC, 1, 1)//
        .add(GOTO, check)//
        .addInsn(ende)//
        .addReturn();
    // RUN
    
    assertEquals(10, methodNode.instructions.size());
    optimizer.optimize(methodNode.instructions, methodNode);
    
    // ASSERT
    assertFalse(optimizer.isOptimized());
  }
  
  /**
   * Tests that RemoveUnusedLocalVars is working correctly.
   */
  @Test
  public void testRemoveUnusedLocalVarsWithCast() {
    // INIT
    builder.add(ICONST_0)//
        .add(I2D).add(DSTORE, 1)//
        .addReturn();
    // RUN
    
    methodNode.instructions = optimizer.optimize(methodNode.instructions, methodNode);
    
    // ASSERT
    assertEquals(1, methodNode.instructions.size());
    assertTrue(optimizer.isOptimized());
  }
  
  /**
   * Tests that RemoveUnusedLocalVars is working correctly.
   */
  @Test
  public void testRemoveUnusedLocalVarsDSRemainder() {
    // INIT
    builder.addMethod("remainder", "([D[D[D)V").//
        add(ALOAD, 1).//
        add(ICONST_0).//
        add(DALOAD).//
        add(ALOAD, 2).//
        add(ICONST_0).//
        add(DALOAD).//
        add(DREM).//
        add(DSTORE, 4).//
        add(ALOAD, 1).//
        add(ICONST_0).//
        add(DALOAD).//
        add(DLOAD, 4).//
        add(DSUB).//
        add(ALOAD, 2).//
        add(ICONST_0).//
        add(DALOAD).//
        add(DDIV).//
        addInsn(new MethodInsnNode(INVOKESTATIC, "org/apache/commons/math3/util/FastMath", "rint", "(D)D")).//
        add(DSTORE, 6).//
        add(ALOAD, 3).//
        add(ICONST_0).//
        add(DLOAD, 4).//
        add(DASTORE).//
        add(ICONST_1).//
        add(ISTORE, 8).//
        addReturn();
    final MethodNode method = builder.getMethod("remainder");
    assertEquals(26, method.instructions.size());
    
    // RUN
    final InsnList optimized = optimizer.optimize(method.instructions, method);
    
    // ASSERT
    assertEquals(13, optimized.size());
  }
}
