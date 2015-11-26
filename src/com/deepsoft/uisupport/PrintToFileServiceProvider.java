/****************************************************************************
 *
 *  System        : 
 *  Module        : 
 *  Object Name   : $RCSfile$
 *  Revision      : $Revision$
 *  Date          : $Date$
 *  Author        : $Author$
 *  Created By    : Robert Heller
 *  Created       : Thu Nov 26 09:04:57 2015
 *  Last Modified : <151126.1220>
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

import javax.print.*;
import javax.print.attribute.*;
import javax.print.event.*;
import java.io.*;

import com.deepsoft.uisupport.*;

public class PrintToFileServiceProvider extends PrintServiceLookup {
    public PrintService[] getPrintServices(DocFlavor flavor,
              AttributeSet attributes) {
        System.err.println("*** PrintToFileServiceProvider.getPrintServices("+flavor+","+attributes+")");
        DocFlavor[] supported = PrintToFilePrinterFactory.getFlavors();
        for (int i = 0; i < supported.length; i++) {
            if (supported[i].equals(flavor)) {
                OutputStream s;
                try {
                    s = new FileOutputStream("test.ps");
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
                PrintService[] result = {new PrintToFileService(s)};
                return result;
            }
        }
        return null;
                
    }
    public PrintService[] getPrintServices() {
        System.err.println("*** PrintToFileServiceProvider.getPrintServices()");
        OutputStream s;
        try {
            s = new FileOutputStream("test.ps");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        PrintService[] result = {new PrintToFileService(s)};
        return result;
    }
    public MultiDocPrintService[] getMultiDocPrintServices(DocFlavor[] flavors,
              AttributeSet attributes) {
        return null;
    }
    public PrintService getDefaultPrintService() {
        System.err.println("*** PrintToFileServiceProvider.getDefaultPrintService()");
        OutputStream s;
        try {
            s = new FileOutputStream("test.ps");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return new PrintToFileService(s);
    }
}
