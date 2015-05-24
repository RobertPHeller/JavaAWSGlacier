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
 *  Last Modified : <150524.1813>
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
    private static final String AMCONFIG = "wendellfreelibrary";
    private static final String AMGETCONF = "/usr/sbin/amgetconf";
    private static final String CONFIGDIR = "/etc/amanda";
    private static final File GlacierVaultDB_File = new File("/var/log/amanda/wendellfreelibrary/glacier.xml");
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
        cmd[1] = AMCONFIG;
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
        cmd[2] = AMCONFIG;
        cmd[3] = tape;
        Process p = Runtime.getRuntime().exec(cmd);
        int status = p.waitFor();
        if (status != 0) throw new Exception(AMRMTAPE+" -q "+AMCONFIG+" "+tape+": failed");
        return;
    }
    public void flushvaultsbefore(Date stamp,String disks[]) throws Exception {
        Element vaultsnode = getvaultnode();
        File tapelist = new File(new File(CONFIGDIR,AMCONFIG),amgetconf("tapelist"));
        NodeList vaults = vaultsnode.getElementsByTagName("vault");
        int i;
        for (i=0; i < vaults.getLength(); i++) {
            Element vault = (Element) vaults.item(i);
            String tape = vault.getAttribute("name");
            InputStream tapelistIS = new FileInputStream(tapelist);
            InputStreamReader tapelistISR = new InputStreamReader(tapelistIS);
            BufferedReader tapelistfp = new BufferedReader(tapelistISR);
            boolean delvault = true;
            String line = null;
            while ((line = tapelistfp.readLine()) != null) {
                if (line.matches("\\s"+tape+"\\s")) {
                    delvault = false;
                    break;
                }
            }
            tapelistIS.close();
            if (delvault) {
                try {
                    deletevault(tape);
                    System.out.println("Vault "+tape+" deleted from the Glacier");
                } catch (Exception e) {
                }
            }
        }
        savedb(GlacierVaultDB_File);
        for (i=0; i < vaults.getLength(); i++) {
            Element vault = (Element) vaults.item(i);
            Date vdate = parseVDBDate(vault.getAttribute("date"));
            if (vdate.after(stamp)) {continue;}
            String tape = vault.getAttribute("name");
            NodeList archives = vault.getElementsByTagName("archive");
            boolean deletable = false;
            for (i=0; i < archives.getLength(); i++) {
                Element a = (Element) archives.item(i);
                NodeList dtag = a.getElementsByTagName("description");
                if (dtag.getLength() > 0) {
                    Element descrele = (Element) dtag.item(0);
                    String descr = descrele.getTextContent();
                    for (int j = 0; j < disks.length; j++) {
                        if (descr.matches("\\."+disks[j]+"\\.0$")){
                            deletable = true;
                            break;
                        }
                    }
                }
            }
            if (!deletable) {continue;}
            boolean deletetape = true;
            for (i=0; i < archives.getLength() && deletetape; i++) {
                Element a = (Element) archives.item(i);
                NodeList dtag = a.getElementsByTagName("description");
                if (dtag.getLength() < 1) {continue;}
                Element descrele = (Element) dtag.item(0);
                String descr = descrele.getTextContent();
                try {
                    deletearchive(vault.getAttribute("name"),descr);
                    System.out.println("Archive "+vault.getAttribute("name")+"/"+descr+" deleted from the Glacier");
                } catch (Exception e) {
                    deletetape = false;
                }
            }
            if (deletetape) {
                try {
                    amrmtape(tape);
                    System.out.println(tape+" deleted from the tape catalog");
                } catch (Exception e) {
                    System.err.println(e.getMessage());
                }
            }
            savedb(GlacierVaultDB_File);
        }
    }
    public static void main(String args[]) throws Exception {
        FlushOldVaults FOV = new FlushOldVaults();
        Calendar today = new GregorianCalendar();
        int thismonth = today.get(Calendar.MONTH);
        int year      = today.get(Calendar.YEAR);
        int ninetydays = 90*24*60*60;
        SimpleDateFormat simpleDate = new SimpleDateFormat("MM/dd/yyyy");
        Formatter f = new Formatter();
        f.format("%02d/01/%04d",thismonth,year);
        long currentMonth = simpleDate.parse(f.toString()).getTime();
        long qbegin1 = currentMonth - ninetydays;
        Calendar qbeginCal = new GregorianCalendar();
        qbeginCal.setTime(new Date(qbegin1));
        int qbeginMonth = qbeginCal.get(Calendar.MONTH);
        int qbeginYear  = qbeginCal.get(Calendar.YEAR);
        f.format("%02d/01/%04d",qbeginMonth,qbeginYear);
        Date qbegin = simpleDate.parse(f.toString());
        String disks[] = new String[]{"root","distros","ub140464"};
        FOV.flushvaultsbefore(qbegin,disks);
    }
}

        
