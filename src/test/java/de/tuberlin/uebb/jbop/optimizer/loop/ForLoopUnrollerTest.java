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
package de.tuberlin.uebb.jbop.optimizer.loop;

import static org.junit.Assert.assertEquals;
import static org.objectweb.asm.Opcodes.GOTO;
import static org.objectweb.asm.Opcodes.ICONST_0;
import static org.objectweb.asm.Opcodes.ICONST_5;
import static org.objectweb.asm.Opcodes.IF_ICMPGT;
import static org.objectweb.asm.Opcodes.IF_ICMPLT;
import static org.objectweb.asm.Opcodes.IINC;
import static org.objectweb.asm.Opcodes.ILOAD;
import static org.objectweb.asm.Opcodes.ISTORE;
import static org.objectweb.asm.Opcodes.NOP;

import org.junit.Before;
import org.junit.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import de.tuberlin.uebb.jbop.optimizer.ClassNodeBuilder;
import de.tuberlin.uebb.jbop.optimizer.utils.NodeHelper;

/**
 * Tests for {@link ForLoopUnroller}.
 * 
 * @author Christopher Ewest
 */
public class ForLoopUnrollerTest {
  
  private ClassNodeBuilder builder;
  private MethodNode method;
  private final ForLoopUnroller optimizer = new ForLoopUnroller();
  
  /**
   * Init for every test.
   */
  @Before
  public void before() {
    builder = ClassNodeBuilder.createClass("de.tuberlin.uebb.jbop.optimizer.loop.ForLoopTestClass").//
        addMethod("testLoop", "()V");//
    method = builder.getMethod("testLoop");
  }
  
  /**
   * Tests that ForLoopUnroller is working correctly.
   * 
   * Used is a loop of the kind:
   * 
   * for(int i=0; i< 3; ++i)
   */
  @Test
  public void testForLoopUnrollerStrictForward() {
    // INIT
    final LabelNode label1 = new LabelNode();
    final LabelNode label2 = new LabelNode();
    builder.addInsn(new InsnNode(Opcodes.ICONST_0)).//
        addInsn(new VarInsnNode(Opcodes.ISTORE, 1)).//
        addInsn(new JumpInsnNode(Opcodes.GOTO, label1)).//
        addInsn(label2).//
        addInsn(new VarInsnNode(Opcodes.ILOAD, 1)).//
        addInsn(new VarInsnNode(Opcodes.ILOAD, 1)).//
        addInsn(new InsnNode(Opcodes.IADD)).//
        addInsn(new IincInsnNode(1, 1)).//
        addInsn(label1).//
        addInsn(NodeHelper.getInsnNodeFor(3)).//
        addInsn(new VarInsnNode(Opcodes.ILOAD, 1)).//
        addInsn(new JumpInsnNode(Opcodes.IF_ICMPLT, label2)).//
        addInsn(new InsnNode(Opcodes.RETURN));
    
    // RUN
    assertEquals(13, method.instructions.size());
    final InsnList optimized = optimizer.optimize(method.instructions, method);
    
    // ASSERT
    assertEquals(19, optimized.size());
    
    int node = 0;
    for (int i = 0; i < 3; ++i) {
      assertEquals(i, NodeHelper.getNumberValue(optimized.get(node)).intValue());
      node++;
      assertEquals(Opcodes.ISTORE, optimized.get(node).getOpcode());
      node++;
      assertEquals(Opcodes.ILOAD, optimized.get(node).getOpcode());
      node++;
      assertEquals(Opcodes.ILOAD, optimized.get(node).getOpcode());
      node++;
      assertEquals(Opcodes.IADD, optimized.get(node).getOpcode());
      node++;
      assertEquals(Opcodes.NOP, optimized.get(node).getOpcode()); // this is the SkipMarkNode
      node++;
    }
  }
  
