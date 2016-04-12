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
package org.openbravo.service.json;

import javax.enterprise.context.ApplicationScoped;

import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.erpCommon.utility.PropertyException;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Organization;

/**
 * This class is used as a singleton by the {@link DefaultJsonDataService} to keep the value of the
 * 'Allow Unpaged Datasource Manual Request' preference in cache during the application life cycle,
 * avoiding the time spent to compute the preference value.
 * 
 * This class it is also used by the
 * {@link org.openbravo.client.application.event.PreferenceEventHandler} class to detect changes in
 * the preference value, and it that case it invalidates the stored value. This way the next time it
 * is requested, the current value will be retrieved from database again.
 * 
 * This mechanism for automatic refresh the preference value, only works on environments with a
 * single JVM. In case of Tomcat clustering environments (multiple JVM) it will be necessary to
 * restart Tomcat to retrieve the new value of the preference in every JVM.
 * 
 * {@link "https://issues.openbravo.com/view.php?id=30204"}
 */
@ApplicationScoped
public class UnpagedRequestCachedPreference {
  private final String property = "OBJSON_AllowUnpagedDatasourceManualRequest";
  private String preferenceValue;

  /**
   * It returns a String with the preference value. In case this value is not stored in cache,i.e.,
   * it is null then the value will be retrieved from database.
   * 
   * @return A String with the value of the OBJSON_AllowUnpagedDatasourceManualRequest preference
   */
  public String getPreferenceValue() {
    if (preferenceValue == null) {
      try {
        OBContext.setAdminMode(false);
        Client systemClient = OBDal.getInstance().get(Client.class, "0");
        Organization asterisk = OBDal.getInstance().get(Organization.class, "0");
        setPreferenceValue(Preferences.getPreferenceValue(property, true, systemClient, asterisk,
            null, null, null));
      } catch (PropertyException ignore) {
        // Ignore the exception, caused because the preference was not found
        setPreferenceValue("N");
      } finally {
        OBContext.restorePreviousMode();
      }
    }
    return preferenceValue;
  }

  /**
   * It returns a String with the property name of the preference.
   * 
   * @return A String with the OBJSON_AllowUnpagedDatasourceManualRequest property name
   */
  public String getProperty() {
    return property;
  }

  /**
   * Sets the cached value of the preference. This method is defined as synchronized in order to
   * avoid concurrency problems.
   * 
   * @param preferenceValue
   *          String with the value assigned to the preference
   */
  public synchronized void setPreferenceValue(String preferenceValue) {
    this.preferenceValue = preferenceValue;
  }
}
