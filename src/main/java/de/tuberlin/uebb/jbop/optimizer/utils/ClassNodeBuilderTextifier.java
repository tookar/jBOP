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

import static org.objectweb.asm.Opcodes.GETFIELD;
import static org.objectweb.asm.Opcodes.GETSTATIC;
import static org.objectweb.asm.Opcodes.IRETURN;
import static org.objectweb.asm.Opcodes.PUTSTATIC;
import static org.objectweb.asm.Opcodes.RETURN;

import org.apache.commons.lang3.StringUtils;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.util.Printer;
import org.objectweb.asm.util.Textifier;

/**
 * The Class ClassNodeBuilderTextifier.
 * 
 * @author Christopher Ewest
 */
class ClassNodeBuilderTextifier extends Textifier {
  
  private int labelCounter = 0;
  
  /**
   * Instantiates a new {@link ClassNodeBuilderTextifier}.
   * 
   * @param node
   *          the node
   * @param classNode
   *          the class node
   */
  ClassNodeBuilderTextifier(final MethodNode node, final ClassNode classNode) {
    super();
    if (classNode != null) {
      text.add("ClassNodeBuilder builder = ClassNodeBuilder.createClass(\"" + classNode.name.replace("/", ".")
          + "\");\n");
    }
    text.add("builder.//\naddMethod(\"" + node.name + "\", \"" + node.desc + "\").//\n");
    
  }
  
  @Override
  public Textifier visitMethod(final int access, final String name, final String desc, final String signature,
      final String[] exceptions) {
    text.add("addMethod(");
    return super.visitMethod(access, name, desc, signature, exceptions);
  }
  
  @Override
  public void visitInsn(final int opcode) {
    buf.setLength(0);
    if ((opcode >= IRETURN) && (opcode <= RETURN)) {
      buf.append("addReturn().//\n");
    } else {
      buf.append("add(").append("Opcodes.").append(OPCODES[opcode]).append(").//\n");
    }
    text.add(buf.toString());
  }
  
  @Override
  public void visitIntInsn(final int opcode, final int operand) {
    buf.setLength(0);
    buf.append("add(").append("Opcodes.").append(OPCODES[opcode]).append(", ");
    if (opcode == Opcodes.NEWARRAY) {
      buf.append("Opcodes.").append(TYPES[operand]);
    } else {
      buf.append(Integer.toString(operand));
    }
    buf.append(").//\n");
    text.add(buf.toString());
  }
  
  @Override
  public void visitVarInsn(final int opcode, final int var) {
    buf.setLength(0);
    buf.append("add(").append("Opcodes.").append(OPCODES[opcode]).append(", ").append(var).append(").//\n");
    text.add(buf.toString());
  }
  
  @Override
  public void visitTypeInsn(final int opcode, final String type) {
    buf.setLength(0);
    buf.append("add(").append("Opcodes.").append(OPCODES[opcode]).append(", ");
    appendDescriptor(INTERNAL_NAME, type);
    buf.append(").//\n");
    text.add(buf.toString());
  }
  
  @Override
  public void visitFieldInsn(final int opcode, final String owner, final String name, final String desc) {
    buf.setLength(0);
    buf.append("add");
    if ((opcode == GETFIELD) || (opcode == GETSTATIC)) {
      buf.append("Get");
    } else {
      buf.append("Set");
    }
    if ((opcode == PUTSTATIC) || (opcode == GETSTATIC)) {
      buf.append("Static");
    } else {
      buf.append("Field");
    }
    appendDescriptor(INTERNAL_NAME, owner);
    buf.append("\", \"").append(name).append("\", \"");
    appendDescriptor(FIELD_DESCRIPTOR, desc);
    buf.append("\").//\n");
    text.add(buf.toString());
  }
  
  @Override
  public void visitMethodInsn(final int opcode, final String owner, final String name, final String desc) {
    buf.setLength(0);
    
    buf.append("invoke(\"").append("Opcodes.").append(OPCODES[opcode]).append("\", \"");
    appendDescriptor(INTERNAL_NAME, owner);
    buf.append("\", \"").append(name).append("\", \"");
    appendDescriptor(METHOD_DESCRIPTOR, desc);
    buf.append("\").//\n");
    text.add(buf.toString());
  }
  
  @Override
  public void visitJumpInsn(final int opcode, final Label label) {
    buf.setLength(0);
    buf.append("add(").append("Opcodes.").append(OPCODES[opcode]).append(", ");
    appendLabel(label);
    buf.append(").//\n");
    text.add(buf.toString());
  }
  
  @Override
  public void visitLabel(final Label label) {
    buf.setLength(0);
    buf.append("addInsn(");
    appendLabel(label);
    buf.append(").//\n");
    text.add(buf.toString());
    text.add(labelCounter++, "LabelNode " + labelNames.get(label) + " = new LabelNode();\n");
  }
  
  @Override
  public void visitLdcInsn(final Object cst) {
    buf.setLength(0);
    buf.append("add(").append("LDC, ");
    if (cst instanceof String) {
      Printer.appendString(buf, (String) cst);
    } else if (cst instanceof Type) {
      buf.append(((Type) cst).getDescriptor()).append(".class");
    } else {
      buf.append(cst);
    }
    buf.append(").//\n");
    text.add(buf.toString());
  }
  
  @Override
  public void visitIincInsn(final int var, final int increment) {
    buf.setLength(0);
    buf.append("add(").append("Opcodes.IINC, ").append(var).append(", ").append(increment).append(").//\n");
    text.add(buf.toString());
  }
  
  /**
   * Prints a disassembled view of the given annotation.
   * 
   * @param desc
   *          the class descriptor of the annotation class.
   * @param visible
   *          <tt>true</tt> if the annotation is visible at runtime.
   * @return a visitor to visit the annotation values.
   */
  @Override
  public Textifier visitAnnotation(final String desc, final boolean visible) {
    buf.setLength(0);
    buf.append("withAnnotation(");
    buf.append(StringUtils.replace(StringUtils.removeEnd(StringUtils.removeStart(desc, "L"), ";"), "/", "."));
    buf.append(".class");
    text.add(buf.toString());
    text.add(").//\n");
    return createTextifier();
  }
  
  @Override
  public void visitMaxs(final int maxStack, final int maxLocals) {
    //
  }
}
