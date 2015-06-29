/****************************************************************************
 *
 *  System        : 
 *  Module        : 
 *  Object Name   : $RCSfile$
 *  Revision      : $Revision$
 *  Date          : $Date$
 *  Author        : $Author$
 *  Created By    : Robert Heller
 *  Created       : Sat May 23 11:30:46 2015
 *  Last Modified : <150629.1655>
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

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.*;
import java.io.*;
import java.lang.*;
import java.util.*;
import java.util.regex.*;

class VaultXMLDB {
    private Document db;
    public VaultXMLDB() throws Exception {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        String str = "<vaults/>";
        InputStream is = new ByteArrayInputStream(str.getBytes());
        db =  dBuilder.parse(is);
    }
    public VaultXMLDB(String filename) throws Exception {
        this(new File(filename),true);
    }
    public VaultXMLDB(File file) throws Exception {
        this(file,true);
    }
    public VaultXMLDB(String filename,boolean errIfNotExists) throws Exception {
        this(new File(filename),errIfNotExists);
    }
    public VaultXMLDB(File file,boolean errIfNotExists) throws Exception {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        try {
            db =  dBuilder.parse(file);
        } catch (IOException ioe) {
            if (!errIfNotExists) {
                String str = "<vaults/>";
                InputStream is = new ByteArrayInputStream(str.getBytes());
                db =  dBuilder.parse(is);
            } else {
                throw ioe;
            }
        } catch (Exception e) {
            throw e;
        }
    }
    public void savedb(String filename) throws Exception {
        savedb(new File(filename));
    }
    public void savedb(File file) throws Exception {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource source = new DOMSource(db);
        file.renameTo(new File(file.getPath()+".bak"));
        StreamResult result = new StreamResult(file);
        transformer.transform(source, result);
    }
    public Element getvaultnode() {
        return (Element) db.getElementsByTagName("vaults").item(0);
    }
    public Element addvault (String location, String date) throws Exception {
        return addvault (location,date,"");
    }
    public Element addvault (String location, String date, String id) throws Exception {
        String accountid;
        String vaultname;
        if (location.matches("^/[0-9]+/vaults/.+$")) {
            String m[] = location.split("/");
            accountid = m[1];
            vaultname = m[3];
        } else {
            throw new Exception("Illformed Location: "+location);
        }
        Element vaultsnode = (Element) db.getElementsByTagName("vaults").item(0);
        Element newvault   = db.createElement("vault");
        newvault.setAttribute("location",location);
        newvault.setAttribute("name",vaultname);
        newvault.setAttribute("accountid",accountid);
        newvault.setAttribute("date",date);
        if (id != null && id.compareTo("") != 0) {
            newvault.setAttribute("id",id);
        }
        vaultsnode.appendChild(newvault);
        return newvault;
    }
    public void removevault(String vname) {
        Element vaultsnode = (Element) db.getElementsByTagName("vaults").item(0);
        NodeList vaults = vaultsnode.getElementsByTagName("vault");
        for (int i=0; i < vaults.getLength(); i++) {
            Element vault = (Element) vaults.item(i);
            if (vault.getAttribute("name").compareTo(vname) == 0) {
                vaultsnode.removeChild(vault);
                return;
            }
        }
    }
    public LinkedList<Element> findvaultsbypattern(Pattern vpattern) {
        Element vaultsnode = (Element) db.getElementsByTagName("vaults").item(0);
        NodeList vaults = vaultsnode.getElementsByTagName("vault");
        LinkedList<Element> result = new LinkedList<Element>();
        for (int i=0; i < vaults.getLength(); i++) {
            Element vault = (Element) vaults.item(i);
            Matcher match = vpattern.matcher(vault.getAttribute("name"));
            if (match.matches()) {
                result.add(vault);
            }
        }
        return result;
    }
    public Element findvaultbyname (String vname) {
        Element vaultsnode = (Element) db.getElementsByTagName("vaults").item(0);
        NodeList vaults = vaultsnode.getElementsByTagName("vault");
        for (int i=0; i < vaults.getLength(); i++) {
            Element vault = (Element) vaults.item(i);
            if (vault.getAttribute("name").compareTo(vname) == 0) {
                return vault;
            }
        }
        return null;
    }
    public Element findarchivebydescr(Element vault, String archivedescr) {
        NodeList archives = vault.getElementsByTagName("archive");
        for (int i=0; i < archives.getLength(); i++) {
            Element a = (Element) archives.item(i);
            NodeList adescrnodes = a.getElementsByTagName("description");
            if (adescrnodes.getLength() < 1) {continue;}
            Element adescrnode = (Element) adescrnodes.item(0);
            String adesc = adescrnode.getTextContent();
            if (adesc.compareTo(archivedescr) == 0) {
                return a;
            }
        }
        return null;
    }
    public Element findarchivebyaid(Element vault, String archiveid) {
        NodeList archives = vault.getElementsByTagName("archive");
        for (int i=0; i < archives.getLength(); i++) {
            Element a = (Element) archives.item(i);
            if (a.getAttribute("archiveid").compareTo(archiveid) == 0) {
                return a;
            }
        }
        return null;
    }
    public Element addarchive(String location,String date,String size,String sha256treehash) throws Exception {
        return addarchive(location,date,size,sha256treehash,"","");
    }
    public Element addarchive(String location,String date,String size,String sha256treehash,String description) throws Exception {
        return addarchive(location,date,size,sha256treehash,description,"");
    }
    public Element addarchive(String location,String date,String size,String sha256treehash,String description,String id) throws Exception {
        String accountid;
        String vaultname;
        String archiveid;
        if (location.matches("^/[0-9]+/vaults/[^/]+/archives/.+$")) {
            String m[] = location.split("/");
            accountid = m[1];
            vaultname = m[3];
            archiveid = m[5];
        } else {
            throw new Exception("Illformed Location: "+location);
        }
        Element vaultsnode = (Element) db.getElementsByTagName("vaults").item(0);
        NodeList vaults = vaultsnode.getElementsByTagName("vault");
        Element vaultnode = null;
        for (int i=0;i < vaults.getLength(); i++) {
            Element v = (Element)vaults.item(i);
            if (v.getAttribute("name").compareTo(vaultname) == 0) {
                vaultnode=v;
                break;
            }
        }
        if (vaultnode == null) {
            throw new Exception("No such vault: "+vaultname);
        }
        Element newarchive = db.createElement("archive");
        newarchive.setAttribute("location",location);
        newarchive.setAttribute("archiveid",archiveid);
        newarchive.setAttribute("date",date);
        if (id != null && id.compareTo("") != 0) {
            newarchive.setAttribute("id",id);
        }
        Element snode = db.createElement("size");
        snode.appendChild(db.createTextNode(size));
        newarchive.appendChild(snode);
        Element sha256node = db.createElement("sha256treehash");
        sha256node.appendChild(db.createTextNode(sha256treehash));
        newarchive.appendChild(sha256node);
        if (description != null && description.compareTo("") != 0) {
            Element descrnode = db.createElement("description");
            descrnode.appendChild(db.createTextNode(description));
            newarchive.appendChild(descrnode);
        }
        vaultnode.appendChild(newarchive);
        return newarchive;
    }
    public Element addTextNode(Element node,String tag,String value) {
        Element newnode = db.createElement(tag);
        newnode.appendChild(db.createTextNode(value));
        node.appendChild(newnode);
        return newnode;
    }
    public void removearchive(String vname,String aid) {
        Element vault = findvaultbyname(vname);
        NodeList archives = vault.getElementsByTagName("archive");
        for (int i=0; i < archives.getLength(); i++) {
            Element a = (Element) archives.item(i);
            if (a.getAttribute("archiveid").compareTo(aid) == 0) {
                vault.removeChild(a);
                return;
            }
        }
    }
}

