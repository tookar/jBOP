package de.tuberlin.uebb.jbop.optimizer.utils;

import static org.junit.Assert.assertEquals;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_SYNTHETIC;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.GETFIELD;
import static org.objectweb.asm.Opcodes.IADD;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.PUTFIELD;

import org.junit.Test;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

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
    final String owner = "de/tuberlin/uebb/jbop/optimizer/utils/TestClass2";
    classBuilder = ClassNodeBuilder.createClass("de.tuberlin.uebb.jbop.optimizer.utils.TestClass2").//
        addField("field1", "I").//
        addField("field2", "I").//
        addField("field3", "I").//
        addToConstructor(new VarInsnNode(ALOAD, 0)).//
        addToConstructor(new VarInsnNode(ALOAD, 0)).//
        addToConstructor(new MethodInsnNode(INVOKESPECIAL, owner, "x", "()I")).//
        addToConstructor(new FieldInsnNode(PUTFIELD, owner, "field1", "I")).//
        addToConstructor(new VarInsnNode(ALOAD, 0)).//
        addToConstructor(new VarInsnNode(ALOAD, 0)).//
        addToConstructor(new FieldInsnNode(GETFIELD, owner, "field1", "I")).//
        addToConstructor(new MethodInsnNode(INVOKESTATIC, owner, "y", "()I")).//
        addToConstructor(new InsnNode(IADD)).//
        addToConstructor(new FieldInsnNode(PUTFIELD, owner, "field2", "I")).//
        addToConstructor(new VarInsnNode(ALOAD, 0)).//
        addToConstructor(new VarInsnNode(ALOAD, 0)).//
        addToConstructor(new FieldInsnNode(GETFIELD, owner, "field2", "I")).//
        addToConstructor(new VarInsnNode(ALOAD, 0)).//
        addToConstructor(new FieldInsnNode(GETFIELD, owner, "field1", "I")).//
        addToConstructor(new InsnNode(IADD)).//
        addToConstructor(new VarInsnNode(ALOAD, 1)).//
        addToConstructor(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/Object", "hasCode", "()I")).//
        addToConstructor(new InsnNode(IADD)).//
        addToConstructor(new FieldInsnNode(PUTFIELD, owner, "field3", "I"));//
    classNode = classBuilder.getClassNode();
    // RUN
    RemoveUnusedFields.removeUnusedFields(classNode);
    
    // ASSERT
    assertEquals(0, classNode.fields.size());
    final MethodNode constructor = classBuilder.getMethod("<init>");
    assertEquals(3, constructor.instructions.size());
    
    classBuilder.instance();
  }
}
