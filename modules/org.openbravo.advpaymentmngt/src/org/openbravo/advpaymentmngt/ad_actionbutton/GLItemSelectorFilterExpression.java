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
 * All portions are Copyright (C) 2012-2015 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.advpaymentmngt.ad_actionbutton;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.openbravo.client.application.FilterExpression;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;

import org.openbravo.erpCommon.utility.Utility;


public class GLItemSelectorFilterExpression implements FilterExpression {
  final static String FINANCIAL_ACCOUNT_WINDOW = "94EAA455D2644E04AB25D93BE5157B6D";

  @Override
  public String getExpression(Map<String, String> requestMap) {
    if (!FINANCIAL_ACCOUNT_WINDOW.equals(requestMap.get("inpwindowId"))) {
      return "";
    }
    StringBuilder whereClause = new StringBuilder();
    String orgId = (String) RequestContext.get().getSession()
        .getAttribute(FINANCIAL_ACCOUNT_WINDOW + "|AD_ORG_ID");
    if(StringUtils.isEmpty(orgId)) {
    	orgId = requestMap.get("inpadOrgId");
    }
    String orgList = Utility.getInStrSet(OBContext.getOBContext()
        .getOrganizationStructureProvider().getNaturalTree(orgId));
    if (!orgList.isEmpty()) {
      whereClause.append("e.organization.id in (" + orgList + ")");
    }
    return whereClause.toString();
  }
}
