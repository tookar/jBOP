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
package de.tuberlin.uebb.jbop.optimizer.arithmetic;

import static org.objectweb.asm.Opcodes.DADD;
import static org.objectweb.asm.Opcodes.DDIV;
import static org.objectweb.asm.Opcodes.DMUL;
import static org.objectweb.asm.Opcodes.DSUB;
import static org.objectweb.asm.Opcodes.FADD;
import static org.objectweb.asm.Opcodes.FDIV;
import static org.objectweb.asm.Opcodes.FMUL;
import static org.objectweb.asm.Opcodes.FSUB;
import static org.objectweb.asm.Opcodes.IADD;
import static org.objectweb.asm.Opcodes.IDIV;
import static org.objectweb.asm.Opcodes.IMUL;
import static org.objectweb.asm.Opcodes.ISUB;
import static org.objectweb.asm.Opcodes.LADD;
import static org.objectweb.asm.Opcodes.LDIV;
import static org.objectweb.asm.Opcodes.LMUL;
import static org.objectweb.asm.Opcodes.LSUB;

import java.util.Iterator;

import org.apache.commons.math3.exception.NotANumberException;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;

import de.tuberlin.uebb.jbop.optimizer.IOptimizer;
import de.tuberlin.uebb.jbop.optimizer.utils.NodeHelper;

/**
 * The Class ArithmeticExpressionInterpreter.
 * 
 * This Optimizer can handle simple arithmetic expressions at bytecode level.
 * 
 * eq:
 * 
 * <pre>
 * bipush 7
 * bipush 8
 * add
 * </pre>
 * 
 * is replaced by
 * 
 * <pre>
 * bipush 15
 * </pre>
 * 
 * @author Christopher Ewest
 */
public class ArithmeticExpressionInterpreter implements IOptimizer {
  
  private boolean optimized = false;
  
  @Override
  public boolean isOptimized() {
    return optimized;
  }
  
  @Override
  public InsnList optimize(final InsnList original, final MethodNode methodNode) {
    optimized = false;
    final Iterator<AbstractInsnNode> iterator = original.iterator();
    while (iterator.hasNext()) {
      final AbstractInsnNode currentNode = iterator.next();
      Number one, two;
      AbstractInsnNode numberNode;
      AbstractInsnNode castNode1 = null;
      AbstractInsnNode castNode2 = null;
      try {
        one = NodeHelper.getNumberValue(currentNode);
        numberNode = NodeHelper.getNext(currentNode);
        if (NodeHelper.isCast(numberNode)) {
          castNode1 = numberNode;
          numberNode = NodeHelper.getNext(castNode1);
        }
        two = NodeHelper.getNumberValue(numberNode);
      } catch (final NotANumberException nane) {
        continue;
      }
      AbstractInsnNode op = NodeHelper.getNext(numberNode);
      if (NodeHelper.isCast(op)) {
        castNode2 = op;
        op = NodeHelper.getNext(castNode2);
      }
      if (isArithmeticOp(op)) {
        final AbstractInsnNode replacement = getReplacement(one, two, op);
        original.insert(op, replacement);
        clean(original, iterator, currentNode, numberNode, castNode1, castNode2, op);
        optimized = true;
      }
    }
    return original;
  }
  
  private void clean(final InsnList original, final Iterator<AbstractInsnNode> iterator,
      final AbstractInsnNode currentNode, final AbstractInsnNode numberNode, final AbstractInsnNode castNode1,
      final AbstractInsnNode castNode2, final AbstractInsnNode op) {
    if (castNode1 != null) {
      iterator.next();// --> castNode1
    }
    iterator.next();// --> numberNode
    if (castNode2 != null) {
      iterator.next();// --> castNode2
    }
    iterator.next();// --> op
    original.remove(currentNode);
    if (castNode1 != null) {
      original.remove(castNode1);
    }
    original.remove(numberNode);
    if (castNode2 != null) {
      original.remove(castNode2);
    }
    original.remove(op);
  }
  
  private AbstractInsnNode getReplacement(final Number one, final Number two, final AbstractInsnNode op) {
    final int opcode = op.getOpcode();
    if ((opcode >= IADD) && (opcode <= DADD)) {
      return handleAdd(opcode, one, two);
    }
    if ((opcode >= ISUB) && (opcode <= DSUB)) {
      return handleSub(opcode, one, two);
    }
    if ((opcode >= IMUL) && (opcode <= DMUL)) {
      return handleMul(opcode, one, two);
    }
    if ((opcode >= IDIV) && (opcode <= DDIV)) {
      return handleDiv(opcode, one, two);
    }
    return null;
  }
  
