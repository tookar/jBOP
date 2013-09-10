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
import static org.junit.Assert.assertTrue;
import static org.objectweb.asm.Opcodes.AALOAD;
import static org.objectweb.asm.Opcodes.AASTORE;
import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ANEWARRAY;
import static org.objectweb.asm.Opcodes.DALOAD;
import static org.objectweb.asm.Opcodes.DASTORE;
import static org.objectweb.asm.Opcodes.DCONST_1;
import static org.objectweb.asm.Opcodes.DRETURN;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.ICONST_0;
import static org.objectweb.asm.Opcodes.ICONST_1;
import static org.objectweb.asm.Opcodes.NEWARRAY;
import static org.objectweb.asm.Opcodes.PUTFIELD;

import org.junit.Before;
import org.junit.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import de.tuberlin.uebb.jbop.optimizer.ClassNodeBuilder;
import de.tuberlin.uebb.jbop.optimizer.annotations.ImmutableArray;
import de.tuberlin.uebb.jbop.optimizer.utils.NodeHelper;

/**
 * Tests for {@link FinalFieldInliner}.
 * 
 * @author Christopher Ewest
 */
public class FinalFieldInlinerTest {
  
  private ClassNodeBuilder builder;
  private ClassNode classNode;
  private MethodNode method;
  
  private Object input;
  private final FinalFieldInliner inliner = new FinalFieldInliner();
  
  /**
   * Init for every test.
   */
  @Before
  public void before() {
    builder = ClassNodeBuilder.createClass("de.tuberlin.uebb.jbop.optimizer.var.FieldInlinerTestClass");//
    classNode = builder.getClassNode();
  }
  
  /**
   * Tests that FinalFieldInliner is working correctly for int-Fields.
   * 
   * @throws Exception
   *           the exception
   */
  @Test
  public void testFinalFieldInlinerIntField() throws Exception {
    // INIT
    initTestMethod("I", 42, 1);
    
    // RUN
    final InsnList optimized = inliner.optimize(method.instructions, method);
    
    // ASSERT
    assertEquals(2, optimized.size());
    assertEquals(42, NodeHelper.getNumberValue(optimized.get(0)).intValue());
  }
  
  /**
   * Tests that FinalFieldInliner is working correctly for double-Fields.
   * 
   * @throws Exception
   *           the exception
   */
  @Test
  public void testFinalFieldInlinerDoubleField() throws Exception {
    // INIT
    initTestMethod("D", 23.0, 2);
    
    // RUN
    final InsnList optimized = inliner.optimize(method.instructions, method);
    
    // ASSERT
    assertEquals(2, optimized.size());
    assertEquals(23.0, NodeHelper.getNumberValue(optimized.get(0)).doubleValue(), .0001);
  }
  
  /**
   * Tests that FinalFieldInliner is working correctly for long-Fields.
   * 
   * @throws Exception
   *           the exception
   */
  @Test
  public void testFinalFieldInlinerLongField() throws Exception {
    // INIT
    initTestMethod("J", 12L, 3);
    
    // RUN
    final InsnList optimized = inliner.optimize(method.instructions, method);
    
    // ASSERT
    assertEquals(2, optimized.size());
    assertEquals(12L, NodeHelper.getNumberValue(optimized.get(0)).longValue());
  }
  
  /**
   * Tests that FinalFieldInliner is working correctly for float-Fields.
   * 
   * @throws Exception
   *           the exception
   */
  @Test
  public void testFinalFieldInlinerFloatField() throws Exception {
    // INIT
    initTestMethod("F", 13.13F, 4);
    
    // RUN
    final InsnList optimized = inliner.optimize(method.instructions, method);
    
    // ASSERT
    assertEquals(2, optimized.size());
    assertEquals(13.13F, NodeHelper.getNumberValue(optimized.get(0)).floatValue(), .0001);
  }
  
