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
package de.tuberlin.uebb.jbop.optimizer.controlflow;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Iterator;

import org.apache.commons.math3.exception.NotANumberException;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.MethodNode;

import de.tuberlin.uebb.jbop.access.ClassAccessor;
import de.tuberlin.uebb.jbop.exception.JBOPClassException;
import de.tuberlin.uebb.jbop.optimizer.IInputObjectAware;
import de.tuberlin.uebb.jbop.optimizer.IOptimizer;
import de.tuberlin.uebb.jbop.optimizer.array.FieldArrayValueInliner;
import de.tuberlin.uebb.jbop.optimizer.array.NonNullArrayValue;
import de.tuberlin.uebb.jbop.optimizer.utils.NodeHelper;

/**
 * Evaluates if conditions where possible and choose only
 * the remaining branch.
 * 
 * Possible evaluations are:
 * 
 * Object equality
 * a == null
 * a != null
 * where a is an object ( aconstnull / something else)
 * 
 * arithmetic relations
 * a == b
 * a != b
 * a < b
 * a > b
 * a <= b
 * a >= b
 * 
 * where a and b are numbers (x_const / bipush / sipush / ldc)
 * 
 * <pre>
 * @ImmutableArray
 * double d = {1.0, 2.0, 3.0}
 * ...
 * if(d.length==3){ //after run of {@link de.tuberlin.uebb.jbop.optimizer.array.FieldArrayLengthInliner} this is 3==3
 *   a
 *   b
 *   c
 * } else {
 *   d
 *   e
 *   f
 * }
 * ...
 * </pre>
 * 
 * becomes
 * 
 * <pre>
 *   a
 *   b
 *   c
 * </pre>
 * 
 * in Bytecode this means eg:
 * 
 * <pre>
 * 1  iload_3
 * 2  iload_3
 * 3  if_icmpne 7
 * 4  a
 * 5  b
 * 6  c
 * 7  goto 11
 * 8  d
 * 9  e
 * 10 f
 * 11 ...
 * </pre>
 * 
 * becomes
 * 
 * <pre>
 * a
 * b
 * c
 * </pre>
 * 
 * @author Christopher Ewest
 */
public class ConstantIfInliner implements IOptimizer, IInputObjectAware {
  
  private static final BigDecimal NONNULL = BigDecimal.ZERO;
  private boolean optimized;
  private final FieldArrayValueInliner arrayValue;
  private Object inputObject;
  
