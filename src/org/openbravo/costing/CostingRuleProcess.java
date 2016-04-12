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
 * All portions are Copyright (C) 2012-2016 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 *************************************************************************
 */
package org.openbravo.costing;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.time.DateUtils;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.security.OrganizationStructureProvider;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.financial.FinancialUtils;
import org.openbravo.materialmgmt.InventoryCountProcess;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.Locator;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.model.common.plm.AttributeSetInstance;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.common.plm.ProductUOM;
import org.openbravo.model.common.uom.UOM;
import org.openbravo.model.materialmgmt.cost.CostingRule;
import org.openbravo.model.materialmgmt.cost.CostingRuleInit;
import org.openbravo.model.materialmgmt.cost.TransactionCost;
import org.openbravo.model.materialmgmt.transaction.InventoryCount;
import org.openbravo.model.materialmgmt.transaction.InventoryCountLine;
import org.openbravo.model.materialmgmt.transaction.MaterialTransaction;
import org.openbravo.scheduling.Process;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessLogger;
import org.openbravo.service.db.DbUtility;

public class CostingRuleProcess implements Process {
  private ProcessLogger logger;
  private static final Logger log4j = Logger.getLogger(CostingRuleProcess.class);

  @Override
  public void execute(ProcessBundle bundle) throws Exception {
    logger = bundle.getLogger();
    OBError msg = new OBError();
    msg.setType("Success");
    msg.setTitle(OBMessageUtils.messageBD("Success"));
    try {
      OBContext.setAdminMode(false);
      final String ruleId = (String) bundle.getParams().get("M_Costing_Rule_ID");
      CostingRule rule = OBDal.getInstance().get(CostingRule.class, ruleId);
      if (rule.getOrganization().getCurrency() == null) {
        throw new OBException("@NoCurrencyInCostingRuleOrg@");
      }
      OrganizationStructureProvider osp = OBContext.getOBContext()
          .getOrganizationStructureProvider(rule.getClient().getId());
      final Set<String> childOrgs = osp.getChildTree(rule.getOrganization().getId(), true);
      final Set<String> naturalOrgs = osp.getNaturalTree(rule.getOrganization().getId());

      // Checks
      migrationCheck();
      CostingRule prevCostingRule = getPreviousRule(rule);
      boolean existsPreviousRule = prevCostingRule != null;
      boolean existsTransactions = existsTransactions(naturalOrgs, childOrgs);
      if (existsPreviousRule) {
        // Product with costing rule. All trx must be calculated.
        checkAllTrxCalculated(naturalOrgs, childOrgs);
      } else if (existsTransactions) {
        // Product configured to have cost not calculated cannot have transactions with cost
        // calculated.
        checkNoTrxWithCostCalculated(naturalOrgs, childOrgs);
        if (rule.getStartingDate() != null) {
          // First rule of an instance that does not need migration. Old transactions costs are not
          // calculated. They are initialized with ZERO cost.
          initializeOldTrx(childOrgs, rule.getStartingDate());
        }
      }
      // Inventories are only needed:
      // - if the costing rule is updating a previous rule
      // - or legacy cost was never used and the first validated rule has a starting date different
      // than null. If the date is old enough that there are not prior transactions no inventories
      // are created.
      if (existsPreviousRule || rule.getStartingDate() != null) {
        Date startingDate = rule.getStartingDate();
        if (existsPreviousRule) {
          // Set valid from date
          startingDate = DateUtils.truncate(new Date(), Calendar.SECOND);
          rule.setStartingDate(startingDate);
          log4j.debug("setting starting date " + startingDate);
          prevCostingRule.setEndingDate(startingDate);
          OBDal.getInstance().save(prevCostingRule);
          OBDal.getInstance().flush();
        }
        if (rule.getFixbackdatedfrom() == null && rule.isBackdatedTransactionsFixed()) {
          rule.setFixbackdatedfrom(startingDate);
        }
        createCostingRuleInits(ruleId, childOrgs, startingDate);

        // Update cost of inventories and process starting physical inventories.
        updateInventoriesCostAndProcessInitInventories(ruleId, startingDate, existsPreviousRule);
      }

      if (rule.getStartingDate() != null && rule.getFixbackdatedfrom() != null
          && rule.isBackdatedTransactionsFixed()
          && rule.getFixbackdatedfrom().before(rule.getStartingDate())) {
        throw new OBException("@FixBackdateFromBeforeStartingDate@");
      }

      // Reload rule after possible session clear.
      rule = OBDal.getInstance().get(CostingRule.class, ruleId);
      rule.setValidated(true);
      CostingStatus.getInstance().setMigrated();
      OBDal.getInstance().save(rule);
    } catch (final OBException e) {
      OBDal.getInstance().rollbackAndClose();
      String resultMsg = OBMessageUtils.parseTranslation(e.getMessage());
      logger.log(resultMsg);
      log4j.error(e);
      msg.setType("Error");
      msg.setTitle(OBMessageUtils.messageBD("Error"));
      msg.setMessage(resultMsg);
      bundle.setResult(msg);

    } catch (final Exception e) {
      OBDal.getInstance().rollbackAndClose();
      String message = DbUtility.getUnderlyingSQLException(e).getMessage();
      logger.log(message);
      log4j.error(message, e);
      msg.setType("Error");
      msg.setTitle(OBMessageUtils.messageBD("Error"));
      msg.setMessage(message);
      bundle.setResult(msg);
    } finally {
      OBContext.restorePreviousMode();
    }
    bundle.setResult(msg);
  }

