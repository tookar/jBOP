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
import static org.objectweb.asm.Opcodes.IADD;
import static org.objectweb.asm.Opcodes.ICONST_0;
import static org.objectweb.asm.Opcodes.ICONST_1;
import static org.objectweb.asm.Opcodes.ICONST_2;
import static org.objectweb.asm.Opcodes.ILOAD;
import static org.objectweb.asm.Opcodes.IRETURN;
import static org.objectweb.asm.Opcodes.ISTORE;

import org.junit.Before;
import org.junit.Test;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import de.tuberlin.uebb.jbop.optimizer.ClassNodeBuilder;

/**
 * Tests for {@link LocalVarInliner}.
 * 
 * @author Christopher Ewest
 */
public class LocalVarInlinerTest {
  
  private ClassNodeBuilder builder;
  private MethodNode methodNode;
  private final LocalVarInliner optimizer = new LocalVarInliner();
  
  /**
   * Init for every test.
   */
  @Before
  public void before() {
    builder = ClassNodeBuilder.createClass("de.tuberlin.uebb.jbop.optimizer.var.LocalVarTestClass").//
        addMethod("localVars", "()I");//
    methodNode = builder.getMethod("localVars");
  }
  
  /**
   * Tests that localVarInliner is working correctly.
   * This is the simplest case:
   * there is a single store of a known value.
   * The value is pushed directly to the stack,
   * instead of loading the var.
   */
  @Test
  public void testLocalVarInliner() {
    // INIT
    builder.addInsn(new InsnNode(ICONST_1)).//
        addInsn(new VarInsnNode(ISTORE, 1)).//
        addInsn(new VarInsnNode(ILOAD, 1)).//
        addInsn(new InsnNode(IRETURN));//
    
    // RUN
    assertEquals(4, methodNode.instructions.size());
    final InsnList optimized = optimizer.optimize(methodNode.instructions, methodNode);
    
    // ASSERT
    assertEquals(4, optimized.size());
    assertTrue(optimizer.isOptimized());
    assertEquals(ICONST_1, optimized.get(2).getOpcode());
  }
  
  /**
   * Tests that LocalVarInliner is working correctly.
   * There is more then one store to the var, therefore
   * the load of the var is not replaced.
   */
  @Test
  public void testLocalVarInlinerDoubleStore() {
    // INIT
    builder.addInsn(new InsnNode(ICONST_1)).//
        addInsn(new VarInsnNode(ISTORE, 1)).//
        addInsn(new InsnNode(ICONST_1)).//
        addInsn(new VarInsnNode(ISTORE, 1)).//
        addInsn(new VarInsnNode(ILOAD, 1)).//
        addInsn(new InsnNode(IRETURN));//
    
    // RUN
    assertEquals(6, methodNode.instructions.size());
    final InsnList optimized = optimizer.optimize(methodNode.instructions, methodNode);
    
    // ASSERT
    assertEquals(6, optimized.size());
    assertFalse(optimizer.isOptimized());
  }
  
  /**
   * Tests that LocalVarInliner is working correctly.
   * There is a single (direct) store to the var, but
   * the var is altered via a iinc instruction. Therefore
   * the load of the variable is not replaced.
   */
  @Test
  public void testLocalVarInlinerIInc() {
    // INIT
    builder.addInsn(new InsnNode(ICONST_0)).//
        addInsn(new VarInsnNode(ISTORE, 1)).//
        addInsn(new IincInsnNode(1, 1)).//
        addInsn(new VarInsnNode(ILOAD, 1)).//
        addInsn(new InsnNode(IRETURN));//
    
    // RUN
    assertEquals(5, methodNode.instructions.size());
    final InsnList optimized = optimizer.optimize(methodNode.instructions, methodNode);
    
    // ASSERT
    assertEquals(5, optimized.size());
    assertFalse(optimizer.isOptimized());
  }
  
  /**
   * Tests that LocalVarInliner is working correctly.
   * There is is a single store to the var but the value is not known (yet).
   * Therefore the load of the var is not replaced.
   */
  @Test
  public void testLocalVarInlinerUnknownStore() {
    // INIT
    builder.addInsn(new InsnNode(ICONST_2)).//
        addInsn(new InsnNode(ICONST_2)).//
        addInsn(new InsnNode(IADD)).//
        addInsn(new VarInsnNode(ISTORE, 1)).//
        addInsn(new VarInsnNode(ILOAD, 1)).//
        addInsn(new InsnNode(IRETURN));//
    
    // RUN
    assertEquals(6, methodNode.instructions.size());
    final InsnList optimized = optimizer.optimize(methodNode.instructions, methodNode);
    
    // ASSERT
    assertEquals(6, optimized.size());
    assertFalse(optimizer.isOptimized());
  }
  
  // /**
  // * Tests that localVarInliner is working correctly.
  // * This is the simplest case:
  // * there is a single store of a known 'String' value.
  // * The value is pushed directly to the stack,
  // * instead of loading the var.
  // */
  // @Test
  // public void testLocalVarInlinerStringValue() {
  // // INIT
  // builder.addInsn(new LdcInsnNode("Hallo")).//
  // addInsn(new VarInsnNode(ASTORE, 1)).//
  // addInsn(new VarInsnNode(ALOAD, 1)).//
  // addInsn(new InsnNode(ARETURN));//
  // methodNode.desc = "()Ljava/lang/String;";
  //
  // // RUN
  // assertEquals(4, methodNode.instructions.size());
  // final InsnList optimized = optimizer.optimize(methodNode.instructions, methodNode);
  //
  // // ASSERT
  // assertEquals(4, optimized.size());
  // assertTrue(optimizer.isOptimized());
  // assertEquals(LDC, optimized.get(2).getOpcode());
  // }
  //
  // /**
  // * Tests that localVarInliner is working correctly.
  // * This is the simplest case:
  // * there is a single store of a known 'null' value.
  // * The value is pushed directly to the stack,
  // * instead of loading the var.
  // */
  // @Test
  // public void testLocalVarInlinerNullValue() {
  // // INIT
  // builder.addInsn(new InsnNode(ACONST_NULL)).//
  // addInsn(new VarInsnNode(ASTORE, 1)).//
  // addInsn(new VarInsnNode(ALOAD, 1)).//
  // addInsn(new InsnNode(ARETURN));//
  // methodNode.desc = "()Ljava/lang/Object;";
  //
  // // RUN
  // assertEquals(4, methodNode.instructions.size());
  // final InsnList optimized = optimizer.optimize(methodNode.instructions, methodNode);
  //
  // // ASSERT
  // assertEquals(4, optimized.size());
  // assertTrue(optimizer.isOptimized());
  // assertEquals(ACONST_NULL, optimized.get(2).getOpcode());
  // }
}
