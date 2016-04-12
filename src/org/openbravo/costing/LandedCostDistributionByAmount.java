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

import org.hibernate.Query;
import org.hibernate.ScrollableResults;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.financial.FinancialUtils;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.materialmgmt.cost.CostAdjustmentLine;
import org.openbravo.model.materialmgmt.cost.LCReceipt;
import org.openbravo.model.materialmgmt.cost.LCReceiptLineAmt;
import org.openbravo.model.materialmgmt.cost.LandedCost;
import org.openbravo.model.materialmgmt.cost.LandedCostCost;
import org.openbravo.model.materialmgmt.cost.TransactionCost;
import org.openbravo.model.materialmgmt.transaction.MaterialTransaction;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;

public class LandedCostDistributionByAmount extends LandedCostDistributionAlgorithm {

  @Override
  public void distributeAmount(LandedCostCost lcCost, boolean isMatching) {
    // Calculate total amount of all receipt lines assigned to the landed cost.
    lcCost = (LandedCostCost) OBDal.getInstance().getProxy(LandedCostCost.ENTITY_NAME,
        lcCost.getId());
    LandedCost landedCost = lcCost.getLandedCost();
    // Get the currency of the Landed Cost Cost
    String strCurId = lcCost.getCurrency().getId();
    String strOrgId = landedCost.getOrganization().getId();
    Date dateReference = landedCost.getReferenceDate();
    int precission = lcCost.getCurrency().getCostingPrecision().intValue();
    BigDecimal baseAmt;
    if (isMatching) {
      baseAmt = lcCost.getMatchingAmount().subtract(lcCost.getAmount());
    } else {
      baseAmt = lcCost.getAmount();
    }

    BigDecimal totalAmt = BigDecimal.ZERO;

    // Loop to get all receipts amounts and calculate the total.
    OBCriteria<LCReceipt> critLCRL = OBDal.getInstance().createCriteria(LCReceipt.class);
    critLCRL.add(Restrictions.eq(LCReceipt.PROPERTY_LANDEDCOST, landedCost));
    ScrollableResults receiptCosts = getReceiptCosts(landedCost, false);
    int i = 0;
    try {
      while (receiptCosts.next()) {
        String strTrxCur = (String) receiptCosts.get()[2];
        BigDecimal trxAmt = (BigDecimal) receiptCosts.get()[3];
        if (!strTrxCur.equals(strCurId)) {
          trxAmt = getConvertedAmount(trxAmt, strTrxCur, strCurId, dateReference, strOrgId);
        }

        totalAmt = totalAmt.add(trxAmt);

        if (i % 100 == 0) {
          OBDal.getInstance().flush();
          OBDal.getInstance().getSession().clear();
        }
        i++;
      }
    } finally {
      receiptCosts.close();
    }

    BigDecimal pendingAmt = baseAmt;
    // Loop to calculate the corresponding adjustment amount for each receipt line.
    receiptCosts = getReceiptCosts(landedCost, true);
    i = 0;
    while (receiptCosts.next()) {
      ShipmentInOutLine receiptline = OBDal.getInstance().get(ShipmentInOutLine.class,
          receiptCosts.get()[1]);
      String strTrxCurId = (String) receiptCosts.get()[2];
      BigDecimal trxAmt = (BigDecimal) receiptCosts.get()[3];

      if (!strTrxCurId.equals(strCurId)) {
        trxAmt = getConvertedAmount(trxAmt, strTrxCurId, strCurId, dateReference, strOrgId);
      }

      BigDecimal receiptAmt = BigDecimal.ZERO;
      if (receiptCosts.isLast()) {
        // Insert pending amount on receipt with higher cost to avoid rounding issues.
        receiptAmt = pendingAmt;
      } else {
        receiptAmt = baseAmt.multiply(trxAmt).divide(totalAmt, precission, RoundingMode.HALF_UP);
      }
      pendingAmt = pendingAmt.subtract(receiptAmt);
      LCReceipt lcrl = (LCReceipt) OBDal.getInstance().getProxy(LCReceipt.ENTITY_NAME,
          receiptCosts.get()[0]);
      LCReceiptLineAmt lcrla = OBProvider.getInstance().get(LCReceiptLineAmt.class);
      lcrla.setLandedCostCost((LandedCostCost) OBDal.getInstance().getProxy(
          LandedCostCost.ENTITY_NAME, lcCost.getId()));
      lcCost = (LandedCostCost) OBDal.getInstance().getProxy(LandedCostCost.ENTITY_NAME,
          lcCost.getId());
      lcrla.setLandedCostReceipt(lcrl);
      lcrla.setGoodsShipmentLine(receiptline);
      lcrla.setMatchingAdjustment(isMatching);
      lcrla.setAmount(receiptAmt);
      lcrla.setOrganization(lcCost.getOrganization());
      OBDal.getInstance().save(lcrla);
      if (i % 100 == 0) {
        OBDal.getInstance().flush();
        OBDal.getInstance().getSession().clear();
      }
      i++;
    }
  }

