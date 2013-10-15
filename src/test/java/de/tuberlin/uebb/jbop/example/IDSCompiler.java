/*
 * Copyright (C) 2013 uebb.tu-berlin.de.
 * 
 * This file is part of JBOP (Java Bytecode OPtimizer).
 * 
 * JBOP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * JBOP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General License for more details.
 * 
 * You should have received a copy of the GNU Lesser General License
 * along with JBOP. If not, see <http://www.gnu.org/licenses/>.
 */
package de.tuberlin.uebb.jbop.example;

import org.apache.commons.math3.exception.DimensionMismatchException;
import org.apache.commons.math3.exception.NumberIsTooLargeException;

/**
 * The Interface IDSCompiler.
 * 
 * @author Christopher Ewest
 */
public interface IDSCompiler {
  
  /**
   * Get the index of a partial derivative in the array.
   * <p>
   * If all orders are set to 0, then the 0<sup>th</sup> order derivative is returned, which is the value of the
   * function.
   * </p>
   * <p>
   * The indices of derivatives are between 0 and {@link #getSize() getSize()} - 1. Their specific order is fixed for a
   * given compiler, but otherwise not ly specified. There are however some simple cases which have guaranteed indices:
   * </p>
   * <ul>
   * <li>the index of 0<sup>th</sup> order derivative is always 0</li>
   * <li>if there is only 1 {@link #getFreeParameters() free parameter}, then the derivatives are sorted in increasing
   * derivation order (i.e. f at index 0, df/dp at index 1, d<sup>2</sup>f/dp<sup>2</sup> at index 2 ...
   * d<sup>k</sup>f/dp<sup>k</sup> at index k),</li>
   * <li>if the {@link #getOrder() derivation order} is 1, then the derivatives are sorted in incresing free parameter
   * order (i.e. f at index 0, df/dx<sub>1</sub> at index 1, df/dx<sub>2</sub> at index 2 ... df/dx<sub>k</sub> at index
   * k),</li>
   * <li>all other cases are not ly specified</li>
   * </ul>
   * <p>
   * This method is the inverse of method {@link #getPartialDerivativeOrders(int)}
   * </p>
   * 
   * @param orders
   *          derivation orders with respect to each parameter
   * @return index of the partial derivative
   * @throws DimensionMismatchException
   *           if the numbers of parameters does not
   *           match the instance
   * @throws NumberIsTooLargeException
   *           if sum of derivation orders is larger
   *           than the instance limits
   * @see #getPartialDerivativeOrders(int)
   */
  int getPartialDerivativeIndex(final int... orders) throws DimensionMismatchException, NumberIsTooLargeException;
  
  /**
   * Get the derivation orders for a specific index in the array.
   * <p>
   * This method is the inverse of {@link #getPartialDerivativeIndex(int...)}.
   * </p>
   * 
   * @param index
   *          of the partial derivative
   * @return orders derivation orders with respect to each parameter
   * @see #getPartialDerivativeIndex(int...)
   */
  int[] getPartialDerivativeOrders(final int index);
  
  /**
   * Get the number of free parameters.
   * 
   * @return number of free parameters
   */
  int getFreeParameters();
  
  /**
   * Get the derivation order.
   * 
   * @return derivation order
   */
  int getOrder();
  
  /**
   * Get the array size required for holding partial derivatives data.
   * <p>
   * This number includes the single 0 order derivative element, which is guaranteed to be stored in the first element
   * of the array.
   * </p>
   * 
   * @return array size required for holding partial derivatives data
   */
  int getSize();
  
  /**
   * Check rules set compatibility.
   * 
   * @param compiler
   *          other compiler to check against instance
   * @throws DimensionMismatchException
   *           if number of free parameters or orders are inconsistent
   */
  void checkCompatibility(final IDSCompiler compiler) throws DimensionMismatchException;
  
