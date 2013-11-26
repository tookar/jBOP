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
package de.tuberlin.uebb.jbop.optimizer.controlflow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.when;
import static org.objectweb.asm.Opcodes.DADD;
import static org.objectweb.asm.Opcodes.DCONST_0;
import static org.objectweb.asm.Opcodes.DLOAD;
import static org.objectweb.asm.Opcodes.DSTORE;
import static org.objectweb.asm.Opcodes.GOTO;
import static org.objectweb.asm.Opcodes.ICONST_0;
import static org.objectweb.asm.Opcodes.ICONST_1;
import static org.objectweb.asm.Opcodes.ICONST_2;
import static org.objectweb.asm.Opcodes.IF_ICMPGE;
import static org.objectweb.asm.Opcodes.IF_ICMPLT;
import static org.objectweb.asm.Opcodes.IINC;
import static org.objectweb.asm.Opcodes.ILOAD;
import static org.objectweb.asm.Opcodes.ISTORE;
import static org.objectweb.asm.Opcodes.NOP;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

import de.tuberlin.uebb.jbop.exception.JBOPClassException;
import de.tuberlin.uebb.jbop.optimizer.ClassNodeBuilder;
import de.tuberlin.uebb.jbop.optimizer.array.FieldArrayValueInliner;
import de.tuberlin.uebb.jbop.optimizer.array.NonNullArrayValue;
import de.tuberlin.uebb.jbop.optimizer.utils.NodeHelper;

/**
 * Tests for {@link ConstantIfInliner}.
 * 
 * @author Christopher Ewest
 */
@RunWith(MockitoJUnitRunner.class)
public class ConstantIfInlinerTest {
  
  @Mock
  private FieldArrayValueInliner arrayValue;
  @InjectMocks
  private ConstantIfInliner constantIfInliner;
  private final List<NonNullArrayValue> nonNullArrayValues = new ArrayList<>();
  private ClassNodeBuilder builder;
  private MethodNode method;
  @Mock
  private NonNullArrayValue nonNullValue;
  
  /**
   * Init for every test.
   */
  @Before
  public void before() {
    when(arrayValue.getNonNullArrayValues()).thenReturn(nonNullArrayValues);
    nonNullArrayValues.add(nonNullValue);
    builder = ClassNodeBuilder.createClass("de.tuberlin.uebb.jbop.optimizer.controlflow.ConstantIfTestClass").//
        addMethod("testIf", "()V");//
    method = builder.getMethod("testIf");
    
  }
  
  /**
   * Tests that constantIfInliner is working correctly.
   * 
   * Input is
   * 
   * <pre>
   * if(1<2)
   * ...
   * </pre>
   */
  
  @Test
  public void testConstantIfInlinerIF_CMPEG() throws JBOPClassException {
    // INIT
    final LabelNode label = new LabelNode();
    builder.addInsn(new InsnNode(Opcodes.ICONST_1)).//
        addInsn(new InsnNode(Opcodes.ICONST_2)).//
        addInsn(new JumpInsnNode(Opcodes.IF_ICMPGE, label)).//
        addInsn(new InsnNode(Opcodes.NOP)).//
        addInsn(label).//
        addInsn(new InsnNode(Opcodes.RETURN));
    
    // RUN
    assertEquals(6, method.instructions.size());
    final InsnList optimized = constantIfInliner.optimize(method.instructions, method);
    
    // ASSERT
    assertEquals(3, optimized.size());
    assertEquals(Opcodes.NOP, optimized.get(0).getOpcode());
  }
  
  /**
   * Tests that constantIfInliner is working correctly.
   * 
   * Input is
   * 
   * <pre>
   * if(1<=2)
   * ...
   * </pre>
   */
  
  @Test
  public void testConstantIfInlinerIF_ICMPGT() throws JBOPClassException {
    // INIT
    final LabelNode label = new LabelNode();
    builder.addInsn(new InsnNode(Opcodes.ICONST_1)).//
        addInsn(new InsnNode(Opcodes.ICONST_2)).//
        addInsn(new JumpInsnNode(Opcodes.IF_ICMPGT, label)).//
        addInsn(new InsnNode(Opcodes.NOP)).//
        addInsn(label).//
        addInsn(new InsnNode(Opcodes.RETURN));
    
    // RUN
    assertEquals(6, method.instructions.size());
    final InsnList optimized = constantIfInliner.optimize(method.instructions, method);
    
    // ASSERT
    assertEquals(3, optimized.size());
    assertEquals(Opcodes.NOP, optimized.get(0).getOpcode());
  }
  
