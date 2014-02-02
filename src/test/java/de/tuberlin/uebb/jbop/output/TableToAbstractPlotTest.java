package de.tuberlin.uebb.jbop.output;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;

public class TableToAbstractPlotTest {
  
  @Test
  public void testTwoColumns() {
    // INIT
    final StringTable table = mock(StringTable.class);
    when(table.getSize()).thenReturn(Pair.of(2, 1));
    when(table.getTitle(anyInt())).thenReturn("Title");
    when(table.getValue(anyInt(), anyInt())).thenReturn("value");
    
    final TableToAbstractPlot transformer = new TableToAbstractPlot();
    
    // RUN
    final AbstractPlot plot = transformer.transform(table);
    
    // ASSERT
    assertEquals(
        "\\begin{figure}\n"
            + "\\begin{tikzpicture}\n"
            + "\\begin{axis}[xlabel={},ylabel={},legend style={at={(1.8,.5)},anchor=east},height=.6\\textwidth,width=.6\\textwidth]\n"
            + "\\addplot coordinates {(value,value)};\n" + "\\legend{Title}\n" + "\\end{axis}\n"
            + "\\end{tikzpicture}\n" + "\\caption[]{}\n" + "\\label{fig:}\n" + "\\end{figure}",
        plot.getTikzPicture("", ""));
  }
  
  @Test
  public void testThreColumns() {
    // INIT
    final StringTable table = mock(StringTable.class);
    when(table.getSize()).thenReturn(Pair.of(3, 1));
    when(table.getTitle(anyInt())).thenReturn("Title");
    when(table.getValue(anyInt(), anyInt())).thenReturn("value");
    
    final TableToAbstractPlot transformer = new TableToAbstractPlot();
    
    // RUN
    final AbstractPlot plot = transformer.transform(table);
    
    // ASSERT
    assertEquals(
        "\\begin{figure}\n"
            + "\\begin{tikzpicture}\n"
            + "\\begin{axis}[xlabel={},ylabel={},legend style={at={(1.8,.5)},anchor=east},height=.6\\textwidth,width=.6\\textwidth]\n"
            + "\\addplot coordinates {(value,value)};\n" + "\\addplot coordinates {(value,value)};\n"
            + "\\legend{Title,Title}\n" + "\\end{axis}\n" + "\\end{tikzpicture}\n" + "\\caption[]{}\n"
            + "\\label{fig:}\n" + "\\end{figure}", plot.getTikzPicture("", ""));
  }
}
