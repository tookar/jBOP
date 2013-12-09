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
package de.tuberlin.uebb.jbop.optimizer.utils;

import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_SYNTHETIC;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.collections15.CollectionUtils;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * The Class RemoveUnusedFields.
 * A little Helper that removes unused private fields (never read / stored in any method)
 * of the class.
 * 
 * As a side effect all constructors are removed.
 * 
 * Therefore the {@link de.tuberlin.uebb.jbop.access.ConstructorBuilder} has to be used afterwards
 * to create a new valid constructor for the class.
 * 
 * @author Christopher Ewest
 */
public final class RemoveUnusedFields {
  
  private RemoveUnusedFields() {
    // utility class
  }
  
  /**
   * Removes the unused fields.
   * 
   * @param classNode
   *          the class node
   */
  public static void removeUnusedFields(final ClassNode classNode) {
    final Set<FieldNode> usedFields = new HashSet<>();
    collectUsedFields(classNode, usedFields);
    final Collection<FieldNode> unusedFields = CollectionUtils.subtract(classNode.fields, usedFields);
    classNode.fields.removeAll(unusedFields);
    correctConstructors(classNode);
  }
  
  private static void correctConstructors(final ClassNode classNode) {
    for (final MethodNode method : new ArrayList<>(classNode.methods)) {
      if ("<init>".equals(method.name)) {
        classNode.methods.remove(method);
      }
    }
  }
  
  private static void collectUsedFields(final ClassNode classNode, final Set<FieldNode> usedFields) {
    for (final FieldNode fieldNode : classNode.fields) {
      if (!((fieldNode.access & ACC_PRIVATE) != 0)) {
        usedFields.add(fieldNode);
        continue;
      }
      if ((fieldNode.access & ACC_SYNTHETIC) != 0) {
        usedFields.add(fieldNode);
        continue;
      }
      
      collectFieldInClass(classNode, usedFields, fieldNode);
    }
  }
  
  private static void collectFieldInClass(final ClassNode classNode, final Set<FieldNode> usedFields,
      final FieldNode fieldNode) {
    for (final MethodNode methodNode : classNode.methods) {
      collectFieldInMethod(usedFields, fieldNode, methodNode, classNode.name);
    }
  }
  
  private static void collectFieldInMethod(final Set<FieldNode> usedFields, final FieldNode fieldNode,
      final MethodNode methodNode, final String className) {
    for (final Iterator<AbstractInsnNode> iterator = methodNode.instructions.iterator(); iterator.hasNext();) {
      final AbstractInsnNode next = iterator.next();
      
      if (!(next instanceof FieldInsnNode)) {
        continue;
      }
      
      final String fieldname = NodeHelper.getFieldname(next);
      final String owner = NodeHelper.getFieldowner(next);
      
      if (!className.equals(owner)) {
        continue;
      }
      
      if (!fieldNode.name.equals(fieldname)) {
        continue;
      }
      if (!"<init>".equals(methodNode.name)) {
        usedFields.add(fieldNode);
      }
    }
  }
}
