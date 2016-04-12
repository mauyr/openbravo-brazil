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
import org.openbravo.dal.service.OBDal;
import org.openbravo.data.FieldProvider;
import org.openbravo.erpCommon.businessUtility.PAttributeSet;
import org.openbravo.erpCommon.businessUtility.PAttributeSetData;
import org.openbravo.erpCommon.businessUtility.PriceAdjustment;
import org.openbravo.erpCommon.businessUtility.Tax;
import org.openbravo.erpCommon.utility.ComboTableData;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.plm.Product;
import org.openbravo.utils.FormatUtilities;
import org.openbravo.xmlEngine.XmlDocument;

public class SL_Order_Product extends HttpSecureAppServlet {
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
      log4j.debug("CHANGED: " + strChanged);
      String strUOM = vars.getStringParameter("inpmProductId_UOM");
      String strPriceList = vars.getNumericParameter("inpmProductId_PLIST");
      String strPriceStd = vars.getNumericParameter("inpmProductId_PSTD");
      String strPriceLimit = vars.getNumericParameter("inpmProductId_PLIM");
      String strCurrency = vars.getStringParameter("inpmProductId_CURR");
      String strQty = vars.getNumericParameter("inpqtyordered");

      String strMProductID = vars.getStringParameter("inpmProductId");
      String strCBPartnerLocationID = vars.getStringParameter("inpcBpartnerLocationId");
      String strADOrgID = vars.getStringParameter("inpadOrgId");
      String strMWarehouseID = vars.getStringParameter("inpmWarehouseId");
      String strCOrderId = vars.getStringParameter("inpcOrderId");
      String strWindowId = vars.getStringParameter("inpwindowId");
      String strIsSOTrx = Utility.getContext(this, vars, "isSOTrx", strWindowId);
      String cancelPriceAd = vars.getStringParameter("inpcancelpricead");

