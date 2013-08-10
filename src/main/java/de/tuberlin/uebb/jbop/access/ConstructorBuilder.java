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
import java.util.List;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
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
final class ConstructorBuilder {
  
  private ConstructorBuilder() {
    //
  }
  
  /**
   * Creates a constructor with all fields of <code>object</code> as parameters
   * and adds it to the method-List of <code>node</code>.
   * A List with all Values (in order of the parameters) is returned.
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
      desc.append(field.desc);
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
    int opcode = Opcodes.ALOAD;
    int nextParam = param + 1;
    final int sort = Type.getType(field.desc).getSort();
    if (sort == Type.INT) {
      opcode = Opcodes.ILOAD;
    } else if (sort == Type.LONG) {
      nextParam++;
      opcode = Opcodes.LLOAD;
    } else if (sort == Type.FLOAT) {
      opcode = Opcodes.FLOAD;
    } else if (sort == Type.DOUBLE) {
      nextParam++;
      opcode = Opcodes.DLOAD;
    } else {
      opcode = Opcodes.ALOAD;
    }
    final AbstractInsnNode nParam = new VarInsnNode(opcode, param);
    final AbstractInsnNode nPut = new FieldInsnNode(Opcodes.PUTFIELD, node.name, field.name, field.desc);
    instructions.add(nThis);
    instructions.add(nParam);
    instructions.add(nPut);
    return nextParam;
  }
  
  private static MethodNode createMethodNode(final ClassNode node) {
    final MethodNode constructor = new MethodNode();
    constructor.access = Opcodes.ACC_PUBLIC;
    constructor.name = "<init>";
    final InsnList list = new InsnList();
    // final AbstractInsnNode nThis = new VarInsnNode(Opcodes.ALOAD, 0);
    // final AbstractInsnNode nSuperConstructor = new MethodInsnNode(Opcodes.INVOKESPECIAL, node.superName, "<init>",
    // "()V");
    // list.add(nThis);
    // list.add(nSuperConstructor);
    constructor.instructions = list;
    return constructor;
  }
}
