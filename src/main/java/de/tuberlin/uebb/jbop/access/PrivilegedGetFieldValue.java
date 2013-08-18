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
  
  private String fieldName;
  private final Object instance;
  
  private final Class<? extends Object> clazz;
  
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
  public PrivilegedGetFieldValue(final Object instance) {
    this.instance = instance;
    clazz = instance.getClass();
  }
  
  @Override
  public Object run() {
    final Field field = getField();
    final boolean isAccessible = field.isAccessible();
    try {
      field.setAccessible(true);
      final Object value = field.get(instance);
      return transform(value);
    } catch (SecurityException | IllegalAccessException | IllegalArgumentException e) {
      throw new RuntimeException("Field '" + fieldName + "' of class Class<" + //
          instance.getClass().getName() + //
          "> could not be accessed.", e);
    } finally {
      try {
        field.setAccessible(isAccessible);
      } catch (final SecurityException e) {
        // skip e in favor for above exception
        LOG.log(Level.INFO, "skipped exception", e);
      }
    }
  }
  
  /**
   * Hook for subclasses to perform additional actions with 'value'
   * before returning it.
   */
  protected Object transform(final Object value) {
    return value;
  }
  
  private Field getField() {
    try {
      return clazz.getDeclaredField(fieldName);
    } catch (NoSuchFieldException | SecurityException e) {
      throw new RuntimeException("There is no Field '" + fieldName + "' in class Class<" + //
          clazz.getName() + //
          ">.", e);
    }
  }
  
  /**
   * Sets the field name.
   * 
   * @param fieldName
   *          the new field name
   */
  public void setFieldName(final String fieldName) {
    this.fieldName = fieldName;
  }
}
