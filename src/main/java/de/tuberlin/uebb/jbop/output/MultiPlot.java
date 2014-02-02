package de.tuberlin.uebb.jbop.output;

import java.util.ArrayList;
import java.util.List;

public class MultiPlot extends AbstractPlot {
  
  private final List<AbstractPlot> plots = new ArrayList<>();
  
  public void addPlot(final AbstractPlot plot) {
    plots.add(plot);
  }
  
  @Override
  public String getTitle() {
    final StringBuilder builder = new StringBuilder();
    for (int i = 0; i < plots.size(); i++) {
      final AbstractPlot plot = plots.get(i);
      builder.append(plot.getTitle());
      if (i < (plots.size() - 1)) {
        builder.append(",");
      }
    }
    return builder.toString();
  }
  
  @Override
  public String getPlot() {
    final StringBuilder builder = new StringBuilder();
    for (int i = 0; i < plots.size(); i++) {
      final AbstractPlot plot = plots.get(i);
      builder.append(plot.getPlot());
    }
    return builder.toString();
  }
  
}
