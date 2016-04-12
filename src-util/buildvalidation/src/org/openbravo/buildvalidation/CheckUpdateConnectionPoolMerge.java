/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.1  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License.
 * The Original Code is Openbravo ERP.
 * The Initial Developer of the Original Code is Openbravo SLU
 * All portions are Copyright (C) 2015 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.buildvalidation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.openbravo.buildvalidation.BuildValidation;
import org.openbravo.database.ConnectionProvider;

/**
 * This build validation prevents a bad behaviour updating to PR15Q4 by taking into account the
 * following scenarios: 
 *      - Update from < 3.0PR15Q4 using defaults connection pools.
 *      - Update from < 3.0PR15Q4 using Apache JDBC Connection Pool module or another external
 *      connection pool.
 * 
 * It must be ensured that if an enviroment did not use "Apache JDBC Connection Pool" module, it
 * must continue without using the connection pool. On the other hand, environments that used the
 * module continue to use the Apache JDBC Connection pool. It will try to retrieve the configuration
 * of the module. The new instances will start using the new connection pool.
 *
 * @author inigo.sanchez
 *
 */
public class CheckUpdateConnectionPoolMerge extends BuildValidation {

  private final static String PROPERTY_CONNECTION_POOL = "db.externalPoolClassName";
  private final static String PATH_CONNECTIONPOOL_PROPERTIES = "/WebContent/WEB-INF/connectionPool.properties";
  private final static String PATH_OPENBRAVO_PROPERTIES = "/config/Openbravo.properties";
  private final static String TARGET_VERSION = "1.0.27056";
  private final static String PREFIX_POOL_PROPERTIES = "db.pool.";
  private final static String SUFFIX_AUX = "_aux";

  private static Logger log = Logger.getLogger(CheckUpdateConnectionPoolMerge.class);
  private Properties obProperties = null;

  @Override
  public List<String> execute() {
    ConnectionProvider cp = getConnectionProvider();
    try {
      String versionOfModule = CheckUpdateConnectionPoolMergeData.versionOfConnectionPoolModule(cp);

      if (versionOfModule == null || isNecessaryMerge(versionOfModule, TARGET_VERSION)) {
        String obDir = getSourcePathFromOBProperties();
        String openbravoPropertiesPath = obDir + PATH_OPENBRAVO_PROPERTIES;

        // It must be ensured that if an enviroment did not use "Apache JDBC Connection Pool"
        // module, it must continue without using the connection pool. In that case remove value of
        // the property.
        if (versionOfModule == null) {
          File fileW = new File(openbravoPropertiesPath);
          // removes value of property that merge in Openbravo.properties
          replaceProperty(fileW, openbravoPropertiesPath + SUFFIX_AUX, PROPERTY_CONNECTION_POOL, "=");
          try {
            fileW.delete();
            File fileAux = new File(openbravoPropertiesPath + SUFFIX_AUX);
            fileAux.renameTo(new File(openbravoPropertiesPath));
          } catch (Exception ex) {
            log.error("Error renaming/deleting Openbravo.properties", ex);
          }
          log.info("Removed value of " + PROPERTY_CONNECTION_POOL + " property.");

        } else {
          // Environments that previously used the connection pool module. It will try to retrieve
          // the configuration of the module. It is necessary to merge connectionPool.properties
          String connectionPoolPropertiesPath = obDir + PATH_CONNECTIONPOOL_PROPERTIES;
          mergeOpenbravoPropertiesConnectionPool(openbravoPropertiesPath,
              connectionPoolPropertiesPath);
        }
      }
    } catch (Exception e) {
      handleError(e);
    }
    return new ArrayList<String>();
  }

  /**
   * Checks version of the module.
   *
   * @return true if it is necessary to merge.
   */
  private boolean isNecessaryMerge(String version, String targetVersion) {
    String[] targetNumberVersion = targetVersion.split(".");
    String[] numberVersion = version.split(".");

    // if version is equal or lower than TARGET_VERSION, it must be merged
    for (int i = 0; i < numberVersion.length; i++) {
      if (Integer.parseInt(numberVersion[i]) > Integer.parseInt(targetNumberVersion[i])) {
        return false;
      } else if (Integer.parseInt(numberVersion[i]) < Integer.parseInt(targetNumberVersion[i])) {
        return true;
      }
    }
    // here it is same version than target version or at least smaller
    return true;
  }

