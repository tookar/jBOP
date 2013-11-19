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
package de.tuberlin.uebb.jbop.optimizer.methodsplitter;

import static org.objectweb.asm.Opcodes.IRETURN;
import static org.objectweb.asm.Opcodes.NOP;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.CodeSizeEvaluator;
import org.objectweb.asm.commons.LocalVariablesSorter;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import de.tuberlin.uebb.jbop.exception.JBOPClassException;
import de.tuberlin.uebb.jbop.optimizer.IOptimizer;
import de.tuberlin.uebb.jbop.optimizer.loop.SplitMarkNode;

/**
 * The Class MethodSplitter.<br/>
 * <p>
 * This Class can split methods that are longer than a given threshold<br>
 * in submethods (default is {@link #MAX_LENGTH}: {@value #MAX_LENGTH} byte).
 * <p>
 * Currently only methods that are preprocessed with the {@link de.tuberlin.uebb.jbop.optimizer.loop.ForLoopUnroller
 * ForLoopUnroller} <br>
 * can be splitted (and only if they were not to long before this preprocessing).
 * <p>
 * The Limit for Java-Methods is 64 kilobytes, therefore classes that contains methods longer than<br>
 * this size could not be loaded.
 * <p>
 * Also the JIT-Compiler of the JVM cannot process Methods that are too long(about 8 kb).<br>
 * See {@link http://blog.leenarts.net/2010/05/26/dontcompilehugemethods/} for more.
 * <p>
 * This Class tries to prevent this cases.
 * 
 * @author Christopher Ewest
 */
public class MethodSplitter implements IOptimizer {
  
  private static final class EmptyMethodVisitor extends MethodVisitor {
    
    private EmptyMethodVisitor(final int api) {
      super(api);
    }
    
    @Override
    public AnnotationVisitor visitAnnotationDefault() {
      return super.visitAnnotationDefault();
    }
    
    @Override
    public AnnotationVisitor visitAnnotation(final String desc, final boolean visible) {
      return super.visitAnnotation(desc, visible);
    }
    
    @Override
    public AnnotationVisitor visitParameterAnnotation(final int parameter, final String desc, final boolean visible) {
      return super.visitParameterAnnotation(parameter, desc, visible);
    }
    
    @Override
    public void visitAttribute(final Attribute attr) {
      super.visitAttribute(attr);
    }
    
    @Override
    public void visitCode() {
      super.visitCode();
    }
    
    @Override
    public void visitFrame(final int type, final int nLocal, final Object[] local, final int nStack,
        final Object[] stack) {
      super.visitFrame(type, nLocal, local, nStack, stack);
    }
    
    @Override
    public void visitInsn(final int opcode) {
      super.visitInsn(opcode);
    }
    
    @Override
    public void visitIntInsn(final int opcode, final int operand) {
      super.visitIntInsn(opcode, operand);
    }
    
    @Override
    public void visitVarInsn(final int opcode, final int var) {
      super.visitVarInsn(opcode, var);
    }
    
    @Override
    public void visitTypeInsn(final int opcode, final String type) {
      super.visitTypeInsn(opcode, type);
    }
    
    @Override
    public void visitFieldInsn(final int opcode, final String owner, final String name, final String desc) {
      super.visitFieldInsn(opcode, owner, name, desc);
    }
    
    @Override
    public void visitMethodInsn(final int opcode, final String owner, final String name, final String desc) {
      super.visitMethodInsn(opcode, owner, name, desc);
    }
    
    @Override
    public void visitInvokeDynamicInsn(final String name, final String desc, final Handle bsm,//
        final Object... bsmArgs) {
      super.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
    }
    
    @Override
    public void visitJumpInsn(final int opcode, final Label label) {
      super.visitJumpInsn(opcode, label);
    }
    
    @Override
    public void visitLabel(final Label label) {
      super.visitLabel(label);
    }
    
    @Override
    public void visitLdcInsn(final Object cst) {
      super.visitLdcInsn(cst);
    }
    
    @Override
    public void visitIincInsn(final int var, final int increment) {
      super.visitIincInsn(var, increment);
    }
    
    @Override
    public void visitTableSwitchInsn(final int min, final int max, final Label dflt, final Label... labels) {
      super.visitTableSwitchInsn(min, max, dflt, labels);
    }
    
    @Override
    public void visitLookupSwitchInsn(final Label dflt, final int[] keys, final Label[] labels) {
      super.visitLookupSwitchInsn(dflt, keys, labels);
    }
    
    @Override
    public void visitMultiANewArrayInsn(final String desc, final int dims) {
      super.visitMultiANewArrayInsn(desc, dims);
    }
    
    @Override
    public void visitTryCatchBlock(final Label start, final Label end, final Label handler, final String type) {
      super.visitTryCatchBlock(start, end, handler, type);
    }
    
    @Override
    public void visitLocalVariable(final String name, final String desc, final String signature, final Label start,
        final Label end, final int index) {
      super.visitLocalVariable(name, desc, signature, start, end, index);
    }
    
    @Override
    public void visitLineNumber(final int line, final Label start) {
      super.visitLineNumber(line, start);
    }
    
    @Override
    public void visitMaxs(final int maxStack, final int maxLocals) {
      super.visitMaxs(maxStack, maxLocals);
    }
    
    @Override
    public void visitEnd() {
      super.visitEnd();
    }
  }
  
  /**
   * Default Max-Length for methods.
   * This is the max length of 8 kb to fit the JIT-Compiler
   * minus 512 bytes for handling variables.
   */
  public static final int MAX_LENGTH = (7 * 1024) + 512;
  
