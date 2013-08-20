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
import java.util.List;
import java.util.Map;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.VarInsnNode;

import de.tuberlin.uebb.jbop.optimizer.utils.NodeHelper;
import de.tuberlin.uebb.jbop.optimizer.utils.predicates.Predicates;

/**
 * The Class LocalArrayValueInliner.
 * 
 * This class can inline values of arrays (local variable).
 * 
 * Currently this is only possible for local arrays that are
 * created via 'ArrayGet' from field-multiArrays.
 * 
 * eg:
 * 
 * <pre>
 * @ImmutableArray
 * private final int[][] multArray = {{1,2,3,4},{5,6,7,8}};
 * ...
 * int[] array = multArray[0];
 * ...
 * int valueFromArray = array[3];
 * ...
 * </pre>
 * 
 * becomes
 * 
 * <pre>
 * @ImmutableArray
 * private final int[][] multArray = {{1,2,3,4},{5,6,7,8}};
 * ...
 * int[] array = multArray[0];
 * ...
 * int valueFromArray = 4;
 * ...
 * </pre>
 * 
 * @author Christopher Ewest
 */
public class LocalArrayValueInliner extends AbstractLocalArrayOptimizer {
  
  /**
   * Instantiates a new {@link LocalArrayValueInliner}.
   * 
   * @param input
   *          the input
   */
  public LocalArrayValueInliner(final Object input) {
    super(input);
  }
  
  @Override
  protected boolean handleValues(final InsnList original, final Map<Integer, Object> knownArrays,
      final AbstractInsnNode currentNode) {
    
    AbstractInsnNode arrayload = currentNode;
    AbstractInsnNode indexNode;
    final List<AbstractInsnNode> arrayloads = new ArrayList<>();
    final List<AbstractInsnNode> indexes = new ArrayList<>();
    do {
      if (!Predicates.IS_XALOAD.evaluate(arrayload)) {
        return false;
      }
      arrayloads.add(arrayload);
      indexNode = NodeHelper.getPrevious(arrayload);
      
      if (!NodeHelper.isNumberNode(indexNode)) {
        return false;
      }
      indexes.add(indexNode);
      arrayload = NodeHelper.getPrevious(indexNode);
      if (arrayload instanceof VarInsnNode) {
        break;
      }
    } while (true);
    
    final VarInsnNode previous2 = (VarInsnNode) arrayload;
    
    final Integer varIndex = Integer.valueOf(previous2.var);
    Object array = knownArrays.get(varIndex);
    
    if (array == null) {
      return false;
    }
    
    for (int i = indexes.size() - 1; i >= 0; --i) {
      final int indexInArray = NodeHelper.getNumberValue(indexes.get(i)).intValue();
      array = Array.get(array, indexInArray);
    }
    if (!(array instanceof Number)) {
      return false;
    }
    final AbstractInsnNode replacement = NodeHelper.getInsnNodeFor((Number) array);
    original.insertBefore(previous2, replacement);
    for (int i = 0; i < indexes.size(); ++i) {
      original.remove(indexes.get(i));
      original.remove(arrayloads.get(i));
    }
    original.remove(previous2);
    
    return true;
  }
}
