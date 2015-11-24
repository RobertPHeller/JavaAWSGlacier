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
 *  Last Modified : <151124.1502>
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
    private LabelEntry startDate, endDate, marker;
    private LabelSpinBox  limit;
    private JOptionPane optionPane;
    private JButton b_ok, b_cancel;
    public InventoryParamsDialog(Frame parent) {
        super(parent,"Inventory Parameters",true);
        
        // Heading + questions:
        String headingString = "Inventory Parameters";
        startDate = new LabelEntry("Start Date: ");
        endDate =   new LabelEntry("End Date:   ");
        marker =    new LabelEntry("Marker:     ");
        limit =   new LabelSpinBox("Limit:      ",new SpinnerNumberModel(0,0,100000,1));
        Object[] header_questions = {headingString, startDate, endDate, marker, limit};
        
        // Option buttons:
        b_ok = new JButton("OK");
        b_ok.addActionListener(this);
        b_ok.setActionCommand("OK");
        b_cancel = new JButton("Cancel");
        b_cancel.addActionListener(this);
        b_cancel.setActionCommand("Cancel");
        Object[] option_buttons = {b_ok,b_cancel};
        
        optionPane = new JOptionPane(header_questions,
                  JOptionPane.QUESTION_MESSAGE,
                  JOptionPane.YES_NO_OPTION,
                  null,
                  option_buttons,
                  b_ok);
        
        setContentPane(optionPane);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
                  public void windowClosing(WindowEvent we) {
                  ok_clicked = false;
                  setVisible(false);
              }
              });
        Dimension psize = getPreferredSize();
        System.err.println("*** InventoryParamsDialog.InventoryParamsDialog(): psize = "+psize);
        psize.setSize(psize.getWidth()+35,psize.getHeight()+35);
        setSize(psize);
        setLocationRelativeTo(parent);
    }
    
    public InventoryRetrievalJobInput getParams() {
        InventoryRetrievalJobInput result = new InventoryRetrievalJobInput();
        String startdate = startDate.getText();
        if (startdate != null || !startdate.equals("")) {
            result.setStartDate(startdate);
        }
        String enddate = endDate.getText();
        if (enddate != null || !enddate.equals("")) {
            result.setEndDate(enddate);
        }
        String markerstr = marker.getText();
        if (markerstr != null || !markerstr.equals("")) {
            result.setMarker(markerstr);
        }
        Integer limitval = (Integer) limit.getValue();
        if (limitval > 0) {
            result.setLimit(limitval.toString());
        }
        System.err.println("*** InventoryParamsDialog.getParams(): result is "+result);
        return result;
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
        ok_clicked = false;
        setVisible(true);
        if (ok_clicked) {
            return getParams();
        } else {
            return null;
        }
    }
    
}
