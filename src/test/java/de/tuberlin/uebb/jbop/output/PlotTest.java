package de.tuberlin.uebb.jbop.output;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class PlotTest {
  
  @Test
  public void test() {
    // INIT
    final Plot plot = new Plot("title");
    plot.addCoordinates("0", "0");
    plot.addCoordinates("1", "1");
    plot.addCoordinates("2", "2");
    plot.setLabel("label");
    plot.setCaption("caption");
    
    // RUN
    final String tikzPicture = plot.getTikzPicture("xlabel", "ylabel");
    
    // ASSERT
    
    assertEquals(
        "\\begin{figure}\n"
            + "\\begin{tikzpicture}\n"
            + "\\begin{axis}[xlabel={xlabel},ylabel={ylabel},legend style={at={(1.8,.5)},anchor=east},height=.6\\textwidth,width=.6\\textwidth]\n"
            + "\\addplot coordinates {(0,0)(1,1)(2,2)};\n" + "\\legend{title}\n" + "\\end{axis}\n"
            + "\\end{tikzpicture}\n" + "\\caption[caption]{caption}\n" + "\\label{fig:label}\n" + "\\end{figure}",
        tikzPicture);
  }
}
