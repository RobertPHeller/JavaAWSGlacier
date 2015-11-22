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
 *  Last Modified : <151122.1703>
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

import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeSelectionModel;
import javax.swing.tree.TreePath;
//import javax.swing.event.TreeSelectionEvent;
//import javax.swing.event.TreeSelectionListener;

import javax.swing.JMenuItem;
import javax.swing.JEditorPane;
import javax.swing.JSplitPane;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.SwingUtilities;
import java.awt.GridLayout;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Dimension;
import java.awt.Component;
import java.awt.event.MouseListener;


import java.awt.event.*;

public class GlacierCommandGUI extends BackupVault implements Runnable, ActionListener, WindowListener, MouseListener {
    private static final Pattern vaultARNPattern = Pattern.compile("^arn:aws:glacier:([^:]+):(\\d+):vaults/([^/]+)$");
    String SNSTopic;
    File GlacierVaultDB_File;
    private String[] args;
    private JFrame mainFrame = null;
    private DefaultMutableTreeNode vaultTree = null;
    private JTree tree = null;
    private JEditorPane displayPane = null;
    private VaultContextMenu vmenu;
    private ArchiveContextMenu amenu;
    public GlacierCommandGUI (File _GlacierVaultDB_File,String _SNSTopic) throws Exception {
        super(_GlacierVaultDB_File);
        GlacierVaultDB_File = _GlacierVaultDB_File;
        SNSTopic = _SNSTopic;
        vaultTree = new DefaultMutableTreeNode(GlacierVaultDB_File.getName());
        createNodes(vaultTree);
    }
    private void createNodes(DefaultMutableTreeNode top) {
        DefaultMutableTreeNode vaulttree = null;
        DefaultMutableTreeNode archivetree = null;
        Element vaultsnode = getvaultnode();
        NodeList vaults = vaultsnode.getElementsByTagName("vault");
        for (int i=0; i < vaults.getLength(); i++) {
            Element vault = (Element) vaults.item(i);
            System.err.println("*** GlacierCommandGUI.createNodes(): vault.getAttribute(\"name\") is "+vault.getAttribute("name"));
            vaulttree = new DefaultMutableTreeNode(vault.getAttribute("name"));
            top.add(vaulttree);
            NodeList archives = vault.getElementsByTagName("archive");
            for (int j=0; j < archives.getLength();j++) {
                Element a = (Element) archives.item(j);
                NodeList dtags = a.getElementsByTagName("description");
                String descr = a.getAttribute("archiveid");
                if (dtags != null && dtags.getLength() > 0) {
                    Element dtag = (Element) dtags.item(0);
                    descr = dtag.getTextContent();
                }
                System.err.println("*** GlacierCommandGUI.createNodes(): descr = "+descr);
                archivetree = new DefaultMutableTreeNode(descr,false);
                vaulttree.add(archivetree);
            }
        }
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
        tree = new JTree(vaultTree);
        tree.getSelectionModel().
              setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.addMouseListener(this);
        JScrollPane treeView = new JScrollPane(tree);
        Dimension minimumSize = new Dimension(100, 50);
        //Dimension preferedSize = new Dimension(900,250);
        treeView.setMinimumSize(minimumSize);
        //treeView.setPreferredSize(preferedSize);
        displayPane = new JEditorPane();
        displayPane.setEditable(false);
        displayPane.setContentType("text/html");
        JScrollPane displayView = new JScrollPane(displayPane);
        displayView.setMinimumSize(minimumSize);
        //displayView.setPreferredSize(preferedSize);
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setTopComponent(treeView);
        splitPane.setBottomComponent(displayView);
        splitPane.setDividerLocation(100);
        c.fill   = GridBagConstraints.BOTH;
        c.weightx = 1.0;
        c.weighty = 1.0;
        splitPane.setPreferredSize(new Dimension(900,500));
        mainFrame.add(splitPane, c);
        mainFrame.setSize(lay.preferredLayoutSize(mainFrame.getContentPane()));
        mainFrame.setLocationRelativeTo(null);
        mainFrame.setVisible(true);
        vmenu = new VaultContextMenu(this,"");
        amenu = new ArchiveContextMenu(this,"","");
    }
    
