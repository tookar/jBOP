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
package de.tuberlin.uebb.jbop.optimizer;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.tree.LabelNode;

/**
 * The Class LabelMap.
 * 
 * Used for cloning nodes.
 * 
 * @author Christopher Ewest
 */
public class LabelMap implements Map<LabelNode, LabelNode> {
  
  private final Map<LabelNode, LabelNode> labels = new HashMap<>();
  
  @Override
  public int size() {
    return labels.size();
  }
  
  @Override
  public boolean isEmpty() {
    return labels.isEmpty();
  }
  
  @Override
  public boolean containsKey(final Object key) {
    return labels.containsKey(key);
  }
  
  @Override
  public boolean containsValue(final Object value) {
    return labels.containsValue(value);
  }
  
  /**
   * Creates new Label nodes for unknown keys.
   * 
   * @see Map#get(Object)
   * 
   * @param key
   *          the key
   * @return the label node
   */
  @Override
  public LabelNode get(final Object key) {
    if (labels.get(key) == null) {
      put((LabelNode) key, new LabelNode());
    }
    return labels.get(key);
  }
  
  @Override
  public LabelNode put(final LabelNode key, final LabelNode value) {
    return labels.put(key, value);
  }
  
  @Override
  public LabelNode remove(final Object key) {
    return labels.remove(key);
  }
  
  @Override
  public void putAll(final Map<? extends LabelNode, ? extends LabelNode> m) {
    labels.putAll(m);
  }
  
  @Override
  public void clear() {
    labels.clear();
  }
  
  @Override
  public Set<LabelNode> keySet() {
    return labels.keySet();
  }
  
  @Override
  public Collection<LabelNode> values() {
    return labels.values();
  }
  
  @Override
  public Set<java.util.Map.Entry<LabelNode, LabelNode>> entrySet() {
    return labels.entrySet();
  }
  
  @Override
  public boolean equals(final Object o) {
    return labels.equals(o);
  }
  
  @Override
  public int hashCode() {
    return labels.hashCode();
  }
  
}
