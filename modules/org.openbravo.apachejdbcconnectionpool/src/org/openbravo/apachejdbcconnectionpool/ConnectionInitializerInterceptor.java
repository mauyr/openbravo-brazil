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
 * All portions are Copyright (C) 2014-2015 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.apachejdbcconnectionpool;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Properties;

import org.apache.tomcat.jdbc.pool.ConnectionPool;
import org.apache.tomcat.jdbc.pool.JdbcInterceptor;
import org.apache.tomcat.jdbc.pool.PooledConnection;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.database.PoolInterceptorProvider;
import org.openbravo.database.SessionInfo;

/**
 * This interceptor allows to act whenever a connection is requested from the pool and whenever any
 * operation is invoked on a connection provided by Apache JDBC Connection Pool.
 */
public class ConnectionInitializerInterceptor extends JdbcInterceptor implements
    PoolInterceptorProvider {

  String rbdms = (String) OBPropertiesProvider.getInstance().getOpenbravoProperties()
      .get("bbdd.rdbms");

  /**
   * This method is called each time the connection is borrowed from the pool and it is used to
   * initialize prepareStatement.
   */
  @Override
  public void reset(ConnectionPool parent, PooledConnection con) {
    if (con != null) {
      HashMap<Object, Object> attributes = con.getAttributes();
      Boolean connectionInitialized = (Boolean) attributes.get("OB_INITIALIZED");
      if (connectionInitialized == null || connectionInitialized == false) {
        SessionInfo.setDBSessionInfo(con.getConnection(), rbdms);
        PreparedStatement pstmt = null;
        try {
          final Properties props = OBPropertiesProvider.getInstance().getOpenbravoProperties();
          final String dbSessionConfig = props.getProperty("bbdd.sessionConfig");
          pstmt = con.getConnection().prepareStatement(dbSessionConfig);
          pstmt.executeQuery();
        } catch (Exception e) {
          throw new IllegalStateException(e);
        } finally {
          try {
            if (pstmt != null && !pstmt.isClosed()) {
              pstmt.close();
            }
          } catch (SQLException e) {
            throw new OBException(e);
          }
        }
        attributes.put("OB_INITIALIZED", true);
      }
    }
  }

  @Override
  public String getPoolInterceptorsClassNames() {
    String fullClassName = this.getClass().getName();
    return fullClassName + ";";
  }
}
