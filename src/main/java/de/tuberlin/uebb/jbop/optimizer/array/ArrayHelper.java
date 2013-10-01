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

import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ARRAYLENGTH;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;

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
  
  private final List<AbstractInsnNode> indexes = new ArrayList<>(3);
  private final List<AbstractInsnNode> arrayloads = new ArrayList<>(3);
  private AbstractInsnNode fieldNode;
  private AbstractInsnNode array;
  
  private static FieldNode getFieldNode(final ClassNode classNode, final AbstractInsnNode node) {
    if (!(node instanceof FieldInsnNode)) {
      return null;
    }
    final FieldInsnNode field = (FieldInsnNode) node;
    if (!classNode.name.equals(field.owner)) {
      return null;
    }
    for (final FieldNode fieldNode : classNode.fields) {
      if (fieldNode.name.equals(field.name)) {
        return fieldNode;
      }
    }
    return null;
  }
  
  private static boolean hasAnnotation(final FieldNode fieldNode, final Class<?> annotationClass) {
    if (fieldNode == null) {
      return false;
    }
    if (fieldNode.visibleAnnotations == null) {
      return false;
    }
    final String annotationDesc = "L" + Type.getInternalName(annotationClass) + ";";
    
    for (final AnnotationNode annotation : fieldNode.visibleAnnotations) {
      if (annotation.desc.equals(annotationDesc)) {
        return true;
      }
    }
    return false;
  }
  
  private static boolean hasAnnotation(final ClassNode classNode, final AbstractInsnNode fieldNode,
      final Class<?> annotationClass) {
    if (annotationClass == null) {
      return false;
    }
    return hasAnnotation(getFieldNode(classNode, fieldNode), annotationClass);
  }
  
  private static boolean isFinal(final ClassNode classNode, final AbstractInsnNode fieldNode) {
    return isFinal(getFieldNode(classNode, fieldNode));
  }
  
  private static boolean isFinal(final FieldNode fieldNode) {
    if (fieldNode == null) {
      return false;
    }
    return (fieldNode.access & ACC_FINAL) != 0;
  }
  
  /**
   * Checks if is array instruction.
   * 
   * @param node
   *          the node
   * @param fieldPredicate
   *          the field predicate
   * @return true, if is array instruction
   */
  boolean isArrayInstruction(final ClassNode classNode, final AbstractInsnNode node, final Class<?> annotationClass) {
    if (!Predicates.IS_ALOAD.evaluate(node)) {
      return false;
    }
    fieldNode = NodeHelper.getNext(node);
    
    if (!isArray(fieldNode)) {
      return false;
    }
    if ((annotationClass == null) && !isFinal(classNode, fieldNode)) {
      return false;
    }
    if ((annotationClass != null) && !hasAnnotation(classNode, fieldNode, annotationClass)) {
      return false;
    }
    indexes.clear();
    arrayloads.clear();
    findIndexes();
    return true;
  }
  
  private static boolean isArray(final AbstractInsnNode fieldNode) {
    if (!(fieldNode instanceof FieldInsnNode)) {
      return false;
    }
    return StringUtils.startsWith(((FieldInsnNode) fieldNode).desc, "[");
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
