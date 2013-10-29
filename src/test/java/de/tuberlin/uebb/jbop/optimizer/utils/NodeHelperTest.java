package de.tuberlin.uebb.jbop.optimizer.utils;

import static org.junit.Assert.assertEquals;
import static org.objectweb.asm.Opcodes.GOTO;
import static org.objectweb.asm.Opcodes.IADD;
import static org.objectweb.asm.Opcodes.IALOAD;
import static org.objectweb.asm.Opcodes.ICONST_0;
import static org.objectweb.asm.Opcodes.ICONST_1;
import static org.objectweb.asm.Opcodes.IFEQ;
import static org.objectweb.asm.Opcodes.ILOAD;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.ISTORE;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

import org.junit.Test;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;

import de.tuberlin.uebb.jbop.optimizer.ClassNodeBuilder;

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
  
  @Test
  public void testPrintMethod() throws UnsupportedEncodingException {
    // INIT
    final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    System.setOut(new PrintStream(byteArrayOutputStream));
    final MethodNode node = ClassNodeBuilder.createClass("de.tuberlin.Class").//
        addField("f1", "I").initWith(1).//
        addField("f2", "D").initWith(2.0).//
        addField("f3", "[D").initArrayWith(1.0, 2.0).//
        getMethod("<init>");
    // RUN
    NodeHelper.printMethod(node);
    
    // ASSERT
    final String methodContent = byteArrayOutputStream.toString("UTF8");
    assertEquals("    ALOAD 0\n" + "    INVOKESPECIAL java/lang/Object.<init> ()V\n" + "    ALOAD 0\n"
        + "    ICONST_1\n" + "    PUTFIELD de/tuberlin/Class.f1 : I\n" + "    ALOAD 0\n" + "    LDC 2.0\n"
        + "    PUTFIELD de/tuberlin/Class.f2 : D\n" + "    ALOAD 0\n" + "    ICONST_2\n" + "    NEWARRAY T_DOUBLE\n"
        + "    ASTORE 1\n" + "    ALOAD 1\n" + "    PUTFIELD de/tuberlin/Class.f3 : [D\n" + "    ALOAD 1\n"
        + "    ICONST_0\n" + "    DCONST_1\n" + "    DASTORE\n" + "    ALOAD 1\n" + "    ICONST_1\n" + "    LDC 2.0\n"
        + "    DASTORE\n" + "    RETURN\n" + "    MAXSTACK = 0\n" + "    MAXLOCALS = 0\n" + "\n" + "", methodContent);
  }
}
