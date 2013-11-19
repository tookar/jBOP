package de.tuberlin.uebb.jbop.example;

import de.tuberlin.uebb.jbop.optimizer.annotations.ImmutableArray;
import de.tuberlin.uebb.jbop.optimizer.annotations.Optimizable;
import de.tuberlin.uebb.jbop.optimizer.annotations.StrictLoops;

public class Example implements IExample {
  
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
        debug("This will crash...");
      }
    }
    double res = 0;
    for (int i = 0; i < doubleArray.length; ++i) {
      final double[] localDoubleArray = doubleArray[0];
      for (int j = 0; j < localDoubleArray.length; ++j) {
        res += chain.chainArray[j] + localDoubleArray[j];
      }
    }
    return res;
  }
  
  private void debug(final String text) {
    System.out.println(text);
  }
}
