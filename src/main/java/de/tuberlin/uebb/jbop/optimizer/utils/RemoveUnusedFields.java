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
import static org.objectweb.asm.Opcodes.INVOKESTATIC;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.collections15.CollectionUtils;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * The Class RemoveUnusedFields.
 * A little Helper that removes unused private fields (never read / stored) in any method of the class.
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
    final Set<FieldNode> usedInConstructor = new HashSet<>();
    collectUsedFields(classNode, usedFields, usedInConstructor);
    final Collection<FieldNode> usedOnlyInConstructor = CollectionUtils.subtract(usedInConstructor, usedFields);
    final Collection<FieldNode> unusedFields = CollectionUtils.subtract(classNode.fields, usedFields);
    classNode.fields.removeAll(unusedFields);
    correctConstructors(classNode, usedOnlyInConstructor);
  }
  
  private static void correctConstructors(final ClassNode classNode, //
      final Collection<FieldNode> usedOnlyInConstructor) {
    for (final MethodNode method : classNode.methods) {
      if ("<init>".equals(method.name)) {
        for (final Iterator<AbstractInsnNode> iterator = method.instructions.iterator(); iterator.hasNext();) {
          AbstractInsnNode next = iterator.next();
          if (!(next instanceof FieldInsnNode)) {
            continue;
          }
          
          int aloadCounter = 0;
          for (final FieldNode node : usedOnlyInConstructor) {
            if (node.name.equals(NodeHelper.getFieldname(next))) {
              while (true) {
                final AbstractInsnNode toBeRemoved = next;
                next = NodeHelper.getPrevious(next);
                if (next instanceof FieldInsnNode) {
                  aloadCounter++;
                }
                if (next instanceof MethodInsnNode) {
                  if (next.getOpcode() != INVOKESTATIC) {
                    if (((MethodInsnNode) next).owner.equals(classNode.name)) {
                      aloadCounter++;
                    }
                  }
                }
                method.instructions.remove(toBeRemoved);
                if (NodeHelper.isAload0(toBeRemoved)) {
                  if (aloadCounter == 0) {
                    break;
                  }
                  aloadCounter--;
                }
              }
            }
          }
        }
      }
    }
  }
  
  private static void collectUsedFields(final ClassNode classNode, final Set<FieldNode> usedFields,
      final Set<FieldNode> usedInConstructor) {
    for (final FieldNode fieldNode : classNode.fields) {
      if (!((fieldNode.access & ACC_PRIVATE) != 0)) {
        usedFields.add(fieldNode);
        continue;
      }
      if ((fieldNode.access & ACC_SYNTHETIC) != 0) {
        usedFields.add(fieldNode);
        continue;
      }
      
      collectFieldInClass(classNode, usedFields, usedInConstructor, fieldNode);
    }
  }
  
  private static void collectFieldInClass(final ClassNode classNode, final Set<FieldNode> usedFields,
      final Set<FieldNode> usedInConstructor, final FieldNode fieldNode) {
    for (final MethodNode methodNode : classNode.methods) {
      collectFieldInMethod(usedFields, usedInConstructor, fieldNode, methodNode, classNode.name);
    }
  }
  
  private static void collectFieldInMethod(final Set<FieldNode> usedFields, final Set<FieldNode> usedInConstructor,
      final FieldNode fieldNode, final MethodNode methodNode, final String className) {
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
      if ("<init>".equals(methodNode.name)) {
        usedInConstructor.add(fieldNode);
      } else {
        usedFields.add(fieldNode);
      }
    }
  }
}
