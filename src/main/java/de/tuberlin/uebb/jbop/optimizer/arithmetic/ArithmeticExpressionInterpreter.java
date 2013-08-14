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
        optimized = true;
      }
    }
    return original;
  }
  
  private AbstractInsnNode getReplacement(final Number one, final Number two, final AbstractInsnNode op) {
    final int opcode = op.getOpcode();
    AbstractInsnNode replacement = null;
    if (opcode == Opcodes.DADD) {
      replacement = NodeHelper.getInsnNodeFor(one.doubleValue() + two.doubleValue());
    } else if (opcode == Opcodes.DMUL) {
      replacement = NodeHelper.getInsnNodeFor(one.doubleValue() * two.doubleValue());
    } else if (opcode == Opcodes.DSUB) {
      replacement = NodeHelper.getInsnNodeFor(one.doubleValue() - two.doubleValue());
    } else if (opcode == Opcodes.DDIV) {
      replacement = NodeHelper.getInsnNodeFor(one.doubleValue() / two.doubleValue());
    } else if (opcode == Opcodes.IADD) {
      replacement = NodeHelper.getInsnNodeFor(one.intValue() + two.intValue());
    } else if (opcode == Opcodes.IMUL) {
      replacement = NodeHelper.getInsnNodeFor(one.intValue() * two.intValue());
    } else if (opcode == Opcodes.ISUB) {
      replacement = NodeHelper.getInsnNodeFor(one.intValue() - two.intValue());
    } else if (opcode == Opcodes.IDIV) {
      replacement = NodeHelper.getInsnNodeFor(one.intValue() / two.intValue());
    } else if (opcode == Opcodes.FADD) {
      replacement = NodeHelper.getInsnNodeFor(one.floatValue() + two.floatValue());
    } else if (opcode == Opcodes.FMUL) {
      replacement = NodeHelper.getInsnNodeFor(one.floatValue() * two.floatValue());
    } else if (opcode == Opcodes.FSUB) {
      replacement = NodeHelper.getInsnNodeFor(one.floatValue() - two.floatValue());
    } else if (opcode == Opcodes.FDIV) {
      replacement = NodeHelper.getInsnNodeFor(one.floatValue() / two.floatValue());
    } else if (opcode == Opcodes.LADD) {
      replacement = NodeHelper.getInsnNodeFor(one.longValue() + two.longValue());
    } else if (opcode == Opcodes.LMUL) {
      replacement = NodeHelper.getInsnNodeFor(one.longValue() * two.longValue());
    } else if (opcode == Opcodes.LSUB) {
      replacement = NodeHelper.getInsnNodeFor(one.longValue() - two.longValue());
    } else if (opcode == Opcodes.LDIV) {
      replacement = NodeHelper.getInsnNodeFor(one.longValue() / two.longValue());
    }
    return replacement;
  }
  
  private boolean isArithmeticOp(final AbstractInsnNode op) {
    final int opcode = op.getOpcode();
    if (opcode == Opcodes.DADD) {
      return true;
    } else if (opcode == Opcodes.DMUL) {
      return true;
    } else if (opcode == Opcodes.DSUB) {
      return true;
    } else if (opcode == Opcodes.DDIV) {
      return true;
    } else if (opcode == Opcodes.IADD) {
      return true;
    } else if (opcode == Opcodes.IMUL) {
      return true;
    } else if (opcode == Opcodes.ISUB) {
      return true;
    } else if (opcode == Opcodes.IDIV) {
      return true;
    } else if (opcode == Opcodes.FADD) {
      return true;
    } else if (opcode == Opcodes.FMUL) {
      return true;
    } else if (opcode == Opcodes.FDIV) {
      return true;
    } else if (opcode == Opcodes.FSUB) {
      return true;
    } else if (opcode == Opcodes.LADD) {
      return true;
    } else if (opcode == Opcodes.LMUL) {
      return true;
    } else if (opcode == Opcodes.LDIV) {
      return true;
    } else if (opcode == Opcodes.LSUB) {
      return true;
    } else {
      return false;
    }
  }
  
}
