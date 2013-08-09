/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tuberlin.uebb.jbop.math;

import java.util.Arrays;

import org.apache.commons.math3.exception.DimensionMismatchException;
import org.apache.commons.math3.exception.NumberIsTooLargeException;
import org.apache.commons.math3.util.ArithmeticUtils;
import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.util.MathArrays;

import de.tuberlin.uebb.jbop.optimizer.annotations.ImmutableArray;
import de.tuberlin.uebb.jbop.optimizer.annotations.Optimizable;
import de.tuberlin.uebb.jbop.optimizer.annotations.StrictLoops;

/**
 * This is a modified copy of {@link org.apache.commons.math3.analysis.differentiation.DSCompiler}.
 * For information about the logic see the Javadoc of
 * {@link org.apache.commons.math3.analysis.differentiation.DSCompiler}.
 * 
 * The Creation of the compilers and the cache are refactored to {@link DSCompilerFactory}.
 * 
 * All the public api of this class was lifted to the interface {@link IDSCompiler}.
 */
public class DSCompiler implements IDSCompiler {
  
  /** Number of free parameters. */
  final int parameters;
  
  /** Derivation order. */
  final int order;
  
  /** Number of partial derivatives (including the single 0 order derivative element). */
  @ImmutableArray
  final int[][] sizes;
  
  /** Indirection array for partial derivatives. */
  @ImmutableArray
  final int[][] derivativesIndirection;
  
  /** Indirection array of the lower derivative elements. */
  @ImmutableArray
  final int[] lowerIndirection;
  
  /** Indirection arrays for multiplication. */
  @ImmutableArray
  final int[][][] multIndirection;
  
  /** Indirection arrays for function composition. */
  @ImmutableArray
  final int[][][] compIndirection;
  
  /**
   * public for simpler access via reflection.
   * Do not use as standalone.
   */
  public DSCompiler(final int parameters, final int order, final int[][] sizes, final int[][] derivativesIndirection,
      final int[] lowerIndirection, final int[][][] multIndirection, final int[][][] compIndirection) {
    this.parameters = parameters;
    this.order = order;
    this.sizes = sizes;
    this.derivativesIndirection = derivativesIndirection;
    this.lowerIndirection = lowerIndirection;
    this.multIndirection = multIndirection;
    this.compIndirection = compIndirection;
    
  }
  
  @Override
  @Optimizable
  @StrictLoops
  public int getPartialDerivativeIndex(final int... orders) throws DimensionMismatchException,
      NumberIsTooLargeException {
    
    // safety check
    if (orders.length != parameters) {
      throw new DimensionMismatchException(orders.length, parameters);
    }
    
    return DSCompilerFactory.getPartialDerivativeIndex(parameters, order, sizes, orders);
    
  }
  
  @Override
  public int[] getPartialDerivativeOrders(final int index) {
    return derivativesIndirection[index];
  }
  
  @Override
  public int getFreeParameters() {
    return parameters;
  }
  
  /**
   * Get the derivation order.
   * 
   * @return derivation order
   */
  @Override
  public int getOrder() {
    return order;
  }
  
  @Override
  @Optimizable
  public int getSize() {
    return sizes[parameters][order];
  }
  
  @Override
  @Optimizable
  @StrictLoops
  public void linearCombination(final double a1, final double[] c1, final double a2, final double[] c2,
      final double[] result) {
    for (int i = 0; i < sizes[parameters][order]; ++i) {
      result[i] = MathArrays.linearCombination(a1, c1[i], a2, c2[i]);
    }
  }
  
  @Override
  @Optimizable
  @StrictLoops
  public void linearCombination(final double a1, final double[] c1, final double a2, final double[] c2,
      final double a3, final double[] c3, final double[] result) {
    for (int i = 0; i < sizes[parameters][order]; ++i) {
      result[i] = MathArrays.linearCombination(a1, c1[i], a2, c2[i], a3, c3[i]);
    }
  }
  
