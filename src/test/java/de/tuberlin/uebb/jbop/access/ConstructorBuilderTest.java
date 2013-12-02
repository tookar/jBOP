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

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.List;

import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.junit.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

import de.tuberlin.uebb.jbop.exception.JBOPClassException;
import de.tuberlin.uebb.jbop.optimizer.ClassNodeBuilder;

/**
 * Tests for {@link ConstructorBuilder}.
 * 
 * @author Christopher Ewest
 */
public class ConstructorBuilderTest {
  
  /**
   * Tests that constructorBuilder() of the Testobject is working correctly.
   * 
   * @throws Exception
   *           the exception
   */
  @Test
  public void testConstructorBuilder() throws Exception {
    // INIT
    final ClassNodeBuilder builder = ClassNodeBuilder
        .createClass("de.tuberlin.uebb.jbop.access.ConstructorBuilderTestClass").//
        addField("doubleValue", "D").initWith(2.0).withGetter().//
        addField("intValue", "I").initWith(1).withGetter().//
        addField("stringValue", Type.getDescriptor(String.class)).initWith("String").withGetter().//
        addField("doubleArrayValue", "[D").initArrayWith(1.0, 2.0, 3.0).withGetter();
    final ClassNode classNode = builder.getClassNode();
    final Object testClass = builder.toClass().instance();
    
    // RUN
    final List<Object> parameterValues = ConstructorBuilder.createConstructor(classNode, testClass);
    
    // ASSERT
    assertEquals(4, parameterValues.size());
    assertEquals(2.0, ((Double) parameterValues.get(0)).doubleValue(), .00001);
    assertEquals(1, ((Integer) parameterValues.get(1)).intValue());
    assertEquals("String", parameterValues.get(2));
    final Object array = parameterValues.get(3);
    assertEquals(1.0, ((Double) Array.get(array, 0)).doubleValue(), .00001);
    assertEquals(2.0, ((Double) Array.get(array, 1)).doubleValue(), .00001);
    assertEquals(3.0, ((Double) Array.get(array, 2)).doubleValue(), .00001);
    
    final Class<?> newClass = getClass(classNode, testClass);
    
    final Object testClassWithConstructor = ConstructorUtils.invokeConstructor(newClass,
        parameterValues.toArray(new Object[parameterValues.size()]));
    
    assertEquals(2.0, ((Double) invoke(newClass, "getDoubleValue", testClassWithConstructor)).doubleValue(), .00001);
    assertEquals(1, ((Integer) invoke(newClass, "getIntValue", testClassWithConstructor)).intValue());
    assertEquals("String", invoke(newClass, "getStringValue", testClassWithConstructor));
    final Object array2 = invoke(newClass, "getDoubleArrayValue", testClassWithConstructor);
    assertEquals(1.0, ((Double) Array.get(array2, 0)).doubleValue(), .00001);
    assertEquals(2.0, ((Double) Array.get(array2, 1)).doubleValue(), .00001);
    assertEquals(3.0, ((Double) Array.get(array2, 2)).doubleValue(), .00001);
  }
  
  private Object invoke(final Class<?> clazz, final String methodName, final Object object) throws Exception {
    final Method method = clazz.getDeclaredMethod(methodName, new Class<?>[] {});
    method.setAccessible(true);
    return method.invoke(object, new Object[] {});
  }
  
  private Class<?> getClass(final ClassNode classNode, final Object object) throws JBOPClassException,
      ClassNotFoundException {
    final ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
    classNode.accept(writer);
    
    final byte[] bytes = writer.toByteArray();
    
    final ClassDescriptor classDescriptor = ClassAccessor.getClassDescriptor(object.getClass());
    classDescriptor.setClassData(bytes);
    final ClassDescriptor renamedClass = ClassAccessor.rename(classDescriptor, "WithConstructor");
    ClassAccessor.store(renamedClass);
    
    return Class.forName(renamedClass.getName(), true, ClassAccessor.getClassloader());
  }
}
