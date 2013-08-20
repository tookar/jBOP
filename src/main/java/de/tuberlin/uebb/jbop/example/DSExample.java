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
    final DerivativeStructure x = new DerivativeStructure(5, 5, 0, x0);
    final double a = 2.0;
    final double b = 3.5;
    final double c = 4.0;
    // ax²
    final DerivativeStructure ax2 = x.pow(2).multiply(a);//
    // + bx
    final DerivativeStructure bx = x.add(b);
    // +c
    final DerivativeStructure fx = ax2.add(bx).add(c);
    
    System.out.println("f   (x) =" + fx.getValue());
    System.out.println("f'  (x) =" + fx.getPartialDerivative(1, 0, 0, 0, 0));
    System.out.println("f'' (x) =" + fx.getPartialDerivative(2, 0, 0, 0, 0));
    System.out.println("f'''(x) =" + fx.getPartialDerivative(3, 0, 0, 0, 0));
  }
}
