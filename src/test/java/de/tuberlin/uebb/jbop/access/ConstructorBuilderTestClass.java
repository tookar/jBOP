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

/**
 * The Class ConstructorBuilderTestClass.
 * 
 * This is a Testclass for {@link ConstructorBuilderTest}.
 * 
 * @author Christopher Ewest
 */
class ConstructorBuilderTestClass {
  
  private final double doubleValue;
  private final int intValue;
  private final String stringValue;
  private final double[] doubleArrayValue;
  
  /**
   * Instantiates a new {@link ConstructorBuilderTestClass}.
   */
  public ConstructorBuilderTestClass() {
    doubleValue = 2.0;
    intValue = 1;
    stringValue = "String";
    doubleArrayValue = new double[] {
        1.0, 2.0, 3.0
    };
  }
  
  /**
   * Gets the double value.
   * 
   * @return the double value
   */
  public double getDoubleValue() {
    return doubleValue;
  }
  
  /**
   * Gets the int value.
   * 
   * @return the int value
   */
  public int getIntValue() {
    return intValue;
  }
  
  /**
   * Gets the string value.
   * 
   * @return the string value
   */
  public String getStringValue() {
    return stringValue;
  }
  
  /**
   * Gets the double array value.
   * 
   * @return the double array value
   */
  public double[] getDoubleArrayValue() {
    return doubleArrayValue;
  }
  
}
