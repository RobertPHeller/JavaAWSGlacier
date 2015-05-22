/****************************************************************************
 *
 *  System        : 
 *  Module        : 
 *  Object Name   : $RCSfile$
 *  Revision      : $Revision$
 *  Date          : $Date$
 *  Author        : $Author$
 *  Created By    : Robert Heller
 *  Created       : Mon May 18 09:47:03 2015
 *  Last Modified : <150521.2002>
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
import java.security.NoSuchAlgorithmException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.glacier.AmazonGlacierClient;
import com.amazonaws.services.glacier.model.ListPartsResult;
import com.amazonaws.services.glacier.model.ListMultipartUploadsResult;
import com.amazonaws.services.glacier.model.PartListElement;
import com.amazonaws.services.glacier.model.UploadListElement;
import com.amazonaws.services.glacier.model.JobParameters;
import com.amazonaws.services.glacier.model.InventoryRetrievalJobInput;
import com.amazonaws.services.glacier.model.InitiateJobResult;
import com.amazonaws.services.glacier.model.ListJobsRequest;
import com.amazonaws.services.glacier.model.ListJobsResult;
import com.amazonaws.services.glacier.model.GlacierJobDescription;
import com.amazonaws.services.glacier.model.InventoryRetrievalJobDescription;
import com.amazonaws.services.glacier.model.GetJobOutputRequest;
import com.amazonaws.services.glacier.model.GetJobOutputResult;
import com.amazonaws.services.glacier.model.DescribeJobResult;
import com.deepsoft.*;


class main {
    static public void main(String args[]) {
        if (args.length < 1) {
            Usage();
        }
        String command = args[0];
        if (command.compareTo("TreeHash") == 0) {
            if (args.length < 2) {
                System.err.println("Missing required filename argument");
                Usage();
            }
            File inputFile = new File(args[1]);
            try {
                byte[] treeHash = TreeHash.computeSHA256TreeHash(inputFile);
                System.out.printf("SHA-256 Tree Hash of %s is %s\n",
                          inputFile, TreeHash.toHex(treeHash));
            } catch (IOException ioe) {
                System.err.format("Exception when reading from file %s: %s",
                          inputFile, ioe.getMessage());
                System.exit(-1);
            } catch (NoSuchAlgorithmException nsae) {
                System.err.format("Cannot locate MessageDigest algorithm for SHA-256: %s",
                          nsae.getMessage());
                System.exit(-1);
            }
        } else if (command.compareTo("CreateVault") == 0) {
            if (args.length < 2) {
                System.err.println("Missing required vaultName argument");
                Usage();
            }
            String vaultName = args[1];
            AWSCredentials credentials = null;
            try {
                String homedir = System.getProperty("user.home");
                File credfile = new File(homedir+"/.AwsCredentials");
                credentials = new PropertiesCredentials(credfile);
            } catch (IOException ioe) {
                System.err.println("Failed to get credentials: " + ioe.getMessage());
                System.exit(-1);
            }
        
            AmazonGlacierClient client = new AmazonGlacierClient(credentials);
            client.setEndpoint("https://glacier.us-east-1.amazonaws.com/");
            try {
                AmazonGlacierVaultOperations.createVault(client, vaultName);
            } catch (Exception e) {
                System.err.println("Vault operation failed." + e.getMessage());
            }
        } else if (command.compareTo("DescribeVault") == 0) {
            if (args.length < 2) {
                System.err.println("Missing required vaultName argument");
                Usage();
            }
            String vaultName = args[1];
            AWSCredentials credentials = null;
            try {
                String homedir = System.getProperty("user.home");
                File credfile = new File(homedir+"/.AwsCredentials");
                credentials = new PropertiesCredentials(credfile);
            } catch (IOException ioe) {
                System.err.println("Failed to get credentials: " + ioe.getMessage());
                System.exit(-1);
            }
        
            AmazonGlacierClient client = new AmazonGlacierClient(credentials);
            client.setEndpoint("https://glacier.us-east-1.amazonaws.com/");
            try {
                AmazonGlacierVaultOperations.describeVault(client, vaultName);
            } catch (Exception e) {
                System.err.println("Vault operation failed." + e.getMessage());
            }
        } else if (command.compareTo("ListVaults") == 0) {
            AWSCredentials credentials = null;
            try {
                String homedir = System.getProperty("user.home");
                File credfile = new File(homedir+"/.AwsCredentials");
                credentials = new PropertiesCredentials(credfile);
            } catch (IOException ioe) {
                System.err.println("Failed to get credentials: " + ioe.getMessage());
                System.exit(-1);
            }
        
            AmazonGlacierClient client = new AmazonGlacierClient(credentials);
            client.setEndpoint("https://glacier.us-east-1.amazonaws.com/");
            try {
                AmazonGlacierVaultOperations.listVaults(client);
            } catch (Exception e) {
                System.err.println("Vault operation failed." + e.getMessage());
            }
        } else if (command.compareTo("DeleteVault") == 0) {
            if (args.length < 2) {
                System.err.println("Missing required vaultName argument");
                Usage();
            }
            String vaultName = args[1];
            AWSCredentials credentials = null;
            try {
                String homedir = System.getProperty("user.home");
                File credfile = new File(homedir+"/.AwsCredentials");
                credentials = new PropertiesCredentials(credfile);
            } catch (IOException ioe) {
                System.err.println("Failed to get credentials: " + ioe.getMessage());
                System.exit(-1);
            }
        
            AmazonGlacierClient client = new AmazonGlacierClient(credentials);
            client.setEndpoint("https://glacier.us-east-1.amazonaws.com/");
            try {
                AmazonGlacierVaultOperations.deleteVault(client, vaultName);
            } catch (Exception e) {
                System.err.println("Vault operation failed." + e.getMessage());
            }
        } else if (command.compareTo("UploadArchive") == 0) {
            if (args.length < 2) {
                System.err.println("Missing required vaultName argument");
                Usage();
            }
            String vaultName = args[1];
            if (args.length < 3) {
                System.err.println("Missing required archiveFile argument");
                Usage();
            }
            File archiveFile = new File(args[2]);
            AWSCredentials credentials = null;
            try {
                String homedir = System.getProperty("user.home");
                File credfile = new File(homedir+"/.AwsCredentials");
                credentials = new PropertiesCredentials(credfile);
            } catch (IOException ioe) {
                System.err.println("Failed to get credentials: " + ioe.getMessage());
                System.exit(-1);
            }
        
            AmazonGlacierClient client = new AmazonGlacierClient(credentials);
            client.setEndpoint("https://glacier.us-east-1.amazonaws.com/");
            try {
                AmazonGlacierArchiveOperations.uploadArchive(client,vaultName,
                          archiveFile);
            } catch (Exception e) {
                System.err.println("Archive operation failed." + e.getMessage());
            }
        } else if (command.compareTo("ListParts") == 0) {
            if (args.length < 2) {
                System.err.println("Missing required vaultName argument");
                Usage();
            }
            String vaultName = args[1];
            if (args.length < 3) {
                System.err.println("Missing required uploadId argument");
                Usage();
            }
            String uploadId = args[2];
            int iopt = 3;
            String marker = null;
            String limit  = null;
            while (iopt < args.length) {
                if (args[iopt].compareTo("-marker") == 0 && 
                    (iopt+1) < args.length) {
                    marker = args[iopt+1];
                    iopt += 2;
                } else if (args[iopt].compareTo("-limit") == 0 &&
                          (iopt+1) < args.length) {
                    limit = args[iopt+1];
                    iopt += 2;
                } else {
                    System.err.println("Unknown option: "+args[iopt]+", should be either -marker or -limit");
                    Usage();
                }
            }
            AWSCredentials credentials = null;
            try {
                String homedir = System.getProperty("user.home");
                File credfile = new File(homedir+"/.AwsCredentials");
                credentials = new PropertiesCredentials(credfile);
            } catch (IOException ioe) {
                System.err.println("Failed to get credentials: " + ioe.getMessage());
                System.exit(-1);
            }
        
            AmazonGlacierClient client = new AmazonGlacierClient(credentials);
            client.setEndpoint("https://glacier.us-east-1.amazonaws.com/");
            try {
                ListPartsResult parts = AmazonGlacierArchiveOperations.listParts(client,vaultName,uploadId,marker,limit);
                System.out.print("ArchiveDescription {"+parts.getArchiveDescription()+"} ");
                System.out.print("CreationDate "+parts.getCreationDate()+" ");
                System.out.print("MultipartUploadId "+parts.getMultipartUploadId()+" ");
                System.out.print("PartSizeInBytes "+parts.getPartSizeInBytes()+" ");
                System.out.print("VaultARN "+parts.getVaultARN()+" ");
                java.util.List<PartListElement> partlist = parts.getParts();
                if (partlist == null) {
                    System.out.println("Parts {}");
                } else {
                    System.out.print("Parts {");
                    Iterator itr = partlist.iterator();
                    String sp0 = "";
                    while (itr.hasNext()) {
                        PartListElement part = (PartListElement)itr.next();
                        String range = part.getRangeInBytes();
                        String treeHash = part.getSHA256TreeHash();
                        System.out.print(sp0+"{");
                        String sp = "";
                        if (range != null) {
                            System.out.print("RangeInBytes "+range);
                            sp = " ";
                        }
                        if (treeHash != null) {
                            System.out.print(sp+"SHA256TreeHash "+treeHash);
                        }
                        System.out.print("}");
                        sp0 = " ";
                    }
                    System.err.println("}");
                }
            } catch (Exception e) {
                System.err.println("Archive operation failed." + e.getMessage());
            }
        } else if (command.compareTo("ListMultipartUploads") == 0) {
            if (args.length < 2) {
                System.err.println("Missing required vaultName argument");
                Usage();
            }
            String vaultName = args[1];
            int iopt = 2;
            String marker = null;
            String limit  = null;
            while (iopt < args.length) {
                if (args[iopt].compareTo("-marker") == 0 && 
                    (iopt+1) < args.length) {
                    marker = args[iopt+1];
                    iopt += 2;
                } else if (args[iopt].compareTo("-limit") == 0 &&
                          (iopt+1) < args.length) {
                    limit = args[iopt+1];
                    iopt += 2;
                } else {
                    System.err.println("Unknown option: "+args[iopt]+", should be either -marker or -limit");
                    Usage();
                }
            }
            AWSCredentials credentials = null;
            try {
                String homedir = System.getProperty("user.home");
                File credfile = new File(homedir+"/.AwsCredentials");
                credentials = new PropertiesCredentials(credfile);
            } catch (IOException ioe) {
                System.err.println("Failed to get credentials: " + ioe.getMessage());
                System.exit(-1);
            }
        
            AmazonGlacierClient client = new AmazonGlacierClient(credentials);
            client.setEndpoint("https://glacier.us-east-1.amazonaws.com/");
            try {
                ListMultipartUploadsResult uploads = AmazonGlacierArchiveOperations.listMultipartUploads(client,vaultName,marker,limit);
                java.util.List<UploadListElement> uploadsList = uploads.getUploadsList();
                if (uploadsList == null) {
                    System.out.println("{}");
                } else {
                    String sp0 = "";
                    Iterator itr = uploadsList.iterator();
                    while (itr.hasNext()) {
                        UploadListElement upload = (UploadListElement)itr.next();
                        System.out.print(sp0+"{");
                        String sp = "";
                        String uploadId = upload.getMultipartUploadId();
                        String vaultARN = upload.getVaultARN();
                        String archiveDescription = upload.getArchiveDescription();
                        Long partSizeInBytes = upload.getPartSizeInBytes();
                        String creationDate = upload.getCreationDate();
                        if (uploadId != null) {
                            System.out.print("MultipartUploadId "+uploadId);
                            sp = " ";
                        }
                        if (vaultARN != null) {
                            System.out.print(sp+"VaultARN "+vaultARN);
                            sp = " ";
                        }
                        if (archiveDescription != null) {
                            System.out.print(sp+"ArchiveDescription {"+archiveDescription+"}");
                            sp = " ";
                        }
                        if (partSizeInBytes != 0) {
                            System.out.print(sp+"PartSizeInBytes "+partSizeInBytes);
                            sp = " ";
                        }
                        if (creationDate != null) {
                            System.out.print(sp+"CreationDate "+creationDate);
                        }
                        System.out.print("}");
                        sp0 = " ";
                    }
                    System.out.println("");
                }
            } catch (Exception e) {
                System.err.println("Archive operation failed." + e.getMessage());
            }
        } else if (command.compareTo("AbortMultipartUpload") == 0) {
            if (args.length < 2) {
                System.err.println("Missing required vaultName argument");
                Usage();
            }
            String vaultName = args[1];
            if (args.length < 3) {
                System.err.println("Missing required uploadId argument");
                Usage();
            }
            String uploadId = args[2];
            AWSCredentials credentials = null;
            try {
                String homedir = System.getProperty("user.home");
                File credfile = new File(homedir+"/.AwsCredentials");
                credentials = new PropertiesCredentials(credfile);
            } catch (IOException ioe) {
                System.err.println("Failed to get credentials: " + ioe.getMessage());
                System.exit(-1);
            }
        
            AmazonGlacierClient client = new AmazonGlacierClient(credentials);
            client.setEndpoint("https://glacier.us-east-1.amazonaws.com/");
            try {
                AmazonGlacierArchiveOperations.abortMultipartUpload(client,vaultName,uploadId);
                System.out.println(vaultName+" "+uploadId);
            } catch (Exception e) {
                System.err.println("Failed to abort Multipart Upload "+vaultName+"/"+uploadId+": "+e.getMessage());
            }
        } else if (command.compareTo("DeleteArchive") == 0) {
            if (args.length < 2) {
                System.err.println("Missing required vaultName argument");
                Usage();
            }
            String vaultName = args[1];
            if (args.length < 3) {
                System.err.println("Missing required archiveId argument");
                Usage();
            }
            String archiveId = args[2];
            AWSCredentials credentials = null;
            try {
                String homedir = System.getProperty("user.home");
                File credfile = new File(homedir+"/.AwsCredentials");
                credentials = new PropertiesCredentials(credfile);
            } catch (IOException ioe) {
                System.err.println("Failed to get credentials: " + ioe.getMessage());
                System.exit(-1);
            }
        
            AmazonGlacierClient client = new AmazonGlacierClient(credentials);
            client.setEndpoint("https://glacier.us-east-1.amazonaws.com/");
            try {
                AmazonGlacierArchiveOperations.deleteArchive(client,vaultName,archiveId);
                System.out.println(vaultName+" "+archiveId);
            } catch (Exception e) {
                System.err.println("Failed to delete archive "+vaultName+"/"+archiveId+": "+e.getMessage());
            }
        } else if (command.compareTo("InitiateJob") == 0) {
            if (args.length < 2) {
                System.err.println("Missing required vaultName argument");
                Usage();
            }
            String vaultName = args[1];
            if (args.length < 3) {
                System.err.println("Missing required jobtype argument");
                Usage();
            }
            String jobtype = args[2];
            String jobstring = vaultName+" "+jobtype;
            JobParameters jobParams = new JobParameters().withType(jobtype);
            InventoryRetrievalJobInput inventoryParams = null;
            int iopt = 3;
            if (jobtype.compareTo("archive-retrieval") == 0) {
                if (args.length < (iopt+1)) {
                    System.err.println("Missing required archiveId argument");
                    Usage();
                }
                jobstring += " "+args[iopt];
                jobParams.setArchiveId(args[iopt++]);
            } else if (jobtype.compareTo("inventory-retrieval") == 0) {
            } else {
                System.err.println("Undefined job type: "+jobtype);
                Usage();
            }
            
            while (iopt < args.length) {
                if (iopt+1 < args.length) {
                    jobstring += " "+args[iopt]+" "+args[iopt+1];
                }
                if (args[iopt].compareTo("-startdate") == 0 &&
                    (iopt+1) < args.length &&
                    jobtype.compareTo("inventory-retrieval") == 0) {
                    if (inventoryParams == null) {
                        inventoryParams = new InventoryRetrievalJobInput();
                    }
                    
                    inventoryParams.setStartDate(args[iopt+1]);
                    iopt += 2;
                } else if (args[iopt].compareTo("-enddate") == 0 &&
                          (iopt+1) < args.length &&
                          jobtype.compareTo("inventory-retrieval") == 0) {
                    if (inventoryParams == null) {
                        inventoryParams = new InventoryRetrievalJobInput();
                    }
                    inventoryParams.setEndDate(args[iopt+1]);
                    iopt += 2;
                } else if (args[iopt].compareTo("-marker") == 0 &&
                          (iopt+1) < args.length &&
                          jobtype.compareTo("inventory-retrieval") == 0) {
                    if (inventoryParams == null) {
                        inventoryParams = new InventoryRetrievalJobInput();
                    }
                    inventoryParams.setMarker(args[iopt+1]);
                    iopt += 2;
                } else if (args[iopt].compareTo("-limit") == 0 &&
                          (iopt+1) < args.length &&
                          jobtype.compareTo("inventory-retrieval") == 0) {
                    if (inventoryParams == null) {
                        inventoryParams = new InventoryRetrievalJobInput();
                    }
                    inventoryParams.setLimit(args[iopt+1]);
                    iopt += 2;
                } else if (args[iopt].compareTo("-format") == 0 &&
                          (iopt+1) < args.length &&
                          jobtype.compareTo("inventory-retrieval") == 0) {
                    jobParams.setFormat(args[iopt+1]);
                    iopt += 2;
                } else if (args[iopt].compareTo("-description") == 0 &&
                          (iopt+1) < args.length) {
                    jobParams.setDescription(args[iopt+1]);
                    iopt += 2;
                } else if (args[iopt].compareTo("-snstopic") == 0 &&
                          (iopt+1) < args.length) {
                    jobParams.setSNSTopic(args[iopt+1]);
                    iopt += 2;
                } else {
                    System.err.println("Unknown option: "+args[iopt]+", should be one of -startdate, -enddate, -marker, -limit, -format, -description, or -snstopic");
                    Usage();
                }
            }
            if (inventoryParams != null) {
                jobParams.setInventoryRetrievalParameters(inventoryParams);
            }
            AWSCredentials credentials = null;
            try {
                String homedir = System.getProperty("user.home");
                File credfile = new File(homedir+"/.AwsCredentials");
                credentials = new PropertiesCredentials(credfile);
            } catch (IOException ioe) {
                System.err.println("Failed to get credentials: " + ioe.getMessage());
                System.exit(-1);
            }
        
            AmazonGlacierClient client = new AmazonGlacierClient(credentials);
            client.setEndpoint("https://glacier.us-east-1.amazonaws.com/");
            try {
                InitiateJobResult jResult = AmazonGlacierJobOperations.initiateJob(client,vaultName,jobParams);
                System.out.println(jResult.getJobId());
            } catch (Exception e) {
                System.err.println("Failed to initiate job "+jobstring+": "+e.getMessage());
            }
        } else if (command.compareTo("ListJobs") == 0) {
            if (args.length < 2) {
                System.err.println("Missing required vaultName argument");
                Usage();
            }
            String vaultName = args[1];
            ListJobsRequest ljRequest = new ListJobsRequest().
                  withVaultName(vaultName);
            int iopt = 2;
            String jobstring = vaultName;
            while (iopt < args.length) {
                if (iopt+1 < args.length) {
                    jobstring += " "+args[iopt]+" "+args[iopt+1];
                }
                if (args[iopt].compareTo("-completed") == 0 &&
                    (iopt+1) < args.length) {
                    boolean completed = false;
                    try {
                        completed = StringToBoolean(args[iopt+1]);
                    } catch (IllegalArgumentException iae) {
                        System.err.println("Bad value for -completed: "+iae.getMessage());
                        Usage();
                    }
                    if (completed) {
                        ljRequest.setCompleted("true");
                    } else {
                        ljRequest.setCompleted("false");
                    }
                } else if (args[iopt].compareTo("-limit") == 0 &&
                          (iopt+1) < args.length) {
                    ljRequest.setLimit(args[iopt+1]);
                } else if (args[iopt].compareTo("-marker") == 0 &&
                          (iopt+1) < args.length) {
                    ljRequest.setMarker(args[iopt+1]);
                } else if (args[iopt].compareTo("-statuscode") == 0 &&
                          (iopt+1) < args.length) {
                    ljRequest.setStatuscode(args[iopt+1]);
                } else {
                    System.err.println("Unknown option: "+args[iopt]+", should be one of  -completed, -limit, -marker, or -statuscode");
                    Usage();
                }
                iopt += 2;
            }
            AWSCredentials credentials = null;
            try {
                String homedir = System.getProperty("user.home");
                File credfile = new File(homedir+"/.AwsCredentials");
                credentials = new PropertiesCredentials(credfile);
            } catch (IOException ioe) {
                System.err.println("Failed to get credentials: " + ioe.getMessage());
                System.exit(-1);
            }
        
            AmazonGlacierClient client = new AmazonGlacierClient(credentials);
            client.setEndpoint("https://glacier.us-east-1.amazonaws.com/");
            try {
                ListJobsResult ljResult = AmazonGlacierJobOperations.listJobs(client,vaultName,ljRequest);
                java.util.List<GlacierJobDescription> jobList = ljResult.getJobList();
                if (jobList == null) {
                    System.out.println("{}");
                } else {
                    String sp0 = "";
                    Iterator itr = jobList.iterator();
                    while (itr.hasNext()) {
                        GlacierJobDescription job = (GlacierJobDescription)itr.next();
                        System.out.print(sp0+"{");
                        String sp = "";
                        String jobId = job.getJobId();
                        String jobDescription = job.getJobDescription();
                        String action = job.getAction();
                        String archiveId = job.getArchiveId();
                        String vaultARN = job.getVaultARN();
                        String creationDate = job.getCreationDate();
                        Boolean completed = job.getCompleted();
                        String statusCode = job.getStatusCode();
                        String statusMessage = job.getStatusMessage();
                        Long archiveSizeInBytes = job.getArchiveSizeInBytes();
                        Long inventorySizeInBytes = job.getInventorySizeInBytes();
                        String sNSTopic = job.getSNSTopic();
                        String completionDate = job.getCompletionDate();
                        String sHA256TreeHash = job.getSHA256TreeHash();
                        String archiveSHA256TreeHash = job.getArchiveSHA256TreeHash();
                        String retrievalByteRange = job.getRetrievalByteRange();
                        InventoryRetrievalJobDescription inventoryRetrievalParameters = job.getInventoryRetrievalParameters();
                        if (jobId != null) {
                            System.out.print("JobId "+jobId);
                            sp = " ";
                        }
                        if (jobDescription != null) {
                            System.out.print(sp+"JobDescription "+jobDescription);
                            sp = " ";
                        }
                        if (action != null) {
                            System.out.print(sp+"Action "+action);
                            sp = " ";
                        }
                        if (archiveId != null) {
                            System.out.print(sp+"ArchiveId "+archiveId);
                            sp = " ";
                        }
                        if (vaultARN != null) {
                            System.out.print(sp+"VaultARN "+vaultARN);
                            sp = " ";
                        }
                        if (creationDate != null) {
                            System.out.print(sp+"CreationDate "+creationDate);
                            sp = " ";
                        }
                        System.out.print(sp+"Completed = "+completed);
                        sp = " ";
                        if (statusCode != null) {
                            System.out.print(sp+"StatusCode "+statusCode);
                            sp = " ";
                        }
                        if (statusMessage != null) {
                            System.out.print(sp+"StatusMessage "+statusMessage);
                            sp = " ";
                        }
                        System.out.print(sp+"ArchiveSizeInBytes "+archiveSizeInBytes);
                        sp = " ";
                        System.out.print(sp+"InventorySizeInBytes "+inventorySizeInBytes);
                        sp = " ";
                        if (sNSTopic != null) {
                            System.out.print(sp+"SNSTopic "+sNSTopic);
                            sp = " ";
                        }
                        if (completionDate != null) {
                            System.out.print(sp+"CompletionDate "+completionDate);
                            sp = " ";
                        }
                        if (sHA256TreeHash != null) {
                            System.out.print(sp+"SHA256TreeHash "+sHA256TreeHash);
                            sp = " ";
                        }
                        if (archiveSHA256TreeHash != null) {
                            System.out.print(sp+"ArchiveSHA256TreeHash "+archiveSHA256TreeHash);
                            sp = " ";
                        }
                        if (retrievalByteRange != null) {
                            System.out.print(sp+"RetrievalByteRange "+retrievalByteRange);
                            sp = " ";
                        }
                        if (inventoryRetrievalParameters != null) {
                            String sp1 = "";
                            System.out.print(sp+"InventoryRetrievalParameters {");
                            String format = inventoryRetrievalParameters.getFormat();
                            String startDate = inventoryRetrievalParameters.getStartDate();
                            String endDate = inventoryRetrievalParameters.getEndDate();
                            String limit = inventoryRetrievalParameters.getLimit();
                            String marker = inventoryRetrievalParameters.getMarker();
                            if (format != null) {
                                System.out.print("Format "+format);
                                sp1 = " ";
                            }
                            if (startDate != null) {
                                System.out.print(sp1+"StartDate "+startDate);
                                sp1 = " ";
                            }
                            if (endDate != null) {
                                System.out.print(sp1+"EndDate "+endDate);
                                sp1 = " ";
                            }
                            if (limit != null) {
                                System.out.print(sp1+"Limit "+limit);
                                sp1 = " ";
                            }
                            if (marker != null) {
                                System.out.print(sp1+"Marker "+marker);
                            }
                            System.out.print("}");
                        }
                        System.out.print("}");
                        sp0 = " ";
                    }
                    System.out.println("");
                }
            } catch (Exception e) {
                System.err.println("Failed to list jobs "+jobstring+": "+e.getMessage());
            }
        } else if (command.compareTo("DescribeJob") == 0) {
            if (args.length < 2) {
                System.err.println("Missing required vaultName argument");
                Usage();
            }
            String vaultName = args[1];
            if (args.length < 3) {
                System.err.println("Missing required jobId argument");
                Usage();
            }
            String jobId = args[2];
            AWSCredentials credentials = null;
            try {
                String homedir = System.getProperty("user.home");
                File credfile = new File(homedir+"/.AwsCredentials");
                credentials = new PropertiesCredentials(credfile);
            } catch (IOException ioe) {
                System.err.println("Failed to get credentials: " + ioe.getMessage());
                System.exit(-1);
            }
        
            AmazonGlacierClient client = new AmazonGlacierClient(credentials);
            client.setEndpoint("https://glacier.us-east-1.amazonaws.com/");
            try {
                DescribeJobResult job = AmazonGlacierJobOperations.describeJob(client,vaultName,jobId);
String sp = "";
                //String jobId = job.getJobId();
                String jobDescription = job.getJobDescription();
                String action = job.getAction();
                String archiveId = job.getArchiveId();
                String vaultARN = job.getVaultARN();
                String creationDate = job.getCreationDate();
                Boolean completed = job.getCompleted();
                String statusCode = job.getStatusCode();
                String statusMessage = job.getStatusMessage();
                Long archiveSizeInBytes = job.getArchiveSizeInBytes();
                Long inventorySizeInBytes = job.getInventorySizeInBytes();
                String sNSTopic = job.getSNSTopic();
                String completionDate = job.getCompletionDate();
                String sHA256TreeHash = job.getSHA256TreeHash();
                String archiveSHA256TreeHash = job.getArchiveSHA256TreeHash();
                String retrievalByteRange = job.getRetrievalByteRange();
                InventoryRetrievalJobDescription inventoryRetrievalParameters = job.getInventoryRetrievalParameters();
                System.out.print("JobId "+jobId);
                sp = " ";
                if (jobDescription != null) {
                    System.out.print(sp+"JobDescription "+jobDescription);
                    sp = " ";
                }
                if (action != null) {
                    System.out.print(sp+"Action "+action);
                    sp = " ";
                }
                if (archiveId != null) {
                    System.out.print(sp+"ArchiveId "+archiveId);
                    sp = " ";
                }
                if (vaultARN != null) {
                    System.out.print(sp+"VaultARN "+vaultARN);
                    sp = " ";
                }
                if (creationDate != null) {
                    System.out.print(sp+"CreationDate "+creationDate);
                    sp = " ";
                }
                System.out.print(sp+"Completed = "+completed);
                sp = " ";
                if (statusCode != null) {
                    System.out.print(sp+"StatusCode "+statusCode);
                    sp = " ";
                }
                if (statusMessage != null) {
                    System.out.print(sp+"StatusMessage "+statusMessage);
                    sp = " ";
                }
                System.out.print(sp+"ArchiveSizeInBytes "+archiveSizeInBytes);
                sp = " ";
                System.out.print(sp+"InventorySizeInBytes "+inventorySizeInBytes);
                sp = " ";
                if (sNSTopic != null) {
                    System.out.print(sp+"SNSTopic "+sNSTopic);
                    sp = " ";
                }
                if (completionDate != null) {
                    System.out.print(sp+"CompletionDate "+completionDate);
                    sp = " ";
                }
                if (sHA256TreeHash != null) {
                    System.out.print(sp+"SHA256TreeHash "+sHA256TreeHash);
                    sp = " ";
                }
                if (archiveSHA256TreeHash != null) {
                    System.out.print(sp+"ArchiveSHA256TreeHash "+archiveSHA256TreeHash);
                    sp = " ";
                }
                if (retrievalByteRange != null) {
                    System.out.print(sp+"RetrievalByteRange "+retrievalByteRange);
                    sp = " ";
                }
                if (inventoryRetrievalParameters != null) {
                    String sp1 = "";
                    System.out.print(sp+"InventoryRetrievalParameters {");
                    String format = inventoryRetrievalParameters.getFormat();
                    String startDate = inventoryRetrievalParameters.getStartDate();
                    String endDate = inventoryRetrievalParameters.getEndDate();
                    String limit = inventoryRetrievalParameters.getLimit();
                    String marker = inventoryRetrievalParameters.getMarker();
                    if (format != null) {
                        System.out.print("Format "+format);
                        sp1 = " ";
                    }
                    if (startDate != null) {
                        System.out.print(sp1+"StartDate "+startDate);
                        sp1 = " ";
                    }
                    if (endDate != null) {
                        System.out.print(sp1+"EndDate "+endDate);
                        sp1 = " ";
                    }
                    if (limit != null) {
                        System.out.print(sp1+"Limit "+limit);
                        sp1 = " ";
                    }
                    if (marker != null) {
                        System.out.print(sp1+"Marker "+marker);
                    }
                    System.out.print("}");
                }                
            } catch (Exception e) {
                System.err.println("Failed to describe job "+vaultName+" "+jobId+": "+e.getMessage());
            }
        } else if (command.compareTo("GetJobOutput") == 0) {
            if (args.length < 2) {
                System.err.println("Missing required vaultName argument");
                Usage();
            }
            String vaultName = args[1];
            if (args.length < 3) {
                System.err.println("Missing required jobId argument");
                Usage();
            }
            String jobId = args[2];
            AWSCredentials credentials = null;
            try {
                String homedir = System.getProperty("user.home");
                File credfile = new File(homedir+"/.AwsCredentials");
                credentials = new PropertiesCredentials(credfile);
            } catch (IOException ioe) {
                System.err.println("Failed to get credentials: " + ioe.getMessage());
                System.exit(-1);
            }
        
            AmazonGlacierClient client = new AmazonGlacierClient(credentials);
            client.setEndpoint("https://glacier.us-east-1.amazonaws.com/");
        } else {
            System.err.println("Missing required command");
            Usage();
        }
    }
    static public void Usage() {
        System.err.println("JavaAWSGlacier.jar command [args...]");
        System.err.println("");
        System.err.println("Where command is:");
        System.err.println("");
        System.err.println(" TreeHash filename");
        System.err.println("");
        System.err.println(" CreateVault newvaultname");
        System.err.println(" DescribeVault vaultname");
        System.err.println(" ListVaults");
        System.err.println(" DeleteVault vaultname");
        System.err.println("");
        //System.err.println(" InitiateMultipartUpload vaultname partsize description");
        //System.err.println(" UploadMultipartPart vaultname uploadid uploadpartfile range");
        //System.err.println(" CompleteMultipartUpload vaultname uploadid size sha256_tree_hash");
        System.err.println(" UploadArchive vaultname archivefile");
        System.err.println(" ListParts vaultname uploadid opts...");
        System.err.println(" ListMultipartUploads vaultname opts...");
        System.err.println(" AbortMultipartUpload vaultname uploadid");
        System.err.println(" DeleteArchive vaultname archiveid");
        System.err.println("");
        System.err.println(" InitiateJob vaultname jobtype jobparameters...");
        System.err.println(" ListJobs vaultname opts...");
        System.err.println(" DescribeJob vaultname jobid");
        System.err.println(" GetJobOutput vaultname jobid [-output filename]");
        System.exit(-1);
    }
    private static boolean StringToBoolean(String boolstring) throws IllegalArgumentException {
        if (boolstring.compareTo("true") == 0) {
            return true;
        } else if (boolstring.compareTo("false") == 0) {
            return false;
        } else if (boolstring.compareTo("yes") == 0) {
            return true;
        } else if (boolstring.compareTo("no") == 0) {
            return false;
        } else if (boolstring.compareTo("1") == 0) {
            return true;
        } else if (boolstring.compareTo("0") == 0) {
            return false;
        } else {
            throw new IllegalArgumentException("Expected true/false/yes/no/1/0, got "+boolstring);
        }
    }
                
}

