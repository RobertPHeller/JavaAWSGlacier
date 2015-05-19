/****************************************************************************
 *
 *  System        : 
 *  Module        : 
 *  Object Name   : $RCSfile$
 *  Revision      : $Revision$
 *  Date          : $Date$
 *  Author        : $Author$
 *  Created By    : Robert Heller
 *  Created       : Tue May 19 09:13:42 2015
 *  Last Modified : <150519.1301>
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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.glacier.AmazonGlacierClient;
import com.amazonaws.services.glacier.TreeHashGenerator;
import com.amazonaws.services.glacier.model.UploadArchiveRequest;
import com.amazonaws.services.glacier.model.UploadArchiveResult;
import com.amazonaws.services.glacier.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.glacier.model.CompleteMultipartUploadResult;
import com.amazonaws.services.glacier.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.glacier.model.InitiateMultipartUploadResult;
import com.amazonaws.services.glacier.model.UploadMultipartPartRequest;
import com.amazonaws.services.glacier.model.UploadMultipartPartResult;
import com.amazonaws.services.glacier.model.AbortMultipartUploadRequest;
import com.amazonaws.services.glacier.model.ListPartsRequest;
import com.amazonaws.services.glacier.model.ListPartsResult;
import com.amazonaws.services.glacier.model.ListMultipartUploadsRequest;
import com.amazonaws.services.glacier.model.ListMultipartUploadsResult;
import com.amazonaws.services.glacier.model.DeleteArchiveRequest;
import com.amazonaws.util.BinaryUtils;

public class AmazonGlacierArchiveOperations {
    private static final long MEG256 = 256 * 1024 * 1024;
    private static class UploadResult {
        public String location;
        public String sha256treehash;
        public UploadResult(String l, String checksum) {
            this.location = l;
            this.sha256treehash = checksum;
        }
    }
    public static void uploadArchive(AmazonGlacierClient client, String vaultName, File archiveFile) throws Exception {
        String description = archiveFile.getName();
        long size = archiveFile.length();
        UploadResult result;
        if (size > MEG256) {
            result = UploadMultiPartArchive(client,vaultName,archiveFile,size,description);
        } else {
            result = UploadArchiveInOnePart(client,vaultName,archiveFile,size,description);
        }
        System.out.println(result.location+" "+result.sha256treehash+" "+description);
    }
    private static UploadResult UploadArchiveInOnePart(AmazonGlacierClient client, String vaultName, File archiveFile, long size, String description) throws Exception {
        InputStream is = new FileInputStream(archiveFile);
        byte[] body = new byte[(int) archiveFile.length()];
        is.read(body);

        UploadArchiveRequest request = new UploadArchiveRequest()
              .withVaultName(vaultName)
              .withChecksum(TreeHashGenerator.calculateTreeHash(archiveFile))
              .withBody(new ByteArrayInputStream(body))
              .withContentLength((long)body.length);
        
        UploadArchiveResult uploadArchiveResult = client.uploadArchive(request);
        return new UploadResult(uploadArchiveResult.getLocation(), uploadArchiveResult.getChecksum());
    }
    private static UploadResult UploadMultiPartArchive(AmazonGlacierClient client, String vaultName, File archiveFile, long size, String description) throws Exception {
        String checksum = "";
        String uploadId = initiateMultipartUpload(client,vaultName,description);
        try {
            checksum = uploadParts(client,vaultName,archiveFile,uploadId);
        } catch (Exception e) {
            abortMultipartUpload(client,vaultName,uploadId);
            throw e;
        }
        String location = CompleteMultiPartUpload(client,vaultName,archiveFile,uploadId, checksum);
        return new UploadResult(location,checksum);
    }
    private static String initiateMultipartUpload(AmazonGlacierClient client,String vaultName,String description) throws Exception {
        InitiateMultipartUploadRequest request = new InitiateMultipartUploadRequest()
              
              .withVaultName(vaultName)
              .withArchiveDescription(description)
              .withPartSize(String.valueOf(MEG256));
        
        InitiateMultipartUploadResult result = client.initiateMultipartUpload(request);
        
        return result.getUploadId();
    }
    private static String uploadParts(AmazonGlacierClient client,String vaultName,File archiveFile,String uploadId) throws Exception {
        int filePosition = 0;
        long currentPosition = 0;
        byte[] buffer = new byte[(int)MEG256];
        List<byte[]> binaryChecksums = new LinkedList<byte[]>();
        
        FileInputStream fileToUpload = new FileInputStream(archiveFile);
        String contentRange;
        int read = 0;
        while (currentPosition < archiveFile.length())
        {
            
            read = fileToUpload.read(buffer, filePosition, buffer.length);
            if (read == -1) { break; }
            byte[] bytesRead = Arrays.copyOf(buffer, read);
            
            contentRange = String.format("bytes %s-%s/*", currentPosition,
                      currentPosition + read - 1);
            
            String checksum = TreeHashGenerator.calculateTreeHash(new ByteArrayInputStream(bytesRead));
            
            byte[] binaryChecksum = BinaryUtils.fromHex(checksum);
            binaryChecksums.add(binaryChecksum);
            
            //Upload part.
            UploadMultipartPartRequest partRequest = new UploadMultipartPartRequest()
                  .withVaultName(vaultName)
                  .withBody(new ByteArrayInputStream(bytesRead))
                  .withChecksum(checksum)
                  .withRange(contentRange)
                  .withUploadId(uploadId);
            
            UploadMultipartPartResult partResult = client.uploadMultipartPart(partRequest);
            
            currentPosition = currentPosition + read;
        }
        String checksum = TreeHashGenerator.calculateTreeHash(binaryChecksums);
        
        return checksum;
    }
    private static String  CompleteMultiPartUpload(AmazonGlacierClient client,String vaultName,File archiveFile,String uploadId, String checksum) throws Exception {
    
        CompleteMultipartUploadRequest compRequest = new CompleteMultipartUploadRequest()
          
              .withVaultName(vaultName)
              .withUploadId(uploadId)
              .withChecksum(checksum)
              .withArchiveSize(String.valueOf(archiveFile.length()));
    
        CompleteMultipartUploadResult compResult = client.completeMultipartUpload(compRequest);
    
        return compResult.getLocation();
    }
    public static void abortMultipartUpload(AmazonGlacierClient client,String vaultName,String uploadId) throws Exception {
        AbortMultipartUploadRequest abortRequest = new AbortMultipartUploadRequest()
              .withVaultName(vaultName)
              .withUploadId(uploadId);
        client.abortMultipartUpload(abortRequest);
    }
    public static void deleteArchive(AmazonGlacierClient client,String vaultName, String archiveId) throws Exception {
        DeleteArchiveRequest deleteRequest = new DeleteArchiveRequest()
              .withVaultName(vaultName)
              .withArchiveId(archiveId);
        client.deleteArchive(deleteRequest);
    }
    public static ListPartsResult listParts(AmazonGlacierClient client,String vaultName,String uploadId,String marker,String limit) throws Exception {
        ListPartsRequest listPartsRequest = new ListPartsRequest()
              .withVaultName(vaultName)
              .withUploadId(uploadId)
              .withMarker(marker)
              .withLimit(limit);
        return client.listParts(listPartsRequest);
    }
    public static ListMultipartUploadsResult listMultipartUploads(AmazonGlacierClient client,String vaultName,String marker,String limit) throws Exception {
        ListMultipartUploadsRequest multipartRequest = new ListMultipartUploadsRequest()
              .withVaultName(vaultName)
              .withUploadIdMarker(marker)
              .withLimit(limit);
        return client.listMultipartUploads(multipartRequest);
    }
}

            
            
                  
