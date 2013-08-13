package de.tuberlin.uebb.jbop.optimizer.methodsplitter;

import static org.junit.Assert.assertEquals;

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

public class MethodSplitterTest {
  
  private MethodSplitter splitter;
  private MethodNode method;
  private ClassNode classNode;
  private ClassNodeBuilder builder;
  private final int length = (8 * 1024) - 1;
  private final int arrayLength = 7500;
  
  @Before
  public void before() {
    builder = ClassNodeBuilder.createClass("de.tuberlin.uebb.jbop.optimizer.methodsplitter.SplitTestClass").//
        addMethod("testMethod", "()V");
    method = builder.getMethod("testMethod");
    classNode = builder.getClassNode();
    splitter = new MethodSplitter(classNode, length);
  }
  
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
    for (int i = 0; i < additionalMethods.size(); ++i) {
      final MethodNode methodNode = additionalMethods.get(i);
      assertEquals("([I)V", methodNode.desc);
    }
    
    // check that the class is valid by instantiating it
    method.instructions = splitted;
    classNode.methods.addAll(additionalMethods);
    builder.toClass().instance();
  }
  
  @Test
  public void testMethodSplitterCreateArrayAndReturn() throws Exception {
    // INIT
    classNode.name += "2";
    method.desc = "()[I";
    builder.addArray("[I", 5);//
    fillArray().// arrayref
        addInsn(new InsnNode(Opcodes.ARETURN));
    
    // RUN
    final InsnList splitted = splitter.optimize(method.instructions, method);
    final List<MethodNode> additionalMethods = splitter.getAdditionalMethods();
    
    // ASSERT
    // check methods
    for (int i = 0; i < (additionalMethods.size() - 1); ++i) {
      final MethodNode methodNode = additionalMethods.get(i);
      assertEquals("([I)V", methodNode.desc);
    }
    final MethodNode methodNode = additionalMethods.get(additionalMethods.size() - 1);
    assertEquals("([I)[I", methodNode.desc);
    
    // check that the class is valid by instantiating it
    method.instructions = splitted;
    classNode.methods.addAll(additionalMethods);
    
    builder.toClass().instance();
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
