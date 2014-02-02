package de.tuberlin.uebb.jbop.output;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;

public class StringTableTest {
  
  @Test
  public void testDefault() {
    // INIT
    final Object[] rowData = {
        "s", 1, 1.0
    };
    final String[] headers = {
        "string", "int", "double"
    };
    final String[] formats = {
        "%15s", "%15d", "%15.9f"
    };
    
    // RUN
    final StringTable table = new StringTable();
    for (int i = 0; i < headers.length; ++i) {
      table.addColumn(headers[i], formats[i]);
    }
    table.addRow(rowData);
    table.addRow(rowData);
    
    table.setCaption("Ueberschrift");
    
    final String string = table.toString();
    table.setLatex(true);
    final String string2 = table.toString();
    // ASSERT
    assertEquals("" +    //
        "Ueberschrift\n" + //
        "+-----------------+-----------------+-----------------+\n" + //
        "|     string      |       int       |     double      |\n" + //
        "+-----------------+-----------------+-----------------+\n" + //
        "|               s |               1 |     1,000000000 |\n" + //
        "+-----------------+-----------------+-----------------+\n" + //
        "|               s |               1 |     1,000000000 |\n" + //
        "+-----------------+-----------------+-----------------+\n"   //
    , string);
    assertEquals("\\begin{table}\n" + "\\scriptsize\n" + "\\caption[Ueberschrift]{Ueberschrift}\n"
        + "\\label{tab:Ueberschrift}\n" + "\\begin{tabular}{rrr}\n" + "\\hline\n"
        + "\\multicolumn{1}{c}{string}&\\multicolumn{1}{c}{int}&\\multicolumn{1}{c}{double}\\\\\n" + "\\hline\n"
        + "               s &               1 &     1,000000000 \\\\\n" + "\\hline\n"
        + "               s &               1 &     1,000000000 \\\\\n" + "\\hline\n" + "\\end{tabular}\n"
        + "\\end{table}\n" + "" //
    , string2);
  }
  
  @Test
  public void testDefaultWithLabel() {
    // INIT
    final Object[] rowData = {
        "s", 1, 1.0
    };
    final String[] headers = {
        "string", "int", "double"
    };
    final String[] formats = {
        "%15s", "%15d", "%15.9f"
    };
    
    // RUN
    final StringTable table = new StringTable();
    for (int i = 0; i < headers.length; ++i) {
      table.addColumn(headers[i], formats[i]);
    }
    table.addRow(rowData);
    table.addRow(rowData);
    
    table.setCaption("Ueberschrift");
    table.setShortCaption("Ueber");
    table.setLabel("Ueberschrift");
    
    final String string = table.toString();
    table.setLatex(true);
    final String string2 = table.toString();
    // ASSERT
    assertEquals("" +    //
        "Ueberschrift\n" + //
        "+-----------------+-----------------+-----------------+\n" + //
        "|     string      |       int       |     double      |\n" + //
        "+-----------------+-----------------+-----------------+\n" + //
        "|               s |               1 |     1,000000000 |\n" + //
        "+-----------------+-----------------+-----------------+\n" + //
        "|               s |               1 |     1,000000000 |\n" + //
        "+-----------------+-----------------+-----------------+\n"   //
    , string);
    assertEquals("\\begin{table}\n" + "\\scriptsize\n" + "\\caption[Ueber]{Ueberschrift}\n"
        + "\\label{tab:Ueberschrift}\n" + "\\begin{tabular}{rrr}\n" + "\\hline\n"
        + "\\multicolumn{1}{c}{string}&\\multicolumn{1}{c}{int}&\\multicolumn{1}{c}{double}\\\\\n" + "\\hline\n"
        + "               s &               1 &     1,000000000 \\\\\n" + "\\hline\n"
        + "               s &               1 &     1,000000000 \\\\\n" + "\\hline\n" + "\\end{tabular}\n"
        + "\\end{table}\n" + "" //
    , string2);
  }
  
  @Test
  public void testDebug() {
    // INIT
    final Object[] rowData = {
        "s", 1, 1.0
    };
    final String[] headers = {
        "string", "int", "double"
    };
    final String[] formats = {
        "%15s", "%15d", "%15.9f"
    };
    
    // RUN
    final StringTable table = new StringTable();
    table.setDebug(true);
    final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    final PrintStream printStream = new PrintStream(byteArrayOutputStream);
    System.setOut(printStream);
    for (int i = 0; i < headers.length; ++i) {
      table.addColumn(headers[i], formats[i]);
    }
    table.addRow(rowData);
    table.addRow(rowData);
    final String string = byteArrayOutputStream.toString();
    // ASSERT
    assertEquals("\n" +    //
        "+-----------------+-----------------+-----------------+\n" + //
        "|     string      |       int       |     double      |\n" + //
        "+-----------------+-----------------+-----------------+\n" + //
        "|               s |               1 |     1,000000000 |\n" + //
        "+-----------------+-----------------+-----------------+\n" + //
        "|               s |               1 |     1,000000000 |\n" + //
        "+-----------------+-----------------+-----------------+\n" //
    , string);
  }
  
