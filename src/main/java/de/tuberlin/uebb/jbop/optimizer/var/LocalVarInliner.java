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

import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ASTORE;
import static org.objectweb.asm.Opcodes.GOTO;
import static org.objectweb.asm.Opcodes.IINC;
import static org.objectweb.asm.Opcodes.ILOAD;
import static org.objectweb.asm.Opcodes.ISTORE;
import static org.objectweb.asm.Opcodes.NEWARRAY;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;

import de.tuberlin.uebb.jbop.optimizer.IOptimizer;
import de.tuberlin.uebb.jbop.optimizer.utils.LoopMatcher;
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
  
  /**
   * Checks if is optimized.
   * 
   * @return true, if is optimized
   */
  @Override
  public boolean isOptimized() {
    return optimized;
  }
  
  /**
   * Optimize.
   * 
   * @param original
   *          the original
   * @param methodNode
   *          the method node
   * @return the insn list
   */
  @Override
  public InsnList optimize(final InsnList original, final MethodNode methodNode) {
    optimized = false;
    final Map<Integer, Object> knownValues = new HashMap<>();
    
    final AbstractInsnNode first = original.getFirst();
    final AbstractInsnNode last = original.getLast();
    handleNodes(first, last, original, knownValues, methodNode);
    
    return original;
  }
  
  private AbstractInsnNode handleNodes(final AbstractInsnNode first, final AbstractInsnNode last,
      final InsnList original, final Map<Integer, Object> knownValues, final MethodNode methodNode) {
    AbstractInsnNode currentNode = first;
    while (currentNode != null && currentNode != last) {
      currentNode = handleNode(original, currentNode, knownValues, methodNode);
    }
    return currentNode;
  }
  
  private AbstractInsnNode handleNode(final InsnList original, final AbstractInsnNode currentNode,
      final Map<Integer, Object> knownValues, final MethodNode methodNode) {
    final int opcode = currentNode.getOpcode();
    if (opcode >= ISTORE && opcode <= ASTORE) {
      handleStore(currentNode, knownValues);
    } else if (opcode >= ILOAD && opcode <= ALOAD) {
      return hanldeLoad(original, currentNode, knownValues).getNext();
    } else if (opcode == IINC) {
      handleIInc(currentNode, knownValues);
    } else if (NodeHelper.isIf(currentNode)) {
      return handleIf(currentNode, knownValues, original, methodNode);
    } else if (opcode == GOTO) {
      if (LoopMatcher.isGotoOfLoop(currentNode)) {
        final AbstractInsnNode skipVars = skipVars(currentNode, knownValues);
        final Pair<AbstractInsnNode, AbstractInsnNode> bodyBounds = LoopMatcher.getBodyBounds(currentNode);
        if (bodyBounds != null) {
          handleNodes(bodyBounds.getLeft(), bodyBounds.getRight(), original, new HashMap<>(knownValues), methodNode);
        }
        return skipVars;
      }
      return currentNode.getNext();
    }
    return currentNode.getNext();
  }
  
  private AbstractInsnNode skipVars(final AbstractInsnNode currentNode, final Map<Integer, Object> knownValues) {
    final Pair<AbstractInsnNode, AbstractInsnNode> loopBounds = LoopMatcher.getLoopBounds(currentNode);
    final Collection<Integer> vars = getVarsInRange(loopBounds.getLeft(), loopBounds.getRight());
    for (final Integer varInLoop : vars) {
      knownValues.remove(varInLoop);
    }
    return loopBounds.getRight().getNext();
  }
  
  private Collection<Integer> getVarsInRange(final AbstractInsnNode start, final AbstractInsnNode end) {
    final Set<Integer> vars = new HashSet<>();
    AbstractInsnNode current = start;
    while (current != null && current != end) {
      final int opcode = current.getOpcode();
      if (opcode >= ISTORE && opcode <= ASTORE || opcode == IINC) {
        vars.add(NodeHelper.getVarIndex(current));
      }
      current = current.getNext();
    }
    return vars;
  }
  
  private AbstractInsnNode handleIf(final AbstractInsnNode currentNode, final Map<Integer, Object> knownValues,
      final InsnList original, final MethodNode methodNode) {
    if (LoopMatcher.isIfOfLoop(currentNode)) {
      return skipVars(currentNode, knownValues);
    }
    final LabelNode endIf = ((JumpInsnNode) currentNode).label;
    final AbstractInsnNode end1 = endIf.getNext();
    handleNodes(currentNode.getNext(), endIf, original, new HashMap<>(knownValues), methodNode);
    AbstractInsnNode end2 = null;
    final List<Integer> stores = getStores(currentNode.getNext(), end1);
    if (endIf.getPrevious() instanceof JumpInsnNode) {
      end2 = ((JumpInsnNode) endIf.getPrevious()).label.getNext();
      handleNodes(end1, end2, original, new HashMap<>(knownValues), methodNode);
      stores.addAll(getStores(endIf, end2));
    }
    for (final Integer var : stores) {
      knownValues.remove(var);
    }
    if (end2 != null) {
      return end2;
    }
    return end1;
  }
  
  private List<Integer> getStores(final AbstractInsnNode start, final AbstractInsnNode end) {
    AbstractInsnNode tmpNode = start;
    final List<Integer> stores = new ArrayList<>();
    while (tmpNode != null && tmpNode != end) {
      if (tmpNode.getOpcode() >= ISTORE && tmpNode.getOpcode() <= ASTORE) {
        stores.add(NodeHelper.getVarIndex(tmpNode));
      } else if (tmpNode.getOpcode() == IINC) {
        final IincInsnNode iinc = (IincInsnNode) tmpNode;
        final int index = iinc.var;
        stores.add(index);
      }
      tmpNode = tmpNode.getNext();
    }
    return stores;
  }
  
  private void handleIInc(final AbstractInsnNode currentNode, final Map<Integer, Object> knownValues) {
    if (LoopMatcher.isIIncOfLoop(currentNode)) {
      return;
    }
    final IincInsnNode iinc = (IincInsnNode) currentNode;
    final int index = iinc.var;
    if (knownValues.containsKey(index)) {
      final int currValue = (Integer) knownValues.get(index);
      final int newValue = currValue + iinc.incr;
      knownValues.put(index, newValue);
    }
  }
  
  private AbstractInsnNode hanldeLoad(final InsnList original, final AbstractInsnNode currentNode,
      final Map<Integer, Object> knownValues) {
    final int index = NodeHelper.getVarIndex(currentNode);
    if (knownValues.containsKey(index) && currentNode.getNext() != null && currentNode.getPrevious() != null) {
      final Object value = knownValues.get(index);
      final AbstractInsnNode replacement = NodeHelper.getInsnNodeFor(value);
      original.set(currentNode, replacement);
      optimized = true;
      return replacement;
    }
    return currentNode;
  }
  
  private void handleStore(final AbstractInsnNode currentNode, final Map<Integer, Object> knownValues) {
    final int index = NodeHelper.getVarIndex(currentNode);
    if (LoopMatcher.isStoreOfLoop(currentNode)) {
      knownValues.remove(index);
      return;
    }
    final AbstractInsnNode previous = currentNode.getPrevious();
    if (previous.getOpcode() != NEWARRAY && NodeHelper.isValue(previous)) {
      final Object value = NodeHelper.getValue(previous);
      knownValues.put(index, value);
    } else {
      knownValues.remove(index);
    }
  }
}
