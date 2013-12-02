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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with JBOP. If not, see <http://www.gnu.org/licenses/>.
 */
package de.tuberlin.uebb.jbop.optimizer.methodsplitter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.lang.reflect.Method;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import de.tuberlin.uebb.jbop.optimizer.ClassNodeBuilder;
import de.tuberlin.uebb.jbop.optimizer.loop.SplitMarkNode;
import de.tuberlin.uebb.jbop.optimizer.utils.NodeHelper;

/**
 * Tests for {@link MethodSplitter}.
 * 
 * @author Christopher Ewest
 */
public class MethodSplitterTest {
  
  private MethodSplitter splitter;
  private MethodNode method;
  private ClassNode classNode;
  private ClassNodeBuilder builder;
  private final int length = MethodSplitter.MAX_LENGTH / 10;
  private final int arrayLength = 750;
  
  /**
   * Init for every test.
   */
  @Before
  public void before() {
    builder = ClassNodeBuilder.createClass("de.tuberlin.uebb.jbop.optimizer.methodsplitter.SplitTestClass").//
        addMethod("testMethod", "()V");
    method = builder.getMethod("testMethod");
    classNode = builder.getClassNode();
    splitter = new MethodSplitter(classNode, length);
  }
  
  /**
   * Tests that methodSplitterArrayIsArgNoReturn() of the Testobject is working correctly.
   * 
   * @throws Exception
   *           the exception
   */
  @Test
  public void testMethodSplitterArrayIsArgNoReturn() throws Exception {
    // INIT
    classNode.name += "1";
    method.desc = "([I)V";
    fillArray().// arrayref
        addInsn(new InsnNode(Opcodes.RETURN));
    
    // RUN
    final InsnList splitted = splitter.optimize(method.instructions, method);
    final List<MethodNode> additionalMethods = splitter.getAdditionalMethods();
    
    // ASSERT
    
    // check methods
    assertFalse(additionalMethods.isEmpty());
    for (int i = 0; i < additionalMethods.size(); ++i) {
      final MethodNode methodNode = additionalMethods.get(i);
      assertEquals("([I)V", methodNode.desc);
    }
    
    // check that the class is valid by instantiating it
    method.instructions = splitted;
    classNode.methods.addAll(additionalMethods);
    final Object instance = builder.toClass().instance();
    
    // check method functionality
    final Method instanceMethod = instance.getClass().getMethod("testMethod", int[].class);
    instanceMethod.setAccessible(true);
    final int[] is = new int[arrayLength];
    instanceMethod.invoke(instance, is);
    for (int i = 0; i < arrayLength; ++i) {
      assertEquals((i + 1) * 2, is[i]);
    }
  }
  
  @Test
  public void testMethodSplitterCreateArrayAndReturn() throws Exception {
    // INIT
    classNode.name += "2";
    method.desc = "()[I";
    builder.addArray("[I", arrayLength);//
    fillArray().// arrayref
        addInsn(new InsnNode(Opcodes.ARETURN));
    
    // RUN
    final InsnList splitted = splitter.optimize(method.instructions, method);
    final List<MethodNode> additionalMethods = splitter.getAdditionalMethods();
    
    // ASSERT
    
    // check methods
    assertFalse(additionalMethods.isEmpty());
    for (int i = 0; i < (additionalMethods.size() - 1); ++i) {
      final MethodNode methodNode = additionalMethods.get(i);
      assertEquals("([I)[I", methodNode.desc);
    }
    final MethodNode methodNode = additionalMethods.get(additionalMethods.size() - 1);
    assertEquals("([I)[I", methodNode.desc);
    
    // check that the class is valid by instantiating it
    method.instructions = splitted;
    classNode.methods.addAll(additionalMethods);
    final Object instance = builder.toClass().instance();
    
    // check method functionality
    final Method intanceMethod = instance.getClass().getMethod("testMethod");
    intanceMethod.setAccessible(true);
    final int[] invoke = (int[]) intanceMethod.invoke(instance);
    for (int i = 0; i < arrayLength; ++i) {
      assertEquals((i + 1) * 2, invoke[i]);
    }
  }
  
  private ClassNodeBuilder fillArray() {
    for (int i = 0; i < arrayLength; ++i) {
      builder.addInsn(new VarInsnNode(Opcodes.ALOAD, 1)).// arrayref
          addInsn(NodeHelper.getInsnNodeFor(i)).// index
          addInsn(NodeHelper.getInsnNodeFor((i + 1) * 2)).// value
          addInsn(new InsnNode(Opcodes.IASTORE)).//
          addInsn(new SplitMarkNode());
    }
    builder.addInsn(new VarInsnNode(Opcodes.ALOAD, 1));
    return builder;
  }
}
