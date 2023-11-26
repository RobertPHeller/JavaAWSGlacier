/****************************************************************************
 *
 *  System        : 
 *  Module        : 
 *  Object Name   : $RCSfile$
 *  Revision      : $Revision$
 *  Date          : $Date$
 *  Author        : $Author$
 *  Created By    : Robert Heller
 *  Created       : Sun Nov 26 12:19:44 2023
 *  Last Modified : <231126.1321>
 *
 *  Description	
 *
 *  Notes
 *
 *  History
 *	
 ****************************************************************************
 *
 *    Copyright (C) 2023  Robert Heller D/B/A Deepwoods Software
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
import java.util.regex.*;
import java.lang.*;
import java.text.*;
import org.w3c.dom.*;
import com.deepsoft.*;

public class DeleteEmptyVaults extends BackupVault {
    private static File GlacierVaultDB_File;
    private static SiteConfig configuration;
    public DeleteEmptyVaults() throws Exception {
        super(GlacierVaultDB_File);
    }
    public void deleteemptyvaults() throws Exception {
        Element vaultsnode = getvaultnode();
        NodeList vaults = vaultsnode.getElementsByTagName("vault");
        int i;
        int count = 0;
        for (i=0; i < vaults.getLength(); i++) {
            Element vault = (Element) vaults.item(i);
            NodeList archives = vault.getElementsByTagName("archive");
            int archivecount = archives.getLength();
            if (archivecount == 0) {
                String tape = vault.getAttribute("name");
                try {
                    deletevault(tape);
                    System.out.println("Vault "+tape+" deleted from the Glacier");
                    count++;
                } catch (Exception e) {
                    System.err.println("Exception deleting vault: "+e.getMessage());
                }
            }
        }
        if (count > 0) {
            String countString = Integer.toString(count,10);
            if (count == 1) {
                System.out.println(countString+" vault deleted from the Glacier");
            } else {
                System.out.println(countString+" vaults deleted from the Glacier");
            }
            savedb(GlacierVaultDB_File);
        } else {
            System.out.println("No vaults deleted from the Glacier");
        }
    }
    public static void main(String args[]) throws Exception {
        configuration = new SiteConfig();
        GlacierVaultDB_File = new File(configuration.GlacierVaultDB_FileName());
        DeleteEmptyVaults DEV = new DeleteEmptyVaults();
        DEV.deleteemptyvaults();
    }
}

