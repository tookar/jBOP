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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with JBOP. If not, see <http://www.gnu.org/licenses/>.
 */
package de.tuberlin.uebb.jbop.optimizer.utils.predicates;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;

import org.apache.commons.collections15.Predicate;
import org.objectweb.asm.tree.AbstractInsnNode;

import de.tuberlin.uebb.jbop.optimizer.utils.NodeHelper;


/**
 * The Class GetFieldPredicate.
 * 
 * @author Christopher Ewest
 */
public final class GetFieldPredicate implements Predicate<AbstractInsnNode> {
  
  private final Map<String, Field> replacementMap;
  
  /**
   * Instantiates a new gets the field predicate.
   * 
   * @param replacementMap
   *          the replacement map
   */
  public GetFieldPredicate(final Map<String, Field> replacementMap) {
    this.replacementMap = Collections.unmodifiableMap(replacementMap);
  }
  
  @Override
  public boolean evaluate(final AbstractInsnNode object) {
    if (NodeHelper.isGetField(object)) {
      final String originalName = NodeHelper.getFieldname(object);
      return replacementMap.get(originalName) != null;
    }
    return false;
  }
}
