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

import java.sql.Connection;
import java.util.List;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;

import org.apache.tomcat.jdbc.pool.DataSource;
import org.apache.tomcat.jdbc.pool.PoolProperties;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.base.session.SessionFactoryController;
import org.openbravo.database.ExternalConnectionPool;
import org.openbravo.database.PoolInterceptorProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JdbcExternalConnectionPool manages all the functionality of the Apache JDBC Connection Pool. This
 * class provides convenience methods to get a connection, close the pool and other actions.
 */
public class JdbcExternalConnectionPool extends ExternalConnectionPool {

  final static private Logger log = LoggerFactory.getLogger(JdbcExternalConnectionPool.class);

  private DataSource dataSource = null;

  /**
   * This method loads all the interceptors of apache jdbc connection pool injected with weld.
   */
  @Override
  public void loadInterceptors(List<PoolInterceptorProvider> interceptors) {
    String currentInterceptors = this.getDataSource().getJdbcInterceptors();
    for (PoolInterceptorProvider interceptor : interceptors) {
      currentInterceptors += interceptor.getPoolInterceptorsClassNames();
    }
    this.getDataSource().setJdbcInterceptors(currentInterceptors);
  }

  /**
   * Gets the data source of apache jdbc connection pool.
   */
  public DataSource getDataSource() {
    return dataSource;
  }

  /**
   * This method provided a connection of apache jdbc connection pool. Apache jdbc connection pool
   * is initialized in the first call to this method.
   */
  @Override
  public Connection getConnection() {
    if (dataSource == null) {
      initPool();
    }
    Connection connection = null;
    try {
      connection = dataSource.getConnection();
      // All connections are setting autoCommit to true. DAL is taking into account his logical and
      // DAL is setting autoCommint to false to maintain transactional way of working.
      connection.setAutoCommit(true);
    } catch (Exception e) {
      log.error("Error while retrieving connection: ", e);
      throw new OBException(e);
    }
    return connection;
  }

  private void initPool() {
    dataSource = new DataSource();
    dataSource.setPoolProperties(getPoolProperties());
  }

