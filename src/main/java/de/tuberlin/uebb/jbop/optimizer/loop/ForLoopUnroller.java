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
package de.tuberlin.uebb.jbop.optimizer.loop;

import java.util.Iterator;
import java.util.LinkedList;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;

import de.tuberlin.uebb.jbop.optimizer.IOptimizer;
import de.tuberlin.uebb.jbop.optimizer.utils.Loop;
import de.tuberlin.uebb.jbop.optimizer.utils.LoopMatcher;

/**
 * Unrolls strict loops (see {@link de.tuberlin.uebb.jbop.modifier.annotations.StrictLoops}). <br>
 * eg:
 * 
 * <pre>
 * double[] d = {1.0, 2.0, 3.0}
 * double e = 0;
 * for(int i = 0; i< 3; ++i){
 *  e = e + d[i];
 * }
 * </pre>
 * 
 * becomes
 * 
 * <pre>
 * double[] d = {1.0, 2.0, 3.0}
 * double e = 0;
 * int i0 = 0;
 * e = e + d[i0];
 * int i1=1;
 * e = e + d[i1];
 * int i2=2
 * e = e + d[i2];
 * </pre>
 * 
 * in bytecode this means:
 * 
 * <pre>
 * 1    iconst0
 * 2    istore   x
 * 3    goto     l
 * 4    A
 * m    B
 * n    C
 * o    iinc     x
 * l    iload    x
 * l+1  bipush   3
 * l+2  ifcpmlt  4
 * </pre>
 * 
 * becomes
 * 
 * <pre>
 * bipush   0
 * istore   x1
 * A
 * B
 * C
 * bipush   1
 * istore   x2
 * A
 * B
 * C 
 * bipush   2
 * istore   x3
 * A
 * B
 * C
 * </pre>
 * 
 * where x1, x2, x3 are new local variables.
 * 
 * Additionally after every Loop a special NOP-Node ({@link SplitMarkNode}) is inserted.
 * These are used in the {@link de.tuberlin.uebb.jbop.optimizer.methodsplitter.MethodSplitter}.
 * 
 * @author Christopher Ewest
 */
public class ForLoopUnroller implements IOptimizer {
  
  private boolean optimized = false;
  
  @Override
  public boolean isOptimized() {
    return optimized;
  }
  
  @Override
  public InsnList optimize(final InsnList original, final MethodNode method) {
    optimized = false;
    final Iterator<AbstractInsnNode> iterator = original.iterator();
    final InsnList insn = new InsnList();
    final LinkedList<AbstractInsnNode> skipped = new LinkedList<>();
    while (iterator.hasNext()) {
      final AbstractInsnNode currentNode = iterator.next();
      final Loop loop = LoopMatcher.getLoop(currentNode);
      
      if (loop == null) {
        skipped.add(currentNode);
        continue;
      }
      final AbstractInsnNode last = skipped.getLast();
      skipped.remove(last);
      correctIteratorPosition(iterator, loop.getEndOfLoop());
      
      optimized = true;
      for (final AbstractInsnNode node : skipped) {
        insn.add(node);
      }
      skipped.clear();
      original.remove(last);
      insn.add(LoopMatcher.toForLoop(loop).getInsnList(method));
    }
    for (final AbstractInsnNode node : skipped) {
      insn.add(node);
    }
    return insn;
    
  }
  
  private void correctIteratorPosition(final Iterator<AbstractInsnNode> iterator, final AbstractInsnNode endOfLoop) {
    while (iterator.hasNext()) {
      if (iterator.next() == endOfLoop) {
        break;
      }
    }
  }
  
}
