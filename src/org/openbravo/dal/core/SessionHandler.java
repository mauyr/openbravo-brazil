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
 * All portions are Copyright (C) 2008-2015 Openbravo SLU
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.dal.core;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.hibernate.FlushMode;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.openbravo.base.model.Entity;
import org.openbravo.base.provider.OBNotSingleton;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.base.session.SessionFactoryController;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.base.structure.Identifiable;
import org.openbravo.base.util.Check;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ExternalConnectionPool;
import org.openbravo.service.db.DbUtility;

/**
 * Keeps the Hibernate Session and Transaction in a ThreadLocal so that it is available throughout
 * the application. This class provides convenience methods to get a Session and to
 * create/commit/rollback a transaction.
 * 
 * @author mtaal
 */
// TODO: revisit when looking at factory pattern and dependency injection
// framework
public class SessionHandler implements OBNotSingleton {
  private static final Logger log = Logger.getLogger(SessionHandler.class);

  private static ExternalConnectionPool externalConnectionPool;

  {
    String poolClassName = OBPropertiesProvider.getInstance().getOpenbravoProperties()
        .getProperty("db.externalPoolClassName");
    if (poolClassName != null && !"".equals(poolClassName)) {
      try {
        externalConnectionPool = ExternalConnectionPool.getInstance(poolClassName);
      } catch (Throwable e) {
        externalConnectionPool = null;
        log.warn("External connection pool class not found: " + poolClassName, e);
      }
    }
  }

  // The threadlocal which handles the session
  private static ThreadLocal<SessionHandler> sessionHandler = new ThreadLocal<SessionHandler>();

  /**
   * Removes the current SessionHandler from the ThreadLocal. A call to getSessionHandler will
   * create a new SessionHandler, session and transaction.
   */
  public static void deleteSessionHandler() {
    log.debug("Removing sessionhandler");
    sessionHandler.set(null);
  }

  /** @return true if a session handler is present for this thread, false */
  public static boolean isSessionHandlerPresent() {
    return sessionHandler.get() != null;
  }

  /**
   * Returns the SessionHandler of this thread. If there is none then a new one is created and a
   * Hibernate Session is created and a transaction is started.
   * 
   * @return the sessionhandler for this thread
   */
  public static SessionHandler getInstance() {
    SessionHandler sh = sessionHandler.get();
    if (sh == null) {
      log.debug("Creating sessionHandler");
      sh = getCreateSessionHandler();
      sh.begin();
      sessionHandler.set(sh);
    }
    return sh;
  }

  private static boolean checkedSessionHandlerRegistration = false;

  private static SessionHandler getCreateSessionHandler() {
    if (!checkedSessionHandlerRegistration
        && !OBProvider.getInstance().isRegistered(SessionHandler.class)) {
      OBProvider.getInstance().register(SessionHandler.class, SessionHandler.class, false);
    }
    return OBProvider.getInstance().get(SessionHandler.class);
  }

  private Session session;
  private Transaction tx;
  private Connection connection;

  // Sets the session handler at rollback so that the controller can rollback
  // at the end
  private boolean doRollback = false;

  /** @return the session */
  public Session getSession() {
    return session;
  }

  protected void setSession(Session thisSession) {
    session = thisSession;
  }

  public void setConnection(Connection connection) {
    this.connection = connection;
  }

  public Connection getConnection() {
    return this.connection;
  }

  protected Session createSession() {
    SessionFactory sf = SessionFactoryController.getInstance().getSessionFactory();
    // Checks if the session connection has to be obtained using an external connection pool
    if (externalConnectionPool != null && this.getConnection() == null) {
      Connection externalConnection = externalConnectionPool.getConnection();
      try {
        // Autocommit is disabled because DAL is taking into account his logical and DAL is setting
        // autoCommint to false to maintain transactional way of working.
        externalConnection.setAutoCommit(false);
      } catch (SQLException e) {
        log.error("Error setting this connection's to auto-commit mode", e);
      }
      this.setConnection(externalConnection);
    }
    if (this.connection != null) {
      // If the connection has been obtained using an external connection pool it is passed to
      // openSession, to prevent a new connection to be created using the Hibernate default
      // connection pool
      return sf.openSession(this.connection);
    } else {
      return sf.openSession();
    }
  }

  protected void closeSession() {
    session.close();
  }

  /**
   * Saves the object in this getSession().
   * 
   * @param obj
   *          the object to persist
   */
  public void save(Object obj) {
    if (Identifiable.class.isAssignableFrom(obj.getClass())) {
      getSession().saveOrUpdate(((Identifiable) obj).getEntityName(), obj);
    } else {
      getSession().saveOrUpdate(obj);
    }
  }

  /**
   * Delete the object from the db.
   * 
   * @param obj
   *          the object to remove
   */
  public void delete(Object obj) {
    if (Identifiable.class.isAssignableFrom(obj.getClass())) {
      getSession().delete(((Identifiable) obj).getEntityName(), obj);
    } else {
      getSession().delete(obj);
    }
  }

