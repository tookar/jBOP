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

import static org.objectweb.asm.Opcodes.AALOAD;
import static org.objectweb.asm.Opcodes.ACONST_NULL;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ASM5;
import static org.objectweb.asm.Opcodes.ASTORE;
import static org.objectweb.asm.Opcodes.D2F;
import static org.objectweb.asm.Opcodes.D2I;
import static org.objectweb.asm.Opcodes.D2L;
import static org.objectweb.asm.Opcodes.DREM;
import static org.objectweb.asm.Opcodes.F2D;
import static org.objectweb.asm.Opcodes.F2I;
import static org.objectweb.asm.Opcodes.F2L;
import static org.objectweb.asm.Opcodes.I2B;
import static org.objectweb.asm.Opcodes.I2D;
import static org.objectweb.asm.Opcodes.I2F;
import static org.objectweb.asm.Opcodes.I2L;
import static org.objectweb.asm.Opcodes.I2S;
import static org.objectweb.asm.Opcodes.IADD;
import static org.objectweb.asm.Opcodes.IALOAD;
import static org.objectweb.asm.Opcodes.ILOAD;
import static org.objectweb.asm.Opcodes.INVOKEDYNAMIC;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.ISTORE;
import static org.objectweb.asm.Opcodes.L2D;
import static org.objectweb.asm.Opcodes.L2F;
import static org.objectweb.asm.Opcodes.L2I;
import static org.objectweb.asm.Opcodes.LDC;

import java.io.IOException;
import java.io.PrintStream;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.exception.NotANumberException;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.util.Printer;
import org.objectweb.asm.util.TraceMethodVisitor;

/**
 * The Class NodeHelper.
 * 
 * This is a staging area for everything about {@link AbstractInsnNode}s.
 * 
 * @author Christopher Ewest
 */
public final class NodeHelper {
  
  private static final int CONST_M1 = -1;
  private static final int CONST_0 = 0;
  private static final int CONST_1 = 1;
  private static final int CONST_2 = 2;
  private static final int CONST_3 = 3;
  private static final int CONST_4 = 4;
  private static final int CONST_5 = 5;
  private static final int CONST_HIGH_BYTE = 127;
  private static final int CONST_LOW_BYTE = -128;
  private static final int CONST_HIGH_INT = 32767;
  private static final int CONST_LOW_INT = -32769;
  
  /**
   * Gets the insn node for 'object'.
   * 
   * @param object
   *          the object
   * @return the insn node for
   */
  public static AbstractInsnNode getInsnNodeFor(final Object object) {
    if (object == null) {
      return new InsnNode(ACONST_NULL);
    }
    if (object instanceof Number) {
      return getInsnNodeFor((Number) object);
    }
    if (object instanceof String) {
      return new LdcInsnNode(object);
    }
    if (object instanceof Character) {
      return getInsnNodeFor((int) ((Character) object).charValue());
    }
    if (object instanceof Boolean) {
      if (((Boolean) object).booleanValue()) {
        return getInsnNodeFor(1);
      }
      return getInsnNodeFor(0);
    }
    return null;
  }
  
  /**
   * Gets the insn node for the number.
   * 
   * @param newNumber
   *          the new number
   * @return the insn node for
   */
  public static AbstractInsnNode getInsnNodeFor(final Number newNumber) {
    if (isIntType(newNumber)) {
      return getIntInsnNode(newNumber);
    } else if (newNumber instanceof Long) {
      return getLongInsnNode(newNumber);
    } else if (newNumber instanceof Float) {
      return getFloatInsnNode(newNumber);
    } else if (newNumber instanceof Double) {
      return getDoubleInsnNode(newNumber);
    }
    return null;
  }
  
  private static AbstractInsnNode getDoubleInsnNode(final Number newNumber) {
    if (newNumber.doubleValue() == 0) {
      return new InsnNode(Opcodes.DCONST_0);
    } else if (newNumber.doubleValue() == 1) {
      return new InsnNode(Opcodes.DCONST_1);
    } else {
      return new LdcInsnNode(newNumber);
    }
  }
  
