/*
 * Copyright (C) 2013 uebb.tu-berlin.de.
 * 
 * This file is part of JBOP (Java Bytecode OPtimizer).
 * 
 * JBOP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * JBOP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General License for more details.
 * 
 * You should have received a copy of the GNU Lesser General License
 * along with JBOP. If not, see <http://www.gnu.org/licenses/>.
 */
package de.tuberlin.uebb.jbop.optimizer.methodsplitter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.CodeSizeEvaluator;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import de.tuberlin.uebb.jbop.optimizer.loop.SplitMarkNode;
import de.tuberlin.uebb.jbop.optimizer.utils.NodeHelper;

/**
 * The Class Block.
 * 
 * This is a DTO / BO that holds instructions for a Method.
 * Whenever a instruction is added to the block, parameters of the method
 * are updated on the fly.
 * 
 * @author Christopher Ewest
 */
class Block implements Comparable<Block> {
  
  private final VarList readers = new VarList();
  private final VarList writers = new VarList();
  private final VarList parameters = new VarList();
  private final Type[] args;
  
  /** The insns. */
  private final List<AbstractInsnNode> insns = new ArrayList<>();
  
  private CodeSizeEvaluator sizeEvaluator = new CodeSizeEvaluator(null);
  
  /** The num. */
  private final int num;
  private final Map<Integer, Integer> varMap = new HashMap<Integer, Integer>();
  private final InsnList pushParameters = new InsnList();
  private int varIndex = 1;
  private final int firstLocal;
  private final int parameterIndexes[];
  
  /**
   * Instantiates a new {@link Block}.
   * 
   * @param num
   *          the number of the block
   * @param args
   *          the top-Level parameters of the method
   */
  Block(final int num, final Type[] args) {
    this.args = args;
    this.num = num;
    int i = 0;
    parameterIndexes = new int[args.length];
    for (final Type type : args) {
      parameterIndexes[i] = varIndex;
      pushParameters.add(new VarInsnNode(getOpcode(type), varIndex));
      parameters.add(new Var(varIndex, -1, VarType.READ, type));
      varMap.put(Integer.valueOf(varIndex), Integer.valueOf(varIndex));
      varIndex += type.getSize();
      i++;
    }
    firstLocal = varIndex;
  }
  
  private int getOpcode(final Type type) {
    if (Type.BOOLEAN_TYPE.equals(type)) {
      return Opcodes.ILOAD;
    }
    if (Type.INT_TYPE.equals(type)) {
      return Opcodes.ILOAD;
    }
    if (Type.FLOAT_TYPE.equals(type)) {
      return Opcodes.FLOAD;
    }
    if (Type.LONG_TYPE.equals(type)) {
      return Opcodes.LLOAD;
    }
    if (Type.DOUBLE_TYPE.equals(type)) {
      return Opcodes.DLOAD;
    }
    if (Type.CHAR_TYPE.equals(type)) {
      return Opcodes.ILOAD;
    }
    return Opcodes.ALOAD;
  }
  
  /**
   * Gets the current Size of the method.
   * 
   * @see CodeSizeEvaluator
   * 
   * @return the size
   */
  int getSize() {
    return sizeEvaluator.getMaxSize();
  }
  
  @Override
  public String toString() {
    return "Block " + num + ": " + getSize() + "bytes";
  }
  
  @Override
  public int compareTo(final Block o) {
    return Integer.compare(num, o.num);
  }
  
  /**
   * Adds the insn to this block.
   * 
   * @param insn
   *          the insn
   */
  void addInsn(final AbstractInsnNode insn) {
    addInsn(insn, false);
  }
  
  /**
   * Adds the insn to this block.
   * Computes parameters and types if deep is true.
   * 
   * @param insn
   *          the insn
   * @param deep
   *          compute parameters and types?
   */
  void addInsn(final AbstractInsnNode insn, final boolean deep) {
    insns.add(insn);
    if (!(insns instanceof SplitMarkNode)) {
      insn.accept(sizeEvaluator);
    }
    if (!deep) {
      return;
    }
    if (isStore(insn)) {
      if (writers.containsIndex(getIndex(insn))) {
        return;
      }
      final Type findType = findType(insn);
      if (insn instanceof InsnNode) {
        writers.add(new Var(-1, insns.indexOf(insn), VarType.WRITE, findType));
      } else {
        final VarInsnNode var = (VarInsnNode) insn;
        writers.add(new Var(var.var, insns.indexOf(insn), VarType.WRITE, findType));
      }
    } else if (isLoad(insn)) {
      final VarInsnNode var = (VarInsnNode) insn;
      final int index = var.var;
      if (index == 0) {
        return;
      }
      
      final Type findType = findType(insn);
      
      final Var e = new Var(index, insns.indexOf(insn), VarType.READ, findType);
      readers.add(e);
      if (!writers.containsIndex(index) && !parameters.containsIndex(index)) {
        parameters.add(e);
        
        final VarInsnNode node = new VarInsnNode(var.getOpcode(), index);
        pushParameters.add(node);
        addVarMapping(var, findType);
      }
    }
  }
  