  @Override
  @Optimizable
  @StrictLoops
  public void linearCombination(final double a1, final double[] c1, final double a2, final double[] c2,
      final double a3, final double[] c3, final double a4, final double[] c4, final double[] result) {
    for (int i = 0; i < sizes[parameters][order]; ++i) {
      result[i] = MathArrays.linearCombination(a1, c1[i], a2, c2[i], a3, c3[i], a4, c4[i]);
    }
  }
  
  @Override
  @Optimizable
  @StrictLoops
  public void add(final double[] lhs, final double[] rhs, final double[] result) {
    for (int i = 0; i < sizes[parameters][order]; ++i) {
      result[i] = lhs[i] + rhs[i];
    }
  }
  
  @Override
  @Optimizable
  @StrictLoops
  public void subtract(final double[] lhs, final double[] rhs, final double[] result) {
    for (int i = 0; i < sizes[parameters][order]; ++i) {
      result[i] = lhs[i] - rhs[i];
    }
  }
  
  @Override
  @Optimizable
  @StrictLoops
  public void multiply(final double[] lhs, final double[] rhs, final double[] result) {
    for (int i = 0; i < multIndirection.length; ++i) {
      final int[][] mappingI = multIndirection[i];
      double r = 0;
      for (int j = 0; j < mappingI.length; ++j) {
        r += mappingI[j][0] * lhs[mappingI[j][1]] * rhs[mappingI[j][2]];
      }
      result[i] = r;
    }
  }
  
  @Override
  @Optimizable
  @StrictLoops
  public void divide(final double[] lhs, final double[] rhs, final double[] result) {
    final double[] reciprocal = new double[sizes[parameters][order]];
    pow(rhs, -1, reciprocal);
    multiply(lhs, reciprocal, result);
  }
  
  @Override
  @Optimizable
  @StrictLoops
  public void remainder(final double[] lhs, final double[] rhs, final double[] result) {
    
    // compute k such that lhs % rhs = lhs - k rhs
    final double rem = lhs[0] % rhs[0];
    final double k = FastMath.rint((lhs[0] - rem) / rhs[0]);
    
    // set up value
    result[0] = rem;
    
    // set up partial derivatives
    for (int i = 1; i < sizes[parameters][order]; ++i) {
      result[i] = lhs[i] - (k * rhs[i]);
    }
    
  }
  
  @Override
  @Optimizable
  @StrictLoops
  public void pow(final double[] operand, final double p, final double[] result) {
    
    // create the function value and derivatives
    // [x^p, px^(p-1), p(p-1)x^(p-2), ... ]
    final double[] function = new double[1 + order];
    double xk = FastMath.pow(operand[0], p - order);
    for (int i = order; i > 0; --i) {
      function[i] = xk;
      xk *= operand[0];
    }
    function[0] = xk;
    double coefficient = p;
    for (int i = 1; i <= order; ++i) {
      function[i] *= coefficient;
      coefficient *= p - i;
    }
    
    // apply function composition
    compose(operand, function, result);
    
  }
  
  @Override
  @Optimizable
  @StrictLoops
  public void pow(final double[] operand, final int n, final double[] result) {
    
    if (n == 0) {
      // special case, x^0 = 1 for all x
      result[0] = 1.0;
      Arrays.fill(result, 1, sizes[parameters][order], 0);
      return;
    }
    
    // create the power function value and derivatives
    // [x^n, nx^(n-1), n(n-1)x^(n-2), ... ]
    final double[] function = new double[1 + order];
    
    if (n > 0) {
      // strictly positive power
      final int maxOrder = FastMath.min(order, n);
      double xk = FastMath.pow(operand[0], n - maxOrder);
      for (int i = maxOrder; i > 0; --i) {
        function[i] = xk;
        xk *= operand[0];
      }
      function[0] = xk;
    } else {
      // strictly negative power
      final double inv = 1.0 / operand[0];
      double xk = FastMath.pow(inv, -n);
      for (int i = 0; i <= order; ++i) {
        function[i] = xk;
        xk *= inv;
      }
    }
    
    double coefficient = n;
    for (int i = 1; i <= order; ++i) {
      function[i] *= coefficient;
      coefficient *= n - i;
    }
    
    // apply function composition
    compose(operand, function, result);
    
  }
  
