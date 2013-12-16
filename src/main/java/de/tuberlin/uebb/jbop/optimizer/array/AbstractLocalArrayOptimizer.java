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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import de.tuberlin.uebb.jbop.access.ClassAccessor;
import de.tuberlin.uebb.jbop.exception.JBOPClassException;
import de.tuberlin.uebb.jbop.optimizer.IInputObjectAware;
import de.tuberlin.uebb.jbop.optimizer.IOptimizer;
import de.tuberlin.uebb.jbop.optimizer.utils.NodeHelper;

/**
 * The Class LocalArrayLengthInliner.
 * 
 * Inlines the size of an array (local variable), so that further optimizationsteps
 * could handle these calls as constant.
 * 
 * eg:
 * 
 * <pre>
 * double[] d = {1.0, 2.0, 3.0};
 * for(int i = 0; i < d.length; ++i){
 * ...
 * }
 * </pre>
 * 
 * becomes
 * 
 * <pre>
 * double[] d = {1.0, 2.0, 3.0};
 * for(int i = 0; i < 3; ++i){
 * ...
 * }
 * </pre>
 * 
 * in bytcode this means:
 * 
 * <pre>
 * aload        x
 * arraylength
 * </pre>
 * 
 * becomes
 * 
 * <pre>
 * bipush 3
 * </pre>
 * 
 * @author Christopher Ewest
 */
abstract class AbstractLocalArrayOptimizer implements IOptimizer, IInputObjectAware {
  
  private boolean optimized;
  private Object input;
  
  /**
   * Checks if is optimized.
   * 
   * @return true, if is optimized
   */
  @Override
  public boolean isOptimized() {
    return optimized;
  }
  
  @Override
  public InsnList optimize(final InsnList original, final MethodNode methodNode) throws JBOPClassException {
    optimized = false;
    final Iterator<AbstractInsnNode> iterator = original.iterator();
    final Map<Integer, Object> knownArrays = new TreeMap<>();
    while (iterator.hasNext()) {
      final AbstractInsnNode currentNode = iterator.next();
      final int registerValues = registerValues(currentNode, knownArrays);
      if (registerValues > 0) {
        if (registerValues > 1) {
          iterator.next();
        }
        continue;
      }
      optimized |= handleValues(original, knownArrays, currentNode);
    }
    return original;
  }
  
  /**
   * Handle values.
   * 
   * @param original
   *          the original
   * @param knownArrays
   *          the known arrays
   * @param currentNode
   *          the current node
   * @return true, if successful
   */
  abstract boolean handleValues(final InsnList original, final Map<Integer, Object> knownArrays,
      final AbstractInsnNode currentNode);
  
  /**
   * Register values.
   * 
   * @param currentNode
   *          the current node
   * @param knownArrays
   *          the known arrays
   * @return true, if successful
   * @throws JBOPClassException
   *           the jBOP class exception
   */
  protected int registerValues(final AbstractInsnNode currentNode, final Map<Integer, Object> knownArrays)
      throws JBOPClassException {
    if (currentNode.getOpcode() == Opcodes.ASTORE) {
      return registerGetArray(currentNode, knownArrays);
    }
    return registerAdditionalValues(currentNode, knownArrays);
  }
  
  /**
   * Register additional values.
   * Hook for inherited clases.
   * 
   * @param currentNode
   *          the current node
   * @param knownArrays
   *          the known arrays
   * @return false;
   */
  protected int registerAdditionalValues(@SuppressWarnings("unused") final AbstractInsnNode currentNode,
      @SuppressWarnings("unused") final Map<Integer, Object> knownArrays) {
    return 0;
  }
  
  /**
   * Register get array.
   * 
   * @param currentNode
   *          the current node
   * @param knownArrays
   *          the known arrays
   * @return true, if successful
   * @throws JBOPClassException
   *           the jBOP class exception
   */
  protected int registerGetArray(final AbstractInsnNode currentNode, final Map<Integer, Object> knownArrays)
      throws JBOPClassException {
    final List<AbstractInsnNode> previous = new ArrayList<>();
    final List<AbstractInsnNode> previous2 = new ArrayList<>();
    AbstractInsnNode previous2x = currentNode;
    while (true) {
      final AbstractInsnNode previousx = NodeHelper.getPrevious(previous2x);
      if (previousx.getOpcode() != Opcodes.AALOAD) {
        knownArrays.remove(Integer.valueOf(NodeHelper.getVarIndex(currentNode)));
        return 0;
      }
      previous.add(previousx);
      previous2x = NodeHelper.getPrevious(previousx);
      if (!NodeHelper.isNumberNode(previous2x)) {
        knownArrays.remove(Integer.valueOf(NodeHelper.getVarIndex(currentNode)));
        return 0;
      }
      previous2.add(previous2x);
      final AbstractInsnNode previous2xtmp = NodeHelper.getPrevious(previous2x);
      if ((previous2xtmp instanceof FieldInsnNode) || NodeHelper.isAload(previous2xtmp)) {
        break;
      }
    }
    final AbstractInsnNode previous3 = NodeHelper.getPrevious(previous2.get(previous2.size() - 1));
    Object array;
    if (previous3 instanceof VarInsnNode) {
      array = knownArrays.get(Integer.valueOf(((VarInsnNode) previous3).var));
      if (array == null) {
        knownArrays.remove(Integer.valueOf(NodeHelper.getVarIndex(currentNode)));
        return 0;
      }
    } else {
      if (!(previous3 instanceof FieldInsnNode)) {
        knownArrays.remove(Integer.valueOf(NodeHelper.getVarIndex(currentNode)));
        return 0;
      }
      final AbstractInsnNode previous4 = NodeHelper.getPrevious(previous3);
      if (!NodeHelper.isAload0(previous4)) {
        knownArrays.remove(Integer.valueOf(NodeHelper.getVarIndex(currentNode)));
        return 0;
      }
      final String fieldName = ((FieldInsnNode) previous3).name;
      
      array = ClassAccessor.getCurrentValue(input, fieldName);
    }
    final Integer varIndex = Integer.valueOf(((VarInsnNode) currentNode).var);
    
    for (int i = previous2.size() - 1; i >= 0; i--) {
      int index1;
      if (previous2.size() <= i) {
        index1 = 0;
      } else {
        final Number arrIndex = NodeHelper.getNumberValue(previous2.get(i));
        index1 = arrIndex.intValue();
      }
      array = Array.get(array, index1);
    }
    knownArrays.put(varIndex, array);
    return 1;
  }
  
  @Override
  public void setInputObject(final Object inputObject) {
    input = inputObject;
  }
}
