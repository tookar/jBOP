package de.tuberlin.uebb.jbop.example;

import java.util.logging.Logger;

import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import de.tuberlin.uebb.jbop.optimizer.annotations.ImmutableArray;
import de.tuberlin.uebb.jbop.optimizer.annotations.Optimizable;
import de.tuberlin.uebb.jbop.optimizer.annotations.StrictLoops;

public class Example implements IExample, Comparable<IExample> {
  
  private final ExampleChain chain;
  @ImmutableArray
  private final double[][] doubleArray;
  
  public Example(final ExampleChain chain, final double[][] doubleArray) {
    this.chain = chain;
    this.doubleArray = doubleArray;
  }
  
  @Override
  @Optimizable
  @StrictLoops
  public double run() {
    if (chain == null) {
      return -1;
    }
    for (int i = 0; i < doubleArray.length; ++i) {
      if (chain.chainArray.length != doubleArray[i].length) {
        Logger.getLogger("").fine("This will crash...");
      }
    }
    double res = 0;
    for (int i = 0; i < doubleArray.length; ++i) {
      final double[] localDoubleArray = doubleArray[0];
      for (int j = 0; j < localDoubleArray.length; ++j) {
        for (int k = 0; k < 8; ++k) {
          res += chain.chainArray[j] + localDoubleArray[j];
        }
      }
    }
    return res;
  }
  
  @Override
  public int hashCode() {
    return HashCodeBuilder.reflectionHashCode(this, false);
  }
  
  @Override
  public boolean equals(final Object obj) {
    return EqualsBuilder.reflectionEquals(this, obj, false);
  }
  
  @Override
  public int compareTo(final IExample o) {
    return CompareToBuilder.reflectionCompare(this, o);
  }
}
