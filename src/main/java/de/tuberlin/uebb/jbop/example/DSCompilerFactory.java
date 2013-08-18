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
package de.tuberlin.uebb.jbop.example;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.math3.exception.NumberIsTooLargeException;
import org.apache.commons.math3.util.FastMath;

import de.tuberlin.uebb.jbop.exception.JBOPClassException;
import de.tuberlin.uebb.jbop.optimizer.Optimizer;

/**
 * A factory for creating {@link DSCompiler} objects.
 * 
 * Extracted from the original {@link org.apache.commons.math3.analysis.differentiation.DSCompiler}.
 */
public class DSCompilerFactory {
  
  /** Array of all compilers created so far. */
  private static AtomicReference<IDSCompiler[][]> compilers = new AtomicReference<IDSCompiler[][]>(null);
  
  /**
   * Get the compiler for number of free parameters and order.
   * 
   * @param parameters
   *          number of free parameters
   * @param order
   *          derivation order
   * @return cached rules set
   */
  public static IDSCompiler getCompiler(final int parameters, final int order) {
    
    // get the cached compilers
    final IDSCompiler[][] cache = compilers.get();
    if ((cache != null) && (cache.length > parameters) && (cache[parameters].length > order)) {
      if (cache[parameters][order] != null) {
        // the compiler has already been created
        return cache[parameters][order];
      }
    }
    
    // we need to create more compilers
    final int maxParameters = FastMath.max(parameters, cache == null ? 0 : cache.length);
    final int maxOrder = FastMath.max(order, cache == null ? 0 : cache[0].length);
    final IDSCompiler[][] newCache = new IDSCompiler[maxParameters + 1][maxOrder + 1];
    
    if (cache != null) {
      // preserve the already created compilers
      for (int i = 0; i < cache.length; ++i) {
        System.arraycopy(cache[i], 0, newCache[i], 0, cache[i].length);
      }
    }
    
    // create the array in increasing diagonal order
    for (int diag = 0; diag <= (parameters + order); ++diag) {
      for (int o = FastMath.max(0, diag - parameters); o <= FastMath.min(order, diag); ++o) {
        final int p = diag - o;
        if (newCache[p][o] == null) {
          final IDSCompiler valueCompiler = (p == 0) ? null : newCache[p - 1][o];
          final IDSCompiler derivativeCompiler = (o == 0) ? null : newCache[p][o - 1];
          
          final int[][] sizes = compileSizes(p, o, valueCompiler);
          final int[][] derivativesIndirection = compileDerivativesIndirection(p, o, valueCompiler, derivativeCompiler);
          final int[] lowerIndirection = compileLowerIndirection(p, o, valueCompiler, derivativeCompiler);
          final int[][][] multiplicationIndirection = compileMultiplicationIndirection(p, o, valueCompiler,
              derivativeCompiler, lowerIndirection);
          final int[][][] compositionIndirection = compileCompositionIndirection(p, o, valueCompiler,
              derivativeCompiler, sizes, derivativesIndirection);
          final IDSCompiler dsCompiler = new DSCompiler(p, o, sizes, derivativesIndirection, lowerIndirection,
              multiplicationIndirection, compositionIndirection);
          final Optimizer optimizer = new Optimizer();
          IDSCompiler optimized;
          try {
            System.out.println("Creating DSCompiler" + p + "x" + o);
            optimized = optimizer.optimize(dsCompiler, "__" + p + "x" + o);
          } catch (final JBOPClassException e) {
            throw new RuntimeException("IDSCompiler couldn't be created.", e);
          }
          newCache[p][o] = optimized;
        }
      }
    }
    
    // atomically reset the cached compilers array
    compilers.compareAndSet(cache, newCache);
    
    return newCache[parameters][order];
    
  }
  
  /**
   * Compile the sizes array.
   * 
   * @param parameters
   *          number of free parameters
   * @param order
   *          derivation order
   * @param valueCompiler
   *          compiler for the value part
   * @return sizes array
   */
  private static int[][] compileSizes(final int parameters, final int order, final IDSCompiler valueCompiler) {
    
    final int[][] sizes = new int[parameters + 1][order + 1];
    if (parameters == 0) {
      Arrays.fill(sizes[0], 1);
    } else {
      System.arraycopy(valueCompiler.getSizes(), 0, sizes, 0, parameters);
      sizes[parameters][0] = 1;
      for (int i = 0; i < order; ++i) {
        sizes[parameters][i + 1] = sizes[parameters][i] + sizes[parameters - 1][i + 1];
      }
    }
    
    return sizes;
    
  }
  
