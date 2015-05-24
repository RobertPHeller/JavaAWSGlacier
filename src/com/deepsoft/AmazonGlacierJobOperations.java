/****************************************************************************
 *
 *  System        : 
 *  Module        : 
 *  Object Name   : $RCSfile$
 *  Revision      : $Revision$
 *  Date          : $Date$
 *  Author        : $Author$
 *  Created By    : Robert Heller
 *  Created       : Thu May 21 15:02:55 2015
 *  Last Modified : <150523.1951>
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

import java.io.FileOutputStream;
import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.glacier.AmazonGlacierClient;
import com.amazonaws.services.glacier.model.InitiateJobRequest;
import com.amazonaws.services.glacier.model.InitiateJobResult;
import com.amazonaws.services.glacier.model.JobParameters;
import com.amazonaws.services.glacier.model.InventoryRetrievalJobInput;
import com.amazonaws.services.glacier.model.ListJobsResult;
import com.amazonaws.services.glacier.model.ListJobsRequest;
import com.amazonaws.services.glacier.model.GetJobOutputRequest;
import com.amazonaws.services.glacier.model.GetJobOutputResult;
import com.amazonaws.services.glacier.model.DescribeJobRequest;
import com.amazonaws.services.glacier.model.DescribeJobResult;

public class AmazonGlacierJobOperations {
    private static final int MEG256 = 256 * 1024 * 1024;
    public static InitiateJobResult initiateJob(AmazonGlacierClient client,String vaultName,JobParameters jobParams) throws Exception {
        InitiateJobRequest request = new InitiateJobRequest()
              .withVaultName(vaultName)
              .withJobParameters(jobParams);
        return client.initiateJob(request);
    }
    public static ListJobsResult listJobs(AmazonGlacierClient client,String vaultName,ListJobsRequest request) throws Exception {
        return client.listJobs(request);
    }
    public static DescribeJobResult describeJob(AmazonGlacierClient client,String vaultName, String jobId) throws Exception {
        DescribeJobRequest request = new DescribeJobRequest()
              .withVaultName(vaultName)
              .withJobId(jobId);
        return client.describeJob(request);
    }
    public static GetJobOutputResult getJobOutput(AmazonGlacierClient client,String vaultName,String jobId,String range) throws Exception {
        GetJobOutputRequest request = new GetJobOutputRequest()
              .withVaultName(vaultName)
              .withJobId(jobId);
        if (range != null) {
            request.setRange(range);
        }
        return client.getJobOutput(request);
    }
}


