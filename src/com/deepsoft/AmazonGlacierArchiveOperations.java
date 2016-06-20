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
 *  Last Modified : <160620.1101>
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
import java.util.Iterator;
import java.util.Scanner;
import java.util.Date;
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
import com.amazonaws.services.glacier.model.PartListElement;
import com.amazonaws.services.glacier.model.ListMultipartUploadsRequest;
import com.amazonaws.services.glacier.model.ListMultipartUploadsResult;
import com.amazonaws.services.glacier.model.DeleteArchiveRequest;
import com.amazonaws.util.BinaryUtils;

public class AmazonGlacierArchiveOperations {
    private static final int DefaultPartsize = 256 * 1024 * 1024;
    public static class UploadResult {
        public String location;
        public String sha256treehash;
        public UploadResult(String l, String checksum) {
            this.location = l;
            this.sha256treehash = checksum;
        }
    }
    public static UploadResult uploadArchive(AmazonGlacierClient client, String vaultName, File archiveFile) throws Exception {
        return uploadArchive(client,vaultName,archiveFile,DefaultPartsize);
    }
    public static UploadResult uploadArchive(AmazonGlacierClient client, String vaultName, File archiveFile, int partsize) throws Exception {
        //System.err.println("*** AmazonGlacierArchiveOperations.uploadArchive(client,"+vaultName+","+archiveFile+")");
        String description = archiveFile.getName();
        //System.err.println("*** AmazonGlacierArchiveOperations.uploadArchive(): description = "+description);
        long size = archiveFile.length();
        //System.err.println("*** AmazonGlacierArchiveOperations.uploadArchive(): size = "+size);
        UploadResult result;
        if (size > (long)partsize) {
            result = UploadMultiPartArchive(client,vaultName,archiveFile,size,description,partsize);
        } else {
            result = UploadArchiveInOnePart(client,vaultName,archiveFile,size,description);
        }
        return result;
    }
    public static class uploadedRange {
        public long s,e;
        public uploadedRange(String range) {
            Scanner scan = new Scanner(range).useDelimiter("-");
            s = scan.nextLong();
            e = scan.nextLong();
        }
        public uploadedRange(uploadedRange other) {
            s = other.s;
            e = other.e;
        }
        public uploadedRange(long start, long end) {
            s = start;
            e = end;
        }
    }
    private static uploadedRange findUploadedRange(long start, long end, List<uploadedRange> ranges) {
        try {
            Iterator itr = ranges.iterator();
            while (itr.hasNext()) {
                uploadedRange range = (uploadedRange)itr.next();
                if (range.s == start && range.e == end) {
                    return range;
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
    public static UploadResult continueMultipartUploadArchive(AmazonGlacierClient client, String vaultName, String uploadId, File archiveFile) throws Exception {
        long partsize = 0;
        long currentPosition = 0;
        List<byte[]> binaryChecksums = new LinkedList<byte[]>();
        List<uploadedRange> uploadedRanges = new LinkedList<uploadedRange>();
        try {
            ListPartsResult parts = listParts(client,vaultName,uploadId,null,null);
            partsize = parts.getPartSizeInBytes();
            java.util.List<PartListElement> partlist = parts.getParts();
            if (partlist != null) {
                Iterator itr = partlist.iterator();
                while (itr.hasNext()) {
                    PartListElement part = (PartListElement)itr.next();
                    String range = part.getRangeInBytes();
                    uploadedRange urange = new uploadedRange(range);
                    uploadedRanges.add(urange);
                    String treeHash = part.getSHA256TreeHash();
                    byte[] binaryChecksum = BinaryUtils.fromHex(treeHash);
                    binaryChecksums.add(binaryChecksum);
                }
            }
        } catch (Exception e) {
            throw e;
        }
        FileInputStream fileToUpload = new FileInputStream(archiveFile);
        String contentRange;
        int read = 0;
        byte[] buffer = new byte[(int)partsize];
        while (currentPosition < archiveFile.length()) {
            read = fileToUpload.read(buffer, 0, buffer.length);
            if (read == -1) { break; }
            uploadedRange r = findUploadedRange(currentPosition,currentPosition+read,uploadedRanges);
            if (r == null) {
                byte[] bytesRead = Arrays.copyOf(buffer, read);
                contentRange = String.format("bytes %s-%s/*", currentPosition,
                          currentPosition + read - 1);
                String checksum = TreeHashGenerator.calculateTreeHash(new ByteArrayInputStream(bytesRead));
            
                byte[] binaryChecksum = BinaryUtils.fromHex(checksum);
                binaryChecksums.add(binaryChecksum);
                uploadedRanges.add(new uploadedRange(currentPosition,currentPosition + read - 1));
            
                //Upload part.
                UploadMultipartPartRequest partRequest = new UploadMultipartPartRequest()
                      .withVaultName(vaultName)
                      .withBody(new ByteArrayInputStream(bytesRead))
                      .withChecksum(checksum)
                      .withRange(contentRange)
                      .withUploadId(uploadId);
            
                //System.err.println("*** AmazonGlacierArchiveOperations.uploadParts(): partRequest = "+partRequest);
                UploadMultipartPartResult partResult = client.uploadMultipartPart(partRequest);
                
            }
            currentPosition = currentPosition + read;
        }
        String checksum = TreeHashGenerator.calculateTreeHash(binaryChecksums);
        String location = CompleteMultiPartUpload(client,vaultName,archiveFile,uploadId, checksum);
        return new UploadResult(location,checksum);
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
        //System.err.println("*** AmazonGlacierArchiveOperations.UploadArchiveInOnePart(): request = "+request);
        UploadArchiveResult uploadArchiveResult = client.uploadArchive(request);
        return new UploadResult(uploadArchiveResult.getLocation(), uploadArchiveResult.getChecksum());
    }
    private static UploadResult UploadMultiPartArchive(AmazonGlacierClient client, String vaultName, File archiveFile, long size, String description,int partsize) throws Exception {
        String checksum = "";
        String uploadId = initiateMultipartUpload(client,vaultName,description,partsize);
        try {
            checksum = uploadParts(client,vaultName,archiveFile,uploadId,partsize);
        } catch (Exception e) {
            abortMultipartUpload(client,vaultName,uploadId);
            throw e;
        }
        String location = CompleteMultiPartUpload(client,vaultName,archiveFile,uploadId, checksum);
        return new UploadResult(location,checksum);
    }
    private static String initiateMultipartUpload(AmazonGlacierClient client,String vaultName,String description,int partsize) throws Exception {
        InitiateMultipartUploadRequest request = new InitiateMultipartUploadRequest()
              
              .withVaultName(vaultName)
              .withArchiveDescription(description)
              .withPartSize(String.valueOf(partsize));
        
        //System.err.println("*** AmazonGlacierArchiveOperations.initiateMultipartUpload(): request = "+request);
        InitiateMultipartUploadResult result = client.initiateMultipartUpload(request);
        
        return result.getUploadId();
    }
    private static String uploadParts(AmazonGlacierClient client,String vaultName,File archiveFile,String uploadId,int partsize) throws Exception {
        int filePosition = 0;
        long currentPosition = 0;
        byte[] buffer = new byte[(int)partsize];
        List<byte[]> binaryChecksums = new LinkedList<byte[]>();
        
        FileInputStream fileToUpload = new FileInputStream(archiveFile);
        String contentRange;
        int read = 0;
        while (currentPosition < archiveFile.length())
        {
            // String tstring = new Date().toString();
            System.err.printf("*** AmazonGlacierArchiveOperations.uploadParts(): Top of file readloop: currentPosition = %d, time = %tT\n",currentPosition,new Date());
            read = fileToUpload.read(buffer, 0, buffer.length);
            if (read == -1) { break; }
            byte[] bytesRead = Arrays.copyOf(buffer, read);
            
            contentRange = String.format("bytes %s-%s/*", currentPosition,
                      currentPosition + read - 1);
            
            System.err.printf("*** AmazonGlacierArchiveOperations.uploadParts(): about to compute TreeHash: contentRange = %s, time = %tT\n",contentRange,new Date());
            String checksum = TreeHashGenerator.calculateTreeHash(new ByteArrayInputStream(bytesRead));
            
            byte[] binaryChecksum = BinaryUtils.fromHex(checksum);
            binaryChecksums.add(binaryChecksum);
            
            System.err.printf("*** AmazonGlacierArchiveOperations.uploadParts(): checksum = %s, time = %tT\n",checksum,new Date());
            //Upload part.
            UploadMultipartPartRequest partRequest = new UploadMultipartPartRequest()
                  .withVaultName(vaultName)
                  .withBody(new ByteArrayInputStream(bytesRead))
                  .withChecksum(checksum)
                  .withRange(contentRange)
                  .withUploadId(uploadId);
            
            //System.err.println("*** AmazonGlacierArchiveOperations.uploadParts(): partRequest = "+partRequest);
            UploadMultipartPartResult partResult = client.uploadMultipartPart(partRequest);
            System.err.printf("*** AmazonGlacierArchiveOperations.uploadParts(): upload competer at time = %tT\n",new Date());
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
    
        //System.err.println("*** AmazonGlacierArchiveOperations.CompleteMultiPartUpload(): compRequest = "+compRequest);
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

            
            
                  
