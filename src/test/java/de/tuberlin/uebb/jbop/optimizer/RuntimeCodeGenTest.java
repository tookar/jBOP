package de.tuberlin.uebb.jbop.optimizer;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import de.tuberlin.uebb.jbop.exception.JBOPClassException;
import de.tuberlin.uebb.jbop.optimizer.annotations.Optimizable;
import de.tuberlin.uebb.jbop.optimizer.annotations.StrictLoops;

public class RuntimeCodeGenTest {
  
  public static interface Foo {
    
    public int sum();
  }
  
  public static class Bar implements Foo {
    
    public final int range;
    
    public Bar(final int range) {
      super();
      this.range = range;
    }
    
    @Override
    @Optimizable
    @StrictLoops
    public int sum() {
      int s = 0;
      for (int i = 0; i < range; ++i) {
        s = s + i;
      }
      return s;
    }
    
  }
  
  public static class Baz implements Foo {
    
    public final int range;
    
    public Baz(final int range) {
      super();
      this.range = range;
    }
    
    @Override
    public int sum() {
      final int s[] = new int[1];
      sumUp(s);
      return s[0];
    }
    
    @Optimizable
    @StrictLoops
    private void sumUp(final int[] r) {
      for (int i = 0; i < range; ++i) {
        r[0] += i;
      }
    }
  }
  
  @Test
  public void testJBOP() throws JBOPClassException {
    final Optimizer optim = new Optimizer();
    final int range = 2;
    final Foo unoptimized = new Bar(range);
    final Foo optimized = optim.optimize(unoptimized, "range_" + range);
    
    assertThat(unoptimized.sum(), is(((range * (range + 1)) / 2) - range));
    
    assertThat(optimized.sum(), is(unoptimized.sum()));
  }
  
  @Test
  public void testJBOPWithoutReturn() throws JBOPClassException {
    final Optimizer optim = new Optimizer();
    final int range = 100;
    final Foo unoptimized = new Baz(range);
    final Foo optimized = optim.optimize(unoptimized, "range_" + range);
    assertThat(optimized.sum(), is(unoptimized.sum()));
  }
  
}