  /**
   * Like
   * {@link org.apache.commons.math3.analysis.differentiation.DSCompiler#add(double[], int, double[], int, double[], int)}
   * but with constant offset "0".
   * 
   * @param lhs
   *          the lhs
   * @param rhs
   *          the rhs
   * @param result
   *          the result
   */
  void add(final double[] lhs, final double[] rhs, final double[] result);
  
  /**
   * Like
   * {@link org.apache.commons.math3.analysis.differentiation.DSCompiler#sub(double[], int, double[], int, double[], int)}
   * but with constant offset "0".
   * 
   * @param lhs
   *          the lhs
   * @param rhs
   *          the rhs
   * @param result
   *          the result
   */
  void subtract(final double[] lhs, final double[] rhs, final double[] result);
  
  /**
   * Like
   * {@link org.apache.commons.math3.analysis.differentiation.DSCompiler#multiply(double[], int, double[], int, double[], int)}
   * but with constant offset "0".
   * 
   * @param lhs
   *          the lhs
   * @param rhs
   *          the rhs
   * @param result
   *          the result
   */
  void multiply(final double[] lhs, final double[] rhs, final double[] result);
  
  /**
   * Like
   * {@link org.apache.commons.math3.analysis.differentiation.DSCompiler#dicide(double[], int, double[], int, double[], int)}
   * but with constant offset "0".
   * 
   * @param lhs
   *          the lhs
   * @param rhs
   *          the rhs
   * @param result
   *          the result
   */
  void divide(final double[] lhs, final double[] rhs, final double[] result);
  
  /**
   * Like
   * {@link org.apache.commons.math3.analysis.differentiation.DSCompiler#compose(double[], int, double[], double[], int)}
   * but with constant offset "0".
   * 
   * @param operand
   *          the operand
   * @param f
   *          the f
   * @param result
   *          the result
   */
  void compose(final double[] operand, final double[] f, final double[] result);
  
  /**
   * Like {@link org.apache.commons.math3.analysis.differentiation.DSCompiler#pow(double[], int, double, double[], int)}
   * but with constant offset "0".
   * 
   * @param operand
   *          the operand
   * @param p
   *          the p
   * @param result
   *          the result
   */
  void pow(final double[] operand, final double p, final double[] result);
  
  /**
   * Like {@link org.apache.commons.math3.analysis.differentiation.DSCompiler#pow(double[], int, int, double[], int)}
   * but with constant offset "0".
   * 
   * @param operand
   *          the operand
   * @param n
   *          the n
   * @param result
   *          the result
   */
  void pow(final double[] operand, final int n, final double[] result);
  
  /**
   * Like
   * {@link org.apache.commons.math3.analysis.differentiation.DSCompiler# pow(double[], int, double[], int, double[], int)}
   * but with constant offset "0".
   * 
   * @param x
   *          the x
   * @param y
   *          the y
   * @param result
   *          the result
   */
  void pow(final double[] x, final double[] y, final double[] result);
  
/**
   * Like {@link org.apache.commons.math3.analysis.differentiation.DSCompiler#rootN(double[], int, int, double[], int) but with constant offset "0".
   *
   * @param operand the operand
   * @param n the n
   * @param result the result
   */
  void rootN(final double[] operand, final int n, final double[] result);
  
  /**
   * Like {@link org.apache.commons.math3.analysis.differentiation.DSCompiler#exp(double[], int, double[], int)} but
   * with constant offset "0".
   * 
   * @param operand
   *          the operand
   * @param result
   *          the result
   */
  void exp(final double[] operand, final double[] result);
  
  /**
   * Like {@link org.apache.commons.math3.analysis.differentiation.DSCompiler#expm1(double[], int, double[], int)} but
   * with constant offset "0".
   * 
   * @param operand
   *          the operand
   * @param result
   *          the result
   */
  void expm1(final double[] operand, final double[] result);
  
  /**
   * Like {@link org.apache.commons.math3.analysis.differentiation.DSCompiler#log(double[], int, double[], int)} but
   * with constant offset "0".
   * 
   * @param operand
   *          the operand
   * @param result
   *          the result
   */
  void log(final double[] operand, final double[] result);
  
