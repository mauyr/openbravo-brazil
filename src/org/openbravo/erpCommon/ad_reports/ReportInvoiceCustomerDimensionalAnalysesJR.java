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
 * All portions are Copyright (C) 2001-2015 Openbravo SLU 
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.erpCommon.ad_reports;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.StringTokenizer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.openbravo.base.filter.IsIDFilter;
import org.openbravo.base.filter.IsPositiveIntFilter;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.costing.CostingBackground;
import org.openbravo.costing.CostingStatus;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.erpCommon.businessUtility.Tree;
import org.openbravo.erpCommon.businessUtility.TreeData;
import org.openbravo.erpCommon.businessUtility.WindowTabs;
import org.openbravo.erpCommon.info.SelectorUtilityData;
import org.openbravo.erpCommon.utility.ComboTableData;
import org.openbravo.erpCommon.utility.DateTimeData;
import org.openbravo.erpCommon.utility.LeftTabsBar;
import org.openbravo.erpCommon.utility.NavigationBar;
import org.openbravo.erpCommon.utility.OBCurrencyUtils;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.PropertyException;
import org.openbravo.erpCommon.utility.ToolBar;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.utils.Replace;
import org.openbravo.xmlEngine.XmlDocument;

public class ReportInvoiceCustomerDimensionalAnalysesJR extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;

  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException,
      ServletException {
    VariablesSecureApp vars = new VariablesSecureApp(request);

    // Get user Client's base currency
    String strUserCurrencyId = Utility.stringBaseCurrencyId(this, vars.getClient());
    if (vars.commandIn("DEFAULT", "DEFAULT_COMPARATIVE")) {
      String strDateFrom = vars.getGlobalVariable("inpDateFrom",
          "ReportInvoiceCustomerDimensionalAnalysesJR|dateFrom", "");
      String strDateTo = vars.getGlobalVariable("inpDateTo",
          "ReportInvoiceCustomerDimensionalAnalysesJR|dateTo", "");
      String strDateFromRef = vars.getGlobalVariable("inpDateFromRef",
          "ReportInvoiceCustomerDimensionalAnalysesJR|dateFromRef", "");
      String strDateToRef = vars.getGlobalVariable("inpDateToRef",
          "ReportInvoiceCustomerDimensionalAnalysesJR|dateToRef", "");
      String strDateFromRef2 = vars.getGlobalVariable("inpDateFromRef2",
          "ReportInvoiceCustomerDimensionalAnalysesJR|dateFromRef2", "");
      String strDateToRef2 = vars.getGlobalVariable("inpDateToRef2",
          "ReportInvoiceCustomerDimensionalAnalysesJR|dateToRef2", "");
      String strDateFromRef3 = vars.getGlobalVariable("inpDateFromRef3",
          "ReportInvoiceCustomerDimensionalAnalysesJR|dateFromRef3", "");
      String strDateToRef3 = vars.getGlobalVariable("inpDateToRef3",
          "ReportInvoiceCustomerDimensionalAnalysesJR|dateToRef3", "");
      String strPartnerGroup = vars.getGlobalVariable("inpPartnerGroup",
          "ReportInvoiceCustomerDimensionalAnalysesJR|partnerGroup", "");
      String strcBpartnerId = vars.getInGlobalVariable("inpcBPartnerId_IN",
          "ReportInvoiceCustomerDimensionalAnalysesJR|partner", "", IsIDFilter.instance);
      String strProductCategory = vars.getGlobalVariable("inpProductCategory",
          "ReportInvoiceCustomerDimensionalAnalysesJR|productCategory", "");
      String strmProductId = vars.getInGlobalVariable("inpmProductId_IN",
          "ReportInvoiceCustomerDimensionalAnalysesJR|product", "", IsIDFilter.instance);
      // ad_ref_list.value for refercence_id 800087
      String strNotShown = vars.getInGlobalVariable("inpNotShown",
          "ReportInvoiceCustomerDimensionalAnalysesJR|notShown", "", IsPositiveIntFilter.instance);
      String strShown = vars.getInGlobalVariable("inpShown",
          "ReportInvoiceCustomerDimensionalAnalysesJR|shown", "", IsPositiveIntFilter.instance);
      String strOrg = vars.getGlobalVariable("inpOrg",
          "ReportInvoiceCustomerDimensionalAnalysesJR|org", "");
      String strsalesrepId = vars.getGlobalVariable("inpSalesrepId",
          "ReportInvoiceCustomerDimensionalAnalysesJR|salesrep", "");
      String strcProjectId = vars.getGlobalVariable("inpcProjectId",
          "ReportInvoiceCustomerDimensionalAnalysesJR|project", "");
      String strProducttype = vars.getGlobalVariable("inpProducttype",
          "ReportInvoiceCustomerDimensionalAnalysesJR|producttype", "");
      String strcDocTypeId = vars.getInGlobalVariable("inpcDocTypeId_IN",
          "ReportInvoiceCustomerDimensionalAnalysesJR|doctype", "", IsIDFilter.instance);
      String strOrder = vars.getGlobalVariable("inpOrder",
          "ReportInvoiceCustomerDimensionalAnalyze|order", "Normal");
      String strMayor = vars.getNumericGlobalVariable("inpMayor",
          "ReportInvoiceCustomerSalesDimensionalAnalyze|mayor", "");
      String strMenor = vars.getNumericGlobalVariable("inpMenor",
          "ReportInvoiceCustomerDimensionalAnalyze|menor", "");
      String strPartnerSalesRepId = vars.getGlobalVariable("inpPartnerSalesrepId",
          "ReportInvoiceCustomerDimensionalAnalysesJR|partnersalesrep", "");
      String strCurrencyId = vars.getGlobalVariable("inpCurrencyId",
          "ReportInvoiceCustomerDimensionalAnalysesJR|currency", strUserCurrencyId);
      String strComparative = "";
      String strVoid = "";
      if (vars.commandIn("DEFAULT_COMPARATIVE")) {
        strComparative = vars.getRequestGlobalVariable("inpComparative",
            "ReportInvoiceCustomerDimensionalAnalysesJR|comparative");
        strVoid = vars.getRequestGlobalVariable("inpVoid",
            "ReportInvoiceCustomerDimensionalAnalysesJR|Void");
      } else {
        strComparative = vars.getGlobalVariable("inpComparative",
            "ReportInvoiceCustomerDimensionalAnalysesJR|comparative", "N");
        strVoid = vars.getGlobalVariable("inpVoid",
            "ReportInvoiceCustomerDimensionalAnalysesJR|Void", "Y");
      }

      printPageDataSheet(request, response, vars, strComparative, strDateFrom, strDateTo,
          strPartnerGroup, strcBpartnerId, strProductCategory, strmProductId, strNotShown,
          strShown, strDateFromRef, strDateToRef, strDateFromRef2, strDateToRef2, strDateFromRef3,
          strDateToRef3, strOrg, strsalesrepId, strcProjectId, strProducttype, strcDocTypeId,
          strOrder, strMayor, strMenor, strPartnerSalesRepId, strCurrencyId, strVoid);
    } else if (vars.commandIn("EDIT_HTML", "EDIT_HTML_COMPARATIVE")) {
      String strDateFrom = vars.getRequestGlobalVariable("inpDateFrom",
          "ReportInvoiceCustomerDimensionalAnalysesJR|dateFrom");
      String strDateTo = vars.getRequestGlobalVariable("inpDateTo",
          "ReportInvoiceCustomerDimensionalAnalysesJR|dateTo");
      String strDateFromRef = vars.getRequestGlobalVariable("inpDateFromRef",
          "ReportInvoiceCustomerDimensionalAnalysesJR|dateFromRef");
      String strDateToRef = vars.getRequestGlobalVariable("inpDateToRef",
          "ReportInvoiceCustomerDimensionalAnalysesJR|dateToRef");
      String strDateFromRef2 = vars.getRequestGlobalVariable("inpDateFromRef2",
          "ReportInvoiceCustomerDimensionalAnalysesJR|dateFromRef2");
      String strDateToRef2 = vars.getRequestGlobalVariable("inpDateToRef2",
          "ReportInvoiceCustomerDimensionalAnalysesJR|dateToRef2");
      String strDateFromRef3 = vars.getRequestGlobalVariable("inpDateFromRef3",
          "ReportInvoiceCustomerDimensionalAnalysesJR|dateFromRef3");
      String strDateToRef3 = vars.getRequestGlobalVariable("inpDateToRef3",
          "ReportInvoiceCustomerDimensionalAnalysesJR|dateToRef3");
      String strPartnerGroup = vars.getRequestGlobalVariable("inpPartnerGroup",
          "ReportInvoiceCustomerDimensionalAnalysesJR|partnerGroup");
      String strcBpartnerId = vars.getRequestInGlobalVariable("inpcBPartnerId_IN",
          "ReportInvoiceCustomerDimensionalAnalysesJR|partner", IsIDFilter.instance);
      String strProductCategory = vars.getRequestGlobalVariable("inpProductCategory",
          "ReportInvoiceCustomerDimensionalAnalysesJR|productCategory");
      String strmProductId = vars.getRequestInGlobalVariable("inpmProductId_IN",
          "ReportInvoiceCustomerDimensionalAnalysesJR|product", IsIDFilter.instance);
      // ad_ref_list.value for refercence_id 800087
      String strNotShown = vars.getInStringParameter("inpNotShown", IsPositiveIntFilter.instance);
      String strShown = vars.getInStringParameter("inpShown", IsPositiveIntFilter.instance);
      String strOrg = vars.getRequestGlobalVariable("inpOrg",
          "ReportInvoiceCustomerDimensionalAnalysesJR|org");
      String strsalesrepId = vars.getRequestGlobalVariable("inpSalesrepId",
          "ReportInvoiceCustomerDimensionalAnalysesJR|salesrep");
      String strcProjectId = vars.getRequestGlobalVariable("inpcProjectId",
          "ReportInvoiceCustomerDimensionalAnalysesJR|project");
      String strProducttype = vars.getRequestGlobalVariable("inpProducttype",
          "ReportInvoiceCustomerDimensionalAnalysesJR|producttype");
      String strcDocTypeId = vars.getRequestInGlobalVariable("inpcDocTypeId_IN",
          "ReportInvoiceCustomerDimensionalAnalysesJR|doctype", IsIDFilter.instance);
      String strOrder = vars.getRequestGlobalVariable("inpOrder",
          "ReportInvoiceCustomerDimensionalAnalysesJR|order");
      String strMayor = vars.getNumericParameter("inpMayor", "");
      String strMenor = vars.getNumericParameter("inpMenor", "");
      String strComparative = vars.getStringParameter("inpComparative", "N");
      String strPartnerSalesrepId = vars.getRequestGlobalVariable("inpPartnerSalesrepId",
          "ReportInvoiceCustomerDimensionalAnalysesJR|partnersalesrep");
      String strCurrencyId = vars.getGlobalVariable("inpCurrencyId",
          "ReportInvoiceCustomerDimensionalAnalysesJR|currency", strUserCurrencyId);
      String strVoid = vars.getRequestGlobalVariable("inpVoid",
          "ReportInvoiceCustomerDimensionalAnalysesJR|Void");
      printPageHtml(request, response, vars, strComparative, strDateFrom, strDateTo,
          strPartnerGroup, strcBpartnerId, strProductCategory, strmProductId, strNotShown,
          strShown, strDateFromRef, strDateToRef, strDateFromRef2, strDateToRef2, strDateFromRef3,
          strDateToRef3, strOrg, strsalesrepId, strcProjectId, strProducttype, strcDocTypeId,
          strOrder, strMayor, strMenor, strPartnerSalesrepId, strCurrencyId, strVoid, "html");
    } else if (vars.commandIn("EDIT_PDF", "EDIT_PDF_COMPARATIVE")) {
      String strDateFrom = vars.getRequestGlobalVariable("inpDateFrom",
          "ReportInvoiceCustomerDimensionalAnalysesJR|dateFrom");
      String strDateTo = vars.getRequestGlobalVariable("inpDateTo",
          "ReportInvoiceCustomerDimensionalAnalysesJR|dateTo");
      String strDateFromRef = vars.getRequestGlobalVariable("inpDateFromRef",
          "ReportInvoiceCustomerDimensionalAnalysesJR|dateFromRef");
      String strDateToRef = vars.getRequestGlobalVariable("inpDateToRef",
          "ReportInvoiceCustomerDimensionalAnalysesJR|dateToRef");
      String strDateFromRef2 = vars.getRequestGlobalVariable("inpDateFromRef2",
          "ReportInvoiceCustomerDimensionalAnalysesJR|dateFromRef2");
      String strDateToRef2 = vars.getRequestGlobalVariable("inpDateToRef2",
          "ReportInvoiceCustomerDimensionalAnalysesJR|dateToRef2");
      String strDateFromRef3 = vars.getRequestGlobalVariable("inpDateFromRef3",
          "ReportInvoiceCustomerDimensionalAnalysesJR|dateFromRef3");
      String strDateToRef3 = vars.getRequestGlobalVariable("inpDateToRef3",
          "ReportInvoiceCustomerDimensionalAnalysesJR|dateToRef3");
      String strPartnerGroup = vars.getRequestGlobalVariable("inpPartnerGroup",
          "ReportInvoiceCustomerDimensionalAnalysesJR|partnerGroup");
      String strcBpartnerId = vars.getRequestInGlobalVariable("inpcBPartnerId_IN",
          "ReportInvoiceCustomerDimensionalAnalysesJR|partner", IsIDFilter.instance);
      String strProductCategory = vars.getRequestGlobalVariable("inpProductCategory",
          "ReportInvoiceCustomerDimensionalAnalysesJR|productCategory");
      String strmProductId = vars.getRequestInGlobalVariable("inpmProductId_IN",
          "ReportInvoiceCustomerDimensionalAnalysesJR|product", IsIDFilter.instance);
      // ad_ref_list.value for refercence_id 800087
      String strNotShown = vars.getInStringParameter("inpNotShown", IsPositiveIntFilter.instance);
      String strShown = vars.getInStringParameter("inpShown", IsPositiveIntFilter.instance);
      String strOrg = vars.getRequestGlobalVariable("inpOrg",
          "ReportInvoiceCustomerDimensionalAnalysesJR|org");
      String strsalesrepId = vars.getRequestGlobalVariable("inpSalesrepId",
          "ReportInvoiceCustomerDimensionalAnalysesJR|salesrep");
      String strcProjectId = vars.getRequestGlobalVariable("inpcProjectId",
          "ReportInvoiceCustomerDimensionalAnalysesJR|project");
      String strProducttype = vars.getRequestGlobalVariable("inpProducttype",
          "ReportInvoiceCustomerDimensionalAnalysesJR|producttype");
      String strcDocTypeId = vars.getRequestInGlobalVariable("inpcDocTypeId_IN",
          "ReportInvoiceCustomerDimensionalAnalysesJR|doctype", IsIDFilter.instance);
      String strOrder = vars.getRequestGlobalVariable("inpOrder",
          "ReportSalesDimensionalAnalyze|order");
      String strMayor = vars.getNumericParameter("inpMayor", "");
      String strMenor = vars.getNumericParameter("inpMenor", "");
      String strComparative = vars.getStringParameter("inpComparative", "N");
      String strPartnerSalesrepId = vars.getRequestGlobalVariable("inpPartnerSalesrepId",
          "ReportInvoiceCustomerDimensionalAnalysesJR|partnersalesrep");
      String strCurrencyId = vars.getGlobalVariable("inpCurrencyId",
          "ReportInvoiceCustomerDimensionalAnalysesJR|currency", strUserCurrencyId);
      String strVoid = vars.getRequestGlobalVariable("inpVoid",
          "ReportInvoiceCustomerDimensionalAnalysesJR|Void");
      printPageHtml(request, response, vars, strComparative, strDateFrom, strDateTo,
          strPartnerGroup, strcBpartnerId, strProductCategory, strmProductId, strNotShown,
          strShown, strDateFromRef, strDateToRef, strDateFromRef2, strDateToRef2, strDateFromRef3,
          strDateToRef3, strOrg, strsalesrepId, strcProjectId, strProducttype, strcDocTypeId,
          strOrder, strMayor, strMenor, strPartnerSalesrepId, strCurrencyId, strVoid, "pdf");
    } else if (vars.commandIn("EXCEL")) {
      String strDateFrom = vars.getRequestGlobalVariable("inpDateFrom",
          "ReportInvoiceCustomerDimensionalAnalysesJR|dateFrom");
      String strDateTo = vars.getRequestGlobalVariable("inpDateTo",
          "ReportInvoiceCustomerDimensionalAnalysesJR|dateTo");
      String strDateFromRef = vars.getRequestGlobalVariable("inpDateFromRef",
          "ReportInvoiceCustomerDimensionalAnalysesJR|dateFromRef");
      String strDateToRef = vars.getRequestGlobalVariable("inpDateToRef",
          "ReportInvoiceCustomerDimensionalAnalysesJR|dateToRef");
      String strDateFromRef2 = vars.getRequestGlobalVariable("inpDateFromRef2",
          "ReportInvoiceCustomerDimensionalAnalysesJR|dateFromRef2");
      String strDateToRef2 = vars.getRequestGlobalVariable("inpDateToRef2",
          "ReportInvoiceCustomerDimensionalAnalysesJR|dateToRef2");
      String strDateFromRef3 = vars.getRequestGlobalVariable("inpDateFromRef3",
          "ReportInvoiceCustomerDimensionalAnalysesJR|dateFromRef3");
      String strDateToRef3 = vars.getRequestGlobalVariable("inpDateToRef3",
          "ReportInvoiceCustomerDimensionalAnalysesJR|dateToRef3");
      String strPartnerGroup = vars.getRequestGlobalVariable("inpPartnerGroup",
          "ReportInvoiceCustomerDimensionalAnalysesJR|partnerGroup");
      String strcBpartnerId = vars.getRequestInGlobalVariable("inpcBPartnerId_IN",
          "ReportInvoiceCustomerDimensionalAnalysesJR|partner", IsIDFilter.instance);
      String strProductCategory = vars.getRequestGlobalVariable("inpProductCategory",
          "ReportInvoiceCustomerDimensionalAnalysesJR|productCategory");
      String strmProductId = vars.getRequestInGlobalVariable("inpmProductId_IN",
          "ReportInvoiceCustomerDimensionalAnalysesJR|product", IsIDFilter.instance);
      // ad_ref_list.value for refercence_id 800087
      String strNotShown = vars.getInStringParameter("inpNotShown", IsPositiveIntFilter.instance);
      String strShown = vars.getInStringParameter("inpShown", IsPositiveIntFilter.instance);
      String strOrg = vars.getRequestGlobalVariable("inpOrg",
          "ReportInvoiceCustomerDimensionalAnalysesJR|org");
      String strsalesrepId = vars.getRequestGlobalVariable("inpSalesrepId",
          "ReportInvoiceCustomerDimensionalAnalysesJR|salesrep");
      String strcProjectId = vars.getRequestGlobalVariable("inpcProjectId",
          "ReportInvoiceCustomerDimensionalAnalysesJR|project");
      String strProducttype = vars.getRequestGlobalVariable("inpProducttype",
          "ReportInvoiceCustomerDimensionalAnalysesJR|producttype");
      String strcDocTypeId = vars.getRequestInGlobalVariable("inpcDocTypeId_IN",
          "ReportInvoiceCustomerDimensionalAnalysesJR|doctype", IsIDFilter.instance);
      String strOrder = vars.getRequestGlobalVariable("inpOrder",
          "ReportSalesDimensionalAnalyze|order");
      String strMayor = vars.getNumericParameter("inpMayor", "");
      String strMenor = vars.getNumericParameter("inpMenor", "");
      String strComparative = vars.getStringParameter("inpComparative", "N");
      String strPartnerSalesrepId = vars.getRequestGlobalVariable("inpPartnerSalesrepId",
          "ReportInvoiceCustomerDimensionalAnalysesJR|partnersalesrep");
      String strCurrencyId = vars.getGlobalVariable("inpCurrencyId",
          "ReportInvoiceCustomerDimensionalAnalysesJR|currency", strUserCurrencyId);
      String strVoid = vars.getRequestGlobalVariable("inpVoid",
          "ReportInvoiceCustomerDimensionalAnalysesJR|Void");
      printPageHtml(request, response, vars, strComparative, strDateFrom, strDateTo,
          strPartnerGroup, strcBpartnerId, strProductCategory, strmProductId, strNotShown,
          strShown, strDateFromRef, strDateToRef, strDateFromRef2, strDateToRef2, strDateFromRef3,
          strDateToRef3, strOrg, strsalesrepId, strcProjectId, strProducttype, strcDocTypeId,
          strOrder, strMayor, strMenor, strPartnerSalesrepId, strCurrencyId, strVoid, "xls");
    } else if (vars.commandIn("CUR")) {
      String orgId = vars.getStringParameter("inpOrg");
      String strOrgCurrencyId = OBCurrencyUtils.getOrgCurrency(orgId);
      if (StringUtils.isEmpty(strOrgCurrencyId)) {
        strOrgCurrencyId = strUserCurrencyId;
      }
      response.setContentType("text/html; charset=UTF-8");
      PrintWriter out = response.getWriter();
      out.print(strOrgCurrencyId);
      out.close();
    } else
      pageErrorPopUp(response);
  }

  private void printPageDataSheet(HttpServletRequest request, HttpServletResponse response,
      VariablesSecureApp vars, String strComparative, String strDateFrom, String strDateTo,
      String strPartnerGroup, String strcBpartnerId, String strProductCategory,
      String strmProductId, String strNotShown, String strShown, String strDateFromRef,
      String strDateToRef, String strDateFromRef2, String strDateToRef2, String strDateFromRef3,
      String strDateToRef3, String strOrg, String strsalesrepId, String strcProjectId,
      String strProducttype, String strcDocTypeId, String strOrder, String strMayor,
      String strMenor, String strPartnerSalesrepId, String strCurrencyId, String strVoid)
      throws IOException, ServletException {
    if (log4j.isDebugEnabled())
      log4j.debug("Output: dataSheet");
    String discard[] = { "selEliminarHeader1" };
    if (strComparative.equals("Y")) {
      discard[0] = "selEliminarHeader2";
    }
    XmlDocument xmlDocument = null;
    xmlDocument = xmlEngine.readXmlTemplate(
        "org/openbravo/erpCommon/ad_reports/ReportInvoiceCustomerDimensionalAnalysesJRFilter",
        discard).createXmlDocument();

    ToolBar toolbar = new ToolBar(this, vars.getLanguage(),
        "ReportInvoiceCustomerDimensionalAnalysesJRFilter", false, "", "", "", false, "ad_reports",
        strReplaceWith, false, true);
    toolbar.prepareSimpleToolBarTemplate();
    xmlDocument.setParameter("toolbar", toolbar.toString());
    try {
      WindowTabs tabs = new WindowTabs(this, vars,
          "org.openbravo.erpCommon.ad_reports.ReportInvoiceCustomerDimensionalAnalysesJR");
      xmlDocument.setParameter("parentTabContainer", tabs.parentTabs());
      xmlDocument.setParameter("mainTabContainer", tabs.mainTabs());
      xmlDocument.setParameter("childTabContainer", tabs.childTabs());
      xmlDocument.setParameter("theme", vars.getTheme());
      NavigationBar nav = new NavigationBar(this, vars.getLanguage(),
          "ReportInvoiceCustomerDimensionalAnalysesJR.html", classInfo.id, classInfo.type,
          strReplaceWith, tabs.breadcrumb());
      xmlDocument.setParameter("navigationBar", nav.toString());
      LeftTabsBar lBar = new LeftTabsBar(this, vars.getLanguage(),
          "ReportInvoiceCustomerDimensionalAnalysesJR.html", strReplaceWith);
      xmlDocument.setParameter("leftTabs", lBar.manualTemplate());
    } catch (Exception ex) {
      throw new ServletException(ex);
    }
    {
      OBError myMessage = vars.getMessage("ReportInvoiceCustomerDimensionalAnalysesJR");
      vars.removeMessage("ReportInvoiceCustomerDimensionalAnalysesJR");
      if (myMessage != null) {
        xmlDocument.setParameter("messageType", myMessage.getType());
        xmlDocument.setParameter("messageTitle", myMessage.getTitle());
        xmlDocument.setParameter("messageMessage", myMessage.getMessage());
      }
      if (CostingStatus.getInstance().isMigrated() == false) {
        advise(request, response, "ERROR",
            Utility.messageBD(this, "NotUsingNewCost", vars.getLanguage()), "");
        return;
      }
      if (!transactionCostDateAcctInitialized()) {
        advise(request, response, "ERROR",
            Utility.messageBD(this, "TransactionCostDateAcctNotInitilized", vars.getLanguage()), "");
        return;
      }
    }

    xmlDocument.setParameter("calendar", vars.getLanguage().substring(0, 2));
    xmlDocument.setParameter("language", "defaultLang=\"" + vars.getLanguage() + "\";");
    xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
    xmlDocument.setParameter("dateFrom", strDateFrom);
    xmlDocument.setParameter("dateFromdisplayFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("dateFromsaveFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("dateTo", strDateTo);
    xmlDocument.setParameter("dateTodisplayFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("dateTosaveFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("dateFromRef", strDateFromRef);
    xmlDocument.setParameter("dateFromRefdisplayFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("dateFromRefsaveFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("dateToRef", strDateToRef);
    xmlDocument.setParameter("dateToRefdisplayFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("dateToRefsaveFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("dateFromRef2", strDateFromRef2);
    xmlDocument
        .setParameter("dateFromRef2displayFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("dateFromRef2saveFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("dateToRef2", strDateToRef2);
    xmlDocument.setParameter("dateToRef2displayFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("dateToRef2saveFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("dateFromRef3", strDateFromRef3);
    xmlDocument
        .setParameter("dateFromRef3displayFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("dateFromRef3saveFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("dateToRef3", strDateToRef3);
    xmlDocument.setParameter("dateToRef3displayFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("dateToRef3saveFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("cBpGroupId", strPartnerGroup);
    xmlDocument.setParameter("mProductCategoryId", strProductCategory);
    xmlDocument.setParameter("adOrgId", strOrg);
    xmlDocument.setParameter("salesRepId", strsalesrepId);
    xmlDocument.setParameter("normal", strOrder);
    xmlDocument.setParameter("amountasc", strOrder);
    xmlDocument.setParameter("amountdesc", strOrder);
    xmlDocument.setParameter("mayor", strMayor);
    xmlDocument.setParameter("menor", strMenor);
    xmlDocument.setParameter("comparative", strComparative);
    xmlDocument.setParameter("void", strVoid);
    xmlDocument.setParameter("cProjectId", strcProjectId);
    xmlDocument.setParameter("producttype", strProducttype);
    xmlDocument.setParameter("partnerSalesRepId", strPartnerSalesrepId);
    xmlDocument.setParameter("projectName",
        ReportInvoiceCustomerDimensionalAnalysesJRData.selectProject(this, strcProjectId));
    try {
      ComboTableData comboTableData = new ComboTableData(vars, this, "TABLEDIR", "C_BP_Group_ID",
          "", "", Utility.getContext(this, vars, "#AccessibleOrgTree",
              "ReportInvoiceCustomerDimensionalAnalysesJR"), Utility.getContext(this, vars,
              "#User_Client", "ReportInvoiceCustomerDimensionalAnalysesJR"), 0);
      Utility.fillSQLParameters(this, vars, null, comboTableData,
          "ReportInvoiceCustomerDimensionalAnalysesJR", strPartnerGroup);
      xmlDocument.setData("reportC_BP_GROUPID", "liststructure", comboTableData.select(false));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

    try {
      ComboTableData comboTableData = new ComboTableData(vars, this, "TABLEDIR",
          "M_Product_Category_ID", "", "", Utility.getContext(this, vars, "#AccessibleOrgTree",
              "ReportInvoiceCustomerDimensionalAnalysesJR"), Utility.getContext(this, vars,
              "#User_Client", "ReportInvoiceCustomerDimensionalAnalysesJR"), 0);
      Utility.fillSQLParameters(this, vars, null, comboTableData,
          "ReportInvoiceCustomerDimensionalAnalysesJR", strProductCategory);
      xmlDocument.setData("reportM_PRODUCT_CATEGORYID", "liststructure",
          comboTableData.select(false));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

    try {
      ComboTableData comboTableData = new ComboTableData(vars, this, "TABLEDIR", "AD_Org_ID", "",
          "", Utility.getContext(this, vars, "#User_Org",
              "ReportInvoiceCustomerDimensionalAnalysesJR"), Utility.getContext(this, vars,
              "#User_Client", "ReportInvoiceCustomerDimensionalAnalysesJR"), 0);
      Utility.fillSQLParameters(this, vars, null, comboTableData,
          "ReportInvoiceCustomerDimensionalAnalysesJR", strOrg);
      xmlDocument.setData("reportAD_ORGID", "liststructure", comboTableData.select(false));
      comboTableData = null;

    } catch (Exception ex) {
      throw new ServletException(ex);
    }
    xmlDocument.setData(
        "reportCBPartnerId_IN",
        "liststructure",
        SelectorUtilityData.selectBpartner(this,
            Utility.getContext(this, vars, "#AccessibleOrgTree", ""),
            Utility.getContext(this, vars, "#User_Client", ""), strcBpartnerId));
    xmlDocument.setData(
        "reportMProductId_IN",
        "liststructure",
        SelectorUtilityData.selectMproduct(this,
            Utility.getContext(this, vars, "#AccessibleOrgTree", ""),
            Utility.getContext(this, vars, "#User_Client", ""), strmProductId));

    try {
      ComboTableData comboTableData = new ComboTableData(vars, this, "LIST", "",
          "M_Product_ProductType", "", Utility.getContext(this, vars, "#AccessibleOrgTree",
              "ReportInvoiceCustomerDimensionalAnalysesJR"), Utility.getContext(this, vars,
              "#User_Client", "ReportInvoiceCustomerDimensionalAnalysesJR"), 0);
      Utility.fillSQLParameters(this, vars, null, comboTableData,
          "ReportInvoiceCustomerDimensionalAnalysesJR", "");
      xmlDocument.setData("reportProductType", "liststructure", comboTableData.select(false));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

    try {
      ComboTableData comboTableData = new ComboTableData(vars, this, "TABLE", "SalesRep_ID",
          "AD_User SalesRep", "", Utility.getContext(this, vars, "#AccessibleOrgTree",
              "ReportSalesDimensionalAnalyzeJR"), Utility.getContext(this, vars, "#User_Client",
              "ReportSalesDimensionalAnalyzeJR"), 0);
      Utility.fillSQLParameters(this, vars, null, comboTableData,
          "ReportSalesDimensionalAnalyzeJR", strsalesrepId);
      xmlDocument.setData("reportSalesRep_ID", "liststructure", comboTableData.select(false));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

    try {
      ComboTableData comboTableData = new ComboTableData(vars, this, "TABLE", "",
          "C_BPartner SalesRep", "", Utility.getContext(this, vars, "#AccessibleOrgTree",
              "ReportInvoiceCustomerDimensionalAnalysesJR"), Utility.getContext(this, vars,
              "#User_Client", "ReportInvoiceCustomerDimensionalAnalysesJR"), 0);
      Utility.fillSQLParameters(this, vars, null, comboTableData,
          "ReportInvoiceCustomerDimensionalAnalysesJR", strPartnerSalesrepId);
      xmlDocument
          .setData("reportPartnerSalesRep_ID", "liststructure", comboTableData.select(false));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

    xmlDocument.setParameter("ccurrencyid", strCurrencyId);
    try {
      ComboTableData comboTableData = new ComboTableData(vars, this, "TABLEDIR", "C_Currency_ID",
          "", "", Utility.getContext(this, vars, "#AccessibleOrgTree",
              "ReportInvoiceCustomerDimensionalAnalysesJR"), Utility.getContext(this, vars,
              "#User_Client", "ReportInvoiceCustomerDimensionalAnalysesJR"), 0);
      Utility.fillSQLParameters(this, vars, null, comboTableData,
          "ReportInvoiceCustomerDimensionalAnalysesJR", strCurrencyId);
      xmlDocument.setData("reportC_Currency_ID", "liststructure", comboTableData.select(false));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

    xmlDocument.setData(
        "reportCDocTypeId_IN",
        "liststructure",
        SelectorUtilityData.selectCDocType(this,
            Utility.getContext(this, vars, "#AccessibleOrgTree", ""),
            Utility.getContext(this, vars, "#User_Client", ""), strcDocTypeId));

    if (vars.getLanguage().equals("en_US")) {
      xmlDocument.setData("structure1",
          ReportInvoiceCustomerDimensionalAnalysesJRData.selectNotShown(this, strShown));
      xmlDocument.setData("structure2",
          strShown.equals("") ? new ReportInvoiceCustomerDimensionalAnalysesJRData[0]
              : ReportInvoiceCustomerDimensionalAnalysesJRData.selectShown(this, strShown));
    } else {
      xmlDocument.setData("structure1", ReportInvoiceCustomerDimensionalAnalysesJRData
          .selectNotShownTrl(this, vars.getLanguage(), strShown));
      xmlDocument.setData(
          "structure2",
          strShown.equals("") ? new ReportInvoiceCustomerDimensionalAnalysesJRData[0]
              : ReportInvoiceCustomerDimensionalAnalysesJRData.selectShownTrl(this,
                  vars.getLanguage(), strShown));
    }

    response.setContentType("text/html; charset=UTF-8");
    PrintWriter out = response.getWriter();
    out.println(xmlDocument.print());
    out.close();
  }

  private void printPageHtml(HttpServletRequest request, HttpServletResponse response,
      VariablesSecureApp vars, String strComparative, String strDateFrom, String strDateTo,
      String strPartnerGroup, String strcBpartnerId, String strProductCategory,
      String strmProductId, String strNotShown, String strShown, String strDateFromRef,
      String strDateToRef, String strDateFromRef2, String strDateToRef2, String strDateFromRef3,
      String strDateToRef3, String strOrg, String strsalesrepId, String strcProjectId,
      String strProducttype, String strcDocTypeId, String strOrder, String strMayor,
      String strMenor, String strPartnerSalesrepId, String strCurrencyId, String strVoid,
      String strOutput) throws IOException, ServletException {
    if (log4j.isDebugEnabled())
      log4j.debug("Output: print html");
    String strOrderby = "";
    String[] discard = { "", "", "", "", "", "", "", "", "", "" };
    String[] discard1 = { "selEliminarBody1", "discard", "discard", "discard", "discard",
        "discard", "discard", "discard", "discard", "discard", "discard", "discard", "discard",
        "discard", "discard", "discard", "discard", "discard", "discard", "discard", "discard",
        "discard", "discard", "discard", "discard", "discard", "discard", "discard", "discard",
        "discard", "discard", "discard", "discard", "discard", "discard", "discard", "discard",
        "discard", "discard", "discard", "discard", "discard", "discard", "discard", "discard",
        "discard", "discard", "discard", "discard", "discard", "discard", "discard", "discard",
        "discard", "discard", "discard", "discard", "discard", "discard" };
    if (strOrg.equals(""))
      strOrg = vars.getOrg();
    boolean multiComparative2 = false;
    boolean multiComparative3 = false;
    if (strComparative.equals("Y")) {
      discard1[0] = "selEliminarBody2";
      if (StringUtils.isNotBlank(strDateFromRef2) && StringUtils.isNotBlank(strDateToRef2)) {
        multiComparative2 = true;
        if (StringUtils.isNotBlank(strDateFromRef3) && StringUtils.isNotBlank(strDateToRef3)) {
          multiComparative3 = true;
        } else {
          strDateFromRef3 = "";
          strDateToRef3 = "";
        }
      } else {
        strDateFromRef2 = "";
        strDateToRef2 = "";
        strDateFromRef3 = "";
        strDateToRef3 = "";
      }
    }
    String strTitle = "";
    strTitle = Utility.messageBD(this, "From", vars.getLanguage()) + " " + strDateFrom + " "
        + Utility.messageBD(this, "to", vars.getLanguage()) + " " + strDateTo;
    if (!strPartnerGroup.equals(""))
      strTitle = strTitle + ", " + Utility.messageBD(this, "ForBPartnerGroup", vars.getLanguage())
          + " "
          + ReportInvoiceCustomerDimensionalAnalysesJRData.selectBpgroup(this, strPartnerGroup);

    if (!strProductCategory.equals(""))
      strTitle = strTitle
          + ", "
          + Utility.messageBD(this, "ProductCategory", vars.getLanguage())
          + " "
          + ReportInvoiceCustomerDimensionalAnalysesJRData.selectProductCategory(this,
              strProductCategory);
    if (!strcProjectId.equals(""))
      strTitle = strTitle + ", " + Utility.messageBD(this, "Project", vars.getLanguage()) + " "
          + ReportInvoiceCustomerDimensionalAnalysesJRData.selectProject(this, strcProjectId);
    if (!strProducttype.equals(""))
      strTitle = strTitle
          + ", "
          + Utility.messageBD(this, "PRODUCTTYPE", vars.getLanguage())
          + " "
          + ReportInvoiceCustomerDimensionalAnalysesJRData.selectProducttype(this, "270",
              vars.getLanguage(), strProducttype);
    if (!strsalesrepId.equals(""))
      strTitle = strTitle + ", " + Utility.messageBD(this, "TheClientSalesRep", vars.getLanguage())
          + " "
          + ReportInvoiceCustomerDimensionalAnalysesJRData.selectSalesrep(this, strsalesrepId);
    if (!strPartnerSalesrepId.equals(""))
      strTitle = strTitle
          + " "
          + Utility.messageBD(this, "And", vars.getLanguage())
          + " "
          + Utility.messageBD(this, "TheClientSalesRep", vars.getLanguage())
          + " "
          + ReportInvoiceCustomerDimensionalAnalysesJRData.selectSalesrep(this,
              strPartnerSalesrepId);

    ReportInvoiceCustomerDimensionalAnalysesJRData[] data = null;
    ReportInvoiceCustomerDimensionalAnalysesJRData dataXLS = null;
    String[] strShownArray = { "", "", "", "", "", "", "", "", "", "" };
    if (strShown.startsWith("("))
      strShown = strShown.substring(1, strShown.length() - 1);
    if (!strShown.equals("")) {
      strShown = Replace.replace(strShown, "'", "");
      strShown = Replace.replace(strShown, " ", "");
      StringTokenizer st = new StringTokenizer(strShown, ",", false);
      int intContador = 0;
      while (st.hasMoreTokens()) {
        strShownArray[intContador] = st.nextToken();
        intContador++;
      }

    }

    ReportInvoiceCustomerDimensionalAnalysesJRData[] dimensionLabel = null;
    if (vars.getLanguage().equals("en_US")) {
      dimensionLabel = ReportInvoiceCustomerDimensionalAnalysesJRData.selectNotShown(this, "");
    } else {
      dimensionLabel = ReportInvoiceCustomerDimensionalAnalysesJRData.selectNotShownTrl(this,
          vars.getLanguage(), "");
    }

    // Checking report limit first
    StringBuffer levelsconcat = new StringBuffer();
    levelsconcat.append("''");
    String[] strLevelLabel = { "", "", "", "", "", "", "", "", "", "" };
    String[] strTextShow = { "", "", "", "", "", "", "", "", "", "" };
    int intOrder = 0;
    int intProductLevel = 11;
    int intAuxDiscard = -1;
    for (int i = 0; i < 10; i++) {
      if (strShownArray[i].equals("1")) {
        strTextShow[i] = "C_BP_GROUP.NAME";
        intOrder++;
        strLevelLabel[i] = dimensionLabel[0].name;
        levelsconcat.append(" || ");
        levelsconcat.append("C_BP_GROUP.C_BP_GROUP_ID");
      } else if (strShownArray[i].equals("2")) {
        strTextShow[i] = "AD_COLUMN_IDENTIFIER(to_char('C_Bpartner'), to_char( C_BPARTNER.C_BPARTNER_ID), to_char( '"
            + vars.getLanguage() + "'))";
        intOrder++;
        strLevelLabel[i] = dimensionLabel[1].name;
        levelsconcat.append(" || ");
        levelsconcat.append("C_BPARTNER.C_BPARTNER_ID");
      } else if (strShownArray[i].equals("3")) {
        strTextShow[i] = "M_PRODUCT_CATEGORY.NAME";
        intOrder++;
        strLevelLabel[i] = dimensionLabel[2].name;
        levelsconcat.append(" || ");
        levelsconcat.append("M_PRODUCT_CATEGORY.M_PRODUCT_CATEGORY_ID");
      } else if (strShownArray[i].equals("4")) {
        strTextShow[i] = "AD_COLUMN_IDENTIFIER(to_char('M_Product'), to_char( M_PRODUCT.M_PRODUCT_ID), to_char( '"
            + vars.getLanguage()
            + "'))|| CASE WHEN uomsymbol IS NULL THEN '' ELSE to_char(' ('||uomsymbol||')') END";
        intAuxDiscard = i;
        intOrder++;
        intProductLevel = i + 1;
        strLevelLabel[i] = dimensionLabel[3].name;
        levelsconcat.append(" || ");
        levelsconcat.append("M_PRODUCT.M_PRODUCT_ID");
      } else if (strShownArray[i].equals("5")) {
        strTextShow[i] = "C_INVOICE.DOCUMENTNO";
        intOrder++;
        strLevelLabel[i] = dimensionLabel[4].name;
        levelsconcat.append(" || ");
        levelsconcat.append("C_INVOICE.C_INVOICE_ID");
      } else if (strShownArray[i].equals("6")) {
        strTextShow[i] = "AD_USER.FIRSTNAME||' '||' '||AD_USER.LASTNAME";
        intOrder++;
        strLevelLabel[i] = dimensionLabel[5].name;
        levelsconcat.append(" || ");
        levelsconcat.append("AD_USER.AD_USER_ID");
      } else if (strShownArray[i].equals("8")) {
        strTextShow[i] = "AD_ORG.NAME";
        intOrder++;
        strLevelLabel[i] = dimensionLabel[6].name;
        levelsconcat.append(" || ");
        levelsconcat.append("AD_ORG.AD_ORG_ID");
      } else if (strShownArray[i].equals("9")) {
        strTextShow[i] = "CASE WHEN AD_USER.AD_USER_ID IS NOT NULL THEN AD_COLUMN_IDENTIFIER(to_char('Ad_User'), to_char( AD_USER.AD_USER_ID), to_char( '"
            + vars.getLanguage() + "')) ELSE '' END";
        intOrder++;
        strLevelLabel[i] = dimensionLabel[7].name;
        levelsconcat.append(" || ");
        levelsconcat.append("AD_USER.AD_USER_ID");
      } else if (strShownArray[i].equals("10")) {
        strTextShow[i] = "C_PROJECT.NAME";
        intOrder++;
        strLevelLabel[i] = dimensionLabel[8].name;
        levelsconcat.append(" || ");
        levelsconcat.append("C_PROJECT.C_PROJECT_ID");
      } else if (strShownArray[i].equals("11")) {
        strTextShow[i] = "AD_COLUMN_IDENTIFIER(to_char('C_Bpartner_Location'), to_char( M_INOUT.C_BPARTNER_LOCATION_ID), to_char( '"
            + vars.getLanguage() + "'))";
        intOrder++;
        strLevelLabel[i] = dimensionLabel[9].name;
        levelsconcat.append(" || ");
        levelsconcat.append("M_INOUT.C_BPARTNER_LOCATION_ID");
      } else {
        strTextShow[i] = "''";
        discard[i] = "display:none;";
      }
    }
    if (intOrder != 0 || intAuxDiscard != -1) {
      int k = 1;
      if (intOrder == 1) {
        strOrderby = " ORDER BY  NIVEL" + k + ",";
      } else {
        strOrderby = " ORDER BY ";
      }
      while (k < intOrder) {
        strOrderby = strOrderby + "NIVEL" + k + ",";
        k++;
      }
      if (k == 1) {
        if (strOrder.equals("Normal")) {
          strOrderby = " ORDER BY NIVEL" + k;
        } else if (strOrder.equals("Amountasc")) {
          strOrderby = " ORDER BY CONVAMOUNT ASC";
        } else if (strOrder.equals("Amountdesc")) {
          strOrderby = " ORDER BY CONVAMOUNT DESC";
        } else {
          strOrderby = "1";
        }
      } else {
        if (strOrder.equals("Normal")) {
          strOrderby += "NIVEL" + k;
        } else if (strOrder.equals("Amountasc")) {
          strOrderby += "CONVAMOUNT ASC";
        } else if (strOrder.equals("Amountdesc")) {
          strOrderby += "CONVAMOUNT DESC";
        } else {
          strOrderby = "1";
        }
      }

    } else {
      strOrderby = " ORDER BY 1";
    }
    String strHaving = "";
    if (!strMayor.equals("") && !strMenor.equals("")) {
      strHaving = " HAVING (SUM(CONVAMOUNT) >= " + strMayor + " AND SUM(CONVAMOUNT) <= " + strMenor
          + ")";
    } else if (!strMayor.equals("") && strMenor.equals("")) {
      strHaving = " HAVING (SUM(CONVAMOUNT) >= " + strMayor + ")";
    } else if (strMayor.equals("") && !strMenor.equals("")) {
      strHaving = " HAVING (SUM(CONVAMOUNT) <= " + strMenor + ")";
    }
    strOrderby = strHaving + strOrderby;

    int limit = 0;
    int mycount = 0;
    try {
      limit = Integer.parseInt(Utility.getPreference(vars, "ReportsLimit", ""));
      if (limit > 0) {
        mycount = Integer
            .parseInt((strComparative.equals("Y")) ? ReportInvoiceCustomerDimensionalAnalysesJRData
                .selectCount(this, levelsconcat.toString(), Tree.getMembers(this,
                    TreeData.getTreeOrg(this, vars.getClient()), strOrg), Utility.getContext(this,
                    vars, "#User_Client", "ReportInvoiceCustomerDimensionalAnalysesJR"),
                    strPartnerGroup, strcBpartnerId, strProductCategory, strmProductId,
                    strsalesrepId, strPartnerSalesrepId, strcProjectId, strProducttype,
                    strcDocTypeId, strVoid.equals("Y") ? "" : "VO", strDateFrom, DateTimeData
                        .nDaysAfter(this, strDateTo, "1"), strDateFromRef, DateTimeData.nDaysAfter(
                        this, strDateToRef, "1"), strDateFromRef2, DateTimeData.nDaysAfter(this,
                        strDateToRef2, "1"), strDateFromRef3, DateTimeData.nDaysAfter(this,
                        strDateToRef3, "1")) : ReportInvoiceCustomerDimensionalAnalysesJRData
                .selectNoComparativeCount(this, levelsconcat.toString(), Tree.getMembers(this,
                    TreeData.getTreeOrg(this, vars.getClient()), strOrg), Utility.getContext(this,
                    vars, "#User_Client", "ReportInvoiceCustomerDimensionalAnalysesJR"),
                    strPartnerGroup, strcBpartnerId, strProductCategory, strmProductId,
                    strsalesrepId, strPartnerSalesrepId, strcProjectId, strProducttype,
                    strcDocTypeId, strVoid.equals("Y") ? "" : "VO", strDateFrom, DateTimeData
                        .nDaysAfter(this, strDateTo, "1")));
      }
    } catch (NumberFormatException e) {
    }

    if (limit > 0 && mycount > limit) {
      String msgbody = Utility.messageBD(this, "ReportsLimitBody", vars.getLanguage());
      msgbody = msgbody.replace("@rows@", Integer.toString(mycount));
      msgbody = msgbody.replace("@limit@", Integer.toString(limit));
      advisePopUp(request, response, "ERROR",
          Utility.messageBD(this, "ReportsLimitHeader", vars.getLanguage()), msgbody);
    } else {
      // Checks if there is a conversion rate for each of the transactions of the report
      String strConvRateErrorMsg = "";
      OBError myMessage = null;
      myMessage = new OBError();
      if ("xls".equals(strOutput)) {
        try {
          dataXLS = ReportInvoiceCustomerDimensionalAnalysesJRData.selectXLS(this, strCurrencyId,
              Tree.getMembers(this, TreeData.getTreeOrg(this, vars.getClient()), strOrg), Utility
                  .getContext(this, vars, "#User_Client",
                      "ReportInvoiceCustomerDimensionalAnalysesJR"), strDateFrom, DateTimeData
                  .nDaysAfter(this, strDateTo, "1"), strPartnerGroup, strcBpartnerId,
              strProductCategory, strmProductId, strsalesrepId, strPartnerSalesrepId,
              strcProjectId, strProducttype, strcDocTypeId, strVoid.equals("Y") ? "" : "VO");
        } catch (ServletException ex) {
          myMessage = Utility.translateError(this, vars, vars.getLanguage(), ex.getMessage());
        }
      } else {
        if (strComparative.equals("Y")) {
          try {
            if (multiComparative2) {
              if (multiComparative3) {
                // Multi-comparative B: 1 base date, 3 reference dates
                data = ReportInvoiceCustomerDimensionalAnalysesJRData.select3(this, strCurrencyId,
                    strTextShow[0], strTextShow[1], strTextShow[2], strTextShow[3], strTextShow[4],
                    strTextShow[5], strTextShow[6], strTextShow[7], strTextShow[8], strTextShow[9],
                    Tree.getMembers(this, TreeData.getTreeOrg(this, vars.getClient()), strOrg),
                    Utility.getContext(this, vars, "#User_Client",
                        "ReportInvoiceCustomerDimensionalAnalysesJR"), strDateFrom, DateTimeData
                        .nDaysAfter(this, strDateTo, "1"), strPartnerGroup, strcBpartnerId,
                    strProductCategory, strmProductId, strsalesrepId, strPartnerSalesrepId,
                    strcProjectId, strProducttype, strcDocTypeId, strVoid.equals("Y") ? "" : "VO",
                    strDateFromRef, DateTimeData.nDaysAfter(this, strDateToRef, "1"),
                    strDateFromRef2, DateTimeData.nDaysAfter(this, strDateToRef2, "1"),
                    strDateFromRef3, DateTimeData.nDaysAfter(this, strDateToRef3, "1"), strOrderby);
              } else {
                // Multi-comparative A: 1 base date, 2 reference dates
                data = ReportInvoiceCustomerDimensionalAnalysesJRData.select2(this, strCurrencyId,
                    strTextShow[0], strTextShow[1], strTextShow[2], strTextShow[3], strTextShow[4],
                    strTextShow[5], strTextShow[6], strTextShow[7], strTextShow[8], strTextShow[9],
                    Tree.getMembers(this, TreeData.getTreeOrg(this, vars.getClient()), strOrg),
                    Utility.getContext(this, vars, "#User_Client",
                        "ReportInvoiceCustomerDimensionalAnalysesJR"), strDateFrom, DateTimeData
                        .nDaysAfter(this, strDateTo, "1"), strPartnerGroup, strcBpartnerId,
                    strProductCategory, strmProductId, strsalesrepId, strPartnerSalesrepId,
                    strcProjectId, strProducttype, strcDocTypeId, strVoid.equals("Y") ? "" : "VO",
                    strDateFromRef, DateTimeData.nDaysAfter(this, strDateToRef, "1"),
                    strDateFromRef2, DateTimeData.nDaysAfter(this, strDateToRef2, "1"), strOrderby);
              }
            } else {
              // Regular comparative: 1 base date, 1 reference date
              data = ReportInvoiceCustomerDimensionalAnalysesJRData.select(this, strCurrencyId,
                  strTextShow[0], strTextShow[1], strTextShow[2], strTextShow[3], strTextShow[4],
                  strTextShow[5], strTextShow[6], strTextShow[7], strTextShow[8], strTextShow[9],
                  Tree.getMembers(this, TreeData.getTreeOrg(this, vars.getClient()), strOrg),
                  Utility.getContext(this, vars, "#User_Client",
                      "ReportInvoiceCustomerDimensionalAnalysesJR"), strDateFrom, DateTimeData
                      .nDaysAfter(this, strDateTo, "1"), strPartnerGroup, strcBpartnerId,
                  strProductCategory, strmProductId, strsalesrepId, strPartnerSalesrepId,
                  strcProjectId, strProducttype, strcDocTypeId, strVoid.equals("Y") ? "" : "VO",
                  strDateFromRef, DateTimeData.nDaysAfter(this, strDateToRef, "1"), strOrderby);
            }
          } catch (ServletException ex) {
            myMessage = Utility.translateError(this, vars, vars.getLanguage(), ex.getMessage());
          }
        } else {
          try {
            data = ReportInvoiceCustomerDimensionalAnalysesJRData.selectNoComparative(this,
                strCurrencyId, strTextShow[0], strTextShow[1], strTextShow[2], strTextShow[3],
                strTextShow[4], strTextShow[5], strTextShow[6], strTextShow[7], strTextShow[8],
                strTextShow[9], Tree.getMembers(this, TreeData.getTreeOrg(this, vars.getClient()),
                    strOrg), Utility.getContext(this, vars, "#User_Client",
                    "ReportInvoiceCustomerDimensionalAnalysesJR"), strDateFrom, DateTimeData
                    .nDaysAfter(this, strDateTo, "1"), strPartnerGroup, strcBpartnerId,
                strProductCategory, strmProductId, strsalesrepId, strPartnerSalesrepId,
                strcProjectId, strProducttype, strcDocTypeId, strVoid.equals("Y") ? "" : "VO",
                strOrderby);
          } catch (ServletException ex) {
            myMessage = Utility.translateError(this, vars, vars.getLanguage(), ex.getMessage());
          }
        }
      }
      strConvRateErrorMsg = myMessage.getMessage();
      // If a conversion rate is missing for a certain transaction, an error message window pops-up.
      if (!strConvRateErrorMsg.equals("") && strConvRateErrorMsg != null) {
        advisePopUp(request, response, "ERROR",
            Utility.messageBD(this, "NoConversionRateHeader", vars.getLanguage()),
            strConvRateErrorMsg);
      } else {
        // Otherwise, the report is launched
        if ("xls".equals(strOutput)) {
          try {
            if (!dataXLS.hasData()) {
              advisePopUp(request, response, "WARNING",
                  Utility.messageBD(this, "ProcessStatus-W", vars.getLanguage()),
                  Utility.messageBD(this, "NoDataFound", vars.getLanguage()));
            } else {
              /*
               * int rowLimit = 65532; ScrollableFieldProvider limitedData = new
               * LimitRowsScrollableFieldProviderFilter( dataXLS, rowLimit);
               */
              String strReportName = "@basedesign@/org/openbravo/erpCommon/ad_reports/ReportInvoiceCustomerDimensionalAnalysesXLS.jrxml";

              HashMap<String, Object> parameters = new HashMap<String, Object>();

              String strDateFormat;
              strDateFormat = vars.getJavaDateFormat();
              parameters.put("strDateFormat", strDateFormat);

              renderJR(vars, response, strReportName, null, "xls", parameters, dataXLS, null);
            }
          } finally {
            if (dataXLS != null) {
              dataXLS.close();
            }
          }
        } else {
          String strReportPath;
          if (strComparative.equals("Y")) {
            strReportPath = "@basedesign@/org/openbravo/erpCommon/ad_reports/ReportInvoiceCustomerDimensionalAnalysesComparativeJR.jrxml";
            if (multiComparative2) {
              strReportPath = "@basedesign@/org/openbravo/erpCommon/ad_reports/ReportInvoiceCustomerDimensionalAnalysesMultiComparativeJR.jrxml";
            }
            if (multiComparative3) {
              strReportPath = "@basedesign@/org/openbravo/erpCommon/ad_reports/ReportInvoiceCustomerDimensionalAnalysesMultiComparativeExtendedJR.jrxml";
            }
          } else {
            strReportPath = "@basedesign@/org/openbravo/erpCommon/ad_reports/ReportInvoiceCustomerDimensionalAnalysesNoComparativeJR.jrxml";
          }
          if (data == null || data.length == 0) {
            advisePopUp(request, response, "WARNING",
                Utility.messageBD(this, "ProcessStatus-W", vars.getLanguage()),
                Utility.messageBD(this, "NoDataFound", vars.getLanguage()));
          } else {
            HashMap<String, Object> parameters = new HashMap<String, Object>();
            parameters.put("LEVEL1_LABEL", strLevelLabel[0]);
            parameters.put("LEVEL2_LABEL", strLevelLabel[1]);
            parameters.put("LEVEL3_LABEL", strLevelLabel[2]);
            parameters.put("LEVEL4_LABEL", strLevelLabel[3]);
            parameters.put("LEVEL5_LABEL", strLevelLabel[4]);
            parameters.put("LEVEL6_LABEL", strLevelLabel[5]);
            parameters.put("LEVEL7_LABEL", strLevelLabel[6]);
            parameters.put("LEVEL8_LABEL", strLevelLabel[7]);
            parameters.put("LEVEL9_LABEL", strLevelLabel[8]);
            parameters.put("LEVEL10_LABEL", strLevelLabel[9]);
            parameters.put("DIMENSIONS", new Integer(intOrder));
            parameters.put("REPORT_SUBTITLE", strTitle);
            parameters.put("PRODUCT_LEVEL", new Integer(intProductLevel));
            renderJR(vars, response, strReportPath, strOutput, parameters, data, null);
          }
        }
      }
    }
  }

  private boolean transactionCostDateAcctInitialized() {
    boolean transactionCostDateacctInitialized = false;
    Client client = OBDal.getInstance().get(Client.class, "0");
    Organization organization = OBDal.getInstance().get(Organization.class, "0");
    try {
      transactionCostDateacctInitialized = Preferences.getPreferenceValue(
          CostingBackground.TRANSACTION_COST_DATEACCT_INITIALIZED, false, client, organization,
          null, null, null).equals("Y");
    } catch (PropertyException e1) {
      transactionCostDateacctInitialized = false;
    }
    return transactionCostDateacctInitialized;
  }

  public String getServletInfo() {
    return "Servlet ReportInvoiceCustomerDimensionalAnalysesJR. This Servlet was made by Jon Alegría";
  } // end of getServletInfo() method
}
