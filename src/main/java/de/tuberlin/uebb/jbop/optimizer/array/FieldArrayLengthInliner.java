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

import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.collections15.Predicate;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;

import de.tuberlin.uebb.jbop.exception.JBOPClassException;
import de.tuberlin.uebb.jbop.optimizer.IOptimizer;
import de.tuberlin.uebb.jbop.optimizer.utils.NodeHelper;
import de.tuberlin.uebb.jbop.optimizer.utils.predicates.GetFieldPredicate;

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
  public FieldArrayLengthInliner(final Collection<String> names, final Object instance) {
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
      if (!arrayHelper.isArrayLength()) {
        continue;
      }
      arrayHelper.addArrayLoad();
      final int length = arrayHelper.getLength(instance);
      
      replaceNodes(original, aload, arrayHelper, length);
      
    }
    return original;
  }
  
  private void replaceNodes(final InsnList original, final AbstractInsnNode aload, final ArrayHelper arrayHelper,
      final Integer length) {
    final AbstractInsnNode replacementNode = NodeHelper.getInsnNodeFor(length);
    original.insert(aload, replacementNode);
    original.remove(aload);
    for (final AbstractInsnNode node : arrayHelper.getIndexes()) {
      original.remove(node);
    }
    for (final AbstractInsnNode node : arrayHelper.getArrayloads()) {
      original.remove(node);
    }
    original.remove(arrayHelper.getFieldNode());
    optimized = true;
  }
  
}
