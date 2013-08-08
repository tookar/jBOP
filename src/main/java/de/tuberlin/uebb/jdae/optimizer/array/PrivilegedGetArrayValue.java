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
package de.tuberlin.uebb.jdae.optimizer.array;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.security.PrivilegedAction;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The Class PrivilegedGetArrayValue.
 * 
 * Privileged getter for Reflection-Use.
 * 
 * @author Christopher Ewest
 */
class PrivilegedGetArrayValue implements PrivilegedAction<Object> {
  
  private static final Logger LOG = Logger.getLogger("PrivilegedGetArray");
  
  private final String originalName;
  private final int[] indexes;
  private final Field field;
  private final Object instance;
  
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
  public PrivilegedGetArrayValue(final String originalName, final Field field, final Object instance,
      final int... indexes) {
    this.originalName = originalName;
    this.indexes = indexes;
    this.field = field;
    this.instance = instance;
  }
  
  @Override
  public Object run() {
    final boolean isAccessible = field.isAccessible();
    try {
      field.setAccessible(true);
      Object array = field.get(instance);
      for (final int index : indexes) {
        array = Array.get(array, index);
      }
      return array;
    } catch (SecurityException | IllegalAccessException e) {
      throw new RuntimeException("Field '" + originalName + "' of class Class<" + //
          instance.getClass().getName() + //
          "> could not be accessed. ", e);
    } finally {
      try {
        field.setAccessible(isAccessible);
      } catch (final SecurityException e) {
        // skip e in favor for above exception
        LOG.log(Level.INFO, "skipped exception", e);
      }
    }
  }
}