  private static AbstractInsnNode getFloatInsnNode(final Number newNumber) {
    if (newNumber.longValue() == 0) {
      return new InsnNode(Opcodes.FCONST_0);
    } else if (newNumber.floatValue() == 1) {
      return new InsnNode(Opcodes.FCONST_1);
    } else if (newNumber.floatValue() == 2) {
      return new InsnNode(Opcodes.FCONST_2);
    } else {
      return new LdcInsnNode(newNumber);
    }
  }
  
  private static AbstractInsnNode getLongInsnNode(final Number newNumber) {
    if (newNumber.longValue() == 0) {
      return new InsnNode(Opcodes.LCONST_0);
    } else if (newNumber.longValue() == 1) {
      return new InsnNode(Opcodes.LCONST_1);
    } else {
      return new LdcInsnNode(newNumber);
    }
  }
  
  private static AbstractInsnNode getIntInsnNode(final Number newNumber) {
    switch (newNumber.intValue()) {
      case CONST_M1:
        return new InsnNode(Opcodes.ICONST_M1);
      case CONST_0:
        return new InsnNode(Opcodes.ICONST_0);
      case CONST_1:
        return new InsnNode(Opcodes.ICONST_1);
      case CONST_2:
        return new InsnNode(Opcodes.ICONST_2);
      case CONST_3:
        return new InsnNode(Opcodes.ICONST_3);
      case CONST_4:
        return new InsnNode(Opcodes.ICONST_4);
      case CONST_5:
        return new InsnNode(Opcodes.ICONST_5);
      default:
        if (newNumber.intValue() >= CONST_LOW_INT && newNumber.intValue() <= CONST_HIGH_INT) {
          return new IntInsnNode(getopcodePush(newNumber.intValue()), newNumber.intValue());
        }
        return new LdcInsnNode(newNumber);
    }
  }
  
  private static boolean isIntType(final Number newNumber) {
    if (newNumber instanceof Integer) {
      return true;
    }
    if (newNumber instanceof Short) {
      return true;
    }
    if (newNumber instanceof Byte) {
      return true;
    }
    return false;
  }
  
  /**
   * Gets the correct push opcode for the number.
   * 
   * @param newNumber
   *          the new number
   * @return the opcode push
   */
  public static int getopcodePush(final int newNumber) {
    if (newNumber >= CONST_LOW_BYTE && newNumber <= CONST_HIGH_BYTE) {
      return Opcodes.BIPUSH;
    }
    return Opcodes.SIPUSH;
  }
  
  /**
   * Gets the next node skipping {@link LineNumberNode}s and {@link LabelNode}s.
   * 
   * @param node
   *          the node
   * @return the next node
   */
  public static AbstractInsnNode getNext(final AbstractInsnNode node) {
    if (node == null) {
      return null;
    }
    AbstractInsnNode current = node;
    do {
      current = current.getNext();
      if (current != null && !(current instanceof LineNumberNode || current instanceof LabelNode)) {
        break;
      }
    } while (current != null);
    return current;
  }
  
  /**
   * Gets the previous node, skipping {@link LineNumberNode}s and {@link LabelNode}s.
   * 
   * @param node
   *          the node
   * @return the previous node
   */
  public static AbstractInsnNode getPrevious(final AbstractInsnNode node) {
    if (node == null) {
      return null;
    }
    AbstractInsnNode current = node;
    do {
      current = current.getPrevious();
      if (current != null && !(current instanceof LineNumberNode || current instanceof LabelNode)) {
        break;
      }
    } while (current != null);
    return current;
  }
  
  /**
   * Checks if node is aload0 (this).
   * 
   * @param node
   *          the node
   * @return true if node is aload0
   */
  public static boolean isAload0(final AbstractInsnNode node) {
    if (node == null) {
      return false;
    }
    if (node instanceof VarInsnNode) {
      return ((VarInsnNode) node).var == 0;
    }
    return false;
  }
  
