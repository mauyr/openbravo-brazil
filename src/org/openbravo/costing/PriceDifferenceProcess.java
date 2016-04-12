/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.0  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License.
 * The Original Code is Openbravo ERP.
 * The Initial Developer of the Original Code is Openbravo SLU
 * All portions are Copyright (C) 2014-2015 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 *************************************************************************
 */
package org.openbravo.costing;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.costing.CostingServer.TrxType;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.financial.FinancialUtils;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.materialmgmt.cost.CostAdjustment;
import org.openbravo.model.materialmgmt.cost.CostAdjustmentLine;
import org.openbravo.model.materialmgmt.transaction.MaterialTransaction;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;
import org.openbravo.model.procurement.ReceiptInvoiceMatch;

public class PriceDifferenceProcess {
  private static CostAdjustment costAdjHeader = null;

  private static boolean calculateTransactionPriceDifferenceLogic(
      MaterialTransaction materialTransaction) throws OBException {
    boolean costAdjCreated = false;

    // With Standard Algorithm, no cost adjustment is needed
    if (StringUtils.equals(materialTransaction.getCostingAlgorithm().getJavaClassName(),
        "org.openbravo.costing.StandardAlgorithm")) {
      return false;
    }

    if (materialTransaction.isCostPermanent()) {
      // Permanently adjusted transaction costs are not checked for price differences.
      return false;
    }
    Currency trxCurrency = materialTransaction.getCurrency();
    Organization trxOrg = materialTransaction.getOrganization();
    Date trxDate = materialTransaction.getMovementDate();
    int costCurPrecission = trxCurrency.getCostingPrecision().intValue();
    ShipmentInOutLine receiptLine = materialTransaction.getGoodsShipmentLine();
    if (receiptLine == null
        || !isValidPriceAdjTrx(receiptLine.getMaterialMgmtMaterialTransactionList().get(0))) {
      // We can only adjust cost of receipt lines.
      return false;
    }

    BigDecimal receiptQty = receiptLine.getMovementQuantity();
    boolean isNegativeReceipt = receiptQty.signum() == -1;
    if (isNegativeReceipt) {
      // If the receipt is negative convert the quantity to positive.
      receiptQty = receiptQty.negate();
    }

    Date costAdjDateAcct = null;
    BigDecimal invoiceAmt = BigDecimal.ZERO;

    // Calculate current transaction unit cost including existing adjustments.
    BigDecimal currentTrxUnitCost = CostAdjustmentUtils.getTrxCost(materialTransaction, true,
        trxCurrency);

    // Calculate expected transaction unit cost based on current invoice amounts and purchase price.
    BigDecimal expectedCost = BigDecimal.ZERO;
    BigDecimal invoiceQty = BigDecimal.ZERO;
    for (ReceiptInvoiceMatch matchInv : receiptLine.getProcurementReceiptInvoiceMatchList()) {
      Invoice invoice = matchInv.getInvoiceLine().getInvoice();
      if (invoice.getDocumentStatus().equals("VO")) {
        // Skip voided invoices.
        continue;
      }
      if (!invoice.isProcessed()) {
        // Skip not processed invoices.
        continue;
      }
      if (isNegativeReceipt) {
        // If the receipt is negative negate the invoiced quantities.
        invoiceQty = invoiceQty.add(matchInv.getQuantity().negate());
      } else {
        invoiceQty = invoiceQty.add(matchInv.getQuantity());
      }
      invoiceAmt = matchInv.getQuantity().multiply(matchInv.getInvoiceLine().getUnitPrice());

      invoiceAmt = FinancialUtils.getConvertedAmount(invoiceAmt, invoice.getCurrency(),
          trxCurrency, trxDate, trxOrg, FinancialUtils.PRECISION_STANDARD,
          invoice.getCurrencyConversionRateDocList());
      expectedCost = expectedCost.add(invoiceAmt);

      Date invoiceDate = invoice.getInvoiceDate();
      if (costAdjDateAcct == null || costAdjDateAcct.before(invoiceDate)) {
        costAdjDateAcct = invoiceDate;
      }
    }

    BigDecimal notInvoicedQty = receiptQty.subtract(invoiceQty);
    if (notInvoicedQty.signum() > 0) {
      // Not all the receipt line is invoiced, add pending invoice quantity valued with current
      // order price if exists or original unit cost.
      BigDecimal basePrice = BigDecimal.ZERO;
      Currency baseCurrency = trxCurrency;
      if (receiptLine.getSalesOrderLine() != null) {
        basePrice = receiptLine.getSalesOrderLine().getUnitPrice();
        baseCurrency = receiptLine.getSalesOrderLine().getSalesOrder().getCurrency();
      } else {
        basePrice = materialTransaction.getTransactionCost().divide(receiptQty, costCurPrecission,
            RoundingMode.HALF_UP);
      }
      BigDecimal baseAmt = notInvoicedQty.multiply(basePrice).setScale(costCurPrecission,
          RoundingMode.HALF_UP);
      if (!baseCurrency.getId().equals(trxCurrency.getId())) {
        baseAmt = FinancialUtils.getConvertedAmount(baseAmt, baseCurrency, trxCurrency, trxDate,
            trxOrg, FinancialUtils.PRECISION_STANDARD);
      }
      expectedCost = expectedCost.add(baseAmt);
    }
    // if the sum of trx costs with flag "isInvoiceCorrection" is distinct that the amount cost
    // generated by Match Invoice then New Cost Adjustment line is created by the difference
    if (expectedCost.compareTo(currentTrxUnitCost) != 0) {
      if (costAdjDateAcct == null) {
        costAdjDateAcct = trxDate;
      }
      createCostAdjustmenHeader(trxOrg);
      CostAdjustmentLine costAdjLine = CostAdjustmentUtils.insertCostAdjustmentLine(
          materialTransaction, costAdjHeader, expectedCost.subtract(currentTrxUnitCost),
          Boolean.TRUE, costAdjDateAcct);
      costAdjLine.setNeedsPosting(Boolean.TRUE);
      OBDal.getInstance().save(costAdjLine);
      costAdjCreated = true;
    }

    return costAdjCreated;
  }

