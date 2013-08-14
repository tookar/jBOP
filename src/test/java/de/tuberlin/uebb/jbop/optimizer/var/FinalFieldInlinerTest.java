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
import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;

import org.junit.Before;
import org.junit.Test;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;

import de.tuberlin.uebb.jbop.optimizer.ClassNodeBuilder;
import de.tuberlin.uebb.jbop.optimizer.utils.NodeHelper;

public class FinalFieldInlinerTest {
  
  private ClassNodeBuilder builder;
  private ClassNode classNode;
  private MethodNode method;
  
  private Object input;
  private FinalFieldInliner inliner;
  
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
  
  private void initTestMethod(final String desc, final Object value, final int testNumber) throws Exception {
    classNode.name += testNumber;
    builder.addField("field", desc).withModifiers(ACC_PRIVATE, ACC_FINAL).withGetter().initWith(value);
    input = builder.toClass().instance();
    inliner = new FinalFieldInliner(input);
    method = builder.getMethod("getField");
  }
}
