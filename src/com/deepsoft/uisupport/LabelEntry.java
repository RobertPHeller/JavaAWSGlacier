/****************************************************************************
 *
 *  System        : 
 *  Module        : 
 *  Object Name   : $RCSfile$
 *  Revision      : $Revision$
 *  Date          : $Date$
 *  Author        : $Author$
 *  Created By    : Robert Heller
 *  Created       : Tue Nov 24 10:04:48 2015
 *  Last Modified : <151124.1201>
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
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.BoxLayout;
import java.awt.event.*;
import javax.swing.text.Document;
import java.awt.Font;

public class LabelEntry extends JPanel {
    private JTextField entry;
    
    public LabelEntry(String label,Document doc, String text, int columns) {
        BoxLayout b = new BoxLayout(this,BoxLayout.X_AXIS);
        this.setLayout(b);
        JLabel l = new JLabel(label);
        Font f = l.getFont();
        l.setFont(new Font(Font.MONOSPACED,f.getStyle(),f.getSize()));
        add(l,b);
        entry = new JTextField(doc, text, columns);
        add(entry,b);
        l.setLabelFor(entry);
    }
    public LabelEntry(String label) {
        this(label,null,null,0);
    }
    public LabelEntry(String label,int columns) {
        this(label,null,null,columns);
    }
    public LabelEntry(String label,String text) {
        this(label,null,text,0);
    }
    public LabelEntry(String label,String text, int columns) {
        this(label,null,text,columns);
    }
    public String getText() {
        return entry.getText();
    }
}