    public void actionPerformed (ActionEvent e) {
        //System.err.println("*** GlacierCommandGUI.actionPerformed("+e+")");
        if (e.getActionCommand().equals("Exit")) {
            CarefulExit();
        } else if (e.getActionCommand().equals("ShowArchivesOfVault")) {
            JMenuItem mi = (JMenuItem) e.getSource();
            VaultContextMenu v = (VaultContextMenu) mi.getParent();
            //System.err.println("*** GlacierCommandGUI.actionPerformed(): v = "+v);
            String vault = v.getVault();
            //System.err.println("*** GlacierCommandGUI.actionPerformed("+e.getActionCommand()+"): vault = "+vault);
            showvault(vault);
        } else if (e.getActionCommand().equals("ShowJobsOfVault")) {
            JMenuItem mi = (JMenuItem) e.getSource();
            VaultContextMenu v = (VaultContextMenu) mi.getParent();
            System.err.println("*** GlacierCommandGUI.actionPerformed(): v = "+v);
            String vault = v.getVault();
            System.err.println("*** GlacierCommandGUI.actionPerformed("+e.getActionCommand()+"): vault = "+vault);
        } else if (e.getActionCommand().equals("DeleteVault")) {
            JMenuItem mi = (JMenuItem) e.getSource();
            VaultContextMenu v = (VaultContextMenu) mi.getParent();
            System.err.println("*** GlacierCommandGUI.actionPerformed(): v = "+v);
            String vault = v.getVault();
            System.err.println("*** GlacierCommandGUI.actionPerformed("+e.getActionCommand()+"): vault = "+vault);
        } else if (e.getActionCommand().equals("ShowUploads")) {
            JMenuItem mi = (JMenuItem) e.getSource();
            VaultContextMenu v = (VaultContextMenu) mi.getParent();
            System.err.println("*** GlacierCommandGUI.actionPerformed(): v = "+v);
            String vault = v.getVault();
            System.err.println("*** GlacierCommandGUI.actionPerformed("+e.getActionCommand()+"): vault = "+vault);
        } else if (e.getActionCommand().equals("ShowArchive")) {
            JMenuItem mi = (JMenuItem) e.getSource();
            ArchiveContextMenu a = (ArchiveContextMenu) mi.getParent();
            String vault = a.getVault();
            String archive = a.getArchive();
            System.err.println("*** GlacierCommandGUI.actionPerformed("+e.getActionCommand()+"): vault = "+vault+", archive = "+archive);
        } else if (e.getActionCommand().equals("DeleteArchive")) {
            JMenuItem mi = (JMenuItem) e.getSource();
            ArchiveContextMenu a = (ArchiveContextMenu) mi.getParent();
            String vault = a.getVault();
            String archive = a.getArchive();
            System.err.println("*** GlacierCommandGUI.actionPerformed("+e.getActionCommand()+"): vault = "+vault+", archive = "+archive);
        } else if (e.getActionCommand().equals("GetArchive")) {
            JMenuItem mi = (JMenuItem) e.getSource();
            ArchiveContextMenu a = (ArchiveContextMenu) mi.getParent();
            String vault = a.getVault();
            String archive = a.getArchive();
            System.err.println("*** GlacierCommandGUI.actionPerformed("+e.getActionCommand()+"): vault = "+vault+", archive = "+archive);
        }
    }
    private void showvault(String vault) {
        Element vnode = findvaultbyname (vault);
        NodeList archives = vnode.getElementsByTagName("archive");
        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb, Locale.US);
        formatter.format("<html><body><h3>Vault: %s:</h3><ol>\n",vnode.getAttribute("name"));
        long totalsize = 0;
        int index = 0;
        for (int j=0; j < archives.getLength();j++) {
            index++;
            Element a = (Element) archives.item(j);
            NodeList dtags = a.getElementsByTagName("description");
            String descr = a.getAttribute("archiveid");
            if (dtags != null && dtags.getLength() > 0) {
                Element dtag = (Element) dtags.item(0);
                descr = dtag.getTextContent();
            }
            formatter.format("<li>%s:<br />\n",index,descr);
            long size = 0;
            NodeList sizes = a.getElementsByTagName("size");
            if (sizes != null && sizes.getLength() > 0) {
                Element sizeelt = (Element) sizes.item(0);
                size = Long.parseLong(sizeelt.getTextContent());
            }
            String sha256th = "";
            NodeList sha256ths = a.getElementsByTagName("sha256treehash");
            if (sha256ths != null && sha256ths.getLength() > 0) {
                Element sha256thelt = (Element) sha256ths.item(0);
                sha256th = sha256thelt.getTextContent();
            }
            formatter.format("Date: %s<br />\n",a.getAttribute("date"));
            formatter.format("Size: %s<br />\n",Humansize(size));
            formatter.format("Tree hash: %s<br />\n",sha256th);
            totalsize += size;
            formatter.format("</li>");
        }
        formatter.format("</ol>Total size: %s byte%s, in %d archive%s.<br /></body></html>\n",
                  Humansize(totalsize),(totalsize != 1)?"s":"",index,
                  (index != 1)?"s":"");
        displayPane.setText(sb.toString());
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
    //public void valueChanged(TreeSelectionEvent e) {
    //    System.err.println("*** GlacierCommandGUI.valueChanged("+e+")");
    //}
    public void mousePressed(MouseEvent e) {
        //System.err.println("*** GlacierCommandGUI.mousePressed("+e+")");
        if (e.getButton() != 3) return;
        int selRow = tree.getRowForLocation(e.getX(), e.getY());
        TreePath selPath = tree.getPathForLocation(e.getX(), e.getY());
        //System.err.println("*** GlacierCommandGUI.mousePressed(): selRow = "+selRow);
        //System.err.println("*** GlacierCommandGUI.mousePressed(): selPath = "+selPath);
        switch (selPath.getPathCount()) {
        case 1: return;
        case 2: vaultContextMenu(selPath.getPathComponent(1).toString(),
                  e.getComponent(),e.getX(), e.getY()); break;
        case 3: archiveContextMenu(selPath.getPathComponent(1).toString(),
                  selPath.getPathComponent(2).toString(),e.getComponent(),
                  e.getX(), e.getY());
        }
    }
    private void vaultContextMenu(String vaultName,Component c, int x, int y) {
        System.err.println("*** GlacierCommandGUI.vaultContextMenu("+vaultName+")");
        vmenu.setVault(vaultName);
        vmenu.show(c,x,y);
    }
    private void archiveContextMenu(String vaultName, String archiveName,Component c, int x, int y) {
        System.err.println("*** GlacierCommandGUI.archiveContextMenu("+vaultName+","+archiveName+")");
        amenu.setVault(vaultName);
        amenu.setArchive(archiveName);
        amenu.show(c,x,y);
    }
    public  void mouseClicked(MouseEvent e) { }
    public  void mouseEntered(MouseEvent e) { }
    public void mouseExited(MouseEvent e) { }
    public void mouseReleased(MouseEvent e) { }
    private void CarefulExit() {
        System.exit(0);
    }
    private final long oneK = 1024;
    private final double oneKD = (double) oneK;
    private final long oneM = 1024*1024;
    private final double oneMD = (double) oneM;
    private final long oneG = 1024*1024*1024;
    private final double oneGD = (double) oneG;
    private final long oneT = oneM*oneM;
    private final double oneTD = (double) oneT;
    private String Humansize(long s) {
        //System.err.printf("*** GlacierCommand.Humansize(%d)\n",s);
        Formatter f = new Formatter();
        if (s < oneK) {
            f.format("%d",s);
            return f.toString();
        } else if (s < oneM) {
            f.format("%5.1fK",(double)s / oneKD);
            return f.toString();
        } else if (s < oneG) {
            f.format("%5.1fM",(double)s / oneMD);
            return f.toString();
        } else if (s < oneT) {
            f.format("%5.1fG",(double)s / oneGD);
            return f.toString();
        } else {
            f.format("%5.1fT",(double)s / oneTD);
            return f.toString();
        }
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