  @Test(expected = IllegalArgumentException.class)
  public void testError() {
    // INIT
    final Object[] rowData = {
        "s", 1, 1.0
    };
    // RUN
    final StringTable table = new StringTable();
    
    table.addRow(rowData);
  }
  
  @Test
  public void testGetSize() {
    // INIT
    final StringTable table = new StringTable();
    table.addColumn("String", "%1s");
    
    // RUN
    table.addRow("s");
    final Pair<Integer, Integer> size = table.getSize();
    table.addRow("c");
    final Pair<Integer, Integer> size2 = table.getSize();
    
    // ASSERT
    
    assertEquals(1, size.getLeft().intValue());
    assertEquals(1, size.getRight().intValue());
    assertEquals(1, size2.getLeft().intValue());
    assertEquals(2, size2.getRight().intValue());
    
  }
  
  @Test
  public void testGetValue() {
    // INIT
    final StringTable table = new StringTable();
    table.addColumn("String", "%1s");
    
    // RUN
    table.addRow("s");
    final Object value = table.getValue(0, 0);
    table.addRow("c");
    final Object value2 = table.getValue(0, 0);
    final Object value3 = table.getValue(0, 1);
    
    // ASSERT
    
    assertEquals("s", value);
    assertEquals("s", value2);
    assertEquals("c", value3);
    
  }
  
  @Test
  public void testGetTitle() {
    // INIT
    final StringTable table = new StringTable();
    table.addColumn("String", "%1s");
    
    // RUN
    final String title = table.getTitle(0);
    // ASSERT
    
    assertEquals("String", title);
    
  }
  
  @Test
  public void testMergeDefault() {
    // INIT
    final StringTable table1 = new StringTable();
    table1.addColumn("String", "%10s");
    table1.addColumn("char", "%1c");
    final StringTable table2 = new StringTable();
    table2.addColumn("String", "%10s");
    table2.addColumn("int", "%10d");
    
    table1.addRow("1", 64);
    table2.addRow("1", 64);
    
    // RUN
    final StringTable merged = StringTable.merge(table1, table2);
    
    // ASSERT
    assertEquals(3, merged.getSize().getLeft().intValue());
    assertEquals(1, merged.getSize().getRight().intValue());
    assertEquals("String", merged.getTitle(0));
    assertEquals("char", merged.getTitle(1));
    assertEquals("int", merged.getTitle(2));
    
  }
  
  @Test(expected = IllegalArgumentException.class)
  public void testMergeError() {
    // INIT
    final StringTable table1 = new StringTable();
    table1.addColumn("String", "%10s");
    table1.addColumn("char", "%1c");
    final StringTable table2 = new StringTable();
    table2.addColumn("Stringx", "%10s");
    table2.addColumn("int", "%10d");
    table1.addRow("s", 1);
    // RUN
    StringTable.merge(table1, table2);
    
  }
  
  @Test
  public void testHeaderToLong() {
    // INIT
    final StringTable table = new StringTable();
    table.addColumn("String", "%5s");
    table.addRow("  s  ");
    
    // RUN
    final String string = table.toString();
    
    // ASSERT
    assertEquals("\n" + "+--------+\n" + "| String |\n" + "+--------+\n" + "|    s   |\n" + "+--------+\n" + "", string);
  }
  
  @Test
  public void testRowDataToLong() {
    // INIT
    final StringTable table = new StringTable();
    table.addColumn("String", "%10s");
    table.addRow("12345678901");
    
    // RUN
    final String string = table.toString();
    
    // ASSERT
    assertEquals("\n" + "+-------------+\n" + "|   String    |\n" + "+-------------+\n" + "| 12345678901 |\n"
        + "+-------------+\n" + "", string);
  }
  
  @Test
  public void testLongCaption() {
    // INIT
    final StringTable table = new StringTable();
    table.addColumn("String", "%10s");
    table.addRow("12345678901");
    table.setCaption("1234 5678 9012 3456");
    
    // RUN
    final String string = table.toString();
    
    // ASSERT
    assertEquals("1234 5678 9012\n" + "3456\n" + "+-------------+\n" + "|   String    |\n" + "+-------------+\n"
        + "| 12345678901 |\n" + "+-------------+\n" + "", string);
  }
}
