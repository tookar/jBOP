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

import static org.objectweb.asm.Opcodes.ARRAYLENGTH;
import static org.objectweb.asm.Opcodes.GETFIELD;
import static org.objectweb.asm.Opcodes.IALOAD;
import static org.objectweb.asm.Opcodes.SALOAD;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;

import de.tuberlin.uebb.jbop.access.ClassAccessor;
import de.tuberlin.uebb.jbop.exception.JBOPClassException;
import de.tuberlin.uebb.jbop.optimizer.IInputObjectAware;
import de.tuberlin.uebb.jbop.optimizer.IOptimizer;
import de.tuberlin.uebb.jbop.optimizer.annotations.ImmutableArray;
import de.tuberlin.uebb.jbop.optimizer.utils.NodeHelper;

/**
 * The Class GetFieldChainInliner.
 * 
 * This Optimizer inlines chains of getField-Instructions.
 * eg:
 * 
 * <pre>
 * int e = a.b[2].c[1].d;
 * (where d is 1)
 * </pre>
 * 
 * will become
 * 
 * <pre>
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * int e = 1;
 * </pre>
 * 
 * In bytecode this means:
 * 
 * <pre>
 * 01 aload 0
 * 02 getfield a
 * 03 getfield b
 * 04 iconst 2
 * 05 iaload
 * 06 getfield c
 * 07 iconst 1
 * 08 iaload
 * 09 getfield d
 * 10 istore x
 * </pre>
 * 
 * will become
 * 
 * <pre>
 * 1 iconst 1
 * 2 istore x
 * </pre>
 * 
 * In fact the first aload instruction is not part of this optimizer, therefore this optimizer
 * should only be used as a sub-step for other optimizers. it cannot run standalone.
 * 
 * This Optimizer modifies the original InsnList and returns the original InsnList.
 * 
 * The iterator to work on has to be set via {@link #setIterator(ListIterator)} before calling
 * {@link #optimize(InsnList, MethodNode)}.
 * 
 * At the end of {@link #optimize(InsnList, MethodNode)} the iterator is at the correct position:
 * <ul>
 * <li>it is not changed if no optimization was performed</li>
 * <li>it is after the modified node if there was an optimization</li>
 * </ul>
 * 
 * @author Christopher Ewest
 */
public class GetFieldChainInliner implements IOptimizer, IInputObjectAware, IIteratorAware<AbstractInsnNode> {
  
  private boolean optimized;
  private Object input;
  private ListIterator<AbstractInsnNode> iterator = Collections.<AbstractInsnNode> emptyList().listIterator();
  
  @Override
  public boolean isOptimized() {
    return optimized;
  }
  
  @Override
  public InsnList optimize(final InsnList original, final MethodNode methodNode) throws JBOPClassException {
    optimized = false;
    final ListIterator<AbstractInsnNode> localIterator = original.iterator(iterator.nextIndex());
    Object object = input;
    Object lastObject = null;
    final List<AbstractInsnNode> nodes = new ArrayList<>();
    String fieldname = null;
    while (localIterator.hasNext()) {
      final AbstractInsnNode next = localIterator.next();
      if (next.getOpcode() == GETFIELD) {
        fieldname = NodeHelper.getFieldname(next);
        if (!ClassAccessor.isFinal(object, fieldname)) {
          correctIterator(localIterator);
          return original;
        }
        nodes.add(next);
        lastObject = object;
        object = ClassAccessor.getCurrentValue(object, fieldname);
        
      } else {
        if (object == input) {
          correctIterator(localIterator);
          return original;
        }
        final Type type = Type.getType(object.getClass());
        final String descriptor = type.getDescriptor();
        
        if (handleBuiltIn(descriptor, original, next, object, nodes, localIterator)) {
          optimized = true;
          return original;
        }
        object = handleArray(type, nodes, object, lastObject, next, fieldname, localIterator);
        if (object == null) {
          correctIterator(localIterator);
          return original;
        }
      }
    }
    return original;
  }
  
  private void correctIterator(final ListIterator<AbstractInsnNode> localIterator) {
    while (iterator.nextIndex() < localIterator.nextIndex()) {
      iterator.next();
    }
  }
  
  private Object handleArray(final Type type, final List<AbstractInsnNode> nodes, final Object object,
      final Object lastObject, final AbstractInsnNode currentNode, final String fieldName,
      final ListIterator<AbstractInsnNode> localIterator) {
    if (!type.getDescriptor().startsWith("[")) {
      return null;
    }
    
    Object localObject = object;
    AbstractInsnNode next = currentNode;
    final boolean isFinal = ClassAccessor.isFinal(lastObject, fieldName);
    final boolean hasAnnotation = ClassAccessor.hasAnnotation(lastObject, fieldName, ImmutableArray.class);
    
    for (int i = 0; i < type.getDimensions(); ++i) {
      if (!localIterator.hasNext()) {
        return null;
      }
      final boolean isArrayLength = next.getOpcode() == ARRAYLENGTH;
      if (isArrayLength) {
        if (!isFinal) {
          return null;
        }
        localObject = Array.getLength(localObject);
      } else {
        
        if (!NodeHelper.isNumberNode(next)) {
          return null;
        }
        final int index = NodeHelper.getNumberValue(next).intValue();
        nodes.add(next);
        if (!localIterator.hasNext()) {
          return null;
        }
        next = localIterator.next();
        final boolean isArrayLoad = next.getOpcode() >= IALOAD && next.getOpcode() <= SALOAD;
        if (isArrayLoad) {
          if (!hasAnnotation) {
            return null;
          }
          localObject = Array.get(localObject, index);
        }
      }
      nodes.add(next);
      if (isArrayLength) {
        break;
      }
      if (i < type.getDimensions() - 1) {
        next = localIterator.next();
      }
    }
    return localObject;
  }
  
  private boolean handleBuiltIn(final String descriptor, final InsnList original, final AbstractInsnNode next,
      final Object object, final List<AbstractInsnNode> nodes, final ListIterator<AbstractInsnNode> localIterator) {
    if (FinalFieldInliner.isBuiltIn(descriptor)) {
      correctIterator(localIterator);
      original.insertBefore(next, NodeHelper.getInsnNodeFor(object));
      for (final AbstractInsnNode node : nodes) {
        original.remove(node);
      }
      iterator.previous();
      return true;
    }
    return false;
  }
  
  @Override
  public void setInputObject(final Object inputObject) {
    input = inputObject;
  }
  
  @Override
  public void setIterator(final ListIterator<AbstractInsnNode> iterator) {
    this.iterator = iterator;
  }
  
}