  /**
   * Tests that constantIfInliner is working correctly.
   * 
   * Input is
   * 
   * <pre>
   * if(2>1)
   * ...
   * </pre>
   */
  
  @Test
  public void testConstantIfInlinerIF_ICMPLE() throws JBOPClassException {
    // INIT
    final LabelNode label = new LabelNode();
    builder.addInsn(new InsnNode(Opcodes.ICONST_2)).//
        addInsn(new InsnNode(Opcodes.ICONST_1)).//
        addInsn(new JumpInsnNode(Opcodes.IF_ICMPLE, label)).//
        addInsn(new InsnNode(Opcodes.NOP)).//
        addInsn(label).//
        addInsn(new InsnNode(Opcodes.RETURN));
    
    // RUN
    assertEquals(6, method.instructions.size());
    final InsnList optimized = constantIfInliner.optimize(method.instructions, method);
    
    // ASSERT
    assertEquals(3, optimized.size());
    assertEquals(Opcodes.NOP, optimized.get(0).getOpcode());
  }
  
  /**
   * Tests that constantIfInliner is working correctly.
   * 
   * Input is
   * 
   * <pre>
   * if(2>=1)
   * ...
   * </pre>
   */
  
  @Test
  public void testConstantIfInlinerIF_ICMPLT() throws JBOPClassException {
    // INIT
    final LabelNode label = new LabelNode();
    builder.addInsn(new InsnNode(Opcodes.ICONST_2)).//
        addInsn(new InsnNode(Opcodes.ICONST_1)).//
        addInsn(new JumpInsnNode(Opcodes.IF_ICMPLT, label)).//
        addInsn(new InsnNode(Opcodes.NOP)).//
        addInsn(label).//
        addInsn(new InsnNode(Opcodes.RETURN));
    
    // RUN
    assertEquals(6, method.instructions.size());
    final InsnList optimized = constantIfInliner.optimize(method.instructions, method);
    
    // ASSERT
    assertEquals(3, optimized.size());
    assertEquals(Opcodes.NOP, optimized.get(0).getOpcode());
  }
  
  /**
   * Tests that constantIfInliner is working correctly.
   * 
   * Input is
   * 
   * <pre>
   * if(1==1)
   * ...
   * </pre>
   */
  @Test
  public void testConstantIfInlinerIF_ICMPNE() throws JBOPClassException {
    // INIT
    final LabelNode label = new LabelNode();
    builder.addInsn(new InsnNode(Opcodes.ICONST_1)).//
        addInsn(new InsnNode(Opcodes.ICONST_1)).//
        addInsn(new JumpInsnNode(Opcodes.IF_ICMPNE, label)).//
        addInsn(new InsnNode(Opcodes.NOP)).//
        addInsn(label).//
        addInsn(new InsnNode(Opcodes.RETURN));
    
    // RUN
    assertEquals(6, method.instructions.size());
    final InsnList optimized = constantIfInliner.optimize(method.instructions, method);
    
    // ASSERT
    assertEquals(3, optimized.size());
    assertEquals(Opcodes.NOP, optimized.get(0).getOpcode());
  }
  
  /**
   * Tests that constantIfInliner is working correctly.
   * 
   * Input is
   * 
   * <pre>
   * if(1!=2)
   * ...
   * </pre>
   */
  @Test
  public void testConstantIfInlinerIF_ICMPEQ() throws JBOPClassException {
    // INIT
    final LabelNode label = new LabelNode();
    builder.addInsn(new InsnNode(Opcodes.ICONST_1)).//
        addInsn(new InsnNode(Opcodes.ICONST_2)).//
        addInsn(new JumpInsnNode(Opcodes.IF_ICMPEQ, label)).//
        addInsn(new InsnNode(Opcodes.NOP)).//
        addInsn(label).//
        addInsn(new InsnNode(Opcodes.RETURN));
    
    // RUN
    assertEquals(6, method.instructions.size());
    final InsnList optimized = constantIfInliner.optimize(method.instructions, method);
    
    // ASSERT
    assertEquals(3, optimized.size());
    assertEquals(Opcodes.NOP, optimized.get(0).getOpcode());
  }
  
