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
package de.tuberlin.uebb.jbop.optimizer.var;

import java.util.HashSet;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.lang3.tuple.Pair;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import de.tuberlin.uebb.jbop.optimizer.IOptimizer;
import de.tuberlin.uebb.jbop.optimizer.utils.NodeHelper;

/**
 * Inlines local variables where possible.
 * Only variables that are stored exactly once are inlined.
 * Therefore there is no complex flow analysis needed. <br>
 * eg:
 * 
 * <pre>
 * int i=1;
 * int j=2
 * if(condition){
 *  i=2;
 * }
 * System.out.println(i+j);
 * </pre>
 * 
 * becomes
 * 
 * <pre>
 * int i=1;
 * int j=2
 * if(condition){
 *  i=2;
 * }
 * System.out.println(i+2);
 * </pre>
 * 
 * @author Christopher Ewest
 */
public class LocalVarInliner implements IOptimizer {
  
  // private static final Logger LOG = Logger.getLogger("LocalVarInliner");
  private boolean optimized;
  
  private static final AbstractInsnNode NULL = new InsnNode(-1) {
    
    @Override
    public String toString() {
      return "NULL";
    }
  };
  
  @Override
  public boolean isOptimized() {
    return optimized;
  }
  
  @SuppressWarnings("null")
  @Override
  public InsnList optimize(final InsnList original, final MethodNode methodNode) {
    optimized = false;
    final Map<Integer, AbstractInsnNode> inlinableVars = findInlinableVars(original);
    final ListIterator<AbstractInsnNode> iterator = original.iterator();
    final InsnList newList = new InsnList();
    while (iterator.hasNext()) {
      final AbstractInsnNode currentNode = iterator.next();
      if (currentNode instanceof VarInsnNode) {
        final Integer varIndex = NodeHelper.getVarIndex(currentNode);
        final AbstractInsnNode valueNode = inlinableVars.get(varIndex);
        final boolean isStore = isStore(currentNode);
        final boolean isLoad = isLoad(currentNode);
        final boolean isValueUnknown = (valueNode == null) || (valueNode == NULL);
        if (isStore || isValueUnknown || (!isStore && !isLoad)) {
          newList.add(currentNode);
          continue;
        }
        newList.add(valueNode.clone(null));
        optimized = true;
        continue;
      }
      newList.add(currentNode);
    }
    return newList;
  }
  
  private boolean isLoad(final AbstractInsnNode currentNode) {
    return currentNode.getOpcode() == Opcodes.ILOAD;
    // return (currentNode.getOpcode() >= Opcodes.ILOAD) && (currentNode.getOpcode() <= Opcodes.ALOAD);
  }
  
  private boolean isStore(final AbstractInsnNode currentNode) {
    return (currentNode.getOpcode() >= Opcodes.ISTORE) && (currentNode.getOpcode() <= Opcodes.ASTORE);
  }
  
  private Map<Integer, AbstractInsnNode> findInlinableVars(final InsnList original) {
    final ListIterator<AbstractInsnNode> iterator = original.iterator();
    final Map<Integer, AbstractInsnNode> values = new TreeMap<Integer, AbstractInsnNode>();
    final Set<Pair<AbstractInsnNode, AbstractInsnNode>> jumpTargets = new HashSet<>();
    while (iterator.hasNext()) {
      final AbstractInsnNode currentNode = iterator.next();
      markVariables(values, currentNode);
      if (currentNode instanceof JumpInsnNode) {
        final LabelNode label = ((JumpInsnNode) currentNode).label;
        if (NodeHelper.isPredecessor(label, currentNode)) {
          jumpTargets.add(Pair.of(currentNode, label.getNext()));
        }
      }
    }
    for (final Pair<AbstractInsnNode, AbstractInsnNode> jumpTarget : jumpTargets) {
      AbstractInsnNode currentNode = jumpTarget.getValue();
      final AbstractInsnNode end = jumpTarget.getKey();
      while (currentNode.getNext() != end) {
        markVariables(values, currentNode);
        currentNode = currentNode.getNext();
      }
    }
    return values;
  }
  
  private void markVariables(final Map<Integer, AbstractInsnNode> values, final AbstractInsnNode currentNode) {
    if (isStore(currentNode)) {
      final AbstractInsnNode valueNode = NodeHelper.getPrevious(currentNode);
      final Integer varIndex = NodeHelper.getVarIndex(currentNode);
      if (values.containsKey(varIndex)) {
        values.put(varIndex, NULL);
      } else if (NodeHelper.isValue(valueNode)) {
        values.put(varIndex, valueNode);
      }
    }
    if (currentNode instanceof IincInsnNode) {
      values.put(Integer.valueOf(((IincInsnNode) currentNode).var), NULL);
    }
  }
}
