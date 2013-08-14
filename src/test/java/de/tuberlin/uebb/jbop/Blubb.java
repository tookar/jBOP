package de.tuberlin.uebb.jbop;

public class Blubb {
  
  Object[][][] object;
  
  public Blubb(final Object[][][] object) {
    super();
    this.object = object;
  }
  
  public void blubb() {
    
    for (final Object o : object[1][1]) {
      System.out.println(o);
    }
  }
}
