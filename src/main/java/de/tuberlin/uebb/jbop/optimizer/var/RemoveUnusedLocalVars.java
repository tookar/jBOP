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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import de.tuberlin.uebb.jbop.optimizer.IOptimizer;
import de.tuberlin.uebb.jbop.optimizer.utils.NodeHelper;

/**
 * Removes unused local variables. <br>
 * Variables are unused if they are stored but never read via load.<br>
 * If a variable is stored and only used by iinc operations,<br>
 * these iinc operations are removed too. <br>
 * eg:
 * 
 * <pre>
 * int i=1;
 * int j=2
 * if(condition){
 *  i=2;
 * }
 * j++;
 * System.out.println(i+2);
 * </pre>
 * 
 * becomes
 * 
 * <pre>
 * int i = 1;
 * if (condition) {
 *   i = 2;
 * }
 * System.out.println(i + 2);
 * </pre>
 * 
 * 
 * @author Christopher Ewest
 */
public class RemoveUnusedLocalVars implements IOptimizer {
  
  private boolean optimized;
  
  private final Map<AbstractInsnNode, AbstractInsnNode> prevs = new HashMap<>();
  
  @Override
  public boolean isOptimized() {
    return optimized;
  }
  
  @Override
  public InsnList optimize(final InsnList original, final MethodNode methodNode) {
    optimized = false;
    prevs.clear();
    final List<VarInsnNode> stores = new ArrayList<>();
    final List<VarInsnNode> users = new ArrayList<>();
    final List<IincInsnNode> iincs = new ArrayList<>();
    findNodes(original, stores, users, iincs);
    final List<AbstractInsnNode> toBeRemoved = new ArrayList<>();
    for (final VarInsnNode node : stores) {
      if (!usersContains(users, node)) {
        toBeRemoved.add(node);
        toBeRemoved.addAll(iincsContains(iincs, node));
      }
    }
    final InsnList newList = new InsnList();
    final Iterator<AbstractInsnNode> iterator = original.iterator();
    while (iterator.hasNext()) {
      final AbstractInsnNode currentNode = iterator.next();
      if (toBeRemoved.contains(currentNode)) {
        if (currentNode.getOpcode() == Opcodes.IINC) {
          original.remove(currentNode);
          optimized = true;
          continue;
        }
        final AbstractInsnNode prev = prevs.get(currentNode);
        if (NodeHelper.isValue(prev)) {
          newList.remove(prev);
          original.remove(currentNode);
          optimized = true;
        } else {
          original.remove(currentNode);
          // POP value instead?
          newList.add(currentNode);
        }
        
      } else {
        original.remove(currentNode);
        newList.add(currentNode);
      }
    }
    return newList;
  }
  
  private List<AbstractInsnNode> iincsContains(final List<IincInsnNode> iincs, final VarInsnNode node) {
    final List<AbstractInsnNode> nodes = new ArrayList<>();
    for (final IincInsnNode iinc : iincs) {
      if (iinc.var == node.var) {
        nodes.add(iinc);
      }
    }
    return nodes;
  }
  
  private boolean usersContains(final List<VarInsnNode> users, final VarInsnNode node) {
    for (final VarInsnNode user : users) {
      if (user.var == node.var) {
        return true;
      }
    }
    return false;
  }
  
  private void findNodes(final InsnList original, final List<VarInsnNode> stores, final List<VarInsnNode> users, //
      final List<IincInsnNode> iincs) {
    final Iterator<AbstractInsnNode> iterator = original.iterator();
    while (iterator.hasNext()) {
      final AbstractInsnNode node = iterator.next();
      if ((node.getOpcode() >= Opcodes.ISTORE) && (node.getOpcode() <= Opcodes.ASTORE)) {
        stores.add((VarInsnNode) node);
        prevs.put(node, NodeHelper.getPrevious(node));
      } else if ((node.getOpcode() >= Opcodes.ILOAD) && (node.getOpcode() <= Opcodes.ALOAD)) {
        users.add((VarInsnNode) node);
      } else if (node.getOpcode() == Opcodes.IINC) {
        iincs.add((IincInsnNode) node);
      }
    }
  }
  
}