  private static boolean calculateTransactionPriceDifference(MaterialTransaction materialTransaction)
      throws OBException {

    boolean costAdjCreated = calculateTransactionPriceDifferenceLogic(materialTransaction);

    materialTransaction.setCheckpricedifference(Boolean.FALSE);
    OBDal.getInstance().save(materialTransaction);
    OBDal.getInstance().flush();

    return costAdjCreated;

  }

  public static JSONObject processPriceDifferenceTransaction(MaterialTransaction materialTransaction)
      throws OBException {
    costAdjHeader = null;

    calculateTransactionPriceDifference(materialTransaction);

    if (costAdjHeader != null) {
      OBDal.getInstance().flush();
      JSONObject message = CostAdjustmentProcess.doProcessCostAdjustment(costAdjHeader);
      try {
        message.put("documentNo", costAdjHeader.getDocumentNo());
        if (message.get("severity") != "success") {
          throw new OBException(OBMessageUtils.parseTranslation("@ErrorProcessingCostAdj@") + ": "
              + costAdjHeader.getDocumentNo() + " - " + message.getString("text"));
        }
      } catch (JSONException e) {
        throw new OBException(OBMessageUtils.parseTranslation("@ErrorProcessingCostAdj@"));
      }

      return message;
    } else {
      JSONObject message = new JSONObject();
      try {
        message.put("severity", "success");
        message.put("title", "");
        message.put("text", OBMessageUtils.messageBD("Success"));
      } catch (JSONException ignore) {
      }
      return message;
    }
  }

  /**
   * @return the message to be shown to the user properly formatted and translated to the user
   *         language.
   * @throws OBException
   *           when there is an error that prevents the cost adjustment to be processed.
   * @throws OBException
   */
  public static JSONObject processPriceDifference(Date date, Product product) throws OBException {

    JSONObject message = null;
    costAdjHeader = null;
    boolean costAdjCreated = false;
    int count = 0;
    OBCriteria<MaterialTransaction> mTrxs = OBDal.getInstance().createCriteria(
        MaterialTransaction.class);
    if (date != null) {
      mTrxs.add(Restrictions.le(MaterialTransaction.PROPERTY_MOVEMENTDATE, date));
    }
    if (product != null) {
      mTrxs.add(Restrictions.eq(MaterialTransaction.PROPERTY_PRODUCT, product));
    }
    mTrxs.add(Restrictions.eq(MaterialTransaction.PROPERTY_CHECKPRICEDIFFERENCE, true));
    mTrxs.add(Restrictions.eq(MaterialTransaction.PROPERTY_ISCOSTCALCULATED, true));
    mTrxs.addOrderBy(MaterialTransaction.PROPERTY_MOVEMENTDATE, true);
    mTrxs.addOrderBy(MaterialTransaction.PROPERTY_TRANSACTIONPROCESSDATE, true);
    ScrollableResults lines = mTrxs.scroll(ScrollMode.FORWARD_ONLY);

    try {
      while (lines.next()) {
        MaterialTransaction line = (MaterialTransaction) lines.get(0);
        costAdjCreated = calculateTransactionPriceDifference(line);
        if (costAdjCreated) {
          count++;
        }

      }
    } finally {
      lines.close();
    }

    Map<String, String> map = new HashMap<String, String>();
    map.put("trxsNumber", Integer.toString(count));
    String messageText = OBMessageUtils.messageBD("PriceDifferenceChecked");

    if (costAdjHeader != null) {
      OBDal.getInstance().flush();
      message = CostAdjustmentProcess.doProcessCostAdjustment(costAdjHeader);
      try {
        if (message.get("severity") != "success") {
          throw new OBException(OBMessageUtils.parseTranslation("@ErrorProcessingCostAdj@") + ": "
              + costAdjHeader.getDocumentNo() + " - " + message.getString("text"));
        } else {
          message.put("title", OBMessageUtils.messageBD("Success"));
          message.put("text", OBMessageUtils.parseTranslation(messageText, map));
        }
      } catch (JSONException e) {
        throw new OBException(OBMessageUtils.parseTranslation("@ErrorProcessingCostAdj@"));
      }
      return message;
    } else {
      try {
        message = new JSONObject();
        message.put("severity", "success");
        message.put("title", OBMessageUtils.messageBD("Success"));
        message.put("text", OBMessageUtils.parseTranslation(messageText, map));
      } catch (JSONException ignore) {
      }
      return message;
    }
  }

  private static void createCostAdjustmenHeader(Organization org) {
    if (costAdjHeader == null) {
      costAdjHeader = CostAdjustmentUtils.insertCostAdjustmentHeader(org, "PDC");
      // PDC: Price Dif Correction
    }
  }

  /**
   * True if is an Incoming Transaction
   */
  private static boolean isValidPriceAdjTrx(MaterialTransaction trx) {
    TrxType transacctionType = TrxType.getTrxType(trx);
    switch (transacctionType) {
    case Receipt:
      return true;
    default:
      return false;
    }
  }
}