  private void migrationCheck() {
    if (!CostingStatus.getInstance().isMigrated()) {
      throw new OBException("@CostMigrationNotDone@");
    }
  }

  private CostingRule getPreviousRule(CostingRule rule) {
    StringBuffer where = new StringBuffer();
    where.append(" as cr");
    where.append(" where cr." + CostingRule.PROPERTY_ORGANIZATION + " = :ruleOrg");
    where.append("   and cr." + CostingRule.PROPERTY_VALIDATED + " = true");
    where.append("   order by cr." + CostingRule.PROPERTY_STARTINGDATE + " desc");

    OBQuery<CostingRule> crQry = OBDal.getInstance().createQuery(CostingRule.class,
        where.toString());
    crQry.setFilterOnReadableOrganization(false);
    crQry.setNamedParameter("ruleOrg", rule.getOrganization());
    crQry.setMaxResult(1);
    return crQry.uniqueResult();
  }

  private boolean existsTransactions(Set<String> naturalOrgs, Set<String> childOrgs) {
    StringBuffer where = new StringBuffer();
    where.append(" as p");
    where.append(" where p." + Product.PROPERTY_PRODUCTTYPE + " = 'I'");
    where.append("   and p." + Product.PROPERTY_STOCKED + " = true");
    where.append("   and p." + Product.PROPERTY_ORGANIZATION + ".id in (:porgs)");
    where.append("   and exists (select 1 from " + MaterialTransaction.ENTITY_NAME);
    where.append("     where " + MaterialTransaction.PROPERTY_PRODUCT + " = p");
    where
        .append("      and " + MaterialTransaction.PROPERTY_ORGANIZATION + " .id in (:childOrgs))");

    OBQuery<Product> pQry = OBDal.getInstance().createQuery(Product.class, where.toString());
    pQry.setFilterOnReadableOrganization(false);
    pQry.setNamedParameter("porgs", naturalOrgs);
    pQry.setNamedParameter("childOrgs", childOrgs);
    return pQry.count() > 0;
  }

