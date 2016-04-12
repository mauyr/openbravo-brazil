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
 * All portions are Copyright (C) 2011 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.client.kernel;

import java.text.SimpleDateFormat;
import java.util.Date;

import javax.enterprise.context.ApplicationScoped;

import org.apache.log4j.Logger;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.type.StringType;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.DateTimeData;
import org.openbravo.service.db.DalConnectionProvider;

/**
 * An example {@link ApplicationInitializer}.
 * 
 * @author mtaal
 */
@ApplicationScoped
public class KernelApplicationInitializer implements ApplicationInitializer {
  private static Logger log4j = Logger.getLogger(KernelApplicationInitializer.class);
  private static final String sqlDateTimeFormat = "DD-MM-YYYY HH24:MI:SS";
  private static final String javaDateTimeFormat = "dd-MM-yyyy HH:mm:ss";
  private static final long THRESHOLD = 5000; // 5 seconds

  public void initialize() {
    registerSQLFunctions();
    checkDatabaseAndTomcatDateTime();
  }

  private void registerSQLFunctions() {
    OBDal.getInstance().registerSQLFunction("ad_org_getcalendarowner",
        new StandardSQLFunction("ad_org_getcalendarowner", new StringType()));
    OBDal.getInstance().registerSQLFunction("ad_org_getperiodcontrolallow",
        new StandardSQLFunction("ad_org_getperiodcontrolallow", new StringType()));
    OBDal.getInstance().registerSQLFunction("m_isparent_ch_value",
        new StandardSQLFunction("m_isparent_ch_value", new StringType()));
  }

  private void checkDatabaseAndTomcatDateTime() {
    // This method checks if both Tomcat and DB are configured to use the same time. If there
    // is a difference bigger than a few seconds, it logs a warning.
    try {
      Date tomcatDate = new Date(); // Tomcat time
      Date dbDate = getDatabaseDateTime(); // Database time
      log4j.debug("Tomcat Time: " + tomcatDate + ", Database Time: " + dbDate);
      if (dbDate != null) {
        long difference = Math.abs(tomcatDate.getTime() - dbDate.getTime());
        if (difference > THRESHOLD) {
          log4j.warn("Tomcat and Database do not have the same time. Tomcat Time: " + tomcatDate
              + ", Database Time: " + dbDate);
        }
      } else {
        log4j
            .error("Received null as Database time. Not possible to check time differences with Tomcat.");
      }
    } catch (Exception ex) {
      log4j.error("Could not check if Tomcat and Database have the same time.", ex);
    }
  }

  private Date getDatabaseDateTime() {
    Date date = null;
    try {
      // We retrieve the time from the database, using the predefined sql date-time format
      String now = DateTimeData.now(new DalConnectionProvider(), sqlDateTimeFormat);
      SimpleDateFormat formatter = new SimpleDateFormat(javaDateTimeFormat);
      date = formatter.parse(now);
    } catch (Exception ex) {
      log4j.error("Could not get the Database time.", ex);
    }
    return date;
  }
}
