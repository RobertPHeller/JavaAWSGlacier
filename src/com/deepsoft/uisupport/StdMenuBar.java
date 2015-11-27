/****************************************************************************
 *
 *  System        : 
 *  Module        : 
 *  Object Name   : $RCSfile$
 *  Revision      : $Revision$
 *  Date          : $Date$
 *  Author        : $Author$
 *  Created By    : Robert Heller
 *  Created       : Fri Nov 20 16:45:50 2015
 *  Last Modified : <151127.1058>
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

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import java.awt.event.*;
import com.deepsoft.uisupport.*;

public class StdMenuBar extends JMenuBar {
    private ActionListener al;
    public ActionListener getActionListener() {return al;}
    public StdMenuBar(ActionListener al) {
        this.al = al;
        JMenuItem mi;
        JMenuRPH fileMenu = new JMenuRPH("File");
        fileMenu.setMnemonic(java.awt.event.KeyEvent.VK_F);
        add(fileMenu);
        mi = fileMenu.add("New");
        mi.setAccelerator(KeyStroke.getKeyStroke('N',java.awt.event.InputEvent.CTRL_MASK));
        mi.setActionCommand("New");
        mi.setMnemonic(java.awt.event.KeyEvent.VK_N);
        mi.addActionListener(al);
        mi.setEnabled(false);
        mi = fileMenu.add("Open...");
        mi.setAccelerator(KeyStroke.getKeyStroke('O',java.awt.event.InputEvent.CTRL_MASK));
        mi.setActionCommand("Open");
        mi.setMnemonic(java.awt.event.KeyEvent.VK_O);
        mi.addActionListener(al);
        mi.setEnabled(false);
        mi = fileMenu.add("Save");
        mi.setAccelerator(KeyStroke.getKeyStroke('S',java.awt.event.InputEvent.CTRL_MASK));
        mi.setActionCommand("Save");
        mi.setMnemonic(java.awt.event.KeyEvent.VK_S);
        mi.addActionListener(al);
        mi.setEnabled(false);
        mi = fileMenu.add("Save As...");
        mi.setAccelerator(KeyStroke.getKeyStroke('A',java.awt.event.InputEvent.CTRL_MASK));
        mi.setActionCommand("Save As");
        mi.setMnemonic(java.awt.event.KeyEvent.VK_A);
        mi.addActionListener(al);
        mi.setEnabled(false);
        mi = fileMenu.add("Export to PDF...");
        mi.setAccelerator(KeyStroke.getKeyStroke('E',java.awt.event.InputEvent.CTRL_MASK));
        mi.setActionCommand("ExportToPDF");
        mi.setMnemonic(java.awt.event.KeyEvent.VK_E);
        mi.addActionListener(al);
        mi = fileMenu.add("Print...");
        mi.setAccelerator(KeyStroke.getKeyStroke('P',java.awt.event.InputEvent.CTRL_MASK));
        mi.setActionCommand("Print");
        mi.setMnemonic(java.awt.event.KeyEvent.VK_P);
        mi.addActionListener(al);
        mi = fileMenu.add("Close");
        mi.setAccelerator(KeyStroke.getKeyStroke('C',java.awt.event.InputEvent.CTRL_MASK));
        mi.setActionCommand("Close");
        mi.setMnemonic(java.awt.event.KeyEvent.VK_C);
        mi.addActionListener(al);
        mi.setEnabled(false);
        mi = fileMenu.add("Exit");
        mi.setAccelerator(KeyStroke.getKeyStroke('Q',java.awt.event.InputEvent.CTRL_MASK));
        mi.setActionCommand("Exit");
        mi.setMnemonic(java.awt.event.KeyEvent.VK_X);
        mi.addActionListener(al);
        JMenuRPH editMenu = new JMenuRPH("Edit");
        editMenu.setMnemonic(java.awt.event.KeyEvent.VK_E);
        add(editMenu);
        mi = editMenu.add("Undo");
        mi.setAccelerator(KeyStroke.getKeyStroke('Z',java.awt.event.InputEvent.CTRL_MASK));
        mi.setMnemonic(java.awt.event.KeyEvent.VK_U);
        mi.setActionCommand("Undo");
        mi.addActionListener(al);
        mi.setEnabled(false);
        mi = editMenu.add("Cut");
        mi.setAccelerator(KeyStroke.getKeyStroke('X',java.awt.event.InputEvent.CTRL_MASK));
        mi.setMnemonic(java.awt.event.KeyEvent.VK_T);
        mi.setActionCommand("Cut");
        mi.addActionListener(al);
        mi.setEnabled(false);
        mi = editMenu.add("Copy");
        mi.setAccelerator(KeyStroke.getKeyStroke('C',java.awt.event.InputEvent.CTRL_MASK));
        mi.setMnemonic(java.awt.event.KeyEvent.VK_C);
        mi.setActionCommand("Copy");
        mi.addActionListener(al);
        mi.setEnabled(false);
        mi = editMenu.add("Paste");
        mi.setAccelerator(KeyStroke.getKeyStroke('V',java.awt.event.InputEvent.CTRL_MASK));
        mi.setMnemonic(java.awt.event.KeyEvent.VK_P);
        mi.setActionCommand("Paste");
        mi.addActionListener(al);
        mi.setEnabled(false);
        mi = editMenu.add("Clear");
        mi.setMnemonic(java.awt.event.KeyEvent.VK_L);
        mi.setActionCommand("Clear");
        mi.addActionListener(al);
        mi.setEnabled(false);
        mi = editMenu.add("Delete");
        mi.setMnemonic(java.awt.event.KeyEvent.VK_D);
        mi.setActionCommand("Delete");
        mi.addActionListener(al);
        mi.setEnabled(false);
        editMenu.addSeparator();
        mi = editMenu.add("Select All");
        mi.setAccelerator(KeyStroke.getKeyStroke('/',java.awt.event.InputEvent.CTRL_MASK));
        mi.setActionCommand("Select All");
        mi.addActionListener(al);
        mi.setEnabled(false);
        mi = editMenu.add("De-select All");
        mi.setAccelerator(KeyStroke.getKeyStroke('\\',java.awt.event.InputEvent.CTRL_MASK));
        mi.setActionCommand("De-select All");
        mi.addActionListener(al);
        mi.setEnabled(false);
        JMenuRPH viewMenu = new JMenuRPH("View");
        viewMenu.setMnemonic(java.awt.event.KeyEvent.VK_V);
        add(viewMenu);
        JMenuRPH optionsMenu = new JMenuRPH("Options");
        optionsMenu.setMnemonic(java.awt.event.KeyEvent.VK_O);
        add(optionsMenu);
        JMenuRPH helpMenu = new JMenuRPH("Help");
        helpMenu.setMnemonic(java.awt.event.KeyEvent.VK_H);
        mi = this.add(helpMenu);
        //this.setHelpMenu(helpMenu);
        mi = helpMenu.add("About");
        mi.setActionCommand("Help About");
        mi.addActionListener(al);
        helpMenu.addSeparator();
        mi = helpMenu.add("On Help...");
        mi.setMnemonic(java.awt.event.KeyEvent.VK_H);
        mi.setActionCommand("Help On Help");
        mi.addActionListener(al);
        mi = helpMenu.add("Contents");
        mi.setMnemonic(java.awt.event.KeyEvent.VK_I);
        mi.setActionCommand("Help Contents");
        mi.addActionListener(al);
        mi = helpMenu.add("Tutorials");
        mi.setMnemonic(java.awt.event.KeyEvent.VK_T);
        mi.setActionCommand("Help Tutorial");
        mi.addActionListener(al);
        mi = helpMenu.add("On Version");
        mi.setMnemonic(java.awt.event.KeyEvent.VK_V);
        mi.setActionCommand("Help On Version");
        mi.addActionListener(al);
    }
    public JMenuRPH add(JMenuRPH menu) {return (JMenuRPH)super.add(menu);}
    public JMenuRPH getMenu(String name) {
        int i;
        for (i = 0; i < getMenuCount() ; i++) {
            if (name == getMenu(i).getText()) return getMenu(i);
        }
        return null;
    }
    public JMenuRPH getMenu(int i) {
        return (JMenuRPH)super.getMenu(i);
    }
}

        