  /**
   * Checks if node is aload.
   * 
   * @param node
   *          the node
   * @return true if node is aload
   */
  public static boolean isAload(final AbstractInsnNode node) {
    if (node == null) {
      return false;
    }
    return node.getOpcode() == Opcodes.ALOAD;
    
  }
  
  /**
   * Checks if node is getField.
   * 
   * @param node
   *          the node
   * @return true if node is a getField
   */
  public static boolean isGetField(final AbstractInsnNode node) {
    if (node == null) {
      return false;
    }
    return node.getOpcode() == Opcodes.GETFIELD;
  }
  
  /**
   * Gets the fieldname.
   * 
   * @param node
   *          the node
   * @return the fieldname or null if node is not a FieldInsnNode
   */
  public static String getFieldname(final AbstractInsnNode node) {
    if (node instanceof FieldInsnNode) {
      return ((FieldInsnNode) node).name;
    }
    return null;
  }
  
  /**
   * Checks if node is array length.
   * 
   * @param node
   *          the node
   * @return true if node is array length
   */
  public static boolean isArrayLength(final AbstractInsnNode node) {
    if (node == null) {
      return false;
    }
    return node.getOpcode() == Opcodes.ARRAYLENGTH;
  }
  
  /**
   * Instantiates a new node helper.
   */
  private NodeHelper() {
    // no instances
  }
  
  /**
   * Checks if node is iconst.
   * 
   * @param node
   *          the node
   * @return true, if is iconst
   */
  public static boolean isIconst(final AbstractInsnNode node) {
    if (node == null) {
      return false;
    }
    return node.getOpcode() >= Opcodes.ICONST_M1 && node.getOpcode() <= Opcodes.ICONST_5;
  }
  
  /**
   * Checks if node is iconst_i.
   * 
   * @param node
   *          the node
   * @param i
   *          the i
   * @return true, if is iconst_i
   */
  public static boolean isIconst(final AbstractInsnNode node, final int i) {
    if (node == null) {
      return false;
    }
    if (node instanceof InsnNode) {
      return node.getOpcode() == Opcodes.ICONST_0 + i;
    }
    return false;
  }
  
  /**
   * Checks if node is iconst0.
   * 
   * @param node
   *          the node
   * @return true if node is iconst0
   */
  public static boolean isIconst0(final AbstractInsnNode node) {
    return isIconst(node, 0);
  }
  
  /**
   * Checks if node is iconst1.
   * 
   * @param node
   *          the node
   * @return true if node is iconst1
   */
  public static boolean isIconst1(final AbstractInsnNode node) {
    return isIconst(node, 1);
  }
  
  /**
   * Checks if node is iconst2.
   * 
   * @param node
   *          the node
   * @return true if node is iconst2
   */
  public static boolean isIconst2(final AbstractInsnNode node) {
    return isIconst(node, 2);
  }
  
  /**
   * Checks if node is iconst3.
   * 
   * @param node
   *          the node
   * @return true if node is iconst3
   */
  public static boolean isIconst3(final AbstractInsnNode node) {
    return isIconst(node, CONST_3);
  }
  
  /**
   * Checks if node is iconst4.
   * 
   * @param node
   *          the node
   * @return true if node is iconst4
   */
  public static boolean isIconst4(final AbstractInsnNode node) {
    return isIconst(node, CONST_4);
  }
  
  /**
   * Checks if node is iconst5.
   * 
   * @param node
   *          the node
   * @return true if node is iconst5
   */
  public static boolean isIconst5(final AbstractInsnNode node) {
    return isIconst(node, CONST_5);
  }
  
