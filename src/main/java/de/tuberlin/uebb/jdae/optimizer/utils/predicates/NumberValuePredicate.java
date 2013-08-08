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
package de.tuberlin.uebb.jdae.optimizer.utils.predicates;

import org.apache.commons.collections15.Predicate;
import org.apache.commons.math3.exception.NotANumberException;
import org.objectweb.asm.tree.AbstractInsnNode;

import de.tuberlin.uebb.jdae.optimizer.utils.NodeHelper;

/**
 * The Class NumberValuePredicate.
 * 
 * @author Christopher Ewest
 */
public final class NumberValuePredicate implements Predicate<AbstractInsnNode> {
  
  @Override
  public boolean evaluate(final AbstractInsnNode object) {
    try {
      NodeHelper.getValue(object);
    } catch (final NotANumberException nane) {
      return false;
    }
    return true;
  }
}
