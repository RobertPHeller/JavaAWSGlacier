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
 *  Last Modified : <150518.1654>
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
import com.deepsoft.*;
import java.security.NoSuchAlgorithmException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.glacier.AmazonGlacierClient;
import java.util.Properties;


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
        } else {
            System.err.println("Missing required command");
            Usage();
        }
        
        /**
         * set response [$glacierClient createVault $vaultname]
         * set response [$glacierClient deleteVault $vault]
         * 
         * set r1 [$glacierClient initiateMultipartUpload  $vaultname $Meg256 $description]
         * set partresponse [$glacierClient uploadMultipartPart $vaultname $uploadid $uploadpartfile $range]
         * set response [$glacierClient completeMultipartUpload $vaultname $uploadid $size $sha256_tree_hash]
         * set response [$glacierClient uploadArchive $vaultname $archivefile $description]
         * set response [eval [list $glacierClient listParts $vault $uploadid] $args]
         * set response [eval [list $glacierClient listMultipartUploads $vault] $args]
         * set response [$glacierClient abortMultipartUpload $vault $uploadid]
         * set response [$glacierClient deleteArchive $vault [$anode attribute archiveid]]
         * 
         * set response [$glacierClient initiateJob $vault archive-retrieval -archiveid [$anode attribute archiveid] -snstopic $snstopic]
         * set response [eval [list $glacierClient listJobs $vault] $args]
         * set response [$glacierClient describeJob $vault $jobid]
         * set response [$glacierClient getJobOutput $vault $jobid -output $filename]
         */
        
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
        System.err.println(" UploadArchive vaultname archivefile description");
        System.err.println(" ListParts vaultname uploadid opts...");
        System.err.println(" ListMultipartUploads vaultname opts...");
        System.err.println(" AbortMultipartUpload vaultname uploadid");
        System.err.println(" DeleteArchive vaultname archiveid");
        System.err.println("");
        System.err.println(" InitiateJob vaultname archive-retrieval archiveid snstopic");
        System.err.println(" ListJobs vaultname opts...");
        System.err.println(" DescribeJob vaultname jobid");
        System.err.println(" GetJobOutput vaultname jobid [-output filename]");
        System.exit(-1);
    }
}

