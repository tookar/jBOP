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
import static org.objectweb.asm.Opcodes.AASTORE;
import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ANEWARRAY;
import static org.objectweb.asm.Opcodes.DALOAD;
import static org.objectweb.asm.Opcodes.DRETURN;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.ICONST_0;
import static org.objectweb.asm.Opcodes.ICONST_1;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.NEW;
import static org.objectweb.asm.Opcodes.PUTFIELD;

import java.util.Arrays;

import org.junit.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import de.tuberlin.uebb.jbop.optimizer.ClassNodeBuilder;
import de.tuberlin.uebb.jbop.optimizer.IOptimizer;
import de.tuberlin.uebb.jbop.optimizer.annotations.ImmutableArray;
import de.tuberlin.uebb.jbop.optimizer.annotations.Optimizable;
import de.tuberlin.uebb.jbop.optimizer.utils.NodeHelper;

/**
 * Tests for {@link FieldArrayValueInliner}.
 * 
 * @author Christopher Ewest
 */
public class FieldArrayValueInlinerTest {
  
  /**
   * Tests that FieldArrayValueInliner is working correctly.
   * 
   * @throws Exception
   *           the exception
   */
  @Test
  public void testFieldArrayValueInliner() throws Exception {
    // INIT
    final String owner = "de.tuberlin.uebb.jbop.optimizer.array.FieldArrayValueTestClass";
    final ClassNodeBuilder builder = ClassNodeBuilder.createClass(owner).//
        addField("doubleArray1", "[D").initArrayWith(1.0, 2.0, 3.0).//
        addField("doubleArray2", "[D").initArrayWith(4.0, 5.0, 6.0).//
        addMethod("sumArrayValues", "()D").withAnnotation(Optimizable.class).//
        loadFieldArrayValue("doubleArray1", 0).// 4 -> 1
        loadFieldArrayValue("doubleArray2", 0).// 4 -> 1
        addInsn(new InsnNode(Opcodes.DADD)).// 1
        loadFieldArrayValue("doubleArray1", 1).// 4 -> 1
        loadFieldArrayValue("doubleArray2", 1).// 4 -> 1
        addInsn(new InsnNode(Opcodes.DADD)).// 1
        addInsn(new InsnNode(Opcodes.DADD)).// 1
        loadFieldArrayValue("doubleArray1", 2).// 4 -> 1
        loadFieldArrayValue("doubleArray2", 2).// 4 -> 1
        addInsn(new InsnNode(Opcodes.DADD)).// 1
        addInsn(new InsnNode(Opcodes.DADD)).// 1
        addInsn(new InsnNode(Opcodes.DRETURN));// 1
    //
    
    final IOptimizer inliner = new FieldArrayValueInliner(Arrays.asList("doubleArray1", "doubleArray2"), builder
        .toClass().instance());
    
    final MethodNode method = builder.getMethod("sumArrayValues");
    assertEquals(30, method.instructions.size());
    
    // RUN STEP 1
    final InsnList optimized = inliner.optimize(method.instructions, method);
    
    // ASSERT STEP 1
    
    assertEquals(12, optimized.size());
    
    // first value pair
    AbstractInsnNode currentNode = optimized.getFirst();
    assertEquals(1.0, NodeHelper.getNumberValue(currentNode).doubleValue(), .0001);
    currentNode = currentNode.getNext();
    assertEquals(4.0, NodeHelper.getNumberValue(currentNode).doubleValue(), .0001);
    currentNode = currentNode.getNext();
    
    // second value pair
    currentNode = currentNode.getNext();
    assertEquals(2.0, NodeHelper.getNumberValue(currentNode).doubleValue(), .0001);
    currentNode = currentNode.getNext();
    assertEquals(5.0, NodeHelper.getNumberValue(currentNode).doubleValue(), .0001);
    currentNode = currentNode.getNext();
    currentNode = currentNode.getNext();
    
    // third value pair
    currentNode = currentNode.getNext();
    assertEquals(3.0, NodeHelper.getNumberValue(currentNode).doubleValue(), .0001);
    currentNode = currentNode.getNext();
    assertEquals(6.0, NodeHelper.getNumberValue(currentNode).doubleValue(), .0001);
    
    // RUN STEP 3
    final InsnList optimized3 = inliner.optimize(method.instructions, method);
    
    // ASSERT STEP 3
    assertEquals(12, optimized3.size());
  }
  
