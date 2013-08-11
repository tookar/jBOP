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

import java.util.Arrays;

import org.junit.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;

import de.tuberlin.uebb.jbop.optimizer.ClassNodeBuilder;
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
    
    final FieldArrayValueInliner inliner = new FieldArrayValueInliner(Arrays.asList("doubleArray1", "doubleArray2"),
        builder.toClass().instance());
    
    final MethodNode method = builder.getMethod("sumArrayValues");
    assertEquals(30, method.instructions.size());
    
    // RUN STEP 1
    final InsnList optimized = inliner.optimize(method.instructions, method);
    
    // ASSERT STEP 1
    
    assertEquals(12, optimized.size());
    
    // first value pair
    AbstractInsnNode currentNode = optimized.getFirst();
    assertEquals(1.0, NodeHelper.getValue(currentNode).doubleValue(), .0001);
    currentNode = currentNode.getNext();
    assertEquals(4.0, NodeHelper.getValue(currentNode).doubleValue(), .0001);
    currentNode = currentNode.getNext();
    
    // second value pair
    currentNode = currentNode.getNext();
    assertEquals(2.0, NodeHelper.getValue(currentNode).doubleValue(), .0001);
    currentNode = currentNode.getNext();
    assertEquals(5.0, NodeHelper.getValue(currentNode).doubleValue(), .0001);
    currentNode = currentNode.getNext();
    currentNode = currentNode.getNext();
    
    // third value pair
    currentNode = currentNode.getNext();
    assertEquals(3.0, NodeHelper.getValue(currentNode).doubleValue(), .0001);
    currentNode = currentNode.getNext();
    assertEquals(6.0, NodeHelper.getValue(currentNode).doubleValue(), .0001);
    
    // RUN STEP 3
    final InsnList optimized3 = inliner.optimize(method.instructions, method);
    
    // ASSERT STEP 3
    assertEquals(12, optimized3.size());
  }
}
