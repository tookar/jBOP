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
import java.security.AccessController;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.collections15.Predicate;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;

import de.tuberlin.uebb.jbop.exception.JBOPClassException;
import de.tuberlin.uebb.jbop.optimizer.IOptimizer;
import de.tuberlin.uebb.jbop.optimizer.utils.NodeHelper;
import de.tuberlin.uebb.jbop.optimizer.utils.predicates.GetFieldPredicate;
import de.tuberlin.uebb.jbop.optimizer.utils.predicates.Predicates;

/**
 * Inlines the size of an array (class field), so that further optimizationsteps
 * could handle these calls as constant.
 * 
 * eg:
 * 
 * <pre>
 * private final double[] d = {1.0, 2.0, 3.0};
 * ...
 * for(int i = 0; i < d.length; ++i){
 * ...
 * }
 * </pre>
 * 
 * becomes
 * 
 * <pre>
 * private final double[] d = {1.0, 2.0, 3.0};
 * ...
 * for(int i = 0; i < 3; ++i){
 * ...
 * }
 * </pre>
 * 
 * In bytecode this means
 * 
 * <pre>
 * aload        0
 * getfield     d
 * arraylength
 * </pre>
 * 
 * becomes one of
 * 
 * <pre>
 * iconstx
 * ore
 * bipush       x
 * or
 * ldc          x
 * </pre>
 * 
 * depending on the real size of d
 * 
 * @author Christopher Ewest
 */
public class FieldArrayLengthInliner implements IOptimizer {
  
  private boolean optimized = false;
  
  private final Map<String, Field> replacementMap;
  
  private final Object instance;
  
  private final Predicate<AbstractInsnNode> is_getfield;
  
  /**
   * Instantiates a new FieldArrayLengthInliner.
   * 
   * @param names
   *          the names
   * @param instance
   *          the instance
   * @throws JBOPClassException
   *           the jBOP class exception
   */
  public FieldArrayLengthInliner(final Collection<String> names, final Object instance) throws JBOPClassException {
    replacementMap = new TreeMap<String, Field>();
    final Class<? extends Object> instanceClass = instance.getClass();
    
    for (final String originalName : names) {
      final Field originalValueField;
      try {
        originalValueField = instanceClass.getDeclaredField(originalName);
      } catch (NoSuchFieldException | SecurityException e) {
        throw new JBOPClassException("There is no Field like '" + originalName + "' in Class<"
            + instanceClass.getName() + ">", e);
      }
      replacementMap.put(originalName, originalValueField);
    }
    this.instance = instance;
    is_getfield = new GetFieldPredicate(replacementMap);
  }
  
  @Override
  public boolean isOptimized() {
    return optimized;
  }
  
  @Override
  public InsnList optimize(final InsnList original, final MethodNode method) {
    optimized = false;
    final Iterator<AbstractInsnNode> iterator = original.iterator();
    
    while (iterator.hasNext()) {
      final AbstractInsnNode aload = iterator.next();
      if (!Predicates.IS_ALOAD.evaluate(aload)) {
        continue;
      }
      final AbstractInsnNode fieldNode = NodeHelper.getNext(aload);
      if (!is_getfield.evaluate(fieldNode)) {
        continue;
      }
      final List<AbstractInsnNode> indexes = new ArrayList<>(3);
      final List<AbstractInsnNode> arrayloads = new ArrayList<>(3);
      AbstractInsnNode index = NodeHelper.getNext(fieldNode);
      AbstractInsnNode array = NodeHelper.getNext(index);
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
      if (array.getOpcode() != Opcodes.ARRAYLENGTH) {
        continue;
      }
      arrayloads.add(array);
      final Integer length = getLength(indexes, fieldNode);
      length.intValue();
      
      replaceNodes(original, aload, fieldNode, indexes, arrayloads, length);
      
    }
    return original;
  }
  
  private void replaceNodes(final InsnList original, final AbstractInsnNode aload, final AbstractInsnNode fieldNode,
      final List<AbstractInsnNode> indexes, final List<AbstractInsnNode> arrayloads, final Integer length) {
    final AbstractInsnNode replacementNode = NodeHelper.getInsnNodeFor(length);
    original.insert(aload, replacementNode);
    original.remove(aload);
    for (final AbstractInsnNode node : indexes) {
      original.remove(node);
    }
    for (final AbstractInsnNode node : arrayloads) {
      original.remove(node);
    }
    original.remove(fieldNode);
    optimized = true;
  }
  
  private Integer getLength(final List<AbstractInsnNode> indexes, final AbstractInsnNode fieldNode) {
    final int[] indexArr = new int[indexes.size()];
    for (int i = 0; i < indexes.size(); ++i) {
      final int indexOf = NodeHelper.getNumberValue(indexes.get(i)).intValue();
      indexArr[i] = indexOf;
    }
    Object value;
    try {
      value = getArrayValue(NodeHelper.getFieldname(fieldNode), indexArr);
    } catch (final JBOPClassException e) {
      throw new RuntimeException(e);
    }
    return Integer.valueOf(Array.getLength(value));
  }
  
  private Object getArrayValue(final String originalName, final int... index) throws JBOPClassException {
    final Field field = replacementMap.get(originalName);
    try {
      return AccessController.doPrivileged(new PrivilegedGetArrayValue(originalName, field, instance, index));
    } catch (final RuntimeException re) {
      throw new JBOPClassException(re.getMessage(), re.getCause());
    }
  }
  
}
