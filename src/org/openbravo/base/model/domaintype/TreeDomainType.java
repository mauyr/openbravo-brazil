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
 * All portions are Copyright (C) 2013-2014 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.base.model.domaintype;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.model.Column;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.RefTree;
import org.openbravo.base.model.Table;

/**
 * The type of columns which have a tree reference.
 */

public class TreeDomainType extends BaseForeignKeyDomainType {
  private static final Logger log = Logger.getLogger(TreeDomainType.class);

  private Column column;
  private String tableName;

  @Override
  public List<Class<?>> getClasses() {
    List<Class<?>> listOfClasses = new ArrayList<Class<?>>();
    listOfClasses.add(RefTree.class);
    return listOfClasses;
  }

  public void initialize() {

    Session session = ModelProvider.getInstance().getSession();

    final Criteria criteria = session.createCriteria(RefTree.class);
    criteria.add(Restrictions.eq("referenceId", getReference().getId()));
    final List<?> list = criteria.list();
    if (list.isEmpty()) {
      // a base reference
      if (getReference().getParentReference() == null) {
        return;
      }
      log.error("No tree reference definition found for reference " + getReference());
      return;
    } else if (list.size() > 1) {
      log.warn("Reference " + getReference()
          + " has more than one tree definition, only one is really used");
    }
    final RefTree treeReference = (RefTree) list.get(0);
    Table table = treeReference.getTable();
    if (table == null) {
      throw new IllegalStateException("The tree reference " + treeReference.getIdentifier()
          + " is used in a foreign key reference but no table has been set");
    }
    tableName = table.getTableName();
    if (treeReference.getColumn() == null) {
      Column keyColumn = readKeyColumn(session, table);
      if (keyColumn != null) {
        column = keyColumn;
      }
    } else {
      column = treeReference.getColumn();
    }
  }

  @SuppressWarnings("unchecked")
  private Column readKeyColumn(Session session, Table table) {
    final Criteria c = session.createCriteria(Column.class);
    c.add(Restrictions.eq("table", table));
    c.add(Restrictions.eq("key", true));
    c.addOrder(Order.asc("position"));
    Column keyColumn = null;
    List<Column> keyColumns = c.list();
    if (!keyColumns.isEmpty()) {
      keyColumn = keyColumns.get(0);
    }
    return keyColumn;
  }

  @Override
  public Column getForeignKeyColumn(String columnName) {
    while (!column.isKey() && column.getDomainType() instanceof ForeignKeyDomainType) {
      column = ((ForeignKeyDomainType) column.getDomainType()).getForeignKeyColumn(column
          .getColumnName());
      tableName = column.getTable().getName();
    }
    return column;
  }

  @Override
  protected String getReferedTableName(String columnName) {
    return tableName;
  }
}