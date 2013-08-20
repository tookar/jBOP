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
package de.tuberlin.uebb.jbop.optimizer.methodsplitter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

/**
 * The Class VarList.
 * 
 * This is a special List for {@link Var}s.
 * 
 * @author Christopher Ewest
 */
class VarList implements Iterable<Var> {
  
  private final LinkedList<Var> delegate = new LinkedList<>();
  
  public Pair<Var, Var> getBounds(final int index) {
    return Pair.of(getFirstVar(index), getLastVar(index));
  }
  
  @Override
  public String toString() {
    return StringUtils.join(this, ", ");
  }
  
  /**
   * @see java.util.List#add(Object)
   */
  public void add(final Var e) {
    delegate.add(e);
  }
  
  /**
   * @see java.util.List#addAll(Collection)
   */
  
  public void addAll(final Collection<? extends Var> c) {
    delegate.addAll(c);
  }
  
  /**
   * @see java.util.List#addAll(Collection)
   */
  public boolean addAll(final VarList c) {
    delegate.addAll(c.delegate);
    return true;
  }
  
  /**
   * @see java.util.List#contains(Object)
   */
  public boolean contains(final Object o) {
    return delegate.contains(o);
  }
  
  /**
   * @return true if the list contains a Variable with the given index
   */
  public boolean containsIndex(final int index) {
    for (final Var var : this) {
      if (var.getVarIndex() == index) {
        return true;
      }
    }
    return false;
  }
  
  /**
   * @see java.util.List#isEmpty()
   */
  public boolean isEmpty() {
    return delegate.isEmpty();
  }
  
  @Override
  public Iterator<Var> iterator() {
    return delegate.iterator();
  }
  
  /**
   * @see java.util.List#remove(Object)
   */
  public void remove(final Object o) {
    delegate.remove(o);
  }
  
  /**
   * @see java.util.List#removeAll(Collection)
   */
  public void removeAll(final Collection<? extends Var> o) {
    delegate.removeAll(o);
  }
  
  /**
   * @see java.util.List#removeAll(Collection)
   */
  public void removeAll(final VarList o) {
    for (final Var var : o) {
      remove(var);
    }
  }
  
  /**
   * @see java.util.List#size()
   */
  public int size() {
    return delegate.size();
  }
  
  /**
   * @return the first Var with the given index
   */
  public Var getFirstVar(final int index) {
    return getVar(index, delegate.iterator());
  }
  
  /**
   * @return the position of the first Var with the given index
   */
  public int getFirstPosition(final int index) {
    return getPosition(index, delegate.iterator());
  }
  
  /**
   * @return the position of the last Var with the given index
   */
  public int getLastPosition(final int index) {
    return getPosition(index, delegate.descendingIterator());
  }
  
  /**
   * @return the last Var with the given index
   */
  public Var getLastVar(final int index) {
    return getVar(index, delegate.descendingIterator());
  }
  
  private int getPosition(final int index, final Iterator<Var> set) {
    while (set.hasNext()) {
      final Var var = iterator().next();
      if (var.getVarIndex() == index) {
        return var.getVarPosition();
      }
    }
    return -1;
  }
  
  private Var getVar(final int index, final Iterator<Var> set) {
    while (set.hasNext()) {
      final Var var = set.next();
      if (var.getVarIndex() == index) {
        return var;
      }
    }
    return null;
  }
  
  /**
   * Removes all Vars with the given index
   */
  public void removeAll(final int index) {
    final Collection<Var> toBeRemoved = new ArrayList<>();
    for (final Var var : this) {
      if (var.getVarIndex() == index) {
        toBeRemoved.add(var);
      }
    }
    removeAll(toBeRemoved);
  }
  
  /**
   * @see java.util.List#clear()
   */
  public void clear() {
    delegate.clear();
  }
  
}
