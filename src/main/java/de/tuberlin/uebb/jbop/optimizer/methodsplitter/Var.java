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

import org.objectweb.asm.Type;

/**
 * The Class Var.
 * 
 * This is a Wrapper for {@link org.objectweb.asm.tree.VarInsnNode}, it stores the index
 * of the variable, its position in the InsnList and the type of the variable.
 * 
 * @author Christopher Ewest
 */
class Var implements Comparable<Var> {
  
  /**
   * The Enum VarType.
   * 
   * @author Christopher Ewest
   */
  
  /** The index. */
  private final int index;
  
  /** The position. */
  private final int position;
  
  /** The var type. */
  private final VarType varType;
  
  /** The type2. */
  private final Type type2;
  
  /**
   * Instantiates a new {@link Var}.
   * 
   * @param index
   *          the index
   * @param position
   *          the position
   * @param varType
   *          the var type
   * @param type2
   *          the type2
   */
  Var(final int index, final int position, final VarType varType, final Type type2) {
    super();
    this.index = index;
    this.position = position;
    this.varType = varType;
    this.type2 = type2;
  }
  
  /**
   * Hash code.
   * 
   * @return the int
   */
  @Override
  public int hashCode() {
    return toString().hashCode();
  }
  
  @Override
  public boolean equals(final Object obj) {
    if (obj == null) {
      return false;
    }
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof Var)) {
      return false;
    }
    final Var var = (Var) obj;
    if (index != var.index) {
      return false;
    }
    if (position != var.position) {
      return false;
    }
    if (!type2.equals(var.type2)) {
      return false;
    }
    if (varType != var.varType) {
      return false;
    }
    return true;
  }
  
  @Override
  public int compareTo(final Var o) {
    if (index == o.index) {
      return position - o.position;
    }
    return index - o.index;
  }
  
  @Override
  public String toString() {
    final StringBuilder builder = new StringBuilder();
    if (varType == VarType.READ) {
      builder.append("r");
    }
    if (varType == VarType.WRITE) {
      builder.append("w");
    }
    if (varType == VarType.ARRAY_READ) {
      builder.append("ar");
    }
    if (varType == VarType.ARRAY_WRITE) {
      builder.append("aw");
    }
    if (varType == VarType.INCREMENT) {
      builder.append("i");
    }
    builder.append(index).append("@").append(position).append(":").append(type2);
    return builder.toString();
  }
  
  /**
   * Gets the parameter type.
   * 
   * @return the parameter type
   */
  Type getParameterType() {
    return type2;
  }
  
  /**
   * Gets the var index.
   * 
   * @return the var index
   */
  int getVarIndex() {
    return index;
  }
  
  /**
   * Gets the poition of the var in the original {@link org.objectweb.asm.tree.InsnList}.
   * 
   * @return the var position
   */
  int getVarPosition() {
    return position;
  }
  
}
