/****************************************************************************
 *
 *  System        : 
 *  Module        : 
 *  Object Name   : $RCSfile$
 *  Revision      : $Revision$
 *  Date          : $Date$
 *  Author        : $Author$
 *  Created By    : Robert Heller
 *  Created       : Sun May 24 14:00:27 2015
 *  Last Modified : <210330.1624>
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
import com.deepsoft.*;


public class VaultToGlacier extends BackupVault {
    private static final File GlacierVaultDB_File = new File("/var/log/amanda/wendellfreelibrary/glacier.xml");
    private static final Pattern LabelPattern = Pattern.compile("00000\\.(wendellfreelibrary-vault-\\d+)$");
    public VaultToGlacier() throws Exception {
        super(GlacierVaultDB_File);
    }
    private boolean after8am() {
        Calendar calendar = new GregorianCalendar();
        return (calendar.get(Calendar.HOUR_OF_DAY) >= 8);
    }
    private boolean ArrayEQ(String a[], String b[]) {
        //System.err.printf("*** VaultToGlacier.ArrayEQ: a.length = %d, b.length = %d\n",a.length,b.length);
        if (a.length != b.length) {
            return false;
        } else {
            for (int i=0; i < a.length; i++) {
                /*try {
                    System.err.printf("*** VaultToGlacier.ArrayEQ: a[%d] = %s, b[%d] = %s\n",i,a[i],i,b[i]);
                } catch (Exception e) {
                }*/
                if (a[i] == null && b[i] != null) return false;
                if (b[i] == null && a[i] != null) return false;
                if (a[i] != null && b[i] != null &&
                    a[i].compareTo(b[i]) != 0) return false;
            }
        }
        //System.err.printf("*** VaultToGlacier.ArrayEQ(): returning true\n");
        return true;
    }
    private class LabelFileFilter implements FilenameFilter {
        public boolean accept(File dir, String name) {
            //System.err.printf("*** VaultToGlacier.LabelFileFilter.accept(%s,%s)\n",dir.toString(),name);
            boolean m = name.matches("^00000\\.wendellfreelibrary-vault-.+$");
            //System.err.printf("*** VaultToGlacier.LabelFileFilter.accept(): m = %s\n",(m?"true":"false"));
            return m;
        }
    }
    public void RsyncToTheGlacier() throws Exception {
        for (int i=1; i <= 30; i++) {
            //System.err.printf("*** VaultToGlacier.RsyncToTheGlacier(): i = %d\n",i);
            if (after8am()) {break;}
            Formatter f = new Formatter();
            f.format("slot%d",i);
            File slotdir = new File("/backupdisk/wendellfreelibrary_vault/slots",f.toString());
            String labels[] = slotdir.list(new LabelFileFilter());
            //System.err.printf("*** VaultToGlacier.RsyncToTheGlacier(): labels.length = %d\n",labels.length);
            if (labels.length < 1) {continue;}
            String label = labels[0];
            //System.err.printf("*** VaultToGlacier.RsyncToTheGlacier(): label = %s\n",label);
            Matcher match = LabelPattern.matcher(label);
            if (match.matches()) {
                String vlab = match.group(1);
                Element v = findvaultbyname(vlab);
                if (v == null) {
                    System.out.println("Creating new vault: "+vlab);
                    v = CreateNewVault(vlab);
                    if (v != null) {
                        System.out.println("Created vault: "+vlab);
                        savedb(GlacierVaultDB_File);
                    }
                }
                if (v == null) {continue;}
                String allArchives[] = slotdir.list();
                Arrays.sort(allArchives);
                String uploadedArchives[] = new String[allArchives.length];
                int uploadedArchivesCount = 0;
                for (String afile: allArchives) {
                    if (after8am()) {break;}
                    File aFile = new File(slotdir,afile);
                    Element a = findarchivebydescr(v,aFile.getName());
                    if (a == null) {
                        System.out.println("Uploading Archive: "+vlab+"/"+aFile.getName());
                        try {
                            a = UploadArchive(vlab,aFile.getPath());
                        } catch (Exception e) {
                            System.out.printf("Archive upload failed %s: %s because %s\n",
                                      vlab,aFile.getName(),e.getMessage());
                            a = null;
                        }
                        if (a != null) {
                            System.out.println("Archive uploaded: "+vlab+": "+aFile.getName());
                            savedb(GlacierVaultDB_File);
                        }
                    } else {
                        uploadedArchives[uploadedArchivesCount++] = afile;
                    }
                    if (a == null) {break;}
                }
                //Calendar calendar = new GregorianCalendar();
                //int dow = calendar.get(Calendar.DAY_OF_WEEK);
                //System.err.printf("*** VaultToGlacier.RsyncToTheGlacier(): dow = %d\n",dow);
                //System.err.printf("*** VaultToGlacier.RsyncToTheGlacier(): Calendar.WEDNESDAY = %d\n",Calendar.WEDNESDAY);
                //System.err.printf("*** VaultToGlacier.RsyncToTheGlacier(): Calendar.SATURDAY = %d\n",Calendar.SATURDAY);
                //System.err.printf("*** VaultToGlacier.RsyncToTheGlacier(): allArchives.length = %d, uploadedArchivesCount = %d\n",allArchives.length,uploadedArchivesCount);
                //if (allArchives.length == uploadedArchivesCount &&
                //    ArrayEQ(allArchives,uploadedArchives) &&
                //    dow > Calendar.WEDNESDAY &&
                //    dow <= Calendar.SATURDAY) {
                //    for (int j = 0; j < uploadedArchivesCount; j++) {
                //        File file = new File(slotdir,uploadedArchives[j]);
                //        if (file.delete())
                //            System.out.println("Local archive deleted: "+uploadedArchives[j]);
                //        else System.err.printf("Failed to delete local archive: %s\n",uploadedArchives[j]);
                //        
                //    }
                //}
            }
        }
        savedb(GlacierVaultDB_File);
    }
    public static void main(String args[]) throws Exception {
        VaultToGlacier V2G = new VaultToGlacier();
        V2G.RsyncToTheGlacier();
    }
}
                    
                            
            
