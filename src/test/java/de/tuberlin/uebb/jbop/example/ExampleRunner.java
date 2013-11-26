package de.tuberlin.uebb.jbop.example;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.Locale;

import de.tuberlin.uebb.jbop.exception.JBOPClassException;
import de.tuberlin.uebb.jbop.optimizer.Optimizer;

public class ExampleRunner {
  
  public static void main(final String[] args) throws JBOPClassException {
    final ExampleChain chain = new ExampleChain(new double[] {
        1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0,//
    });
    final double[][] doubleArray = new double[][] {
        {
            11.0, 12.0, 13.0, 14.0, 10.0, 16.0, 17.0, 18.0, 19.0, 20.0,//
        }, {
            21.0, 22.0, 23.0, 24.0, 25.0, 26.0, 27.0, 28.0, 29.0, 30.0,//
        },
    };
    
    final ThreadMXBean bean = ManagementFactory.getThreadMXBean();
    
    final String header = "\\hline";
    final String glfooter = "end{tabular*}";
    final String glheader = "begin{tabular*}{\\linewidth}{|r|r|r|r|r|r|r|r|r|}\n\\hline";
    final String format = " %,d & %,d & %,d & %,d & %,d & %,d & %,1.9f & %,1.9f & %1.9f \\\\";
    
    final String titlex = "\\multicolum{1}{|c|}{create}&\\multicolum{1}{|c|}{optimize}&\\multicolum{1}{|c|}{run}&\\multicolum{1}{|c|}{run-opt}&\\multicolum{1}{|c|}{c+r}&\\multicolum{1}{|c|}{o+ro}&\\multicolum{1}{|c|}{v\\_c}&\\multicolum{1}{|c|}{v\\_r}&\\multicolum{1}{|c|}{v\\_t}\\\\";
    // final String header =
    // "+-----------------+-----------------+-----------------+-----------------+-----------------+-----------------+-----------------+-----------------+-----------------+";
    // final String format = "| %,15d | %,15d | %,15d | %,15d | %,15d | %,15d | %,15.9f | %,15.9f | %15.9f |";
    // final String titlex =
    // "|      create     |     optimize    |       run       |     run-opt     |        c+r      |       o+ro      |        v_c      |        v_r      |       v_t       |";
    
    System.out.println(glheader);
    System.out.println(titlex);
    System.out.println(header);
    
    for (int i = 0; i < 20; ++i) {
      System.gc();
      final long startCreate = bean.getCurrentThreadCpuTime();
      final Example example = new Example(chain, doubleArray);
      final long endCreate = bean.getCurrentThreadCpuTime();
      
      final long startOptimize = bean.getCurrentThreadCpuTime();
      final IExample optimizedExample = new Optimizer().optimize(example, "__optimized");
      final long endOptimize = bean.getCurrentThreadCpuTime();
      
      final long startRunNormal = bean.getCurrentThreadCpuTime();
      double run = 0;
      for (int j = 0; j < 100000000; ++j) {
        run = example.run();
      }
      final long endRunNormal = bean.getCurrentThreadCpuTime();
      
      final long startRunOptimized = bean.getCurrentThreadCpuTime();
      double runOptimized = 0;
      for (int j = 0; j < 100000000; ++j) {
        runOptimized = optimizedExample.run();
      }
      final long endRunOptimized = bean.getCurrentThreadCpuTime();
      
      final long timeCreate = endCreate - startCreate;
      final long timeOptimize = endOptimize - startOptimize;
      final long timeRunNormal = endRunNormal - startRunNormal;
      final long timeRunOptimized = endRunOptimized - startRunOptimized;
      final long totalNormal = timeCreate + timeRunNormal;
      final long totalOpt = timeOptimize + timeRunOptimized;
      
      final double rateCreate = (double) timeOptimize / (double) timeCreate;
      final double rateRun = (double) timeRunOptimized / (double) timeRunNormal;
      final double rateTotal = (double) totalOpt / (double) totalNormal;
      
      final String ergebnis = String.format(Locale.GERMAN, format, timeCreate, timeOptimize, timeRunNormal,
          timeRunOptimized, totalNormal, totalOpt, rateCreate, rateRun, rateTotal);// , run, runOptimized);
      
      System.out.println(ergebnis);
      System.out.println(header);
    }
    System.out.println(glfooter);
  }
}
