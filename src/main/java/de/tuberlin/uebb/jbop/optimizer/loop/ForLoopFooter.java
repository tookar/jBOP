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

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import de.tuberlin.uebb.jbop.optimizer.utils.NodeHelper;

/**
 * The Class ForLoopFooter.
 * 
 * This DTO holds the footer of the loop.
 * 
 * The Footer contains the increment-amount and the var-Index.
 * 
 * @author Christopher Ewest
 */
public class ForLoopFooter {
  
  /**
   * Instantiates a new for loop footer.
   * 
   * @param iLoadNode
   *          the i load node
   * @param biPushNode
   *          the bi push node
   * @param ifNode
   *          the if node
   */
  public ForLoopFooter(final VarInsnNode iLoadNode, final AbstractInsnNode biPushNode, final JumpInsnNode ifNode,
      final IincInsnNode iinc) {
    super();
    this.iLoadNode = iLoadNode;
    this.biPushNode = biPushNode;
    this.ifNode = ifNode;
    this.iinc = iinc;
  }
  
  private final VarInsnNode iLoadNode;
  private final AbstractInsnNode biPushNode;
  private final JumpInsnNode ifNode;
  private final IincInsnNode iinc;
  
  /**
   * Gets the i load node.
   * 
   * @return the i load node
   */
  public VarInsnNode getiLoadNode() {
    return iLoadNode;
  }
  
  /**
   * Gets the bi push node.
   * 
   * @return the bi push node
   */
  public AbstractInsnNode getBiPushNode() {
    return biPushNode;
  }
  
  /**
   * Gets the if node.
   * 
   * @return the if node
   */
  public JumpInsnNode getIfNode() {
    return ifNode;
  }
  
  public int getLoopCount() {
    return NodeHelper.getValue(biPushNode).intValue();
  }
  
  /**
   * Gets the var index.
   * 
   * @return the var index
   */
  public int getVarIndex() {
    return iLoadNode.var;
  }
  
  /**
   * Gets the iinc.
   * 
   * @return the iinc
   */
  public IincInsnNode getIinc() {
    return iinc;
  }
}