  private void addVarMapping(final VarInsnNode var, final Type type) {
    final Integer index = Integer.valueOf(var.var);
    if (varMap.containsKey(index)) {
      return;
    }
    varMap.put(index, Integer.valueOf(varIndex));
    varIndex += type.getSize();
  }
  
  /**
   * Gets the parameter types (including top-level parameters).
   * 
   * @return the parameter types
   */
  Type[] getParameterTypes() {
    final Type[] types = new Type[parameters.size()];
    int i = 0;
    for (final Var parameter : parameters) {
      types[i++] = parameter.getParameterType();
    }
    return types;
  }
  
  /**
   * Gets the push parameters.
   * 
   * This is an instruction list preparing the stack to call *this* method.
   * 
   * @return the push parameters
   */
  InsnList getPushParameters() {
    return pushParameters;
  }
  
  /**
   * Gets the end type.
   * 
   * @return the end type
   */
  Type getEndType() {
    final AbstractInsnNode lastStore = getLastStore();
    final int index = getIndex(lastStore);
    if (index == -1) {
      return Type.VOID_TYPE;
    }
    final Var lastVar = writers.getLastVar(index);
    if ((lastVar == null)) {
      if ((lastStore == null)) {
        return Type.VOID_TYPE;
      }
      return findType(lastStore);
    }
    return lastVar.getParameterType();
  }
  
  /**
   * Finds the type of the given node.
   * 
   * @param node
   *          the node
   * @return the type
   */
  Type findType(final AbstractInsnNode node) {
    final int index = getIndex(node);
    
    // Types from methodParameters via methodDescriptor
    if ((index > 0) && (index < firstLocal)) {
      int argsIndex = 1;
      for (final Type type : args) {
        if (index == argsIndex) {
          return type;
        }
        argsIndex += type.getSize();
      }
    }
    
    // simple Types
    final int opcode = node.getOpcode();
    if ((opcode == Opcodes.ILOAD) || (opcode == Opcodes.ISTORE) || (opcode == Opcodes.IRETURN)) {
      return Type.INT_TYPE;
    }
    if ((opcode == Opcodes.FLOAD) || (opcode == Opcodes.FSTORE) || (opcode == Opcodes.FRETURN)) {
      return Type.FLOAT_TYPE;
    }
    if ((opcode == Opcodes.LLOAD) || (opcode == Opcodes.LSTORE) || (opcode == Opcodes.LRETURN)) {
      return Type.LONG_TYPE;
    }
    if ((opcode == Opcodes.DLOAD) || (opcode == Opcodes.DSTORE) || (opcode == Opcodes.DRETURN)) {
      return Type.DOUBLE_TYPE;
    }
    
    // ALOAD
    
    // Objects that are loaded and are not parameters must have been written before
    if (isLoad(node)) {
      final Var firstVar = writers.getFirstVar(index);
      if (firstVar == null) {
        // write was in a different block, so this is a new parameter for this method
        // so find the writer and return the type of it.
        return findType(getWriter(node));
      }
      return firstVar.getParameterType();
    }
    
    // Objects that are not parameters and not written before
    // these can be:
    // getField
    // getStatic
    // new
    // new array
    // new multi array
    // return type of method call
    
    int arrayCount = 0;
    AbstractInsnNode currentNode = NodeHelper.getPrevious(node);
    while (currentNode != null) {
      final int opcode2 = currentNode.getOpcode();
      if (opcode2 == Opcodes.NEWARRAY) {
        final int operand = ((IntInsnNode) currentNode).operand;
        return getObjectType(operand);
      } else if (opcode2 == Opcodes.ANEWARRAY) {
        return getObjectType(((TypeInsnNode) currentNode).desc);
      } else if (opcode2 == Opcodes.MULTIANEWARRAY) {
        return getObjectType(((MultiANewArrayInsnNode) currentNode).desc);
      } else if (opcode2 == Opcodes.NEW) {
        final String desc = ((TypeInsnNode) currentNode).desc;
        return getObjectType(desc);
      } else if ((opcode2 >= Opcodes.IALOAD) && (opcode2 <= Opcodes.AALOAD)) {
        arrayCount++;
      } else if ((opcode2 == Opcodes.GETFIELD) || (opcode2 == Opcodes.GETSTATIC)) {
        final String desc = ((FieldInsnNode) currentNode).desc;
        return getObjectType(removeArrayType(desc, arrayCount));
      } else if ((opcode2 == Opcodes.ALOAD)) {
        final Type type2 = readers.getFirstVar(((VarInsnNode) currentNode).var).getParameterType();
        return getObjectType(removeArrayType(type2.getDescriptor(), arrayCount));
      } else if ((opcode2 >= Opcodes.INVOKEVIRTUAL) && (opcode2 <= Opcodes.INVOKEDYNAMIC)) {
        return Type.getReturnType(((MethodInsnNode) currentNode).desc);
      }
      currentNode = NodeHelper.getPrevious(currentNode);
    }
    return Type.VOID_TYPE;
  }
  
