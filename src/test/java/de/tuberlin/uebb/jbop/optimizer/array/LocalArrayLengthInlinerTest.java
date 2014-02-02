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
import static org.objectweb.asm.Opcodes.AALOAD;
import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ARRAYLENGTH;
import static org.objectweb.asm.Opcodes.ASTORE;
import static org.objectweb.asm.Opcodes.IALOAD;
import static org.objectweb.asm.Opcodes.ICONST_0;
import static org.objectweb.asm.Opcodes.ICONST_1;
import static org.objectweb.asm.Opcodes.ICONST_3;
import static org.objectweb.asm.Opcodes.ISTORE;

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
  public void test_newarrayAndSubarray() throws Exception {
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
    method.instructions = optimized;
    
    // ASSERT STEP 1
    assertEquals(12, optimized.size());
    assertEquals(15, NodeHelper.getNumberValue(optimized.get(3)).intValue());
    assertEquals(42, NodeHelper.getNumberValue(optimized.get(9)).intValue());
    
    // RUN STEP 2
    final InsnList optimized2 = inliner.optimize(method.instructions, method);
    
    // ASSERT STEP 2
    assertEquals(12, optimized2.size());
  }
  
  @Test
  public void test_multiArray() throws Exception {
    final ClassNodeBuilder builder = ClassNodeBuilder.createClass("de.tuberlin.uebb.jbop.optimizer.array.Test1");
    builder.addField("field", "[[[I").//
        withModifiers(ACC_PRIVATE, ACC_FINAL).//
        withAnnotation(ImmutableArray.class);
    builder.removeMethod("<init>", "()V").//
        addConstructor("([[[I)V", "()V").//
        add(ALOAD, 0).//
        add(ALOAD, 1).//
        addPutClassField("field");
    builder.//
        addMethod("doit", "()V").//
        addGetClassField("field").//
        add(ICONST_0).//
        add(AALOAD).//
        add(ASTORE, 1).//
        add(ALOAD, 1).//
        add(ICONST_0).//
        add(AALOAD).//
        add(ASTORE, 2).//
        add(ALOAD, 2).//
        add(ICONST_1).//
        add(IALOAD).//
        add(ISTORE, 3).//
        add(ALOAD, 1).//
        add(ARRAYLENGTH).//
        add(ALOAD, 2).//
        add(ARRAYLENGTH).//
        addReturn();
    final Object instance = builder.instance(new Object[] {
      new int[][][] {
        {
          {
              1, 2, 3
          }
        }
      }
    });
    
    final LocalArrayLengthInliner inliner = new LocalArrayLengthInliner();
    inliner.setInputObject(instance);
    
    final MethodNode method = builder.getMethod("doit");
    final InsnList optimized = inliner.optimize(method.instructions, method);
    method.instructions = optimized;
    assertEquals(16, optimized.size());
    assertEquals(ICONST_1, optimized.get(13).getOpcode());
    assertEquals(ICONST_3, optimized.get(14).getOpcode());
  }
  
}