      try {
        printPage(response, vars, strUOM, strPriceList, strPriceStd, strPriceLimit, strCurrency,
            strMProductID, strCBPartnerLocationID, strADOrgID, strMWarehouseID, strCOrderId,
            strIsSOTrx, strQty, cancelPriceAd);
      } catch (ServletException ex) {
        pageErrorCallOut(response);
      }
    } else
      pageError(response);
  }

  private void printPage(HttpServletResponse response, VariablesSecureApp vars, String _strUOM,
      String strPriceList, String _strPriceStd, String _strPriceLimit, String strCurrency,
      String strMProductID, String strCBPartnerLocationID, String strADOrgID,
      String strMWarehouseID, String strCOrderId, String strIsSOTrx, String strQty,
      String cancelPriceAd) throws IOException, ServletException {
    log4j.debug("Output: dataSheet");
    XmlDocument xmlDocument = xmlEngine.readXmlTemplate(
        "org/openbravo/erpCommon/ad_callouts/CallOut").createXmlDocument();

    String strPriceActual = "";
    String strHasSecondaryUOM = "";
    String strUOM = _strUOM;
    String strPriceLimit = _strPriceLimit;
    String strPriceStd = _strPriceStd;
    String strNetPriceList = strPriceList;
    String strGrossPriceList = strPriceList;
    String strGrossBaseUnitPrice = _strPriceStd;
    if (strPriceList.startsWith("\"")) {
      strNetPriceList = strPriceList.substring(1, strPriceList.length() - 1);
      strGrossPriceList = strPriceList.substring(1, strPriceList.length() - 1);
    }
    if (_strPriceStd.startsWith("\"")) {
      strPriceStd = _strPriceStd.substring(1, _strPriceStd.length() - 1);
    }
    boolean isTaxIncludedPriceList = OBDal.getInstance().get(Order.class, strCOrderId)
        .getPriceList().isPriceIncludesTax();

    if (!strMProductID.equals("")) {

      Order order = OBDal.getInstance().get(Order.class, strCOrderId);
      Product product = OBDal.getInstance().get(Product.class, strMProductID);

      if (!"Y".equals(cancelPriceAd)) {
        if (isTaxIncludedPriceList) {
          strPriceActual = PriceAdjustment.calculatePriceActual(order, product,
              "".equals(strQty) ? BigDecimal.ZERO : new BigDecimal(strQty),
              new BigDecimal(strGrossBaseUnitPrice.equals("") ? "0" : strGrossBaseUnitPrice))
              .toString();
          strNetPriceList = "0";
        } else {
          strPriceActual = PriceAdjustment.calculatePriceActual(order, product,
              "".equals(strQty) ? BigDecimal.ZERO : new BigDecimal(strQty),
              new BigDecimal(strPriceStd.equals("") ? "0" : strPriceStd)).toString();
          strGrossPriceList = "0";
        }
     } else {
        if (isTaxIncludedPriceList)
          strPriceActual = strGrossBaseUnitPrice;
        else
          strPriceActual = strPriceList;
     }
    } else {
      strUOM = strNetPriceList = strGrossPriceList = strPriceLimit = strPriceStd = "";
    }
    StringBuffer resultado = new StringBuffer();
    // Discount...
    BigDecimal discount = BigDecimal.ZERO;
    BigDecimal priceStd = null;
    if (isTaxIncludedPriceList) {
      BigDecimal priceList = (strGrossPriceList.equals("") ? BigDecimal.ZERO : new BigDecimal(
          strGrossPriceList));
      BigDecimal grossBaseUnitPrice = (strGrossBaseUnitPrice.equals("") ? BigDecimal.ZERO
          : new BigDecimal(strGrossBaseUnitPrice));
      if (priceList.compareTo(BigDecimal.ZERO) != 0) {
        discount = priceList.subtract(grossBaseUnitPrice).multiply(new BigDecimal("100"))
            .divide(priceList, 2, BigDecimal.ROUND_HALF_UP);
      }
    } else {
      BigDecimal priceList = (strNetPriceList.equals("") ? BigDecimal.ZERO : new BigDecimal(
          strNetPriceList));
      priceStd = (strPriceStd.equals("") ? BigDecimal.ZERO : new BigDecimal(strPriceStd));
      if (priceList.compareTo(BigDecimal.ZERO) != 0) {
        discount = priceList.subtract(priceStd).multiply(new BigDecimal("100"))
            .divide(priceList, 2, BigDecimal.ROUND_HALF_UP);
      }
    }

    resultado.append("var calloutName='SL_Order_Product';\n\n");
    resultado.append("var respuesta = new Array(");
    resultado.append("new Array(\"inpcUomId\", \"" + strUOM + "\"),");
    if (isTaxIncludedPriceList) {
      resultado.append("new Array(\"inpgrossUnitPrice\", "
          + (strPriceActual.equals("") ? "0" : strPriceActual) + "),");
      resultado.append("new Array(\"inpgrosspricelist\", "
          + (strGrossPriceList.equals("") ? "0" : strGrossPriceList) + "),");
      resultado.append("new Array(\"inpgrosspricestd\", "
          + (strGrossBaseUnitPrice.equals("") ? "0" : strGrossBaseUnitPrice) + "),");
    } else {
      resultado.append("new Array(\"inppricelist\", "
          + (strNetPriceList.equals("") ? "0" : strNetPriceList) + "),");
      resultado.append("new Array(\"inppricelimit\", "
          + (strPriceLimit.equals("") ? "0" : strPriceLimit) + "),");
      resultado.append("new Array(\"inppricestd\", " + (strPriceStd.equals("") ? "0" : strPriceStd)
          + "),");
      resultado.append("new Array(\"inppriceactual\", "
          + (strPriceActual.equals("") ? "0" : strPriceActual) + "),");
    }
    if (!"".equals(strCurrency)) {
      resultado.append("new Array(\"inpcCurrencyId\", \"" + strCurrency + "\"),");
    }
    resultado.append("new Array(\"inpdiscount\", " + discount.toString() + "),");
    if (!strMProductID.equals("")) {
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
      } else {
        resultado.append("new Array(\"inpmAttributesetinstanceId\", \"\"),");
        resultado.append("new Array(\"inpmAttributesetinstanceId_R\", \"\"),");
      }
      resultado.append("new Array(\"inpattributeset\", \""
          + FormatUtilities.replaceJS(dataPAttr[0].mAttributesetId) + "\"),\n");
      resultado.append("new Array(\"inpattrsetvaluetype\", \""
          + FormatUtilities.replaceJS(dataPAttr[0].attrsetvaluetype) + "\"),\n");
      strHasSecondaryUOM = SLOrderProductData.hasSecondaryUOM(this, strMProductID);
      resultado.append("new Array(\"inphasseconduom\", " + strHasSecondaryUOM + "),\n");
    }

    String strCTaxID = "";
    String orgLocationID = SLOrderProductData.getOrgLocationId(this,
        Utility.getContext(this, vars, "#User_Client", "SLOrderProduct"), "'" + strADOrgID + "'");
    if (orgLocationID.equals("")) {
      resultado.append("new Array('MESSAGE', \""
          + FormatUtilities.replaceJS(Utility.messageBD(this, "NoLocationNoTaxCalculated",
              vars.getLanguage())) + "\"),\n");
    } else {
      SLOrderTaxData[] data = SLOrderTaxData.select(this, strCOrderId);
      strCTaxID = Tax.get(this, strMProductID, data[0].dateordered, strADOrgID, strMWarehouseID,
          (data[0].billtoId.equals("") ? strCBPartnerLocationID : data[0].billtoId),
          strCBPartnerLocationID, data[0].cProjectId, strIsSOTrx.equals("Y"),
          "Y".equals(data[0].iscashvat));
    }
    if (!strCTaxID.equals("")) {
      resultado.append("new Array(\"inpcTaxId\", \"" + strCTaxID + "\"),\n");
    }

    resultado.append("new Array(\"inpmProductUomId\", ");
    // if (strUOM.startsWith("\""))
    // strUOM=strUOM.substring(1,strUOM.length()-1);
    // String strmProductUOMId =
    // SLOrderProductData.strMProductUOMID(this,strMProductID,strUOM);
    if (vars.getLanguage().equals("en_US")) {
      FieldProvider[] tld = null;
      try {
        ComboTableData comboTableData = new ComboTableData(vars, this, "TABLE", "",
            "M_Product_UOM", "", Utility.getContext(this, vars, "#AccessibleOrgTree",
                "SLOrderProduct"),
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
          if (i < tld.length - 1) {
            resultado.append(",\n");
          }
        }
        resultado.append("\n)");
      } else {
        resultado.append("null");
      }
      resultado.append("\n),");
    } else {
      FieldProvider[] tld = null;
      try {
        ComboTableData comboTableData = new ComboTableData(vars, this, "TABLE", "",
            "M_Product_UOM", "", Utility.getContext(this, vars, "#AccessibleOrgTree",
                "SLOrderProduct"),
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
          if (i < tld.length - 1) {
            resultado.append(",\n");
          }
        }
        resultado.append("\n)");
      } else {
        resultado.append("null");
      }
      resultado.append("\n),");
    }
    resultado.append("new Array(\"EXECUTE\", \"displayLogic();\"),\n");
    // Para posicionar el cursor en el campo de cantidad
    resultado.append("new Array(\"CURSOR_FIELD\", \"inpqtyordered\")\n");
    if (!strHasSecondaryUOM.equals("0")) {
      resultado.append(", new Array(\"CURSOR_FIELD\", \"inpquantityorder\")\n");
    }
    resultado.append(");");
    xmlDocument.setParameter("array", resultado.toString());
    xmlDocument.setParameter("frameName", "appFrame");
    response.setContentType("text/html; charset=UTF-8");
    PrintWriter out = response.getWriter();
    out.println(xmlDocument.print());
    out.close();
  }
}
