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
package org.openbravo.client.application;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.hibernate.SQLQuery;
import org.hibernate.exception.SQLGrammarException;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.UsedByLink;
import org.openbravo.model.ad.alert.Alert;
import org.openbravo.model.ad.alert.AlertRecipient;
import org.openbravo.model.ad.alert.AlertRule;
import org.openbravo.service.datasource.DefaultDataSourceService;
import org.openbravo.service.json.JsonConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Datasource used by the Alert Management window
 */
public class ADAlertDatasourceService extends DefaultDataSourceService {
  private static final String AD_TABLE_ID = "594";
  private static final String ALERT_STATUS = "_alertStatus";
  private static final String ALERT_RULE_TAB = "alertRule.tab.id";
  private static final Logger log = LoggerFactory.getLogger(ADAlertDatasourceService.class);

  @Override
  public Entity getEntity() {
    return ModelProvider.getInstance().getEntityByTableId(AD_TABLE_ID);
  }

  @Override
  public String fetch(Map<String, String> parameters) {
    long t = System.currentTimeMillis();
    String alertStatus = "";
    try {
      // Retrieve the information from the request parameters
      if (parameters.get(JsonConstants.WHERE_PARAMETER) != null) {
        log.warn("_where parameter is not allowed, ignoring it");
      }
      alertStatus = parameters.get(ALERT_STATUS);
      alertStatus = StringUtils.isEmpty(alertStatus) ? "" : alertStatus.toUpperCase();

      List<String> alertList = getAlertIds();

      String whereClause = buildWhereClause(alertStatus, alertList);
      parameters.put(JsonConstants.WHERE_PARAMETER, whereClause);

      if (parameters.get(JsonConstants.DISTINCT_PARAMETER) == null) {
        // Also return the tab id of the alert rule, just when loading the grid from the server.
        // This is used in the Alert Management window to navigate to the record related to an alert
        parameters.put(JsonConstants.ADDITIONAL_PROPERTIES_PARAMETER, ALERT_RULE_TAB);
      }

      return super.fetch(parameters, true);
    } catch (Exception ex) {
      log.error("Error while fetching alert data", ex);
      throw new OBException(ex);
    } finally {
      log.debug("Alert list with status {} retrieved in {} ms", alertStatus,
          System.currentTimeMillis() - t);
    }
  }

  private List<String> getAlertIds() {
    // Get alert rules visible for context's the role/user.
    try {
      OBContext.setAdminMode(false);
      StringBuffer whereClause = new StringBuffer();
      whereClause.append(" as ar ");
      whereClause.append("\nwhere exists (select 1 from ar."
          + AlertRule.PROPERTY_ADALERTRECIPIENTLIST + " as arr");
      whereClause.append("\n    where arr." + AlertRecipient.PROPERTY_USERCONTACT + ".id = :user");
      whereClause.append("\n      or (");
      whereClause.append("arr." + AlertRecipient.PROPERTY_USERCONTACT + " is null");
      whereClause.append("\n          and arr." + AlertRecipient.PROPERTY_ROLE + ".id = :role))");

      OBQuery<AlertRule> alertRulesQuery = OBDal.getInstance().createQuery(AlertRule.class,
          whereClause.toString());
      alertRulesQuery.setNamedParameter("user", DalUtil.getId(OBContext.getOBContext().getUser()));
      alertRulesQuery.setNamedParameter("role", DalUtil.getId(OBContext.getOBContext().getRole()));

      return getAlertIdsFromAlertRules(alertRulesQuery.list());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private List<String> getAlertIdsFromAlertRules(List<AlertRule> alertRules) {
    List<String> alertIds = new ArrayList<String>();
    for (AlertRule alertRule : alertRules) {
      // Adding alert rule if it has not filter clause. In case it has, it will be added only in
      // case it returns data after applying the filter clause.
      if (alertRule.getFilterClause() == null) {
        for (Alert alert : alertRule.getADAlertList()) {
          alertIds.add((String) DalUtil.getId(alert));
        }
      }

      String filterClause = null;
      if (alertRule.getFilterClause() != null) {
        try {
          filterClause = new UsedByLink().getWhereClause(RequestContext.get()
              .getVariablesSecureApp(), "", alertRule.getFilterClause());
        } catch (ServletException e) {
          throw new IllegalStateException(e);
        }
        final String sql = "select * from AD_ALERT where ISACTIVE='Y'" + " AND AD_CLIENT_ID "
            + OBDal.getInstance().getReadableClientsInClause() + " AND AD_ORG_ID "
            + OBDal.getInstance().getReadableOrganizationsInClause() + " AND AD_ALERTRULE_ID = ? "
            + (filterClause == null ? "" : filterClause);
        final SQLQuery sqlQuery = OBDal.getInstance().getSession().createSQLQuery(sql)
            .addEntity(Alert.ENTITY_NAME);
        sqlQuery.setParameter(0, alertRule.getId());

        try {
          @SuppressWarnings("unchecked")
          List<Alert> alertsWithFilterClause = sqlQuery.list();
          log.debug("Alert " + alertRule.getName() + " (" + alertRule.getId() + ") - SQL:'" + sql
              + "' - Rows: " + alertsWithFilterClause.size());
          for (Alert alert : alertsWithFilterClause) {
            alertIds.add((String) DalUtil.getId(alert));
          }
        } catch (SQLGrammarException e) {
          log.error("An error has ocurred when trying to process the alerts: " + e.getMessage(), e);
        }
      }
    }
    return alertIds;
  }

  private String buildWhereClause(String alertStatus, List<String> alertList) {
    int chunkSize = 1000;
    String filterClause;
    String whereClause = "coalesce(to_char(status), 'NEW') = '"
        + StringEscapeUtils.escapeSql(alertStatus) + "'";
    ArrayList<String> alertListToRemove;

    if (alertList.size() == 0) {
      return "1 = 2";
    }

    if (alertList.size() <= chunkSize) {
      whereClause += " and e.id in (" + toStringList(alertList) + ")";
      return whereClause;
    }

    // There are more than 1000 alerts to include in the where clause, Oracle doesn't
    // support it, so let's split them in chunks with <=1000 elements each
    alertListToRemove = new ArrayList<String>();
    filterClause = "";
    while (alertList.size() > chunkSize) {
      alertListToRemove = new ArrayList<String>(alertList.subList(0, chunkSize - 1));
      if (StringUtils.isEmpty(filterClause)) {
        filterClause = " and (e.id in (" + toStringList(alertListToRemove) + ")";
      } else {
        filterClause += " or e.id in (" + toStringList(alertListToRemove) + ")";
      }
      alertList.removeAll(alertListToRemove);
    }
    if (!alertList.isEmpty()) {
      filterClause += " or e.id in (" + toStringList(alertList) + "))";
    } else {
      filterClause += ")";
    }
    whereClause += filterClause;
    return whereClause;
  }

  private String toStringList(List<String> list) {
    String result = "";
    for (String s : list) {
      if (!StringUtils.isEmpty(result)) {
        result += ", ";
      }
      result += "'" + s + "'";
    }
    return result;
  }
}