  private PoolProperties getPoolProperties() {
    Properties poolPropertiesConfig = OBPropertiesProvider.getInstance().getOpenbravoProperties();

    PoolProperties poolProperties = new PoolProperties();

    poolProperties.setJdbcInterceptors("org.apache.tomcat.jdbc.pool.interceptor.ConnectionState;"
        + "org.apache.tomcat.jdbc.pool.interceptor.StatementFinalizer;"
        + "org.openbravo.apachejdbcconnectionpool.ConnectionInitializerInterceptor;");

    if (SessionFactoryController.isJNDIModeOn(poolPropertiesConfig)) {
      try {
        Context initctx = new InitialContext();
        Context ctx = (Context) initctx.lookup("java:/comp/env");
        javax.sql.DataSource ds = (javax.sql.DataSource) ctx.lookup(poolPropertiesConfig
            .getProperty("JNDI.resourceName"));
        poolProperties.setDataSource(ds);
        return poolProperties;
      } catch (Exception e) {
        log.error("Error trying to get JNDI datasource, trying to get direct DB connection", e);
        poolProperties = new PoolProperties();
      }
    }

    String obUrl = poolPropertiesConfig.getProperty("bbdd.url");
    String sid = poolPropertiesConfig.getProperty("bbdd.sid");
    String driver = poolPropertiesConfig.getProperty("bbdd.driver");
    String username = poolPropertiesConfig.getProperty("bbdd.user");
    String password = poolPropertiesConfig.getProperty("bbdd.password");
    String rbdms = poolPropertiesConfig.getProperty("bbdd.rdbms");

    if ("POSTGRE".equals(rbdms)) {
      poolProperties.setUrl(obUrl + "/" + sid);
    } else {
      poolProperties.setUrl(obUrl);
    }
    poolProperties.setDriverClassName(driver);
    poolProperties.setUsername(username);
    poolProperties.setPassword(password);

    if (poolPropertiesConfig.getProperty("db.pool.initialSize") != null) {
      poolProperties.setInitialSize(getIntProperty(poolPropertiesConfig, "db.pool.initialSize"));
    }
    if (poolPropertiesConfig.getProperty("db.pool.maxActive") != null) {
      poolProperties.setMaxActive(getIntProperty(poolPropertiesConfig, "db.pool.maxActive"));
    }
    if (poolPropertiesConfig.getProperty("db.pool.minIdle") != null) {
      poolProperties.setMinIdle(getIntProperty(poolPropertiesConfig, "db.pool.minIdle"));
    }
    if (poolPropertiesConfig.getProperty("db.pool.timeBetweenEvictionRunsMillis") != null) {
      poolProperties.setTimeBetweenEvictionRunsMillis(getIntProperty(poolPropertiesConfig,
          "db.pool.timeBetweenEvictionRunsMillis"));
    }
    if (poolPropertiesConfig.getProperty("db.pool.minEvictableIdleTimeMillis") != null) {
      poolProperties.setMinEvictableIdleTimeMillis(getIntProperty(poolPropertiesConfig,
          "db.pool.minEvictableIdleTimeMillis"));
    }
    if (poolPropertiesConfig.getProperty("db.pool.removeAbandoned") != null) {
      poolProperties.setRemoveAbandoned(getBooleanProperty(poolPropertiesConfig,
          "db.pool.removeAbandoned"));
    }
    if (poolPropertiesConfig.getProperty("db.pool.testWhileIdle") != null) {
      poolProperties.setTestWhileIdle(getBooleanProperty(poolPropertiesConfig,
          "db.pool.testWhileIdle"));
    }
    if (poolPropertiesConfig.getProperty("db.pool.testOnBorrow") != null) {
      poolProperties.setTestOnBorrow(getBooleanProperty(poolPropertiesConfig,
          "db.pool.testOnBorrow"));
    }
    if (poolPropertiesConfig.getProperty("db.pool.testOnReturn") != null) {
      poolProperties.setTestOnReturn(getBooleanProperty(poolPropertiesConfig,
          "db.pool.testOnReturn"));
    }
    if (poolPropertiesConfig.getProperty("db.pool.validationInterval") != null) {
      poolProperties.setValidationInterval(getIntProperty(poolPropertiesConfig,
          "db.pool.validationInterval"));
    }
    if (poolPropertiesConfig.getProperty("db.pool.validationQuery") != null) {
      poolProperties
          .setValidationQuery(poolPropertiesConfig.getProperty("db.pool.validationQuery"));
    }
    if (poolPropertiesConfig.getProperty("db.pool.defaultTransactionIsolation") != null) {
      poolProperties.setDefaultTransactionIsolation(getIntProperty(poolPropertiesConfig,
          "db.pool.defaultTransactionIsolation"));
    }
    if (poolPropertiesConfig.getProperty("db.pool.maxIdle") != null) {
      poolProperties.setMaxIdle(getIntProperty(poolPropertiesConfig, "db.pool.maxIdle"));
    }
    if (poolPropertiesConfig.getProperty("db.pool.maxWait") != null) {
      poolProperties.setMaxWait(getIntProperty(poolPropertiesConfig, "db.pool.maxWait"));
    }
    if (poolPropertiesConfig.getProperty("db.pool.numTestsPerEvictionRun") != null) {
      poolProperties.setNumTestsPerEvictionRun(getIntProperty(poolPropertiesConfig,
          "db.pool.numTestsPerEvictionRun"));
    }
    if (poolPropertiesConfig.getProperty("db.pool.removeAbandonedTimeout") != null) {
      poolProperties.setRemoveAbandonedTimeout(getIntProperty(poolPropertiesConfig,
          "db.pool.removeAbandonedTimeout"));
    }
    if (poolPropertiesConfig.getProperty("db.pool.accessToUnderlyingConnectionAllowed") != null) {
      poolProperties.setAccessToUnderlyingConnectionAllowed(getBooleanProperty(
          poolPropertiesConfig, "db.pool.accessToUnderlyingConnectionAllowed"));
    }
    if (poolPropertiesConfig.getProperty("db.pool.defaultAutoCommit") != null) {
      poolProperties.setDefaultAutoCommit(getBooleanProperty(poolPropertiesConfig,
          "db.pool.defaultAutoCommit"));
    }
    if (poolPropertiesConfig.getProperty("db.pool.defaultReadOnly") != null) {
      poolProperties.setDefaultReadOnly(getBooleanProperty(poolPropertiesConfig,
          "db.pool.defaultReadOnly"));
    }
    if (poolPropertiesConfig.getProperty("db.pool.logAbandoned") != null) {
      poolProperties.setLogAbandoned(getBooleanProperty(poolPropertiesConfig,
          "db.pool.logAbandoned"));
    }
    if (poolPropertiesConfig.getProperty("db.pool.testOnConnect") != null) {
      poolProperties.setTestOnConnect(getBooleanProperty(poolPropertiesConfig,
          "db.pool.testOnConnect"));
    }
    if (poolPropertiesConfig.getProperty("db.pool.connectionProperties") != null) {
      poolProperties.setConnectionProperties(poolPropertiesConfig
          .getProperty("db.pool.connectionProperties"));
    }
    if (poolPropertiesConfig.getProperty("db.pool.defaultCatalog") != null) {
      poolProperties.setDefaultCatalog(poolPropertiesConfig.getProperty("db.pool.defaultCatalog"));
    }
    if (poolPropertiesConfig.getProperty("db.pool.validatorClassName") != null) {
      poolProperties.setValidatorClassName(poolPropertiesConfig
          .getProperty("db.pool.validatorClassName"));
    }
    if (poolPropertiesConfig.getProperty("db.pool.initSQL") != null) {
      poolProperties.setInitSQL(poolPropertiesConfig.getProperty("db.pool.initSQL"));
    }
    if (poolPropertiesConfig.getProperty("db.pool.name") != null) {
      poolProperties.setName(poolPropertiesConfig.getProperty("db.pool.name"));
    }

    return poolProperties;
  }

  private boolean getBooleanProperty(Properties properties, String propertyName) {
    return ("true".equals(properties.getProperty(propertyName)));
  }

  private int getIntProperty(Properties properties, String propertyName) {
    return Integer.parseInt(properties.getProperty(propertyName).trim());
  }

  /**
   * This method closes apache jdbc connection pool.
   */
  @Override
  public void closePool() {
    DataSource ds = getDataSource();
    if (ds != null) {
      // Closes the pool and all idle connections. true parameter is for close the active
      // connections too.
      ds.close(true);
    }
    super.closePool();
  }
}
