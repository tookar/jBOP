package de.tuberlin.uebb.jbop.access;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.List;

import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

import de.tuberlin.uebb.jbop.exception.JBOPClassException;

public class ConstructorBuilderTest {
  
  @Test
  public void testConstructorBuilder() throws Exception {
    // INIT
    final ConstructorBuilderTestClass testClass = new ConstructorBuilderTestClass();
    final ClassNode classNode = new ClassNode(Opcodes.ASM4);
    new ClassReader("de.tuberlin.uebb.jbop.access.ConstructorBuilderTestClass").accept(classNode,
        ClassReader.SKIP_FRAMES);
    
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