  @Override
  @Optimizable
  @StrictLoops
  public void pow(final double[] x, final double[] y, final double[] result) {
    final double[] logX = new double[sizes[parameters][order]];
    log(x, logX);
    final double[] yLogX = new double[sizes[parameters][order]];
    multiply(logX, y, yLogX);
    exp(yLogX, result);
  }
  
  @Override
  @Optimizable
  @StrictLoops
  public void rootN(final double[] operand, final int n, final double[] result) {
    
    // create the function value and derivatives
    // [x^(1/n), (1/n)x^((1/n)-1), (1-n)/n^2x^((1/n)-2), ... ]
    final double[] function = new double[1 + order];
    double xk;
    if (n == 2) {
      function[0] = FastMath.sqrt(operand[0]);
      xk = 0.5 / function[0];
    } else if (n == 3) {
      function[0] = FastMath.cbrt(operand[0]);
      xk = 1.0 / (3.0 * function[0] * function[0]);
    } else {
      function[0] = FastMath.pow(operand[0], 1.0 / n);
      xk = 1.0 / (n * FastMath.pow(function[0], n - 1));
    }
    final double nReciprocal = 1.0 / n;
    final double xReciprocal = 1.0 / operand[0];
    for (int i = 1; i <= order; ++i) {
      function[i] = xk;
      xk *= xReciprocal * (nReciprocal - i);
    }
    
    // apply function composition
    compose(operand, function, result);
    
  }
  
  @Override
  @Optimizable
  @StrictLoops
  public void exp(final double[] operand, final double[] result) {
    
    // create the function value and derivatives
    final double[] function = new double[1 + order];
    Arrays.fill(function, FastMath.exp(operand[0]));
    
    // apply function composition
    compose(operand, function, result);
    
  }
  
  @Override
  @Optimizable
  @StrictLoops
  public void expm1(final double[] operand, final double[] result) {
    
    // create the function value and derivatives
    final double[] function = new double[1 + order];
    function[0] = FastMath.expm1(operand[0]);
    Arrays.fill(function, 1, 1 + order, FastMath.exp(operand[0]));
    
    // apply function composition
    compose(operand, function, result);
    
  }
  
  @Override
  @Optimizable
  @StrictLoops
  public void log(final double[] operand, final double[] result) {
    
    // create the function value and derivatives
    final double[] function = new double[1 + order];
    function[0] = FastMath.log(operand[0]);
    if (order > 0) {
      final double inv = 1.0 / operand[0];
      double xk = inv;
      for (int i = 1; i <= order; ++i) {
        function[i] = xk;
        xk *= -i * inv;
      }
    }
    
    // apply function composition
    compose(operand, function, result);
    
  }
  
  @Override
  @Optimizable
  @StrictLoops
  public void log1p(final double[] operand, final double[] result) {
    
    // create the function value and derivatives
    final double[] function = new double[1 + order];
    function[0] = FastMath.log1p(operand[0]);
    if (order > 0) {
      final double inv = 1.0 / (1.0 + operand[0]);
      double xk = inv;
      for (int i = 1; i <= order; ++i) {
        function[i] = xk;
        xk *= -i * inv;
      }
    }
    
    // apply function composition
    compose(operand, function, result);
    
  }
  
  @Override
  @Optimizable
  @StrictLoops
  public void log10(final double[] operand, final double[] result) {
    
    // create the function value and derivatives
    final double[] function = new double[1 + order];
    function[0] = FastMath.log10(operand[0]);
    if (order > 0) {
      final double inv = 1.0 / operand[0];
      double xk = inv / FastMath.log(10.0);
      for (int i = 1; i <= order; ++i) {
        function[i] = xk;
        xk *= -i * inv;
      }
    }
    
    // apply function composition
    compose(operand, function, result);
    
  }
  