  /**
   * Compile the derivatives indirection array.
   * 
   * @param parameters
   *          number of free parameters
   * @param order
   *          derivation order
   * @param valueCompiler
   *          compiler for the value part
   * @param derivativeCompiler
   *          compiler for the derivative part
   * @return derivatives indirection array
   */
  private static int[][] compileDerivativesIndirection(final int parameters, final int order,
      final IDSCompiler valueCompiler, final IDSCompiler derivativeCompiler) {
    
    if ((parameters == 0) || (order == 0)) {
      return new int[1][parameters];
    }
    
    final int vSize = valueCompiler.getDerivativesIndirection().length;
    final int dSize = derivativeCompiler.getDerivativesIndirection().length;
    final int[][] derivativesIndirection = new int[vSize + dSize][parameters];
    
    // set up the indices for the value part
    for (int i = 0; i < vSize; ++i) {
      // copy the first indices, the last one remaining set to 0
      System.arraycopy(valueCompiler.getDerivativesIndirection()[i], 0, derivativesIndirection[i], 0, parameters - 1);
    }
    
    // set up the indices for the derivative part
    for (int i = 0; i < dSize; ++i) {
      
      // copy the indices
      System.arraycopy(derivativeCompiler.getDerivativesIndirection()[i], 0, derivativesIndirection[vSize + i], 0,
          parameters);
      
      // increment the derivation order for the last parameter
      derivativesIndirection[vSize + i][parameters - 1]++;
      
    }
    
    return derivativesIndirection;
    
  }
  
  /**
   * Compile the lower derivatives indirection array.
   * <p>
   * This indirection array contains the indices of all elements except derivatives for last derivation order.
   * </p>
   * 
   * @param parameters
   *          number of free parameters
   * @param order
   *          derivation order
   * @param valueCompiler
   *          compiler for the value part
   * @param derivativeCompiler
   *          compiler for the derivative part
   * @return lower derivatives indirection array
   */
  private static int[] compileLowerIndirection(final int parameters, final int order, final IDSCompiler valueCompiler,
      final IDSCompiler derivativeCompiler) {
    
    if ((parameters == 0) || (order <= 1)) {
      return new int[] {
        0
      };
    }
    
    // this is an implementation of definition 6 in Dan Kalman's paper.
    final int vSize = valueCompiler.getLowerIndirection().length;
    final int dSize = derivativeCompiler.getLowerIndirection().length;
    final int[] lowerIndirection = new int[vSize + dSize];
    System.arraycopy(valueCompiler.getLowerIndirection(), 0, lowerIndirection, 0, vSize);
    for (int i = 0; i < dSize; ++i) {
      lowerIndirection[vSize + i] = valueCompiler.getSize() + derivativeCompiler.getLowerIndirection()[i];
    }
    
    return lowerIndirection;
    
  }
  