  /**
   * Checks if node is istor_i.
   * 
   * @param node
   *          the node
   * @param i
   *          the i
   * @return true if node is istor_i (-1 for any I_STORE)
   */
  public static boolean isIstor(final AbstractInsnNode node, final int i) {
    if (node == null) {
      return false;
    }
    if (node instanceof VarInsnNode) {
      final boolean isIstore = ((VarInsnNode) node).getOpcode() == Opcodes.ISTORE;
      final boolean isRightNumber = ((VarInsnNode) node).var == i || i == CONST_M1;
      return isIstore && isRightNumber;
    }
    return false;
  }
  
  /**
   * Checks if node is istor0.
   * 
   * @param node
   *          the node
   * @return true if node is istor0
   */
  public static boolean isIstor0(final AbstractInsnNode node) {
    return isIstor(node, CONST_0);
  }
  
  /**
   * Checks if node is istor1.
   * 
   * @param node
   *          the node
   * @return true if node is istor1
   */
  public static boolean isIstor1(final AbstractInsnNode node) {
    return isIstor(node, CONST_1);
  }
  
  /**
   * Checks if node is istor2.
   * 
   * @param node
   *          the node
   * @return true if node is istor2
   */
  public static boolean isIstor2(final AbstractInsnNode node) {
    return isIstor(node, CONST_2);
  }
  
  /**
   * Checks if node is istor3.
   * 
   * @param node
   *          the node
   * @return true if node is istor3
   */
  public static boolean isIstor3(final AbstractInsnNode node) {
    return isIstor(node, CONST_3);
  }
  
  /**
   * Checks if node is goto.
   * 
   * @param node
   *          the node
   * @return true if node1 is goto
   */
  public static boolean isGoto(final AbstractInsnNode node) {
    if (node == null) {
      return false;
    }
    if (node instanceof JumpInsnNode) {
      return node.getOpcode() == Opcodes.GOTO;
    }
    
    return false;
  }
  
  /**
   * Checks if node is iload_i.
   * 
   * @param node
   *          the node
   * @param i
   *          the i
   * @return true, if is iload_i (-1 for any I_LOAD)
   */
  public static boolean isIload(final AbstractInsnNode node, final int i) {
    if (node == null) {
      return false;
    }
    if (node instanceof VarInsnNode) {
      final boolean isIload = node.getOpcode() == Opcodes.ILOAD;
      return isIload && ((VarInsnNode) node).var == i || i == CONST_M1;
    }
    return false;
  }
  
  /**
   * Checks if node is iload0.
   * 
   * @param node
   *          the node
   * @return true if node is iload0
   */
  public static boolean isIload0(final AbstractInsnNode node) {
    return isIload(node, CONST_0);
  }
  
  /**
   * Checks if node is iload1.
   * 
   * @param node
   *          the node
   * @return true if node is iload1
   */
  public static boolean isIload1(final AbstractInsnNode node) {
    return isIload(node, CONST_1);
  }
  
  /**
   * Checks if node is iload2.
   * 
   * @param node
   *          the node
   * @return true if node is iload2
   */
  public static boolean isIload2(final AbstractInsnNode node) {
    return isIload(node, CONST_2);
  }
  
  /**
   * Checks if node is iload3.
   * 
   * @param node
   *          the node
   * @return true if node is iload3
   */
  public static boolean isIload3(final AbstractInsnNode node) {
    return isIload(node, CONST_3);
  }
  
  /**
   * Checks if node is bipush.
   * 
   * @param node
   *          the node
   * @return true if node is bipush
   */
  public static boolean isBipush(final AbstractInsnNode node) {
    if (node == null) {
      return false;
    }
    return node.getOpcode() == Opcodes.BIPUSH;
  }
  
  /**
   * Checks if node is if cmp lt.
   * 
   * @param node
   *          the node
   * @return true if node is if cmp lt
   */
  public static boolean isIfCmpLt(final AbstractInsnNode node) {
    if (node == null) {
      return false;
    }
    return node.getOpcode() == Opcodes.IF_ICMPLT;
  }
  
