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
package de.tuberlin.uebb.jbop.optimizer.utils.predicates;

import org.apache.commons.collections15.Predicate;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.MethodNode;

import de.tuberlin.uebb.jbop.optimizer.annotations.Optimizable;

/**
 * The Class OptimizablePredicate.
 * 
 * @author Christopher Ewest
 */
public class OptimizablePredicate implements Predicate<MethodNode> {
  
  private static final String DESCRIPTOR = Type.getType(Optimizable.class).getDescriptor();
  
  /**
   * Evaluates if the given Method is annotated with {@link Optimizable}.
   * 
   * @param object
   *          the object
   * @return true, if successful
   */
  @Override
  public boolean evaluate(final MethodNode object) {
    if (object.visibleAnnotations == null) {
      return false;
    }
    for (final AnnotationNode annotation : object.visibleAnnotations) {
      if (DESCRIPTOR.equals(annotation.desc)) {
        return true;
      }
    }
    return false;
  }
}
