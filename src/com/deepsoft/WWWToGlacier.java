/****************************************************************************
 *
 *  System        : 
 *  Module        : 
 *  Object Name   : $RCSfile$
 *  Revision      : $Revision$
 *  Date          : $Date$
 *  Author        : $Author$
 *  Created By    : Robert Heller
 *  Created       : Sat May 30 08:54:33 2015
 *  Last Modified : <150530.0942>
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
import java.text.SimpleDateFormat;
import org.w3c.dom.*;
import com.deepsoft.*;

public class WWWToGlacier extends BackupVault {
    private static final File GlacierVaultDB_File = new File("/var/lib/Glacier/Common/glacier.xml");
    private static final String glacierTemp = "/home/AmazonGlacierTemp";
    private static final String WWWVaultName = "WWWDeepsoft";
    private static final String WWWBackupDir = "/var/lib/Glacier/WWWBackups/Archives";
    private static final String WWWIndexDir = "/var/lib/Glacier/WWWBackups/Indexes";
    private static final String WWWRoot = "/var/www";
    private static final String TAR = "/bin/tar";
    private Strings[] backupnames(String backupdirname) {
        File backupdir = new File(backupdirname);
        String chdir = backupdir.getParent();
        String backupfile = backupdir.getName();
        String archivename = backupfile;
        SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");
        StringBuffer temp;
        df.format(new Date(),temp);
        String backupdate = temp.toString();
        File tarfileF = new File(WWWBackupDir,archivename+"_"+backupdate+".tar.gz");
        String tarfile = tarfileF.getPath();
        File indexfileF = new File(WWWIndexDir,archivename+"_"+backupdate);
        String indexfile = indexfileF.getPath();
        File excludefileF = new File(backupdir,".exclude");
        String excludefile = excludefileF.getPath();
        if (!excludefileF.canRead()) {
            excludefile = "";
        }
        new String result[] = new String[7];
        result[0] = chdir;
        result[1] = backupfile;
        result[2] = archivename;
        result[3] = backupdate;
        result[4] = tarfile;
        result[5] = indexfile;
        result[6] = excludefile;
        return result;
    }
    private String[] dobackup (String dir) {
        String names[] = backupnames(dir);
        String chdir = names[0];
        String backupfile = names[1];
        String archivename = names[2];
        String backupdate = names[3];
        String tarfile = names[4];
        String indexfile = names[5];
        String excludefile = names[6];
        String tarcmd[] = null;
        if (excludefile.compareTo("") == 0) {
            tarcmd[] = new String[9];
            tarcmd[0] = TAR;
            tarcmd[1] = "czvf";
            tarcmd[2] = tarfile;
            tarcmd[3] = "--ignore-failed-read";
            tarcmd[4] = "-g";
            tarcmd[5] = indexfile;
            tarcmd[6] = "-C";
            tarcmd[7] = chdir;
            tarcmd[8] = backupfile;
        } else {
            tarcmd[] = new String[11];
            tarcmd[0] = TAR;
            tarcmd[1] = "czvf";
            tarcmd[2] = tarfile;
            tarcmd[3] = "--ignore-failed-read";
            tarcmd[4] = "-g";
            tarcmd[5] = indexfile;
            tarcmd[6] = "-C";
            tarcmd[7] = chdir;
            tarcmd[8] = backupfile;
            tarcmd[9] = "--exclude-from";
            tarcmd[10] = excludefile;
        }
        Process tarProc = getRuntime.exec(tarcmd);
        InputStream pipeIS = tarProc.getInputStream();
        BufferedReader pipefp = new BufferedReader(
                  new InputStreamReader(pipeIS));
        while ((line = pipefp.readLine()) != null) {
            System.out.println(line);
        }
        pipeIS.close();
        tarProc.waitFor();
        File tarfileF = new File(tarfile);
        if (!tarfileF.exists()) {
            return null;
        } else {
            String result[] = new String[2];
            result[0] = tarfile;
            result[1] = indexfile;
            return result;
        }
    }
    public WWWToGlacier() throws Exception {
        super(GlacierVaultDB_File);
    }
    private static final BackupDirs[] = new String[]{"/var/www/deepsoft",
              "/var/www/deepwoods-repo","/var/www/hellerkin",
              "/var/www/wendellfullmoon","/var/www/library","/var/www/world"};
    public void RsyncToTheGlacier () throws Exception {
        String vlab = WWWVaultName;
        Element v = findvaultbyname(vlab);
        if (v == null) {
            v = CreateNewVault(vlab);
            if (v != null) {
                System.out.printf("Created vault: %s\n",vlab);
                savedb(GlacierVaultDB_File);
            }
        }
        if (v == null) {return;}
        
        
        
        
        
        
        
        
          
