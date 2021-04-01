/****************************************************************************
 *
 *  System        : 
 *  Module        : 
 *  Object Name   : $RCSfile$
 *  Revision      : $Revision$
 *  Date          : $Date$
 *  Author        : $Author$
 *  Created By    : Robert Heller
 *  Created       : Wed Mar 31 10:43:58 2021
 *  Last Modified : <210331.1253>
 *
 *  Description	
 *
 *  Notes
 *
 *  History
 *	
 ****************************************************************************
 *
 *    Copyright (C) 2021  Robert Heller D/B/A Deepwoods Software
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

public class SiteConfig {
    private Properties siteProperties;
    private static final String GlacierVaultDB_defaultFileName = "/var/log/amanda/wendellfreelibrary/glacier.xml";
    private static final String AMCONFIG_default = "wendellfreelibrary";
    private static final String VaultPattern_default = "^wendellfreelibrary-vault-\\d+$";
    private static final String Slotdir_default = "/backupdisk/wendellfreelibrary_vault/slots";
    private static final String Disklist_default = "boot,root,home";
    private static final String SNSTopic_default = "arn:aws:sns:us-east-1:647212794748:LibraryGlacier";
    private static final String VAULTLabel_default = "wendellfreelibrary-vault-%%%";
    private static final String LabelPattern_default = "00000\\.(wendellfreelibrary-vault-\\d+)$";
    private static final String LabelFileFilter_default = "^00000\\.wendellfreelibrary-vault-.+$";
    public SiteConfig() throws Exception {
        siteProperties = new Properties();
        String homedir = System.getProperty("user.home");
        File configFile = new File(homedir+"/.javaawsglacier");
        if (configFile.exists()) {
            FileInputStream stream = new FileInputStream(configFile);
            try {
                siteProperties.load(stream);
            } finally {
                try {
                    stream.close();
                } catch (IOException e) {
                }
            }
        }
    }
    public String GlacierVaultDB_FileName() {
        return siteProperties.getProperty("GlacierVaultDB",GlacierVaultDB_defaultFileName);
    }
    public String AMCONFIG() {
        return siteProperties.getProperty("AMCONFIG",AMCONFIG_default);
    }
    public String VaultPattern() {
        return siteProperties.getProperty("VaultPattern",VaultPattern_default);
    }
    public String Slotdir() {
        return siteProperties.getProperty("Slotdir",Slotdir_default);
    }
    public String[] Disks() {
        return siteProperties.getProperty("Disklist",Disklist_default).split(",");
    }
    public String SNSTopic() {
        return siteProperties.getProperty("SNSTopic",SNSTopic_default);
    }
    public String VAULTLabel() {
        return siteProperties.getProperty("VAULTLabel",VAULTLabel_default);
    }
    public String LabelPattern() {
        return siteProperties.getProperty("LabelPattern",LabelPattern_default);
    }
    public String LabelFileFilter() {
        return siteProperties.getProperty("LabelFileFilter",LabelFileFilter_default);
    }
}

