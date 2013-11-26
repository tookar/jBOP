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
import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.ARRAYLENGTH;
import static org.objectweb.asm.Opcodes.ASTORE;
import static org.objectweb.asm.Opcodes.DADD;
import static org.objectweb.asm.Opcodes.DCONST_0;
import static org.objectweb.asm.Opcodes.DLOAD;
import static org.objectweb.asm.Opcodes.DSTORE;
import static org.objectweb.asm.Opcodes.GETSTATIC;
import static org.objectweb.asm.Opcodes.GOTO;
import static org.objectweb.asm.Opcodes.IADD;
import static org.objectweb.asm.Opcodes.IALOAD;
import static org.objectweb.asm.Opcodes.ICONST_0;
import static org.objectweb.asm.Opcodes.ICONST_1;
import static org.objectweb.asm.Opcodes.ICONST_2;
import static org.objectweb.asm.Opcodes.ICONST_3;
import static org.objectweb.asm.Opcodes.IFEQ;
import static org.objectweb.asm.Opcodes.IFGT;
import static org.objectweb.asm.Opcodes.IF_ICMPGE;
import static org.objectweb.asm.Opcodes.IINC;
import static org.objectweb.asm.Opcodes.ILOAD;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.IRETURN;
import static org.objectweb.asm.Opcodes.ISTORE;
import static org.objectweb.asm.Opcodes.LDC;
import static org.objectweb.asm.Opcodes.NOP;

