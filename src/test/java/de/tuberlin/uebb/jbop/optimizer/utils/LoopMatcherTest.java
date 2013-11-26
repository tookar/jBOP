package de.tuberlin.uebb.jbop.optimizer.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.objectweb.asm.Opcodes.GOTO;
import static org.objectweb.asm.Opcodes.ICONST_0;
import static org.objectweb.asm.Opcodes.ICONST_1;
import static org.objectweb.asm.Opcodes.ICONST_5;
import static org.objectweb.asm.Opcodes.IFNE;
import static org.objectweb.asm.Opcodes.IF_ICMPGT;
import static org.objectweb.asm.Opcodes.IF_ICMPLE;
import static org.objectweb.asm.Opcodes.IINC;
import static org.objectweb.asm.Opcodes.ILOAD;
import static org.objectweb.asm.Opcodes.ISTORE;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;

import de.tuberlin.uebb.jbop.optimizer.ClassNodeBuilder;

public class LoopMatcherTest {
  
  @Test
  public void testTypeOne() {
    // INIT
    final LabelNode startLoop = new LabelNode();
    final LabelNode check = new LabelNode();
    final LabelNode aLabel = new LabelNode();
    final LabelNode bLabel = new LabelNode();
    final ClassNodeBuilder builder = ClassNodeBuilder.createClass("de.tuberlin.uebb.jbop.optimizer.utils.LoopTest");
    builder.addMethod("test", "()V").//
        add(ICONST_0).//
        add(ISTORE, 1).//
        add(GOTO, check).//
        addInsn(startLoop).//
        add(ICONST_0).//
        add(ISTORE, 2).//
        add(ICONST_0).//
        add(IFNE, aLabel).//
        add(ICONST_1).//
        add(ISTORE, 2).//
        add(GOTO, bLabel).//
        addInsn(aLabel).//
        add(ICONST_1).//
        add(ISTORE, 2).//
        addInsn(bLabel).//
        add(IINC, 2, 4).//
        add(IINC, 1, 1).//
        addInsn(check).//
        add(ILOAD, 1).//
        add(ICONST_5).//
        add(IF_ICMPLE, startLoop).//
        addReturn();
    final MethodNode method = builder.getMethod("test");
    
    // RUN
    AbstractInsnNode currentNode = method.instructions.get(1);
    final boolean isStoreOfLoop = LoopMatcher.isStoreOfLoop(currentNode);
    final Loop loop = LoopMatcher.getLoop(currentNode);
    final Pair<AbstractInsnNode, AbstractInsnNode> loopBounds = LoopMatcher.getLoopBounds(currentNode);
    currentNode = method.instructions.get(5);
    final boolean isStoreOfLoop2 = LoopMatcher.isStoreOfLoop(currentNode);
    
    currentNode = method.instructions.get(20);
    final boolean isIfOfLoop = LoopMatcher.isIfOfLoop(currentNode);
    currentNode = method.instructions.get(7);
    final boolean isIfOfLoop2 = LoopMatcher.isIfOfLoop(currentNode);
    
    currentNode = method.instructions.get(16);
    final boolean isIIncOfLoop = LoopMatcher.isIIncOfLoop(currentNode);
    currentNode = method.instructions.get(15);
    final boolean isIIncOfLoop2 = LoopMatcher.isIIncOfLoop(currentNode);
    
    currentNode = method.instructions.get(2);
    final boolean isGotoOfLoop = LoopMatcher.isGotoOfLoop(currentNode);
    currentNode = method.instructions.get(10);
    final boolean isGotoOfLoop2 = LoopMatcher.isGotoOfLoop(currentNode);
    
    // ASSERT
    assertTrue(isStoreOfLoop);
    assertFalse(isStoreOfLoop2);
    assertTrue(isIfOfLoop);
    assertFalse(isIfOfLoop2);
    assertTrue(isIIncOfLoop);
    assertFalse(isIIncOfLoop2);
    assertTrue(isGotoOfLoop);
    assertFalse(isGotoOfLoop2);
    
    assertTrue(loop.isPlain());
    assertEquals(method.instructions.get(0), loopBounds.getLeft());
    assertEquals(method.instructions.get(20), loopBounds.getRight());
    assertEquals(12, loop.getBody().size());
  }
  
