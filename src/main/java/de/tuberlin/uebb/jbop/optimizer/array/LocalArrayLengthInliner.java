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
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.math3.exception.NotANumberException;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import de.tuberlin.uebb.jbop.exception.JBOPClassException;
import de.tuberlin.uebb.jbop.optimizer.IOptimizer;
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
public class LocalArrayLengthInliner implements IOptimizer {
  
  private boolean optimized;
  private final Object input;
  private final Class<?> clazz;
  
  /**
   * Instantiates a new {@link LocalArrayLengthInliner}.
   * 
   * @param input
   *          the input
   */
  public LocalArrayLengthInliner(final Object input) {
    this.input = input;
    clazz = input.getClass();
  }
  
  @Override
  public boolean isOptimized() {
    return optimized;
  }
  
  @Override
  public InsnList optimize(final InsnList original, final MethodNode methodNode) throws JBOPClassException {
    optimized = false;
    final Iterator<AbstractInsnNode> iterator = original.iterator();
    final Map<Integer, Object> knownArrays = new TreeMap<>();
    while (iterator.hasNext()) {
      final AbstractInsnNode currentNode = iterator.next();
      if (registerValues(currentNode, knownArrays)) {
        continue;
      }
      handleValues(original, knownArrays, currentNode);
    }
    return original;
  }
  
  private void handleValues(final InsnList original, final Map<Integer, Object> knownArrays,
      final AbstractInsnNode currentNode) {
    if (!NodeHelper.isArrayLength(currentNode)) {
      return;
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
      return;
    }
    
    final Object array = knownArrays.get(Integer.valueOf(index));
    if ((array == null)) {
      return;
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
    optimized = true;
  }
  
  private boolean registerValues(final AbstractInsnNode currentNode, final Map<Integer, Object> knownArrays)
      throws JBOPClassException {
    final int opcode = currentNode.getOpcode();
    if ((opcode == Opcodes.NEWARRAY) || (opcode == Opcodes.ANEWARRAY) || (opcode == Opcodes.MULTIANEWARRAY)) {
      return registerNewArray(currentNode, knownArrays);
    }
    if (opcode == Opcodes.ASTORE) {
      return registerGetArray(currentNode, knownArrays);
    }
    return false;
  }
  
  private boolean registerGetArray(final AbstractInsnNode currentNode, final Map<Integer, Object> knownArrays)
      throws JBOPClassException {
    final List<AbstractInsnNode> previous = new ArrayList<>();
    final List<AbstractInsnNode> previous2 = new ArrayList<>();
    AbstractInsnNode previous2x = currentNode;
    while (true) {
      final AbstractInsnNode previousx = NodeHelper.getPrevious(previous2x);
      if (previousx.getOpcode() != Opcodes.AALOAD) {
        return false;
      }
      previous.add(previousx);
      previous2x = NodeHelper.getPrevious(previousx);
      if (!NodeHelper.isNumberNode(previous2x)) {
        return false;
      }
      previous2.add(previous2x);
      final AbstractInsnNode previous2xtmp = NodeHelper.getPrevious(previous2x);
      if ((previous2xtmp instanceof FieldInsnNode) || NodeHelper.isAload(previous2xtmp)) {
        break;
      }
    }
    final AbstractInsnNode previous3 = NodeHelper.getPrevious(previous2.get(previous2.size() - 1));
    final Object array;
    if (previous3 instanceof VarInsnNode) {
      array = knownArrays.get(Integer.valueOf(((VarInsnNode) previous3).var));
    } else {
      if (!(previous3 instanceof FieldInsnNode)) {
        return false;
      }
      final AbstractInsnNode previous4 = NodeHelper.getPrevious(previous3);
      if (!NodeHelper.isAload0(previous4)) {
        return false;
      }
      final String fieldName = ((FieldInsnNode) previous3).name;
      
      Field field;
      try {
        field = clazz.getDeclaredField(fieldName);
      } catch (NoSuchFieldException | SecurityException e) {
        throw new JBOPClassException("There is no field named '" + fieldName + "' in Class<" + clazz.getName() + ">.",
            e);
      }
      
      if ((field.getModifiers() & Modifier.FINAL) == 0) {
        return false;
      }
      
      final boolean isAccessible = field.isAccessible();
      
      try {
        field.setAccessible(true);
        array = field.get(input);
      } catch (IllegalArgumentException | IllegalAccessException e) {
        throw new JBOPClassException("", e);
      } finally {
        try {
          field.setAccessible(isAccessible);
        } catch (final SecurityException e) {
          //
        }
      }
    }
    final Integer varIndex = Integer.valueOf(((VarInsnNode) currentNode).var);
    
    Object array2 = array;
    for (int i = previous2.size() - 1; i >= 0; i--) {
      int index1;
      if (previous2.size() <= i) {
        index1 = 0;
      } else {
        final Number arrIndex = NodeHelper.getNumberValue(previous2.get(i));
        index1 = arrIndex.intValue();
      }
      array2 = Array.get(array2, index1);
    }
    knownArrays.put(varIndex, array2);
    return true;
  }
  
  private boolean registerNewArray(final AbstractInsnNode currentNode, final Map<Integer, Object> knownArrays) {
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
        return false;
      }
      try {
        final int value = NodeHelper.getNumberValue(previous).intValue();
        sizes[i] = value;
      } catch (final NotANumberException nane) {
        return false;
      }
    }
    final AbstractInsnNode next = NodeHelper.getNext(currentNode);
    if (!(next instanceof VarInsnNode)) {
      return false;
    }
    final int index = ((VarInsnNode) next).var;
    knownArrays.put(Integer.valueOf(index), Array.newInstance(Object.class, sizes));
    return true;
  }
}