  /**
   * Tests that constantIfInliner is working correctly.
   * 
   * Input is
   * 
   * <pre>
   * if(a)
   * ...
   * </pre>
   * 
   * where a is true
   */
  @Test
  public void testConstantIfInlinerIFEQ() throws JBOPClassException {
    // INIT
    final LabelNode label = new LabelNode();
    builder.addInsn(new InsnNode(Opcodes.ICONST_1)).//
        addInsn(new JumpInsnNode(Opcodes.IFEQ, label)).//
        addInsn(new InsnNode(Opcodes.NOP)).//
        addInsn(label).//
        addInsn(new InsnNode(Opcodes.RETURN));
    
    // RUN
    assertEquals(5, method.instructions.size());
    final InsnList optimized = constantIfInliner.optimize(method.instructions, method);
    
    // ASSERT
    assertEquals(3, optimized.size());
    assertEquals(Opcodes.NOP, optimized.get(0).getOpcode());
  }
  
  /**
   * Tests that constantIfInliner is working correctly.
   * 
   * Input is
   * 
   * <pre>
   * if(!a)
   * ...
   * </pre>
   * 
   * where a is false
   */
  @Test
  public void testConstantIfInlinerIFNE() throws JBOPClassException {
    // INIT
    final LabelNode label = new LabelNode();
    builder.addInsn(new InsnNode(Opcodes.ICONST_0)).//
        addInsn(new JumpInsnNode(Opcodes.IFNE, label)).//
        addInsn(new InsnNode(Opcodes.NOP)).//
        addInsn(label).//
        addInsn(new InsnNode(Opcodes.RETURN));
    
    // RUN
    assertEquals(5, method.instructions.size());
    final InsnList optimized = constantIfInliner.optimize(method.instructions, method);
    
    // ASSERT
    assertEquals(3, optimized.size());
    assertEquals(Opcodes.NOP, optimized.get(0).getOpcode());
  }
  
  /**
   * Tests that constantIfInliner is working correctly.
   * 
   * Input is
   * 
   * <pre>
   * if(a==null)
   * ...
   * </pre>
   * 
   * where a is not null
   */
  @Test
  public void testConstantIfInlinerIFNULL() throws JBOPClassException {
    // INIT
    when(
        nonNullValue.is(Matchers.<AbstractInsnNode> any(), Matchers.<AbstractInsnNode> any(),
            Matchers.<List<AbstractInsnNode>> any(), Matchers.<List<AbstractInsnNode>> any())).thenReturn(
        Boolean.valueOf(true));
    final LabelNode label = new LabelNode();
    builder.addInsn(new TypeInsnNode(Opcodes.NEW, Type.getDescriptor(Object.class))).//
        addInsn(new JumpInsnNode(Opcodes.IFNULL, label)).//
        addInsn(new InsnNode(Opcodes.NOP)).//
        addInsn(label).//
        addInsn(new InsnNode(Opcodes.RETURN));
    
    // RUN
    assertEquals(5, method.instructions.size());
    final InsnList optimized = constantIfInliner.optimize(method.instructions, method);
    
    // ASSERT
    assertEquals(3, optimized.size());
    assertEquals(Opcodes.NOP, optimized.get(0).getOpcode());
  }
  
  /**
   * Tests that constantIfInliner is working correctly.
   * 
   * Input is
   * 
   * <pre>
   * if(a==null)
   * ...
   * </pre>
   * 
   * where a is null
   */
  @Test
  public void testConstantIfInlinerIFNONNULL() throws JBOPClassException {
    // INIT
    final LabelNode label = new LabelNode();
    builder.addInsn(new InsnNode(Opcodes.ACONST_NULL)).//
        addInsn(new JumpInsnNode(Opcodes.IFNONNULL, label)).//
        addInsn(new InsnNode(Opcodes.NOP)).//
        addInsn(label).//
        addInsn(new InsnNode(Opcodes.RETURN));
    
    // RUN
    assertEquals(5, method.instructions.size());
    final InsnList optimized = constantIfInliner.optimize(method.instructions, method);
    
    // ASSERT
    assertEquals(3, optimized.size());
    assertEquals(Opcodes.NOP, optimized.get(0).getOpcode());
  }
  
