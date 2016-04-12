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
 ************************************************************************
 */
package org.openbravo.database;

import java.sql.Connection;
import java.util.List;

import org.apache.log4j.Logger;

/**
 * Abstract class that represents an external connection pool
 * 
 */
public abstract class ExternalConnectionPool {

  static Logger log = Logger.getLogger(ExternalConnectionPool.class);

  private static ExternalConnectionPool instance;

  /**
   * 
   * @param externalConnectionPoolClassName
   *          The full class name of the external connection pool
   * @return An instance of the external connection pool
   * @throws InstantiationException
   * @throws IllegalAccessException
   * @throws ClassNotFoundException
   */
  public final synchronized static ExternalConnectionPool getInstance(
      String externalConnectionPoolClassName) throws InstantiationException,
      IllegalAccessException, ClassNotFoundException {
    if (instance == null) {
      instance = (ExternalConnectionPool) Class.forName(externalConnectionPoolClassName)
          .newInstance();
    }
    return instance;
  }

  /**
   * If the external connection pool should be closed this method should be overwritten
   */
  public void closePool() {
    instance = null;
  }

  /**
   * If the external connection pool supports interceptors this method should be overwritten
   * 
   * @param interceptors
   *          List of PoolInterceptorProvider comprised of all the interceptors injected with Weld
   */
  public void loadInterceptors(List<PoolInterceptorProvider> interceptors) {
  }

  /**
   * @return A Connection from the external connection pool
   */
  public abstract Connection getConnection();

}
