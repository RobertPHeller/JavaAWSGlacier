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
 *  Last Modified : <150530.1421>
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
import java.text.*;
import org.w3c.dom.*;
import com.deepsoft.*;

public class FTPToGlacier extends BackupVault {
    private static final File GlacierVaultDB_File = new File("/var/lib/Glacier/Common/glacier.xml");
    private static final String glacierTemp = "/home/AmazonGlacierTemp";
    private static final String FTPVaultName = "FTPDeepsoft";
    private static final String FTPBackupDir = "/var/lib/Glacier/FTPBackups/Archives";
    private static final String FTPIndexDir = "/var/lib/Glacier/FTPBackups/Indexes";
    private static final String FTPRoot = "/var/ftp/pub";
    private static final Pattern BackupfilenamePattern = Pattern.compile("^/var/ftp/pub/(.*)$");
    private static final String TAR = "/bin/tar";
    private String[] backupnames(String backupdirname) {
        File backupdir = new File(backupdirname);
        String chdir = backupdir.getParent();
        Matcher match = BackupfilenamePattern.matcher(backupdirname);
        String backupfile = backupdir.getName();
        if (match.matches()) {
            backupfile = match.group(1);
            backupfile.replaceAll("/","_");
        }
        String archivename = backupfile;
        SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");
        StringBuffer temp = new StringBuffer();
        df.format(new Date(),temp,new FieldPosition(0));
        String backupdate = temp.toString();
        File tarfileF = new File(FTPBackupDir,archivename+"_"+backupdate+".tar.gz");
        String tarfile = tarfileF.getPath();
        File indexfileF = new File(FTPIndexDir,archivename+"_"+backupdate);
        String indexfile = indexfileF.getPath();
        File excludefileF = new File(backupdir,".exclude");
        String excludefile = excludefileF.getPath();
        if (!excludefileF.canRead()) {
            excludefile = "";
        }
        String result[] = new String[7];
        result[0] = chdir;
        result[1] = backupfile;
        result[2] = archivename;
        result[3] = backupdate;
        result[4] = tarfile;
        result[5] = indexfile;
        result[6] = excludefile;
        return result;
    }
    private String cmdstring(String args[]) {
        String result = "";
        String sp = "";
        for (String s: args) {
            result = result + sp + s;
            sp = " ";
        }
        return result;
    }
    private String[] dobackup (String dir) throws Exception {
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
            tarcmd = new String[9];
            tarcmd[0] = TAR;
            tarcmd[1] = "czvf";
            tarcmd[2] = tarfile;
            tarcmd[3] = "--ignore-failed-read";
            tarcmd[4] = "--index-file";
            tarcmd[5] = indexfile;
            tarcmd[6] = "-C";
            tarcmd[7] = chdir;
            tarcmd[8] = backupfile;
        } else {
            tarcmd = new String[11];
            tarcmd[0] = TAR;
            tarcmd[1] = "czvf";
            tarcmd[2] = tarfile;
            tarcmd[3] = "--ignore-failed-read";
            tarcmd[4] = "--index-file";
            tarcmd[5] = indexfile;
            tarcmd[6] = "-C";
            tarcmd[7] = chdir;
            tarcmd[8] = "--exclude-from";
            tarcmd[9] = excludefile;
            tarcmd[10] = backupfile;
        }
        //System.err.println("*** FTPToGlacier.dobackup(): tarcmd is "+cmdstring(tarcmd));
        Process tarProc = Runtime.getRuntime().exec(tarcmd);
        InputStream pipeIS = tarProc.getInputStream();
        BufferedReader pipefp = new BufferedReader(
                  new InputStreamReader(pipeIS));
        String line = null;
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
    public FTPToGlacier() throws Exception {
        super(GlacierVaultDB_File);
    }
    private boolean after8am() {
        Calendar calendar = new GregorianCalendar();
        return (calendar.get(Calendar.HOUR_OF_DAY) >= 8);
    }
    private boolean isFirstSunday() {
        Calendar calendar = new GregorianCalendar();
        return (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY &&
                calendar.get(Calendar.DATE) < 8);
    }
    private class AllTarFiles implements FilenameFilter {
        public boolean accept(File dir, String name) {
            boolean m = name.matches("^.*\\.tar\\.gz$");
            return m;
        }
    }
    private static final String BackupDirs[] = new String[]{"/var/ftp/pub/deepwoods-repo","/var/ftp/pub/wendellfullmoon","/var/ftp/pub/wendellmass","/var/ftp/pub/deepwoods/Products/HomeLibrarian","/var/ftp/pub/deepwoods/Products/MRRSystem","/var/ftp/pub/deepwoods/Products/RolePlayingDB","/var/ftp/pub/deepwoods/Products/WebLibrarian"};
    private static final Pattern TarbasenamePattern = Pattern.compile("^(.*)\\.tar\\.gz");
    public void RsyncToTheGlacier () throws Exception {
        String vlab = FTPVaultName;
        if (isFirstSunday()) {
            for (String dir: BackupDirs) {
                String backupnames[] = dobackup(dir);
            }
        }
        Element v = findvaultbyname(vlab);
        if (v == null) {
            v = CreateNewVault(vlab);
            if (v != null) {
                System.out.printf("Created vault: %s\n",vlab);
                savedb(GlacierVaultDB_File);
            }
        }
        if (v == null) {return;}
        File backupDir = new File(FTPBackupDir);
        String tarfiles[] = backupDir.list(new AllTarFiles());
        for (String tarfilename: tarfiles) {
            if (after8am()) break;
            File tarfile = new File(backupDir,tarfilename);
            Matcher match = TarbasenamePattern.matcher(tarfilename);
            if (match.matches()) {
                String archivename_backupdate = match.group(1);
                String indexfile = new File(FTPIndexDir,archivename_backupdate).getPath();
                Element a = findarchivebydescr(v,tarfilename);
                if (a == null) {
                    a = UploadArchive(vlab,tarfile.getPath());
                    if (a == null) continue;
                    Element indexfilenode = addTextNode(a,"indexfile",indexfile);
                    savedb(GlacierVaultDB_File);
                    System.out.println("Archive uploaded: "+vlab+":"+tarfilename);
                } else {
                    Date filedate = new Date(tarfile.lastModified());
                    Calendar fcal = new GregorianCalendar();
                    fcal.setTime(filedate);
                    Calendar now = new GregorianCalendar();
                    if ((fcal.get(Calendar.MONTH))+1<now.get(Calendar.MONTH)) {
                        System.err.println("Local archive deletable: "+tarfilename);
                        //if (tarfile.delete()) {
                        //    System.out.println("Local archive deleted: "+tarfilename);
                        //} else {
                        //    System.err.println("Failed to delete local archive: "+tarfilename);
                        //}
                    }
                }
            }
        }
    }
    public static void main(String args[]) throws Exception {
        FTPToGlacier FTP2G = new FTPToGlacier();
        FTP2G.RsyncToTheGlacier();
    }
}
        
        
          
