package de.tuberlin.uebb.jbop.output;

import org.apache.commons.collections15.Transformer;
import org.apache.commons.lang3.tuple.Pair;

public class TableToAbstractPlot implements Transformer<StringTable, AbstractPlot> {
  
  @Override
  public AbstractPlot transform(final StringTable input) {
    final Pair<Integer, Integer> size = input.getSize();
    final AbstractPlot plot;
    if (size.getLeft() == 2) {
      plot = singlePlot(input, 1);
    } else {
      plot = multiPlot(input);
    }
    plot.setLabel(input.getLabel());
    plot.setCaption(input.getCaption());
    plot.setShortCaption(input.getShortCaption());
    return plot;
  }
  
  private AbstractPlot multiPlot(final StringTable input) {
    final MultiPlot multiPlot = new MultiPlot();
    for (int i = 1; i < input.getSize().getLeft(); ++i) {
      multiPlot.addPlot(singlePlot(input, i));
    }
    return multiPlot;
  }
  
  private AbstractPlot singlePlot(final StringTable input, final int col) {
    final Plot plot = new Plot(input.getTitle(col));
    for (int i = 0; i < input.getSize().getRight(); ++i) {
      plot.addCoordinates(String.valueOf(input.getValue(col, i)), String.valueOf(input.getValue(0, i)));
    }
    return plot;
  }
  
}
