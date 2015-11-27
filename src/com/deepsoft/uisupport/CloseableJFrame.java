/**
 * Copyright by ActiveTree Inc., California, USA.
 * All rights reserved.
 */
package com.deepsoft.uisupport;

import com.activetree.common.utils.AtDebug;
import com.activetree.common.utils.FileChooserUtil;
import com.activetree.common.data.KeyValue;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.*;
import java.util.Properties;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.io.*;

public class CloseableJFrame extends JFrame {
  public CloseableJFrame() {
    this("");
  }

  public CloseableJFrame(String title) {
    super(title);
    //super.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    super.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
        saveUserPreferences();
        setVisible(false);
        dispose();
      }
    });
  }

  ////////////////////////////////////////////////////////////////////////////////////////
  //Custom method to let file dialog open to the last opened directory location.
  ////////////////////////////////////////////////////////////////////////////////////////
  //To let dialog open to the last opened directory location.
  static String dirToOpen = ".";
  static String lastOpenedDirectory;
  static final String LAST_OPENED_DIRECTORY = "LastOpenedDirectory";
  static String DEFAULT_USER_PREF = ".report_preview.pref";
  static String entryPrefix = "Rpt";
  static Properties oldPrefs;
  static {
    loadUserPreferences();
  }
  /////End open last opened dir.

  public static String getLastOpenedDirectory() {
    return (lastOpenedDirectory != null ? lastOpenedDirectory : dirToOpen);
  }

  public static void setLastOpendedDirectory(String lastOpenedDir) {
    lastOpenedDirectory = lastOpenedDir;
  }

  private static void loadUserPreferences() {
    AtDebug.debug("loadUserPreferences()");
    String userPrefPropertiesName = DEFAULT_USER_PREF;

    InputStream is = null;
    try {
      String userHome = System.getProperty("user.home");
      if (userHome != null) {
        String urlPropFile = userHome + System.getProperty("file.separator") + userPrefPropertiesName;
        is = new FileInputStream(urlPropFile);
      }
    } catch (Throwable t) {
      //ok, may be the file is not created yet.
    }

    if (is != null) {
      try {
        Properties p = new Properties();
        p.load(is);
        Enumeration keys = p.keys();
        while (keys != null && keys.hasMoreElements()) {
          String key = (String) keys.nextElement();
          key = key.trim();
          String value = p.getProperty(key);
          value = value.trim();
          if (key == null) {
            continue;
          }
          if (key.equalsIgnoreCase(LAST_OPENED_DIRECTORY)) {
            dirToOpen = value;
          }
        }
        oldPrefs = p;
      }catch(Throwable t) {
        //ok
      }
    }
  }

  private void saveUserPreferences() {
    String userPrefPropertiesName = DEFAULT_USER_PREF;
    //keep the order as in the model
    LinkedHashMap properties = new LinkedHashMap();

    if (lastOpenedDirectory != null) {
      properties.put(LAST_OPENED_DIRECTORY, lastOpenedDirectory);
    }
    //Done getting the properties.

    //Save the properties.
    OutputStream out = null;
    try {
      //Do not overwrite until there is at least something.
      if (properties.size() > 0) {
        String userHome = System.getProperty("user.home");
        if (userHome != null) {
          String urlPropFile = userHome + System.getProperty("file.separator") + userPrefPropertiesName;
          out = new FileOutputStream(urlPropFile);
        }
      }
    } catch (Throwable t) {
      AtDebug.debug(AtDebug.LEVEL_ERROR, t);
    }

    if (out != null) {
      try {
        Properties p = new Properties();
        if (oldPrefs != null && oldPrefs.size() > 0) {
          p.putAll(oldPrefs);
        }
        p.putAll(properties);
        p.store(out, entryPrefix + " - User preferences");
      } catch (Throwable t) {
        AtDebug.debug(AtDebug.LEVEL_ERROR, t);
      }
    }
  }

  public static OutputStream getOutputStream(Component owner, String extension, String description, int fileAction) {
    String lastOpenedDir = getLastOpenedDirectory();
    KeyValue kv = FileChooserUtil.getOutputStream(owner, extension, description, fileAction, lastOpenedDir);
    OutputStream outputStream = null;
    if (kv != null) {
      outputStream = (OutputStream) kv.value;
      //last openend dir
      File f = (File) kv.key;
      String path = f.getPath();
      if (f.isDirectory()) {
        lastOpenedDirectory = path;
      }else {
        int idx = path.lastIndexOf(File.separatorChar);
        if (idx >= 0) {
          lastOpenedDirectory = path.substring(0, idx);
        }
      }
    }
    return outputStream;
  }
}