  /**
   * Compile the multiplication indirection array.
   * <p>
   * This indirection array contains the indices of all pairs of elements involved when computing a multiplication. This
   * allows a straightforward loop-based multiplication (see
   * {@link #multiply(double[], int, double[], int, double[], int)}).
   * </p>
   * 
   * @param parameters
   *          number of free parameters
   * @param order
   *          derivation order
   * @param valueCompiler
   *          compiler for the value part
   * @param derivativeCompiler
   *          compiler for the derivative part
   * @param lowerIndirection
   *          lower derivatives indirection array
   * @return multiplication indirection array
   */
  private static int[][][] compileMultiplicationIndirection(final int parameters, final int order,
      final IDSCompiler valueCompiler, final IDSCompiler derivativeCompiler, final int[] lowerIndirection) {
    
    if ((parameters == 0) || (order == 0)) {
      return new int[][][] {
        {
          {
              1, 0, 0
          }
        }
      };
    }
    
    // this is an implementation of definition 3 in Dan Kalman's paper.
    final int vSize = valueCompiler.getMultIndirection().length;
    final int dSize = derivativeCompiler.getMultIndirection().length;
    final int[][][] multIndirection = new int[vSize + dSize][][];
    
    System.arraycopy(valueCompiler.getMultIndirection(), 0, multIndirection, 0, vSize);
    
    for (int i = 0; i < dSize; ++i) {
      final int[][] dRow = derivativeCompiler.getMultIndirection()[i];
      final List<int[]> row = new ArrayList<int[]>();
      for (int j = 0; j < dRow.length; ++j) {
        row.add(new int[] {
            dRow[j][0], lowerIndirection[dRow[j][1]], vSize + dRow[j][2]
        });
        row.add(new int[] {
            dRow[j][0], vSize + dRow[j][1], lowerIndirection[dRow[j][2]]
        });
      }
      
      // combine terms with similar derivation orders
      final List<int[]> combined = new ArrayList<int[]>(row.size());
      for (int j = 0; j < row.size(); ++j) {
        final int[] termJ = row.get(j);
        if (termJ[0] > 0) {
          for (int k = j + 1; k < row.size(); ++k) {
            final int[] termK = row.get(k);
            if ((termJ[1] == termK[1]) && (termJ[2] == termK[2])) {
              // combine termJ and termK
              termJ[0] += termK[0];
              // make sure we will skip termK later on in the outer loop
              termK[0] = 0;
            }
          }
          combined.add(termJ);
        }
      }
      
      multIndirection[vSize + i] = combined.toArray(new int[combined.size()][]);
      
    }
    
    return multIndirection;
    
  }
  
  /**
   * Compile the function composition indirection array.
   * <p>
   * This indirection array contains the indices of all sets of elements involved when computing a composition. This
   * allows a straightforward loop-based composition (see {@link #compose(double[], int, double[], double[], int)}).
   * </p>
   * 
   * @param parameters
   *          number of free parameters
   * @param order
   *          derivation order
   * @param valueCompiler
   *          compiler for the value part
   * @param derivativeCompiler
   *          compiler for the derivative part
   * @param sizes
   *          sizes array
   * @param derivativesIndirection
   *          derivatives indirection array
   * @return multiplication indirection array
   */
  private static int[][][] compileCompositionIndirection(final int parameters, final int order,
      final IDSCompiler valueCompiler, final IDSCompiler derivativeCompiler, final int[][] sizes,
      final int[][] derivativesIndirection) {
    
    if ((parameters == 0) || (order == 0)) {
      return new int[][][] {
        {
          {
              1, 0
          }
        }
      };
    }
    
    final int vSize = valueCompiler.getCompIndirection().length;
    final int dSize = derivativeCompiler.getCompIndirection().length;
    final int[][][] compIndirection = new int[vSize + dSize][][];
    
    // the composition rules from the value part can be reused as is
    System.arraycopy(valueCompiler.getCompIndirection(), 0, compIndirection, 0, vSize);
    
    // the composition rules for the derivative part are deduced by
    // differentiation the rules from the underlying compiler once
    // with respect to the parameter this compiler handles and the
    // underlying one did not handle
    for (int i = 0; i < dSize; ++i) {
      final List<int[]> row = new ArrayList<int[]>();
      for (final int[] term : derivativeCompiler.getCompIndirection()[i]) {
        
        // handle term p * f_k(g(x)) * g_l1(x) * g_l2(x) * ... * g_lp(x)
        
        // derive the first factor in the term: f_k with respect to new parameter
        final int[] derivedTermF = new int[term.length + 1];
        derivedTermF[0] = term[0];     // p
        derivedTermF[1] = term[1] + 1; // f_(k+1)
        final int[] orders = new int[parameters];
        orders[parameters - 1] = 1;
        derivedTermF[term.length] = getPartialDerivativeIndex(parameters, order, sizes, orders);  // g_1
        for (int j = 2; j < term.length; ++j) {
          // convert the indices as the mapping for the current order
          // is different from the mapping with one less order
          derivedTermF[j] = convertIndex(term[j], parameters, derivativeCompiler.getDerivativesIndirection(),
              parameters, order, sizes);
        }
        Arrays.sort(derivedTermF, 2, derivedTermF.length);
        row.add(derivedTermF);
        
        // derive the various g_l
        for (int l = 2; l < term.length; ++l) {
          final int[] derivedTermG = new int[term.length];
          derivedTermG[0] = term[0];
          derivedTermG[1] = term[1];
          for (int j = 2; j < term.length; ++j) {
            // convert the indices as the mapping for the current order
            // is different from the mapping with one less order
            derivedTermG[j] = convertIndex(term[j], parameters, derivativeCompiler.getDerivativesIndirection(),
                parameters, order, sizes);
            if (j == l) {
              // derive this term
              System.arraycopy(derivativesIndirection[derivedTermG[j]], 0, orders, 0, parameters);
              orders[parameters - 1]++;
              derivedTermG[j] = getPartialDerivativeIndex(parameters, order, sizes, orders);
            }
          }
          Arrays.sort(derivedTermG, 2, derivedTermG.length);
          row.add(derivedTermG);
        }
        
      }
      
      // combine terms with similar derivation orders
      final List<int[]> combined = new ArrayList<int[]>(row.size());
      for (int j = 0; j < row.size(); ++j) {
        final int[] termJ = row.get(j);
        if (termJ[0] > 0) {
          for (int k = j + 1; k < row.size(); ++k) {
            final int[] termK = row.get(k);
            boolean equals = termJ.length == termK.length;
            for (int l = 1; equals && (l < termJ.length); ++l) {
              equals &= termJ[l] == termK[l];
            }
            if (equals) {
              // combine termJ and termK
              termJ[0] += termK[0];
              // make sure we will skip termK later on in the outer loop
              termK[0] = 0;
            }
          }
          combined.add(termJ);
        }
      }
      
      compIndirection[vSize + i] = combined.toArray(new int[combined.size()][]);
      
    }
    
    return compIndirection;
    
  }
  
