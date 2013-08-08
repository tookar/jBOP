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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Test;

import de.tuberlin.uebb.jdae.exception.JBOPClassException;

/**
 * Tests for {@link ClassAccessor}.
 * 
 * @author Christopher Ewest
 */
public class ClassAccessorTest {
  
  /**
   * * Tests that stringToClasspath() of the Testobject is working correctly.
   */
  @Test
  public void testStringToClasspath() {
    // INIT
    final String internalName = "de.tuberlin.uebb.jdae.access.ClassAccessorTest";
    
    // RUN
    final String classPath = ClassAccessor.toClassPath(internalName);
    
    // ASSERT
    
    assertEquals("The classpath is not as expected.", "/" + internalName + ".class", classPath);
  }
  
  /**
   * * Tests that classToClasspath() of the Testobject is working correctly.
   */
  @Test
  public void testClassToClasspath() {
    // INIT
    final Class<?> clazz = de.tuberlin.uebb.jdae.access.ClassAccessorTest.class;
    
    // RUN
    final String classPath = ClassAccessor.toClassPath(clazz);
    
    // ASSERT
    
    assertEquals("The classpath is not as expected.", "/" + clazz.getName().replace(".", "/") + ".class", classPath);
  }
  
  /**
   * * Tests that classToFile() of the Testobject is working correctly.
   * 
   * @throws JBOPClassException
   *           the jBOP class exception
   * @throws IOException
   *           Signals that an I/O exception has occurred.
   */
  @Test
  public void testClassToFile() throws JBOPClassException, IOException {
    // INIT
    final Class<?> clazz = de.tuberlin.uebb.jdae.access.ClassAccessorTest.class;
    
    // RUN
    final Path file = ClassAccessor.toPath(clazz);
    
    // ASSERT
    final String expected = new File(".").getCanonicalPath() + "/target/test-classes/"
        + clazz.getName().replace(".", "/") + ".class";
    assertEquals("The classpath is not as expected.", expected, file.toAbsolutePath().toString());
  }
  
  /**
   * * Tests that classToFile() of the Testobject is working correctly for classes in jarFiles.
   * 
   * @throws JBOPClassException
   *           the jBOP class exception
   * @throws IOException
   *           Signals that an I/O exception has occurred.
   */
  @Test
  public void testClassToFileFromJar() throws JBOPClassException, IOException, ClassNotFoundException {
    // INIT
    final URLClassLoader cl = new URLClassLoader(new URL[] {
      getClass().getResource("/access.jar")
    });
    
    final Class<?> clazz = Class.forName("de.tuberlin.uebb.jdae.testdata.TestClass", true, cl);
    
    // RUN
    final Path file = ClassAccessor.toPath(clazz);
    
    // ASSERT
    final String expected = new File(".").getCanonicalPath() + "/target/test-classes/access.jar!/"
        + clazz.getName().replace(".", "/") + ".class";
    assertEquals("The classpath is not as expected.", expected, file.toAbsolutePath().toString());
  }
  
  /**
   * * Tests that getClassDescriptor() of the Testobject is working correctly.
   * 
   * @throws JBOPClassException
   *           the jBOP class exception
   * @throws IOException
   *           Signals that an I/O exception has occurred.
   */
  @Test
  public void testGetClassDescriptor() throws JBOPClassException, IOException {
    // INIT
    final Class<?> clazz = de.tuberlin.uebb.jdae.access.ClassAccessorTest.class;
    
    // RUN
    final ClassDescriptor descriptor = ClassAccessor.getClassDescriptor(clazz);
    
    // ASSERT
    final String expectedFile = new File(".").getCanonicalPath() + "/target/test-classes/"
        + clazz.getName().replace(".", "/") + ".class";
    assertEquals(expectedFile, descriptor.getFile());
    
    assertEquals(clazz.getName(), descriptor.getName());
    
    assertIsClassData(descriptor);
  }
  
  private static void assertIsClassData(final ClassDescriptor descriptor) {
    final byte[] classData = descriptor.getClassData();
    assertTrue(classData.length > 4);
    final byte[] header = new byte[] {
        (byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE,//
        (byte) 0, (byte) 0, //
        (byte) 0, (byte) 51,
    };
    // ASSERT MAGIC NUMBER
    assertEquals(header[0], classData[0]);
    assertEquals(header[1], classData[1]);
    assertEquals(header[2], classData[2]);
    assertEquals(header[3], classData[3]);
    // ASSERT minor Version
    assertEquals(header[4], classData[4]);
    assertEquals(header[5], classData[5]);
    // ASSERT major Version
    assertEquals(header[6], classData[6]);
    assertEquals(header[7], classData[7]);
  }
  
  /**
   * * Tests that rename() and store() of the Testobject are working correctly.
   * 
   * @throws JBOPClassException
   *           the jBOP class exception
   * @throws ClassNotFoundException
   *           the class not found exception
   * @throws IOException
   *           Signals that an I/O exception has occurred.
   */
  @Test
  public void testRenameAndStore() throws JBOPClassException, ClassNotFoundException, IOException {
    
    // INIT
    final Class<?> clazz = de.tuberlin.uebb.jdae.access.ClassAccessorTest.class;
    final ClassDescriptor descriptor = ClassAccessor.getClassDescriptor(clazz);
    
    // RUN
    final ClassDescriptor renamed = ClassAccessor.rename(descriptor, "Renamed");
    
    assertEquals(descriptor.getName() + "Renamed", renamed.getName());
    final String classFileName = descriptor.getFile().replace(".class", "Renamed.class");
    assertEquals(classFileName, renamed.getFile());
    
    assertIsClassData(renamed);
    
    Path stored = null;
    try {
      // RUN
      stored = ClassAccessor.store(renamed);
      
      // ASSERT
      Class.forName(renamed.getName(), true, ClassAccessor.getClassloader());
    } finally {
      // CLEAN
      Files.deleteIfExists(stored);
    }
    
  }
  
  /**
   * * Tests that rename() and store() of the Testobject are working correctly with classes in jars.
   * 
   * @throws JBOPClassException
   *           the jBOP class exception
   * @throws ClassNotFoundException
   *           the class not found exception
   * @throws IOException
   *           Signals that an I/O exception has occurred.
   */
  @Test
  public void testRenameAndStoreInJar() throws JBOPClassException, ClassNotFoundException, IOException {
    
    // INIT
    final URLClassLoader cl = new URLClassLoader(new URL[] {
      getClass().getResource("/access.jar")
    });
    
    final Class<?> clazz = Class.forName("de.tuberlin.uebb.jdae.testdata.TestClass", true, cl);
    final ClassDescriptor descriptor = ClassAccessor.getClassDescriptor(clazz);
    
    // RUN
    final ClassDescriptor renamed = ClassAccessor.rename(descriptor, "Renamed");
    
    assertEquals(descriptor.getName() + "Renamed", renamed.getName());
    final String classFileName = descriptor.getFile().replace(".class", "Renamed.class");
    assertEquals(classFileName, renamed.getFile());
    
    assertIsClassData(renamed);
    
    Path stored = null;
    try {
      // RUN
      stored = ClassAccessor.store(renamed);
      
      // ASSERT
      Class.forName(renamed.getName(), true, ClassAccessor.getClassloader());
    } finally {
      // CLEAN
      Files.deleteIfExists(stored);
    }
    
  }
}