  private void checkAllTrxCalculated(Set<String> naturalOrgs, Set<String> childOrgs) {
    StringBuffer where = new StringBuffer();
    where.append(" as p");
    where.append(" where p." + Product.PROPERTY_PRODUCTTYPE + " = 'I'");
    where.append("  and p." + Product.PROPERTY_STOCKED + " = true");
    where.append("  and p." + Product.PROPERTY_ORGANIZATION + ".id in (:porgs)");
    where.append("  and exists (select 1 from " + MaterialTransaction.ENTITY_NAME + " as trx ");
    where.append("   where trx." + MaterialTransaction.PROPERTY_PRODUCT + " = p");
    where.append("     and trx." + MaterialTransaction.PROPERTY_ORGANIZATION
        + ".id in (:childOrgs)");
    where.append("     and trx." + MaterialTransaction.PROPERTY_ISCOSTCALCULATED + " = false");
    where.append("   )");
    OBQuery<Product> pQry = OBDal.getInstance().createQuery(Product.class, where.toString());
    pQry.setFilterOnReadableOrganization(false);
    pQry.setNamedParameter("porgs", naturalOrgs);
    pQry.setNamedParameter("childOrgs", childOrgs);
    if (pQry.count() > 0) {
      throw new OBException("@TrxWithCostNoCalculated@");
    }
  }

  private void checkNoTrxWithCostCalculated(Set<String> naturalOrgs, Set<String> childOrgs) {
    StringBuffer where = new StringBuffer();
    where.append(" as p");
    where.append(" where p." + Product.PROPERTY_PRODUCTTYPE + " = 'I'");
    where.append("  and p." + Product.PROPERTY_STOCKED + " = true");
    where.append("  and p." + Product.PROPERTY_ORGANIZATION + ".id in (:porgs)");
    where.append("  and exists (select 1 from " + MaterialTransaction.ENTITY_NAME + " as trx ");
    where.append("   where trx." + MaterialTransaction.PROPERTY_PRODUCT + " = p");
    where.append("     and trx." + MaterialTransaction.PROPERTY_ISCOSTCALCULATED + " = true");
    where.append("     and trx." + MaterialTransaction.PROPERTY_ORGANIZATION
        + ".id in (:childOrgs)");
    where.append("   )");
    OBQuery<Product> pQry = OBDal.getInstance().createQuery(Product.class, where.toString());
    pQry.setFilterOnReadableOrganization(false);
    pQry.setNamedParameter("porgs", naturalOrgs);
    pQry.setNamedParameter("childOrgs", childOrgs);
    if (pQry.count() > 0) {
      throw new OBException("@ProductsWithTrxCalculated@");
    }
  }

  private void initializeOldTrx(Set<String> childOrgs, Date date) {
    StringBuffer where = new StringBuffer();
    where.append(" where " + MaterialTransaction.PROPERTY_ORGANIZATION + ".id in (:orgs)");
    where.append("   and " + MaterialTransaction.PROPERTY_MOVEMENTDATE + " < :date");
    OBQuery<MaterialTransaction> trxQry = OBDal.getInstance().createQuery(
        MaterialTransaction.class, where.toString());
    trxQry.setFilterOnReadableOrganization(false);
    trxQry.setNamedParameter("orgs", childOrgs);
    trxQry.setNamedParameter("date", date);
    trxQry.setFetchSize(1000);
    ScrollableResults trxs = trxQry.scroll(ScrollMode.FORWARD_ONLY);
    int i = 1;
    try {
      while (trxs.next()) {
        MaterialTransaction trx = (MaterialTransaction) trxs.get(0);

        TransactionCost transactionCost = OBProvider.getInstance().get(TransactionCost.class);
        transactionCost.setInventoryTransaction(trx);
        transactionCost.setCostDate(trx.getTransactionProcessDate());
        transactionCost.setClient(trx.getClient());
        transactionCost.setOrganization(trx.getOrganization());
        transactionCost.setCost(BigDecimal.ZERO);
        transactionCost.setCurrency(trx.getClient().getCurrency());
        transactionCost.setAccountingDate(trx.getGoodsShipmentLine() != null ? trx
            .getGoodsShipmentLine().getShipmentReceipt().getAccountingDate() : trx
            .getMovementDate());
        List<TransactionCost> trxCosts = trx.getTransactionCostList();
        trxCosts.add(transactionCost);
        trx.setTransactionCostList(trxCosts);

        trx.setCostCalculated(true);
        trx.setCostingStatus("CC");
        trx.setTransactionCost(BigDecimal.ZERO);
        trx.setCurrency(trx.getClient().getCurrency());
        trx.setProcessed(true);
        OBDal.getInstance().save(trx);

        if ((i % 100) == 0) {
          OBDal.getInstance().flush();
          OBDal.getInstance().getSession().clear();
        }
        i++;
      }
    } finally {
      trxs.close();
    }
  }

