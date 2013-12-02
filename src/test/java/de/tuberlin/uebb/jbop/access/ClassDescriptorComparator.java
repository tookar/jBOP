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
package de.tuberlin.uebb.jbop.access;

import java.util.Comparator;

import de.tuberlin.uebb.jbop.access.ClassDescriptor;

/**
 * The Class ClassDescriptorComparator.
 * 
 * @author Christopher Ewest
 */
public class ClassDescriptorComparator implements Comparator<ClassDescriptor> {
  
  @Override
  public int compare(final ClassDescriptor o1, final ClassDescriptor o2) {
    if ((o1 == null) && (o2 == null)) {
      return 0;
    }
    if (o1 == null) {
      return -1;
    }
    if (o2 == null) {
      return 1;
    }
    
    return o1.getName().compareTo(o2.getName());
  }
  
}
