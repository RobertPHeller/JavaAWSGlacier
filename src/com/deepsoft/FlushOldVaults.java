/****************************************************************************
 *
 *  System        : 
 *  Module        : 
 *  Object Name   : $RCSfile$
 *  Revision      : $Revision$
 *  Date          : $Date$
 *  Author        : $Author$
 *  Created By    : Robert Heller
 *  Created       : Sun May 24 15:30:12 2015
 *  Last Modified : <210627.0856>
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

public class FlushOldVaults extends BackupVault {
    private static final String AMRMTAPE = "/usr/sbin/amrmtape";
    private static final String AMADMIN = "/usr/sbin/amadmin";
    private static final String AMGETCONF = "/usr/sbin/amgetconf";
    private static final String CONFIGDIR = "/etc/amanda";
    private static File GlacierVaultDB_File;
    private static SiteConfig configuration;
    public FlushOldVaults() throws Exception {
        super(GlacierVaultDB_File);
        
    }
    private static final SimpleDateFormat ISO8601Date = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
    private static final SimpleDateFormat oldTcl = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z");
    private static final SimpleDateFormat newjava = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");
    private Date parseVDBDate(String theDate) throws Exception {
        try {
            return oldTcl.parse(theDate);
        } catch (ParseException pe) {
            try {
                return newjava.parse(theDate);
            } catch (ParseException pe1) {
                return ISO8601Date.parse(theDate);
            } catch (Exception e) {
                throw e;
            }
        } catch (Exception e1) {
            throw e1;
        }
    }
    private String amgetconf(String param) throws Exception {
        String cmd[] = new String[3];
        cmd[0] = AMGETCONF;
        cmd[1] = configuration.AMCONFIG();
        cmd[2] = param;
        Process p = Runtime.getRuntime().exec(cmd);
        InputStream is = p.getInputStream();
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader in = new BufferedReader(isr);
        String line = in.readLine();
        is.close();
        p.waitFor();
        if (line == null) throw new Exception("Failed to get "+param);
        return line;
    }
    private void amrmtape(String tape) throws Exception {
        String cmd[] = new String[4];
        cmd[0] = AMRMTAPE;
        cmd[1] = "-q";
        cmd[2] = configuration.AMCONFIG();
        cmd[3] = tape;
        Process p = Runtime.getRuntime().exec(cmd);
        int status = p.waitFor();
        if (status != 0) throw new Exception(AMRMTAPE+" -q "+configuration.AMCONFIG()+" "+tape+": failed");
        return;
    }
    public void flushvaultsbefore(Date stamp,String disks[]) throws Exception {
        Element vaultsnode = getvaultnode();
        String tapelistfile = amgetconf("tapelist");
        File tapelist;
        if (tapelistfile.charAt(0) != '/') {
            tapelist = new File(new File(CONFIGDIR,configuration.AMCONFIG()),tapelistfile);
        } else {
            tapelist = new File(tapelistfile);
        }
        NodeList vaults = vaultsnode.getElementsByTagName("vault");
        int i;
        for (i=0; i < vaults.getLength(); i++) {
            Element vault = (Element) vaults.item(i);
            String tape = vault.getAttribute("name");
            //System.err.printf("*** FlushOldVaults.flushvaultsbefore(): (deleting valuts) tape = %s\n",tape);
            InputStream tapelistIS = new FileInputStream(tapelist);
            InputStreamReader tapelistISR = new InputStreamReader(tapelistIS);
            BufferedReader tapelistfp = new BufferedReader(tapelistISR);
            boolean delvault = true;
            String line = null;
            String p = "^\\d+\\s+"+tape+"\\s+.*";
            //System.err.printf("*** FlushOldVaults.flushvaultsbefore(): p = '%s'\n",p);
            Pattern tapePattern = Pattern.compile(p);
            while ((line = tapelistfp.readLine()) != null) {
                //System.err.printf("*** FlushOldVaults.flushvaultsbefore(): line = '%s'\n",line);
                Matcher match = tapePattern.matcher(line);
                if (match.matches()) {
                    //System.err.printf("*** FlushOldVaults.flushvaultsbefore(): match matches '%s'\n",match.group(0));
                    delvault = false;
                    break;
                }
            }
            tapelistIS.close();
            //System.err.printf("*** FlushOldVaults.flushvaultsbefore(): delvault = %s\n",(delvault?"true":"false"));
            if (delvault) {
                try {
                    //System.out.println("Vault "+tape+" would have been deleted from the Glacier");
                    deletevault(tape);
                    System.out.println("Vault "+tape+" deleted from the Glacier");
                } catch (Exception e) {
                    System.err.println("Exception deleting vault: "+e.getMessage());
                }
            }
        }
        savedb(GlacierVaultDB_File);
        for (i=0; i < vaults.getLength(); i++) {
            Element vault = (Element) vaults.item(i);
            Date vdate = parseVDBDate(vault.getAttribute("date"));
            //System.err.printf("*** FlushOldVaults.flushvaultsbefore(): vault is %s: vdate is %s, stamp is %s\n",vault.getAttribute("name"),vdate.toString(),stamp.toString());
            if (vdate.after(stamp)) {continue;}
            String tape = vault.getAttribute("name");
            //System.err.printf("*** FlushOldVaults.flushvaultsbefore(): (deleting archives) tape = %s\n",tape);
            NodeList archives = vault.getElementsByTagName("archive");
            boolean deletable = true;
            int j;
            for (j=0; j < archives.getLength(); j++) {
                Element a = (Element) archives.item(j);
                NodeList dtag = a.getElementsByTagName("description");
                if (dtag.getLength() > 0) {
                    Element descrele = (Element) dtag.item(0);
                    String descr = descrele.getTextContent();
                    //System.err.printf("*** FlushOldVaults.flushvaultsbefore(): descr = '%s'\n",descr);
                    if (!descr.matches("^.*\\.\\d$")) continue;
                    Date adate = parseVDBDate(a.getAttribute("date"));
                    //System.err.printf("*** FlushOldVaults.flushvaultsbefore(): descr = %s: adate is %s\n",descr,adate.toString());
                    
                    if (adate.after(stamp)) {
                        deletable = false;
                        continue;
                    }
                    for (int k = 0; k < disks.length; k++) {
                        //System.err.printf("*** FlushOldVaults.flushvaultsbefore(): descr = '%s', disks[%d] = '%s'\n",descr,k,disks[k]);
                        if (descr.matches("^.*\\."+disks[k]+"\\.0$")) {
                            deletable = true;
                            break;
                        }
                    }
                }
            }
            //System.err.printf("*** FlushOldVaults.flushvaultsbefore(): deletable is %s\n",(deletable)?"true":"false");
            if (!deletable) {continue;}
            //System.err.printf("*** FlushOldVaults.flushvaultsbefore(): deletable is true\n");
            boolean deletetape = true;
            int archivecount = archives.getLength();
            int deletedarchives = 0;
            j = archives.getLength();
            while (j > 0) {
                j--;
                //System.err.printf("*** FlushOldVaults.flushvaultsbefore(): j = %d, archives.getLength() = %d\n",j,archives.getLength());
                Element a = (Element) archives.item(j);
                NodeList dtag = a.getElementsByTagName("description");
                if (dtag.getLength() < 1) {continue;}
                Element descrele = (Element) dtag.item(0);
                String descr = descrele.getTextContent();
                System.err.printf("*** FlushOldVaults.flushvaultsbefore(): descr = '%s'\n",descr);
                try {
                    //System.out.println("Archive "+vault.getAttribute("name")+"/"+descr+" would have been deleted from the Glacier");
                    //deletetape = false;
                    deletearchive(vault.getAttribute("name"),descr);
                    System.out.println("Archive "+vault.getAttribute("name")+"/"+descr+" deleted from the Glacier");
                    deletedarchives++;
                } catch (Exception e) {
                    System.err.println("Error deleting Archive "+vault.getAttribute("name")+"/"+descr+": "+e.getMessage());
                    deletetape = false;
                }
            }
            //System.err.printf("*** FlushOldVaults.flushvaultsbefore(): deletedarchives = %d, archivecount = %d\n",deletedarchives,archivecount);
            if (deletedarchives < archivecount) deletetape = false;
            //System.err.printf("*** FlushOldVaults.flushvaultsbefore(): deletetape = %s, archives.getLength() = %d\n",(deletetape)?"true":"false",archives.getLength());
            if (deletetape && archives.getLength() == 0) {
                try {
                    //System.out.println(tape+" would have been deleted from the tape catalog");
                    amrmtape(tape);
                    System.out.println(tape+" deleted from the tape catalog");
                } catch (Exception e) {
                    System.err.println("Error deleting tape "+tape+": "+e.getMessage());
                }
            }
            savedb(GlacierVaultDB_File);
        }
    }
    public static void main(String args[]) throws Exception {
        configuration = new SiteConfig();
        GlacierVaultDB_File = new File(configuration.GlacierVaultDB_FileName());
        FlushOldVaults FOV = new FlushOldVaults();
        Calendar today = new GregorianCalendar();
        int thismonth = today.get(Calendar.MONTH);
        //System.err.printf("*** FlushOldVaults.main(): thismonth = %d\n",thismonth);
        int year      = today.get(Calendar.YEAR);
        //System.err.printf("*** FlushOldVaults.main(): year = %d\n",year);
        int ninetydays = 90*24*60*60;
        SimpleDateFormat simpleDate = new SimpleDateFormat("MM/dd/yyyy");
        Formatter f = new Formatter();
        f.format("%02d/01/%04d",thismonth,year);
        //System.err.printf("*** FlushOldVaults.main(): f is %s\n",f.toString());
        long currentMonth = simpleDate.parse(f.toString()).getTime();
        f.close();
        //System.err.printf("*** FlushOldVaults.main(): currentMonth = %d\n",currentMonth);
        long qbegin1 = currentMonth - ninetydays;
        Calendar qbeginCal = new GregorianCalendar();
        qbeginCal.setTime(new Date(qbegin1));
        int qbeginMonth = qbeginCal.get(Calendar.MONTH);
        //System.err.printf("*** FlushOldVaults.main(): qbeginMonth = %d\n",qbeginMonth);
        int qbeginYear  = qbeginCal.get(Calendar.YEAR);
        //System.err.printf("*** FlushOldVaults.main(): qbeginYear = %d\n",qbeginYear);
        f = new Formatter();
        f.format("%02d/01/%04d",qbeginMonth,qbeginYear);
        //System.err.printf("*** FlushOldVaults.main(): f is %s\n",f.toString());
        Date qbegin = simpleDate.parse(f.toString());
        f.close();
        //System.err.printf("*** FlushOldVaults.main(): qbegin = %s\n",qbegin.toString());
        String disks[] = FOV.configuration.Disks();
        FOV.flushvaultsbefore(qbegin,disks);
    }
}

        
