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

package org.openbravo.test.datasource;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Organization;

/**
 * Test case for 'Allow Unpaged Datasource In Manual Request' preference
 * 
 * See issue http://issues.openbravo.com/view.php?id=30204
 */
@RunWith(Parameterized.class)
public class TestAllowUnpagedDatasourcePreference extends BaseDataSourceTestDal {

  protected Logger logger = Logger.getLogger(this.getClass());
  private String preferenceValue;

  /**
   * @param preferenceValue
   *          value to be assigned to the preference
   * @param description
   *          description for the test case
   */
  public TestAllowUnpagedDatasourcePreference(String preferenceValue, String description) {
    this.preferenceValue = preferenceValue;
  }

  @Parameters(name = "{index}: ''{1}'' -- preference value: {0}")
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][] { { "N", "Manual request should not be performed" },
        { "Y", "Manual request should be performed" } });
  }

  @Test
  public void testDatasourceRequest() {
    OBContext.setAdminMode();
    User user = null;
    String defaultClient = null;
    String defaultOrg = null;
    String defaultRole = null;
    String preferenceId = "";
    try {
      user = OBDal.getInstance().get(User.class, "100"); // Openbravo user;
      defaultClient = user.getDefaultClient() != null ? (String) DalUtil.getId(user
          .getDefaultClient()) : null;
      defaultOrg = user.getDefaultOrganization() != null ? (String) DalUtil.getId(user
          .getDefaultOrganization()) : null;
      defaultRole = (String) DalUtil.getId(user.getDefaultRole());

      // Execute ws with system administrator credentials
      user.setDefaultClient(OBDal.getInstance().get(Client.class, "0"));
      user.setDefaultOrganization(OBDal.getInstance().get(Organization.class, "0"));
      user.setDefaultRole(OBDal.getInstance().get(Role.class, "0"));
      OBDal.getInstance().commitAndClose();

      // Create the 'Allow Unpaged Datasource In Manual Request' preference
      preferenceId = createPreference(preferenceValue);
      // Create a manual request to the datasource
      String response = "";
      Map<String, String> params = new HashMap<String, String>();
      params = new HashMap<String, String>();
      params.put("_operationType", "fetch");
      try {
        response = doRequest("/org.openbravo.service.datasource/UOM", params, 200, "POST");
      } catch (Exception ignore) {
        // Expected exception when preference value is "N"
        logger.debug("Exception in datasource request:" + ignore.getMessage(), ignore);
      }
      // Compare the error message in response, if any
      String errorMsg = "";
      if ("N".equals(preferenceValue)) {
        errorMsg = OBMessageUtils.messageBD("OBJSON_NoPagedFetchManual");
      }
      assertThat("Datasource returned error message", getResponseErrorMessage(response),
          equalTo(errorMsg));
    } finally {
      if (!StringUtils.isEmpty(preferenceId)) {
        deletePreference(preferenceId);
      }
      // restore user defaults
      Client client = defaultClient != null ? OBDal.getInstance().get(Client.class, defaultClient)
          : null;
      Organization org = defaultOrg != null ? OBDal.getInstance().get(Organization.class,
          defaultOrg) : null;
      user = OBDal.getInstance().get(User.class, "100");
      user.setDefaultClient(client);
      user.setDefaultOrganization(org);
      user.setDefaultRole(OBDal.getInstance().get(Role.class, defaultRole));
      OBDal.getInstance().commitAndClose();
      OBContext.restorePreviousMode();
    }
  }

  private String createPreference(String value) {
    try {
      String content = "{" //
          + "  \"data\": {" //
          + "    \"entityName\": \"ADPreference\"," //
          + "    \"property\": \"OBJSON_AllowUnpagedDatasourceManualRequest\"," //
          + "    \"propertyList\": true," //
          + "    \"visibleAtClient\": \"0\"," //
          + "    \"visibleAtOrganization\": \"0\"," //
          + "    \"searchKey\": \"" + value + "\"" //
          + "  }" //
          + "}";
      final HttpURLConnection hc = createConnection(
          "/org.openbravo.service.json.jsonrest/ADPreference", "POST");
      final OutputStream os = hc.getOutputStream();
      os.write(content.getBytes("UTF-8"));
      os.flush();
      os.close();
      hc.connect();
      // Get ws response
      StringBuilder sb = new StringBuilder();
      final InputStream is = hc.getInputStream();
      BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
      String line;
      while ((line = reader.readLine()) != null) {
        sb.append(line).append("\n");
      }
      return getIdFromResponse(sb.toString());

    } catch (Exception e) {
      throw new OBException("Exception on create preference: ", e);
    }
  }

  private String deletePreference(String preferenceId) {
    try {
      // String content = "";
      final HttpURLConnection hc = createConnection(
          "/org.openbravo.service.json.jsonrest/ADPreference/" + preferenceId, "DELETE");
      hc.connect();
      // Get ws response
      StringBuilder sb = new StringBuilder();
      final InputStream is = hc.getInputStream();
      BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
      String line;
      while ((line = reader.readLine()) != null) {
        sb.append(line).append("\n");
      }
      return sb.toString();

    } catch (Exception e) {
      throw new OBException("Exception on delete preference: ", e);
    }
  }

  private HttpURLConnection createConnection(String wsPart, String method) throws Exception {
    Authenticator.setDefault(new Authenticator() {
      @Override
      protected PasswordAuthentication getPasswordAuthentication() {
        return new PasswordAuthentication(LOGIN, PWD.toCharArray());
      }
    });
    final URL url = new URL(getOpenbravoURL() + wsPart);
    final HttpURLConnection hc = (HttpURLConnection) url.openConnection();
    hc.setRequestMethod(method);
    hc.setAllowUserInteraction(false);
    hc.setDefaultUseCaches(false);
    hc.setDoOutput(true);
    hc.setDoInput(true);
    hc.setInstanceFollowRedirects(true);
    hc.setUseCaches(false);
    hc.setRequestProperty("Content-Type", "text/xml");
    return hc;
  }

  private String getResponseErrorMessage(String response) {
    try {
      JSONObject jsonResponse = new JSONObject(response).getJSONObject("response");
      if (jsonResponse.has("error")) {
        JSONObject error = jsonResponse.getJSONObject("error");
        return error.getString("message");
      }
      return "";
    } catch (Exception ex) {
      return "";
    }
  }

  private String getIdFromResponse(String response) {
    try {
      JSONObject jsonResponse = new JSONObject(response).getJSONObject("response");
      if (jsonResponse.has("data")) {
        JSONObject data = jsonResponse.getJSONArray("data").getJSONObject(0);
        return data.getString("id");
      }
      return "";
    } catch (Exception ex) {
      return "";
    }
  }
}
