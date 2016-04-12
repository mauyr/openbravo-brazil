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
 * All portions are Copyright (C) 2001-2012 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.erpCommon.ad_actionButton;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.sql.Connection;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.businessUtility.Tax;
import org.openbravo.erpCommon.utility.DateTimeData;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.SequenceIdData;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.financial.FinancialUtils;
import org.openbravo.model.common.businesspartner.Location;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.invoice.InvoiceLine;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.financialmgmt.gl.GLItem;
import org.openbravo.model.financialmgmt.tax.TaxRate;
import org.openbravo.model.pricing.pricelist.ProductPrice;
import org.openbravo.xmlEngine.XmlDocument;

public class CopyFromInvoice extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;

  public void init(ServletConfig config) {
    super.init(config);
    boolHist = false;
  }

  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException,
      ServletException {
    VariablesSecureApp vars = new VariablesSecureApp(request);

    if (vars.commandIn("DEFAULT")) {
      String strProcessId = vars.getStringParameter("inpProcessId");
      String strWindow = vars.getStringParameter("inpwindowId");
      String strTab = vars.getStringParameter("inpTabId");
      String strKey = vars.getGlobalVariable("inpcInvoiceId", strWindow + "|C_Invoice_ID");
      printPage(response, vars, strKey, strWindow, strTab, strProcessId);
    } else if (vars.commandIn("SAVE")) {
      String strKey = vars.getStringParameter("inpcInvoiceId");
      String strInvoice = vars.getStringParameter("inpNewcInvoiceId");
      String strWindow = vars.getStringParameter("inpwindowId");
      String strTab = vars.getStringParameter("inpTabId");
      String strPriceListCheck = vars.getStringParameter("inpPriceList");
      String strWindowPath = Utility.getTabURL(strTab, "R", true);
      if (strWindowPath.equals(""))
        strWindowPath = strDefaultServlet;

      OBError myError = processButton(vars, strKey, strInvoice, strWindow, strPriceListCheck);
      vars.setMessage(strTab, myError);
      printPageClosePopUp(response, vars, strWindowPath);
    } else
      pageErrorPopUp(response);
  }

  private OBError processButton(VariablesSecureApp vars, String strKey, String strInvoice,
      String windowId, String strPriceListCheck) {
    int i = 0;
    OBError myError = null;
    Connection conn = null;
    try {
      conn = getTransactionConnection();
      CopyFromInvoiceData[] data = CopyFromInvoiceData.select(conn, this, strInvoice,
          Utility.getContext(this, vars, "#User_Client", windowId),
          Utility.getContext(this, vars, "#User_Org", windowId));
      CopyFromInvoiceData[] dataInvoice = CopyFromInvoiceData.selectInvoice(conn, this, strKey);
      Invoice invoice = OBDal.getInstance().get(Invoice.class, strKey);
      Invoice invToCopy = OBDal.getInstance().get(Invoice.class, strInvoice);
      int pricePrecision = invoice.getPriceList().getCurrency().getPricePrecision().intValue();
      int stdPrecision = invoice.getPriceList().getCurrency().getStandardPrecision().intValue();

      if (data == null || data.length == 0) {
        myError = new OBError();
        myError.setType("Success");
        myError.setTitle(OBMessageUtils.messageBD("Success"));
        myError.setMessage(OBMessageUtils.messageBD("RecordsCopied") + " " + i);
        return myError;
      }
      for (i = 0; i < data.length; i++) {
        String strSequence = SequenceIdData.getUUID();
        try {
          InvoiceLine invLine = OBDal.getInstance().get(InvoiceLine.class, data[i].cInvoicelineId);
          String strDateInvoiced = "";
          String strInvPriceList = "";
          String strBPartnerId = "";
          String strmProductId = "";

          BigDecimal priceActual, priceStd, priceList, priceLimit, priceGross, priceListGross, priceStdGross;
          priceActual = priceStd = priceList = priceLimit = priceGross = priceListGross = priceStdGross = BigDecimal.ZERO;
          BigDecimal lineNetAmt, lineGrossAmt;
          lineNetAmt = lineGrossAmt = BigDecimal.ZERO;
          strDateInvoiced = dataInvoice[0].dateinvoiced;
          strInvPriceList = dataInvoice[0].mPricelistId;
          strBPartnerId = dataInvoice[0].mPricelistId;
          strmProductId = data[i].productId;

          String strWindowId = vars.getStringParameter("inpwindowId");
          String strWharehouse = Utility.getContext(this, vars, "#M_Warehouse_ID", strWindowId);
          String strIsSOTrx = Utility.getContext(this, vars, "isSOTrx", strWindowId);

          String strCTaxID = Tax.get(this, data[i].productId, dataInvoice[0].dateinvoiced,
              dataInvoice[0].adOrgId, strWharehouse, dataInvoice[0].cBpartnerLocationId,
              dataInvoice[0].cBpartnerLocationId, dataInvoice[0].cProjectId,
              strIsSOTrx.equals("Y"), data[i].accountId);

          // force get price list price if mixing tax including price lists.
          boolean forcePriceList = (invoice.getPriceList().isPriceIncludesTax() != invToCopy
              .getPriceList().isPriceIncludesTax());
          if ("Y".equals(strPriceListCheck) || forcePriceList) {

            CopyFromInvoiceData[] invoicelineprice = CopyFromInvoiceData.selectPriceForProduct(
                this, strmProductId, strInvPriceList);
            for (int j = 0; invoicelineprice != null && j < invoicelineprice.length; j++) {
              if (invoicelineprice[j].validfrom == null
                  || invoicelineprice[j].validfrom.equals("")
                  || !DateTimeData.compare(this, DateTimeData.today(this),
                      invoicelineprice[j].validfrom).equals("-1")) {
                priceList = new BigDecimal(invoicelineprice[j].pricelist);
                priceLimit = new BigDecimal(invoicelineprice[j].pricelimit);
                priceStd = (invoicelineprice[j].pricestd.equals("") ? BigDecimal.ZERO
                    : (new BigDecimal(invoicelineprice[j].pricestd))).setScale(pricePrecision,
                    BigDecimal.ROUND_HALF_UP);
                priceListGross = BigDecimal.ZERO;
                priceStdGross = BigDecimal.ZERO;

                if (invoice.getPriceList().isPriceIncludesTax()) {
                  priceGross = priceStd;
                  lineGrossAmt = priceGross.multiply(invLine.getInvoicedQuantity()).setScale(
                      stdPrecision, BigDecimal.ROUND_HALF_UP);
                  priceActual = FinancialUtils.calculateNetFromGross(strCTaxID, lineGrossAmt,
                      pricePrecision, lineGrossAmt, invLine.getInvoicedQuantity());
                  ProductPrice prices = FinancialUtils.getProductPrice(
                      OBDal.getInstance().get(Product.class, strmProductId),
                      invoice.getInvoiceDate(), invoice.isSalesTransaction(),
                      invoice.getPriceList(), false);
                  if (prices != null) {
                    priceListGross = prices.getListPrice();
                    priceStdGross = prices.getStandardPrice();
                  }
                } else {
                  // Calculate price adjustments (offers)
                  priceActual = new BigDecimal(CopyFromInvoiceData.getOffersStdPrice(this,
                      strBPartnerId, priceStd.toString(), strmProductId, strDateInvoiced, invLine
                          .getInvoicedQuantity().toString(), strInvPriceList, strKey));
                  if (priceActual.scale() > pricePrecision) {
                    priceActual = priceActual.setScale(pricePrecision, BigDecimal.ROUND_HALF_UP);
                  }
                }
                // Calculate line net amount
                lineNetAmt = invLine.getInvoicedQuantity().multiply(priceActual);
                if (lineNetAmt.scale() > pricePrecision) {
                  lineNetAmt = lineNetAmt.setScale(pricePrecision, BigDecimal.ROUND_HALF_UP);
                }
                break;
              }
            }
          } else {
            priceList = invLine.getListPrice();
            priceLimit = invLine.getPriceLimit();
            priceActual = invLine.getUnitPrice();
            priceGross = invLine.getGrossUnitPrice();
            lineNetAmt = invLine.getLineNetAmount();
            lineGrossAmt = invLine.getGrossAmount();
            priceListGross = invLine.getGrossListPrice();
            priceStdGross = invLine.getBaseGrossUnitPrice();
          }

          // Checking, why is not possible to get a tax
          if ("".equals(strCTaxID) && lineNetAmt.compareTo(BigDecimal.ZERO) != 0) {
            throwTaxNotFoundException(data[i].accountId, data[i].productId,
                dataInvoice[0].cBpartnerLocationId);
          }
          CopyFromInvoiceData.insert(conn, this, strSequence, strKey, dataInvoice[0].adClientId,
              vars.getUser(), priceList.toString(), priceActual.toString(), priceLimit.toString(),
              lineNetAmt.toString(), strCTaxID, priceGross.toString(), lineGrossAmt.toString(),
              priceListGross.toString(), priceStdGross.toString(), data[i].cInvoicelineId);

          // Copy accounting dimensions
          CopyFromInvoiceData.insertAcctDimension(conn, this, dataInvoice[0].adClientId,
              vars.getUser(), strSequence, data[i].cInvoicelineId);
        } catch (ServletException ex) {
          myError = OBMessageUtils.translateError(this, vars, vars.getLanguage(), ex.getMessage());
          releaseRollbackConnection(conn);
        }
      }

      releaseCommitConnection(conn);
    } catch (OBException obe) {
      try {
        releaseRollbackConnection(conn);
      } catch (Exception ignored) {
      }
      myError = new OBError();
      myError.setType("Error");
      myError.setTitle(OBMessageUtils.messageBD("Error"));
      myError.setMessage(obe.getMessage());
      return myError;
    } catch (Exception e) {
      try {
        releaseRollbackConnection(conn);
      } catch (Exception ignored) {
      }
      log4j.warn("Rollback in transaction", e);
      myError = new OBError();
      myError.setType("Error");
      myError.setTitle(OBMessageUtils.messageBD("Error"));
      myError.setMessage(OBMessageUtils.messageBD("ProcessRunError"));
      return myError;
    }
    myError = new OBError();
    myError.setType("Success");
    myError.setTitle(OBMessageUtils.messageBD("Success"));
    myError.setMessage(OBMessageUtils.messageBD("RecordsCopied") + " " + i);
    return myError;
  }

  private void printPage(HttpServletResponse response, VariablesSecureApp vars, String strKey,
      String windowId, String strTab, String strProcessId) throws IOException, ServletException {
    log4j.debug("Output: Button process Copy from Invoice");

    ActionButtonDefaultData[] data = null;
    String strHelp = "", strDescription = "";
    if (vars.getLanguage().equals("en_US")) {
      data = ActionButtonDefaultData.select(this, strProcessId);
    } else {
      data = ActionButtonDefaultData.selectLanguage(this, vars.getLanguage(), strProcessId);
    }

    if (data != null && data.length != 0) {
      strDescription = data[0].description;
      strHelp = data[0].help;
    }
    String[] discard = { "" };
    if (strHelp.equals("")) {
      discard[0] = new String("helpDiscard");
    }
    XmlDocument xmlDocument = xmlEngine.readXmlTemplate(
        "org/openbravo/erpCommon/ad_actionButton/CopyFromInvoice", discard).createXmlDocument();
    xmlDocument.setParameter("key", strKey);
    xmlDocument.setParameter("window", windowId);
    xmlDocument.setParameter("tab", strTab);
    xmlDocument.setParameter("language", "defaultLang=\"" + vars.getLanguage() + "\";");
    xmlDocument.setParameter("question", OBMessageUtils.messageBD("StartProcess?"));
    xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
    xmlDocument.setParameter("theme", vars.getTheme());
    xmlDocument.setParameter("description", strDescription);
    xmlDocument.setParameter("help", strHelp);

    response.setContentType("text/html; charset=UTF-8");
    PrintWriter out = response.getWriter();
    out.println(xmlDocument.print());
    out.close();
  }

  private void throwTaxNotFoundException(String accountId, String productId,
      String cBpartnerLocationId) throws OBException {
    if (!"".equals(accountId)) {
      GLItem glItem = OBDal.getInstance().get(GLItem.class, accountId);

      OBCriteria<TaxRate> obcriteria = OBDal.getInstance().createCriteria(TaxRate.class);
      obcriteria.add(Restrictions.eq(TaxRate.PROPERTY_TAXCATEGORY, glItem.getTaxCategory()));
      obcriteria.add(Restrictions.eq(TaxRate.PROPERTY_ACTIVE, Boolean.TRUE));
      List<TaxRate> taxRates = obcriteria.list();
      if (taxRates.size() == 0) {
        throw new OBException(String.format(OBMessageUtils
            .messageBD("NotExistTaxRateForTaxCategory"), glItem.getTaxCategory().getIdentifier(),
            glItem.getIdentifier()));
      } else {
        Location location = OBDal.getInstance().get(Location.class, cBpartnerLocationId);
        throw new OBException(String.format(OBMessageUtils.messageBD("NotExistTaxRateForTaxZone"),
            glItem.getIdentifier(), glItem.getTaxCategory().getIdentifier(),
            location.getIdentifier()));
      }
    } else if (!"".equals(productId)) {
      Product product = OBDal.getInstance().get(Product.class, productId);

      OBCriteria<TaxRate> obcriteria = OBDal.getInstance().createCriteria(TaxRate.class);
      obcriteria.add(Restrictions.eq(TaxRate.PROPERTY_TAXCATEGORY, product.getTaxCategory()));
      obcriteria.add(Restrictions.eq(TaxRate.PROPERTY_ACTIVE, Boolean.TRUE));
      List<TaxRate> taxRates = obcriteria.list();
      if (taxRates.size() == 0) {
        throw new OBException(String.format(OBMessageUtils
            .messageBD("NotExistTaxRateForTaxCategory"), product.getTaxCategory().getIdentifier(),
            product.getIdentifier()));
      } else {
        Location location = OBDal.getInstance().get(Location.class, cBpartnerLocationId);
        throw new OBException(String.format(OBMessageUtils.messageBD("NotExistTaxRateForTaxZone"),
            product.getIdentifier(), product.getTaxCategory().getIdentifier(),
            location.getIdentifier()));
      }
    }

  }

  public String getServletInfo() {
    return "Servlet Copy from invoice";
  } // end of getServletInfo() method
}
