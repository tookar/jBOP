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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import de.tuberlin.uebb.jbop.exception.JBOPClassException;

/**
 * The Class ConstructorBuilder.
 * 
 * Creates a Constructor for the given Objects
 * and adds it to the list of methods.
 * 
 * @author Christopher Ewest
 */
public final class ConstructorBuilder {
  
  private ConstructorBuilder() {
    //
  }
  
  /**
   * Creates a constructor with all fields of <code>object</code> as parameters
   * and adds it to the method-List of <code>node</code>.
   * A List with all Values (in order of the parameters) is returned.
   * 
   * If such a constructor already exists, only the parameters are returned,
   * no change is made to to the classNode.
   * 
   * @param node
   *          the ClassNode
   * @param object
   *          the input Object
   * @return the value list
   * @throws JBOPClassException
   *           if the Constructor couldn't be created.
   */
  public static List<Object> createConstructor(final ClassNode node, final Object object) throws JBOPClassException {
    final Class<? extends Object> clazz = object.getClass();
    int param = 1;
    final StringBuilder desc = new StringBuilder("(");
    final MethodNode constructor = createMethodNode(node);
    final List<Object> params = new ArrayList<>();
    for (final FieldNode field : node.fields) {
      final InsnList instructions = new InsnList();
      param = createInstructions(param, field, node, instructions);
      final Object value = getValue(clazz, field, object);
      params.add(value);
      constructor.instructions.add(instructions);
      desc.append(expand(field.desc, false));
    }
    constructor.instructions.add(new InsnNode(Opcodes.RETURN));
    desc.append(")V");
    constructor.desc = desc.toString();
    for (final MethodNode method : node.methods) {
      if ("<init>".equals(method.name)) {
        if (constructor.desc.equals(method.desc)) {
          return params;
        }
      }
    }
    node.methods.add(constructor);
    return params;
  }
  
  private static String expand(final String desc, final boolean isArray) {
    if (desc.startsWith("L")) {
      return desc;
    }
    if (desc.startsWith("[")) {
      return Type.getType("[" + expand(desc.substring(1), true)).getDescriptor();
    }
    if (isArray) {
      return desc;
    }
    return expandPrimitives(desc);
  }
  
  private static String expandPrimitives(final String desc) {
    if ("I".equals(desc)) {
      return Type.getDescriptor(Integer.class);
    }
    if ("F".equals(desc)) {
      return Type.getDescriptor(Float.class);
    }
    if ("J".equals(desc)) {
      return Type.getDescriptor(Long.class);
    }
    if ("D".equals(desc)) {
      return Type.getDescriptor(Double.class);
    }
    if ("S".equals(desc)) {
      return Type.getDescriptor(Short.class);
    }
    if ("B".equals(desc)) {
      return Type.getDescriptor(Byte.class);
    }
    if ("C".equals(desc)) {
      return Type.getDescriptor(Character.class);
    }
    if ("Z".equals(desc)) {
      return Type.getDescriptor(Boolean.class);
    }
    return Type.getDescriptor(Object.class);
  }
  
  private static Object getValue(final Class<? extends Object> clazz, final FieldNode field, final Object object)
      throws JBOPClassException {
    final Field declaredField;
    try {
      declaredField = clazz.getDeclaredField(field.name);
    } catch (NoSuchFieldException | SecurityException e) {
      throw new JBOPClassException("Error accessing class parameters", e);
    }
    final boolean accessible = declaredField.isAccessible();
    declaredField.setAccessible(true);
    final Object value;
    try {
      value = declaredField.get(object);
    } catch (IllegalArgumentException | IllegalAccessException e) {
      throw new JBOPClassException("Error accessing class parameters", e);
    } finally {
      declaredField.setAccessible(accessible);
    }
    return value;
  }
  
  private static int createInstructions(final int param, final FieldNode field, final ClassNode node,
      final InsnList instructions) {
    final AbstractInsnNode nThis = new VarInsnNode(Opcodes.ALOAD, 0);
    final int opcode = Opcodes.ALOAD;
    final int nextParam = param + 1;
    final AbstractInsnNode unboxing = getUnboxingNode(field);
    
    instructions.add(nThis);
    final AbstractInsnNode nParam = new VarInsnNode(opcode, param);
    instructions.add(nParam);
    
    if (unboxing != null) {
      instructions.add(unboxing);
    }
    
    final AbstractInsnNode nPut = new FieldInsnNode(Opcodes.PUTFIELD, node.name, field.name, field.desc);
    instructions.add(nPut);
    
    return nextParam;
  }
  
