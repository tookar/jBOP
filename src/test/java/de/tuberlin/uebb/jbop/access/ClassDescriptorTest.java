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
package de.tuberlin.uebb.jdae.access;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

import de.tuberlin.uebb.jdae.access.ClassDescriptor;

/**
 * The Class ClassDescriptorTest.
 * 
 * @author Christopher Ewest
 */
public class ClassDescriptorTest {
  
  private static final String NAME = "de.tuberlin.uebb.jdae.Test";
  private static final String PATH = "src/main/java/de/tuberlin/uebb/jdae/Test";
  private static final byte[] CLASS_DATA = new byte[] {
      -10, -9, -8, -7, -6, -5, -4, -3, -2, -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10
  };
  
  /**
   * Test class descriptor name is empty.
   */
  @SuppressWarnings("unused")
  @Test
  public void testClassDescriptorNameIsNull() {
    // INIT
    // nothing
    
    // RUN
    try {
      new ClassDescriptor(null, CLASS_DATA, PATH);
      fail("Expected Exception was not thrown.");
    } catch (final NullPointerException npe) {
      // ASSERT
      assertEquals("The validated character sequence is blank", npe.getMessage());
    }
  }
  
  /**
   * Test class descriptor name is empty.
   */
  @SuppressWarnings("unused")
  @Test
  public void testClassDescriptorNameIsEmpty() {
    // INIT
    // nothing
    
    // RUN
    try {
      new ClassDescriptor("", CLASS_DATA, PATH);
      fail("Expected Exception was not thrown.");
    } catch (final IllegalArgumentException iae) {
      // ASSERT
      assertEquals("The validated character sequence is blank", iae.getMessage());
    }
  }
  
  /**
   * Test class descriptor class data is null.
   */
  @SuppressWarnings("unused")
  @Test
  public void testClassDescriptorClassDataIsNull() {
    // INIT
    // nothing
    
    // RUN
    try {
      new ClassDescriptor(NAME, null, PATH);
      fail("Expected Exception was not thrown.");
    } catch (final NullPointerException npe) {
      // ASSERT
      assertEquals("The validated object is null", npe.getMessage());
    }
  }
  
  /**
   * Test class descriptor class data is empty.
   */
  @SuppressWarnings("unused")
  @Test
  public void testClassDescriptorClassDataIsEmpty() {
    // INIT
    // nothing
    
    // RUN
    try {
      new ClassDescriptor(NAME, new byte[] {}, PATH);
      fail("Expected Exception was not thrown.");
    } catch (final IllegalArgumentException iae) {
      // ASSERT
      assertEquals("The validated expression is false", iae.getMessage());
    }
  }
  
  /**
   * Test class descriptor is empty.
   */
  @SuppressWarnings("unused")
  @Test
  public void testClassDescriptorPathIsEmpty() {
    // INIT
    // nothing
    
    // RUN
    try {
      new ClassDescriptor(NAME, CLASS_DATA, "");
      fail("Expected Exception was not thrown.");
    } catch (final IllegalArgumentException iae) {
      // ASSERT
      assertEquals("The validated character sequence is blank", iae.getMessage());
    }
  }
  
  /**
   * Test class descriptor is empty.
   */
  @SuppressWarnings("unused")
  @Test
  public void testClassDescriptorPathIsNull() {
    // INIT
    // nothing
    
    // RUN
    try {
      new ClassDescriptor(NAME, CLASS_DATA, null);
      fail("Expected Exception was not thrown.");
    } catch (final NullPointerException npe) {
      // ASSERT
      assertEquals("The validated character sequence is blank", npe.getMessage());
    }
  }
  
  /**
   * Test class descriptor.
   */
  @Test
  public void testClassDescriptor() {
    // INIT
    // nothing
    
    // RUN
    final ClassDescriptor classDescriptor = new ClassDescriptor(NAME, CLASS_DATA, PATH);
    
    // ASSERT
    assertEquals(NAME, classDescriptor.getName());
    assertEquals(PATH, classDescriptor.getFile());
    assertArrayEquals(CLASS_DATA, classDescriptor.getClassData());
    assertEquals("ClassDescriptor[" + NAME + "]", classDescriptor.toString());
  }
}