  /**
   * When updating core and it is include Apache JDBC Connection Pool into distribution in some
   * cases is necessary to update Openbravo.properties taking into account
   * connectionPool.properties.
   *
   * This connectionPool.properties file exists in instances with Apache JDBC Connection Pool
   * module.
   *
   * @return false in case no changes were needed, true in case the merge includes some changes
   */
  private static boolean mergeOpenbravoPropertiesConnectionPool(String OpenbravoPropertiesPath,
      String connectionPoolPath) {
    Properties openbravoProperties = new Properties();
    Properties connectionPoolProperties = new Properties();
    try {
      // load both files
      openbravoProperties.load(new FileInputStream(OpenbravoPropertiesPath));
      connectionPoolProperties.load(new FileInputStream(connectionPoolPath));

      Enumeration<?> propertiesConnectionPool = connectionPoolProperties.propertyNames();
      while (propertiesConnectionPool.hasMoreElements()) {
        String propName = (String) propertiesConnectionPool.nextElement();
        String origValue = openbravoProperties.getProperty(PREFIX_POOL_PROPERTIES + propName);
        String connectionPoolValue = connectionPoolProperties.getProperty(propName);

        // try to get original value for new property, if it does not exist add it to original
        // properties with its default value
        if (origValue == null) {
          addNewProperty(OpenbravoPropertiesPath, PREFIX_POOL_PROPERTIES + propName,
              connectionPoolValue);
          openbravoProperties.setProperty(PREFIX_POOL_PROPERTIES + propName, connectionPoolValue);
        } else {
          // replace value in Openbravo.properties by value in connectionPool.properties
          try {
            File fileW = new File(OpenbravoPropertiesPath);

            if (!(origValue.equals(connectionPoolValue))) {
              replaceProperty(fileW, OpenbravoPropertiesPath + SUFFIX_AUX, PREFIX_POOL_PROPERTIES
                  + propName, "=" + connectionPoolValue);
              try {
                fileW.delete();
                File fileAux = new File(OpenbravoPropertiesPath + SUFFIX_AUX);
                fileAux.renameTo(new File(OpenbravoPropertiesPath));
              } catch (Exception ex) {
                log.error("Error renaming/deleting Openbravo.properties", ex);
              }
            }
          } catch (Exception e) {
            log.error("Error read/write Openbravo.properties", e);
          }
        }
      }
    } catch (Exception notFoundConnectionPoolProperties) {
      return false;
    }
    log.info("Merged connection pool properties with Openbravo.properties file.");
    return true;
  }

  /**
   * Adds a new property in a merge of properties file.
   * 
   * Extract from original method in org.openbravo.erpCommon.utility.Utility.java. It is necessary
   * because build validations can not work with external methods.
   *
   * @param pathFile
   *          properties file path
   * @param propertyName
   *          new property to add
   * @param value
   *          new value to add
   */
  private static void addNewProperty(String pathFile, String propertyName, String value) {
    File fileW = new File(pathFile);
    try {
      BufferedWriter bw = new BufferedWriter(new FileWriter(fileW, true));
      bw.write("\n" + propertyName + "=" + value + "\n");
      bw.close();
    } catch (Exception e1) {
      log.error("Exception reading/writing file: ", e1);
    }
  }

  /**
   * Replaces a value changeOption in addressFilePath. FileR is used to check that exists
   * searchOption with different value.
   * 
   * Extract from original method in org.openbravo.configuration.ConfigurationApp.java. It is
   * necessary because build validations can not work with external methods.
   *
   * @param fileR
   *          old file to read
   * @param addressFilePath
   *          file to write new property
   * @param searchOption
   *          Prefix to search
   * @param changeOption
   *          Value to write in addressFilePath
   */
  private static void replaceProperty(File fileR, String addressFilePath, String searchOption,
      String changeOption) throws Exception {
    boolean isFound = false;
    FileReader fr = new FileReader(fileR);
    BufferedReader br = new BufferedReader(fr);
    // auxiliary file to rewrite
    File fileW = new File(addressFilePath);
    FileWriter fw = new FileWriter(fileW);
    // data for restore
    String line;
    while ((line = br.readLine()) != null) {
      if (line.indexOf(searchOption) == 0) {
        // Replace new option
        line = line.replace(line, searchOption + changeOption);
        isFound = true;
      }
      fw.write(line + "\n");
    }
    if (!isFound) {
      fw.write(searchOption + changeOption);
    }
    fr.close();
    fw.close();
    br.close();
  }

  /**
   * Searches an option in filePath file and returns the value of searchOption.
   * 
   * Extract from original method in org.openbravo.configuration.ConfigurationApp.java. It is
   * necessary because build validations can not work with external methods.
   *
   * @param filePath
   *          Path of file
   * @param searchOption
   *          Prefix of property to search
   * @return valueFound Value found
   */
  private static String searchProperty(File filePath, String searchOption) {
    String valueFound = "";
    try {
      FileReader fr = new FileReader(filePath);
      BufferedReader br = new BufferedReader(fr);
      String line;
      while ((line = br.readLine()) != null) {
        if (line.indexOf(searchOption) == 0) {
          valueFound = line.substring(searchOption.length() + 1);
          break;
        }
      }
      fr.close();
      br.close();
    } catch (Exception e) {
      log.error("Exception searching a property: ", e);
    }
    return valueFound;
  }

  /**
   * Gets source.path property from Openbravo.properties file
   * 
   */
  private String getSourcePathFromOBProperties() {
    // get the location of the current class file
    final URL url = this.getClass().getResource(getClass().getSimpleName() + ".class");
    File f = new File(url.getPath());
    File propertiesFile = null;
    while (f.getParentFile() != null && f.getParentFile().exists()) {
      f = f.getParentFile();
      final File configDirectory = new File(f, "config");
      if (configDirectory.exists()) {
        propertiesFile = new File(configDirectory, "Openbravo.properties");
        if (propertiesFile.exists()) {
          // found it and break
          break;
        }
      }
    }
    return searchProperty(propertiesFile, "source.path");
  }
}
