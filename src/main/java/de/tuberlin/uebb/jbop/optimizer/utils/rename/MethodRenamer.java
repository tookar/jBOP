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
package de.tuberlin.uebb.jdae.optimizer.utils.rename;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * The Class MethodRenamer.
 * 
 * Method-visitor for thw {@link ClassRenamer}.
 * Renames type- method- and Field-Instructions.
 * 
 * @author Christopher Ewest
 */
class MethodRenamer extends MethodVisitor {
  
  private final NameFixer fixer;
  
  /**
   * Instantiates a new {@link MethodRenamer}.
   * 
   * @param parent
   *          the parent
   * @param fixer
   *          the fixer
   */
  public MethodRenamer(final MethodVisitor parent, final NameFixer fixer) {
    super(Opcodes.ASM4, parent);
    this.fixer = fixer;
  }
  
  @Override
  public void visitTypeInsn(final int i, final String s) {
    final String nameToUse;
    if (fixer.contains(s)) {
      nameToUse = fixer.newName;
    } else {
      nameToUse = s;
    }
    super.visitTypeInsn(i, nameToUse);
  }
  
  @Override
  public void visitFieldInsn(final int opcode, final String owner, final String name, final String desc) {
    if (fixer.contains(owner)) {
      super.visitFieldInsn(opcode, fixer.newName, name, fixer.fix(desc));
    } else {
      super.visitFieldInsn(opcode, owner, name, fixer.fix(desc));
    }
  }
  
  @Override
  public void visitMethodInsn(final int opcode, final String owner, final String name, final String desc) {
    if (fixer.contains(owner)) {
      super.visitMethodInsn(opcode, fixer.newName, name, fixer.fix(desc));
    } else {
      super.visitMethodInsn(opcode, owner, name, fixer.fix(desc));
    }
  }
}
