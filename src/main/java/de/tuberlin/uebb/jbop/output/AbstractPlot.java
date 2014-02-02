package de.tuberlin.uebb.jbop.output;

/**
 * The Class AbstractPlot.
 * 
 * @author Christopher Ewest
 */
public abstract class AbstractPlot {
  
  private String caption;
  private String label;
  private String shortCaption;
  private boolean logy;
  private boolean logx;
  
  /**
   * Gets the title.
   * 
   * @return the title
   */
  public abstract String getTitle();
  
  /**
   * Gets the plot.
   * 
   * @return the plot
   */
  public abstract String getPlot();
  
  /**
   * Gets the legend.
   * 
   * @return the legend
   */
  public final String getLegend() {
    return "\\legend{" + getTitle() + "}";
  }
  
  /**
   * Gets the tikz picture.
   * 
   * @param xLabel
   *          the x label
   * @param yLabel
   *          the y label
   * @return the tikz picture
   */
  public final String getTikzPicture(final String xLabel, final String yLabel) {
    final StringBuilder builder = new StringBuilder();
    builder.append(getPrefix(xLabel, yLabel));
    builder.append(getPlot());
    builder.append(getLegend());
    builder.append(getSuffix());
    return builder.toString();
  }
  
  /**
   * Gets the prefix.
   * 
   * @param xLabel
   *          the x label
   * @param yLabel
   *          the y label
   * @return the prefix
   */
  protected String getPrefix(final String xLabel, final String yLabel) {
    final StringBuilder builder = new StringBuilder();
    builder.append("\\begin{figure}\n");
    builder.append("\\begin{tikzpicture}\n");
    builder.append("\\begin{axis}[");
    if (logy) {
      builder.append("ymode=log,");
    }
    if (logx) {
      builder.append("xmode=log,");
    }
    builder.append("xlabel={").append(xLabel).append("},ylabel={").append(yLabel).append("}");
    builder.append(",legend style={at={(1.8,.5)},anchor=east},height=.6\\textwidth,width=.6\\textwidth");
    builder.append("]\n");
    return builder.toString();
  }
  
  /**
   * Gets the suffix.
   * 
   * @return the suffix
   */
  protected String getSuffix() {
    final StringBuilder builder = new StringBuilder();
    builder.append("\n\\end{axis}\n");
    builder.append("\\end{tikzpicture}\n");
    
    builder.append("\\caption[").append(getShortCaption()).append("]{").append(getCaption()).append("}\n");
    builder.append("\\label{fig:").append(getLabel()).append("}\n");
    builder.append("\\end{figure}");
    return builder.toString();
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
  
  private String getCaption() {
    if (caption == null) {
      return "";
    }
    return caption;
  }
  
  /**
   * Sets the caption.
   * 
   * @param shortCaption
   *          the new short caption
   */
  public void setShortCaption(final String shortCaption) {
    this.shortCaption = shortCaption;
  }
  
  /**
   * Gets the short caption.
   * 
   * @return the short caption
   */
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
  
  private String getLabel() {
    if (label == null) {
      return getCaption();
    }
    return label;
  }
  
  /**
   * Sets the logy.
   * 
   * @param logy
   *          the new logy
   */
  public void setLogy(final boolean logy) {
    this.logy = logy;
  }
  
  /**
   * Sets the logx.
   * 
   * @param logx
   *          the new logx
   */
  public void setLogx(final boolean logx) {
    this.logx = logx;
  }
}
