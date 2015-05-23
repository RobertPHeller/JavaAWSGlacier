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
 *  Last Modified : <150523.1658>
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
import com.amazonaws.services.glacier.AmazonGlacierClient;
import com.amazonaws.services.glacier.model.ListPartsResult;
import com.amazonaws.services.glacier.model.ListMultipartUploadsResult;
import com.amazonaws.services.glacier.model.JobParameters;
import com.amazonaws.services.glacier.model.InitiateJobResult;
import org.w3c.dom.*;
import com.deepsoft.VaultXMLDB;





class BackupVault extends VaultXMLDB {
    private final SimpleDateFormat dateFormatter = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z");
    private final long Meg256 = 256 * 1024 * 1024;
    private final String glacierTemp = "/home/AmazonGlacierTemp";
    private int tempFileindex = 1;
    public BackupVault(String dbfile) throws Exception {
        this(new File(dbfile));
    }
    public BackupVault(File file) throws Exception {
        super(file,false);
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
        AWSCredentials credentials = null;
        String homedir = System.getProperty("user.home");
        File credfile = new File(homedir+"/.AwsCredentials");
        credentials = new PropertiesCredentials(credfile);
        AmazonGlacierClient client = new AmazonGlacierClient(credentials);
        client.setEndpoint("https://glacier.us-east-1.amazonaws.com/");
        String loc = AmazonGlacierVaultOperations.createVault(client, vaultName);
        String date = dateFormatter.format(new Date());
        return addvault(loc,date.toString());
    }
    public Element UploadArchive (String vaultName, String archivefile) throws Exception {
        File archiveFile = new File(archivefile);
        AWSCredentials credentials = null;
        String homedir = System.getProperty("user.home");
        File credfile = new File(homedir+"/.AwsCredentials");
        credentials = new PropertiesCredentials(credfile);
        
        AmazonGlacierClient client = new AmazonGlacierClient(credentials);
        client.setEndpoint("https://glacier.us-east-1.amazonaws.com/");
        AmazonGlacierArchiveOperations.UploadResult result = 
              AmazonGlacierArchiveOperations.uploadArchive(client,vaultName,
                        archiveFile);
        String date = dateFormatter.format(new Date());
        Long lsize = new Long(archiveFile.length());
        String size = lsize.toString();
        return addarchive(result.location,date.toString(),size,result.sha256treehash,archiveFile.getName());
    }
    public ListPartsResult ListParts(String vaultName, String uploadId) throws Exception {
        return ListParts(vaultName,uploadId,null,null);
    }
    public ListPartsResult ListParts(String vaultName, String uploadId,String marker, String limit) throws Exception {
        AWSCredentials credentials = null;
        String homedir = System.getProperty("user.home");
        File credfile = new File(homedir+"/.AwsCredentials");
        credentials = new PropertiesCredentials(credfile);
        
        AmazonGlacierClient client = new AmazonGlacierClient(credentials);
        client.setEndpoint("https://glacier.us-east-1.amazonaws.com/");
        return AmazonGlacierArchiveOperations.listParts(client,vaultName,uploadId,marker,limit);
    }
    public ListMultipartUploadsResult ListMultiPartUploads(String vaultName) throws Exception {
        return ListMultiPartUploads(vaultName,null,null);
    }
    public ListMultipartUploadsResult ListMultiPartUploads(String vaultName,String marker, String limit) throws Exception {
        AWSCredentials credentials = null;
        String homedir = System.getProperty("user.home");
        File credfile = new File(homedir+"/.AwsCredentials");
        credentials = new PropertiesCredentials(credfile);
        
        AmazonGlacierClient client = new AmazonGlacierClient(credentials);
        client.setEndpoint("https://glacier.us-east-1.amazonaws.com/");
        return AmazonGlacierArchiveOperations.listMultipartUploads(client,vaultName,marker,limit);
    }
    public void AbortMultipartUpload(String vaultName,String uploadId) throws Exception {
        AWSCredentials credentials = null;
        String homedir = System.getProperty("user.home");
        File credfile = new File(homedir+"/.AwsCredentials");
        credentials = new PropertiesCredentials(credfile);
        
        AmazonGlacierClient client = new AmazonGlacierClient(credentials);
        client.setEndpoint("https://glacier.us-east-1.amazonaws.com/");
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
        AWSCredentials credentials = null;
        String homedir = System.getProperty("user.home");
        File credfile = new File(homedir+"/.AwsCredentials");
        credentials = new PropertiesCredentials(credfile);
        
        AmazonGlacierClient client = new AmazonGlacierClient(credentials);
        client.setEndpoint("https://glacier.us-east-1.amazonaws.com/");
        InitiateJobResult jResult = AmazonGlacierJobOperations.initiateJob(client,vaultName,jobParams);
        return jResult.getJobId();
    }
    
}
