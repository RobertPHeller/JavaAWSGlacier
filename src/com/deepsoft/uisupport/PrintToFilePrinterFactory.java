/****************************************************************************
 *
 *  System        : 
 *  Module        : 
 *  Object Name   : $RCSfile$
 *  Revision      : $Revision$
 *  Date          : $Date$
 *  Author        : $Author$
 *  Created By    : Robert Heller
 *  Created       : Thu Nov 26 11:25:19 2015
 *  Last Modified : <151126.1227>
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

import java.io.OutputStream;
  
import javax.print.DocFlavor;
import javax.print.StreamPrintService;
import javax.print.StreamPrintServiceFactory;

import com.deepsoft.uisupport.*;

public class PrintToFilePrinterFactory extends StreamPrintServiceFactory {
    
    static final String psMimeType = "application/postscript";
    
    static final DocFlavor[] supportedDocFlavors = {
        //DocFlavor.INPUT_STREAM.POSTSCRIPT,
        DocFlavor.SERVICE_FORMATTED.PAGEABLE,
        DocFlavor.SERVICE_FORMATTED.PRINTABLE,
        //DocFlavor.BYTE_ARRAY.GIF,
        //DocFlavor.INPUT_STREAM.GIF,
        //DocFlavor.URL.GIF,
        //DocFlavor.BYTE_ARRAY.JPEG,
        //DocFlavor.INPUT_STREAM.JPEG,
        //DocFlavor.URL.JPEG,
        //DocFlavor.BYTE_ARRAY.PNG,
        //DocFlavor.INPUT_STREAM.PNG,
        //DocFlavor.URL.PNG,
    };
    
    public  String getOutputFormat() {
        return psMimeType;
    }
    
    public DocFlavor[] getSupportedDocFlavors() {
        return getFlavors();
    }
    
    static DocFlavor[] getFlavors() {
        DocFlavor[] flavors = new DocFlavor[supportedDocFlavors.length];
        System.arraycopy(supportedDocFlavors, 0, flavors, 0, flavors.length);
        return flavors;
    }
    
    public StreamPrintService getPrintService(OutputStream out) {
        return new PrintToFileService(out);
    } 
    
}