  @Test
  public void test() throws Exception {
    // INIT
    final ClassNodeBuilder builderC = ClassNodeBuilder.createClass("de.tuberlin.uebb.jbop.optimizer.array.C")
        .addField("d", "[D").withModifiers(ACC_PRIVATE, ACC_FINAL).withAnnotation(ImmutableArray.class)
        .initArrayWith(1.0).//
        toClass();
    final ClassNodeBuilder builderB = ClassNodeBuilder.createClass("de.tuberlin.uebb.jbop.optimizer.array.B")
        .addField("c", Type.getDescriptor(builderC.getBuildedClass())).withModifiers(ACC_PRIVATE, ACC_FINAL)
        .initWith(null).toClass();
    final ClassNodeBuilder builderA = ClassNodeBuilder.createClass("de.tuberlin.uebb.jbop.optimizer.array.A")
        .addField("b", Type.getDescriptor(builderB.getBuildedClass())).withModifiers(ACC_PRIVATE, ACC_FINAL)
        .initWith(null).toClass();
    final ClassNodeBuilder builderTestClass = ClassNodeBuilder
        .createClass("de.tuberlin.uebb.jbop.optimizer.array.ChainedTestClass")
        .addField("a", "[" + Type.getDescriptor(builderA.getBuildedClass())).withModifiers(ACC_PRIVATE, ACC_FINAL)
        .addToConstructor(initArray()).//
        addMethod("get", "()D").//
        addGetClassField("a").// ;
        addInsn(new InsnNode(ICONST_0)).//
        addInsn(new InsnNode(AALOAD)).//
        addGetField(builderA, "b").//
        addGetField(builderB, "c").//
        addGetField(builderC, "d").//
        addInsn(new InsnNode(ICONST_0)).//
        addInsn(new InsnNode(DALOAD)).//
        addInsn(new InsnNode(DRETURN));//
    
    final Object instance = builderTestClass.instance();
    
    // RUN
    final FieldArrayValueInliner inliner = new FieldArrayValueInliner(Arrays.asList("a"), instance);
    final MethodNode method = builderTestClass.getMethod("get");
    final InsnList optimized = inliner.optimize(method.instructions, method);
    
    // ASSERT
    assertEquals(2, optimized.size());
    assertEquals(1.0, NodeHelper.getNumberValue(optimized.get(0)).doubleValue(), .00001);
  }
  
  private InsnList initArray() {
    
    final InsnList list = new InsnList();
    list.add(new VarInsnNode(ALOAD, 0));
    list.add(new InsnNode(ICONST_1));
    list.add(new TypeInsnNode(ANEWARRAY, "de/tuberlin/uebb/jbop/optimizer/array/A"));
    list.add(new InsnNode(DUP));
    list.add(new InsnNode(ICONST_0));
    list.add(new TypeInsnNode(NEW, "de/tuberlin/uebb/jbop/optimizer/array/A"));
    list.add(new InsnNode(DUP));
    list.add(new MethodInsnNode(INVOKESPECIAL, "de/tuberlin/uebb/jbop/optimizer/array/A", "<init>", "()V"));
    list.add(new InsnNode(AASTORE));
    list.add(new FieldInsnNode(PUTFIELD, "de/tuberlin/uebb/jbop/optimizer/array/ChainedTestClass", "a",
        "[Lde/tuberlin/uebb/jbop/optimizer/array/A;"));
    return list;
  }
}
