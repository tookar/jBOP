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
import org.apache.commons.collections15.PredicateUtils;
import org.objectweb.asm.tree.AbstractInsnNode;

/**
 * The Class Predicates.
 * 
 * Holds Constants of the Node-Predicates.
 * 
 * @author Christopher Ewest
 */
public final class Predicates {
  
  private Predicates() {
    //
  }
  
  /** The Constant IS_DALOAD. */
  public static final DALoadPredicate IS_DALOAD = new DALoadPredicate();
  
  /** The Constant IS_FALOAD. */
  public static final FALoadPredicate IS_FALOAD = new FALoadPredicate();
  
  /** The Constant IS_IALOAD. */
  public static final IALoadPredicate IS_IALOAD = new IALoadPredicate();
  
  /** The Constant IS_LALOAD. */
  public static final LALoadPredicate IS_LALOAD = new LALoadPredicate();
  
  /** The Constant IS_AALOAD. */
  public static final AALoadPredicate IS_AALOAD = new AALoadPredicate();
  
  /** The Constant IS_XALOAD. */
  @SuppressWarnings("unchecked")
  public static final Predicate<AbstractInsnNode> IS_XALOAD = PredicateUtils.anyPredicate(IS_DALOAD, IS_FALOAD,
      IS_LALOAD, IS_IALOAD, IS_AALOAD);
  
  /** The Constant IS_INT_VALUE. */
  public static final NumberValuePredicate IS_NUMBER_VALUE = new NumberValuePredicate();
  
  /** The Constant IS_ALOAD. */
  public static final ALoadPredicate IS_ALOAD = new ALoadPredicate();
}