  @Override
  @Optimizable
  @StrictLoops
  public void cos(final double[] operand, final double[] result) {
    
    // create the function value and derivatives
    final double[] function = new double[1 + order];
    function[0] = FastMath.cos(operand[0]);
    if (order > 0) {
      function[1] = -FastMath.sin(operand[0]);
      for (int i = 2; i <= order; ++i) {
        function[i] = -function[i - 2];
      }
    }
    
    // apply function composition
    compose(operand, function, result);
    
  }
  
  @Override
  @Optimizable
  @StrictLoops
  public void sin(final double[] operand, final double[] result) {
    
    // create the function value and derivatives
    final double[] function = new double[1 + order];
    function[0] = FastMath.sin(operand[0]);
    if (order > 0) {
      function[1] = FastMath.cos(operand[0]);
      for (int i = 2; i <= order; ++i) {
        function[i] = -function[i - 2];
      }
    }
    
    // apply function composition
    compose(operand, function, result);
    
  }
  
  @Override
  @Optimizable
  @StrictLoops
  public void tan(final double[] operand, final double[] result) {
    
    // create the function value and derivatives
    final double[] function = new double[1 + order];
    final double t = FastMath.tan(operand[0]);
    function[0] = t;
    
    if (order > 0) {
      
      // the nth order derivative of tan has the form:
      // dn(tan(x)/dxn = P_n(tan(x))
      // where P_n(t) is a degree n+1 polynomial with same parity as n+1
      // P_0(t) = t, P_1(t) = 1 + t^2, P_2(t) = 2 t (1 + t^2) ...
      // the general recurrence relation for P_n is:
      // P_n(x) = (1+t^2) P_(n-1)'(t)
      // as per polynomial parity, we can store coefficients of both P_(n-1) and P_n in the same array
      final double[] p = new double[order + 2];
      p[1] = 1;
      final double t2 = t * t;
      for (int n = 1; n <= order; ++n) {
        
        // update and evaluate polynomial P_n(t)
        double v = 0;
        p[n + 1] = n * p[n];
        for (int k = n + 1; k >= 0; k -= 2) {
          v = (v * t2) + p[k];
          if (k > 2) {
            p[k - 2] = ((k - 1) * p[k - 1]) + ((k - 3) * p[k - 3]);
          } else if (k == 2) {
            p[0] = p[1];
          }
        }
        if ((n & 0x1) == 0) {
          v *= t;
        }
        
        function[n] = v;
        
      }
    }
    
    // apply function composition
    compose(operand, function, result);
  }
  
  @Override
  @Optimizable
  @StrictLoops
  public void acos(final double[] operand, final double[] result) {
    
    // create the function value and derivatives
    final double[] function = new double[1 + order];
    final double x = operand[0];
    function[0] = FastMath.acos(x);
    if (order > 0) {
      // the nth order derivative of acos has the form:
      // dn(acos(x)/dxn = P_n(x) / [1 - x^2]^((2n-1)/2)
      // where P_n(x) is a degree n-1 polynomial with same parity as n-1
      // P_1(x) = -1, P_2(x) = -x, P_3(x) = -2x^2 - 1 ...
      // the general recurrence relation for P_n is:
      // P_n(x) = (1-x^2) P_(n-1)'(x) + (2n-3) x P_(n-1)(x)
      // as per polynomial parity, we can store coefficients of both P_(n-1) and P_n in the same array
      final double[] p = new double[order];
      p[0] = -1;
      final double x2 = x * x;
      final double f = 1.0 / (1 - x2);
      double coeff = FastMath.sqrt(f);
      function[1] = coeff * p[0];
      for (int n = 2; n <= order; ++n) {
        
        // update and evaluate polynomial P_n(x)
        double v = 0;
        p[n - 1] = (n - 1) * p[n - 2];
        for (int k = n - 1; k >= 0; k -= 2) {
          v = (v * x2) + p[k];
          if (k > 2) {
            p[k - 2] = ((k - 1) * p[k - 1]) + (((2 * n) - k) * p[k - 3]);
          } else if (k == 2) {
            p[0] = p[1];
          }
        }
        if ((n & 0x1) == 0) {
          v *= x;
        }
        
        coeff *= f;
        function[n] = coeff * v;
        
      }
    }
    
    // apply function composition
    compose(operand, function, result);
    
  }
  