  /**
   * Checks if node is daload.
   * 
   * @param node
   *          the node
   * @return true if node is daload
   */
  public static boolean isDAload(final AbstractInsnNode node) {
    if (node == null) {
      return false;
    }
    return node.getOpcode() == Opcodes.DALOAD;
  }
  
  /**
   * Checks if node is aaload.
   * 
   * @param node
   *          the node
   * @return true if node is a aload
   */
  public static boolean isAAload(final AbstractInsnNode node) {
    if (node == null) {
      return false;
    }
    return node.getOpcode() == Opcodes.AALOAD;
  }
  
  /**
   * Checks if node is a value node.
   * 
   * @param node
   *          the node
   * @return true if node is value
   */
  public static boolean isValue(final AbstractInsnNode node) {
    if (node == null) {
      return false;
    }
    AbstractInsnNode checkNode = node;
    if (isCast(node)) {
      checkNode = node.getPrevious();
    }
    try {
      NodeHelper.getValue(checkNode);
      return true;
    } catch (final NotANumberException nane) {
      return false;
    }
  }
  
  /**
   * Gets the value of the Node, if this is a value node.
   * 
   * @param valueNode
   *          the value node
   * @return the value
   * @throws NotANumberException
   *           the not a number exception
   */
  public static Object getValue(final AbstractInsnNode valueNode) throws NotANumberException {
    try {
      return getNumberValue(valueNode);
    } catch (final NotANumberException nane) {
      return getStringValue(valueNode);
    }
  }
  
  public static Number cast(final Number number, final AbstractInsnNode numberNode, final AbstractInsnNode castNode) {
    if (castNode == null) {
      return null;
    }
    if (castNode == numberNode) {
      return number;
    }
    final int opcode = castNode.getOpcode();
    if (opcode == I2B) {
      return (byte) number.intValue();
    }
    // if(opcode==I2C) {
    // return (char)number.intValue();
    // }
    if (opcode == I2L) {
      return (long) number.intValue();
    }
    if (opcode == I2S) {
      return (short) number.intValue();
    }
    if (opcode == I2D) {
      return (double) number.intValue();
    }
    if (opcode == I2F) {
      return (float) number.intValue();
    }
    if (opcode == D2F) {
      return (float) number.doubleValue();
    }
    if (opcode == D2I) {
      return (int) number.doubleValue();
    }
    if (opcode == D2L) {
      return (long) number.doubleValue();
    }
    if (opcode == F2D) {
      return (double) number.floatValue();
    }
    if (opcode == F2I) {
      return (int) number.floatValue();
    }
    if (opcode == F2L) {
      return (long) number.floatValue();
    }
    if (opcode == L2D) {
      return (long) number.longValue();
    }
    if (opcode == L2F) {
      return (float) number.longValue();
    }
    if (opcode == L2I) {
      return (int) number.longValue();
    }
    
    return number;
  }
  
  /**
   * Gets the value.
   * 
   * @param node
   *          the node
   * @return the value
   * @throws NotANumberException
   *           if node is not a number-Node
   */
  public static Number getNumberValue(final AbstractInsnNode node) throws NotANumberException {
    if (node == null) {
      throw new NotANumberException();
    }
    AbstractInsnNode checkNode = node;
    if (isCast(node)) {
      checkNode = node.getPrevious();
    }
    if (isIconst(checkNode)) {
      return cast(Integer.valueOf(checkNode.getOpcode() - Opcodes.ICONST_0), checkNode, node);
    }
    if (checkNode.getOpcode() == Opcodes.LCONST_0) {
      return cast(Long.valueOf(0), checkNode, node);
    }
    if (checkNode.getOpcode() == Opcodes.LCONST_1) {
      return cast(Long.valueOf(1), checkNode, node);
    }
    if (checkNode.getOpcode() == Opcodes.FCONST_0) {
      return cast(Float.valueOf(0), checkNode, node);
    }
    if (checkNode.getOpcode() == Opcodes.FCONST_1) {
      return cast(Float.valueOf(1), checkNode, node);
    }
    if (checkNode.getOpcode() == Opcodes.FCONST_2) {
      return cast(Float.valueOf(1), checkNode, node);
    }
    if (checkNode.getOpcode() == Opcodes.DCONST_0) {
      return cast(Double.valueOf(0), checkNode, node);
    }
    if (checkNode.getOpcode() == Opcodes.DCONST_1) {
      return cast(Double.valueOf(1), checkNode, node);
    }
    if (checkNode instanceof IntInsnNode) {
      return cast(Integer.valueOf(((IntInsnNode) checkNode).operand), checkNode, node);
    }
    return cast(getConstantValue(checkNode, Number.class), checkNode, node);
  }
  
