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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.collections15.Predicate;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;

import de.tuberlin.uebb.jbop.exception.JBOPClassException;
import de.tuberlin.uebb.jbop.optimizer.IOptimizer;
import de.tuberlin.uebb.jbop.optimizer.utils.NodeHelper;
import de.tuberlin.uebb.jbop.optimizer.utils.predicates.GetFieldPredicate;

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
  public FieldArrayValueInliner(final Collection<String> names, final Object instance) {
    this.instance = instance;
    is_getfield = new GetFieldPredicate(names);
  }
  
  @Override
  public boolean isOptimized() {
    return optimized;
  }
  
  @Override
  public InsnList optimize(final InsnList original, final MethodNode method) throws JBOPClassException {
    optimized = false;
    final Iterator<AbstractInsnNode> iterator = original.iterator();
    final ArrayHelper arrayHelper = new ArrayHelper();
    while (iterator.hasNext()) {
      final AbstractInsnNode aload = iterator.next();
      if (!arrayHelper.isArrayInstruction(aload, is_getfield)) {
        continue;
      }
      if (arrayHelper.isIndexEmpty()) {
        continue;
      }
      
      handleValue(original, aload, arrayHelper, iterator);
    }
    return original;
  }
  
  private void handleValue(final InsnList newList, final AbstractInsnNode aload, final ArrayHelper arrayHelper,
      final Iterator<AbstractInsnNode> iterator) throws JBOPClassException {
    
    final Object value = arrayHelper.getValue(instance);
    
    if (isArrayLoad(arrayHelper.getLastLoad().getOpcode())) {
      final AbstractInsnNode replacementNode;
      if ((value instanceof Number)) {
        replacementNode = NodeHelper.getInsnNodeFor((Number) value);
      } else {
        replacementNode = new LdcInsnNode(value);
      }
      replaceNodes(newList, aload, arrayHelper, replacementNode, iterator);
    } else {
      if (value == null) {
        final AbstractInsnNode replacementNode = new InsnNode(Opcodes.ACONST_NULL);
        replaceNodes(newList, aload, arrayHelper, replacementNode, iterator);
      } else {
        final NonNullArrayValue arrayValue = new NonNullArrayValue(aload, arrayHelper.getFieldNode(),
            arrayHelper.getIndexes(), arrayHelper.getArrayloads());
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
  
  private void replaceNodes(final InsnList newList, final AbstractInsnNode aload, final ArrayHelper arrayHelper,
      final AbstractInsnNode replacementNode, final Iterator<AbstractInsnNode> iterator) {
    newList.insert(arrayHelper.getLastLoad(), replacementNode);
    newList.remove(aload);
    for (final AbstractInsnNode node : arrayHelper.getIndexes()) {
      newList.remove(node);
    }
    for (final AbstractInsnNode node : arrayHelper.getArrayloads()) {
      newList.remove(node);
    }
    iterator.next();
    newList.remove(arrayHelper.getFieldNode());
    optimized = true;
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