  @Override
  @Optimizable
  @StrictLoops
  public void asin(final double[] operand, final double[] result) {
    
    // create the function value and derivatives
    final double[] function = new double[1 + order];
    final double x = operand[0];
    function[0] = FastMath.asin(x);
    if (order > 0) {
      // the nth order derivative of asin has the form:
      // dn(asin(x)/dxn = P_n(x) / [1 - x^2]^((2n-1)/2)
      // where P_n(x) is a degree n-1 polynomial with same parity as n-1
      // P_1(x) = 1, P_2(x) = x, P_3(x) = 2x^2 + 1 ...
      // the general recurrence relation for P_n is:
      // P_n(x) = (1-x^2) P_(n-1)'(x) + (2n-3) x P_(n-1)(x)
      // as per polynomial parity, we can store coefficients of both P_(n-1) and P_n in the same array
      final double[] p = new double[order];
      p[0] = 1;
      final double x2 = x * x;
      final double f = 1.0 / (1 - x2);
      double coeff = FastMath.sqrt(f);
      function[1] = coeff * p[0];
      for (int n = 2; n <= order; ++n) {
        
        // update and evaluate polynomial P_n(x)
        double v = 0;
        p[n - 1] = (n - 1) * p[n - 2];
        for (int k = n - 1; k >= 0; k -= 2) {
          v = (v * x2) + p[k];
          if (k > 2) {
            p[k - 2] = ((k - 1) * p[k - 1]) + (((2 * n) - k) * p[k - 3]);
          } else if (k == 2) {
            p[0] = p[1];
          }
        }
        if ((n & 0x1) == 0) {
          v *= x;
        }
        
        coeff *= f;
        function[n] = coeff * v;
        
      }
    }
    
    // apply function composition
    compose(operand, function, result);
    
  }
  
  @Override
  @Optimizable
  @StrictLoops
  public void atan(final double[] operand, final double[] result) {
    
    // create the function value and derivatives
    final double[] function = new double[1 + order];
    final double x = operand[0];
    function[0] = FastMath.atan(x);
    if (order > 0) {
      // the nth order derivative of atan has the form:
      // dn(atan(x)/dxn = Q_n(x) / (1 + x^2)^n
      // where Q_n(x) is a degree n-1 polynomial with same parity as n-1
      // Q_1(x) = 1, Q_2(x) = -2x, Q_3(x) = 6x^2 - 2 ...
      // the general recurrence relation for Q_n is:
      // Q_n(x) = (1+x^2) Q_(n-1)'(x) - 2(n-1) x Q_(n-1)(x)
      // as per polynomial parity, we can store coefficients of both Q_(n-1) and Q_n in the same array
      final double[] q = new double[order];
      q[0] = 1;
      final double x2 = x * x;
      final double f = 1.0 / (1 + x2);
      double coeff = f;
      function[1] = coeff * q[0];
      for (int n = 2; n <= order; ++n) {
        
        // update and evaluate polynomial Q_n(x)
        double v = 0;
        q[n - 1] = -n * q[n - 2];
        for (int k = n - 1; k >= 0; k -= 2) {
          v = (v * x2) + q[k];
          if (k > 2) {
            q[k - 2] = ((k - 1) * q[k - 1]) + ((k - 1 - (2 * n)) * q[k - 3]);
          } else if (k == 2) {
            q[0] = q[1];
          }
        }
        if ((n & 0x1) == 0) {
          v *= x;
        }
        
        coeff *= f;
        function[n] = coeff * v;
        
      }
    }
    
    // apply function composition
    compose(operand, function, result);
    
  }
  
