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
package de.tuberlin.uebb.jbop.optimizer.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import de.tuberlin.uebb.jbop.optimizer.IOptimizer;

/**
 * This annotation can be used, to use additional
 * optimizer-Steps when processing the annotated method.
 * 
 * The provided IOptimizers must have a no args constructor.
 * But they can implement {@link de.tuberlin.uebb.jbop.optimizer.IClassNodeAware} and
 * {@link de.tuberlin.uebb.jbop.optimizer.IInputObjectAware} to be initialized
 * with the current Objects.
 * 
 * @author Christopher Ewest
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface AdditionalSteps {
  
  /**
   * array with additional Optimizer-Steps.
   * 
   * @return the class<? extends i optimizer>[]
   */
  Class<? extends IOptimizer>[] steps();
}
