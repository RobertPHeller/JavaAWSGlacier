/****************************************************************************
 *
 *  System        : 
 *  Module        : 
 *  Object Name   : $RCSfile$
 *  Revision      : $Revision$
 *  Date          : $Date$
 *  Author        : $Author$
 *  Created By    : Robert Heller
 *  Created       : Fri Nov 20 16:18:48 2015
 *  Last Modified : <151120.1657>
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


package com.deepsoft;
import java.io.*;
import java.util.*;
import java.util.regex.*;
import java.lang.*;
import org.w3c.dom.*;
import com.amazonaws.services.glacier.TreeHashGenerator;
import com.amazonaws.services.glacier.model.ListPartsResult;
import com.amazonaws.services.glacier.model.ListMultipartUploadsResult;
import com.amazonaws.services.glacier.model.PartListElement;
import com.amazonaws.services.glacier.model.UploadListElement;
import com.amazonaws.services.glacier.model.InitiateJobResult;
import com.amazonaws.services.glacier.model.ListJobsResult;
import com.amazonaws.services.glacier.model.GlacierJobDescription;
import com.amazonaws.services.glacier.model.DescribeJobResult;
import com.amazonaws.services.glacier.model.InventoryRetrievalJobDescription;
import com.amazonaws.services.glacier.model.JobParameters;
import com.amazonaws.services.glacier.model.InventoryRetrievalJobInput;
import com.amazonaws.services.glacier.model.DescribeVaultResult;
import com.amazonaws.util.json.*;
import com.deepsoft.*;
import com.deepsoft.uisupport.*;

import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.SwingUtilities;
import java.awt.GridLayout;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;


import java.awt.event.*;

public class GlacierCommandGUI extends BackupVault implements Runnable, ActionListener, WindowListener {
    private static final Pattern vaultARNPattern = Pattern.compile("^arn:aws:glacier:([^:]+):(\\d+):vaults/([^/]+)$");
    String SNSTopic;
    File GlacierVaultDB_File;
    private String[] args;
    private JFrame mainFrame = null;
    public GlacierCommandGUI (File _GlacierVaultDB_File,String _SNSTopic) throws Exception {
        super(_GlacierVaultDB_File);
        GlacierVaultDB_File = _GlacierVaultDB_File;
        SNSTopic = _SNSTopic;
    }
    public final String MainHeading = "Glacier";
    public void run() {
        try {
            UIManager.setLookAndFeel(
                      UIManager.getSystemLookAndFeelClassName());
        } catch (UnsupportedLookAndFeelException ex) {
            System.err.println("Unable to load native look and feel");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        mainFrame = new JFrame ("GlacierCommand");
        StdMenuBar menuBar =  new StdMenuBar(this);
        mainFrame.setJMenuBar(menuBar);
        GridBagLayout lay = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        mainFrame.setLayout(lay);
        mainFrame.setSize(lay.preferredLayoutSize(mainFrame.getContentPane()));
        mainFrame.setLocationRelativeTo(null);
        mainFrame.setVisible(true);
    }
    
    public void actionPerformed (ActionEvent e) {
        if (e.getActionCommand().equals("Exit")) {
            CarefulExit();
        }            
    }
    public void windowClosing(WindowEvent e) {
        CarefulExit();
    }
    public void windowDeactivated(WindowEvent e) {
    }
    public void windowActivated(WindowEvent e) {
    }
    public void windowDeiconified(WindowEvent e) {
    }
    public void windowIconified(WindowEvent e) {
    }
    public void windowClosed(WindowEvent e) {
    }
    public void windowOpened(WindowEvent e) {
    }
    private void CarefulExit() {
        System.exit(0);
    }
    
    public static void main(String[] args) throws Exception {
        String dbfile = "/var/log/amanda/wendellfreelibrary/glacier.xml";
        String snstopic = "arn:aws:sns:us-east-1:647212794748:LibraryGlacier";
        int iopt = 0;
        while (iopt < args.length) {
            if (args[iopt].compareTo("-dbfile") == 0 &&
                (iopt+1) < args.length) {
                dbfile = args[iopt+1];
            } else if (args[iopt].compareTo("-snstopic") == 0 &&
                      (iopt+1) < args.length) {
                snstopic = args[iopt+1];
            } else if (args[iopt].matches("^-.*")) {
                System.err.println("Unknown option: "+args[iopt]+", should be one of -dbfile or -snstopic");
                Usage();
            } else {
                break;
            }
            iopt += 2;
        }
        SwingUtilities.invokeLater(new  GlacierCommandGUI(new File(dbfile),snstopic));
    }
    private static void Usage() {
        System.err.println("GlacierCommandGUI [opts] [command words...]");
        System.err.println("Where opts are:");
        System.err.println("    -dbfile dbfile");
        System.err.println("    -snstopic snstopic");
        System.exit(-1);
    }
}
