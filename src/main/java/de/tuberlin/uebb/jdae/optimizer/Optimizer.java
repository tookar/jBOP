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
package de.tuberlin.uebb.jdae.optimizer;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections15.Predicate;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;

import de.tuberlin.uebb.jdae.access.OptimizerUtils;
import de.tuberlin.uebb.jdae.exception.JBOPClassException;
import de.tuberlin.uebb.jdae.optimizer.annotations.AdditionalSteps;
import de.tuberlin.uebb.jdae.optimizer.annotations.ImmutableArray;
import de.tuberlin.uebb.jdae.optimizer.annotations.Optimizable;
import de.tuberlin.uebb.jdae.optimizer.annotations.StrictLoops;
import de.tuberlin.uebb.jdae.optimizer.arithmetic.ArithmeticExpressionInterpreter;
import de.tuberlin.uebb.jdae.optimizer.array.FieldArrayLengthInliner;
import de.tuberlin.uebb.jdae.optimizer.array.FieldArrayValueInliner;
import de.tuberlin.uebb.jdae.optimizer.array.LocalArrayLengthInliner;
import de.tuberlin.uebb.jdae.optimizer.array.LocalArrayValueInliner;
import de.tuberlin.uebb.jdae.optimizer.controlflow.ConstantIfInliner;
import de.tuberlin.uebb.jdae.optimizer.loop.ForLoopUnroller;
import de.tuberlin.uebb.jdae.optimizer.methodsplitter.MethodSplitter;
import de.tuberlin.uebb.jdae.optimizer.var.FinalFieldInliner;
import de.tuberlin.uebb.jdae.optimizer.var.LocalVarInliner;
import de.tuberlin.uebb.jdae.optimizer.var.RemoveUnusedLocalVars;

/**
 * The Main class for optimizing.
 * <ol>
 * <li>reads a classfile</li>
 * <li>analyzes the annotations ({@link de.tuberlin.uebb.jdae.optimizer.annotations})</li>
 * <li>initializes optimization steps</li>
 * <li>run optimization steps</li>
 * <li>split methods</li>
 * <li>rename class</li>
 * <li>write class</li>
 * <li>initialize optimized class</li>
 * </ol>
 * 
 * Uses a cache to deliver already optimized classes.
 * 
 * @author Christopher Ewest
 */
public class Optimizer {
  
  // private static final Logger log = Logger.getLogger("Optimizer");
  
  private final Predicate<MethodNode> optimizeThis = new Predicate<MethodNode>() {
    
    private final String descriptor = Type.getType(Optimizable.class).getDescriptor();
    
    @Override
    public boolean evaluate(final MethodNode object) {
      if (object.visibleAnnotations == null) {
        return false;
      }
      for (final AnnotationNode annotation : object.visibleAnnotations) {
        if (descriptor.equals(annotation.desc)) {
          return true;
        }
      }
      return false;
    }
  };
  
  /**
   * Optimize.
   * 
   * @param <T>
   *          the type of the Object to optimize
   * @param input
   *          the input-Object to optimize
   * @param suffix
   *          the suffix for the new name
   * @return the optimized instance
   * @throws JBOPClassException
   *           if on of the steps fails.
   */
  public <T> T optimize(final T input, final String suffix) throws JBOPClassException {
    if (OptimizerUtils.existsInstance(input)) {
      return OptimizerUtils.getInstanceFor(input);
    }
    
    final ClassNode classNode = OptimizerUtils.readClass(input);
    
    final FinalFieldInliner finalFieldInliner = new FinalFieldInliner(input);
    
    final List<MethodNode> additionalMethods = new ArrayList<>();
    for (final MethodNode methodNode : classNode.methods) {
      if (optimizeThis.evaluate(methodNode)) {
        // Constantfolding needs to be run only once per method
        methodNode.instructions = finalFieldInliner.optimize(methodNode.instructions, methodNode);
        
        final List<IOptimizer> optimizers = initOptimizers(classNode, methodNode, input);
        // stores newly created Methods (see de.tuberlin.uebb.jdae.optimizer.methodsplitter.MethodSplitter)
        // for later usage.
        // Direct adding to classNode.methods would cause a concurrentModification-Exception
        additionalMethods.addAll(runOptimization(optimizers, methodNode, classNode));
      }
      
    }
    
    classNode.methods.addAll(additionalMethods);
    
    return OptimizerUtils.createInstance(classNode, input, suffix);
  }
  
