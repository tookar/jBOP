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
package de.tuberlin.uebb.jbop.optimizer.utils;

import static org.junit.Assert.assertEquals;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.DALOAD;
import static org.objectweb.asm.Opcodes.DASTORE;
import static org.objectweb.asm.Opcodes.DDIV;
import static org.objectweb.asm.Opcodes.DLOAD;
import static org.objectweb.asm.Opcodes.DREM;
import static org.objectweb.asm.Opcodes.DSTORE;
import static org.objectweb.asm.Opcodes.DSUB;
import static org.objectweb.asm.Opcodes.GOTO;
import static org.objectweb.asm.Opcodes.IADD;
import static org.objectweb.asm.Opcodes.IALOAD;
import static org.objectweb.asm.Opcodes.ICONST_0;
import static org.objectweb.asm.Opcodes.ICONST_1;
import static org.objectweb.asm.Opcodes.IFEQ;
import static org.objectweb.asm.Opcodes.ILOAD;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.ISTORE;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

import org.junit.Ignore;
import org.junit.Test;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import de.tuberlin.uebb.jbop.optimizer.ClassNodeBuilder;

/**
 * Tests for {@link NodeHelper}.
 * 
 * @author Christopher Ewest
 */
public class NodeHelperTest {
  
  @Test
  public void testGetFirstOfStack1() {
    // INIT
    final ClassNodeBuilder builder = ClassNodeBuilder.createClass("de.tuberlin.uebb.example.Test").//
        addMethod("test", "(I)V").//
        add(ILOAD, 1).//
        add(ISTORE, 2).//
        addReturn();
    final MethodNode method = builder.getMethod("test");
    final AbstractInsnNode store = method.instructions.get(method.instructions.size() - 2);
    
    // RUN
    final AbstractInsnNode firstOfStack = NodeHelper.getFirstOfStack(store);
    
    // ASSERT
    assertEquals(method.instructions.get(0), firstOfStack);
  }
  
  @Test
  public void testGetFirstOfStack2() {
    // INIT
    final ClassNodeBuilder builder = ClassNodeBuilder.createClass("de.tuberlin.uebb.example.Test").//
        addMethod("test", "(I)V").//
        add(ICONST_0).//
        add(ISTORE, 2).//
        add(ILOAD, 1).//
        add(ISTORE, 2).//
        addReturn();
    final MethodNode method = builder.getMethod("test");
    final AbstractInsnNode store = method.instructions.get(method.instructions.size() - 2);
    
    // RUN
    final AbstractInsnNode firstOfStack = NodeHelper.getFirstOfStack(store);
    
    // ASSERT
    assertEquals(store.getPrevious(), firstOfStack);
  }
  
  @Test
  public void testGetFirstOfStack3() {
    // INIT
    final ClassNodeBuilder builder = ClassNodeBuilder.createClass("de.tuberlin.uebb.example.Test").//
        addMethod("offset", "(I)I").//
        add(ICONST_0).//
        addReturn().//
        addMethod("test", "(I)V").//
        add(ICONST_0).//
        add(ISTORE, 2).//
        invoke(INVOKEVIRTUAL, "offset", ICONST_1).//
        add(ISTORE, 2).//
        addReturn();
    final MethodNode method = builder.getMethod("test");
    final AbstractInsnNode store = method.instructions.get(method.instructions.size() - 2);
    
    // RUN
    final AbstractInsnNode firstOfStack = NodeHelper.getFirstOfStack(store);
    
    // ASSERT
    assertEquals(method.instructions.get(2), firstOfStack);
  }
  
