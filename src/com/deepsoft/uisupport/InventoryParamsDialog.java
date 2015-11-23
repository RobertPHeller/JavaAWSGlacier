/****************************************************************************
 *
 *  System        : 
 *  Module        : 
 *  Object Name   : $RCSfile$
 *  Revision      : $Revision$
 *  Date          : $Date$
 *  Author        : $Author$
 *  Created By    : Robert Heller
 *  Created       : Mon Nov 23 15:49:28 2015
 *  Last Modified : <151123.1701>
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
import javax.swing.*;
import javax.swing.text.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.imageio.ImageIO;

import com.amazonaws.services.glacier.model.InventoryRetrievalJobInput;

import com.deepsoft.*;
import com.deepsoft.uisupport.*;

public class InventoryParamsDialog extends JDialog implements ActionListener {
    private static Image iconImage = null;
    private boolean ok_clicked = false;
    public InventoryParamsDialog(Frame parent) {
        super(parent,"Inventory Parameters",true);
        
        if (iconImage == null) {
            try {
                iconImage = ImageIO.read(getClass().getResource("questhead.png"));
            } catch (IOException e) {
                System.err.println("Couldn't open icon Image");
                e.printStackTrace();
            }
        }
	GridBagLayout gblay = new GridBagLayout();
	GridBagConstraints c = new GridBagConstraints();
	this.setLayout(gblay);
        
        ImageIcon dialogIcon = new ImageIcon(iconImage);
        JLabel dialogIconLabel = new JLabel(dialogIcon);
        JLabel dialogHeading = new JLabel("Inventory Parameters");
	dialogHeading.setFont(new Font("Serif",Font.BOLD,16));
        c.anchor = GridBagConstraints.WEST;
        c.ipadx = 5;
        add(dialogIconLabel,c);
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.CENTER;
        add(dialogHeading,c);
        
        c.ipadx = 0;
        
        
        
        //int iopt = 1;
        //while (iopt < args.length) {
        //    if (args[iopt].compareTo("-startdate") == 0 &&
        //        (iopt+1) < args.length) {
        //        if (inventoryParams == null) {
        //            inventoryParams = new InventoryRetrievalJobInput();
        //        }
        //        inventoryParams.setStartDate(args[iopt+1]);
        //        iopt += 2;
        //    } else if (args[iopt].compareTo("-enddate") == 0 &&
        //              (iopt+1) < args.length) {
        //        if (inventoryParams == null) {
        //            inventoryParams = new InventoryRetrievalJobInput();
        //        }
        //        inventoryParams.setEndDate(args[iopt+1]);
        //        iopt += 2;
        //    } else if (args[iopt].compareTo("-marker") == 0 &&
        //              (iopt+1) < args.length) {
        //        if (inventoryParams == null) {
        //            inventoryParams = new InventoryRetrievalJobInput();
        //        }
        //        inventoryParams.setMarker(args[iopt+1]);
        //        iopt += 2;
        //    } else if (args[iopt].compareTo("-limit") == 0 &&
        //              (iopt+1) < args.length) {
        //        if (inventoryParams == null) {
        //            inventoryParams = new InventoryRetrievalJobInput();
        //        }
        //        inventoryParams.setLimit(args[iopt+1]);
        //        iopt += 2;
        //    } else if (args[iopt].compareTo("-format") == 0 &&
        //              (iopt+1) < args.length) {
        //        jobParams.setFormat(args[iopt+1]);
        //        iopt += 2;
        //    } else if (args[iopt].compareTo("-description") == 0 &&
        //              (iopt+1) < args.length) {
        //        jobParams.setDescription(args[iopt+1]);
        //        iopt += 2;
        //    } else {
        //        System.err.println("Unknown option: "+args[iopt]+", should be one of -startdate, -enddate, -marker, -limit, -format, -description, or -snstopic");
        //        Usage();
        //    }
        //}
        

        c.fill = GridBagConstraints.HORIZONTAL;c.weightx = 1.0;
        c.gridwidth = GridBagConstraints.REMAINDER;

        JPanel buttons = new JPanel();
        BoxLayout buttonslayout = new BoxLayout(buttons,BoxLayout.X_AXIS);
        JButton b = new JButton("OK");
        b.addActionListener(this);
        b.setActionCommand("OK");
        buttons.add(b,buttonslayout);
        b = new JButton("Cancel");
        b.addActionListener(this);
        b.setActionCommand("Cancel");
        buttons.add(b,buttonslayout);
        add(buttons,c);
        setSize(gblay.preferredLayoutSize(getContentPane()));
    }
    
    public InventoryRetrievalJobInput getParams() {
        return null;
    }
    
    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals("OK")) {
            ok_clicked = true;
            setVisible(false);
        } else if (e.getActionCommand().equals("Cancel")) {
            ok_clicked = false;
            setVisible(false);
        }
    }
    
    public InventoryRetrievalJobInput draw() {
        setVisible(true);
        if (ok_clicked) {
            return getParams();
        } else {
            return null;
        }
    }
    
}
