/****************************************************************************
 *
 *  System        : 
 *  Module        : 
 *  Object Name   : $RCSfile$
 *  Revision      : $Revision$
 *  Date          : $Date$
 *  Author        : $Author$
 *  Created By    : Robert Heller
 *  Created       : Sat May 23 14:21:22 2015
 *  Last Modified : <150913.1231>
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
import java.text.SimpleDateFormat;
import java.security.NoSuchAlgorithmException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.glacier.TreeHashGenerator;
import com.amazonaws.services.glacier.AmazonGlacierClient;
import com.amazonaws.services.glacier.model.ListPartsResult;
import com.amazonaws.services.glacier.model.ListMultipartUploadsResult;
import com.amazonaws.services.glacier.model.JobParameters;
import com.amazonaws.services.glacier.model.InitiateJobResult;
import com.amazonaws.services.glacier.model.ListJobsRequest;
import com.amazonaws.services.glacier.model.ListJobsResult;
import com.amazonaws.services.glacier.model.DescribeJobResult;
import com.amazonaws.services.glacier.model.GetJobOutputResult;
import com.amazonaws.services.glacier.model.DescribeVaultRequest;
import com.amazonaws.services.glacier.model.DescribeVaultResult;
import org.w3c.dom.*;
import com.deepsoft.VaultXMLDB;





class BackupVault extends VaultXMLDB {
    private final SimpleDateFormat dateFormatter = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z");
    private final int DefaultPartsize = 256 * 1024 * 1024;
    private final String glacierTemp = "/home/AmazonGlacierTemp";
    private int tempFileindex = 1;
    private AmazonGlacierClient client;
    public BackupVault(String dbfile) throws Exception {
        this(new File(dbfile));
    }
    public BackupVault(File file) throws Exception {
        super(file,false);
        AWSCredentials credentials = null;
        String homedir = System.getProperty("user.home");
        File credfile = new File(homedir+"/.AwsCredentials");
        credentials = new PropertiesCredentials(credfile);
        client = new AmazonGlacierClient(credentials);
        client.setEndpoint("https://glacier.us-east-1.amazonaws.com/");
    }
    private File generateTempfile() {
        while (true) {
            Formatter f = new Formatter();
            f.format("TMP%08x",tempFileindex);
            File filename = new File(glacierTemp,f.toString());
            if (!filename.exists()) {
                return filename;
            }
            tempFileindex++;
        }
    }
    public Element CreateNewVault (String vaultName) throws Exception {
        String loc = AmazonGlacierVaultOperations.createVault(client, vaultName);
        String date = dateFormatter.format(new Date());
        return addvault(loc,date.toString());
    }
    public Element UploadArchive (String vaultName, String archivefile) throws Exception {
        return UploadArchive(vaultName,archivefile,DefaultPartsize);
    }
    public Element UploadArchive (String vaultName, String archivefile, int partsize) throws Exception {
        File archiveFile = new File(archivefile);
        try {
            AmazonGlacierArchiveOperations.UploadResult result = 
                  AmazonGlacierArchiveOperations.uploadArchive(client,vaultName,
                            archiveFile,partsize);
            String date = dateFormatter.format(new Date());
            Long lsize = new Long(archiveFile.length());
            String size = lsize.toString();
            return addarchive(result.location,date.toString(),size,result.sha256treehash,archiveFile.getName());
        } catch (Exception e) {
            System.out.printf("Archive upload failed %s: %s because %s\n",
                      vaultName,archivefile,e.getMessage());
            return null;
        }
    }
    public ListPartsResult ListParts(String vaultName, String uploadId) throws Exception {
        return ListParts(vaultName,uploadId,null,null);
    }
    public ListPartsResult ListParts(String vaultName, String uploadId,String marker, String limit) throws Exception {
        return AmazonGlacierArchiveOperations.listParts(client,vaultName,uploadId,marker,limit);
    }
    public ListMultipartUploadsResult ListMultiPartUploads(String vaultName) throws Exception {
        return ListMultiPartUploads(vaultName,null,null);
    }
    public ListMultipartUploadsResult ListMultiPartUploads(String vaultName,String marker, String limit) throws Exception {
        return AmazonGlacierArchiveOperations.listMultipartUploads(client,vaultName,marker,limit);
    }
    public void AbortMultipartUpload(String vaultName,String uploadId) throws Exception {
        AmazonGlacierArchiveOperations.abortMultipartUpload(client,vaultName,uploadId);
    }
    public String InitiateRetrieveArchiveJob(String vaultName, String archive, String snstopic) throws Exception {
        Element vnode = findvaultbyname(vaultName);
        if (vnode == null) {
            throw new Exception("No such vault: "+vaultName);
        }
        Element anode = findarchivebydescr(vnode,archive);
        if (anode == null) {
            throw new Exception("No such archive in "+vaultName+": "+archive);
        }
        JobParameters jobParams = new JobParameters()
              .withType("archive-retrieval")
              .withArchiveId(anode.getAttribute("archiveid"))
              .withSNSTopic(snstopic);
        InitiateJobResult jResult = AmazonGlacierJobOperations.initiateJob(client,vaultName,jobParams);
        return jResult.getJobId();
    }
    String InitiateRetrieveInventory(String vaultName,JobParameters jobParams) throws Exception {
        InitiateJobResult jResult = AmazonGlacierJobOperations.initiateJob(client,vaultName,jobParams);
        return jResult.getJobId();
    }
    private boolean StringToBoolean(String boolstring) throws IllegalArgumentException {
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
    public ListJobsResult GetJobList(String vaultName, String[] args) throws Exception {
        Element vnode = findvaultbyname(vaultName);
        if (vnode == null) {
            throw new Exception("No such vault: "+vaultName);
        }
        ListJobsRequest ljRequest = new ListJobsRequest()
              .withVaultName(vaultName);
        int iopt = 0;
        while (iopt < args.length) {
            if (args[iopt].compareTo("-completed") == 0 &&
                (iopt+1) < args.length) {
                boolean completed = false;
                completed = StringToBoolean(args[iopt+1]);
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
                throw new Exception("Unknown option: "+args[iopt]+", should be one of  -completed, -limit, -marker, or -statuscode");
            }
            iopt += 2;
        }
        return AmazonGlacierJobOperations.listJobs(client,vaultName,ljRequest);
    }
    public DescribeJobResult GetJobDescription(String vaultName,String jobId) throws Exception {
        Element vnode = findvaultbyname(vaultName);
        if (vnode == null) {
            throw new Exception("No such vault: "+vaultName);
        }
        return AmazonGlacierJobOperations.describeJob(client,vaultName,jobId);
    }
    public String getJobBody(String vault,String jobid) throws Exception {
        GetJobOutputResult result = AmazonGlacierJobOperations.getJobOutput(client,vault,jobid,null);
        java.io.InputStream bodyStream = result.getBody();
        int bodysize = bodyStream.available();
        byte buffer[] = new byte[bodysize];
        bodyStream.read(buffer);
        return new String(buffer);
    }
    public String RetrieveArchive(String vault, String archiveid, String jobid, String filename, long size, String range, String treehash, String wholetreehash) throws Exception {
        long first = 0, last = 0;
        if (range.matches("^[0-9]+-[0-9]+$")) {
            String m[] = range.split("-");
            first = Long.parseLong(m[0]);
            last  = Long.parseLong(m[1]);
        } else {
            throw new Exception("Illformed range: "+range);
        }
        long partialsize = (last-first)+1;
        String result = null;
        if (partialsize == size) {
            result = RetrieveWholeArchive(vault,archiveid,jobid,filename,wholetreehash,size);
        } else {
            result = RetrievePartArchive(vault,archiveid,jobid,filename,treehash,partialsize);
        }
        if (result == null) {
            return null;
        } else {
            return filename;
        }
    }
    private String RetrieveWholeArchive(String vault, String archiveid, String jobid, String filename, String wholetreehash, long size) throws Exception {
        return RetrieveWholeArchive(vault,archiveid,jobid,filename,wholetreehash,size,DefaultPartsize);
    }
    private String RetrieveWholeArchive(String vault, String archiveid, String jobid, String filename, String wholetreehash, long size, int partsize) throws Exception {
        if (size > (long)partsize) {
            return RetrieveWholeArchiveInParts(vault,archiveid,jobid,filename,wholetreehash,size,partsize);
        } else {
            GetJobOutputResult result = AmazonGlacierJobOperations.getJobOutput(client,vault,jobid,null);
            java.io.InputStream bodyStream = result.getBody();
            byte buffer[] = new byte[partsize];
            int bytesRead;
            
            String Checksum = result.getChecksum();
            File file = new File(filename);
            FileOutputStream of = new FileOutputStream(file);
            while ((bytesRead = bodyStream.read(buffer, 0, partsize)) > 0) {
                of.write(buffer, 0, bytesRead);
            }
            of.close();
            
            String computedTreeHash = TreeHashGenerator.calculateTreeHash(file);
            if (computedTreeHash.compareTo(wholetreehash) != 0 ||
                computedTreeHash.compareTo(Checksum) != 0) {
                throw new Exception("Archive SHA256 Tree Hash failure: locally computed: "+computedTreeHash+", returned: "+Checksum+", in job descr: "+wholetreehash);
            }
            return filename;
        }
    }
    private String RetrieveWholeArchiveInParts(String vault, String archiveid, String jobid, String filename, String wholetreehash, long size,int thepartsize) throws Exception {
        File downloadpartfile = generateTempfile();
        File file = new File(filename);
        FileOutputStream fp = new FileOutputStream(file);
        boolean done = false;
        long pos = 0;
        long remainder = size;
        int partsize = 0;
        while (!done) {
            if (remainder > (long)thepartsize) {
                partsize = thepartsize;
            } else {
                partsize = (int)remainder;
            }
            Formatter f = new Formatter();
            f.format("bytes=%d-%d",pos,(pos+partsize)-1);
            String range = f.toString();
            GetJobOutputResult result = AmazonGlacierJobOperations.getJobOutput(client,vault,jobid,range);
            java.io.InputStream bodyStream = result.getBody();
            byte buffer[] = new byte[partsize];
            int bytesRead;
            
            String Checksum = result.getChecksum();
            FileOutputStream of = new FileOutputStream(downloadpartfile);
            while ((bytesRead = bodyStream.read(buffer, 0, partsize)) > 0) {
                of.write(buffer, 0, bytesRead);
            }
            of.close();
            
            String computedTreeHash = TreeHashGenerator.calculateTreeHash(downloadpartfile);
            if (computedTreeHash.compareTo(Checksum) != 0) {
                throw new Exception("Archive part ("+range+") SHA256 Tree Hash failure: locally computed: "+computedTreeHash+", returned: "+Checksum);
            }
            FileInputStream dfp = new FileInputStream(downloadpartfile);
            while ((bytesRead = dfp.read(buffer, 0, partsize)) > 0) {
                fp.write(buffer, 0, bytesRead);
            }
            dfp.close();
            pos += partsize;
            remainder -= partsize;
            done = pos >= size;
        }
        fp.close();
        String computedTreeHash = TreeHashGenerator.calculateTreeHash(file);
        if (computedTreeHash.compareTo(wholetreehash) != 0) {
            throw new Exception("Archive SHA256 Tree Hash failure: locally computed: "+computedTreeHash+", in job descr: "+wholetreehash);
        } else {
            return filename;
        }
    }
    private String RetrievePartArchive(String vault,String archiveid,String jobid,String filename,String treehash,long partialsize) throws Exception {
        return RetrievePartArchive(vault,archiveid,jobid,filename,treehash,partialsize,DefaultPartsize);
    }
    private String RetrievePartArchive(String vault,String archiveid,String jobid,String filename,String treehash,long partialsize,int thepartsize) throws Exception {
        if (partialsize > (long)thepartsize) {
            return RetrievePartArchivePartArchiveInParts(vault,archiveid,jobid,filename,treehash,partialsize,thepartsize);
        } else {
            GetJobOutputResult result = AmazonGlacierJobOperations.getJobOutput(client,vault,jobid,null);
            java.io.InputStream bodyStream = result.getBody();
            byte buffer[] = new byte[thepartsize];
            int bytesRead;
            
            String Checksum = result.getChecksum();
            File file = new File(filename);
            FileOutputStream of = new FileOutputStream(file);
            while ((bytesRead = bodyStream.read(buffer, 0, thepartsize)) > 0) {
                of.write(buffer, 0, bytesRead);
            }
            of.close();
            
            String computedTreeHash = TreeHashGenerator.calculateTreeHash(file);
            if (computedTreeHash.compareTo(treehash) != 0 ||
                computedTreeHash.compareTo(Checksum) != 0) {
                throw new Exception("Archive SHA256 Tree Hash failure: locally computed: "+computedTreeHash+", returned: "+Checksum+", in job descr: "+treehash);
            }
            return filename;
        }
    }
    private String RetrievePartArchivePartArchiveInParts(String vault,String archiveid,String jobid,String filename,String treehash,long partialsize,int thepartsize) throws Exception {
        File downloadpartfile = generateTempfile();
        File file = new File(filename);
        FileOutputStream fp = new FileOutputStream(file);
        boolean done = false;
        long pos = 0;
        long remainder = partialsize;
        int partsize = 0;
        while (!done) {
            if (remainder > (long)thepartsize) {
                partsize = thepartsize;
            } else {
                partsize = (int)remainder;
            }
            Formatter f = new Formatter();
            f.format("bytes=%d-%d",pos,(pos+partsize)-1);
            String range = f.toString();
            GetJobOutputResult result = AmazonGlacierJobOperations.getJobOutput(client,vault,jobid,range);
            java.io.InputStream bodyStream = result.getBody();
            byte buffer[] = new byte[thepartsize];
            int bytesRead;
            
            String Checksum = result.getChecksum();
            FileOutputStream of = new FileOutputStream(downloadpartfile);
            while ((bytesRead = bodyStream.read(buffer, 0, thepartsize)) > 0) {
                of.write(buffer, 0, bytesRead);
            }
            of.close();
            
            if (Checksum != null) {
                String computedTreeHash = TreeHashGenerator.calculateTreeHash(downloadpartfile);
                if (computedTreeHash.compareTo(Checksum) != 0) {
                    throw new Exception("Archive part ("+range+") SHA256 Tree Hash failure: locally computed: "+computedTreeHash+", returned: "+Checksum);
                }
            }
            FileInputStream dfp = new FileInputStream(downloadpartfile);
            while ((bytesRead = dfp.read(buffer, 0, thepartsize)) > 0) {
                fp.write(buffer, 0, bytesRead);
            }
            dfp.close();
            pos += partsize;
            remainder -= partsize;
            done = pos >= partialsize;
        }
        fp.close();
        if (treehash != null) {
            String computedTreeHash = TreeHashGenerator.calculateTreeHash(file);
            if (computedTreeHash.compareTo(treehash) != 0) {
                throw new Exception("Archive SHA256 Tree Hash failure: locally computed: "+computedTreeHash+", in job descr: "+treehash);
            } else {
                return filename;
            }
        } else {
            return filename;
        }
    }
    public String deletearchive(String vault,String archive) throws Exception {
        Element vnode = findvaultbyname(vault);
        if (vnode == null) {
            throw new Exception("No such vault: "+vault);
        }
        Element anode = findarchivebydescr(vnode,archive);
        if (anode == null) {
            throw new Exception("No such archive in "+vault+": "+archive);
        }
        String archiveid = anode.getAttribute("archiveid");
        AmazonGlacierArchiveOperations.deleteArchive(client,vault,archiveid);
        removearchive(vault,archiveid);
        return vault+" "+archiveid;
    }
    public String deletearchive_byaid(String vault,String aid) throws Exception {
        Element vnode = findvaultbyname(vault);
        if (vnode == null) {
            throw new Exception("No such vault: "+vault);
        }
        Element anode = findarchivebyaid(vnode,aid);
        if (anode == null) {
            throw new Exception("No such archive in "+vault+": "+aid);
        }
        String archiveid = anode.getAttribute("archiveid");
        AmazonGlacierArchiveOperations.deleteArchive(client,vault,archiveid);
        removearchive(vault,archiveid);
        return vault+" "+archiveid;
    }
    public String deletevault(String vault) throws Exception {
        Element vnode = findvaultbyname(vault);
        if (vnode == null) {
            throw new Exception("No such vault: "+vault);
        }
        AmazonGlacierVaultOperations.deleteVault(client, vault);
        removevault(vault);
        return vault;
    }
    public DescribeVaultResult describevault(String vaultName) throws Exception {
        DescribeVaultRequest describeVaultRequest = new DescribeVaultRequest()
              .withVaultName(vaultName);
        DescribeVaultResult describeVaultResult = client.describeVault(describeVaultRequest);
        return(describeVaultResult);
    }
        
}