  /**
   * Gets the string value.
   * 
   * @param node
   *          the node
   * @return the string value
   * @throws NotANumberException
   *           the not a number exception if node is null or not a string constant
   */
  public static String getStringValue(final AbstractInsnNode node) throws NotANumberException {
    return getConstantValue(node, String.class);
  }
  
  private static <T> T getConstantValue(final AbstractInsnNode node, final Class<T> clazz) throws NotANumberException {
    if (node == null) {
      throw new NotANumberException();
    }
    if (node.getOpcode() == ACONST_NULL) {
      return null;
    }
    if (!(node instanceof LdcInsnNode)) {
      throw new NotANumberException();
    }
    try {
      final Object value = ((LdcInsnNode) node).cst;
      return clazz.cast(value);
    } catch (final ClassCastException cce) {
      throw new NotANumberException();
    }
  }
  
  /**
   * Gets the boolean value.
   * 
   * @param node
   *          the node
   * @return the boolean value
   * @throws NotANumberException
   *           the not a number exception
   */
  public static boolean getBooleanValue(final AbstractInsnNode node) throws NotANumberException {
    if (node == null) {
      throw new NotANumberException();
    }
    final Number numberValue = getNumberValue(node);
    if (numberValue == null) {
      throw new NotANumberException();
    }
    final int bool = numberValue.intValue();
    return Boolean.valueOf(bool == 1);
  }
  
  /**
   * Gets the char value.
   * 
   * @param node
   *          the node
   * @return the char value
   * @throws NotANumberException
   *           the not a number exception
   */
  public static char getCharValue(final AbstractInsnNode node) throws NotANumberException {
    if (node == null) {
      throw new NotANumberException();
    }
    final Number numberValue = getNumberValue(node);
    if (numberValue == null) {
      throw new NotANumberException();
    }
    final int charValue = numberValue.intValue();
    return Character.valueOf((char) charValue);
  }
  
  /**
   * Checks if node1 is a predecessor of node2.
   * 
   * @param node1
   *          the first node
   * @param node2
   *          the other node
   * @return true, if node1 is a predecessor of node2
   */
  public static boolean isPredecessor(final AbstractInsnNode node1, final AbstractInsnNode node2) {
    AbstractInsnNode prev = node2;
    while (prev != null) {
      if (prev == node1) {
        return true;
      }
      prev = getPrevious(prev);
    }
    return false;
  }
  
  /**
   * Checks if node is astore.
   * 
   * @param node
   *          the node
   * @return true if node is astore
   */
  public static boolean isAstore(final AbstractInsnNode node) {
    if (node == null) {
      return false;
    }
    return node.getOpcode() == Opcodes.ASTORE;
  }
  
  /**
   * Checks if node is int node.
   * 
   * @param node
   *          the node
   * @return true if node is int node
   */
  public static boolean isIntNode(final AbstractInsnNode node) {
    if (node == null) {
      return false;
    }
    return isIconst(node) || isBipush(node) || isSipush(node) || node instanceof LdcInsnNode;
  }
  
  /**
   * Checks if node is sipush.
   * 
   * @param node
   *          the node
   * @return true if node is sipush
   */
  public static boolean isSipush(final AbstractInsnNode node) {
    if (node == null) {
      return false;
    }
    return node.getOpcode() == Opcodes.SIPUSH;
  }
  
