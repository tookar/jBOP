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
import de.tuberlin.uebb.jbop.optimizer.annotations.Optimizable;
import de.tuberlin.uebb.jbop.optimizer.utils.NodeHelper;

/**
 * Tests for {@link LocalArrayLengthInliner}.
 * 
 * @author Christopher Ewest
 */
public class LocalArrayLengthInlinerTest {
  
  /**
   * Tests that LocalArrayLengthInliner is working correctly.
   * 
   * @throws Exception
   *           the exception
   */
  @Test
  public void testLocalArrayLengthInliner() throws Exception {
    // INIT
    final String owner = "de.tuberlin.uebb.jbop.optimizer.array.LocalArrayLengthTestClass";
    final ClassNodeBuilder builder = ClassNodeBuilder.createClass(owner).//
        addField("doubleArray", "[[D").initArray(15, 42).withModifiers(Opcodes.ACC_PRIVATE, Opcodes.ACC_FINAL).//
        addMethod("getArrayLength", "()I").withAnnotation(Optimizable.class).//
        addArray("[D", 15).// 3 -> 3
        addInsn(new VarInsnNode(Opcodes.ALOAD, 1)).// 1 -> 0|
        addInsn(new InsnNode(Opcodes.ARRAYLENGTH)).// 1 -> 0|1
        addGetClassField("doubleArray").// 2 -> 2
        addInsn(new InsnNode(Opcodes.ICONST_0)).// 1 -> 1
        addInsn(new InsnNode(Opcodes.AALOAD)).// 1 -> 1
        addInsn(new VarInsnNode(Opcodes.ASTORE, 2)).// 1 -> 1
        addInsn(new VarInsnNode(Opcodes.ALOAD, 2)).// 1 -> 0|
        addInsn(new InsnNode(Opcodes.ARRAYLENGTH)).// 1 -> 0|1
        addInsn(new InsnNode(Opcodes.IADD)).// 1 -> 1
        addInsn(new InsnNode(Opcodes.IRETURN));// 1 -> 1
    // 14 -> 12
    final LocalArrayLengthInliner inliner = new LocalArrayLengthInliner();
    inliner.setInputObject(builder.toClass().instance());
    
    // RUN STEP 1
    final MethodNode method = builder.getMethod("getArrayLength");
    assertEquals(14, method.instructions.size());
    final InsnList optimized = inliner.optimize(method.instructions, method);
    
    // ASSERT STEP 1
    
    assertEquals(12, optimized.size());
    assertEquals(15, NodeHelper.getNumberValue(optimized.get(3)).intValue());
    assertEquals(42, NodeHelper.getNumberValue(optimized.get(9)).intValue());
    
    // RUN STEP 2
    final InsnList optimized2 = inliner.optimize(method.instructions, method);
    
    // ASSERT STEP 2
    assertEquals(12, optimized2.size());
  }
}
