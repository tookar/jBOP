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
import org.objectweb.asm.tree.InsnNode;

/**
 * The Class SplitMarkNode.
 * 
 * This special Node represents a special NO OPERATION.
 * 
 * It could be used to mark places in a method where a splitting
 * of the method could be performed.
 * 
 * @author Christopher Ewest
 */
public class SplitMarkNode extends InsnNode {
  
  /**
   * Instantiates a new split mark node.
   */
  public SplitMarkNode() {
    super(Opcodes.NOP);
  }
  
}
