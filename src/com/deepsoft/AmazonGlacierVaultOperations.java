/****************************************************************************
 *
 *  System        : 
 *  Module        : 
 *  Object Name   : $RCSfile$
 *  Revision      : $Revision$
 *  Date          : $Date$
 *  Author        : $Author$
 *  Created By    : Robert Heller
 *  Created       : Mon May 18 10:48:03 2015
 *  Last Modified : <150518.1623>
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

import java.io.IOException;
import java.util.List;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.glacier.AmazonGlacierClient;
import com.amazonaws.services.glacier.model.CreateVaultRequest;
import com.amazonaws.services.glacier.model.CreateVaultResult;
import com.amazonaws.services.glacier.model.DeleteVaultRequest;
import com.amazonaws.services.glacier.model.DescribeVaultOutput;
import com.amazonaws.services.glacier.model.DescribeVaultRequest;
import com.amazonaws.services.glacier.model.DescribeVaultResult;
import com.amazonaws.services.glacier.model.ListVaultsRequest;
import com.amazonaws.services.glacier.model.ListVaultsResult;

public class AmazonGlacierVaultOperations {
    
    public static AmazonGlacierClient client;
    
    public static void main(String[] args) throws IOException {
        
        AWSCredentials credentials = new PropertiesCredentials(
                  AmazonGlacierVaultOperations.class
                  .getResourceAsStream("AwsCredentials.properties"));
        
        client = new AmazonGlacierClient(credentials);
        client.setEndpoint("https://glacier.us-east-1.amazonaws.com/");
        
        String vaultName = "examplevaultfordelete";
        
        try {
            createVault(client, vaultName);
            describeVault(client, vaultName);
            listVaults(client);
            deleteVault(client, vaultName);
            
        } catch (Exception e) {
            System.err.println("Vault operation failed." + e.getMessage());
            
        }
    }
    public static void createVault(AmazonGlacierClient client, String vaultName)
    {
        
        CreateVaultRequest createVaultRequest = new CreateVaultRequest()
              .withVaultName(vaultName);
        
        CreateVaultResult createVaultResult = client.createVault(createVaultRequest);
        
        System.out.println(vaultName + " " + createVaultResult.getLocation());
    }
    
    public static void describeVault(AmazonGlacierClient client, String
              vaultName) {
        
        DescribeVaultRequest describeVaultRequest = new DescribeVaultRequest()
              
              .withVaultName(vaultName);
        DescribeVaultResult describeVaultResult = client.describeVault(describeVaultRequest);
        
        System.out.println(vaultName+" "+describeVaultResult.getCreationDate()+" "+describeVaultResult.getNumberOfArchives()+" "+describeVaultResult.getVaultARN());
    }
    
    public static void listVaults(AmazonGlacierClient client) {
        ListVaultsRequest listVaultsRequest = new ListVaultsRequest();
        
        ListVaultsResult listVaultsResult = client.listVaults(listVaultsRequest);
        
        List<DescribeVaultOutput> vaultList = listVaultsResult.getVaultList();
        
        String SP="";
        for (DescribeVaultOutput vault : vaultList) {
            System.out.print(SP+"{"+vault.getVaultName()+" "+vault.getCreationDate()+" "+vault.getNumberOfArchives()+" "+vault.getVaultARN()+"}");
            SP=" ";
        }
        System.out.println("");
    }
    
    public static void deleteVault(AmazonGlacierClient client, String vaultName)
    {
        
        DeleteVaultRequest request = new DeleteVaultRequest()
              .withVaultName(vaultName);
        
        client.deleteVault(request);
        System.out.println(vaultName);
    }
}
