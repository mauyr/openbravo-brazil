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
 * All portions are Copyright (C) 2014 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.service.datasource;

import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.openbravo.dal.core.OBContext;

/**
 * This class contains utility methods for dataSource related classes
 * 
 */
public class DataSourceUtils {
  /**
   * Returns a comma separated list of organization ids to filter the HQL. If an organization id is
   * provided its natural tree is returned. If no organization is provided or the given value is
   * invalid the readable organizations are returned.
   */
  public static String getOrgs(String orgId) {
    StringBuffer orgPart = new StringBuffer();
    if (StringUtils.isNotEmpty(orgId)) {
      final Set<String> orgSet = OBContext.getOBContext().getOrganizationStructureProvider()
          .getNaturalTree(orgId);
      if (orgSet.size() > 0) {
        boolean addComma = false;
        for (String org : orgSet) {
          if (addComma) {
            orgPart.append(",");
          }
          orgPart.append("'" + org + "'");
          addComma = true;
        }
      }
    }
    if (orgPart.length() == 0) {
      String[] orgs = OBContext.getOBContext().getReadableOrganizations();
      boolean addComma = false;
      for (int i = 0; i < orgs.length; i++) {
        if (addComma) {
          orgPart.append(",");
        }
        orgPart.append("'" + orgs[i] + "'");
        addComma = true;
      }
    }
    return orgPart.toString();
  }
}