import org.junit.Before;
import org.junit.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import de.tuberlin.uebb.jbop.optimizer.ClassNodeBuilder;
import de.tuberlin.uebb.jbop.optimizer.utils.NodeHelper;

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
        addInsn(new InsnNode(ICONST_2)).//
        addInsn(new VarInsnNode(ISTORE, 1)).//
        addInsn(new VarInsnNode(ILOAD, 1)).//
        addInsn(new InsnNode(IRETURN));//
    
    // RUN
    assertEquals(6, methodNode.instructions.size());
    final InsnList optimized = optimizer.optimize(methodNode.instructions, methodNode);
    
    // ASSERT
    assertEquals(6, optimized.size());
    assertTrue(optimizer.isOptimized());
    assertEquals(ICONST_2, optimized.get(4).getOpcode());
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
    assertTrue(optimizer.isOptimized());
    assertEquals(ICONST_1, optimized.get(3).getOpcode());
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
  
  /**
   * Tests that localVarInliner is working correctly.
   * This is the simplest case:
   * there is a single store of a known 'String' value.
   * The value is pushed directly to the stack,
   * instead of loading the var.
   */
  @Test
  public void testLocalVarInlinerStringValue() {
    // INIT
    builder.addInsn(new LdcInsnNode("Hallo")).//
        addInsn(new VarInsnNode(ASTORE, 1)).//
        addInsn(new VarInsnNode(ALOAD, 1)).//
        addInsn(new InsnNode(ARETURN));//
    methodNode.desc = "()Ljava/lang/String;";
    
    // RUN
    assertEquals(4, methodNode.instructions.size());
    final InsnList optimized = optimizer.optimize(methodNode.instructions, methodNode);
    
    // ASSERT
    assertEquals(4, optimized.size());
    assertTrue(optimizer.isOptimized());
    assertEquals(LDC, optimized.get(2).getOpcode());
  }
  
  /**
   * Tests that localVarInliner is working correctly.
   * This is the simplest case:
   * there is a single store of a known 'null' value.
   * The value is pushed directly to the stack,
   * instead of loading the var.
   */
  @Test
  public void testLocalVarInlinerNullValue() {
    // INIT
    builder.addInsn(new InsnNode(ACONST_NULL)).//
        addInsn(new VarInsnNode(ASTORE, 1)).//
        addInsn(new VarInsnNode(ALOAD, 1)).//
        addInsn(new InsnNode(ARETURN));//
    methodNode.desc = "()Ljava/lang/Object;";
    
    // RUN
    assertEquals(4, methodNode.instructions.size());
    final InsnList optimized = optimizer.optimize(methodNode.instructions, methodNode);
    
    // ASSERT
    assertEquals(4, optimized.size());
    assertTrue(optimizer.isOptimized());
    assertEquals(ACONST_NULL, optimized.get(2).getOpcode());
  }
  
  @Test
  public void testLocalVarInlinerWithIfElse() {
    // INIT
    final LabelNode label1 = new LabelNode();
    final LabelNode label2 = new LabelNode();
    builder.add(ICONST_1).//
        add(IFEQ, label1).//
        add(ICONST_1).//
        add(ISTORE, 1).//
        add(GOTO, label2).//
        addInsn(label1).//
        add(ICONST_2).//
        add(ISTORE, 1).//
        addInsn(label2).//
        add(ILOAD, 1).//
        addReturn();
    
    // RUN
    optimizer.optimize(methodNode.instructions, methodNode);
    
    // ASSERT
    assertFalse(optimizer.isOptimized());
  }
  
  @Test
  public void testLocalVarInlinerWithIf() {
    // INIT
    final LabelNode label1 = new LabelNode();
    builder.add(ICONST_1).//
        add(ISTORE, 1).//
        add(ICONST_1).//
        add(IFEQ, label1).//
        add(ICONST_2).//
        add(ISTORE, 1).//
        addInsn(label1).//
        add(ILOAD, 1).//
        addReturn();
    
    // RUN
    optimizer.optimize(methodNode.instructions, methodNode);
    
    // ASSERT
    assertFalse(optimizer.isOptimized());
  }
  
  @Test
  public void testLocalVarInlinerWithIf2() {
    // INIT
    final LabelNode label1 = new LabelNode();
    builder.add(ICONST_1).//
        add(ISTORE, 1).//
        add(ICONST_1).//
        add(IFEQ, label1).//
        add(ICONST_2).//
        add(ISTORE, 2).//
        add(ILOAD, 2).//
        add(ISTORE, 3).//
        addInsn(label1).//
        add(ILOAD, 1).//
        addReturn();
    
    // RUN
    final InsnList optimized = optimizer.optimize(methodNode.instructions, methodNode);
    
    // ASSERT
    assertTrue(optimizer.isOptimized());
    assertEquals(ICONST_2, optimized.get(6).getOpcode());
    assertEquals(ICONST_1, optimized.get(9).getOpcode());
  }
  
  @Test
  public void testLocalVarInlinerWithIf3() {
    // INIT
    final LabelNode label1 = new LabelNode();
    builder.add(ICONST_1).//
        add(ISTORE, 1).//
        add(ICONST_1).//
        add(IFEQ, label1).//
        add(IINC, 1, 1).//
        addInsn(label1).//
        add(ILOAD, 1).//
        addReturn();
    
    // RUN
    optimizer.optimize(methodNode.instructions, methodNode);
    
    // ASSERT
    assertFalse(optimizer.isOptimized());
  }
  
  @Test
  public void testLocalVarInlinerLoop() {
    // INIT
    final LabelNode label1 = new LabelNode();
    final LabelNode label2 = new LabelNode();
    builder.add(DCONST_0).//
        add(DSTORE, 2).//
        add(ICONST_1).//
        add(ISTORE, 1).//
        add(GOTO, label1).//
        addInsn(label2).//
        add(DCONST_0).//
        add(DLOAD, 2).//
        add(DADD).//
        add(DSTORE, 2).//
        add(IINC, 1, 1).//
        addInsn(label1).//
        addInsn(NodeHelper.getInsnNodeFor(3)).//
        add(ILOAD, 1).//
        add(Opcodes.IF_ICMPLT, label2).//
        add(DLOAD, 2).//
        addReturn();
    
    // RUN
    optimizer.optimize(methodNode.instructions, methodNode);
    
    // ASSERT
    assertFalse(optimizer.isOptimized());
  }
  
  @Test
  public void testLocalVarInlinerAlternativeLoop() {
    // INIT
    final LabelNode check = new LabelNode();
    final LabelNode loopEnd = new LabelNode();
    builder.add(ICONST_0).//
        add(ISTORE, 1).//
        addInsn(check).//
        add(ICONST_2).//
        add(ILOAD, 1).//
        add(IF_ICMPGE, loopEnd).//
        add(NOP).//
        add(IINC, 1, 1).//
        add(GOTO, check).//
        addInsn(loopEnd).//
        addReturn();
    
    // RUN
    optimizer.optimize(methodNode.instructions, methodNode);
    
    // ASSERT
    assertFalse(optimizer.isOptimized());
  }
  
  @Test
  public void testLocalVarInlinerLoopOverArray() {
    // INIT
    final LabelNode label1 = new LabelNode();
    final LabelNode label2 = new LabelNode();
    builder.add(ICONST_0).//
        add(ISTORE, 1).//
        add(GOTO, label1).//
        addInsn(label2).//
        add(IINC, 1, 1).//
        addInsn(label1).//
        add(ALOAD, 2).//
        add(ARRAYLENGTH).//
        add(ILOAD, 1).//
        add(Opcodes.IF_ICMPLT, label2).//
        add(DLOAD, 2).//
        addReturn();
    
    // RUN
    optimizer.optimize(methodNode.instructions, methodNode);
    
    // ASSERT
    assertFalse(optimizer.isOptimized());
  }
  
  @Test
  public void testLocalVarInlinerEmptyLoop() {
    // INIT
    final LabelNode label1 = new LabelNode();
    final LabelNode label2 = new LabelNode();
    builder.add(ICONST_1).//
        add(ISTORE, 1).//
        add(GOTO, label1).//
        addInsn(label2).//
        add(IINC, 1, 1).//
        addInsn(label1).//
        add(ICONST_3).//
        add(ILOAD, 1).//
        add(Opcodes.IF_ICMPLT, label2).//
        addReturn();
    
    // RUN
    optimizer.optimize(methodNode.instructions, methodNode);
    
    // ASSERT
    assertFalse(optimizer.isOptimized());
  }
  
  @Test
  public void testLocalVarInlinerArray() {
    // INIT
    builder.addField("arr", "[I").//
        selectMethod("localVars", "()I").//
        add(ICONST_0).//
        add(ISTORE, 1).//
        addGetClassField("arr").//
        add(ILOAD, 1).//
        add(IALOAD).//
        addReturn();
    
    // RUN
    optimizer.optimize(methodNode.instructions, methodNode);
    
    // ASSERT
    assertTrue(optimizer.isOptimized());
  }
  
  @Test
  public void testLocalVarLoopMin() {
    // INIT
    final LabelNode l1 = new LabelNode();
    final LabelNode l2 = new LabelNode();
    builder.add(ICONST_2).//
        add(ISTORE, 1).//
        add(GOTO, l1).//
        addInsn(l2).//
        addInsn(new FieldInsnNode(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;")).//
        add(ILOAD, 1).//
        addInsn(new MethodInsnNode(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(I)V")).//
        add(IINC, 1, -1).//
        addInsn(l1).//
        add(ILOAD, 1).//
        add(IFGT, l2).//
        add(ILOAD, 1).//
        addReturn();
    
    // RUN
    optimizer.optimize(methodNode.instructions, methodNode);
    
    // ASSERT
    assertFalse(optimizer.isOptimized());
  }
  
}
