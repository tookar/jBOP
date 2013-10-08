package de.tuberlin.uebb.jbop.optimizer.utils;

import static org.junit.Assert.assertEquals;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.ACC_SYNTHETIC;
import static org.objectweb.asm.Opcodes.GOTO;
import static org.objectweb.asm.Opcodes.IADD;
import static org.objectweb.asm.Opcodes.ICONST_1;
import static org.objectweb.asm.Opcodes.IFEQ;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;

import org.junit.Test;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import de.tuberlin.uebb.jbop.optimizer.ClassNodeBuilder;

public class RemoveUnusedFieldsTest {
  
  private ClassNodeBuilder classBuilder;
  private ClassNode classNode;
  
  @Test
  public void test() throws Exception {
    // INIT
    classBuilder = ClassNodeBuilder.createClass("de.tuberlin.uebb.jbop.optimizer.utils.TestClass").//
        addField("field1", "D", 1.0).withGetter().//
        addField("field2", "D").withSetter().//
        addField("field3", "D").withGetterAndSetter().//
        addField("field4", "D", 1.0).//
        addField("field5", "D", 1.0).withModifiers(ACC_PUBLIC).//
        addField("field6", "D", 1.0).withModifiers(ACC_PRIVATE, ACC_SYNTHETIC).//
        addField("field7", "D").initWith(2.0);//
    classNode = classBuilder.getClassNode();
    
    // RUN
    RemoveUnusedFields.removeUnusedFields(classNode);
    
    // ASSERT
    assertEquals(5, classNode.fields.size());
    assertEquals("field1", classNode.fields.get(0).name);
    assertEquals("field2", classNode.fields.get(1).name);
    assertEquals("field3", classNode.fields.get(2).name);
    assertEquals("field5", classNode.fields.get(3).name);
    assertEquals("field6", classNode.fields.get(4).name);
    
    classBuilder.instance();
  }
  
  @Test
  public void test2() throws Exception {
    // INIT
    classBuilder = ClassNodeBuilder.createClass("de.tuberlin.uebb.jbop.optimizer.utils.TestClass2").addConstructor(
        "(I)V", "()V");
    classBuilder.//
        addField("field1", "I").//
        addField("field2", "I").//
        addField("field3", "I").//
        addMethod("x", "()I").//
        add(ICONST_1).addReturn().//
        addMethod("y", "()I", ACC_STATIC).//
        add(ICONST_1).addReturn().//
        selectConstructor("(I)V").//
        load(0).//
        load(0).//
        invoke(INVOKESPECIAL, "x").//
        addPutField(classBuilder, "field1").//
        load(0).//
        load(0).//
        addGetField(classBuilder, "field1").//
        invoke(INVOKESTATIC, "y").//
        add(IADD).//
        addPutField(classBuilder, "field2").//
        load(0).//
        load(0).//
        addGetField(classBuilder, "field2").//
        load(0).//
        addGetField(classBuilder, "field1").//
        add(IADD).//
        load(1).//
        addInsn(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/Object", "hashCode", "()I")).//
        add(IADD).//
        addPutField(classBuilder, "field3");
    classNode = classBuilder.getClassNode();
    // RUN
    RemoveUnusedFields.removeUnusedFields(classNode);
    
    // ASSERT
    assertEquals(0, classNode.fields.size());
    final MethodNode constructor = classBuilder.getMethod("<init>");
    assertEquals(3, constructor.instructions.size());
    
    classBuilder.instance();
  }
  
  /**
   * Test the removal code against if-branches.
   * 
   * @throws Exception
   */
  @Test
  public void test3() throws Exception {
    // INIT
    final LabelNode label_then = new LabelNode();
    final LabelNode label_endif = new LabelNode();
    
    classBuilder = ClassNodeBuilder.createClass("de.tuberlin.uebb.jbop.optimizer.utils.TestClass3").//
        addField("field1", "I");
    
    final MethodNode constructor = classBuilder.addConstructor("(II)V", "()V").//
        load(0).//
        load(1).//
        add(IFEQ, label_then).//
        load(2).//
        addPutField(classBuilder, "field1").//
        add(GOTO, label_endif).//
        addInsn(label_then).//
        load(1).//
        addPutField(classBuilder, "field1").//
        addInsn(label_endif).//
        getConstructor("(II)V");
    
    classNode = classBuilder.getClassNode();
    
    // RUN
    RemoveUnusedFields.removeUnusedFields(classNode);
    
    assertEquals(5, constructor.instructions.size()); // 3 nodes + 2 labels
    
    assertEquals(0, classNode.fields.size());
    
    classBuilder.instance();
  }
  
  /**
   * Test the removal code against if-branches.
   * 
   * @throws Exception
   */
  @Test
  public void test3b() throws Exception {
    // INIT
    final LabelNode label_then = new LabelNode();
    final LabelNode label_endif = new LabelNode();
    
    classBuilder = ClassNodeBuilder.createClass("de.tuberlin.uebb.jbop.optimizer.utils.TestClass3b").//
        addField("field1", "I");
    
    final MethodNode constructor = classBuilder.addConstructor("(II)V", "()V").//
        load(1).//
        add(IFEQ, label_then).//
        load(0).//
        load(2).//
        addPutField(classBuilder, "field1").//
        add(GOTO, label_endif).//
        addInsn(label_then).//
        load(0).//
        load(1).//
        addPutField(classBuilder, "field1").//
        addInsn(label_endif).//
        getConstructor("(II)V");
    
    classNode = classBuilder.getClassNode();
    
    // RUN
    RemoveUnusedFields.removeUnusedFields(classNode);
    
    assertEquals(8, constructor.instructions.size()); // 6 nodes + 2 labels
    
    assertEquals(0, classNode.fields.size());
    
    classBuilder.instance();
  }
}