  private ScrollableResults getReceiptCosts(LandedCost landedCost, boolean doOrderBy) {
    StringBuffer qry = new StringBuffer();
    qry.append("select lcr.id as lcreceipt"); // 0
    qry.append("   , iol.id as receiptline"); // 1
    qry.append("   , trx." + MaterialTransaction.PROPERTY_CURRENCY + ".id as currency"); // 2
    qry.append("   , sum(tc." + TransactionCost.PROPERTY_COST + ") as cost"); // 3
    qry.append(" from " + TransactionCost.ENTITY_NAME + " as tc");
    qry.append("   join tc." + TransactionCost.PROPERTY_INVENTORYTRANSACTION + " as trx");
    qry.append("   join trx." + MaterialTransaction.PROPERTY_GOODSSHIPMENTLINE + " as iol");
    qry.append(" , " + LCReceipt.ENTITY_NAME + " as lcr");
    qry.append(" where tc." + CostAdjustmentLine.PROPERTY_UNITCOST + " = true");
    qry.append(" and iol." + ShipmentInOutLine.PROPERTY_MOVEMENTQUANTITY + " >= 0");
    qry.append("   and ((lcr." + LCReceipt.PROPERTY_GOODSSHIPMENTLINE + " is not null");
    qry.append("        and lcr." + LCReceipt.PROPERTY_GOODSSHIPMENTLINE + " = iol)");
    qry.append("         or (lcr." + LCReceipt.PROPERTY_GOODSSHIPMENTLINE + " is null");
    qry.append("        and lcr." + LCReceipt.PROPERTY_GOODSSHIPMENT + " = iol."
        + ShipmentInOutLine.PROPERTY_SHIPMENTRECEIPT + "))");
    qry.append("   and lcr." + LCReceipt.PROPERTY_LANDEDCOST + ".id = :landedCost");
    qry.append(" group by lcr.id, iol.id, trx." + MaterialTransaction.PROPERTY_CURRENCY
        + ".id, iol." + ShipmentInOutLine.PROPERTY_LINENO);
    if (doOrderBy) {
      qry.append(" order by iol." + ShipmentInOutLine.PROPERTY_LINENO);
      qry.append(" , sum(tc." + TransactionCost.PROPERTY_COST + ")");
    }

    Query qryReceiptCosts = OBDal.getInstance().getSession().createQuery(qry.toString());
    qryReceiptCosts.setParameter("landedCost", landedCost.getId());

    return qryReceiptCosts.scroll();
  }

  private BigDecimal getConvertedAmount(BigDecimal trxAmt, String strCurFromId, String strCurToId,
      Date dateReference, String strOrgId) {
    return FinancialUtils.getConvertedAmount(trxAmt,
        OBDal.getInstance().get(Currency.class, strCurFromId),
        OBDal.getInstance().get(Currency.class, strCurToId), dateReference, OBDal.getInstance()
            .get(Organization.class, strOrgId), "C");
  }
}
