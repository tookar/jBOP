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
package de.tuberlin.uebb.jbop.optimizer.loop;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import de.tuberlin.uebb.jbop.optimizer.IOptimizer;
import de.tuberlin.uebb.jbop.optimizer.utils.NodeHelper;

/**
 * Unrolls strict loops (see {@link de.tuberlin.uebb.jbop.modifier.annotations.StrictLoops}). <br>
 * eg:
 * 
 * <pre>
 * double[] d = {1.0, 2.0, 3.0}
 * double e = 0;
 * for(int i = 0; i< 3; ++i){
 *  e = e + d[i];
 * }
 * </pre>
 * 
 * becomes
 * 
 * <pre>
 * double[] d = {1.0, 2.0, 3.0}
 * double e = 0;
 * int i0 = 0;
 * e = e + d[i0];
 * int i1=1;
 * e = e + d[i1];
 * int i2=2
 * e = e + d[i2];
 * </pre>
 * 
 * in bytecode this means:
 * 
 * <pre>
 * 1    iconst0
 * 2    istore   x
 * 3    goto     l
 * 4    A
 * m    B
 * n    C
 * o    iinc     x
 * l    iload    x
 * l+1  bipush   3
 * l+2  ifcpmlt  4
 * </pre>
 * 
 * becomes
 * 
 * <pre>
 * bipush   0
 * istore   x1
 * A
 * B
 * C
 * bipush   1
 * istore   x2
 * A
 * B
 * C 
 * bipush   2
 * istore   x3
 * A
 * B
 * C
 * </pre>
 * 
 * where x1, x2, x3 are new local variables.
 * 
 * Additionally after every Loop a special NOP-Node ({@link SplitMarkNode}) is inserted.
 * These are used in the {@link de.tuberlin.uebb.jbop.optimizer.methodsplitter.MethodSplitter}.
 * 
 * @author Christopher Ewest
 */
public class ForLoopUnroller implements IOptimizer {
  
  private boolean optimized = false;
  private int countervar;
  private AbstractInsnNode _goto;
  private Number start;
  
  @Override
  public boolean isOptimized() {
    return optimized;
  }
  
  @Override
  public InsnList optimize(final InsnList original, final MethodNode method) {
    optimized = false;
    final Iterator<AbstractInsnNode> iterator = original.iterator();
    final InsnList insn = new InsnList();
    while (iterator.hasNext()) {
      final ForLoop loop = findForloop(iterator, insn);
      if (loop == null) {
        continue;
      }
      optimized = true;
      insn.add(loop.getInsnList(method));
    }
    return insn;
  }
  
  private ForLoop findForloop(final Iterator<AbstractInsnNode> iterator, final InsnList skipped) {
    start = null;
    final AbstractInsnNode endNode = getEndNode(iterator, skipped);
    if (endNode == null) {
      return null;
    }
    
    final ForLoopFooter footer = getFooter(endNode);
    if (footer == null) {
      skipped.add(_goto);
      _goto = null;
      return null;
    }
    _goto = null;
    final ForLoopBody body = getBody(iterator, endNode);
    
    correctIteratorPosition(iterator, footer);
    
    return new ForLoop(body, footer, start);
  }
  
  private void correctIteratorPosition(final Iterator<AbstractInsnNode> iterator, final ForLoopFooter footer) {
    while (iterator.hasNext()) {
      if (iterator.next() == footer.getIfNode()) {
        break;
      }
    }
  }
  
  private ForLoopBody getBody(final Iterator<AbstractInsnNode> iterator, final AbstractInsnNode endNode) {
    final List<AbstractInsnNode> bodyNodes = new ArrayList<>();
    while (iterator.hasNext()) {
      final AbstractInsnNode bodyNode = iterator.next();
      bodyNodes.add(bodyNode);
      if (bodyNode == endNode) {
        break;
      }
    }
    
    final AbstractInsnNode iinc = NodeHelper.getPrevious(endNode);  // IINC
    bodyNodes.remove(iinc);
    
    final ForLoopBody body = new ForLoopBody(bodyNodes);
    return body;
  }
  
  private ForLoopFooter getFooter(final AbstractInsnNode endNode) {
    final AbstractInsnNode node4 = NodeHelper.getNext(endNode);
    final AbstractInsnNode node5 = NodeHelper.getNext(node4);
    final AbstractInsnNode node6 = NodeHelper.getNext(node5);
    final boolean isIiload = NodeHelper.isIload(node4, countervar);
    final boolean isInt5 = NodeHelper.isNumberNode(node5);
    final boolean isJump6 = NodeHelper.isIf(node6);
    final boolean isInt6 = NodeHelper.isNumberNode(node6);
    final boolean isJump5 = NodeHelper.isIf(node5);
    final boolean isForLoopFooter = isIiload && ((isInt5 && isJump6) || (isInt6 && isJump5));
    if (!isForLoopFooter) {
      return null;
    }
    final AbstractInsnNode iinc = NodeHelper.getPrevious(endNode);
    final ForLoopFooter footer;
    if (isJump5) {
      footer = new ForLoopFooter((VarInsnNode) node4, node6, (JumpInsnNode) node5, ((IincInsnNode) iinc));
    } else {
      footer = new ForLoopFooter((VarInsnNode) node4, node5, (JumpInsnNode) node6, ((IincInsnNode) iinc));
    }
    
    return footer;
  }
  
  private AbstractInsnNode getEndNode(final Iterator<AbstractInsnNode> iterator, final InsnList skipped) {
    final AbstractInsnNode node3 = iterator.next();
    final AbstractInsnNode node2 = NodeHelper.getPrevious(node3);
    final AbstractInsnNode node1 = NodeHelper.getPrevious(node2);
    final boolean isForLoop = NodeHelper.isNumberNode(node1) && NodeHelper.isIstor(node2, -1)
        && NodeHelper.isGoto(node3);
    if (!isForLoop) {
      add(node3, skipped);
      return null;
    }
    
    countervar = ((VarInsnNode) node2).var;
    start = NodeHelper.getNumberValue(node1);
    _goto = node3;
    final AbstractInsnNode endNode = ((JumpInsnNode) node3).label;
    return endNode;
  }
  
  private void add(final AbstractInsnNode node, final InsnList skipped) {
    if (node == null) {
      return;
    }
    skipped.add(node);
  }
  
}