  /**
   * Like {@link org.apache.commons.math3.analysis.differentiation.DSCompiler#log1p(double[], int, double[], int)} but
   * with constant offset "0".
   * 
   * @param operand
   *          the operand
   * @param result
   *          the result
   */
  void log1p(final double[] operand, final double[] result);
  
  /**
   * Like {@link org.apache.commons.math3.analysis.differentiation.DSCompiler#log10(double[], int, double[], int)} but
   * with constant offset "0".
   * 
   * @param operand
   *          the operand
   * @param result
   *          the result
   */
  void log10(final double[] operand, final double[] result);
  
  /**
   * Like {@link org.apache.commons.math3.analysis.differentiation.DSCompiler#cos(double[], int, double[], int)} but wis
   * constant offset "0".
   * 
   * @param operand
   *          the operand
   * @param result
   *          the result
   */
  void cos(final double[] operand, final double[] result);
  
  /**
   * Like {@link org.apache.commons.math3.analysis.differentiation.DSCompiler#sin(double[], int, double[], int)} but
   * with constant offset "0".
   * 
   * @param operand
   *          the operand
   * @param result
   *          the result
   */
  void sin(final double[] operand, final double[] result);
  
  /**
   * Like {@link org.apache.commons.math3.analysis.differentiation.DSCompiler#tan(double[], int, double[], int)} but
   * with constant offset "0".
   * 
   * @param operand
   *          the operand
   * @param result
   *          the result
   */
  void tan(final double[] operand, final double[] result);
  
  /**
   * Like {@link org.apache.commons.math3.analysis.differentiation.DSCompiler#acos(double[], int, double[], int)} but
   * with constant offset "0".
   * 
   * @param operand
   *          the operand
   * @param result
   *          the result
   */
  void acos(final double[] operand, final double[] result);
  
  /**
   * Like {@link org.apache.commons.math3.analysis.differentiation.DSCompiler#asin(double[], int, double[], int)} but
   * with constant offset "0".
   * 
   * @param operand
   *          the operand
   * @param result
   *          the result
   */
  void asin(final double[] operand, final double[] result);
  
/**
   * Like {@link org.apache.commons.math3.analysis.differentiation.DSCompiler#atan(double[], int, double[], int) but with constant offset "0".
   *
   * @param operand the operand
   * @param result the result
   */
  void atan(final double[] operand, final double[] result);
  
  /**
   * Like
   * {@link org.apache.commons.math3.analysis.differentiation.DSCompiler#atan2(double[], int, double[], int, double[], int)}
   * but with constant offset "0".
   * 
   * @param y
   *          the y
   * @param x
   *          the x
   * @param result
   *          the result
   */
  void atan2(final double[] y, final double[] x, final double[] result);
  
  /**
   * Like {@link org.apache.commons.math3.analysis.differentiation.DSCompiler#cosh(double[], int, double[], int)} nut
   * with constant offset "0".
   * 
   * @param operand
   *          the operand
   * @param result
   *          the result
   */
  void cosh(final double[] operand, final double[] result);
  
  /**
   * Like {@link org.apache.commons.math3.analysis.differentiation.DSCompiler#sinh(double[], int, double[], int)} but
   * with constant offset "0".
   * 
   * @param operand
   *          the operand
   * @param result
   *          the result
   */
  void sinh(final double[] operand, final double[] result);
  
  /**
   * Like {@link org.apache.commons.math3.analysis.differentiation.DSCompiler#tanh(double[], int, double[], int)} but
   * with constant offset "0".
   * 
   * @param operand
   *          the operand
   * @param result
   *          the result
   */
  void tanh(final double[] operand, final double[] result);
  
