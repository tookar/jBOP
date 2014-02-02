/*
 * Copyright (C) 2013 uebb.tu-berlin.de.
 * 
 * This file is part of JBOP (Java Bytecode OPtimizer).
 * 
 * JBOP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * JBOP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with JBOP. If not, see <http://www.gnu.org/licenses/>.
 */
package de.tuberlin.uebb.jbop.output;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.text.WordUtils;
import org.apache.commons.lang3.tuple.Pair;

public class StringTable {
  
  private boolean isDebug = false;
  private boolean headerPrinted = false;
  
  private final List<StringColumn> columns = new LinkedList<>();
  private final List<Object[]> rows = new LinkedList<>();
  private String caption;
  private String shortCaption;
  private String label;
  private boolean isLatex = false;
  private int width = 1;
  
  public void setLatex(final boolean isLatex) {
    this.isLatex = isLatex;
  }
  
  public void addColumn(final String header, final String format) {
    addColumn(StringColumn.of(header, format));
  }
  
  public void addColumn(final StringColumn column) {
    columns.add(column);
    width += column.getWidth() + 3;
  }
  
  public void addRow(final Object... rowData) {
    Validate.isTrue(rowData.length == columns.size());
    if (isDebug) {
      final String format = format(isLatex, columns);
      final String line = line(isLatex, columns);
      final StringBuilder buffer = new StringBuilder();
      if (!headerPrinted) {
        headerPrinted = true;
        final String glheader = glheader(isLatex, columns, getCaption(), getShortCaption(), getLabel(), getWidth());
        final String title = title(isLatex, columns);
        buffer.append(glheader).append("\n");
        buffer.append(title).append("\n");
        buffer.append(line).append("\n");
      }
      buffer.append(String.format(Locale.GERMAN, format, rowData)).append("\n");
      buffer.append(line);
      System.out.println(buffer.toString());
    }
    for (int i = 0; i < columns.size(); i++) {
      final StringColumn column = columns.get(i);
      final int realLength = String.format(column.getFormat(), rowData[i]).length();
      if (realLength > column.getWidth()) {
        final int widthModifier = realLength - column.getWidth();
        column.addToWidth(widthModifier);
        width += widthModifier;
      }
    }
    rows.add(rowData);
  }
  
  private int getWidth() {
    return width;
  }
  
  @Override
  public String toString() {
    final StringBuilder buffer = new StringBuilder();
    final String glheader = glheader(isLatex, columns, getCaption(), getShortCaption(), getLabel(), getWidth());
    final String line = line(isLatex, columns);
    final String title = title(isLatex, columns);
    final String glfooter = glfooter(isLatex, columns);
    buffer.append(glheader).append("\n");
    buffer.append(title).append("\n");
    buffer.append(line).append("\n");
    for (int i = 0; i < rows.size(); i++) {
      final Object[] row = rows.get(i);
      final String format = format(isLatex, columns);
      buffer.append(String.format(Locale.GERMAN, format, row)).append("\n");
      if (i < (rows.size() - 1)) {
        buffer.append(line).append("\n");
      }
    }
    buffer.append(glfooter).append("\n");
    return buffer.toString();
  }
  
  private static String mc(final String content) {
    return "\\multicolumn{1}{c}{" + content + "}";
  }
  
  private static String title(final boolean latex, final List<StringColumn> cols) {
    final StringBuilder buffer = new StringBuilder();
    if (!latex) {
      buffer.append("|");
    }
    for (int i = 0; i < cols.size(); i++) {
      final StringColumn col = cols.get(i);
      if (latex) {
        buffer.append(mc(col.getHeader()));
        if (i < (cols.size() - 1)) {
          buffer.append("&");
        }
      } else {
        buffer.append(" ").append(StringUtils.center(col.getHeader(), col.getWidth())).append(" ");
        buffer.append("|");
      }
    }
    if (latex) {
      buffer.append("\\\\");
    }
    return buffer.toString();
  }
  
