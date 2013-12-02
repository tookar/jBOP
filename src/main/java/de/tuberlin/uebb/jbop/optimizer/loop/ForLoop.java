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

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;

/**
 * The Class ForLoop.
 * 
 * This is a DTO that stores the whole loop.
 * Additionally there is some logic, to evaluate
 * the loop condition / invariant.
 * 
 * It provides the unrolled loop.
 * 
 * @author Christopher Ewest
 */
public class ForLoop {
  
  private final ForLoopBody body;
  private final ForLoopFooter footer;
  private final Number start;
  
  /**
   * Instantiates a new for loop.
   * 
   * @param body
   *          the body
   * @param footer
   *          the footer
   * @param start
   *          the start
   */
  public ForLoop(final ForLoopBody body, final ForLoopFooter footer, final Number start) {
    this.body = body;
    this.footer = footer;
    this.start = start;
  }
  
  /**
   * Gets the insn list.
   * 
   * @param method
   *          the method
   * @return the insn list with the unroled loop.
   */
  public InsnList getInsnList(final MethodNode method) {
    final int loopCount = footer.getLoopCount();
    final int ifNode = footer.getIfNode().getOpcode();
    final InsnList unfolded = new InsnList();
    for (int i = start.intValue(); eval(i, loopCount, ifNode); i += footer.getIinc().incr) {
      unfolded.add(body.getInsnList(i, footer, method));
    }
    return unfolded;
  }
  
  private boolean eval(final int i, final int loopCount, final int ifNode) {
    if (ifNode == Opcodes.IF_ICMPLE) {
      return i <= loopCount;
    }
    if (ifNode == Opcodes.IF_ICMPEQ) {
      return i == loopCount;
    }
    if (ifNode == Opcodes.IF_ICMPGE) {
      return i >= loopCount;
    }
    if (ifNode == Opcodes.IF_ICMPGT) {
      return i > loopCount;
    }
    if (ifNode == Opcodes.IF_ICMPLT) {
      return i < loopCount;
    }
    if (ifNode == Opcodes.IF_ICMPNE) {
      return i != loopCount;
    }
    if (ifNode == Opcodes.IFGE) {
      return i >= 0;
    }
    if (ifNode == Opcodes.IFEQ) {
      return i == 0;
    }
    if (ifNode == Opcodes.IFGT) {
      return i > 0;
    }
    if (ifNode == Opcodes.IFLE) {
      return i <= 0;
    }
    if (ifNode == Opcodes.IFLT) {
      return i < 0;
    }
    if (ifNode == Opcodes.IFNE) {
      return i != 0;
    }
    return false;
  }
}