  /**
   * Instantiates a new {@link ConstantIfInliner}.
   * 
   * @param arrayValue
   *          the array value
   */
  public ConstantIfInliner(final FieldArrayValueInliner arrayValue) {
    this.arrayValue = arrayValue;
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
      if (NodeHelper.isIf(currentNode)) {
        final AbstractInsnNode node1 = NodeHelper.getPrevious(currentNode);
        if (NodeHelper.isTwoValueIf(currentNode)) {
          final AbstractInsnNode node2 = NodeHelper.getPrevious(node1);
          handle(currentNode, node1, node2, original, iterator);
        } else {
          
          handle(currentNode, node1, null, original, iterator);
        }
      }
    }
    return original;
  }
  
  private void handle(final AbstractInsnNode currentNode, final AbstractInsnNode node1, final AbstractInsnNode node2,
      final InsnList list, final Iterator<AbstractInsnNode> iterator) throws JBOPClassException {
    if (handleNumberInstruction(currentNode, node1, node2, list, iterator)) {
      optimized = true;
      return;
    } else if (handleNullInstruction(currentNode, node1, list, iterator)) {
      optimized = true;
      return;
    }
  }
  
  private boolean handleNullInstruction(final AbstractInsnNode currentNode, final AbstractInsnNode node1,
      final InsnList list, final Iterator<AbstractInsnNode> iterator) throws JBOPClassException {
    if ((currentNode.getOpcode() == Opcodes.IFNULL) || (currentNode.getOpcode() == Opcodes.IFNONNULL)) {
      final boolean eval;
      if (node1.getOpcode() == Opcodes.ACONST_NULL) {
        final AbstractInsnNode node2 = NodeHelper.getPrevious(node1);
        if ((NodeHelper.getFieldname(node1) != null) && (NodeHelper.getVarIndex(node2) == 0)) {
          final Object currentValue = ClassAccessor.getCurrentValue(inputObject, NodeHelper.getFieldname(node1));
          if (currentValue != null) {
            return false;
          }
          removeNodes(currentNode, node1, node2, null, list, iterator, //
              evalSingleOpValue(null, currentNode.getOpcode()));
          return false;
        }
        eval = evalSingleOpValue(null, currentNode.getOpcode());
      } else {
        final AbstractInsnNode node2 = NodeHelper.getPrevious(node1);
        if ((NodeHelper.getFieldname(node1) != null) && (NodeHelper.getVarIndex(node2) == 0)) {
          final Object currentValue = ClassAccessor.getCurrentValue(inputObject, NodeHelper.getFieldname(node1));
          if (currentValue == null) {
            return false;
          }
          removeNodes(currentNode, node1, node2, null, list, iterator,
              evalSingleOpValue(NONNULL, currentNode.getOpcode()));
          return false;
        }
        // doesn't work for multiarrays yet
        final AbstractInsnNode node3 = NodeHelper.getPrevious(node2);
        final AbstractInsnNode node4 = NodeHelper.getPrevious(node3);
        boolean isNonNullArrayValue = false;
        if (arrayValue != null) {
          for (final NonNullArrayValue nonNullarrayValue : arrayValue.getNonNullArrayValues()) {
            if (nonNullarrayValue.is(node4, node3, Arrays.asList(node2), Arrays.asList(node1))) {
              isNonNullArrayValue = true;
              break;
            }
          }
        }
        if (!isNonNullArrayValue) {
          return false;
        }
        if (node2 != null) {
          list.remove(node2);
        }
        if (node3 != null) {
          list.remove(node3);
        }
        if (node4 != null) {
          list.remove(node4);
        }
        eval = evalSingleOpValue(NONNULL, currentNode.getOpcode());
      }
      removeNodes(currentNode, node1, null, null, list, iterator, eval);
      return true;
    }
    return false;
  }
  
  private boolean handleNumberInstruction(final AbstractInsnNode currentNode, final AbstractInsnNode node1,
      final AbstractInsnNode node2, final InsnList list, final Iterator<AbstractInsnNode> iterator) {
    Number op1;
    AbstractInsnNode node3 = node1;
    AbstractInsnNode node4 = node2;
    final boolean eval;
    final Number op2;
    if (isCompare(node1)) {
      node3 = NodeHelper.getPrevious(node1);
      node4 = NodeHelper.getPrevious(node3);
    }
    AbstractInsnNode node5 = node3;
    
    try {
      if (NodeHelper.isCast(node3)) {
        node5 = node4;
        node4 = NodeHelper.getPrevious(node4);
      }
      op1 = NodeHelper.getNumberValue(node5);
    } catch (final NotANumberException nan) {
      return false;
    }
    AbstractInsnNode node6 = node4;
    if (node6 != null) {
      try {
        if (NodeHelper.isCast(node4)) {
          node6 = NodeHelper.getPrevious(node4);
        }
        op2 = NodeHelper.getNumberValue(node6);
      } catch (final NotANumberException nan) {
        return false;
      }
    } else {
      op2 = Double.valueOf(Double.NaN);
    }
    eval = evaluate(currentNode, node1, op1, op2);
    removeNodes(currentNode, node1, node3, node4, list, iterator, eval);
    if ((node5 != null) && (node5 != node3)) {
      list.remove(node5);
    }
    if ((node6 != null) && (node6 != node4)) {
      list.remove(node6);
    }
    return true;
  }
  
  private boolean evaluate(final AbstractInsnNode currentNode, final AbstractInsnNode node1, final Number op1,
      final Number op2) {
    final boolean eval;
    if (NodeHelper.isTwoValueIf(currentNode)) {
      eval = evalTwoOpValue(op2, op1, currentNode.getOpcode());
    } else {
      final Number operator = calculateOparator(node1, op1, op2);
      eval = evalSingleOpValue(operator, currentNode.getOpcode());
    }
    return eval;
  }
  
  private Number calculateOparator(final AbstractInsnNode node1, final Number op1, final Number op2) {
    Number newNumber = op1;
    if (isCompare(node1)) {
      switch (node1.getOpcode()) {
        case Opcodes.DCMPG:
        case Opcodes.DCMPL:
          newNumber = Double.valueOf(op2.doubleValue() - op1.doubleValue());
          break;
        case Opcodes.FCMPG:
        case Opcodes.FCMPL:
          newNumber = Float.valueOf(op2.floatValue() - op1.floatValue());
          break;
        case Opcodes.LCMP:
          newNumber = Long.valueOf(op2.longValue() - op1.longValue());
          break;
        default:
          newNumber = op1;
      }
    }
    return newNumber;
  }
  
  private boolean isCompare(final AbstractInsnNode node1) {
    final int opcode = node1.getOpcode();
    if (opcode == Opcodes.DCMPG) {
      return true;
    }
    if (opcode == Opcodes.DCMPL) {
      return true;
    }
    if (opcode == Opcodes.FCMPG) {
      return true;
    }
    if (opcode == Opcodes.FCMPL) {
      return true;
    }
    if (opcode == Opcodes.LCMP) {
      return true;
    }
    return false;
  }
  
  private void removeNodes(final AbstractInsnNode currentNode, final AbstractInsnNode node1,
      final AbstractInsnNode node3, final AbstractInsnNode node4, final InsnList list,
      final Iterator<AbstractInsnNode> iterator, //
      final boolean eval) {
    list.remove(node1);
    if ((node3 != null) && (node3 != node1)) {
      list.remove(node3);
    }
    if ((node4 != null) && (node4 != node1) && (node4 != node3)) {
      list.remove(node4);
    }
    AbstractInsnNode label = ((JumpInsnNode) currentNode).label;
    list.remove(currentNode);
    if (!eval) {
      final AbstractInsnNode previousOfLabel = NodeHelper.getPrevious(label);
      if (previousOfLabel.getOpcode() == Opcodes.GOTO) {
        while (iterator.hasNext()) {
          final AbstractInsnNode node = iterator.next();
          if (node == label) {
            break;
          }
        }
        label = ((JumpInsnNode) previousOfLabel).label;
        list.remove(previousOfLabel);
      } else {
        return;
      }
    }
    
    while (iterator.hasNext()) {
      final AbstractInsnNode node = iterator.next();
      list.remove(node);
      if (node == label) {
        break;
      }
    }
    
  }
  
  private boolean evalSingleOpValue(final Number op1, final int opcode) {
    switch (opcode) {
      case Opcodes.IFEQ:
        return op1.intValue() == 0;
      case Opcodes.IFNE:
        return op1.intValue() != 0;
      case Opcodes.IFLT:
        return op1.intValue() < 0;
      case Opcodes.IFGE:
        return op1.intValue() >= 0;
      case Opcodes.IFGT:
        return op1.intValue() > 0;
      case Opcodes.IFLE:
        return op1.intValue() <= 0;
      case Opcodes.IFNULL:
        return op1 == null;
      case Opcodes.IFNONNULL:
        return op1 != null;
      default:
        return false;
    }
  }
  
  private boolean evalTwoOpValue(final Number op1, final Number op2, final int opcode) {
    switch (opcode) {
      case Opcodes.IF_ICMPEQ:
        return op1.intValue() == op2.intValue();
      case Opcodes.IF_ICMPNE:
        return op1.intValue() != op2.intValue();
      case Opcodes.IF_ICMPLT:
        return op1.intValue() < op2.intValue();
      case Opcodes.IF_ICMPGE:
        return op1.intValue() >= op2.intValue();
      case Opcodes.IF_ICMPGT:
        return op1.intValue() > op2.intValue();
      case Opcodes.IF_ICMPLE:
        return op1.intValue() <= op2.intValue();
      case Opcodes.IF_ACMPEQ:
        return op1 == op2;
      case Opcodes.IF_ACMPNE:
        return op1 != op2;
      default:
        return false;
    }
  }
  
  @Override
  public void setInputObject(final Object inputObject) {
    this.inputObject = inputObject;
  }
}
