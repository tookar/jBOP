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
package de.tuberlin.uebb.jbop.optimizer.utils;

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.tree.AbstractInsnNode;

/**
 * The Class Loop.
 * 
 * @author Christopher Ewest
 */
public class Loop {
  
  private final AbstractInsnNode ifNode;
  private final AbstractInsnNode startValue;
  private final AbstractInsnNode endValue;
  private final AbstractInsnNode iinc;
  private final AbstractInsnNode firstOfBody;
  private final AbstractInsnNode endOfLoop;
  private final AbstractInsnNode counter;
  
  /**
   * Instantiates a new {@link Loop}.
   * 
   * @param ifNode
   *          the if node
   * @param startValue
   *          the start value
   * @param endValue
   *          the end value
   * @param iinc
   *          the iinc
   * @param firstOfBody
   *          the first of body
   * @param endOfLoop
   *          the end of loop
   * @param counter
   *          the counter
   */
  Loop(final AbstractInsnNode ifNode, final AbstractInsnNode startValue, final AbstractInsnNode endValue,
      final AbstractInsnNode iinc, final AbstractInsnNode firstOfBody, final AbstractInsnNode endOfLoop,
      final AbstractInsnNode counter) {
    super();
    this.ifNode = ifNode;
    this.startValue = startValue;
    this.endValue = endValue;
    this.iinc = iinc;
    this.firstOfBody = firstOfBody;
    this.endOfLoop = endOfLoop;
    this.counter = counter;
  }
  
  /**
   * Gets the if node.
   * 
   * @return the if node
   */
  protected AbstractInsnNode getIfNode() {
    return ifNode;
  }
  
  /**
   * Gets the start value.
   * 
   * @return the start value
   */
  protected AbstractInsnNode getStartValue() {
    return startValue;
  }
  
  /**
   * Gets the end value.
   * 
   * @return the end value
   */
  protected AbstractInsnNode getEndValue() {
    return endValue;
  }
  
  /**
   * Checks if the loop is plain (this means, that the start end end value
   * is a number, and not an expression).
   * 
   * @return true, if is plain
   */
  public boolean isPlain() {
    return NodeHelper.isNumberNode(getStartValue()) && NodeHelper.isNumberNode(getEndValue());
  }
  
  /**
   * Gets the body.
   * 
   * @return the body
   */
  protected List<AbstractInsnNode> getBody() {
    final List<AbstractInsnNode> body = new ArrayList<>();
    AbstractInsnNode currentNode = firstOfBody;
    final AbstractInsnNode endNode = getIInc();
    while (currentNode != endNode) {
      body.add(currentNode);
      currentNode = currentNode.getNext();
    }
    return body;
  }
  
  /**
   * Gets the iinc.
   * 
   * @return the iinc
   */
  protected AbstractInsnNode getIInc() {
    return iinc;
  }
  
  /**
   * Gets the end of loop.
   * 
   * @return the end of loop
   */
  public AbstractInsnNode getEndOfLoop() {
    return endOfLoop;
  }
  
  /**
   * Gets the counter.
   * 
   * @return the counter
   */
  public AbstractInsnNode getCounter() {
    return counter;
  }
  
}