  @Deprecated
  protected void createCostingRuleInits(CostingRule rule, Set<String> childOrgs) {
    createCostingRuleInits(rule.getId(), childOrgs, null);
  }

  protected void createCostingRuleInits(String ruleId, Set<String> childOrgs, Date date) {
    CostingRule rule = OBDal.getInstance().get(CostingRule.class, ruleId);
    ScrollableResults stockLines = getStockLines(childOrgs, date);
    // The key of the Map is the concatenation of orgId and warehouseId
    Map<String, String> initLines = new HashMap<String, String>();
    Map<String, Long> maxLineNumbers = new HashMap<String, Long>();
    InventoryCountLine closingInventoryLine = null;
    InventoryCountLine openInventoryLine = null;
    int i = 1;
    try {
      while (stockLines.next()) {
        Object[] stockLine = stockLines.get();
        String productId = (String) stockLine[0];
        String attrSetInsId = (String) stockLine[1];
        String uomId = (String) stockLine[2];
        String orderUOMId = (String) stockLine[3];
        String locatorId = (String) stockLine[4];
        String warehouseId = (String) stockLine[5];
        BigDecimal qty = (BigDecimal) stockLine[6];
        BigDecimal orderQty = (BigDecimal) stockLine[7];

        String criId = initLines.get(warehouseId);
        CostingRuleInit cri = null;
        if (criId == null) {
          cri = createCostingRuleInitLine(rule, warehouseId, date);

          initLines.put(warehouseId, cri.getId());
        } else {
          cri = OBDal.getInstance().get(CostingRuleInit.class, criId);
        }
        Long lineNo = (maxLineNumbers.get(criId) == null ? 0L : maxLineNumbers.get(criId)) + 10L;
        maxLineNumbers.put(criId, lineNo);

        if (BigDecimal.ZERO.compareTo(qty) < 0) {
          // Do not insert negative values in Inventory lines, instead reverse the Quantity Count
          // and the Book Quantity. For example:
          // Instead of CountQty=0 and BookQty=-5 insert CountQty=5 and BookQty=0
          // By doing so the difference between both quantities remains the same and no negative
          // values have been inserted.

          openInventoryLine = insertInventoryLine(cri.getInitInventory(), productId, attrSetInsId,
              uomId, orderUOMId, locatorId, qty, BigDecimal.ZERO, orderQty, BigDecimal.ZERO,
              lineNo, null);
          insertInventoryLine(cri.getCloseInventory(), productId, attrSetInsId, uomId, orderUOMId,
              locatorId, BigDecimal.ZERO, qty, BigDecimal.ZERO, orderQty, lineNo, openInventoryLine);

        } else {
          openInventoryLine = insertInventoryLine(cri.getInitInventory(), productId, attrSetInsId,
              uomId, orderUOMId, locatorId, BigDecimal.ZERO, qty.abs(), BigDecimal.ZERO,
              orderQty == null ? null : orderQty.abs(), lineNo, closingInventoryLine);
          insertInventoryLine(cri.getCloseInventory(), productId, attrSetInsId, uomId, orderUOMId,
              locatorId, qty == null ? null : qty.abs(), BigDecimal.ZERO, orderQty == null ? null
                  : orderQty.abs(), BigDecimal.ZERO, lineNo, openInventoryLine);

        }

        if ((i % 100) == 0) {
          OBDal.getInstance().flush();
          OBDal.getInstance().getSession().clear();
          // Reload rule after clear session.
          rule = OBDal.getInstance().get(CostingRule.class, ruleId);
        }
        i++;
      }
    } finally {
      stockLines.close();
    }
    // Process closing physical inventories.
    for (CostingRuleInit cri : rule.getCostingRuleInitList()) {
      new InventoryCountProcess().processInventory(cri.getCloseInventory(), false);
    }
  }

