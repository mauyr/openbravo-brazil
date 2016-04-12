/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.0  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License.
 * The Original Code is Openbravo ERP.
 * The Initial Developer of the Original Code is Openbravo SLU
 * All portions are Copyright (C) 2012-2014 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 *************************************************************************
 */

package org.openbravo.advpaymentmngt.utility;

import javax.enterprise.context.ApplicationScoped;
import javax.servlet.ServletException;

import org.hibernate.SQLQuery;
import org.hibernate.dialect.function.SQLFunctionTemplate;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.type.StandardBasicTypes;
import org.openbravo.client.kernel.ApplicationInitializer;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.SystemInfo;
import org.openbravo.service.db.DalConnectionProvider;

@ApplicationScoped
public class APRMApplicationInitializer implements ApplicationInitializer {
  final static String RDBMS = new DalConnectionProvider(false).getRDBMS();

  @Override
  public void initialize() {
    OBDal.getInstance().registerSQLFunction("ad_message_get2",
        new StandardSQLFunction("ad_message_get2", StandardBasicTypes.STRING));
    OBDal.getInstance().registerSQLFunction("hqlagg",
        new SQLFunctionTemplate(StandardBasicTypes.STRING, getAggregationSQL()));
    OBDal.getInstance().registerSQLFunction("get_uuid",
        new StandardSQLFunction("get_uuid", StandardBasicTypes.STRING));
  }

  private String getAggregationSQL() {
    if ("ORACLE".equals(RDBMS)) {
      if (is11R2orNewer()) {
        return "listagg(to_char(?1), ',') WITHIN GROUP (ORDER BY ?1)";
      } else if (existsStrAgg()) {
        return "stragg(to_char(?1))";
      } else {
        return "wm_concat(to_char(?1))";
      }
    } else {
      return "array_to_string(array_agg(?1), ',')";
    }
  }

  private boolean existsStrAgg() {
    try {
      SQLQuery qry = OBDal.getInstance().getSession().createSQLQuery("select stragg(1) from dual");
      qry.list();
    } catch (Exception e) {
      return false;
    }
    return true;
  }

  private boolean is11R2orNewer() {
    String dbVersion = null;
    try {
      dbVersion = SystemInfo.getDatabaseVersion(new DalConnectionProvider(false));
    } catch (ServletException ignore) {

    }
    if (dbVersion == null) {
      return false;
    }
    int version = Integer.valueOf(dbVersion.replaceAll("\\.", "").substring(0, 3));
    if (version >= 112) {
      return true;
    }
    return false;
  }
}
