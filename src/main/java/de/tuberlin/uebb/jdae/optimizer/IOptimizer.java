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
package de.tuberlin.uebb.jdae.optimizer;

import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;

import de.tuberlin.uebb.jdae.exception.JBOPClassException;

/**
 * The Interface IOptimizer.
 * 
 * @author Christopher Ewest
 */
public interface IOptimizer {
  
  /**
   * Checks if the last call to {@link #optimize(AbstractInsnNode)} made any changes (optimizations).
   * 
   * @return true, if is optimized
   */
  boolean isOptimized();
  
  /**
   * Optimize the given node.
   * 
   * @param original
   *          the original instructionlist
   * @param methodNode
   *          the method node that contains the inbstructions
   * @return the (maybe) modified InsnList. This may be a new List, but it could also be the original list.
   * @throws JBOPClassException
   *           if the optimization couldn't be performed due to unexpected circumstances.
   */
  InsnList optimize(InsnList original, MethodNode methodNode) throws JBOPClassException;
  
}
