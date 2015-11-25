/****************************************************************************
 *
 *  System        : 
 *  Module        : 
 *  Object Name   : $RCSfile$
 *  Revision      : $Revision$
 *  Date          : $Date$
 *  Author        : $Author$
 *  Created By    : Robert Heller
 *  Created       : Wed Nov 25 09:28:06 2015
 *  Last Modified : <151125.1549>
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
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.imageio.ImageIO;

import javax.print.*;
import javax.print.attribute.*;

import com.deepsoft.*;
import com.deepsoft.uisupport.*;

public class SelectPrinterDialog extends JDialog implements ActionListener {
    private static Image iconImage = null;
    private boolean print_clicked = false;
    private JOptionPane optionPane;
    private JButton b_print, b_cancel;
    private JList printlist;
    
    public SelectPrinterDialog(Frame parent) {
        super(parent,"Select Printer",true);
        
        if (iconImage == null) {
            try {
                iconImage = ImageIO.read(getClass().getResource("Printer.png"));
            } catch (IOException e) {
                System.err.println("Couldn't open printer icon");
                e.printStackTrace();
            }
        }
        
        // Heading + questions:
        String headingString = "Select Printer";
        printlist = new JList();
        printlist.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        printlist.setCellRenderer(new PrinterCellRenderer());
        Object[] header_questions = {headingString, printlist};
        
        // Option buttons:
        b_print = new JButton("Print");
        b_print.addActionListener(this);
        b_print.setActionCommand("Print");
        b_cancel = new JButton("Cancel");
        b_cancel.addActionListener(this);
        b_cancel.setActionCommand("Cancel");
        Object[] option_buttons = {b_print,b_cancel};
        
        ImageIcon dialogIcon = new ImageIcon(iconImage);
        
        optionPane = new JOptionPane(header_questions,
                  JOptionPane.QUESTION_MESSAGE,
                  JOptionPane.YES_NO_OPTION,
                  dialogIcon,
                  option_buttons,
                  b_print);
        setContentPane(optionPane);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
                  public void windowClosing(WindowEvent we) {
                  print_clicked = false;
                  setVisible(false);
              }
              });
        Dimension psize = getPreferredSize();
        System.err.println("*** SelectPrinterDialog.SelectPrinterDialog(): psize = "+psize);
        psize.setSize(psize.getWidth()+35,psize.getHeight()+35);
        setSize(psize);
        setLocationRelativeTo(parent);
    }
    
    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals("Print")) {
            print_clicked = true;
            setVisible(false);
        } else if (e.getActionCommand().equals("Cancel")) {
            print_clicked = false;
            setVisible(false);
        }
    }
    
    public PrintService draw(DocFlavor flavor, AttributeSet attributes) {
        print_clicked = false;
        System.err.println("*** SelectPrinterDialog.draw(): flavor is "+flavor);
        PrintService printers[] = PrintServiceLookup.lookupPrintServices(flavor, attributes);
        System.err.println("*** SelectPrinterDialog.draw(): printers.length = "+printers.length);
        for (int i = 0; i < printers.length; i++) {
            System.err.println("*** SelectPrinterDialog.draw(): printers["+i+"] is "+printers[i].getName());
        }
        printlist.setListData(printers);
        printlist.setSelectedIndex(0);              
        setVisible(true);
        if (print_clicked) {
            return (PrintService) printlist.getSelectedValue();
        } else {
            return null;
        }
    }
    class PrinterCellRenderer extends JLabel implements ListCellRenderer {
        public Component getListCellRendererComponent(
                  JList list,              // the list
                  Object value,            // value to display
                  int index,               // cell index
                  boolean isSelected,      // is the cell selected
                  boolean cellHasFocus)    // does the cell have focus
        {
            PrintService p = (PrintService) value;
            String s = p.getName();
            setText(s);
            if (isSelected) {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            } else {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            }
            setEnabled(list.isEnabled());
            setFont(list.getFont());
            setOpaque(true);
            return this;
        }
    }
}