  private AbstractInsnNode getWriter(final AbstractInsnNode node) {
    final int index = getIndex(node);
    AbstractInsnNode currentNode = NodeHelper.getPrevious(node);
    while (currentNode != null) {
      currentNode = NodeHelper.getPrevious(currentNode);
      if (isStore(currentNode) && (getIndex(currentNode) == index)) {
        return currentNode;
      }
    }
    return null;
  }
  
  private String removeArrayType(final String desc, final int count) {
    String s = desc;
    for (int i = 0; i < count; ++i) {
      if (s.charAt(0) == '[') {
        s = s.substring(1);
      }
    }
    return s;
  }
  
  private Type getObjectType(final int operand) {
    if (Opcodes.T_INT == operand) {
      return Type.getType(int[].class);
    }
    if (Opcodes.T_FLOAT == operand) {
      return Type.getType(float[].class);
    }
    if (Opcodes.T_LONG == operand) {
      return Type.getType(long[].class);
    }
    if (Opcodes.T_DOUBLE == operand) {
      return Type.getType(double[].class);
    }
    return Type.getType(Object.class);
  }
  
  private Type getObjectType(final String operand) {
    return Type.getType(operand);
  }
  
  /**
   * Gets the writers.
   * 
   * @return the writers
   */
  VarList getWriters() {
    return writers;
  }
  
  private static boolean isLoad(final AbstractInsnNode currentNode) {
    if (currentNode == null) {
      return false;
    }
    if ((currentNode.getOpcode() >= Opcodes.ILOAD) && (currentNode.getOpcode() <= Opcodes.ALOAD)) {
      return true;
    }
    return false;
  }
  
  private static boolean isStore(final AbstractInsnNode currentNode) {
    if (currentNode == null) {
      return false;
    }
    if ((currentNode.getOpcode() >= Opcodes.ISTORE) && (currentNode.getOpcode() <= Opcodes.ASTORE)) {
      return true;
    }
    if ((currentNode.getOpcode() >= Opcodes.IRETURN) && (currentNode.getOpcode() <= Opcodes.RETURN)) {
      return true;
    }
    return false;
  }
  
  /**
   * Adds the block to this block.
   * Thereby the parameters and types are calculated.
   * 
   * @param otherBlock
   *          the other block
   */
  void add(final Block otherBlock) {
    for (final AbstractInsnNode node : otherBlock.insns) {
      addInsn(node, true);
    }
  }
  
  /**
   * Clears this block.
   */
  void clear() {
    readers.clear();
    writers.clear();
    parameters.clear();
    insns.clear();
    varMap.clear();
    pushParameters.clear();
    sizeEvaluator = new CodeSizeEvaluator(null);
  }
  
  /**
   * Rename insns.
   * This means: correct the indexes of the local variables to match the parameters.
   */
  void renameInsns() {
    for (final AbstractInsnNode node : insns) {
      renameIfVar(node);
    }
  }
  
  private void renameIfVar(final AbstractInsnNode currentNode) {
    final int index = getIndex(currentNode);
    if (index < firstLocal) {
      return;
    }
    final Integer key = Integer.valueOf(index);
    Integer value = varMap.get(key);
    if (value == null) {
      value = Integer.valueOf(varIndex);
      varIndex += writers.getFirstVar(index).getParameterType().getSize();
      varMap.put(key, value);
    }
    final int mapped = value.intValue();
    setIndex(currentNode, mapped);
  }
  
  private int getIndex(final AbstractInsnNode node) {
    if (node instanceof VarInsnNode) {
      return ((VarInsnNode) node).var;
    } else if (node instanceof IincInsnNode) {
      return ((IincInsnNode) node).var;
    } else {
      return -1;
    }
  }
  
  private void setIndex(final AbstractInsnNode node, final int index) {
    if (node instanceof VarInsnNode) {
      ((VarInsnNode) node).var = index;
    } else if (node instanceof IincInsnNode) {
      ((IincInsnNode) node).var = index;
    }
  }
  
  /**
   * Gets the last store.
   * 
   * @return the last store or null if no store occurs
   */
  AbstractInsnNode getLastStore() {
    for (int i = insns.size() - 1; i >= 0; --i) {
      final AbstractInsnNode insn = insns.get(i);
      if (isStore(insn)) {
        return insn;
      }
    }
    return null;
  }
  
  int getBlockNumber() {
    return num;
  }
  
  List<AbstractInsnNode> getInstructions() {
    return Collections.unmodifiableList(insns);
  }
  
}