  private ScrollableResults getStockLines(Set<String> childOrgs, Date date) {
    StringBuffer select = new StringBuffer();
    select.append("select trx." + MaterialTransaction.PROPERTY_PRODUCT + ".id");
    select.append(", trx." + MaterialTransaction.PROPERTY_ATTRIBUTESETVALUE + ".id");
    select.append(", trx." + MaterialTransaction.PROPERTY_UOM + ".id");
    select.append(", trx." + MaterialTransaction.PROPERTY_ORDERUOM + ".id");
    select.append(", trx." + MaterialTransaction.PROPERTY_STORAGEBIN + ".id");
    select.append(", loc." + Locator.PROPERTY_WAREHOUSE + ".id");
    select.append(", sum(trx." + MaterialTransaction.PROPERTY_MOVEMENTQUANTITY + ")");
    select.append(", sum(trx." + MaterialTransaction.PROPERTY_ORDERQUANTITY + ")");
    select.append(" from " + MaterialTransaction.ENTITY_NAME + " as trx");
    select.append("    join trx." + MaterialTransaction.PROPERTY_STORAGEBIN + " as loc");
    select.append(" where trx." + MaterialTransaction.PROPERTY_ORGANIZATION + ".id in (:orgs)");
    if (date != null) {
      select.append("   and trx." + MaterialTransaction.PROPERTY_MOVEMENTDATE + " < :date");
    }
    select.append("   and trx." + MaterialTransaction.PROPERTY_PRODUCT + ".productType = 'I'");
    select.append("   and trx." + MaterialTransaction.PROPERTY_PRODUCT + ".stocked = true");
    select.append(" group by trx." + MaterialTransaction.PROPERTY_PRODUCT + ".id");
    select.append(", trx." + MaterialTransaction.PROPERTY_ATTRIBUTESETVALUE + ".id");
    select.append(", trx." + MaterialTransaction.PROPERTY_UOM + ".id");
    select.append(", trx." + MaterialTransaction.PROPERTY_ORDERUOM + ".id");
    select.append(", trx." + MaterialTransaction.PROPERTY_STORAGEBIN + ".id");
    select.append(", loc." + Locator.PROPERTY_WAREHOUSE + ".id");
    select.append(" having ");
    select.append(" sum(trx." + MaterialTransaction.PROPERTY_MOVEMENTQUANTITY + ") <> 0");
    select.append(" or sum(trx." + MaterialTransaction.PROPERTY_ORDERQUANTITY + ") <> 0");
    select.append(" order by loc." + Locator.PROPERTY_WAREHOUSE + ".id");
    select.append(", trx." + MaterialTransaction.PROPERTY_PRODUCT + ".id");
    select.append(", trx." + MaterialTransaction.PROPERTY_STORAGEBIN + ".id");
    select.append(", trx." + MaterialTransaction.PROPERTY_ATTRIBUTESETVALUE + ".id");
    select.append(", trx." + MaterialTransaction.PROPERTY_UOM + ".id");
    select.append(", trx." + MaterialTransaction.PROPERTY_ORDERUOM + ".id");

    Query stockLinesQry = OBDal.getInstance().getSession().createQuery(select.toString());
    stockLinesQry.setParameterList("orgs", childOrgs);
    if (date != null) {
      stockLinesQry.setTimestamp("date", date);
    }
    stockLinesQry.setFetchSize(1000);
    ScrollableResults stockLines = stockLinesQry.scroll(ScrollMode.FORWARD_ONLY);
    return stockLines;
  }

