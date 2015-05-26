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
 *  Last Modified : <150526.1708>
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
    private void mainLoop() throws Exception {
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
                System.err.println(e.getMessage());
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
    }
    private void showarchives(String args[]) throws Exception {
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
        