  /**
   * Queries for a certain object using the class and id. If not found then null is returned.
   * 
   * @param clazz
   *          the class to query
   * @param id
   *          the id to use for querying
   * @return the retrieved object, can be null
   */
  @SuppressWarnings("unchecked")
  public <T extends Object> T find(Class<T> clazz, Object id) {
    // translates a class to an entityname because the hibernate
    // getSession().get method can not handle class names if the entity was
    // mapped with entitynames.
    if (Identifiable.class.isAssignableFrom(clazz)) {
      return (T) find(DalUtil.getEntityName(clazz), id);
    }
    return (T) getSession().get(clazz, (Serializable) id);
  }

  /**
   * Queries for a certain object using the entity name and id. If not found then null is returned.
   * 
   * @param entityName
   *          the name of the entity to query
   * @param id
   *          the id to use for querying
   * @return the retrieved object, can be null
   * 
   * @see Entity
   */
  public BaseOBObject find(String entityName, Object id) {
    return (BaseOBObject) getSession().get(entityName, (Serializable) id);
  }

  /**
   * Create a query object from the current getSession().
   * 
   * @param qryStr
   *          the HQL query
   * @return a new Query object
   */
  public Query createQuery(String qryStr) {
    return getSession().createQuery(qryStr);
  }

  /**
   * Starts a transaction.
   */
  protected void begin() {
    Check.isTrue(getSession() == null, "Session must be null before begin");
    setSession(createSession());
    getSession().setFlushMode(FlushMode.COMMIT);
    Check.isTrue(tx == null, "tx must be null before begin");
    tx = getSession().beginTransaction();
    log.debug("Transaction started");
  }

  /**
   * Commits the transaction and closes the session, should normally be called at the end of all the
   * work.
   */
  public void commitAndClose() {
    boolean err = true;
    try {
      Check.isFalse(TriggerHandler.getInstance().isDisabled(),
          "Triggers disabled, commit is not allowed when in triggers-disabled mode, "
              + "call TriggerHandler.enable() before committing");

      checkInvariant();
      flushRemainingChanges();
      if (connection == null || (connection != null && !connection.isClosed())) {
        if (connection != null) {
          connection.setAutoCommit(false);
        }
        tx.commit();
      }
      tx = null;
      err = false;
    } catch (SQLException e) {
      log.error("Error while closing the connection", DbUtility.getUnderlyingSQLException(e));
    } finally {
      if (err) {
        try {
          tx.rollback();
          tx = null;
        } catch (Throwable t) {
          // ignore these exception not to hide others
        }
      }
      try {
        if (connection != null && !connection.isClosed()) {
          connection.close();
        }
      } catch (SQLException e) {
        log.error("Error while closing the connection", e);
      }
      deleteSessionHandler();
      closeSession();
    }
    setSession(null);
    log.debug("Transaction closed, session closed");
  }

  /**
   * Commits the transaction and starts a new transaction.
   */
  public void commitAndStart() {
    Check.isFalse(TriggerHandler.getInstance().isDisabled(),
        "Triggers disabled, commit is not allowed when in triggers-disabled mode, "
            + "call TriggerHandler.enable() before committing");

    checkInvariant();
    flushRemainingChanges();
    tx.commit();
    tx = null;
    tx = getSession().beginTransaction();
    log.debug("Committed and started new transaction");
  }

  private void flushRemainingChanges() {

    // business event handlers can change the data
    // during flush, flush several times until
    // the session is really cleaned up
    int countFlushes = 0;
    while (OBDal.getInstance().getSession().isDirty()) {
      OBDal.getInstance().flush();
      countFlushes++;
      // arbitrary point to give up...
      if (countFlushes > 100) {
        log.error("Infinite loop in flushing session, tried more than 100 flushes");
        break;
      }
    }
  }

  /**
   * Rolls back the transaction and closes the getSession().
   */
  public void rollback() {
    log.debug("Rolling back transaction");
    try {
      checkInvariant();
      if (connection == null || (connection != null && !connection.isClosed())) {
        tx.rollback();
      }
      tx = null;
    } catch (SQLException e) {
      log.error("Error while closing the connection", e);
    } finally {
      deleteSessionHandler();
      try {
        if (connection != null && !connection.isClosed()) {
          connection.close();
        }
        log.debug("Closing session");
        closeSession();
      } catch (SQLException e) {
        log.error("Error while closing the connection", e);
      }
    }
    setSession(null);
  }

  /**
   * The invariant is that for begin, rollback and commit the session etc. are alive
   */
  private void checkInvariant() {
    Check.isNotNull(getSession(), "Session is null");
    Check.isNotNull(tx, "Tx is null");
    Check.isTrue(tx.isActive(), "Tx is not active");
  }

  /**
   * Registers that the transaction should be rolled back. Is used by the {@link DalThreadHandler}.
   * 
   * @param setRollback
   *          if true then the transaction will be rolled back at the end of the thread.
   */
  public void setDoRollback(boolean setRollback) {
    if (setRollback) {
      log.debug("Rollback is set to true");
    }
    this.doRollback = setRollback;
  }

  /** @return the doRollback value */
  public boolean getDoRollback() {
    return doRollback;
  }

  /**
   * Returns true if the session-in-view pattern should be supported. That is that the session is
   * closed and committed at the end of the request.
   * 
   * @return always true in this implementation
   */
  public boolean doSessionInViewPatter() {
    return true;
  }
}
