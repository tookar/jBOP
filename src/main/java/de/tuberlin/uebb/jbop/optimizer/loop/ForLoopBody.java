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

import java.util.List;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import de.tuberlin.uebb.jbop.optimizer.LabelMap;

/**
 * The Class ForLoopBody.
 * 
 * This DTO stores the body of the loop.
 * 
 * It provides the instructions of the body in
 * respect to the 'current' counter variable.
 * 
 * @author Christopher Ewest
 */
public class ForLoopBody {
  
  private final List<AbstractInsnNode> body;
  
  /**
   * Instantiates a new for loop body.
   * 
   * @param body
   *          the body
   */
  public ForLoopBody(final List<AbstractInsnNode> body) {
    super();
    this.body = body;
  }
  
  /**
   * Gets the body.
   * 
   * @return the body
   */
  public List<AbstractInsnNode> getBody() {
    return body;
  }
  
  /**
   * Gets the insn list.
   * 
   * @param i
   *          the curretn counter
   * @param footer
   *          the footer
   * @param method
   *          the method
   * @return the insn list of the body in respect to the current counter (<code>i</code>).
   */
  public InsnList getInsnList(final int i, final ForLoopFooter footer, final MethodNode method) {
    final InsnList list = new InsnList();
    final LabelMap labelMap = new LabelMap();
    final int newLocalVar = method.maxLocals + 1;
    final int oldLocalVar = footer.getVarIndex();
    list.add(new IntInsnNode(Opcodes.BIPUSH, i));
    list.add(new VarInsnNode(Opcodes.ISTORE, newLocalVar));
    for (final AbstractInsnNode node : body) {
      final AbstractInsnNode copyNode = node.clone(labelMap);
      if (copyNode instanceof VarInsnNode) {
        if (((VarInsnNode) copyNode).var == oldLocalVar) {
          ((VarInsnNode) copyNode).var = newLocalVar;
        }
      }
      list.add(copyNode);
    }
    list.add(new SplitMarkNode());
    method.maxLocals++;
    return list;
  }
  
}
