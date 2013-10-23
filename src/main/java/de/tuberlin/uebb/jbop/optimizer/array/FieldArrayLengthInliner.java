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
package de.tuberlin.uebb.jbop.optimizer.array;

import java.util.ListIterator;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;

import de.tuberlin.uebb.jbop.exception.JBOPClassException;
import de.tuberlin.uebb.jbop.optimizer.IClassNodeAware;
import de.tuberlin.uebb.jbop.optimizer.IInputObjectAware;
import de.tuberlin.uebb.jbop.optimizer.IOptimizer;
import de.tuberlin.uebb.jbop.optimizer.var.GetFieldChainInliner;

/**
 * Inlines the size of an array (class field), so that further optimizationsteps
 * could handle these calls as constant.
 * 
 * eg:
 * 
 * <pre>
 * private final double[] d = {1.0, 2.0, 3.0};
 * ...
 * for(int i = 0; i < d.length; ++i){
 * ...
 * }
 * </pre>
 * 
 * becomes
 * 
 * <pre>
 * private final double[] d = {1.0, 2.0, 3.0};
 * ...
 * for(int i = 0; i < 3; ++i){
 * ...
 * }
 * </pre>
 * 
 * In bytecode this means
 * 
 * <pre>
 * aload        0
 * getfield     d
 * arraylength
 * </pre>
 * 
 * becomes one of
 * 
 * <pre>
 * iconstx
 * ore
 * bipush       x
 * or
 * ldc          x
 * </pre>
 * 
 * depending on the real size of d
 * 
 * @author Christopher Ewest
 */
public class FieldArrayLengthInliner implements IOptimizer, IInputObjectAware, IClassNodeAware {
  
  private boolean optimized = false;
  
  private Object instance;
  
  private ClassNode classNode;
  
  @Override
  public boolean isOptimized() {
    return optimized;
  }
  
  @Override
  public InsnList optimize(final InsnList original, final MethodNode method) throws JBOPClassException {
    optimized = false;
    final ListIterator<AbstractInsnNode> iterator = original.iterator();
    final ArrayHelper arrayHelper = new ArrayHelper();
    while (iterator.hasNext()) {
      final AbstractInsnNode aload = iterator.next();
      if (!arrayHelper.isArrayInstruction(classNode, aload, null)) {
        continue;
      }
      if (!arrayHelper.isArrayLength()) {
        continue;
      }
      final GetFieldChainInliner fieldChainInliner = new GetFieldChainInliner();
      fieldChainInliner.setIterator(iterator);
      fieldChainInliner.setInputObject(instance);
      fieldChainInliner.optimize(original, null);
      if (fieldChainInliner.isOptimized()) {
        optimized = true;
        original.remove(aload);
      }
    }
    return original;
  }
  
  @Override
  public void setInputObject(final Object input) {
    instance = input;
  }
  
  @Override
  public void setClassNode(final ClassNode classNode) {
    this.classNode = classNode;
  }
}