  /**
   * Perform the OptimizationSteps.
   * 
   * Runs as long as one of the steps has made changes.
   * 
   * At the end, the {@link MethodSplitter} runs.
   */
  private List<MethodNode> runOptimization(final List<IOptimizer> optimizers, final MethodNode methodNode,
      final ClassNode classNode) throws JBOPClassException {
    boolean canOptimize = true;
    InsnList list = methodNode.instructions;
    while (canOptimize) {
      boolean optimized = false;
      for (final IOptimizer optimizer : optimizers) {
        list = optimizer.optimize(list, methodNode);
        optimized |= optimizer.isOptimized();
        methodNode.instructions = list;
      }
      canOptimize = optimized;
    }
    final MethodSplitter methodSplitter = new MethodSplitter(classNode);
    methodNode.instructions = methodSplitter.optimize(list, methodNode);
    return methodSplitter.getAdditionalMethods();
  }
  
  /**
   * Init the Optimizersteps that could / should be performed.
   */
  List<IOptimizer> initOptimizers(final ClassNode classNode, final MethodNode methodNode, final Object input)
      throws JBOPClassException {
    
    final List<IOptimizer> optimizers = new ArrayList<>();
    
    final String additionalSteps = Type.getType(AdditionalSteps.class).getDescriptor();
    for (final AnnotationNode annotation : methodNode.visibleAnnotations) {
      if (additionalSteps.equals(annotation.desc)) {
        final List<Object> values = annotation.values;
        if (values.size() != 2) {
          continue;
        }
        final List<Type> additionalOptimizers = (List<Type>) values.get(1);
        for (final Type additionalOptimizerType : additionalOptimizers) {
          try {
            final Class<? extends IOptimizer> additionalOptimizerClass = (Class<? extends IOptimizer>) Class
                .forName(additionalOptimizerType.getClassName());
            optimizers.add(additionalOptimizerClass.newInstance());
          } catch (InstantiationException | IllegalAccessException | ClassNotFoundException | ClassCastException e) {
            throw new JBOPClassException("Additional optimizationstep ('" + additionalOptimizerType.getClassName()
                + "') couldn't be instantiated.", e);
          }
        }
        break;
      }
    }
    
    final IOptimizer localArrayLengthInliner = new LocalArrayLengthInliner(input);
    optimizers.add(localArrayLengthInliner);
    
    final List<String> immutableArrayNames = new ArrayList<>();
    final List<String> finalArrayNames = new ArrayList<>();
    final String immutableArray = Type.getType(ImmutableArray.class).getDescriptor();
    for (final FieldNode field : classNode.fields) {
      if (field.desc.startsWith("[") && ((field.access & Opcodes.ACC_FINAL) != 0)) {
        finalArrayNames.add(field.name);
      }
      if (field.visibleAnnotations == null) {
        continue;
      }
      for (final AnnotationNode annotation : field.visibleAnnotations) {
        if (immutableArray.equals(annotation.desc)) {
          immutableArrayNames.add(field.name);
        }
      }
    }
    
    final IOptimizer arrayLength = new FieldArrayLengthInliner(finalArrayNames, input);
    optimizers.add(arrayLength);
    
    final String strictLoops = Type.getType(StrictLoops.class).getDescriptor();
    for (final AnnotationNode annotation : methodNode.visibleAnnotations) {
      if (strictLoops.equals(annotation.desc)) {
        final IOptimizer forLoop = new ForLoopUnroller();
        optimizers.add(forLoop);
        break;
      }
    }
    
    final IOptimizer localVars = new LocalVarInliner();
    optimizers.add(localVars);
    
    final IOptimizer unusedLocals = new RemoveUnusedLocalVars();
    optimizers.add(unusedLocals);
    
    final FieldArrayValueInliner arrayValue = new FieldArrayValueInliner(immutableArrayNames, input);
    optimizers.add(arrayValue);
    
    final IOptimizer constantIf = new ConstantIfInliner(arrayValue);
    optimizers.add(constantIf);
    
    final IOptimizer arithmeticInterpreter = new ArithmeticExpressionInterpreter();
    optimizers.add(arithmeticInterpreter);
    
    final LocalArrayValueInliner localArrayValue = new LocalArrayValueInliner(input);
    optimizers.add(localArrayValue);
    
    return optimizers;
  }
  
}