  private AbstractInsnNode handleDiv(final int opcode, final Number one, final Number two) {
    final Number number;
    switch (opcode) {
      case (IDIV):
        number = Integer.valueOf(one.intValue() / two.intValue());
        break;
      case (DDIV):
        number = Double.valueOf(one.doubleValue() / two.doubleValue());
        break;
      case (FDIV):
        number = Float.valueOf(one.floatValue() / two.floatValue());
        break;
      case (LDIV):
        number = Long.valueOf(one.longValue() / two.longValue());
        break;
      default:
        return null;
    }
    return NodeHelper.getInsnNodeFor(number);
  }
  
  private AbstractInsnNode handleMul(final int opcode, final Number one, final Number two) {
    final Number number;
    switch (opcode) {
      case (IMUL):
        number = Integer.valueOf(one.intValue() * two.intValue());
        break;
      case (DMUL):
        number = Double.valueOf(one.doubleValue() * two.doubleValue());
        break;
      case (FMUL):
        number = Float.valueOf(one.floatValue() * two.floatValue());
        break;
      case (LMUL):
        number = Long.valueOf(one.longValue() * two.longValue());
        break;
      default:
        return null;
    }
    return NodeHelper.getInsnNodeFor(number);
  }
  
  private AbstractInsnNode handleSub(final int opcode, final Number one, final Number two) {
    final Number number;
    switch (opcode) {
      case (ISUB):
        number = Integer.valueOf(one.intValue() - two.intValue());
        break;
      case (DSUB):
        number = Double.valueOf(one.doubleValue() - two.doubleValue());
        break;
      case (FSUB):
        number = Float.valueOf(one.floatValue() - two.floatValue());
        break;
      case (LSUB):
        number = Long.valueOf(one.longValue() - two.longValue());
        break;
      default:
        return null;
    }
    return NodeHelper.getInsnNodeFor(number);
  }
  
  private AbstractInsnNode handleAdd(final int opcode, final Number one, final Number two) {
    final Number number;
    switch (opcode) {
      case (IADD):
        number = Integer.valueOf(one.intValue() + two.intValue());
        break;
      case (DADD):
        number = Double.valueOf(one.doubleValue() + two.doubleValue());
        break;
      case (FADD):
        number = Float.valueOf(one.floatValue() + two.floatValue());
        break;
      case (LADD):
        number = Long.valueOf(one.longValue() + two.longValue());
        break;
      default:
        return null;
    }
    return NodeHelper.getInsnNodeFor(number);
  }
  
  private boolean isArithmeticOp(final AbstractInsnNode op) {
    final int opcode = op.getOpcode();
    if (isDoubleArithmetic(opcode)) {
      return true;
    } else if (isIntArithmetic(opcode)) {
      return true;
    } else if (isFloatArithmetic(opcode)) {
      return true;
    } else if (isLongArithmetic(opcode)) {
      return true;
    }
    return false;
  }
  
  private boolean isDoubleArithmetic(final int opcode) {
    if (opcode == Opcodes.DADD) {
      return true;
    } else if (opcode == Opcodes.DMUL) {
      return true;
    } else if (opcode == Opcodes.DSUB) {
      return true;
    } else if (opcode == Opcodes.DDIV) {
      return true;
    }
    return false;
  }
  
  private boolean isIntArithmetic(final int opcode) {
    if (opcode == Opcodes.IADD) {
      return true;
    } else if (opcode == Opcodes.IMUL) {
      return true;
    } else if (opcode == Opcodes.ISUB) {
      return true;
    } else if (opcode == Opcodes.IDIV) {
      return true;
    }
    return false;
  }
  
  private boolean isFloatArithmetic(final int opcode) {
    if (opcode == Opcodes.FADD) {
      return true;
    } else if (opcode == Opcodes.FMUL) {
      return true;
    } else if (opcode == Opcodes.FSUB) {
      return true;
    } else if (opcode == Opcodes.FDIV) {
      return true;
    }
    return false;
  }
  
  private boolean isLongArithmetic(final int opcode) {
    if (opcode == Opcodes.LADD) {
      return true;
    } else if (opcode == Opcodes.LMUL) {
      return true;
    } else if (opcode == Opcodes.LSUB) {
      return true;
    } else if (opcode == Opcodes.LDIV) {
      return true;
    }
    return false;
  }
}