  /**
   * Tests that ForLoopUnroller is working correctly.
   * 
   * Used is a loop of the kind:
   * 
   * for(int i=6; i> 0; i=i-2)
   */
  @Test
  public void testForLoopUnrollerBackward() {
    // INIT
    final LabelNode label1 = new LabelNode();
    final LabelNode label2 = new LabelNode();
    builder.addInsn(NodeHelper.getInsnNodeFor(6)).//
        addInsn(new VarInsnNode(Opcodes.ISTORE, 1)).//
        addInsn(new JumpInsnNode(Opcodes.GOTO, label1)).//
        addInsn(label2).//
        addInsn(new VarInsnNode(Opcodes.ILOAD, 1)).//
        addInsn(new VarInsnNode(Opcodes.ILOAD, 1)).//
        addInsn(new InsnNode(Opcodes.IADD)).//
        addInsn(new IincInsnNode(1, -2)).//
        addInsn(label1).//
        addInsn(new VarInsnNode(Opcodes.ILOAD, 1)).//
        addInsn(new JumpInsnNode(Opcodes.IFGT, label2)).//
        addInsn(new InsnNode(Opcodes.RETURN));
    
    // RUN
    assertEquals(12, method.instructions.size());
    final InsnList optimized = optimizer.optimize(method.instructions, method);
    
    // ASSERT
    assertEquals(19, optimized.size());
    
    int node = 0;
    for (int i = 6; i > 0; i -= 2) {
      assertEquals(i, NodeHelper.getNumberValue(optimized.get(node)).intValue());
      node++;
      assertEquals(Opcodes.ISTORE, optimized.get(node).getOpcode());
      node++;
      assertEquals(Opcodes.ILOAD, optimized.get(node).getOpcode());
      node++;
      assertEquals(Opcodes.ILOAD, optimized.get(node).getOpcode());
      node++;
      assertEquals(Opcodes.IADD, optimized.get(node).getOpcode());
      node++;
      assertEquals(Opcodes.NOP, optimized.get(node).getOpcode());
      node++;
    }
  }
  
  @Test
  public void testForLoopUnrollerEmptyLoop() {
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
    assertEquals(10, method.instructions.size());
    final InsnList optimized = optimizer.optimize(method.instructions, method);
    
    // ASSERT
    assertEquals(1, optimized.size()); //
  }
  
  @Test
  public void testForLoopTypeTwo() {
    // INIT
    final LabelNode check = new LabelNode();
    final LabelNode ende = new LabelNode();
    builder.add(ICONST_0)//
        .add(ISTORE, 1)//
        .addInsn(check)//
        .add(ICONST_5)//
        .add(ILOAD, 1)//
        .add(IF_ICMPGT, ende)//
        .add(NOP)//
        .add(NOP)//
        .add(NOP)//
        .add(IINC, 1, 1)//
        .add(GOTO, check)//
        .addInsn(ende)//
        .add(NOP)//
        .addReturn();
    
    // RUN
    assertEquals(14, method.instructions.size());
    final InsnList optimized = optimizer.optimize(method.instructions, method);
    
    // ASSERT
    assertEquals(38, optimized.size()); //
    int node = 0;
    for (int i = 0; i < 6; ++i) {
      assertEquals(i, NodeHelper.getNumberValue(optimized.get(node)).intValue());
      node++;
      assertEquals(Opcodes.ISTORE, optimized.get(node).getOpcode());
      node++;
      assertEquals(Opcodes.NOP, optimized.get(node).getOpcode());
      node++;
      assertEquals(Opcodes.NOP, optimized.get(node).getOpcode());
      node++;
      assertEquals(Opcodes.NOP, optimized.get(node).getOpcode());
      node++;
      assertEquals(Opcodes.NOP, optimized.get(node).getOpcode());
      node++;
    }
    assertEquals(Opcodes.NOP, optimized.get(node).getOpcode());
    node++;
    assertEquals(Opcodes.RETURN, optimized.get(node).getOpcode());
    node++;
  }
}
