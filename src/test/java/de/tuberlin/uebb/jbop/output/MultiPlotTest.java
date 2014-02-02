package de.tuberlin.uebb.jbop.output;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class MultiPlotTest {
  
  @Test
  public void test() {
    // INIT
    final Plot subplot1 = new Plot("plot1");
    final Plot subplot2 = new Plot("plot2");
    
    // RUN
    final MultiPlot plot = new MultiPlot();
    plot.setShortCaption("hallo");
    plot.addPlot(subplot1);
    plot.addPlot(subplot2);
    final String tikzPicture = plot.getTikzPicture("xlabel", "ylabel");
    
    // ASSERT
    assertEquals(
        "\\begin{figure}\n"
            + "\\begin{tikzpicture}\n"
            + "\\begin{axis}[xlabel={xlabel},ylabel={ylabel},legend style={at={(1.8,.5)},anchor=east},height=.6\\textwidth,width=.6\\textwidth]\n"
            + "\\addplot coordinates {};\n" + "\\addplot coordinates {};\n" + "\\legend{plot1,plot2}\n"
            + "\\end{axis}\n" + "\\end{tikzpicture}\n" + "\\caption[hallo]{}\n" + "\\label{fig:}\n" + "\\end{figure}",
        tikzPicture);
  }
}
