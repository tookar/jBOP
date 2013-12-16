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

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.util.Textifier;

/**
 * The Class ExtendedTextifier.
 * 
 * @author Christopher Ewest
 */
class ExtendedTextifier extends Textifier {
  
  /**
   * Instantiates a new {@link ExtendedTextifier}.
   * 
   * @param node
   *          the node
   * @param classNode
   *          the class node
   */
  ExtendedTextifier(final MethodNode node, final ClassNode classNode) {
    super();
    final StringBuilder builder = new StringBuilder();
    if (classNode != null) {
      builder.append(classNode.name.replace("/", ".")).append(".");
    }
    builder.append(node.name).append(node.desc).append("\n");
    text.add(builder.toString());
  }
}
