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
 * All portions are Copyright (C) 2014-15 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.costing;

import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Query;
import org.openbravo.base.exception.OBException;
import org.openbravo.client.application.process.BaseProcessActionHandler;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.security.OrganizationStructureProvider;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.financialmgmt.calendar.Period;
import org.openbravo.service.db.DbUtility;
import org.openbravo.service.json.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PriceDifferenceByDateProcess extends BaseProcessActionHandler {
  private static final Logger log = LoggerFactory.getLogger(PriceDifferenceByDateProcess.class);

  @Override
  protected JSONObject doExecute(Map<String, Object> parameters, String content) {
    JSONObject jsonRequest = null;
    String message = new String();
    OBContext.setAdminMode(true);
    try {
      jsonRequest = new JSONObject(content);
      JSONObject params = jsonRequest.getJSONObject("_params");
      log.debug("{}", jsonRequest);
      JSONArray productIds = params.getJSONArray("M_Product_ID");
      String mvdate = params.getString("movementdate");
      String orgId = params.getString("ad_org_id");
      productIds.toString();
      Date movementdate = JsonUtils.createDateFormat().parse(mvdate);
      doChecks(orgId, movementdate);

      String strUpdate = "UPDATE MaterialMgmtMaterialTransaction trx"
          + " SET checkpricedifference = 'Y'"
          + " WHERE exists ("
          + " SELECT 1"
          + " FROM  ProcurementReceiptInvoiceMatch mpo"
          + " WHERE trx.isCostCalculated = 'Y' and mpo.goodsShipmentLine.id = trx.goodsShipmentLine.id  "
          + " AND trx.movementDate >= :date and trx.organization.id in (:orgIds))";

      if (productIds.length() > 0) {
        strUpdate = strUpdate.concat(" AND product.id IN :productIds ");
      }

      Set<String> products = new HashSet<String>();
      for (int i = 0; i < productIds.length(); i++) {
        products.add(productIds.getString(i));
      }
      Query update = OBDal.getInstance().getSession().createQuery(strUpdate);

      if (productIds.length() > 0) {
        update.setParameterList("productIds", products);
      }
      update.setParameterList("orgIds",
          new OrganizationStructureProvider().getChildTree(orgId, true));
      update.setDate("date", movementdate);

      update.executeUpdate();

      JSONObject msg = new JSONObject();
      msg = PriceDifferenceProcess.processPriceDifference(null, null);
      jsonRequest.put("message", msg);
    } catch (Exception e) {
      log.error("Error Process Price Correction", e);

      try {
        jsonRequest = new JSONObject();
        jsonRequest.put("retryExecution", true);
        if (message.isEmpty()) {
          Throwable ex = DbUtility.getUnderlyingSQLException(e);
          message = OBMessageUtils.translateError(ex.getMessage()).getMessage();
        }
        JSONObject errorMessage = new JSONObject();
        errorMessage.put("severity", "error");
        errorMessage.put("text", message);
        jsonRequest.put("message", errorMessage);
        return jsonRequest;
      } catch (Exception ignore) {
      }
    } finally {
      OBContext.restorePreviousMode();
    }
    return jsonRequest;
  }

  private void doChecks(String orgId, Date movementdate) {
    try {
      Organization org = OBDal.getInstance().get(Organization.class, orgId);
      Date maxDate = CostingUtils.getMaxTransactionDate(org);
      Period periodClosed = CostingUtils.periodClosed(org, movementdate, maxDate, "CAD");
      if (periodClosed != null) {
        String errorMsg = OBMessageUtils.getI18NMessage("DocumentTypePeriodClosed", new String[] {
            "CAD", periodClosed.getIdentifier() });
        throw new OBException(errorMsg);
      }
    } catch (ServletException e) {
      throw new OBException(e.getMessage());
    }
  }
}