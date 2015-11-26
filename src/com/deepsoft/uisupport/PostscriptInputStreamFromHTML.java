/****************************************************************************
 *
 *  System        : 
 *  Module        : 
 *  Object Name   : $RCSfile$
 *  Revision      : $Revision$
 *  Date          : $Date$
 *  Author        : $Author$
 *  Created By    : Robert Heller
 *  Created       : Wed Nov 25 11:07:59 2015
 *  Last Modified : <151125.1926>
 *
 *  Description	
 *
 *  Notes
 *
 *  History
 *	
 ****************************************************************************
 *
 *    Copyright (C) 2015  Robert Heller D/B/A Deepwoods Software
 *			51 Locke Hill Road
 *			Wendell, MA 01379-9728
 *
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 * 
 *
 ****************************************************************************/

package com.deepsoft.uisupport;

import java.io.*;
import java.util.*;
import java.lang.*;
import javax.imageio.ImageIO;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.*;
import javax.swing.text.html.parser.*;
import javax.swing.text.html.HTMLEditorKit;

import com.deepsoft.*;
import com.deepsoft.uisupport.*;


public class PostscriptInputStreamFromHTML extends InputStream {
    private StringBuilder postscriptDoc;
    private int position = 0;
    private static ParserDelegator parsedelegator = null;
    public boolean markSupported() {return false;}
    public int available() {
        //System.err.println("*** PostscriptInputStreamFromHTML.available(): postscriptDoc.length() is "+postscriptDoc.length()+", position is "+position);
        int avail = postscriptDoc.length() - position;
        //System.err.println("*** PostscriptInputStreamFromHTML.available(): avail is "+avail);
        return avail;
    }
    public int read() {
        //System.err.println("*** PostscriptInputStreamFromHTML.read(): postscriptDoc.length() is "+postscriptDoc.length()+", position is "+position);
        if (position < postscriptDoc.length()) {
            return postscriptDoc.charAt(position++);
        } else {
            return -1;
        }
    }
    
    public PostscriptInputStreamFromHTML(String HTMLText) {
        if (parsedelegator == null) parsedelegator = new ParserDelegator();
        postscriptDoc = new StringBuilder();
        position = 0;
        Formatter formatter = new Formatter(postscriptDoc, Locale.US);
        StringReader in = new StringReader(HTMLText);

        try {
            parsedelegator.parse(in,new HTMLCallbacks(postscriptDoc,formatter),
                      true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    class HTMLCallbacks extends HTMLEditorKit.ParserCallback {
        private StringBuilder postscriptDoc;
        private Formatter formatter;
        public HTMLCallbacks(StringBuilder postscriptDoc, Formatter formatter) {
            this.postscriptDoc = postscriptDoc;
            this.formatter     = formatter;
        }
        
        public void handleText(char[] data, int pos) {
            System.err.println("*** HTMLCallbacks.handleText(): data = "+data);
            String text = new String(data);
            System.err.println("*** HTMLCallbacks.handleText(): text = '"+text+"'");
            formatter.format("gsave (%s) show grestore 0 lineheight 1.2 mul neg rmoveto\n",text);
            
        }
        public void handleStartTag(HTML.Tag t,
                  MutableAttributeSet a,
                  int pos) {
            System.err.println("*** HTMLCallbacks.handleStartTag(): t = "+t);
            if (t == HTML.Tag.BODY) {
                formatter.format("%%!PS-Adobe-2.0\n%%%%Creator: PostscriptInputStreamFromHTML\n%%%%Title: PostscriptInputStreamFromHTML\n");
                formatter.format("%%%%CreationDate: %s\n",new Date().toString());
                formatter.format("%%%%Pages: 1\n");
                formatter.format("%%%%BoundingBox: 0 0 612 792\n");
                formatter.format("%%%%EndComments\n");
                formatter.format("%%%%BeginProlog\n");
                formatter.format("/inch {72 mul} def\n");
                formatter.format("/lineheight {12} def\n");
                formatter.format("%%%%EndProlog\n");
                formatter.format("%%%%Page: 1 1\n");
                formatter.format("/Times-Roman findfont lineheight scalefont setfont\n");
                formatter.format("1 inch 10 inch moveto\n");
                
            }
        }
        public void handleEndTag(HTML.Tag t,
                  int pos) {
            System.err.println("*** HTMLCallbacks.handleEndTag(): t = "+t);
            if (t == HTML.Tag.BODY) {
                formatter.format("showpage\n");
                formatter.format("%%%%Trailer\n");
                formatter.format("%%%%EOF\n");
            }
        }
        public void handleSimpleTag(HTML.Tag t,
                  MutableAttributeSet a,
                  int pos) {
            System.err.println("*** HTMLCallbacks.handleSimpleTag(): t = "+t);
        }
        public void handleError(String errorMsg,
                  int pos) {
        }
    }
}