  @Override
  @Optimizable
  @StrictLoops
  public void atan2(final double[] y, final double[] x, final double[] result) {
    
    // compute r = sqrt(x^2+y^2)
    final double[] tmp1 = new double[sizes[parameters][order]];
    multiply(x, x, tmp1);      // x^2
    final double[] tmp2 = new double[sizes[parameters][order]];
    multiply(y, y, tmp2);      // y^2
    add(tmp1, tmp2, tmp2);                 // x^2 + y^2
    rootN(tmp2, 2, tmp1);                     // r = sqrt(x^2 + y^2)
    
    if (x[0] >= 0) {
      
      // compute atan2(y, x) = 2 atan(y / (r + x))
      add(tmp1, x, tmp2);          // r + x
      divide(y, tmp2, tmp1);       // y /(r + x)
      atan(tmp1, tmp2);                     // atan(y / (r + x))
      for (int i = 0; i < tmp2.length; ++i) {
        result[i] = 2 * tmp2[i]; // 2 * atan(y / (r + x))
      }
      
    } else {
      
      // compute atan2(y, x) = +/- pi - 2 atan(y / (r - x))
      subtract(tmp1, x, tmp2);     // r - x
      divide(y, tmp2, tmp1);       // y /(r - x)
      atan(tmp1, tmp2);                     // atan(y / (r - x))
      result[0] = ((tmp2[0] <= 0) ? -FastMath.PI : FastMath.PI) - (2 * tmp2[0]); // +/-pi - 2 * atan(y / (r -
                                                                                 // x))
      for (int i = 1; i < tmp2.length; ++i) {
        result[i] = -2 * tmp2[i]; // +/-pi - 2 * atan(y / (r - x))
      }
      
    }
    
  }
  
  @Override
  @Optimizable
  @StrictLoops
  public void cosh(final double[] operand, final double[] result) {
    
    // create the function value and derivatives
    final double[] function = new double[1 + order];
    function[0] = FastMath.cosh(operand[0]);
    if (order > 0) {
      function[1] = FastMath.sinh(operand[0]);
      for (int i = 2; i <= order; ++i) {
        function[i] = function[i - 2];
      }
    }
    
    // apply function composition
    compose(operand, function, result);
    
  }
  
  @Override
  @Optimizable
  @StrictLoops
  public void sinh(final double[] operand, final double[] result) {
    
    // create the function value and derivatives
    final double[] function = new double[1 + order];
    function[0] = FastMath.sinh(operand[0]);
    if (order > 0) {
      function[1] = FastMath.cosh(operand[0]);
      for (int i = 2; i <= order; ++i) {
        function[i] = function[i - 2];
      }
    }
    
    // apply function composition
    compose(operand, function, result);
    
  }
  
  @Override
  @Optimizable
  @StrictLoops
  public void tanh(final double[] operand, final double[] result) {
    
    // create the function value and derivatives
    final double[] function = new double[1 + order];
    final double t = FastMath.tanh(operand[0]);
    function[0] = t;
    
    if (order > 0) {
      
      // the nth order derivative of tanh has the form:
      // dn(tanh(x)/dxn = P_n(tanh(x))
      // where P_n(t) is a degree n+1 polynomial with same parity as n+1
      // P_0(t) = t, P_1(t) = 1 - t^2, P_2(t) = -2 t (1 - t^2) ...
      // the general recurrence relation for P_n is:
      // P_n(x) = (1-t^2) P_(n-1)'(t)
      // as per polynomial parity, we can store coefficients of both P_(n-1) and P_n in the same array
      final double[] p = new double[order + 2];
      p[1] = 1;
      final double t2 = t * t;
      for (int n = 1; n <= order; ++n) {
        
        // update and evaluate polynomial P_n(t)
        double v = 0;
        p[n + 1] = -n * p[n];
        for (int k = n + 1; k >= 0; k -= 2) {
          v = (v * t2) + p[k];
          if (k > 2) {
            p[k - 2] = ((k - 1) * p[k - 1]) - ((k - 3) * p[k - 3]);
          } else if (k == 2) {
            p[0] = p[1];
          }
        }
        if ((n & 0x1) == 0) {
          v *= t;
        }
        
        function[n] = v;
        
      }
    }
    
    // apply function composition
    compose(operand, function, result);
    
  }
  
