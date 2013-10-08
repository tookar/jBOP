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
import static org.objectweb.asm.Opcodes.GOTO;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.NEW;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.collections15.CollectionUtils;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.JumpInsnNode;
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
        correctConstructor(classNode, usedOnlyInConstructor, method);
      }
    }
  }
  
  private static void correctConstructor(final ClassNode classNode, final Collection<FieldNode> usedOnlyInConstructor,
      final MethodNode method) {
    for (final Iterator<AbstractInsnNode> iterator = method.instructions.iterator(); iterator.hasNext();) {
      final AbstractInsnNode next = iterator.next();
      if (!isOnlyUsedInConstrucorField(usedOnlyInConstructor, next)) {
        continue;
      }
      
      AbstractInsnNode currentNode = NodeHelper.getPrevious(next);
      method.instructions.remove(next);
      
      int aloadCounter = 0;
      int conditionalCounter = 0;
      int gotoCounter = 0;
      
      while (currentNode != null) {
        
        if (isSuperConstructorCall(classNode.superName, currentNode)) {
          aloadCounter = 0;
          conditionalCounter = 0;
          gotoCounter = 0;
          break;
        }
        
        if (NodeHelper.isAload0(currentNode)) {
          if ((aloadCounter == 0) && (conditionalCounter == 0) && (gotoCounter == 0)) {
            method.instructions.remove(currentNode);
            break;
          }
          aloadCounter++;
          currentNode = getPrevAndRemove(method, currentNode);
          
        } else if (currentNode instanceof JumpInsnNode) {
          if (currentNode.getOpcode() == GOTO) {
            gotoCounter++;
            if (gotoCounter > conditionalCounter) {
              currentNode = getPrevAndRemove(method, currentNode);
            } else {
              currentNode = NodeHelper.getPrevious(currentNode);
            }
          } else {
            if (conditionalCounter < gotoCounter) {
              currentNode = getPrevAndRemove(method, currentNode);
            } else {
              currentNode = NodeHelper.getPrevious(currentNode);
              conditionalCounter++;
            }
          }
        } else {
          currentNode = getPrevAndRemove(method, currentNode);
        }
        
      }
      
    }
  }
  
  private static AbstractInsnNode getPrevAndRemove(final MethodNode method, final AbstractInsnNode currentNode) {
    final AbstractInsnNode prev = NodeHelper.getPrevious(currentNode);
    method.instructions.remove(currentNode);
    return prev;
  }
  
  private static boolean isSuperConstructorCall(final String superClass, final AbstractInsnNode currentNode) {
    if (currentNode.getOpcode() != INVOKESPECIAL) {
      return false;
    }
    if (!"<init>".equals(NodeHelper.getMethodName(currentNode))) {
      return false;
    }
    if (!superClass.equals(NodeHelper.getMethodOwner(currentNode))) {
      return false;
    }
    AbstractInsnNode prev = NodeHelper.getPrevious(currentNode);
    while (prev != null) {
      if (prev.getOpcode() == NEW) {
        return false;
      }
      prev = NodeHelper.getPrevious(prev);
    }
    return true;
  }
  
  private static boolean isOnlyUsedInConstrucorField(final Collection<FieldNode> usedOnlyInConstructor,
      final AbstractInsnNode next) {
    if (!(next instanceof FieldInsnNode)) {
      return false;
    }
    for (final FieldNode fieldNode : usedOnlyInConstructor) {
      if (fieldNode.name.equals(NodeHelper.getFieldname(next))) {
        return true;
      }
    }
    return false;
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
