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
import static org.objectweb.asm.Opcodes.ICONST_1;
import static org.objectweb.asm.Opcodes.ICONST_2;
import static org.objectweb.asm.Opcodes.ICONST_3;
import static org.objectweb.asm.Opcodes.ICONST_4;
import static org.objectweb.asm.Opcodes.ICONST_5;
import static org.objectweb.asm.Opcodes.ILOAD;
import static org.objectweb.asm.Opcodes.IRETURN;
import static org.objectweb.asm.Opcodes.ISTORE;

import org.junit.Before;
import org.junit.Test;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
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
    builder.addInsn(new InsnNode(ICONST_1)).//
        addInsn(new VarInsnNode(ISTORE, 1)).//
        addInsn(new InsnNode(ICONST_2)).//
        addInsn(new VarInsnNode(ISTORE, 2)).//
        addInsn(new InsnNode(ICONST_3)).//
        addInsn(new VarInsnNode(ISTORE, 3)).//
        addInsn(new InsnNode(ICONST_4)).//
        addInsn(new VarInsnNode(ISTORE, 4)).//
        addInsn(new InsnNode(ICONST_5)).//
        addInsn(new VarInsnNode(ISTORE, 5)).//
        addInsn(new VarInsnNode(ILOAD, 1)).//
        addInsn(new InsnNode(IRETURN));
    
    // RUN
    assertEquals(12, methodNode.instructions.size());
    final InsnList optimized = optimizer.optimize(methodNode.instructions, methodNode);
    
    // ASSERT
    assertEquals(4, optimized.size());
    assertEquals(ICONST_1, optimized.get(0).getOpcode());
    assertEquals(ISTORE, optimized.get(1).getOpcode());
    assertEquals(1, (((VarInsnNode) optimized.get(1)).var));
    assertEquals(ILOAD, optimized.get(2).getOpcode());
    assertEquals(1, (((VarInsnNode) optimized.get(2)).var));
    assertEquals(IRETURN, optimized.get(3).getOpcode());
  }
}
