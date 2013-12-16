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
package de.tuberlin.uebb.jbop.optimizer.array;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.exception.NotANumberException;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import de.tuberlin.uebb.jbop.optimizer.utils.NodeHelper;

/**
 * The Class LocalArrayLengthInliner.
 * 
 * Inlines the size of an array (local variable), so that further optimizationsteps
 * could handle these calls as constant.
 * 
 * eg:
 * 
 * <pre>
 * double[] d = {1.0, 2.0, 3.0};
 * for(int i = 0; i < d.length; ++i){
 * ...
 * }
 * </pre>
 * 
 * becomes
 * 
 * <pre>
 * double[] d = {1.0, 2.0, 3.0};
 * for(int i = 0; i < 3; ++i){
 * ...
 * }
 * </pre>
 * 
 * in bytcode this means:
 * 
 * <pre>
 * aload        x
 * arraylength
 * </pre>
 * 
 * becomes
 * 
 * <pre>
 * bipush 3
 * </pre>
 * 
 * @author Christopher Ewest
 */
public class LocalArrayLengthInliner extends AbstractLocalArrayOptimizer {
  
  @Override
  protected boolean handleValues(final InsnList original, final Map<Integer, Object> knownArrays,
      final AbstractInsnNode currentNode) {
    if (!NodeHelper.isArrayLength(currentNode)) {
      return false;
    }
    
    AbstractInsnNode previous = NodeHelper.getPrevious(currentNode);
    final List<AbstractInsnNode> toBeRemoved = new ArrayList<>();
    int index2 = 0;
    while ((previous != null) && !NodeHelper.isAload(previous)) {
      if (NodeHelper.isAAload(previous)) {
        index2 += 1;
      }
      toBeRemoved.add(previous);
      previous = NodeHelper.getPrevious(previous);
    }
    final int index;
    if ((previous != null) && NodeHelper.isAload(previous)) {
      toBeRemoved.add(previous);
      index = ((VarInsnNode) previous).var;
    } else {
      return false;
    }
    
    final Object array = knownArrays.get(Integer.valueOf(index));
    if ((array == null)) {
      return false;
    }
    Object array2 = array;
    for (int i = 0; i < index2; ++i) {
      array2 = Array.get(array2, i);
    }
    final int arrayLength = Array.getLength(array2);
    original.insertBefore(currentNode, NodeHelper.getInsnNodeFor(arrayLength));
    
    for (final AbstractInsnNode remove : toBeRemoved) {
      original.remove(remove);
    }
    original.remove(currentNode);
    return true;
  }
  
  /**
   * Registers values for local arrays that are created via NEWARRAY / ANEWARRAY or MULTIANEWARRAY.
   * 
   * @param currentNode
   *          the current node
   * @param knownArrays
   *          the known arrays
   * @return true, if successful
   */
  @Override
  protected int registerAdditionalValues(final AbstractInsnNode currentNode, //
      final Map<Integer, Object> knownArrays) {
    final int opcode = currentNode.getOpcode();
    if (!((opcode == Opcodes.NEWARRAY) || (opcode == Opcodes.ANEWARRAY) || (opcode == Opcodes.MULTIANEWARRAY))) {
      return 0;
    }
    int dims = 1;
    if (currentNode.getOpcode() == Opcodes.MULTIANEWARRAY) {
      final MultiANewArrayInsnNode node = (MultiANewArrayInsnNode) currentNode;
      dims = node.dims;
    }
    final int sizes[] = new int[dims];
    AbstractInsnNode previous = currentNode;
    for (int i = 0; i < dims; ++i) {
      previous = NodeHelper.getPrevious(previous);
      if (!NodeHelper.isIntNode(previous)) {
        return 0;
      }
      try {
        final int value = NodeHelper.getNumberValue(previous).intValue();
        sizes[i] = value;
      } catch (final NotANumberException nane) {
        return 0;
      }
    }
    final AbstractInsnNode next = NodeHelper.getNext(currentNode);
    if (!(next instanceof VarInsnNode)) {
      return 0;
    }
    final int index = ((VarInsnNode) next).var;
    knownArrays.put(Integer.valueOf(index), Array.newInstance(Object.class, sizes));
    return 2;
  }
}
