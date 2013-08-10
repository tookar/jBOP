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

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.apache.commons.collections15.map.HashedMap;
import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

import de.tuberlin.uebb.jbop.exception.JBOPClassException;

/**
 * The Class OptimizerUtils.
 * 
 * @author Christopher Ewest
 */
public final class OptimizerUtils {
  
  private static final HashedMap<Object, Object> CACHE = new HashedMap<>();
  
  private OptimizerUtils() {
    //
  }
  
  /**
   * Init
   */
  public static void init() {
    CACHE.clear();
  }
  
  /**
   * Read class.
   * 
   * @param input
   *          the input
   * @return the class node
   */
  public static ClassNode readClass(final Object input) throws JBOPClassException {
    final ClassReader classReader = new ClassReader(ClassAccessor.toBytes(input));
    final ClassNode classNode = new ClassNode(Opcodes.ASM4);
    classReader.accept(classNode, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
    return classNode;
  }
  
  /**
   * Write class and instantiate the Object.
   * The new class (classNode) has to be a subclass of the Type of input.
   * The new class is renamed to "Input name" + "suffix".
   * 
   * @param <T>
   *          the generic type
   * @param classNode
   *          the class node
   * @param input
   *          the input
   * @param suffix
   *          the suffix
   * @return the newe Class-instance
   * @throws JBOPClassException
   *           the jBOP class exception
   */
  public static <T> T createInstance(final ClassNode classNode, final T input, final String suffix)
      throws JBOPClassException {
    final List<Object> params = ConstructorBuilder.createConstructor(classNode, input);
    final ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
    classNode.accept(writer);
    
    final byte[] bytes = writer.toByteArray();
    final T newInstance = instanceOf(bytes, input, params, suffix);
    CACHE.put(input, newInstance);
    return newInstance;
  }
  
  private static <T> T instanceOf(final byte[] newClass, final T originalObject, final List<Object> params,
      final String suffix) throws JBOPClassException {
    final ClassDescriptor classDescriptor = ClassAccessor.getClassDescriptor(originalObject.getClass());
    classDescriptor.setClassData(newClass);
    final ClassDescriptor renamedClass = ClassAccessor.rename(classDescriptor, suffix);
    ClassAccessor.store(renamedClass);
    
    try {
      final Class<?> forName = Class.forName(renamedClass.getName(), true, ClassAccessor.getClassloader());
      return (T) ConstructorUtils.invokeConstructor(forName, params.toArray(new Object[params.size()]));
    } catch (InstantiationException | IllegalAccessException | ClassNotFoundException | NoSuchMethodException
        | InvocationTargetException e) {
      throw new JBOPClassException("Optimized class could not be instantiated.", e);
    }
  }
  
  /**
   * Exists already a specialized instance for input?.
   * 
   * @param input
   *          the input
   * @return true, if successful
   */
  public static boolean existsInstance(final Object input) {
    if (CACHE.containsKey(input)) {
      return true;
    }
    return false;
  }
  
  /**
   * Gets the specialized instance for input.
   * 
   * @param input
   *          the input
   * @return the instance
   */
  public static <T> T getInstanceFor(final T input) {
    return (T) CACHE.get(input);
  }
}
