/****************************************************************************
 *
 *  System        : 
 *  Module        : 
 *  Object Name   : $RCSfile$
 *  Revision      : $Revision$
 *  Date          : $Date$
 *  Author        : $Author$
 *  Created By    : Robert Heller
 *  Created       : Tue May 26 15:38:55 2015
 *  Last Modified : <150526.2029>
 *
 *  Description	
 *
 *  Notes
 *
 *  History
 *	
 ****************************************************************************
 *
 *  Copyright (c) 2015 Deepwoods Software.
 * 
 *  All Rights Reserved.
 * 
 * This  document  may  not, in  whole  or in  part, be  copied,  photocopied,
 * reproduced,  translated,  or  reduced to any  electronic  medium or machine
 * readable form without prior written consent from Deepwoods Software.
 *
 ****************************************************************************/


package com.deepsoft;

import java.io.*;
import java.util.*;
import java.util.regex.*;
import java.lang.*;
import org.w3c.dom.*;
import com.deepsoft.*;

public class GlacierCommand extends BackupVault {
    String SNSTopic;
    File GlacierVaultDB_File;
    boolean _istty = false;
          
    public GlacierCommand(File _GlacierVaultDB_File,String _SNSTopic) throws Exception {
        super(_GlacierVaultDB_File);
        GlacierVaultDB_File = _GlacierVaultDB_File;
        SNSTopic = _SNSTopic;
        _istty = (System.console() != null);
        //System.err.println("*** GlacierCommand(), _istty = "+_istty);
    }
    private void prompt(String p) {
        System.out.print(p);
        System.out.flush();
    }
    private void mainLoop() throws IOException {
        BufferedReader in = new BufferedReader(
                  new InputStreamReader(System.in));
        String line = null;
        if (_istty) {prompt("% ");}
        while ((line = in.readLine()) != null) {
            String command[] = line.split("\\s");
            if (command.length < 1) {continue;}
            try {
                evaluate(command);
            } catch (Exception e) {
                System.err.println("Error in command ("+line+"): "+e.getClass().getName()+": "+e.getMessage());
            }
            if (_istty) {prompt("% ");}
        }
    }
    private void evaluate(String command[]) throws Exception {
        if (command.length < 1) return;
        String verb = command[0];
        verb.toLowerCase();
        if (verb.compareTo("ls") == 0 ||
            verb.matches("^sh.*")) {
            if (command.length < 2) {
                throw new Exception("Missing second command word for "+verb);
            }
            String verb2 = command[1];
            verb2.toLowerCase();
            if (verb2.matches("^va.*")) {
                showvaults(copyTail(command,2));
            } else if (verb2.matches("^ar.*")) {
                showarchives(copyTail(command,2));
            } else if (verb2.compareTo("jobs") == 0) {
                showjobs(copyTail(command,2));
            } else if (verb2.compareTo("job") == 0) {
                showjob(copyTail(command,2));
            } else if (verb2.matches("^up.*")) {
                showuploads(copyTail(command,2));
            } else if (verb2.matches("^pa.*")) {
                showparts(copyTail(command,2));
            } else {
                throw new Exception("I don't know how to show "+verb2);
            }
        } else if (verb.matches("^abort.*")) {
            abortmulti(copyTail(command,1));
        } else if (verb.compareTo("get") == 0) {
            getarchive(copyTail(command,1));
        } else if (verb.compareTo("rm") == 0 ||
                  verb.matches("^del.*")) {
            if (command.length < 2) {
                throw new Exception("Missing second command word for "+verb);
            }
            String verb2 = command[1];
            verb2.toLowerCase();
            if (verb2.matches("^va.*")) {
                deletevault(copyTail(command,2));
            } else if (verb2.matches("^ar.*")) {
                deletearchive(copyTail(command,2));
            } else {
                throw new Exception("I don't know how to delete "+verb2);
            }
        } else if (verb.compareTo("exit") == 0) {
            System.exit(0);
        } else {
            throw new Exception("I don't know how to "+verb);
        }
    }
    private void showvaults(String args[]) throws Exception {
        Element vaultsnode = getvaultnode();
        NodeList vaults = vaultsnode.getElementsByTagName("vault");
        for (int i=0; i < vaults.getLength(); i++) {
            Element vault = (Element) vaults.item(i);
            System.out.println(vault.getAttribute("name")+":");
            System.out.println("  Date Created: "+vault.getAttribute("date"));
            NodeList archives = vault.getElementsByTagName("archive");
            System.out.println("  Number of archives: "+archives.getLength());
            long totalsize = 0;
            for (int j=0; j < archives.getLength();j++) {
                Element a = (Element) archives.item(i);
                NodeList sizes = a.getElementsByTagName("size");
                long size = 0;
                if (sizes != null && sizes.getLength() > 0) {
                    Element sizeelt = (Element) sizes.item(0);
                    size = Long.parseLong(sizeelt.getTextContent());
                }
                totalsize += size;
            }
            System.out.print("  Total size: "+Humansize(totalsize)+" byte");
            if (totalsize != 1) {
                System.out.println("s.");
            } else {
                System.out.println(".");
            }
            System.out.println("");
        }
    }
    private String Humansize(long s) {
        Formatter f = new Formatter();
        if (s < 1024) {
            f.format("%d",s);
            return f.toString();
        } else if (s < (1024*1024)) {
            f.format("%5.1fK",(double)s / 1024.0);
            return f.toString();
        } else if (s < (1024*1024*1024)) {
            f.format("%5.1fM",(double)s / (1024.0*1024.0));
            return f.toString();
        } else if (s < (1024*1024*1024*1024)) {
            f.format("%5.1fG",(double)s / (1024.0*1024.0*1024.0));
            return f.toString();
        } else {
            f.format("%5.1fT",(double)s / (1024.0*1024.0*1024.0*1024.0));
            return f.toString();
        }
    }
    private void showarchives(String args[]) throws Exception {
        if (args.length < 1) {
            throw new Exception("Missing vault name");
        }
        String vault = args[0];
        Element vnode = findvaultbyname(vault);
        if (vnode == null) {
            throw new Exception("No such vault: "+vault);
        }
        NodeList archives = vnode.getElementsByTagName("archive");
        System.out.println("Vault: "+vnode.getAttribute("name")+":");
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
            System.out.printf("  %3d) %s:\n",index,descr);
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
            System.out.println("    Size: "+Humansize(size));
            System.out.println("    Tree hash: "+sha256th);
            totalsize += size;
            System.out.println("");
        }
        System.out.print("  Total size: "+Humansize(totalsize)+" byte");
        if (totalsize != 1) {
            System.out.print("s");
        }
        System.out.print(", in "+index+" archive");
        if (index != 1) {
            System.out.println("s.");
        } else {
            System.out.println(".");
        }
        System.out.println("");
    }
    private void showjobs(String args[]) throws Exception {
    }
    private void showjob(String args[]) throws Exception {
    }
    private void showuploads(String args[]) throws Exception {
    }
    private void showparts(String args[]) throws Exception {
    }
    private void abortmulti(String args[]) throws Exception {
    }
    private void getarchive(String args[]) throws Exception {
    }
    private void deletevault(String args[]) throws Exception {
    }
    private void deletearchive(String args[]) throws Exception {
    }
    public static void main(String args[]) throws Exception {
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
        GlacierCommand cli = new GlacierCommand(new File(dbfile),snstopic);
        if (iopt < args.length) {
            String command[] = copyTail(args,iopt);
            cli.evaluate(command);
        } else {
            cli.mainLoop();
        }
    }
    private static String[] copyTail(String args[],int start) {
        int dest;
        String result[] = new String[args.length-start];
        for (int i = 0;i+start < args.length;i++) {
            result[i] = args[i+start];
        }
        return result;
    }
    private static void Usage() {
        System.err.println("GlacierCommand [opts] [command words...]");
        System.err.println("Where opts are:");
        System.err.println("    -dbfile dbfile");
        System.err.println("    -snstopic snstopic");
        System.exit(-1);
    }
}
        
