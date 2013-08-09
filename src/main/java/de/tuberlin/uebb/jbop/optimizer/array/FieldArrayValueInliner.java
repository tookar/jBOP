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

import java.lang.reflect.Field;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.collections15.Predicate;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;

import de.tuberlin.uebb.jdae.exception.JBOPClassException;
import de.tuberlin.uebb.jdae.optimizer.IOptimizer;
import de.tuberlin.uebb.jdae.optimizer.utils.NodeHelper;
import de.tuberlin.uebb.jdae.optimizer.utils.predicates.GetFieldPredicate;
import de.tuberlin.uebb.jdae.optimizer.utils.predicates.Predicates;

/**
 * Inlines the value of an array (class field) at position i, so that further optimizationsteps
 * could handle these calls as constant.
 * 
 * eg:
 * 
 * <pre>
 * @ImmtuableArray
 * private final double[] d = {1.0, 2.0, 3.0};
 * ...
 * double dv = d[2]
 * </pre>
 * 
 * becomes
 * 
 * <pre>
 * @ImmtuableArray
 * private final double[] d = {1.0, 2.0, 3.0};
 * ...
 * double dv = 3.0
 * </pre>
 * 
 * In bytecode this means
 * 
 * <pre>
 * aload        0
 * getfield     d
 * iconst2          [or iload i]
 * daload
 * </pre>
 * 
 * becomes one of
 * 
 * <pre>
 * ldc2w       x
 * or
 * dconstx
 * </pre>
 * 
 * depending on the real size of d
 * 
 * @author Christopher Ewest
 */
public class FieldArrayValueInliner implements IOptimizer {
  
  private final ArrayList<NonNullArrayValue> nonNullArrayValues = new ArrayList<>();
  
  private boolean optimized = false;
  
  private final Map<String, Field> replacementMap;
  
  private final Object instance;
  
  private final Predicate<AbstractInsnNode> is_getfield;
  
  /**
   * Instantiates a new ArrayValueInliner.
   * 
   * @param names
   *          the names
   * @param instance
   *          the instance
   * @throws JBOPClassException
   *           the jBOP class exception
   */
  public FieldArrayValueInliner(final Collection<String> names, final Object instance) throws JBOPClassException {
    replacementMap = new TreeMap<String, Field>();
    this.instance = instance;
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
      AbstractInsnNode arrayload = NodeHelper.getNext(index);
      do {
        if (!Predicates.IS_NUMBER_VALUE.evaluate(index)) {
          break;
        }
        if (!Predicates.IS_XALOAD.evaluate(arrayload)) {
          break;
        }
        indexes.add(index);
        arrayloads.add(arrayload);
        index = NodeHelper.getNext(arrayload);
        arrayload = NodeHelper.getNext(index);
      } while (true);
      if (indexes.isEmpty()) {
        continue;
      }
      handleValue(original, aload, fieldNode, indexes, arrayloads, iterator);
    }
    return original;
  }
  
  private void handleValue(final InsnList newList, final AbstractInsnNode aload, final AbstractInsnNode fieldNode,
      final List<AbstractInsnNode> indexes, final List<AbstractInsnNode> arrayloads,
      final Iterator<AbstractInsnNode> iterator) {
    final int[] indexArr = new int[indexes.size()];
    for (int i = 0; i < indexes.size(); ++i) {
      final int indexOf = NodeHelper.getValue(indexes.get(i)).intValue();
      indexArr[i] = indexOf;
    }
    Object value;
    try {
      value = getArrayValue(NodeHelper.getFieldname(fieldNode), indexArr);
    } catch (final JBOPClassException e) {
      throw new RuntimeException(e);
    }
    
    if (isArrayLoad(arrayloads.get(arrayloads.size() - 1).getOpcode())) {
      final AbstractInsnNode replacementNode;
      if ((value instanceof Number)) {
        replacementNode = NodeHelper.getInsnNodeFor((Number) value);
      } else {
        replacementNode = new LdcInsnNode(value);
      }
      replaceNodes(newList, aload, fieldNode, indexes, arrayloads, replacementNode, iterator);
    } else {
      if (value == null) {
        final AbstractInsnNode replacementNode = new InsnNode(Opcodes.ACONST_NULL);
        replaceNodes(newList, aload, fieldNode, indexes, arrayloads, replacementNode, iterator);
      } else {
        final NonNullArrayValue arrayValue = new NonNullArrayValue(aload, fieldNode, indexes, arrayloads);
        nonNullArrayValues.add(arrayValue);
      }
    }
  }
  
  private boolean isArrayLoad(final int opcode) {
    if (opcode == Opcodes.IALOAD) {
      return true;
    }
    if (opcode == Opcodes.FALOAD) {
      return true;
    }
    if (opcode == Opcodes.LALOAD) {
      return true;
    }
    if (opcode == Opcodes.DALOAD) {
      return true;
    }
    return false;
  }
  
  private void replaceNodes(final InsnList newList, final AbstractInsnNode aload, final AbstractInsnNode fieldNode,
      final List<AbstractInsnNode> indexes, final List<AbstractInsnNode> arrayloads,
      final AbstractInsnNode replacementNode, final Iterator<AbstractInsnNode> iterator) {
    newList.insert(arrayloads.get(arrayloads.size() - 1), replacementNode);
    newList.remove(aload);
    for (final AbstractInsnNode node : indexes) {
      newList.remove(node);
    }
    for (final AbstractInsnNode node : arrayloads) {
      newList.remove(node);
    }
    iterator.next();
    newList.remove(fieldNode);
    optimized = true;
  }
  
  private Object getArrayValue(final String originalName, final int... index) throws JBOPClassException {
    final Field field = replacementMap.get(originalName);
    try {
      return AccessController.doPrivileged(new PrivilegedGetArrayValue(originalName, field, instance, index));
    } catch (final RuntimeException re) {
      throw new JBOPClassException(re.getMessage(), re.getCause());
    }
  }
  
  /**
   * Gets the {@link NonNullArrayValue}s that were found.
   * 
   * @return the non null array values
   */
  public List<NonNullArrayValue> getNonNullArrayValues() {
    return Collections.unmodifiableList(nonNullArrayValues);
  }
  
}
