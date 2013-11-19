package de.tuberlin.uebb.jbop.example;

import de.tuberlin.uebb.jbop.optimizer.annotations.ImmutableArray;

public class ExampleChain {
  
  @ImmutableArray
  public final double[] chainArray;
  
  public ExampleChain(final double[] chainArray) {
    this.chainArray = chainArray;
  }
  
}
