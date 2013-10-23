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
import static org.objectweb.asm.Opcodes.ACC_FINAL;

import org.junit.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;

import de.tuberlin.uebb.jbop.optimizer.ClassNodeBuilder;
import de.tuberlin.uebb.jbop.optimizer.annotations.Optimizable;
import de.tuberlin.uebb.jbop.optimizer.utils.NodeHelper;

public class FieldArrayLengthInlinerTest {
  
  /**
   * Tests that FieldArrayLengthInliner is working correctly.
   * 
   * @throws Exception
   *           the exception
   */
  @Test
  public void testFieldArrayLengthInliner() throws Exception {
    // INIT
    final String owner = "de.tuberlin.uebb.jbop.optimizer.array.FieldArrayLengthTestClass";
    final ClassNodeBuilder builder = ClassNodeBuilder.createClass(owner).//
        addField("doubleArray", "[D").withModifiers(ACC_FINAL).initArray(15).//
        addField("objectArray", Type.getDescriptor(Object[].class)).withModifiers(ACC_FINAL).initArray(23).//
        addMethod("sumArrayLength", "()I").withAnnotation(Optimizable.class).//
        addGetClassField("doubleArray").//
        addInsn(new InsnNode(Opcodes.ARRAYLENGTH)).//
        addGetClassField("objectArray").//
        addInsn(new InsnNode(Opcodes.ARRAYLENGTH)).//
        addInsn(new InsnNode(Opcodes.IADD)).//
        addInsn(new InsnNode(Opcodes.IRETURN));
    
    final FieldArrayLengthInliner inliner = new FieldArrayLengthInliner();
    inliner.setClassNode(builder.getClassNode());
    inliner.setInputObject(builder.toClass().instance());
    
    // RUN STEP 1
    final MethodNode method = builder.getMethod("sumArrayLength");
    final InsnList optimized = inliner.optimize(method.instructions, method);
    
    // ASSERT STEP 1
    assertEquals(6, optimized.size());
    
    // RUN STEP 2
    final InsnList optimized2 = inliner.optimize(method.instructions, method);
    
    // ASSERT STEP 2
    assertEquals(4, optimized2.size());
    assertEquals(15, NodeHelper.getNumberValue(optimized2.getFirst()).intValue());
    assertEquals(23, NodeHelper.getNumberValue(optimized2.getFirst().getNext()).intValue());
    
    // RUN STEP 3
    final InsnList optimized3 = inliner.optimize(method.instructions, method);
    
    // ASSERT STEP 3
    assertEquals(4, optimized3.size());
  }
}