  @Override
  @Optimizable
  @StrictLoops
  public void acosh(final double[] operand, final double[] result) {
    
    // create the function value and derivatives
    final double[] function = new double[1 + order];
    final double x = operand[0];
    function[0] = FastMath.acosh(x);
    if (order > 0) {
      // the nth order derivative of acosh has the form:
      // dn(acosh(x)/dxn = P_n(x) / [x^2 - 1]^((2n-1)/2)
      // where P_n(x) is a degree n-1 polynomial with same parity as n-1
      // P_1(x) = 1, P_2(x) = -x, P_3(x) = 2x^2 + 1 ...
      // the general recurrence relation for P_n is:
      // P_n(x) = (x^2-1) P_(n-1)'(x) - (2n-3) x P_(n-1)(x)
      // as per polynomial parity, we can store coefficients of both P_(n-1) and P_n in the same array
      final double[] p = new double[order];
      p[0] = 1;
      final double x2 = x * x;
      final double f = 1.0 / (x2 - 1);
      double coeff = FastMath.sqrt(f);
      function[1] = coeff * p[0];
      for (int n = 2; n <= order; ++n) {
        
        // update and evaluate polynomial P_n(x)
        double v = 0;
        p[n - 1] = (1 - n) * p[n - 2];
        for (int k = n - 1; k >= 0; k -= 2) {
          v = (v * x2) + p[k];
          if (k > 2) {
            p[k - 2] = ((1 - k) * p[k - 1]) + ((k - (2 * n)) * p[k - 3]);
          } else if (k == 2) {
            p[0] = -p[1];
          }
        }
        if ((n & 0x1) == 0) {
          v *= x;
        }
        
        coeff *= f;
        function[n] = coeff * v;
        
      }
    }
    
    // apply function composition
    compose(operand, function, result);
    
  }
  
  @Override
  @Optimizable
  @StrictLoops
  public void asinh(final double[] operand, final double[] result) {
    
    // create the function value and derivatives
    final double[] function = new double[1 + order];
    final double x = operand[0];
    function[0] = FastMath.asinh(x);
    if (order > 0) {
      // the nth order derivative of asinh has the form:
      // dn(asinh(x)/dxn = P_n(x) / [x^2 + 1]^((2n-1)/2)
      // where P_n(x) is a degree n-1 polynomial with same parity as n-1
      // P_1(x) = 1, P_2(x) = -x, P_3(x) = 2x^2 - 1 ...
      // the general recurrence relation for P_n is:
      // P_n(x) = (x^2+1) P_(n-1)'(x) - (2n-3) x P_(n-1)(x)
      // as per polynomial parity, we can store coefficients of both P_(n-1) and P_n in the same array
      final double[] p = new double[order];
      p[0] = 1;
      final double x2 = x * x;
      final double f = 1.0 / (1 + x2);
      double coeff = FastMath.sqrt(f);
      function[1] = coeff * p[0];
      for (int n = 2; n <= order; ++n) {
        
        // update and evaluate polynomial P_n(x)
        double v = 0;
        p[n - 1] = (1 - n) * p[n - 2];
        for (int k = n - 1; k >= 0; k -= 2) {
          v = (v * x2) + p[k];
          if (k > 2) {
            p[k - 2] = ((k - 1) * p[k - 1]) + ((k - (2 * n)) * p[k - 3]);
          } else if (k == 2) {
            p[0] = p[1];
          }
        }
        if ((n & 0x1) == 0) {
          v *= x;
        }
        
        coeff *= f;
        function[n] = coeff * v;
        
      }
    }
    
    // apply function composition
    compose(operand, function, result);
    
  }
  