  /**
   * Get the index of a partial derivative in an array.
   * 
   * @param parameters
   *          number of free parameters
   * @param order
   *          derivation order
   * @param sizes
   *          sizes array
   * @param orders
   *          derivation orders with respect to each parameter
   *          (the lenght of this array must match the number of parameters)
   * @return index of the partial derivative
   * @throws NumberIsTooLargeException
   *           if sum of derivation orders is larger
   *           than the instance limits
   */
  static int getPartialDerivativeIndex(final int parameters, final int order, final int[][] sizes, final int... orders)
      throws NumberIsTooLargeException {
    
    // the value is obtained by diving into the recursive Dan Kalman's structure
    // this is theorem 2 of his paper, with recursion replaced by iteration
    int index = 0;
    int m = order;
    int ordersSum = 0;
    for (int i = parameters - 1; i >= 0; --i) {
      
      // derivative order for current free parameter
      int derivativeOrder = orders[i];
      
      // safety check
      ordersSum += derivativeOrder;
      if (ordersSum > order) {
        throw new NumberIsTooLargeException(ordersSum, order, true);
      }
      
      while (derivativeOrder-- > 0) {
        // as long as we differentiate according to current free parameter,
        // we have to skip the value part and dive into the derivative part
        // so we add the size of the value part to the base index
        index += sizes[i][m--];
      }
      
    }
    
    return index;
    
  }
  
  /**
   * Convert an index from one (parameters, order) structure to another.
   * 
   * @param index
   *          index of a partial derivative in source derivative structure
   * @param srcP
   *          number of free parameters in source derivative structure
   * @param srcDerivativesIndirection
   *          derivatives indirection array for the source
   *          derivative structure
   * @param destP
   *          number of free parameters in destination derivative structure
   * @param destO
   *          derivation order in destination derivative structure
   * @param destSizes
   *          sizes array for the destination derivative structure
   * @return index of the partial derivative with the <em>same</em> characteristics
   *         in destination derivative structure
   */
  private static int convertIndex(final int index, final int srcP, final int[][] srcDerivativesIndirection,
      final int destP, final int destO, final int[][] destSizes) {
    final int[] orders = new int[destP];
    System.arraycopy(srcDerivativesIndirection[index], 0, orders, 0, FastMath.min(srcP, destP));
    return getPartialDerivativeIndex(destP, destO, destSizes, orders);
  }
}