  /**
   * Checks if this is a cast.
   * <p>
   * i2l 0x85 value → result convert an int into a long<br/>
   * i2f 0x86 value → result convert an int into a float<br/>
   * i2d 0x87 value → result convert an int into a double<br/>
   * l2i 0x88 value → result convert a long to a int<br/>
   * l2f 0x89 value → result convert a long to a float<br/>
   * l2d 0x8a value → result convert a long to a double<br/>
   * f2i 0x8b value → result convert a float to an int<br/>
   * f2l 0x8c value → result convert a float to a long<br/>
   * f2d 0x8d value → result convert a float to a double<br/>
   * d2i 0x8e value → result convert a double to an int<br/>
   * d2l 0x8f value → result convert a double to a long<br/>
   * d2f 0x90 value → result convert a double to a float<br/>
   * i2b 0x91 value → result convert an int into a byte<br/>
   * i2c 0x92 value → result convert an int into a character<br/>
   * i2s 0x93 value → result convert an int into a short
   * 
   * @param numberNode
   *          the number node
   * @return true if numberNode is cast
   */
  public static boolean isCast(final AbstractInsnNode numberNode) {
    if (numberNode == null) {
      return false;
    }
    if (numberNode.getOpcode() >= I2L && numberNode.getOpcode() <= I2S) {
      return true;
    }
    return false;
  }
  
  /**
   * Checks if node is a number node.
   * 
   * @param node
   *          the node
   * @return true if node is a number node
   */
  public static boolean isNumberNode(final AbstractInsnNode node) {
    try {
      getNumberValue(node);
      return true;
    } catch (final NotANumberException nane) {
      return false;
    }
  }
  
  /**
   * Returns true if node is an if-Statement.
   * 
   * @param node
   *          the node
   * @return true if node is if
   */
  public static boolean isIf(final AbstractInsnNode node) {
    if (node == null) {
      return false;
    }
    return isTwoValueIf(node) || isOneValueIf(node);
  }
  
  /**
   * Returns true if node is an one-param-if-Statement.
   * 
   * @param node
   *          the node
   * @return true if node is one value if
   */
  public static boolean isOneValueIf(final AbstractInsnNode node) {
    if (node == null) {
      return false;
    }
    if (node.getOpcode() == Opcodes.IFNULL) {
      return true;
    }
    if (node.getOpcode() == Opcodes.IFNONNULL) {
      return true;
    }
    if (node.getOpcode() >= Opcodes.IFEQ && node.getOpcode() <= Opcodes.IFLE) {
      return true;
    }
    return false;
  }
  
  /**
   * Returns true if node is an two-param-if-Statement.
   * 
   * @param node
   *          the node
   * @return true if node is two value if
   */
  public static boolean isTwoValueIf(final AbstractInsnNode node) {
    if (node == null) {
      return false;
    }
    return node.getOpcode() >= Opcodes.IF_ICMPEQ && node.getOpcode() <= Opcodes.IF_ACMPNE;
  }
  
  /**
   * Gets the owner of the fieldNode.
   * 
   * @param node
   *          the node
   * @return the fielowner
   */
  public static String getFieldowner(final AbstractInsnNode node) {
    if (!(node instanceof FieldInsnNode)) {
      return null;
    }
    return ((FieldInsnNode) node).owner;
  }
  
  /**
   * Gets the var index or -1 if node is not a VarInsnNode.
   * 
   * @param node
   *          the node
   * @return the var index
   */
  public static int getVarIndex(final AbstractInsnNode node) {
    if (node instanceof VarInsnNode) {
      return ((VarInsnNode) node).var;
    }
    return -1;
  }
  
  /**
   * Gets the method name.
   * 
   * @param node
   *          the node
   * @return the method name
   */
  public static String getMethodName(final AbstractInsnNode node) {
    if (!(node instanceof MethodInsnNode)) {
      return null;
    }
    return ((MethodInsnNode) node).name;
  }
  
