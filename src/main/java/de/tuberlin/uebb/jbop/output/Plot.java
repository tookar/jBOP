package de.tuberlin.uebb.jbop.output;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

public class Plot extends AbstractPlot {
  
  private final List<Pair<String, String>> coordinates = new ArrayList<>();
  private final String title;
  
  public Plot(final String title) {
    this.title = title;
  }
  
  public void addCoordinates(final String x, final String y) {
    coordinates.add(Pair.of(x, y));
  }
  
  @Override
  public String getTitle() {
    return title;
  }
  
  @Override
  public String getPlot() {
    final StringBuilder builder = new StringBuilder();
    builder.append("\\addplot coordinates {");
    for (final Pair<String, String> coordinate : coordinates) {
      // builder.append("(").append(coordinate.getLeft()).append(",").append(coordinate.getRight()).append(")");
      builder.append("(").append(coordinate.getRight()).append(",").append(coordinate.getLeft()).append(")");
    }
    builder.append("};\n");
    return builder.toString();
  }
  
}