  private CostingRuleInit createCostingRuleInitLine(CostingRule rule, String warehouseId, Date date) {
    Date localDate = date;
    if (localDate == null) {
      localDate = new Date();
    }
    String clientId = (String) DalUtil.getId(rule.getClient());
    String orgId = (String) DalUtil.getId(rule.getOrganization());
    CostingRuleInit cri = OBProvider.getInstance().get(CostingRuleInit.class);
    cri.setClient((Client) OBDal.getInstance().getProxy(Client.ENTITY_NAME, clientId));
    cri.setOrganization((Organization) OBDal.getInstance()
        .getProxy(Organization.ENTITY_NAME, orgId));
    cri.setWarehouse((Warehouse) OBDal.getInstance().getProxy(Warehouse.ENTITY_NAME, warehouseId));
    cri.setCostingRule(rule);
    List<CostingRuleInit> criList = rule.getCostingRuleInitList();
    criList.add(cri);
    rule.setCostingRuleInitList(criList);

    InventoryCount closeInv = OBProvider.getInstance().get(InventoryCount.class);
    closeInv.setClient((Client) OBDal.getInstance().getProxy(Client.ENTITY_NAME, clientId));
    closeInv.setOrganization((Organization) OBDal.getInstance().getProxy(Organization.ENTITY_NAME,
        orgId));
    closeInv.setName(OBMessageUtils.messageBD("CostCloseInventory"));
    closeInv.setWarehouse((Warehouse) OBDal.getInstance().getProxy(Warehouse.ENTITY_NAME,
        warehouseId));
    closeInv.setMovementDate(localDate);
    closeInv.setInventoryType("C");
    cri.setCloseInventory(closeInv);

    InventoryCount initInv = OBProvider.getInstance().get(InventoryCount.class);
    initInv.setClient((Client) OBDal.getInstance().getProxy(Client.ENTITY_NAME, clientId));
    initInv.setOrganization((Organization) OBDal.getInstance().getProxy(Organization.ENTITY_NAME,
        orgId));
    initInv.setName(OBMessageUtils.messageBD("CostInitInventory"));
    initInv.setWarehouse((Warehouse) OBDal.getInstance().getProxy(Warehouse.ENTITY_NAME,
        warehouseId));
    initInv.setMovementDate(localDate);
    initInv.setInventoryType("O");
    cri.setInitInventory(initInv);
    OBDal.getInstance().save(rule);
    OBDal.getInstance().save(closeInv);
    OBDal.getInstance().save(initInv);

    OBDal.getInstance().flush();

    return cri;
  }