  @Test
  public void testTypeTwo() {
    // INIT
    final LabelNode check = new LabelNode();
    final LabelNode endLoop = new LabelNode();
    final LabelNode aLabel = new LabelNode();
    final LabelNode bLabel = new LabelNode();
    final ClassNodeBuilder builder = ClassNodeBuilder.createClass("de.tuberlin.uebb.jbop.optimizer.utils.LoopTest");
    builder.addMethod("test", "()V").//
        add(ICONST_0).//
        add(ISTORE, 1).//
        addInsn(check).//
        add(ILOAD, 1).//
        add(ICONST_5).//
        add(IF_ICMPGT, endLoop).//
        add(ICONST_0).//
        add(ISTORE, 2).//
        add(ICONST_0).//
        add(IFNE, aLabel).//
        add(ICONST_1).//
        add(ISTORE, 2).//
        add(GOTO, bLabel).//
        addInsn(aLabel).//
        add(ICONST_1).//
        add(ISTORE, 2).//
        addInsn(bLabel).//
        add(IINC, 2, 4).//
        add(IINC, 1, 1).//
        add(GOTO, check).//
        addInsn(endLoop).//
        addReturn();
    final MethodNode method = builder.getMethod("test");
    
    // RUN
    AbstractInsnNode currentNode = method.instructions.get(1);
    final boolean isStoreOfLoop = LoopMatcher.isStoreOfLoop(currentNode);
    final Loop loop = LoopMatcher.getLoop(currentNode);
    final Pair<AbstractInsnNode, AbstractInsnNode> loopBounds = LoopMatcher.getLoopBounds(currentNode);
    currentNode = method.instructions.get(7);
    final boolean isStoreOfLoop2 = LoopMatcher.isStoreOfLoop(currentNode);
    
    currentNode = method.instructions.get(5);
    final boolean isIfOfLoop = LoopMatcher.isIfOfLoop(currentNode);
    currentNode = method.instructions.get(9);
    final boolean isIfOfLoop2 = LoopMatcher.isIfOfLoop(currentNode);
    
    currentNode = method.instructions.get(18);
    final boolean isIIncOfLoop = LoopMatcher.isIIncOfLoop(currentNode);
    currentNode = method.instructions.get(17);
    final boolean isIIncOfLoop2 = LoopMatcher.isIIncOfLoop(currentNode);
    
    currentNode = method.instructions.get(19);
    final boolean isGotoOfLoop = LoopMatcher.isGotoOfLoop(currentNode);
    currentNode = method.instructions.get(12);
    final boolean isGotoOfLoop2 = LoopMatcher.isGotoOfLoop(currentNode);
    
    // ASSERT
    assertTrue(isStoreOfLoop);
    assertFalse(isStoreOfLoop2);
    assertTrue(isIfOfLoop);
    assertFalse(isIfOfLoop2);
    assertTrue(isIIncOfLoop);
    assertFalse(isIIncOfLoop2);
    assertTrue(isGotoOfLoop);
    assertFalse(isGotoOfLoop2);
    
    assertTrue(loop.isPlain());
    assertEquals(method.instructions.get(0), loopBounds.getLeft());
    assertEquals(method.instructions.get(20), loopBounds.getRight());
    assertEquals(12, loop.getBody().size());
    
  }
  
  @Test
  public void testTypeOneNotPlain() {
    // INIT
    final LabelNode startLoop = new LabelNode();
    final LabelNode check = new LabelNode();
    final LabelNode aLabel = new LabelNode();
    final LabelNode bLabel = new LabelNode();
    final ClassNodeBuilder builder = ClassNodeBuilder.createClass("de.tuberlin.uebb.jbop.optimizer.utils.LoopTest");
    builder.addMethod("test", "()V").//
        add(ILOAD, 3).//
        add(ISTORE, 1).//
        add(GOTO, check).//
        addInsn(startLoop).//
        add(ICONST_0).//
        add(ISTORE, 2).//
        add(ICONST_0).//
        add(IFNE, aLabel).//
        add(ICONST_1).//
        add(ISTORE, 2).//
        add(GOTO, bLabel).//
        addInsn(aLabel).//
        add(ICONST_1).//
        add(ISTORE, 2).//
        addInsn(bLabel).//
        add(IINC, 2, 4).//
        add(IINC, 1, 1).//
        addInsn(check).//
        add(ILOAD, 3).//
        add(ILOAD, 1).//
        add(IF_ICMPLE, startLoop).//
        addReturn();
    final MethodNode method = builder.getMethod("test");
    
    // RUN
    AbstractInsnNode currentNode = method.instructions.get(1);
    final boolean isStoreOfLoop = LoopMatcher.isStoreOfLoop(currentNode);
    final Loop loop = LoopMatcher.getLoop(currentNode);
    final Pair<AbstractInsnNode, AbstractInsnNode> loopBounds = LoopMatcher.getLoopBounds(currentNode);
    currentNode = method.instructions.get(5);
    final boolean isStoreOfLoop2 = LoopMatcher.isStoreOfLoop(currentNode);
    
    currentNode = method.instructions.get(20);
    final boolean isIfOfLoop = LoopMatcher.isIfOfLoop(currentNode);
    currentNode = method.instructions.get(7);
    final boolean isIfOfLoop2 = LoopMatcher.isIfOfLoop(currentNode);
    
    currentNode = method.instructions.get(16);
    final boolean isIIncOfLoop = LoopMatcher.isIIncOfLoop(currentNode);
    currentNode = method.instructions.get(15);
    final boolean isIIncOfLoop2 = LoopMatcher.isIIncOfLoop(currentNode);
    
    currentNode = method.instructions.get(2);
    final boolean isGotoOfLoop = LoopMatcher.isGotoOfLoop(currentNode);
    currentNode = method.instructions.get(10);
    final boolean isGotoOfLoop2 = LoopMatcher.isGotoOfLoop(currentNode);
    
    // ASSERT
    assertTrue(isStoreOfLoop);
    assertFalse(isStoreOfLoop2);
    assertTrue(isIfOfLoop);
    assertFalse(isIfOfLoop2);
    assertTrue(isIIncOfLoop);
    assertFalse(isIIncOfLoop2);
    assertTrue(isGotoOfLoop);
    assertFalse(isGotoOfLoop2);
    
    assertFalse(loop.isPlain());
    assertEquals(method.instructions.get(0), loopBounds.getLeft());
    assertEquals(method.instructions.get(20), loopBounds.getRight());
    assertEquals(12, loop.getBody().size());
  }
  
