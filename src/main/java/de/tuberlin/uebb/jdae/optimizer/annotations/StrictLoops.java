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
package de.tuberlin.uebb.jdae.optimizer.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation marks a method, that holds only loops (if any)
 * that have
 * <ul>
 * <li>a strict counter.</li>
 * <li>at most one assigned variable (not arrays).</li>
 * </ul>
 * This means
 * <ul>
 * <li>the counter variable is increased exactly once in the loop</li>
 * <li>the counter variable is not used outside the loop</li>
 * <li>there is a specified 'return value' of the loop</li>
 * </ul>
 * 
 * Therefore the loop could be unroled.
 * 
 * eg:
 * 
 * <pre>
 * ...
 * double[] in = {1.0, 2.0, 3.0}
 * double [] out[] = new double[in.length];
 * double e = 0;
 * for(int i = 0; i< 3; ++i){
 *  e = e + in[i];
 *  out[i] = e*2;
 * }
 * ...
 * </pre>
 * 
 * Methods that are annotated with this Annotation are processed by the
 * {@link de.tuberlin.uebb.jdae.optimizer.loop.ForLoopUnroller}.
 * 
 * @author Christopher Ewest
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface StrictLoops {
  // no values
}
