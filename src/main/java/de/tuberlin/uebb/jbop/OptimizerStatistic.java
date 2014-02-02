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
package de.tuberlin.uebb.jbop;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import de.tuberlin.uebb.jbop.optimizer.IOptimizer;
import de.tuberlin.uebb.jbop.output.StringTable;

/**
 * The Class OptimizerStatistic.
 * 
 * @author Christopher Ewest
 */
public class OptimizerStatistic {
  
  private final Map<Class<? extends IOptimizer>, Integer> runs = new HashMap<Class<? extends IOptimizer>, Integer>();
  private final Map<Class<? extends IOptimizer>, Integer> emptyRuns = new HashMap<Class<? extends IOptimizer>, Integer>();
  
  public void addRun(final IOptimizer optimizer, final boolean optimized) {
    add(runs, optimizer);
    if (!optimized) {
      add(emptyRuns, optimizer);
    }
  }
  
  private void add(final Map<Class<? extends IOptimizer>, Integer> map, final IOptimizer optimizer) {
    Integer runsOfOpt = map.get(optimizer.getClass());
    int currentValue = 0;
    if (runsOfOpt != null) {
      currentValue = runsOfOpt.intValue();
    }
    runsOfOpt = currentValue + 1;
    map.put(optimizer.getClass(), runsOfOpt);
  }
  
  @Override
  public String toString() {
    final StringTable table = new StringTable();
    table.addColumn("Optimizer", "%20s");
    table.addColumn("Runs", "%5d");
    table.addColumn("empty Runs", "%5d");
    for (final Entry<Class<? extends IOptimizer>, Integer> entry : runs.entrySet()) {
      table.addRow(entry.getKey().getSimpleName(), entry.getValue(), nullTo0(emptyRuns.get(entry.getKey())));
    }
    table.setLatex(true);
    return table.toString();
  }
  
  private Integer nullTo0(final Integer integer) {
    if (integer == null) {
      return 0;
    }
    return integer;
  }
}
