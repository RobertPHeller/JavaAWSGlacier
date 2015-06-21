/****************************************************************************
 *
 *  System        : 
 *  Module        : 
 *  Object Name   : $RCSfile$
 *  Revision      : $Revision$
 *  Date          : $Date$
 *  Author        : $Author$
 *  Created By    : Robert Heller
 *  Created       : Sun Jun 21 08:34:02 2015
 *  Last Modified : <150621.1017>
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
import com.amazonaws.services.glacier.model.DescribeVaultResult;
import com.amazonaws.util.json.*;
import com.deepsoft.*;

public class HandleNotification extends BackupVault {
    private File GlacierVaultDB_File;
    private File ArchiveRestoreDir;
    private static final Pattern vaultARNPattern = Pattern.compile("^arn:aws:glacier:([^:]+):(\\d+):vaults/([^/]+)$");
    private static final Pattern RangeSplitPattern = Pattern.compile("^(\\d+)-(\\d+)$");
    
    public HandleNotification (File _GlacierVaultDB_File, String _ArchiveRestoreDir) throws Exception {
        super(_GlacierVaultDB_File);
        GlacierVaultDB_File = _GlacierVaultDB_File;
        ArchiveRestoreDir = new File(_ArchiveRestoreDir);
    }
    public void processNotification(InputStream ifp) throws Exception {
        InputStreamReader isr = new InputStreamReader(ifp);
        BufferedReader in = new BufferedReader(isr);
        String line;
        
        try {
            while (true) {
                line = in.readLine();
                if (line.compareTo("") == 0) break;
            }
        } catch (IOException ioe) {
            throw ioe;
        }
        String jsonBody = in.readLine();
        JSONObject obj = new JSONObject(jsonBody);
        String action = obj.getString("Action");
        if (action.compareTo("InventoryRetrieval") == 0) {
            SyncInventory(obj);
        } else if (action.compareTo("ArchiveRetrieval") == 0) {
            GetArchive(obj);
        } else {
            throw new Exception("Unknown action: "+action);
        }
    }
    private void GetArchive(JSONObject job) throws Exception {
        String archiveid = job.getString("ArchiveId");
        String jobid     = job.getString("JobId");
        String vaultARN  = job.getString("VaultARN");
        String vault = "";
        Matcher match = vaultARNPattern.matcher(vaultARN);
        if (match.matches()) {
            vault = match.group(3);
        } else {
            throw new Exception("Cannot parse vaultARN: "+vaultARN);
        }
        String treehash = job.getString("SHA256TreeHash");
        if (treehash == null) treehash = "";
        String wholetreehash = job.getString("ArchiveSHA256TreeHash");
        long size = job.getLong("ArchiveSizeInBytes");
        String range = job.getString("RetrievalByteRange");
        match = RangeSplitPattern.matcher(range);
        String psuff = "";
        if (match.matches()) {
            long first = Long.getLong(match.group(1));
            long last  = Long.getLong(match.group(2));
            if ((last-first)+1 != size) {
                psuff = ".partial:"+range;
            }
        }
        Element vnode = findvaultbyname(vault);
        Element anode = findarchivebyaid(vnode,archiveid);
        NodeList dtags = anode.getElementsByTagName("description");
        File filename;
        if (dtags != null && dtags.getLength() > 0) {
            Element dtag = (Element) dtags.item(0);
            filename = new File(ArchiveRestoreDir,dtag.getTextContent()+psuff);
        } else {
            int index = 0;
            while (true) {
                Formatter f = new Formatter();
                index++;
                f.format("TMP%8X",index);
                filename = new File(ArchiveRestoreDir,f.toString()+psuff);
                if (!filename.exists()) {
                    break;
                }
            }
        }
        RetrieveArchive(vault,archiveid,jobid,filename.getPath(),size,range,treehash,wholetreehash);
    }
    private void SyncInventory(JSONObject job) throws Exception {
        String jobid     = job.getString("JobId");
        String vaultARN  = job.getString("VaultARN");
        String vault = "";
        String vlocation = "";
        boolean modified = false;
        Matcher match = vaultARNPattern.matcher(vaultARN);
        if (match.matches()) {
            vault = match.group(3);
            vlocation = "/"+match.group(2)+"/vaults/"+match.group(3);
        } else {
            throw new Exception("Cannot parse vaultARN: "+vaultARN);
        }
        String inventoryBody = getJobBody(vault,jobid);
        JSONObject obj = new JSONObject(inventoryBody);
        Element vnode = findvaultbyname(vault);
        if (vnode == null) {
            DescribeVaultResult describeVaultResult = describevault(vault);
            vnode = addvault(vlocation,describeVaultResult.getCreationDate());
            modified = true;
        }    
        JSONArray ArchiveList = obj.getJSONArray("ArchiveList");
        int ia;
        for (ia = 0; ia < ArchiveList.length(); ia++) {
            String ArchiveId = ArchiveList.getJSONObject(ia).getString("ArchiveId");
            String ArchiveDescription = ArchiveList.getJSONObject(ia).getString("ArchiveDescription");
            String CreationDate = ArchiveList.getJSONObject(ia).getString("CreationDate");
            long Size = ArchiveList.getJSONObject(ia).getLong("Size");
            String SHA256TreeHash = ArchiveList.getJSONObject(ia).getString("SHA256TreeHash");
            Element anode = findarchivebyaid(vnode,ArchiveId);
            if (anode == null) {
                String aloc = vlocation+"/archives/"+ArchiveId;
                anode = addarchive(aloc,CreationDate,new Long(Size).toString(),SHA256TreeHash,ArchiveDescription);
                modified = true;
            }
        }
        boolean notdone = true;
        while (notdone) {
            boolean remove = true;
            NodeList archives = vnode.getElementsByTagName("archive");
            int j;
            for (j=0; j < archives.getLength();j++) {
                Element a = (Element) archives.item(j);
                String aid = a.getAttribute("archiveid");
                for (ia = 0; ia < ArchiveList.length(); ia++) {
                    String ArchiveId = ArchiveList.getJSONObject(ia).getString("ArchiveId");
                    if (ArchiveId.compareTo(aid) == 0) {
                        remove = false;
                        break;
                    }
                }
                if (remove) {
                    vnode.removeChild(a);
                    modified = true;
                    break;
                }
            }
            notdone = (j >= archives.getLength());
        }
        if (modified) savedb(GlacierVaultDB_File);
    }
    public static void main(String args[]) throws Exception {
        String dbfile = "/var/log/amanda/wendellfreelibrary/glacier.xml";
        String ARD = "/home/amandarestore";
        int iopt = 0;
        while (iopt < args.length) {
            if (args[iopt].compareTo("-dbfile") == 0 &&
                (iopt+1) < args.length) {
                dbfile = args[iopt+1];
            } else if (args[iopt].compareTo("-restoredir") == 0 &&
                      (iopt+1) < args.length) {
                ARD = args[iopt+1];
            } else if (args[iopt].matches("^-.*")) { 
                System.err.println("Unknown option: "+args[iopt]+", should be one of -dbfile or -restoredir");
                Usage();
            } else {
                break;
            }
            iopt += 2;
        }
        HandleNotification hn = new HandleNotification(new File(dbfile),ARD);
        hn.processNotification(System.in);
    }
    private static void Usage() {
        System.err.println("HandleNotification [opts]");
        System.err.println("Where opts are:");
        System.err.println("    -dbfile dbfile");
        System.err.println("    -restoredir dir");
        System.exit(-1);
    }
}
