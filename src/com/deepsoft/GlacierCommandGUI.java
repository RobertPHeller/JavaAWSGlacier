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
 *  Last Modified : <151123.1655>
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

import javax.imageio.ImageIO;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JToolBar;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeSelectionModel;
import javax.swing.tree.TreePath;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.JTabbedPane;
import javax.swing.JCheckBox;
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
import java.awt.FlowLayout;
import java.awt.Font;

import java.awt.Dimension;
import java.awt.Component;
import java.awt.event.MouseListener;
import java.awt.Image;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import java.awt.event.*;

public class GlacierCommandGUI extends BackupVault implements Runnable, ActionListener, WindowListener, MouseListener {
    private static final Pattern vaultARNPattern = Pattern.compile("^arn:aws:glacier:([^:]+):(\\d+):vaults/([^/]+)$");
    String SNSTopic;
    File GlacierVaultDB_File;
    private String[] args;
    private JFrame mainFrame = null;
    private DefaultMutableTreeNode vaultTree = null;
    private JTree archiveTree = null;
    private JTree jobTree = null;
    private JTree uploadTree = null;
    private JEditorPane displayPane = null;
    private VaultContextMenu vmenu;
    private ArchiveContextMenu amenu;
    private JobContextMenu jmenu;
    private UploadContextMenu umenu;
    private static Image mainImage   = null;
    private JTabbedPane tabPane;
    private InventoryParamsDialog invParams = null;
    
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
            //System.err.println("*** GlacierCommandGUI.createNodes(): vault.getAttribute(\"name\") is "+vault.getAttribute("name"));
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
                //System.err.println("*** GlacierCommandGUI.createNodes(): descr = "+descr);
                archivetree = new DefaultMutableTreeNode(descr,false);
                vaulttree.add(archivetree);
            }
        }
    }
    public final String MainHeading = "Glacier Command";
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
        
        if (mainImage == null)
        {
            try {
                mainImage   = ImageIO.read(getClass().getResource("GlacierCommand.png"));
            } catch (IOException e) {
                System.err.println("Couldn't open main Image");
                e.printStackTrace();
            }
        }
        
        mainFrame = new JFrame ("GlacierCommand");
        StdMenuBar menuBar =  new StdMenuBar(this);
        mainFrame.setJMenuBar(menuBar);
        
        JToolBar optionsPanel = new JToolBar();
        optionsPanel.setPreferredSize(new Dimension(200,50));
        optionsPanel.setLayout(new FlowLayout());
        
        JButton printButton = new JButton("Print");
        printButton.setActionCommand("Print");
        printButton.addActionListener(this);
        JButton exitButton = new JButton("Exit");
        exitButton.setActionCommand("Exit");
        exitButton.addActionListener(this);
        
        optionsPanel.add(printButton);
        optionsPanel.add(exitButton);


        GridBagLayout lay = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        mainFrame.setLayout(lay);
        ImageIcon mainIcon = new ImageIcon(mainImage);
        JLabel mainIconLabel = new JLabel(mainIcon);
        c.anchor = GridBagConstraints.WEST;
        mainFrame.add(mainIconLabel,c);
        c.gridwidth = GridBagConstraints.REMAINDER;
        JLabel mainHeading = new JLabel(MainHeading);
        mainHeading.setFont(new Font("Serif",Font.BOLD,24));
        c.anchor = GridBagConstraints.CENTER;
        mainFrame.add(mainHeading,c);
        c.fill   = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        mainFrame.add(optionsPanel, c);
        
        tabPane = new JTabbedPane();
        tabPane.setPreferredSize(new Dimension(900,500));
        c.fill   = GridBagConstraints.BOTH;
        c.weighty = 1.0;

        archiveTree = new JTree(vaultTree);
        archiveTree.getSelectionModel().
              setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        archiveTree.addMouseListener(this);
        JScrollPane archiveTreeView = new JScrollPane(archiveTree);
        Dimension minimumSize = new Dimension(100, 50);
        //Dimension preferedSize = new Dimension(900,250);
        archiveTreeView.setMinimumSize(minimumSize);
        //archiveTreeView.setPreferredSize(preferedSize);
        tabPane.add("Vault Tree",archiveTreeView);
        displayPane = new JEditorPane();
        displayPane.setEditable(false);
        displayPane.setContentType("text/html");
        JScrollPane displayView = new JScrollPane(displayPane);
        displayView.setMinimumSize(minimumSize);
        //displayView.setPreferredSize(preferedSize);
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setTopComponent(tabPane);
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
        jmenu = new JobContextMenu(this,"","");
        umenu = new UploadContextMenu(this,"","");
    }
    
    public void actionPerformed (ActionEvent e) {
        //System.err.println("*** GlacierCommandGUI.actionPerformed("+e+")");
        if (e.getActionCommand().equals("Exit")) {
            CarefulExit();
        } else if (e.getActionCommand().equals("Print")) {
            //PrintWhatDialog();
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
            //System.err.println("*** GlacierCommandGUI.actionPerformed(): v = "+v);
            String vault = v.getVault();
            //System.err.println("*** GlacierCommandGUI.actionPerformed("+e.getActionCommand()+"): vault = "+vault);
            showjobs(vault);
        } else if (e.getActionCommand().equals("DeleteVault")) {
            JMenuItem mi = (JMenuItem) e.getSource();
            VaultContextMenu v = (VaultContextMenu) mi.getParent();
            //System.err.println("*** GlacierCommandGUI.actionPerformed(): v = "+v);
            String vault = v.getVault();
            //System.err.println("*** GlacierCommandGUI.actionPerformed("+e.getActionCommand()+"): vault = "+vault);
            _deletevault(vault);
        } else if (e.getActionCommand().equals("ShowUploads")) {
            JMenuItem mi = (JMenuItem) e.getSource();
            VaultContextMenu v = (VaultContextMenu) mi.getParent();
            //System.err.println("*** GlacierCommandGUI.actionPerformed(): v = "+v);
            String vault = v.getVault();
            //System.err.println("*** GlacierCommandGUI.actionPerformed("+e.getActionCommand()+"): vault = "+vault);
            showuploads(vault);
        } else if (e.getActionCommand().equals("StartInventoryListJob")) {
            JMenuItem mi = (JMenuItem) e.getSource();
            VaultContextMenu v = (VaultContextMenu) mi.getParent();
            //System.err.println("*** GlacierCommandGUI.actionPerformed(): v = "+v);
            String vault = v.getVault();
            //System.err.println("*** GlacierCommandGUI.actionPerformed("+e.getActionCommand()+"): vault = "+vault);
            startInventoryRetrievalJob(vault);
        } else if (e.getActionCommand().equals("ShowArchive")) {
            JMenuItem mi = (JMenuItem) e.getSource();
            ArchiveContextMenu a = (ArchiveContextMenu) mi.getParent();
            String vault = a.getVault();
            String archive = a.getArchive();
            //System.err.println("*** GlacierCommandGUI.actionPerformed("+e.getActionCommand()+"): vault = "+vault+", archive = "+archive);
            showarchive(vault,archive);
        } else if (e.getActionCommand().equals("DeleteArchive")) {
            JMenuItem mi = (JMenuItem) e.getSource();
            ArchiveContextMenu a = (ArchiveContextMenu) mi.getParent();
            String vault = a.getVault();
            String archive = a.getArchive();
            //System.err.println("*** GlacierCommandGUI.actionPerformed("+e.getActionCommand()+"): vault = "+vault+", archive = "+archive);
            _deletearchive(vault,archive);
        } else if (e.getActionCommand().equals("GetArchive")) {
            JMenuItem mi = (JMenuItem) e.getSource();
            ArchiveContextMenu a = (ArchiveContextMenu) mi.getParent();
            String vault = a.getVault();
            String archive = a.getArchive();
            //System.err.println("*** GlacierCommandGUI.actionPerformed("+e.getActionCommand()+"): vault = "+vault+", archive = "+archive);
            getarchive(vault,archive);
        } else if (e.getActionCommand().equals("ShowJob")) {
            JMenuItem mi = (JMenuItem) e.getSource();
            JobContextMenu a = (JobContextMenu) mi.getParent();
            String vault = a.getVault();
            String jobid = a.getJobID();
            //System.err.println("*** GlacierCommandGUI.actionPerformed("+e.getActionCommand()+"): vault = "+vault+", archive = "+archive);
            showjob(vault,jobid);
        } else if (e.getActionCommand().equals("ListJobOutput")) {
            JMenuItem mi = (JMenuItem) e.getSource();
            JobContextMenu a = (JobContextMenu) mi.getParent();
            String vault = a.getVault();
            String jobid = a.getJobID();
            //System.err.println("*** GlacierCommandGUI.actionPerformed("+e.getActionCommand()+"): vault = "+vault+", archive = "+archive);
            listjoboutput(vault,jobid);
        } else if (e.getActionCommand().equals("ShowUpload")) {
            JMenuItem mi = (JMenuItem) e.getSource();
            UploadContextMenu u = (UploadContextMenu) mi.getParent();
            String vault = u.getVault();
            String uploadId = u.getUploadID();
            showupload(vault,uploadId);
        } else if (e.getActionCommand().equals("ShowParts")) {
            JMenuItem mi = (JMenuItem) e.getSource();
            UploadContextMenu u = (UploadContextMenu) mi.getParent();
            String vault = u.getVault();
            String uploadId = u.getUploadID();
            showparts(vault,uploadId);
        } else if (e.getActionCommand().equals("AbortUpload")) {
            JMenuItem mi = (JMenuItem) e.getSource();
            UploadContextMenu u = (UploadContextMenu) mi.getParent();
            String vault = u.getVault();
            String uploadId = u.getUploadID();
            abortupload(vault,uploadId);
        } else {
            System.err.println("*** GlacierCommandGUI.actionPerformed("+e.getActionCommand()+")");
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
    private void showjobs(String vault) {
        DefaultMutableTreeNode jobs = new DefaultMutableTreeNode(vault);
        DefaultMutableTreeNode jobTN = null;
        String args[] = {};
        try {
            ListJobsResult ljResult = GetJobList(vault,args);
            java.util.List<GlacierJobDescription> jobList = ljResult.getJobList();
            if (jobList != null) {
                Iterator itr = jobList.iterator();
                while (itr.hasNext()) {
                    GlacierJobDescription job = (GlacierJobDescription)itr.next();
                    jobTN = new DefaultMutableTreeNode(job.getJobId(),false);
                    jobs.add(jobTN);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (jobs.getChildCount() == 0) {
            return;
        }            
        if (jobTree == null) {
            jobTree = new JTree(jobs);
            jobTree.getSelectionModel().
                  setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
            jobTree.addMouseListener(this);
            JScrollPane jobTreeView = new JScrollPane(jobTree);
            Dimension minimumSize = new Dimension(100, 50);
            jobTreeView.setMinimumSize(minimumSize);
            tabPane.add("Job Tree for vault "+vault,jobTreeView);
        } else {
            DefaultTreeModel tm = (DefaultTreeModel) jobTree.getModel();
            tm.setRoot(jobs);
            for (int i = 1; i < tabPane.getTabCount(); i++) {
                String tt = tabPane.getTitleAt(i);
                if (tt.startsWith("Job Tree for vault ")) {
                    tabPane.setTitleAt(i,"Job Tree for vault "+vault);
                    break;
                }
            }
        }
    }
    private void _deletevault(String vault) {
        if (getyesno("Really delete vault "+vault+"?")) {
            try {
                String response = super.deletevault(vault);
                if (response != null && response != "") {
                    savedb(GlacierVaultDB_File);
                    for (int i = 0; i < vaultTree.getChildCount(); i++) {
                        DefaultMutableTreeNode vnode = (DefaultMutableTreeNode) vaultTree.getChildAt(i);
                        if (vnode.toString().equals(vault)) {
                            vaultTree.remove(i);
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    private void showuploads(String vault) {
        DefaultMutableTreeNode uploads = new DefaultMutableTreeNode(vault);
        DefaultMutableTreeNode uploadTN = null;
        try {
            ListMultipartUploadsResult _uploads = ListMultiPartUploads(vault);
            java.util.List<UploadListElement> uploadsList = _uploads.getUploadsList();
            if (uploadsList != null) {
                Iterator itr = uploadsList.iterator();
                while (itr.hasNext()) {
                    UploadListElement upload = (UploadListElement)itr.next();
                    String multipartUploadId = upload.getMultipartUploadId();
                    uploadTN = new DefaultMutableTreeNode(multipartUploadId,false);
                    uploads.add(uploadTN);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (uploads.getChildCount() == 0) {
            return;
        }
        if (uploadTree == null) {
            uploadTree = new JTree(uploads);
            uploadTree.getSelectionModel().
                  setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
            uploadTree.addMouseListener(this);
            JScrollPane uploadTreeView = new JScrollPane(uploadTree);
            Dimension minimumSize = new Dimension(100, 50);
            uploadTreeView.setMinimumSize(minimumSize);
            tabPane.add("Uploads Tree for vault "+vault,uploadTreeView);
        } else {
            DefaultTreeModel tm = (DefaultTreeModel) uploadTree.getModel();
            tm.setRoot(uploads);
            for (int i = 1; i < tabPane.getTabCount(); i++) {
                String tt = tabPane.getTitleAt(i);
                if (tt.startsWith("Uploads Tree for vault")) {
                    tabPane.setTitleAt(i,"Uploads Tree for vault "+vault);
                    break;
                }
            }
        }
    }
    private void startInventoryRetrievalJob(String vaultName) {
        JobParameters jobParams = new JobParameters()
              .withType("inventory-retrieval")
              .withSNSTopic(SNSTopic);
        //InventoryRetrievalJobInput inventoryParams = null;
        if (invParams == null) {
            invParams = new InventoryParamsDialog(mainFrame);
        }
        InventoryRetrievalJobInput inventoryParams = invParams.draw();
        if (inventoryParams != null) {
            jobParams.setInventoryRetrievalParameters(inventoryParams);
            try {
                String jobId = InitiateRetrieveInventory(vaultName,jobParams);
                StringBuilder sb = new StringBuilder();
                Formatter formatter = new Formatter(sb, Locale.US);
                formatter.format("<html><body><p>Job created, job id is %s</p></body></html>",jobId);
                displayPane.setText(sb.toString());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    private void showarchive(String vault,String archive) {
        Element vnode = findvaultbyname (vault);
        if (vnode == null) {return;}
        Element a = findarchivebydescr(vnode,archive);
        if (a == null) {return;}
        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb, Locale.US);
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
        formatter.format("<html><body><h3>%s/%s</h3><dl>",vault,archive);
        formatter.format("<dt>Date:</dt><dd>%s</dd>",a.getAttribute("date"));
        formatter.format("<dt>Size:</dt><dd>%s</dd>",Humansize(size));
        formatter.format("<dt>Tree hash:</dt><dd>%s</dd></dl></body></html>",sha256th);
        displayPane.setText(sb.toString());
    }
    private void _deletearchive(String vault,String archive) {
        if (getyesno("Really delete archive "+archive+" from vault "+vault)) {
            try {
                String response = "";
                response = super.deletearchive(vault,archive);
                if (response != null && response != "") {
                    savedb(GlacierVaultDB_File);
                    for (int i = 0; i < vaultTree.getChildCount(); i++) {
                        DefaultMutableTreeNode vnode = (DefaultMutableTreeNode) vaultTree.getChildAt(i);
                        if (vnode.toString().equals(vault)) {
                            for (int j = 0; j < vnode.getChildCount(); j++) {
                                DefaultMutableTreeNode anode = (DefaultMutableTreeNode) vnode.getChildAt(j);
                                if (anode.toString().equals(archive)) {
                                    vnode.remove(j);
                                    break;
                                }
                            }
                            break;
                        }
                    }
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    private void getarchive(String vaultName,String archive) {
        try {
            String jobId = InitiateRetrieveArchiveJob(vaultName,archive,SNSTopic);
            StringBuilder sb = new StringBuilder();
            Formatter formatter = new Formatter(sb, Locale.US);
            formatter.format("<html><body><p>Job created, job id is %s</p></body></html>",jobId);
            displayPane.setText(sb.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void showjob(String vaultName,String jobId) {
        try {
            DescribeJobResult job = GetJobDescription(vaultName,jobId);
            //String jobId = job.getJobId();
            String jobDescription = job.getJobDescription();
            if (jobDescription == null) jobDescription = jobId;
            String action = job.getAction();
            if (action == null) action = "";
            String archiveId = job.getArchiveId();
            if (archiveId == null) archiveId = "";
            String vaultARN = job.getVaultARN();
            String creationDate = job.getCreationDate();
            Boolean completed = job.getCompleted();
            String statusCode = job.getStatusCode();
            if (statusCode == null) statusCode = "";
            String statusMessage = job.getStatusMessage();
            if (statusMessage == null) statusMessage = "";
            long archiveSizeInBytes = 0;
            try {
                archiveSizeInBytes = job.getArchiveSizeInBytes();
            } catch (Exception e) {
                archiveSizeInBytes = -1;
            }
            long inventorySizeInBytes = 0;
            try {
                inventorySizeInBytes = job.getInventorySizeInBytes();
            } catch (Exception e) {
                inventorySizeInBytes = -1;
            }
            String sNSTopic = job.getSNSTopic();
            if (sNSTopic == null) sNSTopic = "";
            String completionDate = job.getCompletionDate();
            if (completionDate == null) completionDate = "";
            String sHA256TreeHash = job.getSHA256TreeHash();
            if (sHA256TreeHash == null) sHA256TreeHash = "";
            String archiveSHA256TreeHash = job.getArchiveSHA256TreeHash();
            if (archiveSHA256TreeHash == null) archiveSHA256TreeHash = "";
            String retrievalByteRange = job.getRetrievalByteRange();
            if (retrievalByteRange == null) retrievalByteRange = "";
            InventoryRetrievalJobDescription inventoryRetrievalParameters = job.getInventoryRetrievalParameters();
            StringBuilder sb = new StringBuilder();
            Formatter formatter = new Formatter(sb, Locale.US);
            formatter.format("<html><body><dl><dt>JobDescription:</dt><dd>%s</dd>",jobDescription);
            formatter.format("<dt>Action: </dt><dd>%s</dd>",action);
            formatter.format("<dt>ArchiveId: </dt><dd>%s</dd>",archiveId);
            formatter.format("<dt>ArchiveSHA256TreeHash: </dt><dd>%s</dd>",archiveSHA256TreeHash);
            formatter.format("<dt>ArchiveSizeInBytes: </dt><dd>%s</dd>",Humansize(archiveSizeInBytes));
            formatter.format("<dt>Completed: </dt><dd>%s</dd>",(completed?"Yes":"No"));
            formatter.format("<dt>CompletionDate: </dt><dd>%s</dd>",completionDate);
            formatter.format("<dt>CreationDate: </dt><dd>%s</dd>",creationDate);
            if (inventoryRetrievalParameters == null) {
                sb.append("<dt>InventoryRetrievalParameters: </dt><dd>null</dd>");
            } else {
                sb.append("<dt>InventoryRetrievalParameters: </dt><dd><dl>");
                String format = inventoryRetrievalParameters.getFormat();
                if (format == null) format = "";
                String startDate = inventoryRetrievalParameters.getStartDate();
                if (startDate == null) startDate = "";
                String endDate = inventoryRetrievalParameters.getEndDate();
                if (endDate == null) endDate = "";
                String limit = inventoryRetrievalParameters.getLimit();
                if (limit == null) limit = "";
                String marker = inventoryRetrievalParameters.getMarker();
                if (marker == null) marker = "";
                formatter.format("<dt>StartDate: </dt><dd>%s</dd>",startDate);
                formatter.format("<dt>EndDate: </dt><dd>%s</dd>",endDate);
                formatter.format("<dt>Format: </dt><dd>%s</dd>",format);
                formatter.format("<dt>Limit: </dt><dd>%s</dd>",limit);
                formatter.format("<dt>Marker: </dt><dd>%s</dd></dl></dd>",marker);
            }
            formatter.format("<dt>InventorySizeInBytes: </dt><dd>%s</dd>",Humansize(inventorySizeInBytes));
            formatter.format("<dt>JobId: </dt><dd>%s</dd>",jobId);
            formatter.format("<dt>RetrievalByteRange: </dt><dd>%s</dd>",retrievalByteRange);
            formatter.format("<dt>SHA256TreeHash: </dt><dd>%s</dd>",sHA256TreeHash);
            formatter.format("<dt>SNSTopic: </dt><dd>%s</dd>",sNSTopic);
            formatter.format("<dt>StatusCode: </dt><dd>%s</dd>",statusCode);
            formatter.format("<dt>StatusMessage: </dt><dd>%s</dd>",statusMessage);
            formatter.format("<dt>VaultARN: </dt><dd>%s</dd></dl></body></html>",vaultARN);
            displayPane.setText(sb.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void listjoboutput(String vaultName,String jobId) {
        try {
            StringBuilder sb = new StringBuilder();
            Formatter formatter = new Formatter(sb, Locale.US);
            String inventoryBody = getJobBody(vaultName,jobId);
            JSONObject obj = new JSONObject(inventoryBody);
            String VaultARN = obj.getString("VaultARN");
            String InventoryDate = obj.getString("InventoryDate");
            JSONArray ArchiveList = obj.getJSONArray("ArchiveList");
            formatter.format("<html><body><dl><dt>VaultARN:</dt><dd>%s</dd>",VaultARN); 
            formatter.format("<dt>InventoryDate: </dt><dd>%s</dd></dl><ol>",InventoryDate);
            int ia;
            for (ia = 0; ia < ArchiveList.length(); ia++) {
                String ArchiveId = ArchiveList.getJSONObject(ia).getString("ArchiveId");
                String ArchiveDescription = ArchiveList.getJSONObject(ia).getString("ArchiveDescription");
                String CreationDate = ArchiveList.getJSONObject(ia).getString("CreationDate");
                long Size = ArchiveList.getJSONObject(ia).getLong("Size");
                String SHA256TreeHash = ArchiveList.getJSONObject(ia).getString("SHA256TreeHash");
                formatter.format("<li>%s<dl>",ArchiveId);
                formatter.format("<dt>Description: </dt><dd>%s</dd>",ArchiveDescription);
                formatter.format("<dt>Date: </dt><dd>%s</dd>",CreationDate);
                formatter.format("<dt>TreeHash: </dt><dd>%s</dd>",SHA256TreeHash);
                formatter.format("<dt>Size: </dt><dd>%s</dd>",Humansize(Size));
                sb.append("</dl></li>");
            }
            sb.append("</ol></body></html>");
            displayPane.setText(sb.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void showupload(String vault, String uploadId) {
        try {
            ListMultipartUploadsResult _uploads = ListMultiPartUploads(vault);
            java.util.List<UploadListElement> uploadsList = _uploads.getUploadsList();
            if (uploadsList != null) {
                Iterator itr = uploadsList.iterator();
                while (itr.hasNext()) {
                    UploadListElement upload = (UploadListElement)itr.next();
                    String multipartUploadId = upload.getMultipartUploadId();
                    if (multipartUploadId.equals(uploadId)) {
                        String vaultARN = upload.getVaultARN();
                        if (vaultARN == null) vaultARN = "";
                        String archiveDescription = upload.getArchiveDescription();
                        if (archiveDescription == null) archiveDescription = "";
                        Long partSizeInBytes = upload.getPartSizeInBytes();
                        String creationDate = upload.getCreationDate();
                        if (creationDate == null) creationDate = "";
                        StringBuilder sb = new StringBuilder();
                        Formatter formatter = new Formatter(sb, Locale.US);
                        formatter.format("<html><body><h3>%s</h3><dl>",archiveDescription);
                        formatter.format("<dt>CreationDate: </dt><dd>%s</dd>",creationDate);
                        formatter.format("<dt>PartSizeInBytes: </dt><dd>%s</dd>",Humansize(partSizeInBytes));
                        formatter.format("<dt>MultipartUploadId: </dt><dd>%s</dd>",multipartUploadId);
                        formatter.format("<dt>VaultARN: </dt><dd>%s</dd>",vaultARN);
                        sb.append("</dl></body></html>");
                        displayPane.setText(sb.toString());
                        break;
                    } else {
                        continue;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }                    
    }
    private void showparts(String vaultName, String uploadId) {
        try {
            ListPartsResult parts = ListParts(vaultName,uploadId);
            StringBuilder sb = new StringBuilder();
            Formatter formatter = new Formatter(sb, Locale.US);
            formatter.format("<html><body><dl><dt>ArchiveDescription: </dt><dd>%s</dd>",parts.getArchiveDescription());
            formatter.format("<dt>CreationDate: </dt><dd>%s</dd>",parts.getCreationDate());
            formatter.format("<dt>MultipartUploadId: </dt><dd>%s</dd>",parts.getMultipartUploadId());
            formatter.format("<dt>PartSizeInBytes: </dt><dd>%s</dd>",Humansize(parts.getPartSizeInBytes()));
            formatter.format("<dt>VaultARN: </dt><dd>%s</dd>",parts.getVaultARN());
            java.util.List<PartListElement> partlist = parts.getParts();
            if (partlist == null) {
                sb.append("<dt>Parts: </dt><dd>null</dd>");
            } else {
                sb.append("<dt>Parts: </dt><dd><ol>");
                int index = 0;
                Iterator itr = partlist.iterator();
                while (itr.hasNext()) {
                    index++;
                    PartListElement part = (PartListElement)itr.next();
                    String range = part.getRangeInBytes();
                    String treeHash = part.getSHA256TreeHash();
                    sb.append("<li><dl>");
                    formatter.format("<dt>RangeInBytes: </dt><dd>%s</dd>",range);
                    formatter.format("<dt>SHA256TreeHash: </dt><dd>%s</dd></dl></li>",treeHash);
                }
                sb.append("</ol>");
                if (index == 1) {
                    formatter.format("<p>%d part.</p></dd>",index);
                } else {
                    formatter.format("<p>%d parts.</p></dd>",index);
                }
                sb.append("</dl></body></html>");
                displayPane.setText(sb.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }            
    }
    private void abortupload(String vaultName, String uploadId) {
        if (getyesno("Really abort upload "+uploadId+"?")) {
            try {
                AbortMultipartUpload(vaultName,uploadId);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    private boolean getyesno(String question) {
        int answer = JOptionPane.showConfirmDialog(mainFrame,question,
                  question,JOptionPane.YES_NO_OPTION,
                  JOptionPane.QUESTION_MESSAGE);
        if (answer == JOptionPane.YES_OPTION) {
            return true;
        } else {
            return false;
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
    //public void valueChanged(TreeSelectionEvent e) {
    //    System.err.println("*** GlacierCommandGUI.valueChanged("+e+")");
    //}
    public void mousePressed(MouseEvent e) {
        //System.err.println("*** GlacierCommandGUI.mousePressed("+e+")");
        if (e.getButton() != 3) return;
        JTree t = (JTree) e.getComponent();
        if (t == archiveTree) {
            int selRow = archiveTree.getRowForLocation(e.getX(), e.getY());
            TreePath selPath = archiveTree.getPathForLocation(e.getX(), e.getY());
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
        } else if (t == jobTree) {
            int selRow = jobTree.getRowForLocation(e.getX(), e.getY());
            TreePath selPath = jobTree.getPathForLocation(e.getX(), e.getY());
            switch (selPath.getPathCount()) {
            case 1: return;
            case 2: jobContextMenu(selPath.getPathComponent(1).toString(),  
                      selPath.getPathComponent(2).toString(),e.getComponent(),
                      e.getX(), e.getY());
            }
        } else if (t == uploadTree) {
            int selRow = uploadTree.getRowForLocation(e.getX(), e.getY());
            TreePath selPath = uploadTree.getPathForLocation(e.getX(), e.getY());
            switch (selPath.getPathCount()) {
            case 1: return;
            case 2: uploadContextMenu(selPath.getPathComponent(1).toString(),  
                      selPath.getPathComponent(2).toString(),e.getComponent(),
                      e.getX(), e.getY());
            }
        } else {
        }
    }
    private void vaultContextMenu(String vaultName,Component c, int x, int y) {
        //System.err.println("*** GlacierCommandGUI.vaultContextMenu("+vaultName+")");
        vmenu.setVault(vaultName);
        vmenu.show(c,x,y);
    }
    private void archiveContextMenu(String vaultName, String archiveName,Component c, int x, int y) {
        //System.err.println("*** GlacierCommandGUI.archiveContextMenu("+vaultName+","+archiveName+")");
        amenu.setVault(vaultName);
        amenu.setArchive(archiveName);
        amenu.show(c,x,y);
    }
    private void jobContextMenu(String vaultName, String jobID,Component c, int x, int y) {
        //System.err.println("*** GlacierCommandGUI.jobContextMenu("+vaultName+","+jobID+")");
        jmenu.setVault(vaultName);
        jmenu.setJobID(jobID);
        jmenu.show(c,x,y);
    }
    private void uploadContextMenu(String vaultName, String uploadID,Component c, int x, int y) {
        //System.err.println("*** GlacierCommandGUI.uploadContextMenu("+vaultName+","+uploadID+")");
        umenu.setVault(vaultName);
        umenu.setUploadID(uploadID);
        umenu.show(c,x,y);
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
