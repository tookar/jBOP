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
package de.tuberlin.uebb.jbop.optimizer.var;

import java.lang.reflect.Field;
import java.security.PrivilegedAction;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The Class PrivilegedGetFieldValue.
 * 
 * Privileged getter for Reflection-Use.
 * 
 * @author Christopher Ewest
 */
class PrivilegedGetFieldValue implements PrivilegedAction<Object> {
  
  private static final Logger LOG = Logger.getLogger("PrivilegedGetField");
  
  private final String originalName;
  private final Field field;
  private final Object instance;
  
  /**
   * Instantiates a new {@link PrivilegedGetFieldValue}.
   * 
   * @param originalName
   *          the original name
   * @param field
   *          the field
   * @param instance
   *          the instance
   */
  public PrivilegedGetFieldValue(final String originalName, final Field field, final Object instance) {
    this.originalName = originalName;
    this.field = field;
    this.instance = instance;
  }
  
  @Override
  public Object run() {
    final boolean isAccessible = field.isAccessible();
    try {
      field.setAccessible(true);
      final Object value = field.get(instance);
      return value;
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
