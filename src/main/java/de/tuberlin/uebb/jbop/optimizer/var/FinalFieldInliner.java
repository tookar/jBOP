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
package de.tuberlin.uebb.jbop.optimizer.var;

import java.lang.reflect.Field;
import java.security.AccessController;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;

import de.tuberlin.uebb.jbop.access.OptimizerUtils;
import de.tuberlin.uebb.jbop.exception.JBOPClassException;
import de.tuberlin.uebb.jbop.optimizer.IOptimizer;
import de.tuberlin.uebb.jbop.optimizer.utils.NodeHelper;
import de.tuberlin.uebb.jbop.optimizer.utils.predicates.GetFieldPredicate;

/**
 * The Class FinalFieldInliner.
 * 
 * Inlines final primitive-type Fields.
 * 
 * @author Christopher Ewest
 */
public class FinalFieldInliner implements IOptimizer {
  
  private static final Type intType = Type.getType(int.class);
  private static final Type intObjectType = Type.getType(Integer.class);
  private static final Type longType = Type.getType(long.class);
  private static final Type longObjectType = Type.getType(Long.class);
  private static final Type floatType = Type.getType(float.class);
  private static final Type floatObjectType = Type.getType(Float.class);
  private static final Type doubleType = Type.getType(double.class);
  private static final Type doubleObjectType = Type.getType(Double.class);
  private static final Type stringObjectType = Type.getType(String.class);
  
  private boolean optimized;
  private final Object instance;
  private final Map<String, Field> fieldMap = new TreeMap<String, Field>();
  private final GetFieldPredicate isField;
  
  /**
   * Instantiates a new {@link FinalFieldInliner}.
   * 
   * @param input
   *          the input
   * @throws JBOPClassException
   *           the jBOP class exception
   */
  public FinalFieldInliner(final Object input) throws JBOPClassException {
    instance = input;
    final Class<?> clazz = input.getClass();
    final ClassNode readClass = OptimizerUtils.readClass(input);
    final List<FieldNode> fields = readClass.fields;
    for (final FieldNode field : fields) {
      if ((field.access & Opcodes.ACC_FINAL) != 0) {
        if (isPrimitive(field.desc)) {
          try {
            fieldMap.put(field.name, clazz.getDeclaredField(field.name));
          } catch (NoSuchFieldException | SecurityException e) {
            throw new JBOPClassException("There is no Field '" + field.name + "' in Class<" + clazz.getName() + ">.", e);
          }
        }
      }
    }
    isField = new GetFieldPredicate(fieldMap);
  }
  
  private static boolean isPrimitive(final String desc) {
    final Type type = Type.getType(desc);
    if (type.equals(intType)) {
      return true;
    }
    if (type.equals(longType)) {
      return true;
    }
    if (type.equals(floatType)) {
      return true;
    }
    if (type.equals(doubleType)) {
      return true;
    }
    if (type.equals(intObjectType)) {
      return true;
    }
    if (type.equals(longObjectType)) {
      return true;
    }
    if (type.equals(floatObjectType)) {
      return true;
    }
    if (type.equals(doubleObjectType)) {
      return true;
    }
    if (type.equals(stringObjectType)) {
      return true;
    }
    return false;
  }
  
  @Override
  public boolean isOptimized() {
    return optimized;
  }
  
  @Override
  public InsnList optimize(final InsnList original, final MethodNode methodNode) throws JBOPClassException {
    optimized = false;
    final Iterator<AbstractInsnNode> iterator = original.iterator();
    while (iterator.hasNext()) {
      final AbstractInsnNode currentNode = iterator.next();
      if (!NodeHelper.isAload(currentNode)) {
        continue;
      }
      final AbstractInsnNode getfield = NodeHelper.getNext(currentNode);
      if (!isField.evaluate(getfield)) {
        continue;
      }
      final Object value = getValue(getfield);
      final AbstractInsnNode replacement = NodeHelper.getInsnNodeFor(value);
      
      original.insertBefore(currentNode, replacement);
      original.remove(currentNode);
      iterator.next();
      original.remove(getfield);
    }
    return original;
  }
  
  private Object getValue(final AbstractInsnNode getfield) throws JBOPClassException {
    final String name = ((FieldInsnNode) getfield).name;
    final Field field = fieldMap.get(name);
    try {
      return AccessController.doPrivileged(new PrivilegedGetFieldValue(name, field, instance));
    } catch (final RuntimeException re) {
      throw new JBOPClassException(re.getMessage(), re.getCause());
    }
  }
}
