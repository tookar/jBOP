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
package de.tuberlin.uebb.jdae.optimizer.array;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import de.tuberlin.uebb.jdae.exception.JBOPClassException;
import de.tuberlin.uebb.jdae.optimizer.IOptimizer;
import de.tuberlin.uebb.jdae.optimizer.annotations.ImmutableArray;
import de.tuberlin.uebb.jdae.optimizer.utils.NodeHelper;
import de.tuberlin.uebb.jdae.optimizer.utils.predicates.Predicates;

/**
 * The Class LocalArrayValueInliner.
 * 
 * This class can inline values of arrays (local variable).
 * 
 * Currently this is only possible for local arrays that are
 * created via 'ArrayGet' from field-multiArrays.
 * 
 * eg:
 * 
 * <pre>
 * @ImmutableArray
 * private final int[][] multArray = {{1,2,3,4},{5,6,7,8}};
 * ...
 * int[] array = multArray[0];
 * ...
 * int valueFromArray = array[3];
 * ...
 * </pre>
 * 
 * becomes
 * 
 * <pre>
 * @ImmutableArray
 * private final int[][] multArray = {{1,2,3,4},{5,6,7,8}};
 * ...
 * int[] array = multArray[0];
 * ...
 * int valueFromArray = 4;
 * ...
 * </pre>
 * 
 * @author Christopher Ewest
 */
public class LocalArrayValueInliner implements IOptimizer {
  
  private boolean optimized;
  private final Object input;
  private final Class<?> clazz;
  
  /**
   * Instantiates a new {@link LocalArrayValueInliner}.
   * 
   * @param input
   *          the input
   */
  public LocalArrayValueInliner(final Object input) {
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
    
    AbstractInsnNode arrayload = currentNode;
    AbstractInsnNode indexNode;
    final List<AbstractInsnNode> arrayloads = new ArrayList<>();
    final List<AbstractInsnNode> indexes = new ArrayList<>();
    do {
      if (!Predicates.IS_XALOAD.evaluate(arrayload)) {
        return;
      }
      arrayloads.add(arrayload);
      indexNode = NodeHelper.getPrevious(arrayload);
      
      if (!NodeHelper.isNumberNode(indexNode)) {
        return;
      }
      indexes.add(indexNode);
      arrayload = NodeHelper.getPrevious(indexNode);
      if (arrayload instanceof VarInsnNode) {
        break;
      }
    } while (true);
    
    final AbstractInsnNode previous2 = arrayload;
    if (!(previous2 instanceof VarInsnNode)) {
      return;
    }
    
    final Integer varIndex = Integer.valueOf(((VarInsnNode) previous2).var);
    Object array = knownArrays.get(varIndex);
    
    if (array == null) {
      return;
    }
    
    for (int i = indexes.size() - 1; i >= 0; --i) {
      final int indexInArray = NodeHelper.getValue(indexes.get(i)).intValue();
      array = Array.get(array, indexInArray);
    }
    if (!(array instanceof Number)) {
      return;
    }
    final AbstractInsnNode replacement = NodeHelper.getInsnNodeFor((Number) array);
    original.insertBefore(previous2, replacement);
    for (int i = 0; i < indexes.size(); ++i) {
      original.remove(indexes.get(i));
      original.remove(arrayloads.get(i));
    }
    original.remove(previous2);
    
    optimized = true;
  }
  
  private boolean registerValues(final AbstractInsnNode currentNode, final Map<Integer, Object> knownArrays)
      throws JBOPClassException {
    if (currentNode.getOpcode() == Opcodes.ASTORE) {
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
      if (field.getAnnotation(ImmutableArray.class) == null) {
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
        final Number arrIndex = NodeHelper.getValue(previous2.get(i));
        index1 = arrIndex.intValue();
      }
      array2 = Array.get(array2, index1);
    }
    knownArrays.put(varIndex, array2);
    return true;
  }
}
