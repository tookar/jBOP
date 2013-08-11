package de.tuberlin.uebb.jbop.optimizer.array;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import de.tuberlin.uebb.jbop.optimizer.ClassNodeBuilder;
import de.tuberlin.uebb.jbop.optimizer.annotations.Optimizable;
import de.tuberlin.uebb.jbop.optimizer.utils.NodeHelper;

public class FieldArrayLengthInlinerTest {
  
  @Test
  public void testFieldArrayLengthInbliner() throws Exception {
    // INIT
    final String owner = "de.tuberlin.uebb.jbop.optimizer.array.FieldArrayLengthTestClass";
    final ClassNodeBuilder builder = ClassNodeBuilder.createClass(owner).//
        addField("doubleArray", "[D").initArray(15).//
        addField("objectArray", Type.getDescriptor(Object[].class)).initArray(23).//
        addMethod("sumArrayLength", "()I").withAnnotation(Optimizable.class).//
        addInsn(new VarInsnNode(Opcodes.ALOAD, 0)).//
        addGetField("doubleArray").//
        addInsn(new InsnNode(Opcodes.ARRAYLENGTH)).//
        addInsn(new VarInsnNode(Opcodes.ALOAD, 0)).//
        addGetField("objectArray").//
        addInsn(new InsnNode(Opcodes.ARRAYLENGTH)).//
        addInsn(new InsnNode(Opcodes.IADD)).//
        addInsn(new InsnNode(Opcodes.IRETURN));
    
    final FieldArrayLengthInliner inliner = new FieldArrayLengthInliner(Arrays.asList("doubleArray", "objectArray"),
        builder.toClass().instance());
    
    // RUN STEP 1
    final MethodNode method = builder.getMethod("sumArrayLength");
    final InsnList optimized = inliner.optimize(method.instructions, method);
    
    // ASSERT STEP 1
    assertEquals(6, optimized.size());
    
    // RUN STEP 2
    final InsnList optimized2 = inliner.optimize(method.instructions, method);
    
    // ASSERT STEP 2
    assertEquals(4, optimized2.size());
    assertEquals(15, NodeHelper.getValue(optimized2.getFirst()).intValue());
    assertEquals(23, NodeHelper.getValue(optimized2.getFirst().getNext()).intValue());
    
    // RUN STEP 3
    final InsnList optimized3 = inliner.optimize(method.instructions, method);
    
    // ASSERT STEP 3
    assertEquals(4, optimized3.size());
  }
}