  private InventoryCountLine insertInventoryLine(InventoryCount inventory, String productId,
      String attrSetInsId, String uomId, String orderUOMId, String locatorId, BigDecimal qtyCount,
      BigDecimal qtyBook, BigDecimal orderQtyCount, BigDecimal orderQtyBook, Long lineNo,
      InventoryCountLine relatedInventoryLine) {
    InventoryCountLine icl = OBProvider.getInstance().get(InventoryCountLine.class);
    icl.setClient(inventory.getClient());
    icl.setOrganization(inventory.getOrganization());
    icl.setPhysInventory(inventory);
    icl.setLineNo(lineNo);
    icl.setStorageBin((Locator) OBDal.getInstance().getProxy(Locator.ENTITY_NAME, locatorId));
    icl.setProduct((Product) OBDal.getInstance().getProxy(Product.ENTITY_NAME, productId));
    icl.setAttributeSetValue((AttributeSetInstance) OBDal.getInstance().getProxy(
        AttributeSetInstance.ENTITY_NAME, attrSetInsId));
    icl.setQuantityCount(qtyCount);
    icl.setBookQuantity(qtyBook);
    icl.setUOM((UOM) OBDal.getInstance().getProxy(UOM.ENTITY_NAME, uomId));
    if (orderUOMId != null) {
      icl.setOrderQuantity(orderQtyCount);
      icl.setQuantityOrderBook(orderQtyBook);
      icl.setOrderUOM((ProductUOM) OBDal.getInstance().getProxy(ProductUOM.ENTITY_NAME, orderUOMId));
    }
    icl.setRelatedInventory(relatedInventoryLine);
    List<InventoryCountLine> invLines = inventory.getMaterialMgmtInventoryCountLineList();
    invLines.add(icl);
    inventory.setMaterialMgmtInventoryCountLineList(invLines);
    OBDal.getInstance().save(inventory);
    OBDal.getInstance().flush();
    return icl;
  }

  private void updateInventoriesCostAndProcessInitInventories(String ruleId, Date startingDate,
      boolean existsPreviousRule) {
    CostingRule rule = OBDal.getInstance().get(CostingRule.class, ruleId);
    for (CostingRuleInit cri : rule.getCostingRuleInitList()) {
      for (InventoryCountLine icl : cri.getCloseInventory().getMaterialMgmtInventoryCountLineList()) {
        MaterialTransaction trx = getInventoryLineTransaction(icl);
        // Remove 1 second from transaction date to ensure that cost is calculated with previous
        // costing rule.
        trx.setTransactionProcessDate(DateUtils.addSeconds(startingDate, -1));
        BigDecimal trxCost = BigDecimal.ZERO;
        BigDecimal cost = null;
        Currency cur = FinancialUtils.getLegalEntityCurrency(trx.getOrganization());
        if (existsPreviousRule) {
          trxCost = CostingUtils.getTransactionCost(trx, startingDate, true, cur);
          if (trx.getMovementQuantity().compareTo(BigDecimal.ZERO) != 0) {
            cost = trxCost.divide(trx.getMovementQuantity().abs(), cur.getCostingPrecision()
                .intValue(), RoundingMode.HALF_UP);
            trx = OBDal.getInstance().get(MaterialTransaction.class, trx.getId());
          }
        } else {
          // Insert transaction cost record big ZERO cost.
          cur = trx.getClient().getCurrency();
          TransactionCost transactionCost = OBProvider.getInstance().get(TransactionCost.class);
          transactionCost.setInventoryTransaction(trx);
          transactionCost.setCostDate(trx.getTransactionProcessDate());
          transactionCost.setClient(trx.getClient());
          transactionCost.setOrganization(trx.getOrganization());
          transactionCost.setCost(BigDecimal.ZERO);
          transactionCost.setCurrency(trx.getClient().getCurrency());
          transactionCost.setAccountingDate(trx.getGoodsShipmentLine() != null ? trx
              .getGoodsShipmentLine().getShipmentReceipt().getAccountingDate() : trx
              .getMovementDate());
          List<TransactionCost> trxCosts = trx.getTransactionCostList();
          trxCosts.add(transactionCost);
          trx.setTransactionCostList(trxCosts);
          OBDal.getInstance().save(trx);
        }

        trx.setCostCalculated(true);
        trx.setCostingStatus("CC");
        trx.setCurrency(cur);
        trx.setTransactionCost(trxCost);
        trx.setProcessed(true);
        OBDal.getInstance().save(trx);
        icl = OBDal.getInstance().get(InventoryCountLine.class, icl.getId());
        InventoryCountLine initICL = getInitIcl(cri.getInitInventory(), icl);
        initICL.setCost(cost);
        OBDal.getInstance().save(initICL);
      }
      OBDal.getInstance().flush();
      new InventoryCountProcess().processInventory(cri.getInitInventory(), false);
    }
    if (!existsPreviousRule) {
      updateInitInventoriesTrxDate(startingDate, ruleId);
    }
  }

