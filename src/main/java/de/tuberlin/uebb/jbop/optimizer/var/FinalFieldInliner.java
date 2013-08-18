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

import static org.objectweb.asm.Type.BOOLEAN_TYPE;
import static org.objectweb.asm.Type.BYTE_TYPE;
import static org.objectweb.asm.Type.CHAR_TYPE;
import static org.objectweb.asm.Type.DOUBLE_TYPE;
import static org.objectweb.asm.Type.FLOAT_TYPE;
import static org.objectweb.asm.Type.INT_TYPE;
import static org.objectweb.asm.Type.LONG_TYPE;
import static org.objectweb.asm.Type.SHORT_TYPE;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;

import de.tuberlin.uebb.jbop.access.ClassAccessor;
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
  
  private static final Type INT_OBJECT_TYPE = Type.getType(Integer.class);
  private static final Type LONG_OBJECT_TYPE = Type.getType(Long.class);
  private static final Type FLOAT_OBJECT_TYPE = Type.getType(Float.class);
  private static final Type DOUBLE_OBJECT_TYPE = Type.getType(Double.class);
  private static final Type STRING_OBJECT_TYPE = Type.getType(String.class);
  private static final Object SHORT_OBJECT_TYPE = Type.getType(Short.class);
  private static final Object BYTE_OBJECT_TYPE = Type.getType(Byte.class);
  private static final Object CHAR_OBJECT_TYPE = Type.getType(Character.class);
  private static final Object BOOLEAN_OBJECT_TYPE = Type.getType(Boolean.class);
  
  private boolean optimized;
  private final Object instance;
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
    final ClassNode readClass = OptimizerUtils.readClass(input);
    final List<FieldNode> fieldNodes = readClass.fields;
    final Set<String> fields = collectFields(fieldNodes);
    isField = new GetFieldPredicate(fields);
  }
  
  private Set<String> collectFields(final List<FieldNode> fieldNodes) {
    final Set<String> fields = new HashSet<>();
    for (final FieldNode field : fieldNodes) {
      if ((field.access & Opcodes.ACC_FINAL) != 0) {
        if (isBuiltIn(field.desc)) {
          fields.add(field.name);
        }
      }
    }
    return fields;
  }
  
  private static boolean isBuiltIn(final String desc) {
    final Type type = Type.getType(desc);
    if (isPrimitive(type)) {
      return true;
    }
    return isPrimitiveWrapper(type);
  }
  
  private static boolean isPrimitiveWrapper(final Type type) {
    if (INT_OBJECT_TYPE.equals(type)) {
      return true;
    }
    if (LONG_OBJECT_TYPE.equals(type)) {
      return true;
    }
    if (FLOAT_OBJECT_TYPE.equals(type)) {
      return true;
    }
    if (DOUBLE_OBJECT_TYPE.equals(type)) {
      return true;
    }
    if (SHORT_OBJECT_TYPE.equals(type)) {
      return true;
    }
    if (BYTE_OBJECT_TYPE.equals(type)) {
      return true;
    }
    if (CHAR_OBJECT_TYPE.equals(type)) {
      return true;
    }
    if (BOOLEAN_OBJECT_TYPE.equals(type)) {
      return true;
    }
    if (STRING_OBJECT_TYPE.equals(type)) {
      return true;
    }
    return false;
  }
  
  private static boolean isPrimitive(final Type type) {
    if (INT_TYPE.equals(type)) {
      return true;
    }
    if (LONG_TYPE.equals(type)) {
      return true;
    }
    if (FLOAT_TYPE.equals(type)) {
      return true;
    }
    if (DOUBLE_TYPE.equals(type)) {
      return true;
    }
    if (SHORT_TYPE.equals(type)) {
      return true;
    }
    if (BYTE_TYPE.equals(type)) {
      return true;
    }
    if (CHAR_TYPE.equals(type)) {
      return true;
    }
    if (BOOLEAN_TYPE.equals(type)) {
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
  
  private Object getValue(final AbstractInsnNode getFieldNode) throws JBOPClassException {
    return ClassAccessor.getCurrentValue(instance, NodeHelper.getFieldname(getFieldNode));
  }
}