  /**
   * Gets the method owner.
   * 
   * @param node
   *          the node
   * @return the method owner
   */
  public static String getMethodOwner(final AbstractInsnNode node) {
    if (!(node instanceof MethodInsnNode)) {
      return null;
    }
    return ((MethodInsnNode) node).owner;
  }
  
  /**
   * returns the first Element of the "Stack" that produces the value, that is stored
   * by this node.
   * 
   * returns null if node is null.
   * returns null if node is not a store.
   * 
   * @param node
   *          the node
   * @return the first of stack
   */
  public static AbstractInsnNode getFirstOfStack(final AbstractInsnNode node) {
    if (node == null) {
      return null;
    }
    int opcode = node.getOpcode();
    if (opcode < ISTORE || opcode > ASTORE) {
      return null;
    }
    
    int stackCounter = 1;
    AbstractInsnNode prev = getPrevious(node);
    while (prev != null) {
      opcode = prev.getOpcode();
      stackCounter = getStackCounter(opcode, stackCounter, prev);
      
      if (stackCounter == 0) {
        return prev;
      }
      prev = prev.getPrevious();
    }
    return null;
  }
  
  private static int getStackCounter(final int opcode, final int currentStackCounter, final AbstractInsnNode prev) {
    int localStackCounter = currentStackCounter;
    if (opcode >= ACONST_NULL && opcode <= LDC) {
      localStackCounter--;
    } else if (opcode >= ILOAD && opcode <= ALOAD) {
      localStackCounter--;
    } else if (opcode >= IALOAD && opcode <= AALOAD) {
      localStackCounter++;
    } else if (opcode >= IADD && opcode <= DREM) {
      localStackCounter++;
    } else if (opcode >= INVOKEVIRTUAL && opcode <= INVOKEDYNAMIC) {
      final String desc = ((MethodInsnNode) prev).desc;
      final Type methodType = Type.getMethodType(desc);
      final Type[] argumentTypes = methodType.getArgumentTypes();
      final int length = argumentTypes.length;
      if (length == 0) {
        localStackCounter++;
      } else {
        localStackCounter += length;
      }
      if (opcode == INVOKESTATIC) {
        localStackCounter--;
      }
    } else if (prev instanceof LabelNode) {
      localStackCounter++;
    }
    return localStackCounter;
  }
  
  /**
   * To classbuilder.
   * Prints Javacode to generate the given method with {@link de.tuberlin.uebb.jbop.optimizer.ClassNodeBuilder}.
   * 
   * @param methodName
   *          the method name
   * @param ofClass
   *          the of class
   * @throws IOException
   *           Signals that an I/O exception has occurred.
   */
  public static void toClassbuilder(final String methodName, final Class<?> ofClass) throws IOException {
    final ClassNode classVisitor = new ClassNode(ASM5);
    new ClassReader(ofClass.getName()).accept(classVisitor, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
    for (final MethodNode method : classVisitor.methods) {
      if (methodName.equals(method.name)) {
        NodeHelper.printMethod(method, true);
      }
    }
  }
  
  /**
   * Prints the method.
   * 
   * @param node
   *          the node
   */
  public static void printMethod(final MethodNode node, final boolean build) {
    printMethod(node, null, System.out, build);
  }
  
  /**
   * Prints the method.
   * 
   * @param node
   *          the node
   * @param stream
   *          the stream
   */
  public static void printMethod(final MethodNode node, final ClassNode classNode, final PrintStream stream,
      final boolean build) {
    final Printer p;
    if (build) {
      p = new ClassNodeBuilderTextifier(node, classNode);
    } else {
      p = new ExtendedTextifier(node, classNode);
    }
    final TraceMethodVisitor traceMethodVisitor = new TraceMethodVisitor(p);
    node.accept(traceMethodVisitor);
    stream.println(StringUtils.join(p.getText(), ""));
  }
  
}
