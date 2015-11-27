/**
 * Version: ${VERSION}$
 * Copyright ${2003-2004}$ by ActiveTree Inc.
 * All rights reserved.
 *
 * Warning:
 * This source code provided to help you understand how the APIs
 * can work for your program. It carries no liability for any kind of damage
 * non-functioning and any other as specified in the license terms and consitions
 * for any product.
 *
 * This example program is redistributable provided it carries the above
 * copy right and warning information.
 */
package com.deepsoft.uisupport;

import com.activetree.common.report.PageHeaderFooterListener;
import com.activetree.common.report.AtHeaderFooterElement;
import com.activetree.common.report.PageHeaderFooterRenderer;

import java.awt.*;
import java.awt.print.PageFormat;

public class SimpleHeaderFooterRenderer implements PageHeaderFooterListener {
  public static final String DEFAULT_HEADER_TEXT = "GlacierCommand GUI";
  public static final String DEFAULT_FOOTER_TEXT = "Copyright (c) Deepwoods Software.";
  protected int lineSpacing = 2;
  protected String headerText = DEFAULT_HEADER_TEXT;
  protected String footerText = DEFAULT_FOOTER_TEXT;
  protected Color lineColor = Color.BLACK;
  protected Color headerTextColor = Color.BLACK;
  protected Color footerTextColor = Color.BLACK;
  protected Font headerFont = new Font("Helvetica", Font.PLAIN, 10);
  protected Font footerFont = new Font("Helvetica", Font.PLAIN, 10);

  public SimpleHeaderFooterRenderer() {}

  public SimpleHeaderFooterRenderer(String headerText, String footerText) {
    this.headerText = headerText;
    this.footerText = footerText;
  }

  public void drawHeader(Graphics g, Rectangle headerBbox, int pageNumber, int totalPageCount, int pageNumberLocation, PageFormat pf, double zoom) {
//    Color orgColor = g.getColor();
//    Font orgFont = g.getFont();

    //draw a line on the header bottom
    int x1 = (int) pf.getImageableX();
    int y1 = (int) (headerBbox.getY() + headerBbox.getHeight()) - lineSpacing; //  headerBbox.getHeight());
    int x2 = (int) (pf.getWidth() - pf.getImageableX());
    int y2 = y1;
    g.setColor(lineColor);
    g.drawLine(x1, y1, x2, y2);

    if (headerText == null) {
      return;
    }
    //now draw some text left justified
    FontMetrics fm = g.getFontMetrics(headerFont);
    int fontHeight = fm.getHeight();
    int fontWidth = fm.stringWidth(headerText);
    int descent = fm.getDescent();
    g.setColor(headerTextColor);
    g.setFont(headerFont);
    int textY = y1 - (descent + lineSpacing);
    g.drawString(headerText, x1, textY);

    //page number on right
    String pageInfo = "Page " + pageNumber + "/" + totalPageCount;
    int pInfoWidth = fm.stringWidth(pageInfo);
    g.drawString(pageInfo, x2-pInfoWidth, textY);

    //temp
    //((Graphics2D)g).draw(headerBbox);

//    g.setFont(orgFont);
//    g.setColor(orgColor);
  }

  public void drawFooter(Graphics g, Rectangle footerBbox, int pageNumber, int totalPageCount, int pageNumberLocation, PageFormat pf, double zoom) {
    //draw a line on the header bottom
    int x1 = (int) pf.getImageableX();
    int y1 = (int) footerBbox.getY();
    int x2 = (int) (pf.getWidth() - pf.getImageableX());
    int y2 = y1;
    g.setColor(lineColor);
    g.drawLine(x1, y1, x2, y2);

    if (footerText == null) {
      return;
    }
    //now draw some text left justified
    FontMetrics fm = g.getFontMetrics(footerFont);
    double footerHeight = fm.getHeight();
    g.setColor(footerTextColor);
    g.setFont(footerFont);
    g.drawString(footerText, x1, (int) ((double)y1 + (footerHeight/4.0)*3.0));

    //temp
    //((Graphics2D)g).draw(footerBbox);    
  }

  
    public void setPageHeaderAndFooter(PageHeaderFooterRenderer r) {
//      //System.out.println("setPageHeaderAndFooter()...totalPages=" + r.getTotalPageCount());
//      AtHeaderFooterElement header = new AtHeaderFooterElement(null, new Font("Helvetica", Font.BOLD, 20), Color.decode("#333366"), SystemColor.white, false, AtHeaderFooterElement.LEFT);
//      AtHeaderFooterElement footer = new AtHeaderFooterElement("Page " + r.getPageNumber() + "/" + r.getTotalPageCount(), new Font("Helvetica", Font.PLAIN, 10), Color.decode("#000033"), SystemColor.white, false, AtHeaderFooterElement.CENTER);
//      if (r.getPageNumber() == 1) {
//        header.setStr(headerText);
//      }
//      r.setHeader(header);
//      r.setFooter(footer);
    }
}