  @Test
  public void testGetFirstOfStack4() {
    // INIT
    final LabelNode label1 = new LabelNode();
    final LabelNode label2 = new LabelNode();
    final ClassNodeBuilder builder = ClassNodeBuilder.createClass("de.tuberlin.uebb.example.Test").//
        addField("array", "[I").//
        addMethod("test", "(I)V").//
        loadFieldArrayValue("array", 0).//
        add(IFEQ, label1).//
        add(ICONST_0).//
        add(ISTORE, 2).//
        add(GOTO, label2).//
        addInsn(label1).//
        add(ICONST_1).//
        add(ISTORE, 2).//
        addInsn(label2).//
        add(ILOAD, 2).//
        add(ILOAD, 1).//
        add(IADD).//
        add(ISTORE, 2).//
        addReturn();
    final MethodNode method = builder.getMethod("test");
    final AbstractInsnNode store = method.instructions.get(method.instructions.size() - 2);
    
    // RUN
    final AbstractInsnNode firstOfStack = NodeHelper.getFirstOfStack(store);
    
    // ASSERT
    assertEquals(method.instructions.get(12), firstOfStack);
  }
  
  @Test
  public void testGetFirstOfStack5() {
    // INIT
    final ClassNodeBuilder builder = ClassNodeBuilder.createClass("de.tuberlin.uebb.example.Test").//
        addMethod("remainder", "([D[D[D)V").//
        add(ALOAD, 1).//
        add(ICONST_0).//
        add(DALOAD).//
        add(ALOAD, 2).//
        add(ICONST_0).//
        add(DALOAD).//
        add(DREM).//
        add(DSTORE, 4).//
        add(ALOAD, 1).//
        add(ICONST_0).//
        add(DALOAD).//
        add(DLOAD, 4).//
        add(DSUB).//
        add(ALOAD, 2).//
        add(ICONST_0).//
        add(DALOAD).//
        add(DDIV).//
        addInsn(new MethodInsnNode(INVOKESTATIC, "org/apache/commons/math3/util/FastMath", "rint", "(D)D")).//
        add(DSTORE, 6).//
        add(ALOAD, 3).//
        add(ICONST_0).//
        add(DLOAD, 4).//
        add(DASTORE).//
        add(ICONST_1).//
        add(ISTORE, 8).//
        addReturn();
    final MethodNode method = builder.getMethod("remainder");
    assertEquals(26, method.instructions.size());
    
    // RUN
    final AbstractInsnNode store = method.instructions.get(24);
    final AbstractInsnNode firstOfStack = NodeHelper.getFirstOfStack(store);
    
    // ASSERT
    final AbstractInsnNode first = method.instructions.get(23);
    assertEquals(first, firstOfStack);
    
    // RUN
    final AbstractInsnNode store2 = method.instructions.get(18);
    final AbstractInsnNode firstOfStack2 = NodeHelper.getFirstOfStack(store2);
    
    // ASSERT
    final AbstractInsnNode first2 = method.instructions.get(8);
    assertEquals(method.instructions.indexOf(first2), method.instructions.indexOf(firstOfStack2));
  }
  
  @Test
  public void testGetFirstOfStackN() {
    // INIT
    final LabelNode label1 = new LabelNode();
    final LabelNode label2 = new LabelNode();
    final ClassNodeBuilder builder = ClassNodeBuilder.createClass("de.tuberlin.uebb.example.Test").//
        addEmptyMethod("initArray").//
        addMethod("offset", "(I)I").//
        add(ICONST_0).//
        addReturn().//
        addField("array", "[[I").//
        addMethod("test", "(I)V").//
        loadFieldArrayValue("array", 0).//
        add(ILOAD, 1).//
        add(IFEQ, label1).invoke(INVOKEVIRTUAL, "offset", ICONST_0).//
        add(GOTO, label2).//
        addInsn(label1).//
        invoke(INVOKEVIRTUAL, "initArray").//
        invoke(INVOKEVIRTUAL, "offset", ICONST_1).//
        addInsn(label2).//
        add(ILOAD, 1).//
        add(IADD).//
        addInsn(new InsnNode(IALOAD)).//
        add(ISTORE, 2).//
        addReturn();
    final MethodNode method = builder.getMethod("test");
    final AbstractInsnNode store = method.instructions.get(method.instructions.size() - 2);
    
    // RUN
    final AbstractInsnNode firstOfStack = NodeHelper.getFirstOfStack(store);
    
    // ASSERT
    assertEquals(method.instructions.get(0), firstOfStack);
  }
  
