package de.tuberlin.uebb.jbop.output;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

public class StringColumn {
  
  private static final String REGEX = "(\\D*)(\\d*)(.*(.))";
  private static final Pattern PATTERN = Pattern.compile(REGEX);
  
  private final String header;
  private String format;
  
  public StringColumn(final String header, final String format) {
    this.format = format;
    this.header = header;
    final Matcher matcher = getMatcher();
    final String group = matcher.group(2);
    
    final int width;
    if (StringUtils.isBlank(group)) {
      width = 0;
    } else {
      width = Integer.parseInt(group);
    }
    if (header.length() > width) {
      this.format = matcher.group(1) + header.length() + matcher.group(3);
    }
  }
  
  protected String getHeader() {
    return header;
  }
  
  protected String getFormat() {
    return format;
  }
  
  protected int getWidth() {
    return Integer.parseInt(getMatcher().group(2));
  }
  
  public static StringColumn of(final String header, final String format) {
    return new StringColumn(header, format);
  }
  
  protected void addToWidth(final int modifier) {
    final Matcher matcher = getMatcher();
    format = matcher.group(1) + (getWidth() + modifier) + matcher.group(3);
  }
  
  private Matcher getMatcher() {
    final Matcher matcher = PATTERN.matcher(format);
    matcher.matches();
    return matcher;
  }
  
}