  @Test
  public void testTypeTwoNotPlain() {
    // INIT
    final LabelNode check = new LabelNode();
    final LabelNode endLoop = new LabelNode();
    final LabelNode aLabel = new LabelNode();
    final LabelNode bLabel = new LabelNode();
    final ClassNodeBuilder builder = ClassNodeBuilder.createClass("de.tuberlin.uebb.jbop.optimizer.utils.LoopTest");
    builder.addMethod("test", "()V").//
        add(ILOAD, 3).//
        add(ISTORE, 1).//
        addInsn(check).//
        add(ILOAD, 3).//
        add(ILOAD, 1).//
        add(IF_ICMPGT, endLoop).//
        add(ICONST_0).//
        add(ISTORE, 2).//
        add(ICONST_0).//
        add(IFNE, aLabel).//
        add(ICONST_1).//
        add(ISTORE, 2).//
        add(GOTO, bLabel).//
        addInsn(aLabel).//
        add(ICONST_1).//
        add(ISTORE, 2).//
        addInsn(bLabel).//
        add(IINC, 2, 4).//
        add(IINC, 1, 1).//
        add(GOTO, check).//
        addInsn(endLoop).//
        addReturn();
    final MethodNode method = builder.getMethod("test");
    
    // RUN
    AbstractInsnNode currentNode = method.instructions.get(1);
    final boolean isStoreOfLoop = LoopMatcher.isStoreOfLoop(currentNode);
    final Loop loop = LoopMatcher.getLoop(currentNode);
    final Pair<AbstractInsnNode, AbstractInsnNode> loopBounds = LoopMatcher.getLoopBounds(currentNode);
    currentNode = method.instructions.get(7);
    final boolean isStoreOfLoop2 = LoopMatcher.isStoreOfLoop(currentNode);
    
    currentNode = method.instructions.get(5);
    final boolean isIfOfLoop = LoopMatcher.isIfOfLoop(currentNode);
    currentNode = method.instructions.get(9);
    final boolean isIfOfLoop2 = LoopMatcher.isIfOfLoop(currentNode);
    
    currentNode = method.instructions.get(18);
    final boolean isIIncOfLoop = LoopMatcher.isIIncOfLoop(currentNode);
    currentNode = method.instructions.get(17);
    final boolean isIIncOfLoop2 = LoopMatcher.isIIncOfLoop(currentNode);
    
    currentNode = method.instructions.get(19);
    final boolean isGotoOfLoop = LoopMatcher.isGotoOfLoop(currentNode);
    currentNode = method.instructions.get(12);
    final boolean isGotoOfLoop2 = LoopMatcher.isGotoOfLoop(currentNode);
    
    // ASSERT
    assertTrue(isStoreOfLoop);
    assertFalse(isStoreOfLoop2);
    assertTrue(isIfOfLoop);
    assertFalse(isIfOfLoop2);
    assertTrue(isIIncOfLoop);
    assertFalse(isIIncOfLoop2);
    assertTrue(isGotoOfLoop);
    assertFalse(isGotoOfLoop2);
    
    assertFalse(loop.isPlain());
    assertEquals(method.instructions.get(0), loopBounds.getLeft());
    assertEquals(method.instructions.get(20), loopBounds.getRight());
    assertEquals(12, loop.getBody().size());
  }
}