  /**
   * Tests that printMethod() of the Testobject is working correctly.
   * 
   * @throws UnsupportedEncodingException
   *           the unsupported encoding exception
   */
  @Test
  @Ignore
  public void testPrintMethod() throws UnsupportedEncodingException {
    // INIT
    final ClassNodeBuilder classNodeBuilder = ClassNodeBuilder.createClass("de.tuberlin.Class").//
        addField("f1", "I").initWith(1).//
        addField("f2", "D").initWith(2.0).//
        addField("f3", "[D").initArrayWith(1.0, 2.0);
    final MethodNode node = classNodeBuilder.//
        getMethod("<init>");
    // RUN
    final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    final PrintStream printStream = new PrintStream(byteArrayOutputStream);
    NodeHelper.printMethod(node, classNodeBuilder.getClassNode(), printStream, false);
    
    final ByteArrayOutputStream byteArrayOutputStream2 = new ByteArrayOutputStream();
    final PrintStream printStream2 = new PrintStream(byteArrayOutputStream2);
    NodeHelper.printMethod(node, classNodeBuilder.getClassNode(), printStream2, true);
    
    // ASSERT
    final String methodContent = byteArrayOutputStream.toString("UTF8");
    assertEquals(
        "de.tuberlin.Class.<init>()V\n    ALOAD 0\n" + "    INVOKESPECIAL java/lang/Object.<init> ()V\n"
            + "    ALOAD 0\n" + "    ICONST_1\n" + "    PUTFIELD de/tuberlin/Class.f1 : I\n" + "    ALOAD 0\n"
            + "    LDC 2.0\n" + "    PUTFIELD de/tuberlin/Class.f2 : D\n" + "    ALOAD 0\n" + "    ICONST_2\n"
            + "    NEWARRAY T_DOUBLE\n" + "    ASTORE 1\n" + "    ALOAD 1\n"
            + "    PUTFIELD de/tuberlin/Class.f3 : [D\n" + "    ALOAD 1\n" + "    ICONST_0\n" + "    DCONST_1\n"
            + "    DASTORE\n" + "    ALOAD 1\n" + "    ICONST_1\n" + "    LDC 2.0\n" + "    DASTORE\n" + "    RETURN\n"
            + "    MAXSTACK = 0\n" + "    MAXLOCALS = 0\n" + "\n" + "", methodContent);
    
    // ASSERT
    final String methodContent2 = byteArrayOutputStream2.toString("UTF8");
    assertEquals("ClassNodeBuilder builder = ClassNodeBuilder.createClass(\"de.tuberlin.Class\");\n" + "builder.//\n"
        + "addMethod(\"<init>\", \"()V\").//\n" + "add(Opcodes.ALOAD, 0).//\n"
        + "invoke(\"Opcodes.INVOKESPECIAL\", \"java/lang/Object\", \"<init>\", \"()V\").//\n"
        + "add(Opcodes.ALOAD, 0).//\n" + "add(Opcodes.ICONST_1).//\n"
        + "addPutField(\"de/tuberlin/Class\", \"f1\", \"I\").//\n" + "add(Opcodes.ALOAD, 0).//\n"
        + "add(LDC, 2.0).//\n" + "addPutField(\"de/tuberlin/Class\", \"f2\", \"D\").//\n"
        + "add(Opcodes.ALOAD, 0).//\n" + "add(Opcodes.ICONST_2).//\n" + "add(Opcodes.NEWARRAY, T_DOUBLE).//\n"
        + "add(Opcodes.ASTORE, 1).//\n" + "add(Opcodes.ALOAD, 1).//\n"
        + "addPutField(\"de/tuberlin/Class\", \"f3\", \"[D\").//\n" + "add(Opcodes.ALOAD, 1).//\n"
        + "add(Opcodes.ICONST_0).//\n" + "add(Opcodes.DCONST_1).//\n" + "add(Opcodes.DASTORE).//\n"
        + "add(Opcodes.ALOAD, 1).//\n" + "add(Opcodes.ICONST_1).//\n" + "add(LDC, 2.0).//\n"
        + "add(Opcodes.DASTORE).//\n" + "addReturn().//\n\n", methodContent2);
  }
}
