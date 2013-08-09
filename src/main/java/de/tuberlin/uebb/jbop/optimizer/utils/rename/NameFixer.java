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
package de.tuberlin.uebb.jdae.optimizer.utils.rename;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

/**
 * The Class NameFixer.
 * 
 * Replaces substrings of Names
 * 
 * @author Christopher Ewest
 */
class NameFixer {
  
  private final Set<String> oldNames = new HashSet<>();
  
  /** The new name. */
  final String newName;
  
  /**
   * Instantiates a new {@link NameFixer}.
   * 
   * @param newName
   *          the new name
   */
  NameFixer(final String newName) {
    super();
    this.newName = newName;
  }
  
  /**
   * Fix the name.
   * 
   * @param s
   *          the s
   * @return the string
   */
  String fix(final String s) {
    final String nameTouse = s;
    if (nameTouse != null) {
      for (final String name : oldNames) {
        StringUtils.replace(nameTouse, name, newName);
      }
    }
    return nameTouse;
  }
  
  /**
   * Adds the name to the list of known names.
   * 
   * @param name
   *          the name
   */
  void add(final String name) {
    oldNames.add(name);
  }
  
  /**
   * checks, if the name is already known.
   * 
   * @param s
   *          the s
   * @return true, if successful
   */
  boolean contains(final String s) {
    return oldNames.contains(s);
  }
}
