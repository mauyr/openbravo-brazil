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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.costing.CostingBackground;
import org.openbravo.costing.CostingStatus;
import org.openbravo.costing.CostingUtils;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.erpCommon.businessUtility.WindowTabs;
import org.openbravo.erpCommon.utility.ComboTableData;
import org.openbravo.erpCommon.utility.DateTimeData;
import org.openbravo.erpCommon.utility.LeftTabsBar;
import org.openbravo.erpCommon.utility.NavigationBar;
import org.openbravo.erpCommon.utility.OBDateUtils;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.PropertyException;
import org.openbravo.erpCommon.utility.ToolBar;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Locator;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.materialmgmt.cost.CostingRule;
import org.openbravo.model.materialmgmt.transaction.MaterialTransaction;
import org.openbravo.xmlEngine.XmlDocument;

public class ReportValuationStock extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;

  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException,
      ServletException {
    VariablesSecureApp vars = new VariablesSecureApp(request);

    // Get user Client's base currency
    String strUserCurrencyId = Utility.stringBaseCurrencyId(this, vars.getClient());
    if (vars.commandIn("DEFAULT", "RELATION")) {
      String strDate = vars.getGlobalVariable("inpDate", "ReportValuationStock|Date",
          DateTimeData.today(this));
      String strWarehouse = vars.getGlobalVariable("inpmWarehouseId",
          "ReportValuationStock|Warehouse", "");
      String strCategoryProduct = vars.getGlobalVariable("inpCategoryProduct",
          "ReportValuationStock|CategoryProduct", "");
      String strCurrencyId = vars.getGlobalVariable("inpCurrencyId",
          "ReportValuationStock|currency", strUserCurrencyId);
      printPageDataSheet(request, response, vars, strDate, strWarehouse, strCategoryProduct,
          strCurrencyId);
    } else if (vars.commandIn("FIND")) {
      String strDate = vars.getGlobalVariable("inpDate", "ReportValuationStock|Date",
          DateTimeData.today(this));
      String strWarehouse = vars.getRequestGlobalVariable("inpmWarehouseId",
          "ReportValuationStock|Warehouse");
      String strCategoryProduct = vars.getRequestGlobalVariable("inpCategoryProduct",
          "ReportValuationStock|CategoryProduct");
      String strCurrencyId = vars.getGlobalVariable("inpCurrencyId",
          "ReportValuationStock|currency", strUserCurrencyId);
      printPageDataSheet(request, response, vars, strDate, strWarehouse, strCategoryProduct,
          strCurrencyId);
    } else
      pageError(response);
  }

  private void printPageDataSheet(HttpServletRequest request, HttpServletResponse response,
      VariablesSecureApp vars, String strDate, String strWarehouse, String strCategoryProduct,
      String strCurrencyId) throws IOException, ServletException {
    if (log4j.isDebugEnabled())
      log4j.debug("Output: dataSheet");
    response.setContentType("text/html; charset=UTF-8");
    PrintWriter out = response.getWriter();
    String discard[] = { "discard" };
    XmlDocument xmlDocument;

    // Checks if there is a conversion rate for each of the transactions of
    // the report
    ReportValuationStockData[] data = null;
    OBError myMessage = null;
    myMessage = new OBError();
    String strConvRateErrorMsg = "";
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
    if (vars.commandIn("FIND")) {
      try {
        Warehouse wh = OBDal.getInstance().get(Warehouse.class, strWarehouse);
        Organization legalEntity = OBContext.getOBContext()
            .getOrganizationStructureProvider(wh.getClient().getId())
            .getLegalEntity(wh.getOrganization());
        String strDateFrom = DateTimeData.nDaysAfter(this, strDate, "1");
        if (legalEntity == null) {
          advise(request, response, "ERROR",
              Utility.messageBD(this, "WarehouseNotInLE", vars.getLanguage()), "");
        } else {
          try {
            CostingRule costingRule = CostingUtils.getCostDimensionRule(legalEntity,
                OBDateUtils.getDate(strDate));
            strDateFrom = OBDateUtils.formatDateTime(CostingUtils
                .getCostingRuleStartingDate(costingRule));
          } catch (Exception exception) {
            advise(
                request,
                response,
                "ERROR",
                Utility.messageBD(this, "NoCostingRuleFoundForOrganizationAndDate",
                    vars.getLanguage()), "");
          }
        }
        data = ReportValuationStockData.select(this, vars.getLanguage(), strCurrencyId,
            (legalEntity == null) ? null : legalEntity.getId(),
            DateTimeData.nDaysAfter(this, strDate, "1"), strDateFrom, strWarehouse,
            strCategoryProduct);
        boolean hasTrxWithNoCost = hasTrxWithNoCost(strDate, strWarehouse, strCategoryProduct);
        if (hasTrxWithNoCost) {
          OBError warning = new OBError();
          warning.setType("Warning");
          warning.setTitle(OBMessageUtils.messageBD("Warning"));
          warning.setMessage(OBMessageUtils.messageBD("TrxWithNoCost"));
          vars.setMessage("ReportValuationStock", warning);
        }
      } catch (ServletException ex) {
        myMessage = Utility.translateError(this, vars, vars.getLanguage(), ex.getMessage());
      }
      strConvRateErrorMsg = myMessage.getMessage();
    }
    // If a conversion rate is missing for a certain transaction, an error
    // message window pops-up.
    if (!strConvRateErrorMsg.equals("") && strConvRateErrorMsg != null) {
      advise(request, response, "ERROR",
          Utility.messageBD(this, "NoConversionRateHeader", vars.getLanguage()),
          strConvRateErrorMsg);
    } else { // Otherwise, the report is launched
      if (data == null || data.length == 0) {
        data = ReportValuationStockData.set();
        discard[0] = "sectionCategoryProduct";
      }
      if (vars.commandIn("DEFAULT")) {
        discard[0] = "sectionCategoryProduct";
        xmlDocument = xmlEngine.readXmlTemplate(
            "org/openbravo/erpCommon/ad_reports/ReportValuationStock", discard).createXmlDocument();
        data = ReportValuationStockData.set();
      } else {
        xmlDocument = xmlEngine.readXmlTemplate(
            "org/openbravo/erpCommon/ad_reports/ReportValuationStock", discard).createXmlDocument();
      }

      ToolBar toolbar = new ToolBar(this, vars.getLanguage(), "ReportValuationStock", false, "",
          "", "", false, "ad_reports", strReplaceWith, false, true);
      toolbar.prepareSimpleToolBarTemplate();
      xmlDocument.setParameter("toolbar", toolbar.toString());

      try {
        WindowTabs tabs = new WindowTabs(this, vars,
            "org.openbravo.erpCommon.ad_reports.ReportValuationStock");
        xmlDocument.setParameter("parentTabContainer", tabs.parentTabs());
        xmlDocument.setParameter("mainTabContainer", tabs.mainTabs());
        xmlDocument.setParameter("childTabContainer", tabs.childTabs());
        xmlDocument.setParameter("theme", vars.getTheme());
        NavigationBar nav = new NavigationBar(this, vars.getLanguage(),
            "ReportValuationStock.html", classInfo.id, classInfo.type, strReplaceWith,
            tabs.breadcrumb());
        xmlDocument.setParameter("navigationBar", nav.toString());
        LeftTabsBar lBar = new LeftTabsBar(this, vars.getLanguage(), "ReportValuationStock.html",
            strReplaceWith);
        xmlDocument.setParameter("leftTabs", lBar.manualTemplate());
      } catch (Exception ex) {
        throw new ServletException(ex);
      }
      {
        myMessage = vars.getMessage("ReportValuationStock");
        vars.removeMessage("ReportValuationStock");
        if (myMessage != null) {
          xmlDocument.setParameter("messageType", myMessage.getType());
          xmlDocument.setParameter("messageTitle", myMessage.getTitle());
          xmlDocument.setParameter("messageMessage", myMessage.getMessage());
        }
      }

      xmlDocument.setParameter("calendar", vars.getLanguage().substring(0, 2));
      xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
      xmlDocument.setParameter("paramLanguage", "defaultLang=\"" + vars.getLanguage() + "\";");
      xmlDocument.setParameter("date", strDate);
      xmlDocument.setParameter("dateFromdisplayFormat", vars.getSessionValue("#AD_SqlDateFormat"));
      xmlDocument.setParameter("dateFromsaveFormat", vars.getSessionValue("#AD_SqlDateFormat"));
      xmlDocument.setParameter("mWarehouseId", strWarehouse);
      xmlDocument.setParameter("categoryProduct", strCategoryProduct);
      try {
        ComboTableData comboTableData = new ComboTableData(vars, this, "TABLE", "M_Warehouse_ID",
            "M_Warehouse of Client", "", Utility.getContext(this, vars, "#AccessibleOrgTree", ""),
            Utility.getContext(this, vars, "#User_Client", ""), 0);
        Utility.fillSQLParameters(this, vars, null, comboTableData, "", "");
        xmlDocument.setData("reportM_WAREHOUSEID", "liststructure", comboTableData.select(false));
        comboTableData = null;
      } catch (Exception ex) {
        throw new ServletException(ex);
      }
      try {
        ComboTableData comboTableData = new ComboTableData(vars, this, "TABLEDIR",
            "M_Product_Category_ID", "", "", Utility.getContext(this, vars, "#AccessibleOrgTree",
                ""), Utility.getContext(this, vars, "#User_Client", ""), 0);
        Utility.fillSQLParameters(this, vars, null, comboTableData, "", strCategoryProduct);
        xmlDocument.setData("reportM_PRODUCT_CATEGORYID", "liststructure",
            comboTableData.select(false));
        comboTableData = null;
      } catch (Exception ex) {
        throw new ServletException(ex);
      }

      xmlDocument.setParameter("ccurrencyid", strCurrencyId);
      try {
        ComboTableData comboTableData = new ComboTableData(vars, this, "TABLEDIR", "C_Currency_ID",
            "", "", Utility.getContext(this, vars, "#AccessibleOrgTree", "ReportValuationStock"),
            Utility.getContext(this, vars, "#User_Client", "ReportValuationStock"), 0);
        Utility.fillSQLParameters(this, vars, null, comboTableData, "ReportValuationStock",
            strCurrencyId);
        xmlDocument.setData("reportC_Currency_ID", "liststructure", comboTableData.select(false));
        comboTableData = null;
      } catch (Exception ex) {
        throw new ServletException(ex);
      }

      xmlDocument.setData("structure1", data);
      out.println(xmlDocument.print());
      out.close();
    }
  }

  private boolean hasTrxWithNoCost(String strDate, String strWarehouse, String strCategoryProduct) {
    StringBuffer where = new StringBuffer();
    where.append(" as trx");
    where.append(" join trx." + MaterialTransaction.PROPERTY_STORAGEBIN + " as loc");
    where.append(" join trx." + MaterialTransaction.PROPERTY_PRODUCT + " as p");
    where.append(" where trx." + MaterialTransaction.PROPERTY_MOVEMENTDATE + " < :maxDate");
    where.append("   and trx." + MaterialTransaction.PROPERTY_ISCOSTCALCULATED + " = false");
    where.append("   and loc." + Locator.PROPERTY_WAREHOUSE + ".id = :wh");
    where.append("   and p." + Product.PROPERTY_STOCKED + " = true");
    if (!"".equals(strCategoryProduct)) {
      where.append("   and p." + Product.PROPERTY_PRODUCTCATEGORY + ".id = :prodCategory");
    }

    OBQuery<MaterialTransaction> whereQry = OBDal.getInstance().createQuery(
        MaterialTransaction.class, where.toString());
    whereQry.setFilterOnReadableClients(false);
    whereQry.setFilterOnReadableOrganization(false);
    try {
      whereQry.setNamedParameter("maxDate",
          OBDateUtils.getDate(DateTimeData.nDaysAfter(this, strDate, "1")));
    } catch (Exception e) {
      // DoNothing parse exception not expected.
      log4j.error("error parsing date: " + strDate, e);
    }
    whereQry.setNamedParameter("wh", strWarehouse);
    if (!"".equals(strCategoryProduct)) {
      whereQry.setNamedParameter("prodCategory", strCategoryProduct);
    }
    whereQry.setMaxResult(1);
    return whereQry.uniqueResult() != null;
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
    return "Servlet ReportValuationStock. This Servlet was made by Pablo Sarobe";
  } // end of getServletInfo() method
}