  /**
   * Tests that constantIfInliner is working correctly.
   * 
   * Input is
   * 
   * <pre>
   * if(1>2)
   * ...
   * else
   * ...
   * </pre>
   */
  @Test
  public void testConstantIfInlinerIF_ICMPLEWithElseChooseIf() throws JBOPClassException {
    // INIT
    final LabelNode label = new LabelNode();
    final LabelNode label2 = new LabelNode();
    builder.addInsn(new InsnNode(Opcodes.ICONST_2)).//
        addInsn(new InsnNode(Opcodes.ICONST_1)).//
        addInsn(new JumpInsnNode(Opcodes.IF_ICMPLE, label)).//
        addInsn(new InsnNode(Opcodes.NOP)).//
        addInsn(new InsnNode(Opcodes.NOP)).//
        addInsn(new JumpInsnNode(Opcodes.GOTO, label2)).//
        addInsn(label).//
        addInsn(new InsnNode(Opcodes.NOP)).//
        addInsn(label2).//
        addInsn(new InsnNode(Opcodes.RETURN));
    
    // RUN
    assertEquals(10, method.instructions.size());
    final InsnList optimized = constantIfInliner.optimize(method.instructions, method);
    
    // ASSERT
    assertEquals(4, optimized.size());
    assertEquals(Opcodes.NOP, optimized.get(0).getOpcode());
    assertEquals(Opcodes.NOP, optimized.get(1).getOpcode());
  }
  
  @Test
  public void test() throws JBOPClassException {
    final LabelNode label = new LabelNode();
    builder.add(DCONST_0).//
        add(DSTORE, 1).//
        addInsn(label).//
        add(NOP).//
        add(ICONST_1).add(ICONST_2).add(IF_ICMPLT, label).//
        add(DLOAD, 1).//
        addReturn();
    final InsnList optimized = constantIfInliner.optimize(method.instructions, method);
    method.instructions = optimized;
  }
  
  /**
   * Tests that constantIfInliner is working correctly.
   * 
   * Input is
   * 
   * <pre>
   * if(2>1)
   * ...
   * else
   * ...
   * </pre>
   */
  @Test
  public void testConstantIfInlinerIF_ICMPLEWithElseChooseElse() throws JBOPClassException {
    // INIT
    final LabelNode label = new LabelNode();
    final LabelNode label2 = new LabelNode();
    builder.addInsn(new InsnNode(Opcodes.ICONST_2)).//
        addInsn(new InsnNode(Opcodes.ICONST_1)).//
        addInsn(new JumpInsnNode(Opcodes.IF_ICMPLE, label)).//
        addInsn(new InsnNode(Opcodes.NOP)).//
        addInsn(new JumpInsnNode(Opcodes.GOTO, label2)).//
        addInsn(label).//
        addInsn(new InsnNode(Opcodes.NOP)).//
        addInsn(new InsnNode(Opcodes.NOP)).//
        addInsn(label2).//
        addInsn(new InsnNode(Opcodes.RETURN));
    
    // RUN
    assertEquals(10, method.instructions.size());
    final InsnList optimized = constantIfInliner.optimize(method.instructions, method);
    
    // ASSERT
    assertEquals(3, optimized.size());
    assertEquals(Opcodes.NOP, optimized.get(0).getOpcode());
    assertEquals(-1, optimized.get(1).getOpcode());
  }
  
  @Test
  public void testLocalVarInlinerLoop() throws JBOPClassException {
    // INIT
    final LabelNode label1 = new LabelNode();
    final LabelNode label2 = new LabelNode();
    builder.add(DCONST_0).//
        add(DSTORE, 2).//
        add(ICONST_1).//
        add(ISTORE, 1).//
        add(GOTO, label1).//
        addInsn(label2).//
        add(DCONST_0).//
        add(DLOAD, 2).//
        add(DADD).//
        add(DSTORE, 2).//
        add(IINC, 1, 1).//
        addInsn(label1).//
        addInsn(NodeHelper.getInsnNodeFor(3)).//
        add(ILOAD, 1).//
        add(Opcodes.IF_ICMPLT, label2).//
        add(DLOAD, 2).//
        addReturn();
    
    // RUN
    constantIfInliner.optimize(method.instructions, method);
    
    // ASSERT
    assertFalse(constantIfInliner.isOptimized());
  }
  
  @Test
  public void testLocalVarInlinerAlternativeLoop() throws JBOPClassException {
    // INIT
    final LabelNode check = new LabelNode();
    final LabelNode loopEnd = new LabelNode();
    builder.add(ICONST_0).//
        add(ISTORE, 1).//
        addInsn(check).//
        add(ICONST_2).//
        add(ILOAD, 1).//
        add(IF_ICMPGE, loopEnd).//
        add(NOP).//
        add(IINC, 1, 1).//
        add(GOTO, check).//
        addInsn(loopEnd).//
        addReturn();
    
    // RUN
    constantIfInliner.optimize(method.instructions, method);
    
    // ASSERT
    assertFalse(constantIfInliner.isOptimized());
  }
}