  private final ClassNode classNode;
  private final int maxInsns;
  private final List<MethodNode> additionalMethods = new ArrayList<>();
  
  /**
   * Instantiates a new {@link MethodSplitter}.
   * 
   * @param classNode
   *          the class node
   */
  public MethodSplitter(final ClassNode classNode) {
    this(classNode, MAX_LENGTH);
  }
  
  /**
   * Instantiates a new {@link MethodSplitter}.
   * 
   * @param classNode
   *          the class node
   * @param maxInsns
   *          the max Nu,ber of insns per method.
   */
  public MethodSplitter(final ClassNode classNode, final int maxInsns) {
    super();
    this.classNode = classNode;
    this.maxInsns = maxInsns;
  }
  
  private static final int ACCESS = Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL;
  
  @Override
  public boolean isOptimized() {
    return false;
  }
  
  @Override
  public InsnList optimize(final InsnList original, final MethodNode methodNode) throws JBOPClassException {
    
    LocalVariablesSorter sorter = new LocalVariablesSorter(methodNode.access, methodNode.desc, new EmptyMethodVisitor(
        Opcodes.ASM5));
    methodNode.accept(sorter);
    
    if (getLength(methodNode) < maxInsns) {
      return clean(original);
    }
    
    final List<Block> blocks = getBlocks(original, methodNode);
    
    final String baseName = methodNode.name;
    final String[] exceptions = getExceptions(methodNode);
    
    final InsnList returnList = new InsnList();
    InsnList list = returnList;
    
    Block block = blocks.get(0);
    block.renameInsns(block.getVarMap());
    
    final String name = baseName + "__split__part__";
    final Type endType = Type.getReturnType(methodNode.desc);
    for (int i = 1; i < blocks.size(); ++i) {
      final Map<Integer, Integer> paramRenameMap = block.getVarMap();
      add(list, block.getInstructions(), original);
      block = blocks.get(i);
      block.renameInsns(paramRenameMap);
      final String methodDescriptor = block.getDescriptor();
      final String newMethodName = name + block.getBlockNumber();
      final MethodNode splitMethod = new MethodNode(Opcodes.ASM5, ACCESS, newMethodName, methodDescriptor, null,
          exceptions);
      additionalMethods.add(splitMethod);
      
      list.add(new VarInsnNode(Opcodes.ALOAD, 0));
      list.add(block.getPushParameters());
      list.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, classNode.name, splitMethod.name, methodDescriptor));
      list.add(new InsnNode(endType.getOpcode(IRETURN)));
      sorter = new LocalVariablesSorter(splitMethod.access, splitMethod.desc, new EmptyMethodVisitor(Opcodes.ASM5));
      splitMethod.accept(sorter);
      list = splitMethod.instructions;
    }
    add(list, block.getInstructions(), original);
    return returnList;
  }
  
  private InsnList clean(final InsnList original) {
    final ListIterator<AbstractInsnNode> iterator = original.iterator();
    
    while (iterator.hasNext()) {
      final AbstractInsnNode next = iterator.next();
      if ((next instanceof SplitMarkNode) || (next.getOpcode() == NOP)) {
        original.remove(next);
      }
    }
    return original;
  }
  
  private int getLength(final MethodNode methodNode) {
    final CodeSizeEvaluator codeSizeEvaluator = new CodeSizeEvaluator(null);
    methodNode.accept(codeSizeEvaluator);
    return codeSizeEvaluator.getMaxSize();
  }
  
  private void add(final InsnList list, final List<AbstractInsnNode> insns, final InsnList original) {
    for (final AbstractInsnNode node : insns) {
      original.remove(node);
      list.add(node);
    }
  }
  
  private String[] getExceptions(final MethodNode methodNode) {
    final List<String> exceptions = methodNode.exceptions;
    if ((exceptions == null) || (exceptions.size() == 0)) {
      return new String[] {};
    }
    return exceptions.toArray(new String[exceptions.size()]);
  }
  
  private List<Block> getBlocks(final InsnList original, final MethodNode methodNode) {
    final ListIterator<AbstractInsnNode> iterator = original.iterator();
    final Type[] args = Type.getArgumentTypes(methodNode.desc);
    
    final Type returnType = Type.getReturnType(methodNode.desc);
    final Block currentBlock = new Block(-1, args, returnType);
    final List<Block> blocks = new ArrayList<>();
    boolean added = false;
    int num = 0;
    Block methodBlock = new Block(num, args, returnType);
    
    while (iterator.hasNext()) {
      final AbstractInsnNode current = iterator.next();
      if (current instanceof SplitMarkNode) {
        final int methodLength = currentBlock.getSize() + methodBlock.getSize();
        if (methodLength < maxInsns) {
          methodBlock.add(currentBlock);
          currentBlock.clear();
        } else {
          if (methodBlock.getSize() == 0) {
            methodBlock.add(currentBlock);
            currentBlock.clear();
          }
          added = true;
          blocks.add(methodBlock);
          num++;
          methodBlock = new Block(num, args, returnType);
          if (currentBlock.getSize() > 0) {
            methodBlock.add(currentBlock);
            currentBlock.clear();
          }
        }
      } else {
        currentBlock.addInsn(current);
        added = false;
      }
    }
    
    if (!added) {
      methodBlock.add(currentBlock);
      blocks.add(methodBlock);
    }
    
    return blocks;
  }
  
  /**
   * Gets the additional methods.
   * 
   * @return the additional methods
   */
  public List<MethodNode> getAdditionalMethods() {
    return additionalMethods;
  }
}