  /**
   * Gets the unboxing node.
   * 
   * @param field
   *          the field
   * @return the unboxing node
   */
  public static AbstractInsnNode getUnboxingNode(final FieldNode field) {
    final AbstractInsnNode unboxing;
    final Type type = Type.getType(field.desc);
    final int sort = type.getSort();
    if (sort == Type.INT) {
      unboxing = new MethodInsnNode(Opcodes.INVOKEVIRTUAL, Type.getInternalName(Integer.class), "intValue", "()I");
    } else if (sort == Type.LONG) {
      unboxing = new MethodInsnNode(Opcodes.INVOKEVIRTUAL, Type.getInternalName(Long.class), "longValue", "()J");
    } else if (sort == Type.FLOAT) {
      unboxing = new MethodInsnNode(Opcodes.INVOKEVIRTUAL, Type.getInternalName(Float.class), "intValue", "()F");
    } else if (sort == Type.DOUBLE) {
      unboxing = new MethodInsnNode(Opcodes.INVOKEVIRTUAL, Type.getInternalName(Double.class), "doubleValue", "()D");
    } else if (sort == Type.BOOLEAN) {
      unboxing = new MethodInsnNode(Opcodes.INVOKEVIRTUAL, Type.getInternalName(Boolean.class), "booleanValue", "()Z");
    } else if (sort == Type.SHORT) {
      unboxing = new MethodInsnNode(Opcodes.INVOKEVIRTUAL, Type.getInternalName(Short.class), "shortValue", "()S");
    } else if (sort == Type.CHAR) {
      unboxing = new MethodInsnNode(Opcodes.INVOKEVIRTUAL, Type.getInternalName(Character.class), "charValue", "()C");
    } else if (sort == Type.BYTE) {
      unboxing = new MethodInsnNode(Opcodes.INVOKEVIRTUAL, Type.getInternalName(Byte.class), "byteValue", "()B");
    } else {
      unboxing = null;
    }
    return unboxing;
  }
  
  /**
   * Gets the boxing node.
   * 
   * @param field
   *          the field
   * @return the boxing node
   */
  public static AbstractInsnNode getBoxingNode(final FieldNode field) {
    final AbstractInsnNode boxing;
    Type type = Type.getType(field.desc);
    if (field.desc.startsWith("[")) {
      type = type.getElementType();
    }
    final int sort = type.getSort();
    if (sort == Type.INT) {
      boxing = new MethodInsnNode(Opcodes.INVOKESTATIC, Type.getInternalName(Integer.class), "valueOf",
          "(D)Ljava/lang/Integer;");
    } else if (sort == Type.LONG) {
      boxing = new MethodInsnNode(Opcodes.INVOKESTATIC, Type.getInternalName(Long.class), "valueOf",
          "(D)Ljava/lang/Long;");
    } else if (sort == Type.FLOAT) {
      boxing = new MethodInsnNode(Opcodes.INVOKESTATIC, Type.getInternalName(Float.class), "valueOf",
          "(D)Ljava/lang/Float;");
    } else if (sort == Type.DOUBLE) {
      boxing = new MethodInsnNode(Opcodes.INVOKESTATIC, Type.getInternalName(Integer.class), "valueOf",
          "(D)Ljava/lang/Double;");
    } else if (sort == Type.BOOLEAN) {
      boxing = new MethodInsnNode(Opcodes.INVOKESTATIC, Type.getInternalName(Integer.class), "valueOf",
          "(D)Ljava/lang/Boolean;");
    } else if (sort == Type.SHORT) {
      boxing = new MethodInsnNode(Opcodes.INVOKESTATIC, Type.getInternalName(Integer.class), "valueOf",
          "(D)Ljava/lang/Short;");
    } else if (sort == Type.CHAR) {
      boxing = new MethodInsnNode(Opcodes.INVOKESTATIC, Type.getInternalName(Integer.class), "valueOf",
          "(D)Ljava/lang/Character;");
    } else if (sort == Type.BYTE) {
      boxing = new MethodInsnNode(Opcodes.INVOKESTATIC, Type.getInternalName(Integer.class), "valueOf",
          "(D)Ljava/lang/Byte;");
    } else {
      boxing = null;
    }
    return boxing;
  }
  
  private static MethodNode createMethodNode(final ClassNode node) {
    final MethodNode constructor = new MethodNode();
    constructor.access = Opcodes.ACC_PUBLIC;
    constructor.name = "<init>";
    constructor.exceptions = Collections.emptyList();
    final InsnList list = new InsnList();
    // currently only call to noarg super constructor is supported
    final AbstractInsnNode nThis = new VarInsnNode(Opcodes.ALOAD, 0);
    final AbstractInsnNode nSuperConstructor = new MethodInsnNode(Opcodes.INVOKESPECIAL, node.superName, "<init>",
        "()V");
    list.add(nThis);
    list.add(nSuperConstructor);
    constructor.instructions = list;
    return constructor;
  }
}
