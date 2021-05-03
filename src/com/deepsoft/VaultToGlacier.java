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
 *  Last Modified : <210503.0833>
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
    private static File GlacierVaultDB_File;
    private static Pattern LabelPattern;
    private static SiteConfig configuration;
    private boolean timecheck_;
    public VaultToGlacier(boolean timecheck) throws Exception {
        super(GlacierVaultDB_File);
        timecheck_ = timecheck;
    }
    private boolean after8am() {
        if (!timecheck_) return false;
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
            boolean m = name.matches(configuration.LabelFileFilterString());
            //System.err.printf("*** VaultToGlacier.LabelFileFilter.accept(): m = %s\n",(m?"true":"false"));
            return m;
        }
    }
    public void RsyncToTheGlacier() throws Exception {
        for (int i=0; i <= 150; i++) {
            //System.err.printf("*** VaultToGlacier.RsyncToTheGlacier(): i = %d\n",i);
            if (after8am()) {break;}
            Formatter f = new Formatter();
            f.format("slot%d",i);
            //System.err.printf("*** VaultToGlacier.RsyncToTheGlacier(): configuration.Slotdir() is %s\n",configuration.Slotdir());
            //System.err.printf("*** VaultToGlacier.RsyncToTheGlacier(): f is %s\n",f.toString());
            File slotdir = new File(configuration.Slotdir(),f.toString());
            if (slotdir == null) continue;
            //System.err.printf("*** VaultToGlacier.RsyncToTheGlacier(): slotdir is not null\n");
            LabelFileFilter filter = new LabelFileFilter();
            if (filter == null) continue;
            //System.err.printf("*** VaultToGlacier.RsyncToTheGlacier(): filter is not null\n");
            String labels[] = slotdir.list(filter);
            //System.err.printf("*** VaultToGlacier.RsyncToTheGlacier(): after String labels[] =\n");
            if (labels == null || labels.length < 1) {continue;}
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
        configuration = new SiteConfig();
        if (configuration == null) throw new Exception("No configuration!");
        GlacierVaultDB_File = new File(configuration.GlacierVaultDB_FileName());
        if (GlacierVaultDB_File == null) throw new Exception("No GlacierVaultDB_File");
        LabelPattern = Pattern.compile(configuration.LabelPattern());
        if (LabelPattern == null) throw new Exception("No LabelPattern");
        boolean timecheck = true;
        if (args.length > 0 && args[0].compareTo("-notimecheck") == 0) timecheck = false;
        VaultToGlacier V2G = new VaultToGlacier(timecheck);
        if (V2G == null) throw new Exception("No V2G");
        V2G.RsyncToTheGlacier();
    }
}
                    
                            
            
