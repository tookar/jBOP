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
package de.tuberlin.uebb.jbop.access;

import java.lang.reflect.Array;

/**
 * The Class PrivilegedGetArrayValue.
 * 
 * Privileged getter for Reflection-Use.
 * 
 * @author Christopher Ewest
 */
class PrivilegedGetArrayValue extends PrivilegedGetFieldValue {
  
  private int[] indexes = new int[0];
  
  /**
   * Instantiates a new {@link PrivilegedGetArrayValue}.
   * 
   * @param originalName
   *          the original name
   * @param field
   *          the field
   * @param instance
   *          the instance
   * @param indexes
   *          the indexes
   */
  public PrivilegedGetArrayValue(final Object instance) {
    super(instance);
  }
  
  @Override
  protected Object transform(final Object value) {
    Object array = value;
    for (final int index : indexes) {
      array = Array.get(array, index);
    }
    return array;
  }
  
  public void setIndexes(final int... indexes) {
    this.indexes = indexes;
  }
}
