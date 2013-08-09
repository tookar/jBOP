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

import java.util.List;

import org.objectweb.asm.tree.AbstractInsnNode;

/**
 * The Class NonNullArrayValue.
 * 
 * A Mapping for Array-Values that are known to be null.
 * 
 * @author Christopher Ewest
 */
public class NonNullArrayValue {
  
  private final AbstractInsnNode node1;
  private final AbstractInsnNode node2;
  private final List<AbstractInsnNode> node3;
  private final List<AbstractInsnNode> node4;
  
  /**
   * Instantiates a new non null array value.
   * 
   * @param node1
   *          the node1
   * @param node2
   *          the node2
   * @param node3
   *          the node3
   * @param node4
   *          the node4
   */
  public NonNullArrayValue(final AbstractInsnNode node1, final AbstractInsnNode node2,
      final List<AbstractInsnNode> node3, final List<AbstractInsnNode> node4) {
    this.node1 = node1;
    this.node2 = node2;
    this.node3 = node3;
    this.node4 = node4;
  }
  
  /**
   * @return the first node of the Array-Load-Pattern.
   */
  public AbstractInsnNode getNode1() {
    return node1;
  }
  
  /**
   * @return the second node of the Array-Load-Pattern.
   */
  public AbstractInsnNode getNode2() {
    return node2;
  }
  
  /**
   * @return the third node of the Array-Load-Pattern.
   */
  public List<AbstractInsnNode> getNode3() {
    return node3;
  }
  
  /**
   * @return the fourth node of the Array-Load-Pattern.
   */
  public List<AbstractInsnNode> getNode4() {
    return node4;
  }
  
  /**
   * Checks if the given Pattern is equal toThis.
   * 
   * @param n1
   *          the n1
   * @param n2
   *          the n2
   * @param n3
   *          the n3
   * @param n4
   *          the n4
   * @return true, if successful
   */
  public boolean is(final AbstractInsnNode n1, final AbstractInsnNode n2, final AbstractInsnNode n3,
      final AbstractInsnNode n4) {
    if (node1 != n1) {
      return false;
    }
    if (node2 != n2) {
      return false;
    }
    if (node3 != n3) {
      return false;
    }
    if (node4 != n4) {
      return false;
    }
    return true;
  }
  
}