  private static String line(final boolean latex, final List<StringColumn> cols) {
    if (latex) {
      return "\\hline";
    }
    final StringBuilder buffer = new StringBuilder();
    buffer.append("+");
    for (final StringColumn col : cols) {
      buffer.append(StringUtils.leftPad("", col.getWidth() + 2, "-")).append("+");
    }
    return buffer.toString();
  }
  
  private static String glheader(final boolean latex, final List<StringColumn> cols, final String caption,
      final String smallCaption, final String label, final int width) {
    final StringBuilder buffer = new StringBuilder();
    if (!latex) {
      
      buffer.append(WordUtils.wrap(caption, width)).append("\n").append(line(latex, cols));
      return buffer.toString();
    }
    buffer.append("\\begin{table}\n\\scriptsize\n").//
        append("\\caption[").append(smallCaption).append("]{").append(caption).append("}\n").//
        append("\\label{tab:").append(label).append("}\n").//
        append("\\begin{tabular}{").//
        append(StringUtils.repeat("r", cols.size())).//
        append("}\n\\hline");
    return buffer.toString();
    
  }
  
  private static String format(final boolean latex, final List<StringColumn> formats) {
    final StringBuilder buffer = new StringBuilder();
    if (!latex) {
      buffer.append("|");
    }
    for (int i = 0; i < formats.size(); i++) {
      final StringColumn stringColumn = formats.get(i);
      final String format = stringColumn.getFormat();
      buffer.append(" ").append(format).append(" ");
      if (latex) {
        if (i < (formats.size() - 1)) {
          buffer.append("&");
        }
      } else {
        buffer.append("|");
      }
    }
    if (latex) {
      buffer.append("\\\\");
    }
    return buffer.toString();
  }
  
  private static String glfooter(final boolean latex, final List<StringColumn> cols) {
    if (latex) {
      return "\\hline\n\\end{tabular}\n\\end{table}";
    }
    return line(latex, cols);
  }
  
  public void setDebug(final boolean isDebug) {
    this.isDebug = isDebug;
  }
  
  public Object getValue(final int col, final int row) {
    return (rows.get(row)[col]);
  }
  
  public String getTitle(final int col) {
    return (columns.get(col).getHeader());
  }
  
  public Pair<Integer, Integer> getSize() {
    return Pair.of(columns.size(), rows.size());
  }
  
  public static StringTable merge(final StringTable one, final StringTable two) {
    
    Validate.isTrue(one.getSize().getRight().intValue() == two.getSize().getRight().intValue());
    Validate.isTrue(StringUtils.equals(one.columns.get(0).getHeader(), two.columns.get(0).getHeader()));
    Validate.isTrue(StringUtils.equals(one.columns.get(0).getFormat(), two.columns.get(0).getFormat()));
    
    final List<StringColumn> columns = new ArrayList<>();
    columns.addAll(one.columns);
    for (int i = 1; i < two.columns.size(); ++i) {
      columns.add(two.columns.get(i));
    }
    
    final StringTable merged = new StringTable();
    for (final StringColumn column : columns) {
      merged.addColumn(column);
    }
    
    for (int i = 0; i < one.rows.size(); ++i) {
      final Object[] rowData = ArrayUtils.addAll(one.rows.get(i),
          ArrayUtils.subarray(two.rows.get(i), 1, two.rows.get(i).length));
      merged.addRow(rowData);
    }
    merged.setLatex(one.isLatex);
    return merged;
  }
  
  /**
   * Sets the caption.
   * 
   * @param caption
   *          the new caption
   */
  public void setCaption(final String caption) {
    this.caption = caption;
  }
  
  String getCaption() {
    if (caption == null) {
      return "";
    }
    return caption;
  }
  
  /**
   * Sets the caption.
   * 
   * @param caption
   *          the new caption
   */
  public void setShortCaption(final String shortCaption) {
    this.shortCaption = shortCaption;
  }
  
  String getShortCaption() {
    if (shortCaption == null) {
      return getCaption();
    }
    return shortCaption;
  }
  
  /**
   * Sets the label.
   * 
   * @param label
   *          the new label
   */
  public void setLabel(final String label) {
    this.label = label;
  }
  
  String getLabel() {
    if (label == null) {
      return getCaption();
    }
    return label;
  }
  
}
