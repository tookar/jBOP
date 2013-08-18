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

import java.util.Collection;
import java.util.Collections;

import org.apache.commons.collections15.Predicate;
import org.objectweb.asm.tree.AbstractInsnNode;

import de.tuberlin.uebb.jbop.optimizer.utils.NodeHelper;

/**
 * The Class GetFieldPredicate.
 * 
 * @author Christopher Ewest
 */
public final class GetFieldPredicate implements Predicate<AbstractInsnNode> {
  
  private final Collection<String> validFields;
  
  /**
   * Instantiates a new GetFieldPredicate.
   * 
   * @param validFields
   *          the Collection with the valid fieldnames
   */
  public GetFieldPredicate(final Collection<String> validFields) {
    this.validFields = Collections.unmodifiableCollection(validFields);
  }
  
  @Override
  public boolean evaluate(final AbstractInsnNode object) {
    if (NodeHelper.isGetField(object)) {
      final String originalName = NodeHelper.getFieldname(object);
      return validFields.contains(originalName);
    }
    return false;
  }
}
