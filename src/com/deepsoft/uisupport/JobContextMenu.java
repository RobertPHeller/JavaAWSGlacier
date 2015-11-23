/****************************************************************************
 *
 *  System        : 
 *  Module        : 
 *  Object Name   : $RCSfile$
 *  Revision      : $Revision$
 *  Date          : $Date$
 *  Author        : $Author$
 *  Created By    : Robert Heller
 *  Created       : Mon Nov 23 10:33:57 2015
 *  Last Modified : <151123.1236>
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
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;

import java.awt.event.*;
import com.deepsoft.uisupport.*;

public class JobContextMenu extends JPopupMenu {
    private ActionListener al;
    private String vault;
    private String jobid;
    
    public ActionListener getActionListener() {return al;}
    public String getVault() {return vault;}
    public void setVault(String vault) {this.vault = vault;}
    public String getJobID() {return jobid;}
    public void setJobID(String jobid) {this.jobid = jobid;}
    
    public JobContextMenu(ActionListener al, String vault, String jobid) {
        this.vault = vault;
        this.jobid = jobid;
        this.al = al;
        JMenuItem mi;
        mi = add("Show");
        mi.setAccelerator(KeyStroke.getKeyStroke('S',
                  java.awt.event.InputEvent.CTRL_MASK));
        mi.setMnemonic(java.awt.event.KeyEvent.VK_S);
        mi.setActionCommand("ShowJob");
        mi.addActionListener(al);
        mi = add("List Job Output");
        mi.setAccelerator(KeyStroke.getKeyStroke('L',
                  java.awt.event.InputEvent.CTRL_MASK));
        mi.setMnemonic(java.awt.event.KeyEvent.VK_L);
        mi.setActionCommand("ListJobOutput");
        mi.addActionListener(al);
    }
}

