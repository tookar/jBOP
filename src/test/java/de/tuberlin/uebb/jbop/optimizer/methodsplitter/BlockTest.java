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
package de.tuberlin.uebb.jdae.optimizer.methodsplitter;

import static org.junit.Assert.assertEquals;

import java.util.ListIterator;

import org.junit.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Test for {@link Block}.
 * 
 * @author Christopher Ewest
 */
public class BlockTest {
  
  /**
   * Tests the type erasure.
   */
  @Test
  public void testTypeErasure() {
    final InsnList list = new InsnList();
    list.add(new InsnNode(Opcodes.ICONST_5));
    list.add(new IntInsnNode(Opcodes.NEWARRAY, Opcodes.T_INT));
    list.add(new VarInsnNode(Opcodes.ASTORE, 3));
    list.add(new VarInsnNode(Opcodes.ILOAD, 1));
    list.add(new VarInsnNode(Opcodes.ILOAD, 2));
    list.add(new InsnNode(Opcodes.IADD));
    list.add(new InsnNode(Opcodes.ICONST_0));
    list.add(new VarInsnNode(Opcodes.ALOAD, 3));
    list.add(new InsnNode(Opcodes.IASTORE));
    list.add(new InsnNode(Opcodes.ICONST_5));
    list.add(new IntInsnNode(Opcodes.NEWARRAY, Opcodes.T_INT));
    list.add(new VarInsnNode(Opcodes.ASTORE, 4));
    list.add(new VarInsnNode(Opcodes.ILOAD, 1));
    list.add(new VarInsnNode(Opcodes.ILOAD, 2));
    list.add(new InsnNode(Opcodes.IADD));
    list.add(new InsnNode(Opcodes.ICONST_1));
    final VarInsnNode insn = new VarInsnNode(Opcodes.ALOAD, 3);
    list.add(insn);
    list.add(new InsnNode(Opcodes.IASTORE));
    list.add(new InsnNode(Opcodes.RETURN));
    
    final ListIterator<AbstractInsnNode> iterator = list.iterator();
    
    final Block block = new Block(1, new Type[] {
        Type.INT_TYPE, Type.INT_TYPE
    });
    while (iterator.hasNext()) {
      block.addInsn(iterator.next(), true);
    }
    
    final Type findType = block.findType(insn);
    assertEquals(Type.getType("[I"), findType);
  }
  
}
