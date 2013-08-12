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
package de.tuberlin.uebb.jbop.optimizer.array;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import de.tuberlin.uebb.jbop.optimizer.ClassNodeBuilder;
import de.tuberlin.uebb.jbop.optimizer.annotations.ImmutableArray;
import de.tuberlin.uebb.jbop.optimizer.annotations.Optimizable;
import de.tuberlin.uebb.jbop.optimizer.utils.NodeHelper;

/**
 * Tests for {@link LocalArrayValueInliner}.
 * 
 * @author Christopher Ewest
 */
public class LocalArrayValueInlinerTest {
  
  /**
   * Tests that LocalArrayValueInliner is working correctly.
   * 
   * @throws Exception
   *           the exception
   */
  @Test
  public void testLocalArrayValueInliner() throws Exception {
    // INIT
    final String owner = "de.tuberlin.uebb.jbop.optimizer.array.LocalArrayValueTestClass";
    final ClassNodeBuilder builder = ClassNodeBuilder.createClass(owner).//
        addField("doubleArray", "[[D").//
        withAnnotation(ImmutableArray.class).//
        withModifiers(Opcodes.ACC_PRIVATE, Opcodes.ACC_FINAL).//
        initArray(2, 2).//
        initMultiArrayWith(1.0, 0, 0).//
        initMultiArrayWith(2.0, 0, 1).//
        initMultiArrayWith(3.0, 1, 0).//
        initMultiArrayWith(4.0, 1, 1).//
        addMethod("getArrayValue", "()D").withAnnotation(Optimizable.class).//
        addGetField("doubleArray").// 2 -> 2
        addInsn(new InsnNode(Opcodes.ICONST_0)).// 1 -> 1
        addInsn(new InsnNode(Opcodes.AALOAD)).// 1 -> 1
        addInsn(new VarInsnNode(Opcodes.ASTORE, 2)).// 1 -> 1
        addInsn(new VarInsnNode(Opcodes.ALOAD, 2)).// 1 -> 0|
        addInsn(new InsnNode(Opcodes.ICONST_1)).// 1 -> 0|
        addInsn(new InsnNode(Opcodes.DALOAD)).// 1 -> 0| 1
        addInsn(new InsnNode(Opcodes.DRETURN));// 1 -> 1
    // 14 -> 12
    final LocalArrayValueInliner inliner = new LocalArrayValueInliner(builder.toClass().instance());
    
    // RUN STEP 1
    final MethodNode method = builder.getMethod("getArrayValue");
    assertEquals(9, method.instructions.size());
    final InsnList optimized = inliner.optimize(method.instructions, method);
    
    // ASSERT STEP 1
    
    assertEquals(7, optimized.size());
    assertEquals(2.0, NodeHelper.getValue(optimized.get(5)).doubleValue(), .0001);
    
    // RUN STEP 2
    final InsnList optimized2 = inliner.optimize(method.instructions, method);
    
    // ASSERT STEP 2
    assertEquals(7, optimized2.size());
  }
}