  /**
   * Tests that FinalFieldInliner is working correctly for byte-Fields.
   * 
   * @throws Exception
   *           the exception
   */
  @Test
  public void testFinalFieldInlinerByteField() throws Exception {
    // INIT
    initTestMethod("B", (byte) 1, 5);
    
    // RUN
    final InsnList optimized = inliner.optimize(method.instructions, method);
    
    // ASSERT
    assertEquals(2, optimized.size());
    assertEquals((byte) 1, NodeHelper.getNumberValue(optimized.get(0)).byteValue());
  }
  
  /**
   * Tests that FinalFieldInliner is working correctly for short-Fields.
   * 
   * @throws Exception
   *           the exception
   */
  @Test
  public void testFinalFieldInlinerShortField() throws Exception {
    // INIT
    initTestMethod("S", (short) 3, 6);
    
    // RUN
    final InsnList optimized = inliner.optimize(method.instructions, method);
    
    // ASSERT
    assertEquals(2, optimized.size());
    assertEquals((short) 3, NodeHelper.getNumberValue(optimized.get(0)).byteValue());
  }
  
  /**
   * Tests that FinalFieldInliner is working correctly for boolean-Fields.
   * 
   * @throws Exception
   *           the exception
   */
  @Test
  public void testFinalFieldInlinerBooleanField() throws Exception {
    // INIT
    initTestMethod("Z", true, 7);
    
    // RUN
    final InsnList optimized = inliner.optimize(method.instructions, method);
    
    // ASSERT
    assertEquals(2, optimized.size());
    assertTrue(NodeHelper.getBooleanValue(optimized.get(0)));
  }
  
  /**
   * Tests that FinalFieldInliner is working correctly for boolean-Fields.
   * 
   * @throws Exception
   *           the exception
   */
  @Test
  public void testFinalFieldInlinerCharField() throws Exception {
    // INIT
    initTestMethod("C", 'C', 8);
    
    // RUN
    final InsnList optimized = inliner.optimize(method.instructions, method);
    
    // ASSERT
    assertEquals(2, optimized.size());
    assertEquals('C', NodeHelper.getCharValue(optimized.get(0)));
  }
  
  /**
   * Tests that finalFieldInliner is is working correctly for field-Chains with mutli-arrays.
   * 
   * @throws Exception
   *           the exception
   */
  @Test
  public void testFinalFieldInlinerFieldChainMultiArray() throws Exception {
    // INIT
    final ClassNodeBuilder builderC = ClassNodeBuilder.createClass("de.tuberlin.uebb.jbop.optimizer.var.CM")
        .addField("d", "[[D").withModifiers(ACC_PRIVATE, ACC_FINAL).withAnnotation(ImmutableArray.class)
        .addToConstructor(new VarInsnNode(ALOAD, 0)).//
        addToConstructor(new InsnNode(ICONST_1)).//
        addToConstructor(new TypeInsnNode(ANEWARRAY, "[D")).//
        addToConstructor(new InsnNode(DUP)).//
        addToConstructor(new InsnNode(ICONST_0)).//
        addToConstructor(new InsnNode(ICONST_1)).//
        addToConstructor(new IntInsnNode(NEWARRAY, Opcodes.T_DOUBLE)).//
        addToConstructor(new InsnNode(DUP)).//
        addToConstructor(new InsnNode(ICONST_0)).//
        addToConstructor(new InsnNode(DCONST_1)).//
        addToConstructor(new InsnNode(DASTORE)).//
        addToConstructor(new InsnNode(AASTORE)).//
        addToConstructor(new FieldInsnNode(PUTFIELD, "de/tuberlin/uebb/jbop/optimizer/var/CM", "d", "[[D")).//
        toClass();
    final ClassNodeBuilder builderB = ClassNodeBuilder.createClass("de.tuberlin.uebb.jbop.optimizer.var.BM")
        .addField("c", Type.getDescriptor(builderC.getBuildedClass())).withModifiers(ACC_PRIVATE, ACC_FINAL)
        .initWith(null).toClass();
    final ClassNodeBuilder builderA = ClassNodeBuilder.createClass("de.tuberlin.uebb.jbop.optimizer.var.AM")
        .addField("b", Type.getDescriptor(builderB.getBuildedClass())).withModifiers(ACC_PRIVATE, ACC_FINAL)
        .initWith(null).toClass();
    final ClassNodeBuilder builderTestClass = ClassNodeBuilder
        .createClass("de.tuberlin.uebb.jbop.optimizer.var.ChainedTestClassM")
        .addField("a", Type.getDescriptor(builderA.getBuildedClass())).withModifiers(ACC_PRIVATE, ACC_FINAL)
        .initWith(null).//
        addMethod("get", "()D").//
        addGetClassField("a").// ;
        addGetField(builderA, "b").//
        addGetField(builderB, "c").//
        addGetField(builderC, "d").//
        addInsn(new InsnNode(ICONST_0)).//
        addInsn(new InsnNode(AALOAD)).//
        addInsn(new InsnNode(ICONST_0)).//
        addInsn(new InsnNode(DALOAD)).//
        addInsn(new InsnNode(DRETURN));//
    
    // RUN
    final Object instance = builderTestClass.instance();
    
    inliner.setInputObject(instance);
    method = builderTestClass.getMethod("get");
    final InsnList optimized = inliner.optimize(method.instructions, method);
    
    // ASSERT
    assertEquals(2, optimized.size());
    assertEquals(1.0, NodeHelper.getNumberValue(optimized.get(0)).doubleValue(), .00001);
  }
  
