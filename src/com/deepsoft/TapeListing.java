/****************************************************************************
 *
 *  System        : 
 *  Module        : 
 *  Object Name   : $RCSfile$
 *  Revision      : $Revision$
 *  Date          : $Date$
 *  Author        : $Author$
 *  Created By    : Robert Heller
 *  Created       : Sun May 24 09:37:29 2015
 *  Last Modified : <210331.0854>
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

import java.lang.*;
import java.io.*;
import java.util.*;
import java.util.regex.*;
import org.w3c.dom.*;
import com.deepsoft.*;

public class TapeListing {
    private static final Pattern LinePattern = Pattern.compile("^(\\d{4})-(\\d{2})-(\\d{2})\\s+(\\d{2}):(\\d{2}):(\\d{2})\\s+(\\S+)\\s+(\\S+)\\s+(\\d+)[\\s]+(\\S+)\\s+(\\S+)\\s+(\\S+)\\s+(\\d+)\\s+(\\d+)/([0-9-]+)\\s+(\\S+)\\s*$");
    private static final Pattern VaultPattern = Pattern.compile("^wendellfreelibrary-vault-\\d+$");
    private static Hashtable<String,LinkedList<TapeListing>> tapes = new Hashtable<String,LinkedList<TapeListing>>();
    private static Hashtable<String,String> host_disk = new Hashtable<String,String>();
    private String timestamp = "00000000000000";
    private String host;
    private String disk;
    private boolean vaulted = false;
    public TapeListing(String _y,String _m, String _d, String _H, String _M, String _S, String _host, String _disk, String tapelabel) {
        //System.err.println("*** TapeListing() entered");
        int y = Integer.parseInt(_y,10);
        int m = Integer.parseInt(_m,10);
        int d = Integer.parseInt(_d,10);
        int H = Integer.parseInt(_H,10);
        int M = Integer.parseInt(_M,10);
        int S = Integer.parseInt(_S,10);
        Formatter f = new Formatter();
        f.format("%04d%02d%02d%02d%02d%02d",y,m,d,H,M,S);
        timestamp = f.toString();
        //System.err.println("*** TapeListing(): timestamp = "+timestamp);
        host = _host;
        //System.err.println("*** TapeListing(): host = "+host);
        disk = _disk;
        //System.err.println("*** TapeListing(): disk = "+disk);
        Matcher labelmatch = VaultPattern.matcher(tapelabel);
        vaulted = labelmatch.matches();
        //System.err.println("*** TapeListing(): vaulted = "+vaulted);
        String hd = host+":"+disk;
        //System.err.println("*** TapeListing(): hd = "+hd);
        if (host_disk.containsKey(hd)) {
            String ts = host_disk.get(hd);
            //System.err.println("*** TapeListing(): ts = "+ts);
            int comp = timestamp.compareTo(ts);
            //System.err.println("*** TapeListing(): comp = "+comp);
            if (comp > 0) {
                LinkedList<TapeListing> tps = tapes.get(ts);
                LinkedList<TapeListing> newl = new LinkedList<TapeListing>();
                ListIterator<TapeListing> iter = tps.listIterator(0);
                while (iter.hasNext()) {
                    TapeListing t = (TapeListing) iter.next();
                    if (samedisk(t)) {continue;}
                    newl.add(t);
                }
                tapes.put(ts,newl);
                host_disk.remove(hd);
            } else if (comp < 0) {
                return;
            }
        }
        host_disk.put(hd,timestamp);
        if (tapes.containsKey(timestamp)) {
            LinkedList<TapeListing> tps = tapes.get(timestamp);
            LinkedList<TapeListing> newl = new LinkedList<TapeListing>();
            ListIterator<TapeListing> iter = tps.listIterator(0);
            while (iter.hasNext()) {
                TapeListing t = (TapeListing) iter.next();
                if (samedisk(t)) {continue;}
                newl.add(t);
            }
            tapes.put(timestamp,newl);
        } else {
            tapes.put(timestamp,new LinkedList<TapeListing>());
        }
        tapes.get(timestamp).add(this);
    }
    private boolean diskeq (String d) {
        return (d.compareTo(disk) == 0);
    }
    private boolean hosteq (String h) {
        return (h.compareTo(host) == 0);
    }
    private boolean samedisk(TapeListing other) {
        return (other.diskeq(disk) && other.hosteq(host));
    }
    public static void processfind(InputStream is) throws IOException {
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader in = new BufferedReader(isr);
        String line;
        while ((line = in.readLine()) != null) {
            //System.err.println("*** processfind(): line is '"+line+"'");
            Matcher match = LinePattern.matcher(line);
            if (match.matches()) {
                String y = match.group(1);
                //System.err.println("*** processfind(): y = "+y);
                String m = match.group(2);
                //System.err.println("*** processfind(): m = "+m);
                String d = match.group(3);
                //System.err.println("*** processfind(): d = "+d);
                String H = match.group(4);
                //System.err.println("*** processfind(): H = "+H);
                String M = match.group(5);
                //System.err.println("*** processfind(): M = "+M);
                String S = match.group(6);
                //System.err.println("*** processfind(): S = "+S);
                String _host = match.group(7);
                //System.err.println("*** processfind(): _host = "+_host);
                String _disk = match.group(8);
                //System.err.println("*** processfind(): _disk = "+_disk);
                String level = match.group(9);
                //System.err.println("*** processfind(): level = "+level);
                String tapelabel = match.group(12);
                //System.err.println("*** processfind(): tapelabel = "+tapelabel);
                String file = match.group(13);
                //System.err.println("*** processfind(): file = "+file);
                String part = match.group(14);
                //System.err.println("*** processfind(): part = "+part);
                String ofparts = match.group(15);
                //System.err.println("*** processfind(): ofparts = "+ofparts);
                String status = match.group(16);
                //System.err.println("*** processfind(): status = "+status);
                if (Integer.parseInt(level) > 0) {continue;}
                //System.err.println("*** processfind(): level == 0");
                //System.err.println("*** processfind(): status is '"+status+"'");
                if (status.compareTo("OK") != 0) {continue;}
                TapeListing t = new TapeListing(y,m,d,H,M,S,_host,_disk,tapelabel);
            }
        }
    }
    public String Host() {return host;}
    public String Disk() {return disk;}
    public boolean Vaulted() {return vaulted;}
    private static String[] orderdKeys() {
        Enumeration<String> e_keys = tapes.keys();
        String keys[] = new String[tapes.size()];
        int i = 0;
        while (e_keys.hasMoreElements()) {
            keys[i++] = (String) e_keys.nextElement();
        }
        Arrays.sort(keys);
        return keys;
    }

    public static void printtapes() {
        String keys[] = orderdKeys();
        for (int i = keys.length-1;i >= 0; i--) {
            String timestamp = keys[i];
            LinkedList<TapeListing> tps = tapes.get(timestamp);
            ListIterator<TapeListing> iter = tps.listIterator(0);
            while (iter.hasNext()) {
                TapeListing tape = (TapeListing) iter.next();
                String yesno;
                if (tape.Vaulted()) {
                    yesno = "yes";
                } else {
                    yesno = "no";
                }
                System.out.println(timestamp+" "+tape.Host()+" "+tape.Disk()+" "+yesno);
            }
        }
    }
    public static LinkedList<String> vault_timestamps_needed() {
        String keys[] = orderdKeys();
        LinkedList<String> vts = new LinkedList<String>();
        for (int i = keys.length-1;i >= 0; i--) {
            String timestamp = keys[i];
            LinkedList<TapeListing> tps = tapes.get(timestamp);
            ListIterator<TapeListing> iter = tps.listIterator(0);
            while (iter.hasNext()) {
                TapeListing tape = (TapeListing) iter.next();
                if (tape.Vaulted()) {continue;}
                vts.add(timestamp);
                break;
            }
        }
        return vts;
    }
    public static void main(String args[]) throws IOException, InterruptedException {
        //System.err.println("*** Creating a process builder...");
        ProcessBuilder findproc = new ProcessBuilder("/usr/sbin/amadmin","wendellfreelibrary","find","--sort","LD");
        //System.err.println("*** Creating a process...");
        Process p = findproc.start();
        //System.err.println("*** Process started");
        InputStream is = p.getInputStream();
        TapeListing.processfind(is);
        is.close();
        p.waitFor();
        TapeListing.printtapes();
        Calendar calendar = new GregorianCalendar();
        int day = calendar.get(Calendar.DAY_OF_WEEK);
        if (day == calendar.SUNDAY) {
            LinkedList<String> tss = TapeListing.vault_timestamps_needed();
            ListIterator<String> iter = tss.listIterator(0);
            while (iter.hasNext()) {
                String ts = (String) iter.next();
                System.out.println("/usr/sbin/amvault wendellfreelibrary "+ts+" vault_changer wendellfreelibrary-vault-%%%");
                ProcessBuilder vaultproc = new ProcessBuilder("/usr/sbin/amvault","wendellfreelibrary",ts,"vault_changer","wendellfreelibrary-vault-%%%");
                p = vaultproc.start();
                is = p.getInputStream();
                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader in = new BufferedReader(isr);
                String line;
                while ((line = in.readLine()) != null) {
                    System.out.println(line);
                }
                is.close();
            }
        }
    }
}

