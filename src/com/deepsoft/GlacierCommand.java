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
 *  Last Modified : <220310.1555>
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
import com.amazonaws.services.glacier.TreeHashGenerator;
import com.amazonaws.services.glacier.model.ListPartsResult;
import com.amazonaws.services.glacier.model.ListMultipartUploadsResult;
import com.amazonaws.services.glacier.model.PartListElement;
import com.amazonaws.services.glacier.model.UploadListElement;
import com.amazonaws.services.glacier.model.InitiateJobResult;
import com.amazonaws.services.glacier.model.ListJobsResult;
import com.amazonaws.services.glacier.model.GlacierJobDescription;
import com.amazonaws.services.glacier.model.DescribeJobResult;
import com.amazonaws.services.glacier.model.InventoryRetrievalJobDescription;
import com.amazonaws.services.glacier.model.JobParameters;
import com.amazonaws.services.glacier.model.InventoryRetrievalJobInput;
import com.amazonaws.services.glacier.model.DescribeVaultResult;
import com.amazonaws.util.json.*;
import com.deepsoft.*;

public class GlacierCommand extends BackupVault {
    private static final Pattern vaultARNPattern = Pattern.compile("^arn:aws:glacier:([^:]+):(\\d+):vaults/([^/]+)$");
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
        String cp = System.getProperty("java.class.path");
        String jname = new File(cp).getCanonicalFile().getName();
        Pattern p = Pattern.compile("JavaAWSGlacier-(\\d+)\\.jar");
        Matcher m = p.matcher(jname);
        String jdate = "unknown";
        if (m.matches()) {
            jdate = m.group(1);
        }
        System.out.printf("GlacierCommand (build %s)\n\n",jdate);
        BufferedReader in = new BufferedReader(
                  new InputStreamReader(System.in));
        String line = null;
        if (_istty) {prompt("% ");}
        while ((line = in.readLine()) != null) {
            String command[] = line.trim().split("\\s+");
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
            } else if (verb2.matches("^inv.*")) {
                showInventoryJob(copyTail(command,2));
            } else {
                throw new Exception("I don't know how to show "+verb2);
            }
        } else if (verb.matches("^abort.*")) {
            abortmulti(copyTail(command,1));
        } else if (verb.matches("^cre.*")) {
            if (command.length < 2) {
                throw new Exception("Missing second command word for "+verb);
            }
            String verb2 = command[1];
            verb2.toLowerCase();
            if (verb2.matches("^va.*")) {
                createvault(copyTail(command,2));
            } else {
                throw new Exception("I don't know how to create "+verb2);
            }
        } else if (verb.compareTo("get") == 0) {
            if (command.length < 2) {
                throw new Exception("Missing second command word for "+verb);
            }
            String verb2 = command[1];
            verb2.toLowerCase();
            if (verb2.matches("^arch.*")) {
                getarchive(copyTail(command,2));
            } else if (verb2.matches("^vault.*")) {
                getvault(copyTail(command,2));
            } else if (verb2.matches("^inv.*")) {
                getInventoryJob(copyTail(command,2));
            } else {
                throw new Exception("I don't know how to get "+verb2); 
            }
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
        } else if (verb.matches("^inv.*")) {
            startInventoryRetrievalJob(copyTail(command,1));
        } else if (verb.matches("^tree.*")) {
            treehash(copyTail(command,1));
        } else if (verb.matches("^sync.*")) {
            syncinventory(copyTail(command,1));
        } else if (verb.compareTo("exit") == 0) {
            System.exit(0);
        } else if (verb.compareTo("help") == 0) {
            showhelp(copyTail(command,1));
        } else {
            throw new Exception("I don't know how to "+verb);
        }
    }
    private void treehash(String args[]) throws Exception {
        if (args.length < 1) {
            throw new Exception("Missing filename");
        }
        String filename = args[0];
        String computedTreeHash = TreeHashGenerator.calculateTreeHash(new File(filename));
        System.out.printf("Tree Hash of %s is %s\n",filename,computedTreeHash);
    }
    private void startInventoryRetrievalJob(String args[]) throws Exception {
        if (args.length < 1) {
            throw new Exception("Missing vault name");
        }
        String vaultName = args[0];
        JobParameters jobParams = new JobParameters()
              .withType("inventory-retrieval")
              .withSNSTopic(SNSTopic);
        InventoryRetrievalJobInput inventoryParams = null;
        int iopt = 1;
        while (iopt < args.length) {
            if (args[iopt].compareTo("-startdate") == 0 &&
                (iopt+1) < args.length) {
                if (inventoryParams == null) {
                    inventoryParams = new InventoryRetrievalJobInput();
                }
                inventoryParams.setStartDate(args[iopt+1]);
                iopt += 2;
            } else if (args[iopt].compareTo("-enddate") == 0 &&
                      (iopt+1) < args.length) {
                if (inventoryParams == null) {
                    inventoryParams = new InventoryRetrievalJobInput();
                }
                inventoryParams.setEndDate(args[iopt+1]);
                iopt += 2;
            } else if (args[iopt].compareTo("-marker") == 0 &&
                      (iopt+1) < args.length) {
                if (inventoryParams == null) {
                    inventoryParams = new InventoryRetrievalJobInput();
                }
                inventoryParams.setMarker(args[iopt+1]);
                iopt += 2;
            } else if (args[iopt].compareTo("-limit") == 0 &&
                      (iopt+1) < args.length) {
                if (inventoryParams == null) {
                    inventoryParams = new InventoryRetrievalJobInput();
                }
                inventoryParams.setLimit(args[iopt+1]);
                iopt += 2;
            } else if (args[iopt].compareTo("-format") == 0 &&
                      (iopt+1) < args.length) {
                jobParams.setFormat(args[iopt+1]);
                iopt += 2;
            } else if (args[iopt].compareTo("-description") == 0 &&
                      (iopt+1) < args.length) {
                jobParams.setDescription(args[iopt+1]);
                iopt += 2;
            } else {
                System.err.println("Unknown option: "+args[iopt]+", should be one of -startdate, -enddate, -marker, -limit, -format, -description, or -snstopic");
                Usage();
            }
        }
        if (inventoryParams != null) {
            jobParams.setInventoryRetrievalParameters(inventoryParams);
        }
        String jobId = InitiateRetrieveInventory(vaultName,jobParams);
        System.out.printf("Job created, job id is %s\n",jobId);
    }
    private void showvaults(String args[]) throws Exception {
        String p = ".*";
        if (args.length > 0) {
            p = args[0]+".*";
        }
        Pattern vpattern = Pattern.compile(p);              
        Element vaultsnode = getvaultnode();
        NodeList vaults = vaultsnode.getElementsByTagName("vault");
        //System.err.printf("*** GlacierCommand.showvaults(): vaults.getLength() = %d\n",vaults.getLength());
        for (int i=0; i < vaults.getLength();i++) {
            Element vault = (Element) vaults.item(i);
            //System.err.printf("*** GlacierCommand.showvaults(): vault.getAttribute(\"name\") is '%s'\n",vault.getAttribute("name"));
            Matcher match = vpattern.matcher(vault.getAttribute("name"));
            if (!match.matches()) continue;
            System.out.println(vault.getAttribute("name")+":");
            System.out.println("  Date Created: "+vault.getAttribute("date"));
            NodeList archives = vault.getElementsByTagName("archive");
            System.out.println("  Number of archives: "+archives.getLength());
            long totalsize = 0;
            for (int j=0; j < archives.getLength();j++) {
                Element a = (Element) archives.item(j);
                NodeList sizes = a.getElementsByTagName("size");
                long size = 0;
                if (sizes != null && sizes.getLength() > 0) {
                    Element sizeelt = (Element) sizes.item(0);
                    size = Long.parseLong(sizeelt.getTextContent());
                }
                totalsize += size;
                //System.err.printf("*** showvaults(): totalsize = %d, size = %d\n",totalsize,size);
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
    private final long oneK = 1024;
    private final double oneKD = (double) oneK;
    private final long oneM = 1024*1024;
    private final double oneMD = (double) oneM;
    private final long oneG = 1024*1024*1024;
    private final double oneGD = (double) oneG;
    private final long oneT = oneM*oneM;
    private final double oneTD = (double) oneT;
    private String Humansize(long s) {
        //System.err.printf("*** GlacierCommand.Humansize(%d)\n",s);
        Formatter f = new Formatter();
        if (s < oneK) {
            f.format("%d",s);
            return f.toString();
        } else if (s < oneM) {
            f.format("%5.1fK",(double)s / oneKD);
            return f.toString();
        } else if (s < oneG) {
            f.format("%5.1fM",(double)s / oneMD);
            return f.toString();
        } else if (s < oneT) {
            f.format("%5.1fG",(double)s / oneGD);
            return f.toString();
        } else {
            f.format("%5.1fT",(double)s / oneTD);
            return f.toString();
        }
    }
    private void showarchives(String args[]) throws Exception {
        String p = ".*";
        if (args.length > 0) {
            p = args[0]+".*";
        }
        Pattern vpattern = Pattern.compile(p);              
        LinkedList<Element> vaults = findvaultsbypattern(vpattern);
        long grandtotalsize = 0;
        int vcount = 0;
        ListIterator<Element> viter = vaults.listIterator(0);
        if (!viter.hasNext()) {
            throw new Exception("No such matching vaults: "+p);
        }
        while (viter.hasNext()) {
            vcount++;
            Element vnode = (Element) viter.next();
            NodeList archives = vnode.getElementsByTagName("archive");
            System.out.printf("%d) Vault: %s:\n",vcount,vnode.getAttribute("name"));
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
                System.out.println("    Date: "+a.getAttribute("date"));
                System.out.println("    Size: "+Humansize(size));
                System.out.println("    Tree hash: "+sha256th);
                totalsize += size;
                System.out.println("");
            }
            System.out.print("  Total size: "+Humansize(totalsize)+" byte");
            if (totalsize != 1) {
                System.out.print("s");
            }
            grandtotalsize += totalsize;
            System.out.print(", in "+index+" archive");
            if (index != 1) {
                System.out.println("s.");
            } else {
                System.out.println(".");
            }
            System.out.println("");
        }
        System.out.print("Grand Total size: "+Humansize(grandtotalsize)+" byte");
        if (grandtotalsize != 1) {
            System.out.print("s");
        }
        System.out.print(", in "+vcount+" vault");
        if (vcount != 1) {
            System.out.println("s.");
        } else {
            System.out.println(".");
        }
        System.out.println("");
    }
    private void showjobs(String args[]) throws Exception {
        if (args.length < 1) {
            throw new Exception("Missing vault name");
        }
        String vault = args[0];
        ListJobsResult ljResult = GetJobList(vault,copyTail(args,1));
        //System.err.println("*** GlacierCommand.showjobs(): ljResult is "+ljResult.toString());
        java.util.List<GlacierJobDescription> jobList = ljResult.getJobList();
        if (jobList != null) {
            int index = 0;
            Iterator itr = jobList.iterator();
            while (itr.hasNext()) {
                GlacierJobDescription job = (GlacierJobDescription)itr.next();
                //System.err.println("*** GlacierCommand.showjobs(): job is "+job.toString());
                index++;
                String jobId = job.getJobId();
                String jobDescription = job.getJobDescription();
                if (jobDescription == null) jobDescription = jobId;
                String action = job.getAction();
                if (action == null) action = "";
                String archiveId = job.getArchiveId();
                if (archiveId == null) archiveId = "";
                String vaultARN = job.getVaultARN();
                String creationDate = job.getCreationDate();
                Boolean completed = job.getCompleted();
                String statusCode = job.getStatusCode();
                if (statusCode == null) statusCode = "";
                String statusMessage = job.getStatusMessage();
                if (statusMessage == null) statusMessage = "";
                //System.err.println("*** GlacierCommand.showjobs(): statusMessage = "+statusMessage);
                long archiveSizeInBytes = 0;
                long inventorySizeInBytes = 0;
                //System.err.println("*** GlacierCommand.showjobs(): action is "+action);
                if (action.compareTo("InventoryRetrieval") == 0) {
                    try {
                        inventorySizeInBytes = job.getInventorySizeInBytes();
                    } catch (Exception e) {
                        inventorySizeInBytes = -1;
                    }
                } else {
                    try {
                        archiveSizeInBytes = job.getArchiveSizeInBytes();
                    } catch (Exception e) {
                        archiveSizeInBytes = -1;
                    }
                }
                //System.err.println("*** GlacierCommand.showjobs(): archiveSizeInBytes = "+archiveSizeInBytes+", inventorySizeInBytes = "+inventorySizeInBytes);
                String sNSTopic = job.getSNSTopic();
                if (sNSTopic == null) sNSTopic = "";
                //System.err.println("*** GlacierCommand.showjobs(): sNSTopic is "+sNSTopic);
                String completionDate = job.getCompletionDate();
                if (completionDate == null) completionDate = "";
                String sHA256TreeHash = job.getSHA256TreeHash();
                if (sHA256TreeHash == null) sHA256TreeHash = "";
                String archiveSHA256TreeHash = job.getArchiveSHA256TreeHash();
                if (archiveSHA256TreeHash == null) archiveSHA256TreeHash = "";
                String retrievalByteRange = job.getRetrievalByteRange();
                if (retrievalByteRange == null) retrievalByteRange = "";
                InventoryRetrievalJobDescription inventoryRetrievalParameters = job.getInventoryRetrievalParameters();
                //if (inventoryRetrievalParameters != null) System.err.println("*** GlacierCommand.showjobs(): inventoryRetrievalParameters is "+inventoryRetrievalParameters.toString());
                System.out.printf("%2d: %s\n",index,jobDescription);
                System.out.printf("    Action: %s\n",action);
                System.out.printf("    ArchiveId: %s\n",archiveId);
                System.out.printf("    ArchiveSHA256TreeHash: %s\n",archiveSHA256TreeHash);
                System.out.printf("    ArchiveSizeInBytes: %s\n",Humansize(archiveSizeInBytes));
                System.out.printf("    Completed: %s\n",(completed?"Yes":"No"));
                System.out.printf("    CompletionDate: %s\n",completionDate);
                System.out.printf("    CreationDate: %s\n",creationDate);
                if (inventoryRetrievalParameters == null) {
                    System.out.println("    InventoryRetrievalParameters: null");
                } else {
                    System.out.println("    InventoryRetrievalParameters: ");
                    String format = inventoryRetrievalParameters.getFormat();
                    if (format == null) format = "";
                    String startDate = inventoryRetrievalParameters.getStartDate();
                    if (startDate == null) startDate = "";
                    String endDate = inventoryRetrievalParameters.getEndDate();
                    if (endDate == null) endDate = "";
                    String limit = inventoryRetrievalParameters.getLimit();
                    if (limit == null) limit = "";
                    String marker = inventoryRetrievalParameters.getMarker();
                    if (marker == null) marker = "";
                    System.out.printf("        StartDate: %s\n",startDate);
                    System.out.printf("        EndDate: %s\n",endDate);
                    System.out.printf("        Format: %s\n",format);
                    System.out.printf("        Limit: %s\n",limit);
                    System.out.printf("        Marker: %s\n",marker);
                }
                System.out.printf("    InventorySizeInBytes: %s\n",Humansize(inventorySizeInBytes));
                System.out.printf("    JobId: %s\n",jobId);
                System.out.printf("    RetrievalByteRange: %s\n",retrievalByteRange);
                System.out.printf("    SHA256TreeHash: %s\n",sHA256TreeHash);
                System.out.printf("    SNSTopic: %s\n",sNSTopic);
                System.out.printf("    StatusCode: %s\n",statusCode);
                System.out.printf("    StatusMessage: %s\n",statusMessage);
                System.out.printf("    VaultARN: %s\n",vaultARN);
            }
            if (index > 0) System.out.println("");
            if (index == 1) {
                System.out.printf("%d job.\n",index);
            } else {
                System.out.printf("%d jobs.\n",index);
            }
        }
    }
    private void showjob(String args[]) throws Exception {
        if (args.length < 1) {
            throw new Exception("Missing vault name");
        }
        String vaultName = args[0];
        if (args.length < 2) {
            throw new Exception("Missing job id");
        }
        String jobId = args[1];
        DescribeJobResult job = GetJobDescription(vaultName,jobId);
        //String jobId = job.getJobId();
        String jobDescription = job.getJobDescription();
        if (jobDescription == null) jobDescription = jobId;
        String action = job.getAction();
        if (action == null) action = "";
        String archiveId = job.getArchiveId();
        if (archiveId == null) archiveId = "";
        String vaultARN = job.getVaultARN();
        String creationDate = job.getCreationDate();
        Boolean completed = job.getCompleted();
        String statusCode = job.getStatusCode();
        if (statusCode == null) statusCode = "";
        String statusMessage = job.getStatusMessage();
        if (statusMessage == null) statusMessage = "";
        long archiveSizeInBytes = 0;
        try {
            archiveSizeInBytes = job.getArchiveSizeInBytes();
        } catch (Exception e) {
            archiveSizeInBytes = -1;
        }
        long inventorySizeInBytes = 0;
        try {
            inventorySizeInBytes = job.getInventorySizeInBytes();
        } catch (Exception e) {
            inventorySizeInBytes = -1;
        }
        String sNSTopic = job.getSNSTopic();
        if (sNSTopic == null) sNSTopic = "";
        String completionDate = job.getCompletionDate();
        if (completionDate == null) completionDate = "";
        String sHA256TreeHash = job.getSHA256TreeHash();
        if (sHA256TreeHash == null) sHA256TreeHash = "";
        String archiveSHA256TreeHash = job.getArchiveSHA256TreeHash();
        if (archiveSHA256TreeHash == null) archiveSHA256TreeHash = "";
        String retrievalByteRange = job.getRetrievalByteRange();
        if (retrievalByteRange == null) retrievalByteRange = "";
        InventoryRetrievalJobDescription inventoryRetrievalParameters = job.getInventoryRetrievalParameters();
        System.out.printf("JobDescription: %s\n",jobDescription);
        System.out.printf("Action: %s\n",action);
        System.out.printf("ArchiveId: %s\n",archiveId);
        System.out.printf("ArchiveSHA256TreeHash: %s\n",archiveSHA256TreeHash);
        System.out.printf("ArchiveSizeInBytes: %s\n",Humansize(archiveSizeInBytes));
        System.out.printf("Completed: %s\n",(completed?"Yes":"No"));
        System.out.printf("CompletionDate: %s\n",completionDate);
        System.out.printf("CreationDate: %s\n",creationDate);
        if (inventoryRetrievalParameters == null) {
            System.out.println("InventoryRetrievalParameters: null");
        } else {
            System.out.println("InventoryRetrievalParameters: ");
            String format = inventoryRetrievalParameters.getFormat();
            if (format == null) format = "";
            String startDate = inventoryRetrievalParameters.getStartDate();
            if (startDate == null) startDate = "";
            String endDate = inventoryRetrievalParameters.getEndDate();
            if (endDate == null) endDate = "";
            String limit = inventoryRetrievalParameters.getLimit();
            if (limit == null) limit = "";
            String marker = inventoryRetrievalParameters.getMarker();
            if (marker == null) marker = "";
            System.out.printf("    StartDate: %s\n",startDate);
            System.out.printf("    EndDate: %s\n",endDate);
            System.out.printf("    Format: %s\n",format);
            System.out.printf("    Limit: %s\n",limit);
            System.out.printf("    Marker: %s\n",marker);
        }
        System.out.printf("InventorySizeInBytes: %s\n",Humansize(inventorySizeInBytes));
        System.out.printf("JobId: %s\n",jobId);
        System.out.printf("RetrievalByteRange: %s\n",retrievalByteRange);
        System.out.printf("SHA256TreeHash: %s\n",sHA256TreeHash);
        System.out.printf("SNSTopic: %s\n",sNSTopic);
        System.out.printf("StatusCode: %s\n",statusCode);
        System.out.printf("StatusMessage: %s\n",statusMessage);
        System.out.printf("VaultARN: %s\n",vaultARN);
    }
    private void showInventoryJob(String args[]) throws Exception {
        if (args.length < 1) {
            throw new Exception("Missing vault name");
        }
        String vaultName = args[0];
        if (args.length < 2) {
            throw new Exception("Missing job id");
        }
        String jobId = args[1];
        String inventoryBody = getJobBody(vaultName,jobId);
        JSONObject obj = new JSONObject(inventoryBody);
        String VaultARN = obj.getString("VaultARN");
        String InventoryDate = obj.getString("InventoryDate");
        JSONArray ArchiveList = obj.getJSONArray("ArchiveList");
        System.out.printf("VaultARN:      %s\n",VaultARN);
        System.out.printf("InventoryDate: %s\n",InventoryDate);
        int ia;
        for (ia = 0; ia < ArchiveList.length(); ia++) {
            String ArchiveId = ArchiveList.getJSONObject(ia).getString("ArchiveId");
            String ArchiveDescription = ArchiveList.getJSONObject(ia).getString("ArchiveDescription");
            String CreationDate = ArchiveList.getJSONObject(ia).getString("CreationDate");
            long Size = ArchiveList.getJSONObject(ia).getLong("Size");
            String SHA256TreeHash = ArchiveList.getJSONObject(ia).getString("SHA256TreeHash");
            System.out.printf(" %4d) %s\n",ia+1,ArchiveId);
            System.out.printf("     Description: %s\n",ArchiveDescription);
            System.out.printf("     Date:        %s\n",CreationDate);
            System.out.printf("     TreeHash:    %s\n",SHA256TreeHash);
            System.out.printf("     Size:        %s\n",Humansize(Size));
            System.out.println("");
        }
    }
    private void getInventoryJob(String args[]) throws Exception {
        if (args.length < 1) {
            throw new Exception("Missing vault name");
        }
        String vaultName = args[0];
        if (args.length < 2) {
            throw new Exception("Missing job id");
        }
        String jobId = args[1];
        String inventoryBody = getJobBody(vaultName,jobId);
        System.out.println(inventoryBody);
    }
    private void syncinventory(String args[]) throws Exception {
        if (args.length < 1) {
            throw new Exception("Missing vault name");
        }
        String vault = args[0];
        if (args.length < 2) {
            throw new Exception("Missing job id");
        }
        String jobId = args[1];
        String inventoryBody = getJobBody(vault,jobId);
        JSONObject obj = new JSONObject(inventoryBody);
        String vaultARN = obj.getString("VaultARN");
        String vlocation = "";
        boolean modified = false;
        Matcher match = vaultARNPattern.matcher(vaultARN);
        if (match.matches()) {
            vlocation = "/"+match.group(2)+"/vaults/"+match.group(3);
        } else {
            throw new Exception("Cannot parse vaultARN: "+vaultARN);
        }
        Element vnode = findvaultbyname(vault);
        if (vnode == null) {
            DescribeVaultResult describeVaultResult = describevault(vault);
            //System.err.printf("*** GlacierCommand.syncinventory() adding vault: %s\n",vault);
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
                //System.err.printf("*** GlacierCommand.syncinventory() adding archive: %s\n",aloc);
                anode = addarchive(aloc,CreationDate,new Long(Size).toString(),SHA256TreeHash,ArchiveDescription);
                modified = true;
            }
        }
        boolean notdone = true;
        while (notdone) {
            notdone = false;
            boolean remove = true;
            NodeList archives = vnode.getElementsByTagName("archive");
            int j;
            for (j=0; j < archives.getLength();j++) {
                Element a = (Element) archives.item(j);
                String aid = a.getAttribute("archiveid");
                for (ia = 0; ia < ArchiveList.length(); ia++) {
                    String ArchiveId = ArchiveList.getJSONObject(ia).getString("ArchiveId");
                    //System.err.printf("*** GlacierCommand.syncinventory() comparing %s to %s\n",ArchiveId,aid);
                    if (ArchiveId.compareTo(aid) == 0) {
                        remove = false;
                        break;
                    }
                }
                if (remove) {
                    //System.err.printf("*** GlacierCommand.syncinventory() removing archive: %s\n",aid);
                    vnode.removeChild(a);
                    modified = true;
                    notdone = true;
                    break;
                }
            }
        }
        if (modified) savedb(GlacierVaultDB_File);
    }
    private void showuploads(String args[]) throws Exception {
        if (args.length < 1) {
            throw new Exception("Missing vault name");
        }
        String vault = args[0];
        ListMultipartUploadsResult uploads = ListMultiPartUploads(vault);
        java.util.List<UploadListElement> uploadsList = uploads.getUploadsList();
        int index = 0;
        if (uploadsList != null) {
            Iterator itr = uploadsList.iterator();
            while (itr.hasNext()) {
                index++;
                UploadListElement upload = (UploadListElement)itr.next();
                String multipartUploadId = upload.getMultipartUploadId();
                if (multipartUploadId == null) multipartUploadId = "";
                String vaultARN = upload.getVaultARN();
                if (vaultARN == null) vaultARN = "";
                String archiveDescription = upload.getArchiveDescription();
                if (archiveDescription == null) archiveDescription = "";
                Long partSizeInBytes = upload.getPartSizeInBytes();
                String creationDate = upload.getCreationDate();
                if (creationDate == null) creationDate = "";
                System.out.printf("%2d: %s\n",index,archiveDescription);
                System.out.printf("    CreationDate: %s\n",creationDate);
                System.out.printf("    PartSizeInBytes: %s\n",Humansize(partSizeInBytes));
                System.out.printf("    MultipartUploadId: %s\n",multipartUploadId);
                System.out.printf("    VaultARN: %s\n",vaultARN);
            }
        }
        if (index > 0) System.out.println("");
        if (index == 1) {
            System.out.printf("%d upload.\n",index);
        } else {
            System.out.printf("%d uploads.\n",index);
        }
    }
    private void showparts(String args[]) throws Exception {
        if (args.length < 1) {
            throw new Exception("Missing vault name");
        }
        String vaultName = args[0];
        if (args.length < 2) {
            throw new Exception("Missing upload id");
        }
        String uploadId = args[1];
        ListPartsResult parts = ListParts(vaultName,uploadId);
        System.out.printf("ArchiveDescription: %s\n",parts.getArchiveDescription());
        System.out.printf("CreationDate: %s\n",parts.getCreationDate());
        System.out.printf("MultipartUploadId: %s\n",parts.getMultipartUploadId());
        System.out.printf("PartSizeInBytes: %s\n",Humansize(parts.getPartSizeInBytes()));
        System.out.printf("VaultARN: %s\n",parts.getVaultARN());
        java.util.List<PartListElement> partlist = parts.getParts();
        if (partlist == null) {
            System.out.println("Parts: null");
        } else {
            System.out.println("Parts:");
            int index = 0;
            Iterator itr = partlist.iterator();
            while (itr.hasNext()) {
                index++;
                PartListElement part = (PartListElement)itr.next();
                String range = part.getRangeInBytes();
                String treeHash = part.getSHA256TreeHash();
                System.out.printf("    %2d: \n",index);
                System.out.printf("    RangeInBytes: %s\n",range);
                System.out.printf("    SHA256TreeHash: %s\n",treeHash);
            }
            if (index > 0) System.out.println("");
            if (index == 1) {
                System.out.printf("%d part.\n",index);
            } else {
                System.out.printf("%d parts.\n",index);
            }
        }
    }
    private void abortmulti(String args[]) throws Exception {
        if (args.length < 1) {
            throw new Exception("Missing vault name");
        }
        String vaultName = args[0];
        if (args.length < 2) {
            throw new Exception("Missing upload id");
        }
        String uploadId = args[1];
        AbortMultipartUpload(vaultName,uploadId);
    }
    private void getvault(String args[]) throws Exception {
        if (args.length < 1) {
            throw new Exception("Missing vault name");
        }
        String vaultName = args[0];
        Element vnode = findvaultbyname(vaultName);
        NodeList archives = vnode.getElementsByTagName("archive");
        for (int j=0; j < archives.getLength();j++) {
            Element a = (Element) archives.item(j);
            NodeList dtags = a.getElementsByTagName("description");
            String descr = a.getAttribute("archiveid");
            if (dtags != null && dtags.getLength() > 0) {
                Element dtag = (Element) dtags.item(0);
                descr = dtag.getTextContent();
            }
            String jargs[] = {"",""};
            jargs[0] = args[0];
            jargs[1] = descr;
            getarchive(jargs);
        }
    }
    private void getarchive(String args[]) throws Exception {
        if (args.length < 1) {
            throw new Exception("Missing vault name");
        }
        String vaultName = args[0];
        if (args.length < 2) {
            throw new Exception("Missing archive name");
        }
        String archive = args[1];
        String jobId = InitiateRetrieveArchiveJob(vaultName,archive,SNSTopic);
        System.out.printf("Job created, job id is %s\n",jobId);
    }
    private void deletevault(String args[]) throws Exception {
        if (args.length < 1) {
            throw new Exception("Missing vault name");
        }
        String vault = args[0];
        String response = super.deletevault(vault);
        if (response != null && response != "") savedb(GlacierVaultDB_File);
    }
    private void deletearchive(String args[]) throws Exception {
        if (args.length < 1) {
            throw new Exception("Missing vault name");
        }
        String vault = args[0];
        if (args.length < 2) {
            throw new Exception("Missing archive name");
        }
        String archive = args[1];
        String response = "";
        if (archive.compareTo("-aid") == 0) {
            if (args.length < 3) {
                throw new Exception("Missing archive id");
            }
            response = deletearchive_byaid(vault,args[2]);
        } else {
            response = super.deletearchive(vault,archive);
        }
        if (response != null && response != "") savedb(GlacierVaultDB_File);
    }
    private void createvault(String args[]) throws Exception {
        if (args.length < 1) {
            throw new Exception("Missing vault name");
        }
        String vault = args[0];
        Element e = super.CreateNewVault(vault);
        if (e != null) savedb(GlacierVaultDB_File);
    }
    private static SiteConfig configuration;
    public static void main(String args[]) throws Exception {
        configuration = new SiteConfig();
        String dbfile = configuration.GlacierVaultDB_FileName();
        String snstopic = configuration.SNSTopic();
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
        //System.err.printf("*** GlacierCommand.main(): dbfile is '%s'\n",dbfile);
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
    private void showhelp(String args[]) {
        if (args.length < 1) {
            System.out.println("Available commands: ");
            System.out.println("");
            System.out.println("   show vault [vaultname]");
            System.out.println("      Show vault. vaultname can be a wildcard or ommited (== *)");
            System.out.println("   show archive vaultname [archivename]");
            System.out.println("      Show archives in vaultname. archivename can be a wildcard or ommited (== *)");
            System.out.println("   show jobs vaultname");
            System.out.println("      List jobs for vaultname.");
            System.out.println("   show job vaultname jobid");
            System.out.println("      List job detail");
            System.out.println("   show uploads vaultname");
            System.out.println("      List multipart uploads currently in progress");
            System.out.println("   show parts vaultname uploadid");
            System.out.println("      List uploaded parts for uploadid");
            System.out.println("   show inventory vaultname jobid");
            System.out.println("      Display inventory job output");
            System.out.println("   ls   (see 'show')");
            System.out.println("   abort vaultname uploadid");
            System.out.println("      About the specificed multipart upload");
            System.out.println("   get archive vaultname archivename");
            System.out.println("      Start an archive retrieval job");
            System.out.println("   get inventory vaultname jobid");
            System.out.println("      Raw output of an inventory retrieval job");
            System.out.println("   delete vault vaultname");
            System.out.println("      Delete the specified vault.  It must be empty");
            System.out.println("   delete archive vaultname archivename");
            System.out.println("      Delete the specified archive");
            System.out.println("   rm   (see 'delete')");
            System.out.println("   inventory vaultname");
            System.out.println("      Start an inventory retrieval job"); 
            System.out.println("   treehash filename");
            System.out.println("      Compute the treehash of a local filename");
            System.out.println("   syncronize vaultname jobid");
            System.out.println("      Syncronize a vault's local inventory with a retrieved inventory.");
            System.out.println("   help [what]");
            System.out.println("      Get detailed help.");
            System.out.println("   exit");
            System.out.println("      Exit the program");
        } else {
            String verb = args[0];
            verb.toLowerCase();
            if (verb.compareTo("ls") == 0 ||
                verb.matches("^sh.*")) {
                if (args.length < 2) {
                    System.out.println("   show vault [vaultname]");
                    System.out.println("      Show vault. vaultname can be a wildcard or ommited (== *)");
                    System.out.println("   show archive vaultname [archivename]");
                    System.out.println("      Show archives in vaultname. archivename can be a wildcard or ommited (== *)");
                    System.out.println("   show jobs vaultname");
                    System.out.println("      List jobs for vaultname.");
                    System.out.println("   show job vaultname jobid");
                    System.out.println("      List job detail");
                    System.out.println("   show uploads vaultname");
                    System.out.println("      List multipart uploads currently in progress");
                    System.out.println("   show parts vaultname uploadid");
                    System.out.println("      List uploaded parts for uploadid");
                    System.out.println("   show inventory vaultname jobid");
                    System.out.println("      Display inventory job output");
                    System.out.println("   ls is an alias for show");
                } else {
                    String verb2 = args[1];
                    verb2.toLowerCase();
                    if (verb2.matches("^va.*")) {
                        System.out.println("      Show vault. vaultname can be a wildcard or ommited (== *)");
                    } else if (verb2.matches("^ar.*")) {
                        System.out.println("      Show archives in vaultname. archivename can be a wildcard or ommited (== *)");
                    } else if (verb2.compareTo("jobs") == 0) {
                        System.out.println("      List jobs for vaultname.");
                    } else if (verb2.compareTo("job") == 0) {
                        System.out.println("      List job detail");
                    } else if (verb2.matches("^up.*")) {
                        System.out.println("      List multipart uploads currently in progress");
                    } else if (verb2.matches("^pa.*")) {
                        System.out.println("      List uploaded parts for uploadid");
                    } else if (verb2.matches("^inv.*")) {
                        System.out.println("      Display inventory job output");
                    } else {
                        System.out.println("Huh?");
                    }
                }
            } else if (verb.matches("^abort.*")) {
                System.out.println("      About the specificed multipart upload");
            } else if (verb.compareTo("get") == 0) {
                if (args.length < 2) {
                    System.out.println("   get archive vaultname archivename");
                    System.out.println("      Start an archive retrieval job");
                    System.out.println("   get inventory vaultname jobid");
                    System.out.println("      Raw output of an inventory retrieval job");
                } else {
                    String verb2 = args[1];
                    verb2.toLowerCase();
                    if (verb2.matches("^arch.*")) {
                        System.out.println("      Start an archive retrieval job");
                    } else if (verb2.matches("^inv.*")) {
                        System.out.println("      Raw output of an inventory retrieval job");
                    } else {
                        System.out.println("Huh?");
                    }
                }
            } else if (verb.compareTo("rm") == 0 ||
                      verb.matches("^del.*")) {
                if (args.length < 2) {
                    System.out.println("   delete vault vaultname");
                    System.out.println("      Delete the specified vault.  It must be empty");
                    System.out.println("   delete archive vaultname archivename");
                    System.out.println("      Delete the specified archive");
                    System.out.println("   rm is an alias for  delete");
                } else {
                    String verb2 = args[1];
                    verb2.toLowerCase();
                    if (verb2.matches("^va.*")) {
                        System.out.println("      Delete the specified vault.  It must be empty");
                    } else if (verb2.matches("^ar.*")) {
                        System.out.println("      Delete the specified archive");
                    } else {
                        System.out.println("Huh?");
                    }
                }
            } else if (verb.matches("^inv.*")) {
                System.out.println("      Start an inventory retrieval job"); 
            } else if (verb.matches("^tree.*")) {
                System.out.println("      Compute the treehash of a local filename");
            } else if (verb.matches("^sync.*")) {
                System.out.println("      Syncronize a vault's local inventory with a retrieved inventory.");
            } else if (verb.compareTo("help") == 0) {
                System.out.println("      Get detailed help.");
            } else if (verb.compareTo("exit") == 0) {
                System.out.println("      Exit the program");
            } else {
                System.out.println("Huh?");
            }
        }
    }
}
        