  /**
   * Tests that finalFieldInliner is is working correctly for field-Chains with single-arrays.
   * 
   * @throws Exception
   *           the exception
   */
  @Test
  public void testFinalFieldInlinerFieldChainArray() throws Exception {
    // INIT
    final ClassNodeBuilder builderC = ClassNodeBuilder.createClass("de.tuberlin.uebb.jbop.optimizer.var.C")
        .addField("d", "[D").withModifiers(ACC_PRIVATE, ACC_FINAL).withAnnotation(ImmutableArray.class)
        .initArrayWith(1.0).//
        toClass();
    final ClassNodeBuilder builderB = ClassNodeBuilder.createClass("de.tuberlin.uebb.jbop.optimizer.var.B")
        .addField("c", Type.getDescriptor(builderC.getBuildedClass())).withModifiers(ACC_PRIVATE, ACC_FINAL)
        .initWith(null).toClass();
    final ClassNodeBuilder builderA = ClassNodeBuilder.createClass("de.tuberlin.uebb.jbop.optimizer.var.A")
        .addField("b", Type.getDescriptor(builderB.getBuildedClass())).withModifiers(ACC_PRIVATE, ACC_FINAL)
        .initWith(null).toClass();
    final ClassNodeBuilder builderTestClass = ClassNodeBuilder
        .createClass("de.tuberlin.uebb.jbop.optimizer.var.ChainedTestClass")
        .addField("a", Type.getDescriptor(builderA.getBuildedClass())).withModifiers(ACC_PRIVATE, ACC_FINAL)
        .initWith(null).//
        addMethod("get", "()D").//
        addGetClassField("a").// ;
        addGetField(builderA, "b").//
        addGetField(builderB, "c").//
        addGetField(builderC, "d").//
        addInsn(new InsnNode(ICONST_0)).//
        addInsn(new InsnNode(DALOAD)).//
        addInsn(new InsnNode(DRETURN));//
    
    // RUN
    final Object instance = builderTestClass.instance();
    
    inliner.setInputObject(instance);
    method = builderTestClass.getMethod("get");
    final InsnList optimized = inliner.optimize(method.instructions, method);
    
    // ASSERT
    assertEquals(2, optimized.size());
    assertEquals(1.0, NodeHelper.getNumberValue(optimized.get(0)).doubleValue(), .00001);
  }
  
  private void initTestMethod(final String desc, final Object value, final int testNumber) throws Exception {
    classNode.name += testNumber;
    builder.addField("field", desc).withModifiers(ACC_PRIVATE, ACC_FINAL).withGetter().initWith(value);
    input = builder.toClass().instance();
    inliner.setInputObject(input);
    method = builder.getMethod("getField");
  }
}