  /**
   * Like {@link org.apache.commons.math3.analysis.differentiation.DSCompiler#acosh(double[], int, double[], int)} but
   * with constant offset "0".
   * 
   * @param operand
   *          the operand
   * @param result
   *          the result
   */
  void acosh(final double[] operand, final double[] result);
  
  /**
   * Like {@link org.apache.commons.math3.analysis.differentiation.DSCompiler#asinh(double[], int, double[], int)} but
   * with constant offset "0".
   * 
   * @param operand
   *          the operand
   * @param result
   *          the result
   */
  void asinh(final double[] operand, final double[] result);
  
  /**
   * Like {@link org.apache.commons.math3.analysis.differentiation.DSCompiler#atanh(double[], int, double[], int)} but
   * with constant offset "0".
   * 
   * @param operand
   *          the operand
   * @param result
   *          the result
   */
  void atanh(final double[] operand, final double[] result);
  
  /**
   * Like {@link org.apache.commons.math3.analysis.differentiation.DSCompiler#taylor(double[], int, double...)} but with
   * constant offset "0".
   * 
   * @param ds
   *          the ds
   * @param delta
   *          the delta
   * @return the double
   */
  double taylor(final double[] ds, final double... delta);
  
  /**
   * Like
   * {@link org.apache.commons.math3.analysis.differentiation.DSCompiler#linearCombination(double, double[], int, double, double[], int, double[], int)}
   * but with constant
   * offset "0".
   * 
   * @param a1
   *          the a1
   * @param c1
   *          the c1
   * @param a2
   *          the a2
   * @param c2
   *          the c2
   * @param result
   *          the result
   */
  void linearCombination(final double a1, final double[] c1, final double a2, final double[] c2, final double[] result);
  
  /**
   * Like
   * {@link org.apache.commons.math3.analysis.differentiation.DSCompiler#linearCombination(double, double[], int, double, double[], int, double, double[], int, double[], int)}
   * but with constant offset "0".
   * 
   * @param a1
   *          the a1
   * @param c1
   *          the c1
   * @param a2
   *          the a2
   * @param c2
   *          the c2
   * @param a3
   *          the a3
   * @param c3
   *          the c3
   * @param result
   *          the result
   */
  void linearCombination(final double a1, final double[] c1, final double a2, final double[] c2, final double a3,
      final double[] c3, final double[] result);
  
  /**
   * Like.
   * 
   * @param a1
   *          the a1
   * @param c1
   *          the c1
   * @param a2
   *          the a2
   * @param c2
   *          the c2
   * @param a3
   *          the a3
   * @param c3
   *          the c3
   * @param a4
   *          the a4
   * @param c4
   *          the c4
   * @param result
   *          the result
   *          {@link #linearCombination(double, double[], int, double, double[], int, double, double[], int, double, double[], int, double[], int)}
   *          but with constant offset "0".
   */
  void linearCombination(final double a1, final double[] c1, final double a2, final double[] c2, final double a3,
      final double[] c3, final double a4, final double[] c4, final double[] result);
  
  /**
   * Like
   * {@link org.apache.commons.math3.analysis.differentiation.DSCompiler#remainder(double[], int, double[], int, double[], int)}
   * but with constant offset "0".
   * 
   * @param lhs
   *          the lhs
   * @param rhs
   *          the rhs
   * @param result
   *          the result
   */
  void remainder(final double[] lhs, final double[] rhs, final double[] result);
  
  /**
   * Gets the sizes.
   * 
   * @return the sizes
   */
  int[][] getSizes();
  
  /**
   * Gets the derivatives indirection.
   * 
   * @return the derivatives indirection
   */
  int[][] getDerivativesIndirection();
  
  /**
   * Gets the lower indirection.
   * 
   * @return the lower indirection
   */
  int[] getLowerIndirection();
  
  /**
   * Gets the mult indirection.
   * 
   * @return the mult indirection
   */
  int[][][] getMultIndirection();
  
  /**
   * Gets the comp indirection.
   * 
   * @return the comp indirection
   */
  int[][][] getCompIndirection();
  
}
