package de.tuberlin.uebb.jbop.output;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class StringColumnTest {
  
  @Test
  public void testGetHeader() {
    // INIT
    final StringColumn column = StringColumn.of("header", "%15s");
    
    // RUN
    final String header = column.getHeader();
    
    // ASSERT
    assertEquals("header", header);
  }
  
  @Test
  public void testGetFormat() {
    // INIT
    final StringColumn column = StringColumn.of("header", "%15s");
    
    // RUN
    final String format = column.getFormat();
    
    // ASSERT
    assertEquals("%15s", format);
  }
  
  @Test
  public void testGetWidthNotSpecified() {
    // RUN + ASSERT
    final StringColumn column = StringColumn.of("header", "%s");
    
    assertEquals(6, column.getWidth());
    column.addToWidth(2);
    assertEquals(8, column.getWidth());
  }
  
  @Test
  public void testGetWidth() {
    final String[] formats = {
        "%15f", "%15d", "%15b", "%15c", "%15o", "%15x", "%15X", "%15e", "%15E", "%15g", "%15G", "%15a", "%15A",
    };
    for (final String format : formats) {
      // INIT
      final StringColumn column = StringColumn.of("header", format);
      
      // RUN
      final int size = column.getWidth();
      
      // ASSERT
      assertEquals(15, size);
    }
  }
  
}
