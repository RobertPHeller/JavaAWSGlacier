/****************************************************************************
 *
 *  System        : 
 *  Module        : 
 *  Object Name   : $RCSfile$
 *  Revision      : $Revision$
 *  Date          : $Date$
 *  Author        : $Author$
 *  Created By    : Robert Heller
 *  Created       : Tue Nov 24 11:14:14 2015
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
import javax.swing.JSpinner;
import javax.swing.SpinnerModel;
import javax.swing.JComponent;
import javax.swing.BoxLayout;
import java.awt.event.*;
import java.awt.Font;

public class LabelSpinBox extends JPanel {
    private JSpinner spinner;
    
    public LabelSpinBox(String label,SpinnerModel model) {
        BoxLayout b = new BoxLayout(this,BoxLayout.X_AXIS);
        this.setLayout(b);
        JLabel l = new JLabel(label);
        Font f = l.getFont();
        l.setFont(new Font(Font.MONOSPACED,f.getStyle(),f.getSize()));
        add(l,b);
        spinner = new JSpinner(model);
        add(spinner,b);
        l.setLabelFor(spinner);
    }
    public LabelSpinBox(String label) {this(label,null);}
    public Object getValue() {
        return spinner.getValue();
    }
    public void setValue(Object value) {
        spinner.setValue(value);
    }
    public SpinnerModel getModel() {
        return spinner.getModel();
    }
    public JComponent getEditor() {
        return spinner.getEditor();
    }
    public void setEditor(JComponent editor) {
        spinner.setEditor(editor);
    }
    public void setModel(SpinnerModel model) {
        spinner.setModel(model);
    }
}

