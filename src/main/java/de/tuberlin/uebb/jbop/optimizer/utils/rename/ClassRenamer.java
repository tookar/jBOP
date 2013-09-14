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
package de.tuberlin.uebb.jbop.optimizer.utils.rename;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * The Class ClassRenamer.
 * 
 * Renames a class.
 * 
 * @author Christopher Ewest
 */
public class ClassRenamer extends ClassVisitor {
  
  private final NameFixer fixer;
  
  /**
   * Instantiates a new class renamer.
   * 
   * @param parent
   *          the parent
   * @param newName
   *          the new name
   */
  public ClassRenamer(final ClassWriter parent, final String newName) {
    super(Opcodes.ASM5, parent);
    fixer = new NameFixer(newName);
  }
  
  @Override
  public void visit(final int version, final int access, final String name, final String signature,
      final String superName, final String[] interfaces) {
    fixer.add(name);
    super.visit(version, access, fixer.getNewName(), signature, superName, interfaces);
  }
  
  @Override
  public MethodVisitor visitMethod(final int access, final String name, final String desc, final String signature,
      final String[] exceptions) {
    MethodVisitor mv = super.visitMethod(access, name, fixer.fix(desc), fixer.fix(signature), exceptions);
    if ((mv != null) && ((access & Opcodes.ACC_ABSTRACT) == 0)) {
      mv = new MethodRenamer(mv, fixer);
    }
    return mv;
  }
  
  /**
   * @return the byte[] of the processed class
   */
  public byte[] toByteArray() {
    return ((ClassWriter) cv).toByteArray();
  }
}
