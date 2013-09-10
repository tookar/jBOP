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

/**
 * The Class DSExample.
 * 
 * @author Christopher Ewest
 */
public class DSExample {
  
  /**
   * The main method.
   * Example for Usage of jBOP.
   * 
   * @param args
   *          the arguments
   */
  public static void main(final String[] args) {
    final double x0 = 15.0;
    final DerivativeStructure x = new DerivativeStructure(3, 3, 0, x0);
    final double y0 = 15.0;
    final DerivativeStructure y = new DerivativeStructure(3, 3, 1, y0);
    final double z0 = 15.0;
    final DerivativeStructure z = new DerivativeStructure(3, 3, 2, z0);
    final double a = 2.0;
    final double b = 3.5;
    final double c = 4.0;
    // ax²
    final DerivativeStructure ax2 = x.pow(2).multiply(a);//
    // ay²
    final DerivativeStructure ay2 = y.pow(2).multiply(a);//
    // az²
    final DerivativeStructure az2 = z.pow(2).multiply(a);//
    // + bx
    final DerivativeStructure bx = x.multiply(b);
    // + by
    final DerivativeStructure by = y.multiply(b);
    // + bz
    final DerivativeStructure bz = z.multiply(b);
    // +c
    final DerivativeStructure fx = ax2.multiply(ay2).multiply(az2).add(bx.multiply(by).multiply(bz)).add(c);
    System.out.println("f(x,y,z)   = ax²ay²az²+bxbybz+c");
    System.out.println("a          = " + a);
    System.out.println("b          = " + b);
    System.out.println("c          = " + c);
    System.out.println("x          = " + x0);
    System.out.println("y          = " + y0);
    System.out.println("z          = " + z0);
    System.out.println("f          = " + fx.getValue());
    System.out.println("df/dx      = " + fx.getPartialDerivative(1, 0, 0));
    System.out.println("df2/dx2    = " + fx.getPartialDerivative(2, 0, 0));
    System.out.println("df3/dx3    = " + fx.getPartialDerivative(3, 0, 0));
    System.out.println("df/dy      = " + fx.getPartialDerivative(0, 1, 0));
    System.out.println("df2/dy2    = " + fx.getPartialDerivative(0, 2, 0));
    System.out.println("df3/dy3    = " + fx.getPartialDerivative(0, 3, 0));
    System.out.println("df/dz      = " + fx.getPartialDerivative(0, 0, 1));
    System.out.println("df2/dz2    = " + fx.getPartialDerivative(0, 0, 2));
    System.out.println("df3/dz3    = " + fx.getPartialDerivative(0, 0, 3));
    System.out.println("df/dxdydz  = " + fx.getPartialDerivative(1, 1, 1));
  }
}
