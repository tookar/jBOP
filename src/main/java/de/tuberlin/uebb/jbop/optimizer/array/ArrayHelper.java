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

import static org.objectweb.asm.Opcodes.ARRAYLENGTH;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections15.Predicate;
import org.objectweb.asm.tree.AbstractInsnNode;

import de.tuberlin.uebb.jbop.access.ClassAccessor;
import de.tuberlin.uebb.jbop.exception.JBOPClassException;
import de.tuberlin.uebb.jbop.optimizer.utils.NodeHelper;
import de.tuberlin.uebb.jbop.optimizer.utils.predicates.Predicates;

/**
 * Simple Helper for common patterns of {@link FieldArrayLengthInliner} and {@link FieldArrayValueInliner}.
 * 
 * @author Christopher Ewest
 */
class ArrayHelper {
  
  private List<AbstractInsnNode> indexes;
  private List<AbstractInsnNode> arrayloads;
  private AbstractInsnNode fieldNode;
  private AbstractInsnNode array;
  
  /**
   * Checks if is array instruction.
   * 
   * @param node
   *          the node
   * @param fieldPredicate
   *          the field predicate
   * @return true, if is array instruction
   */
  boolean isArrayInstruction(final AbstractInsnNode node, final Predicate<AbstractInsnNode> fieldPredicate) {
    if (!Predicates.IS_ALOAD.evaluate(node)) {
      return false;
    }
    fieldNode = NodeHelper.getNext(node);
    if (!fieldPredicate.evaluate(fieldNode)) {
      return false;
    }
    indexes = new ArrayList<>(3);
    arrayloads = new ArrayList<>(3);
    findIndexes();
    return true;
  }
  
  private void findIndexes() {
    AbstractInsnNode index = NodeHelper.getNext(fieldNode);
    array = NodeHelper.getNext(index);
    do {
      if (!Predicates.IS_NUMBER_VALUE.evaluate(index)) {
        array = index;
        break;
      }
      if (!Predicates.IS_XALOAD.evaluate(array)) {
        break;
      }
      indexes.add(index);
      arrayloads.add(array);
      index = NodeHelper.getNext(array);
      array = NodeHelper.getNext(index);
    } while (true);
  }
  
  /**
   * Gets the indexes.
   * 
   * @return the indexes
   */
  List<AbstractInsnNode> getIndexes() {
    return indexes;
  }
  
  /**
   * Gets the arrayloads.
   * 
   * @return the arrayloads
   */
  List<AbstractInsnNode> getArrayloads() {
    return arrayloads;
  }
  
  /**
   * Gets the field node.
   * 
   * @return the field node
   */
  AbstractInsnNode getFieldNode() {
    return fieldNode;
  }
  
  /**
   * Gets the index array.
   * 
   * @return the index array
   */
  int[] getIndexArray() {
    final int[] indexArr = new int[indexes.size()];
    for (int i = 0; i < indexes.size(); ++i) {
      final int indexOf = NodeHelper.getNumberValue(indexes.get(i)).intValue();
      indexArr[i] = indexOf;
    }
    return indexArr;
  }
  
  /**
   * Gets the value.
   * 
   * @param instance
   *          the instance
   * @return the value
   * @throws JBOPClassException
   *           the jBOP class exception
   */
  Object getValue(final Object instance) throws JBOPClassException {
    return ClassAccessor.getCurrentValue(instance, NodeHelper.getFieldname(fieldNode), getIndexArray());
  }
  
  /**
   * Gets the length.
   * 
   * @param instance
   *          the instance
   * @return the length
   * @throws JBOPClassException
   *           the jBOP class exception
   */
  int getLength(final Object instance) throws JBOPClassException {
    try {
      return Array.getLength(getValue(instance));
    } catch (final IllegalArgumentException iae) {
      throw new JBOPClassException("Arraylength could not be determined.", iae);
    }
  }
  
  /**
   * Gets the last load.
   * 
   * @return the last load
   */
  AbstractInsnNode getLastLoad() {
    return arrayloads.get(arrayloads.size() - 1);
  }
  
  /**
   * Adds the last node as array load.
   */
  void addArrayLoad() {
    arrayloads.add(array);
  }
  
  /**
   * Gets the last Node.
   * 
   * @return the last Node
   */
  AbstractInsnNode getLastNode() {
    return array;
  }
  
  /**
   * Checks if the indexlist is empty.
   * 
   * @return true, if is index empty
   */
  boolean isIndexEmpty() {
    return indexes.isEmpty();
  }
  
  /**
   * Checks if the last Node is array length.
   * 
   * @return true, if is array length
   */
  boolean isArrayLength() {
    return array.getOpcode() == ARRAYLENGTH;
  }
}
