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
 * All portions are Copyright (C) 2001-2013 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.erpCommon.ad_callouts;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.security.OrganizationStructureProvider;
import org.openbravo.dal.service.OBDal;
import org.openbravo.data.FieldProvider;
import org.openbravo.erpCommon.businessUtility.PAttributeSet;
import org.openbravo.erpCommon.businessUtility.PAttributeSetData;
import org.openbravo.erpCommon.businessUtility.PriceAdjustment;
import org.openbravo.erpCommon.businessUtility.Tax;
import org.openbravo.erpCommon.utility.AccDefUtility;
import org.openbravo.erpCommon.utility.ComboTableData;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.plm.Product;
import org.openbravo.utils.FormatUtilities;
import org.openbravo.xmlEngine.XmlDocument;

public class SL_Invoice_Product extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;

  public void init(ServletConfig config) {
    super.init(config);
    boolHist = false;
  }

  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException,
      ServletException {
    VariablesSecureApp vars = new VariablesSecureApp(request);
    if (vars.commandIn("DEFAULT")) {
      String strChanged = vars.getStringParameter("inpLastFieldChanged");
      if (log4j.isDebugEnabled())
        log4j.debug("CHANGED: " + strChanged);
      String strUOM = vars.getStringParameter("inpmProductId_UOM");
      String strPriceList = vars.getNumericParameter("inpmProductId_PLIST");
      String strPriceStd = vars.getNumericParameter("inpmProductId_PSTD");
      String strPriceLimit = vars.getStringParameter("inpmProductId_PLIM");
      String strCurrency = vars.getStringParameter("inpmProductId_CURR");
      String strQty = vars.getNumericParameter("inpqtyinvoiced");

      String strMProductID = vars.getStringParameter("inpmProductId");
      String strADOrgID = vars.getStringParameter("inpadOrgId");
      String strCInvoiceID = vars.getStringParameter("inpcInvoiceId");
      String strWindowId = vars.getStringParameter("inpwindowId");
      String strIsSOTrx = Utility.getContext(this, vars, "isSOTrx", strWindowId);
      String strWharehouse = Utility.getContext(this, vars, "#M_Warehouse_ID", strWindowId);
      String strWarehouseOrg = SLOrderProductData.getWarehouseOrg(this, strWharehouse);
      String strWarehouseForOrg = "";
      final OrganizationStructureProvider osp = OBContext.getOBContext()
          .getOrganizationStructureProvider(vars.getClient());
      if (!strADOrgID.equals(strWarehouseOrg)) {
        Organization org = OBDal.getInstance().get(Organization.class, strADOrgID);
        if (strWarehouseOrg != null) {
          Organization warehouseOrg = OBDal.getInstance().get(Organization.class, strWarehouseOrg);
          if (!osp.isInNaturalTree(org, warehouseOrg) && !osp.isInNaturalTree(warehouseOrg, org))
            strWarehouseForOrg = SLOrderProductData.getWarehouseOfOrg(this, vars.getClient(),
                strADOrgID);
          if (!strWarehouseForOrg.equals(""))
            strWharehouse = strWarehouseForOrg;
        }
      }
      String strTabId = vars.getStringParameter("inpTabId");

      try {
        printPage(response, vars, strUOM, strPriceList, strPriceStd, strPriceLimit, strCurrency,
            strMProductID, strADOrgID, strCInvoiceID, strIsSOTrx, strWharehouse, strTabId, strQty);
      } catch (ServletException ex) {
        pageErrorCallOut(response);
      }
    } else
      pageError(response);
  }

  private void printPage(HttpServletResponse response, VariablesSecureApp vars, String strUOM,
      String strPriceList, String strPriceStd, String strPriceLimit, String strCurrency,
      String strMProductID, String strADOrgID, String strCInvoiceID, String strIsSOTrx,
      String strWharehouse, String strTabId, String strQty) throws IOException, ServletException {
    if (log4j.isDebugEnabled())
      log4j.debug("Output: dataSheet");
    XmlDocument xmlDocument = xmlEngine.readXmlTemplate(
        "org/openbravo/erpCommon/ad_callouts/CallOut").createXmlDocument();

    String strPriceActual = "";
    Invoice invoice = OBDal.getInstance().get(Invoice.class, strCInvoiceID);
    boolean priceIncludeTaxes = invoice.getPriceList().isPriceIncludesTax();
    if (!strMProductID.equals("")) {
      Product product = OBDal.getInstance().get(Product.class, strMProductID);
      strPriceActual = PriceAdjustment.calculatePriceActual(invoice, product,
          new BigDecimal(strQty), new BigDecimal((strPriceStd.equals("") ? "0" : strPriceStd)))
          .toString();
    }
    StringBuffer resultado = new StringBuffer();

    resultado.append("var calloutName='SL_Invoice_Product';\n\n");
    resultado.append("var respuesta = new Array(");
    resultado.append("new Array(\"inpcUomId\", \"" + strUOM + "\"),");
    if (priceIncludeTaxes) {
      resultado.append("new Array(\"inpgrossUnitPrice\", "
          + (strPriceActual.equals("") ? "0" : strPriceActual) + "),");
      resultado.append("new Array(\"inpgrosspricestd\", "
          + (strPriceStd.equals("") ? "0" : strPriceStd) + "),");
      resultado.append("new Array(\"inpgrosspricelist\", "
          + (strPriceList.equals("") ? "\"\"" : strPriceList) + "),");

    } else {
      resultado.append("new Array(\"inppricestd\", "
          + (strPriceStd.equals("") ? "\"\"" : strPriceStd) + "),");
      resultado.append("new Array(\"inppriceactual\", "
          + (strPriceActual.equals("") ? "\"\"" : strPriceActual) + "),");

    }

    resultado.append("new Array(\"inppricelist\", "
        + (strPriceList.equals("") ? "\"\"" : strPriceList) + "),");
    resultado.append("new Array(\"inppricelimit\", "
        + (strPriceLimit.equals("") ? "\"\"" : strPriceLimit) + "),");
    PAttributeSetData[] dataPAttr = PAttributeSetData.selectProductAttr(this, strMProductID);
    if (dataPAttr != null && dataPAttr.length > 0 && dataPAttr[0].attrsetvaluetype.equals("D")) {
      PAttributeSetData[] data2 = PAttributeSetData.select(this, dataPAttr[0].mAttributesetId);
      if (PAttributeSet.isInstanceAttributeSet(data2)) {
        resultado.append("new Array(\"inpmAttributesetinstanceId\", \"\"),");
        resultado.append("new Array(\"inpmAttributesetinstanceId_R\", \"\"),");
      } else {
        resultado.append("new Array(\"inpmAttributesetinstanceId\", \""
            + dataPAttr[0].mAttributesetinstanceId + "\"),");
        resultado.append("new Array(\"inpmAttributesetinstanceId_R\", \""
            + FormatUtilities.replaceJS(dataPAttr[0].description) + "\"),");
      }
      resultado.append("new Array(\"inpattributeset\", \""
          + FormatUtilities.replaceJS(dataPAttr[0].mAttributesetId) + "\"),\n");
      resultado.append("new Array(\"inpattrsetvaluetype\", \""
          + FormatUtilities.replaceJS(dataPAttr[0].attrsetvaluetype) + "\"),\n");
    } else {
      resultado.append("new Array(\"inpmAttributesetinstanceId\", \"\"),");
      resultado.append("new Array(\"inpmAttributesetinstanceId_R\", \"\"),");
      resultado.append("new Array(\"inpattributeset\", \"\"),\n");
      resultado.append("new Array(\"inpattrsetvaluetype\", \"\"),\n");
    }
    String strHasSecondaryUOM = SLOrderProductData.hasSecondaryUOM(this, strMProductID);
    resultado.append("new Array(\"inphasseconduom\", " + strHasSecondaryUOM + ")");
    if (!"".equals(strCurrency)) {
      resultado.append(",\n new Array(\"inpcCurrencyId\", " + strCurrency + ")");
    }

    SLInvoiceTaxData[] data = SLInvoiceTaxData.select(this, strCInvoiceID);
    if (data != null && data.length > 0) {
      String strCTaxID = Tax.get(this, strMProductID, data[0].dateinvoiced, strADOrgID,
          strWharehouse, data[0].cBpartnerLocationId, data[0].cBpartnerLocationId,
          data[0].cProjectId, strIsSOTrx.equals("Y"), "Y".equals(data[0].iscashvat));

      resultado.append(", new Array(\"inpcTaxId\", \"" + strCTaxID + "\")");
    }
    if (!"".equals(strCInvoiceID)) {
      Invoice inv = OBDal.getInstance().get(Invoice.class, strCInvoiceID);
      final String CURRENT_MONTH = "C";
      final String NEXT_MONTH = "N";
      // Set empty values
      String isdeferred = "N";
      String defplantype = "";
      String periodnumber = "";
      String cPeriodId = "";
      try {
        if (inv.isSalesTransaction() && !"".equals(strMProductID)) {
          Product product = OBDal.getInstance().get(Product.class, strMProductID);
          if (product.isDeferredRevenue()) {
            isdeferred = "Y";
            defplantype = product.getRevenuePlanType();
            periodnumber = product.getPeriodNumber().toString();
            if (CURRENT_MONTH.equals(product.getDefaultPeriod())) {
              cPeriodId = AccDefUtility.getCurrentPeriod(inv.getAccountingDate(),
                  AccDefUtility.getCalendar(inv.getOrganization())).getId();
            } else if (NEXT_MONTH.equals(product.getDefaultPeriod())) {
              cPeriodId = AccDefUtility.getNextPeriod(
                  AccDefUtility.getCurrentPeriod(inv.getAccountingDate(),
                      AccDefUtility.getCalendar(inv.getOrganization()))).getId();
            }
          }
        } else if (!inv.isSalesTransaction() && !"".equals(strMProductID)) {
          Product product = OBDal.getInstance().get(Product.class, strMProductID);
          if (product.isDeferredexpense()) {
            isdeferred = "Y";
            defplantype = product.getExpplantype();
            periodnumber = product.getPeriodnumberExp().toString();
            if (CURRENT_MONTH.equals(product.getDefaultPeriodExpense())) {
              cPeriodId = AccDefUtility.getCurrentPeriod(inv.getAccountingDate(),
                  AccDefUtility.getCalendar(inv.getOrganization())).getId();
            } else if (NEXT_MONTH.equals(product.getDefaultPeriodExpense())) {
              cPeriodId = AccDefUtility.getNextPeriod(
                  AccDefUtility.getCurrentPeriod(inv.getAccountingDate(),
                      AccDefUtility.getCalendar(inv.getOrganization()))).getId();
            }
          }
        }
      } catch (Exception e) {
        isdeferred = "N";
        defplantype = "";
        periodnumber = "";
        cPeriodId = "";
        log4j.error("Error calculating Accruals and Deferrals Plan");
      }
      // Set values
      resultado.append(", new Array(\"inpisdeferred\", \"" + isdeferred + "\")");
      resultado.append(", new Array(\"inpdefplantype\", \"" + defplantype + "\")");
      resultado.append(", new Array(\"inpperiodnumber\", \"" + periodnumber + "\")");
      resultado.append(", new Array(\"inpcPeriodId\", \"" + cPeriodId + "\")");

    }
    resultado.append(", new Array(\"inpmProductUomId\", ");
    // if (strUOM.startsWith("\""))
    // strUOM=strUOM.substring(1,strUOM.length()-1);
    // String strmProductUOMId =
    // SLOrderProductData.strMProductUOMID(this,strMProductID,strUOM);

    FieldProvider[] tld = null;
    try {
      ComboTableData comboTableData = new ComboTableData(vars, this, "TABLE", "", "M_Product_UOM",
          "", Utility.getContext(this, vars, "#AccessibleOrgTree", "SLOrderProduct"),
          Utility.getContext(this, vars, "#User_Client", "SLOrderProduct"), 0);
      Utility.fillSQLParameters(this, vars, null, comboTableData, "SLOrderProduct", "");
      tld = comboTableData.select(false);
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

    if (tld != null && tld.length > 0) {
      resultado.append("new Array(");
      for (int i = 0; i < tld.length; i++) {
        resultado.append("new Array(\"" + tld[i].getField("id") + "\", \""
            + FormatUtilities.replaceJS(tld[i].getField("name")) + "\", \"false\")");
        if (i < tld.length - 1)
          resultado.append(",\n");
      }
      resultado.append("\n)");
    } else
      resultado.append("null");
    resultado.append("\n),");
    resultado.append("new Array(\"EXECUTE\", \"displayLogic();\")\n");

    resultado.append(");");
    xmlDocument.setParameter("array", resultado.toString());
    xmlDocument.setParameter("frameName", "appFrame");
    response.setContentType("text/html; charset=UTF-8");
    PrintWriter out = response.getWriter();
    out.println(xmlDocument.print());
    out.close();
  }

}