  @Override
  @Optimizable
  @StrictLoops
  public void atanh(final double[] operand, final double[] result) {
    
    // create the function value and derivatives
    final double[] function = new double[1 + order];
    final double x = operand[0];
    function[0] = FastMath.atanh(x);
    if (order > 0) {
      // the nth order derivative of atanh has the form:
      // dn(atanh(x)/dxn = Q_n(x) / (1 - x^2)^n
      // where Q_n(x) is a degree n-1 polynomial with same parity as n-1
      // Q_1(x) = 1, Q_2(x) = 2x, Q_3(x) = 6x^2 + 2 ...
      // the general recurrence relation for Q_n is:
      // Q_n(x) = (1-x^2) Q_(n-1)'(x) + 2(n-1) x Q_(n-1)(x)
      // as per polynomial parity, we can store coefficients of both Q_(n-1) and Q_n in the same array
      final double[] q = new double[order];
      q[0] = 1;
      final double x2 = x * x;
      final double f = 1.0 / (1 - x2);
      double coeff = f;
      function[1] = coeff * q[0];
      for (int n = 2; n <= order; ++n) {
        
        // update and evaluate polynomial Q_n(x)
        double v = 0;
        q[n - 1] = n * q[n - 2];
        for (int k = n - 1; k >= 0; k -= 2) {
          v = (v * x2) + q[k];
          if (k > 2) {
            q[k - 2] = ((k - 1) * q[k - 1]) + ((((2 * n) - k) + 1) * q[k - 3]);
          } else if (k == 2) {
            q[0] = q[1];
          }
        }
        if ((n & 0x1) == 0) {
          v *= x;
        }
        
        coeff *= f;
        function[n] = coeff * v;
        
      }
    }
    
    // apply function composition
    compose(operand, function, result);
    
  }
  
  @Override
  @Optimizable
  @StrictLoops
  public void compose(final double[] operand, final double[] f, final double[] result) {
    for (int i = 0; i < compIndirection.length; ++i) {
      final int[][] mappingI = compIndirection[i];
      double r = 0;
      for (int j = 0; j < mappingI.length; ++j) {
        final int[] mappingIJ = mappingI[j];
        double product = mappingIJ[0] * f[mappingIJ[1]];
        for (int k = 2; k < mappingIJ.length; ++k) {
          product *= operand[mappingIJ[k]];
        }
        r += product;
      }
      result[i] = r;
    }
  }
  
  @Override
  @Optimizable
  @StrictLoops
  public double taylor(final double[] ds, final double... delta) {
    double value = 0;
    for (int i = sizes[parameters][order] - 1; i >= 0; --i) {
      final int[] orders = derivativesIndirection[i];
      double term = ds[i];
      for (int k = 0; k < orders.length; ++k) {
        if (orders[k] > 0) {
          term *= FastMath.pow(delta[k], orders[k]) / ArithmeticUtils.factorial(orders[k]);
        }
      }
      value += term;
    }
    return value;
  }
  
  @Override
  @Optimizable
  @StrictLoops
  public void checkCompatibility(final IDSCompiler compiler) throws DimensionMismatchException {
    if (parameters != compiler.getFreeParameters()) {
      throw new DimensionMismatchException(parameters, compiler.getFreeParameters());
    }
    if (order != compiler.getOrder()) {
      throw new DimensionMismatchException(order, compiler.getOrder());
    }
  }
  
  @Override
  @Optimizable
  @StrictLoops
  public int[][] getSizes() {
    return sizes;
  }
  
  @Override
  @Optimizable
  @StrictLoops
  public int[][] getDerivativesIndirection() {
    return derivativesIndirection;
  }
  
  @Override
  @Optimizable
  @StrictLoops
  public int[] getLowerIndirection() {
    return lowerIndirection;
  }
  
  @Override
  public int[][][] getMultIndirection() {
    return multIndirection;
  }
  
  @Override
  @Optimizable
  @StrictLoops
  public int[][][] getCompIndirection() {
    return compIndirection;
  }
  
}
