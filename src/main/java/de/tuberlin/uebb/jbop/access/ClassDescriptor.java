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
package de.tuberlin.uebb.jdae.access;

import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

/**
 * The Class ClassDescriptor.
 * A Simple DTO to describe a classfile with its full qualified name and its byte-data.
 * Additionally the origin (path) is stored.
 * 
 * @author Christopher Ewest
 */
public class ClassDescriptor {
  
  /** The name. */
  private final String name;
  /** the file */
  private final String file;
  /** The class data. */
  private byte[] classData;
  
  /**
   * Instantiates a new class descriptor.
   * 
   * @param name
   *          the name
   * @param classData
   *          the class data
   * @param file
   *          the file
   */
  public ClassDescriptor(final String name, final byte[] classData, final String file) {
    Validate.notBlank(name);
    Validate.notNull(classData);
    Validate.isTrue(classData.length != 0);
    Validate.notBlank(file);
    this.name = name;
    this.classData = Arrays.copyOf(classData, classData.length);
    this.file = file;
  }
  
  /**
   * Gets the fully qualified name.
   * 
   * @return the name
   */
  public String getName() {
    return name;
  }
  
  /**
   * Gets the class data as bytearray.
   * 
   * @return the classData
   */
  public byte[] getClassData() {
    return Arrays.copyOf(classData, classData.length);
  }
  
  /**
   * A String representation of this object.
   * 
   * @return the string
   */
  @Override
  public String toString() {
    return "ClassDescriptor[" + name + "]";
  }
  
  /**
   * Gets the file this descriptor was read from.
   * 
   * @return the file
   */
  public String getFile() {
    return file;
  }
  
  /**
   * Get the Package of the class.
   */
  public String getPackage() {
    return StringUtils.substringBeforeLast(name, ".");
  }
  
  /**
   * Get the Package of the class as folder
   * ('.' replaced by '/').
   */
  public String getPackageDir() {
    return StringUtils.replace(getPackage(), ".", "/");
  }
  
  /**
   * Get the Simple name (without package) of the class.
   */
  public String getSimpleName() {
    return StringUtils.substringAfterLast(name, ".");
  }
  
  /**
   * Sets the class data.
   * 
   * @param classData
   *          the new class data
   */
  void setClassData(final byte[] classData) {
    this.classData = classData.clone();
  }
}