  protected MaterialTransaction getInventoryLineTransaction(InventoryCountLine icl) {
    OBQuery<MaterialTransaction> trxQry = OBDal.getInstance().createQuery(
        MaterialTransaction.class,
        MaterialTransaction.PROPERTY_PHYSICALINVENTORYLINE + ".id = :invline");
    trxQry.setFilterOnReadableClients(false);
    trxQry.setFilterOnReadableOrganization(false);
    trxQry.setNamedParameter("invline", icl.getId());
    MaterialTransaction trx = trxQry.uniqueResult();
    return trx;
  }

  protected InventoryCountLine getInitIcl(InventoryCount initInventory, InventoryCountLine icl) {
    StringBuffer where = new StringBuffer();
    where.append(InventoryCountLine.PROPERTY_PHYSINVENTORY + ".id = :inventory");
    where.append(" and " + InventoryCountLine.PROPERTY_PRODUCT + ".id = :product");
    where.append(" and " + InventoryCountLine.PROPERTY_ATTRIBUTESETVALUE + ".id = :asi");
    where.append(" and " + InventoryCountLine.PROPERTY_STORAGEBIN + ".id = :locator");
    if (icl.getOrderUOM() == null) {
      where.append(" and " + InventoryCountLine.PROPERTY_ORDERUOM + " is null");
    } else {
      where.append(" and " + InventoryCountLine.PROPERTY_ORDERUOM + ".id = :orderuom");
    }
    OBQuery<InventoryCountLine> iclQry = OBDal.getInstance().createQuery(InventoryCountLine.class,
        where.toString());
    iclQry.setFilterOnReadableClients(false);
    iclQry.setFilterOnReadableOrganization(false);
    iclQry.setNamedParameter("inventory", initInventory.getId());
    iclQry.setNamedParameter("product", icl.getProduct().getId());
    iclQry.setNamedParameter("asi", icl.getAttributeSetValue().getId());
    iclQry.setNamedParameter("locator", icl.getStorageBin().getId());
    if (icl.getOrderUOM() != null) {
      iclQry.setNamedParameter("orderuom", icl.getOrderUOM().getId());
    }
    return iclQry.uniqueResult();
  }

  private void updateInitInventoriesTrxDate(Date startingDate, String ruleId) {
    StringBuffer where = new StringBuffer();
    where.append(" as trx");
    where.append("   join trx." + MaterialTransaction.PROPERTY_PHYSICALINVENTORYLINE + " as il");
    where.append(" where il." + InventoryCountLine.PROPERTY_PHYSINVENTORY + ".id IN (");
    where.append("    select cri." + CostingRuleInit.PROPERTY_INITINVENTORY + ".id");
    where.append("    from " + CostingRuleInit.ENTITY_NAME + " as cri");
    where.append("    where cri." + CostingRuleInit.PROPERTY_COSTINGRULE + ".id = :cr");
    where.append("    )");
    OBQuery<MaterialTransaction> trxQry = OBDal.getInstance().createQuery(
        MaterialTransaction.class, where.toString());
    trxQry.setNamedParameter("cr", ruleId);
    trxQry.setFetchSize(1000);
    ScrollableResults trxs = trxQry.scroll(ScrollMode.FORWARD_ONLY);
    int i = 0;
    while (trxs.next()) {
      MaterialTransaction trx = (MaterialTransaction) trxs.get(0);
      trx.setTransactionProcessDate(startingDate);
      OBDal.getInstance().save(trx);
      if ((i % 100) == 0) {
        OBDal.getInstance().flush();
        OBDal.getInstance().getSession().clear();
      }
    }
    trxs.close();
  }
}
