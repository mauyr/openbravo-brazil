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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.time.DateUtils;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.criterion.Restrictions;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.type.DateType;
import org.hibernate.type.StringType;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.core.SessionHandler;
import org.openbravo.dal.security.OrganizationStructureProvider;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.ad_forms.ProductInfo;
import org.openbravo.erpCommon.utility.OBDateUtils;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.financial.FinancialUtils;
import org.openbravo.model.ad.access.ProcessAccess;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.alert.Alert;
import org.openbravo.model.ad.alert.AlertRecipient;
import org.openbravo.model.ad.alert.AlertRule;
import org.openbravo.model.ad.domain.Preference;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.OrganizationType;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.financialmgmt.calendar.Period;
import org.openbravo.model.financialmgmt.calendar.PeriodControl;
import org.openbravo.model.materialmgmt.cost.Costing;
import org.openbravo.model.materialmgmt.cost.CostingAlgorithm;
import org.openbravo.model.materialmgmt.cost.CostingRule;
import org.openbravo.model.materialmgmt.cost.CostingRuleInit;
import org.openbravo.model.materialmgmt.cost.TransactionCost;
import org.openbravo.model.materialmgmt.onhandquantity.StorageDetail;
import org.openbravo.model.materialmgmt.transaction.InventoryCount;
import org.openbravo.model.materialmgmt.transaction.InventoryCountLine;
import org.openbravo.model.materialmgmt.transaction.MaterialTransaction;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;
import org.openbravo.scheduling.Process;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessLogger;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.service.db.DbUtility;

public class CostingMigrationProcess implements Process {
  private ProcessLogger logger;
  private static final Logger log4j = Logger.getLogger(CostingMigrationProcess.class);
  private static CostingAlgorithm averageAlgorithm = null;
  private static final String alertRuleName = "Products with transactions without available cost on date.";
  private static final String pareto = "75F83D534E764C7C8781FFA6C08E87ED";
  private static final String mUpdatePareto = "9CD67D41E43242CDA034FB994B75812A";
  private static final String valued = "E5BE98DCF4514A18B571F21183B397DD";
  private static final String dimensional = "6D3B1C36BF594A51878281B505F6CECF";
  private static final String paretoLegacy = "1000500000";
  private static final String mUpdateParetoLegacy = "1000500001";
  private static final String valuedLegacy = "800088";
  private static final String dimensionalLegacy = "800205";
  private static final String processEntity = org.openbravo.model.ad.ui.Process.ENTITY_NAME;

  @Override
  public void execute(ProcessBundle bundle) throws Exception {

    logger = bundle.getLogger();
    OBError msg = new OBError();
    msg.setType("Success");
    msg.setTitle(OBMessageUtils.messageBD("Success"));
    try {
      OBContext.setAdminMode(false);

      if (CostingStatus.getInstance().isMigrated()) {
        throw new OBException("@CostMigratedInstance@");
      }

      // FIXME: Remove when HQL based inserts are removed.
      OBDal.getInstance().registerSQLFunction("get_uuid",
          new StandardSQLFunction("get_uuid", new StringType()));
      OBDal.getInstance()
          .registerSQLFunction("now", new StandardSQLFunction("now", new DateType()));

      if (!isMigrationFirstPhaseCompleted()) {
        doChecks();

        updateLegacyCosts();
        createRules();
        createMigrationFirstPhaseCompletedPreference();
      } else {
        checkAllInventoriesAreProcessed();
        for (CostingRule rule : getRules()) {
          rule.setValidated(true);
          OBDal.getInstance().save(rule);
        }
        deleteAlertRule();
        updateReportRoles();
        CostingStatus.getInstance().setMigrated();
        deleteMigrationFirstPhaseCompletedPreference();

      }
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

  private void doChecks() {
    // Check all transactions have a legacy cost available.
    AlertRule legacyCostAvailableAlert = getLegacyCostAvailableAlert();
    if (legacyCostAvailableAlert == null) {
      Organization org0 = OBDal.getInstance().get(Organization.class, "0");
      Client client0 = OBDal.getInstance().get(Client.class, "0");

      legacyCostAvailableAlert = OBProvider.getInstance().get(AlertRule.class);
      legacyCostAvailableAlert.setClient(client0);
      legacyCostAvailableAlert.setOrganization(org0);
      legacyCostAvailableAlert.setName(alertRuleName);
      // Header tab of Product window
      legacyCostAvailableAlert.setTab(OBDal.getInstance().get(org.openbravo.model.ad.ui.Tab.class,
          "180"));
      StringBuffer sql = new StringBuffer();
      sql.append("select t.m_product_id as referencekey_id, '0' as ad_role_id, null as ad_user_id,");
      sql.append("\n    'Product ' || p.name || ' has transactions on dates without available");
      sql.append(" costs. Min date ' || min(t.movementdate) || '. Max date ' || max(t.movementdate)");
      sql.append(" as description,");
      sql.append("\n    'Y' as isactive, p.ad_org_id, p.ad_client_id,");
      sql.append("\n    now() as created, '0' as createdby, now() as updated, '0' as updatedby,");
      sql.append("\n    p.name as record_id");
      sql.append("\nfrom m_transaction t join m_product p on t.m_product_id = p.m_product_id");
      sql.append("\nwhere not exists (select 1 from m_costing c ");
      sql.append("\n                  where t.isactive = 'Y'");
      sql.append("\n                    and t.m_product_id = c.m_product_id");
      sql.append("\n                    and t.movementdate >= c.datefrom");
      sql.append("\n                    and t.movementdate < c.dateto");
      sql.append("\n                    and c.cost is not null)");
      sql.append("\ngroup by t.m_product_id, p.ad_org_id, p.ad_client_id, p.name");
      legacyCostAvailableAlert.setSql(sql.toString());

      OBDal.getInstance().save(legacyCostAvailableAlert);
      OBDal.getInstance().flush();

      insertAlertRecipients(legacyCostAvailableAlert);
    }

    // Delete previous alerts
    StringBuffer delete = new StringBuffer();
    delete.append("delete from " + Alert.ENTITY_NAME);
    delete.append(" where " + Alert.PROPERTY_ALERTRULE + " = :alertRule ");
    Query queryDelete = OBDal.getInstance().getSession().createQuery(delete.toString());
    queryDelete.setEntity("alertRule", legacyCostAvailableAlert);
    queryDelete.executeUpdate();

    if (legacyCostAvailableAlert.isActive()) {

      SQLQuery alertQry = OBDal.getInstance().getSession()
          .createSQLQuery(legacyCostAvailableAlert.getSql());
      alertQry.addScalar("REFERENCEKEY_ID", StringType.INSTANCE);
      alertQry.addScalar("AD_ROLE_ID", StringType.INSTANCE);
      alertQry.addScalar("AD_USER_ID", StringType.INSTANCE);
      alertQry.addScalar("DESCRIPTION", StringType.INSTANCE);
      alertQry.addScalar("ISACTIVE", StringType.INSTANCE);
      alertQry.addScalar("AD_ORG_ID", StringType.INSTANCE);
      alertQry.addScalar("AD_CLIENT_ID", StringType.INSTANCE);
      alertQry.addScalar("CREATED", DateType.INSTANCE);
      alertQry.addScalar("CREATEDBY", StringType.INSTANCE);
      alertQry.addScalar("UPDATED", DateType.INSTANCE);
      alertQry.addScalar("UPDATEDBY", StringType.INSTANCE);
      alertQry.addScalar("RECORD_ID", StringType.INSTANCE);
      List<?> rows = alertQry.list();
      for (final Object row : rows) {
        final Object[] values = (Object[]) row;
        Alert alert = OBProvider.getInstance().get(Alert.class);
        alert.setCreatedBy(OBDal.getInstance().get(User.class, "0"));
        alert.setUpdatedBy(OBDal.getInstance().get(User.class, "0"));
        alert.setClient(OBDal.getInstance().get(Client.class, values[6]));
        alert.setOrganization(OBDal.getInstance().get(Organization.class, values[5]));
        alert.setAlertRule(legacyCostAvailableAlert);
        alert.setRecordID((String) values[11]);
        alert.setReferenceSearchKey((String) values[0]);
        alert.setDescription((String) values[3]);
        alert.setUserContact(null);
        alert.setRole(OBDal.getInstance().get(org.openbravo.model.ad.access.Role.class, "0"));
        OBDal.getInstance().save(alert);
      }
      if (SessionHandler.isSessionHandlerPresent()) {
        SessionHandler.getInstance().commitAndStart();
      }
      if (rows.size() > 0) {
        throw new OBException("@TrxWithNoCost@");
      }
    }

    // Check current period is opened for Material Physical Inventory document
    OBCriteria<Organization> ohql = OBDal.getInstance().createCriteria(Organization.class);
    ohql.add(Restrictions.eq(Organization.PROPERTY_ALLOWPERIODCONTROL, true));
    ohql.setFilterOnReadableClients(false);
    ohql.setFilterOnReadableOrganization(false);
    ScrollableResults orgList = ohql.scroll(ScrollMode.FORWARD_ONLY);
    int i = 0;
    try {
      while (orgList.next()) {
        Organization organization = (Organization) orgList.get()[0];
        StringBuffer phqlWhere = new StringBuffer();
        phqlWhere.append(" as pc");
        phqlWhere.append(" join pc." + PeriodControl.PROPERTY_PERIOD + " as p");
        phqlWhere.append(" where p." + Period.PROPERTY_STARTINGDATE + " <= :date");
        phqlWhere.append(" and p." + Period.PROPERTY_ENDINGDATE + " >= :date");
        phqlWhere.append(" and pc." + PeriodControl.PROPERTY_ORGANIZATION + " = :org");
        phqlWhere.append(" and pc." + PeriodControl.PROPERTY_DOCUMENTCATEGORY + " = 'MMI'");
        phqlWhere.append(" and pc." + PeriodControl.PROPERTY_PERIODSTATUS + " = 'O'");
        final OBQuery<PeriodControl> phql = OBDal.getInstance().createQuery(PeriodControl.class,
            phqlWhere.toString());
        phql.setFilterOnReadableClients(false);
        phql.setFilterOnReadableOrganization(false);
        phql.setNamedParameter("date", new Date());
        phql.setNamedParameter("org", organization);
        phql.setMaxResult(1);
        PeriodControl period = phql.uniqueResult();
        if (period == null) {
          throw new OBException(String.format(OBMessageUtils.messageBD("PeriodClosedForMMI"),
              organization.getName()));
        }

        i++;
        if (i % 100 == 0) {
          OBDal.getInstance().flush();
          OBDal.getInstance().getSession().clear();
        }
      }
    } finally {
      orgList.close();
    }

    // Check there is not negative stock without allowing it
    for (Client client : getClients()) {
      if (!client.getClientInformationList().get(0).isAllowNegativeStock()) {
        final OBCriteria<StorageDetail> sdhql = OBDal.getInstance().createCriteria(
            StorageDetail.class);
        sdhql.add(Restrictions.eq(StorageDetail.PROPERTY_CLIENT, client));
        sdhql.add(Restrictions.or(
            Restrictions.lt(StorageDetail.PROPERTY_QUANTITYONHAND, BigDecimal.ZERO),
            Restrictions.lt(StorageDetail.PROPERTY_ONHANDORDERQUANITY, BigDecimal.ZERO)));
        sdhql.setFilterOnReadableClients(false);
        sdhql.setFilterOnReadableOrganization(false);
        sdhql.setMaxResults(1);
        StorageDetail storageDetail = (StorageDetail) sdhql.uniqueResult();
        if (storageDetail != null) {
          throw new OBException(String.format(
              OBMessageUtils.messageBD("NegativeStockWithoutAllowing"), client.getName()));
        }
      }
    }

    // Check there is not stock of a product in a UOM different from the one defined for it
    StringBuffer sdphqlWhere = new StringBuffer();
    sdphqlWhere.append(" as sd");
    sdphqlWhere.append(" join sd." + StorageDetail.PROPERTY_PRODUCT + " as p");
    sdphqlWhere.append(" where p." + Product.PROPERTY_UOM + " <> sd." + StorageDetail.PROPERTY_UOM);
    sdphqlWhere.append(" and (coalesce(sd." + StorageDetail.PROPERTY_QUANTITYONHAND + ",0) > 0");
    sdphqlWhere.append(" or coalesce(sd." + StorageDetail.PROPERTY_ONHANDORDERQUANITY + ",0) > 0)");
    final OBQuery<StorageDetail> sdphql = OBDal.getInstance().createQuery(StorageDetail.class,
        sdphqlWhere.toString());
    sdphql.setFilterOnReadableClients(false);
    sdphql.setFilterOnReadableOrganization(false);
    sdphql.setMaxResult(1);
    StorageDetail storageDetailProduct = sdphql.uniqueResult();
    if (storageDetailProduct != null) {
      throw new OBException("@ProductStockInDifferentUOM@");
    }

    // Check there are not inconsistencies between M_TRANSACTION and M_STORAGE_DETAIL tables
    StringBuffer tsdhqlWhere = new StringBuffer();
    tsdhqlWhere.append(" select 1");
    tsdhqlWhere.append(" from " + MaterialTransaction.ENTITY_NAME + " as t");
    tsdhqlWhere.append(", " + StorageDetail.ENTITY_NAME + " as sd");
    tsdhqlWhere.append(" where t." + MaterialTransaction.PROPERTY_PRODUCT + " = sd."
        + StorageDetail.PROPERTY_PRODUCT);
    tsdhqlWhere.append(" and t." + MaterialTransaction.PROPERTY_STORAGEBIN + " = sd."
        + StorageDetail.PROPERTY_STORAGEBIN);
    tsdhqlWhere.append(" and t." + MaterialTransaction.PROPERTY_ATTRIBUTESETVALUE + " = sd."
        + StorageDetail.PROPERTY_ATTRIBUTESETVALUE);
    tsdhqlWhere.append(" and t." + MaterialTransaction.PROPERTY_UOM + " = sd."
        + StorageDetail.PROPERTY_UOM);
    tsdhqlWhere.append(" and coalesce(t." + MaterialTransaction.PROPERTY_ORDERUOM
        + ", '0') = coalesce(sd." + StorageDetail.PROPERTY_ORDERUOM + ", '0')");
    tsdhqlWhere.append(" group by t." + MaterialTransaction.PROPERTY_PRODUCT);
    tsdhqlWhere.append(" , t." + MaterialTransaction.PROPERTY_STORAGEBIN);
    tsdhqlWhere.append(" , t." + MaterialTransaction.PROPERTY_ATTRIBUTESETVALUE);
    tsdhqlWhere.append(" , t." + MaterialTransaction.PROPERTY_UOM);
    tsdhqlWhere.append(" , t." + MaterialTransaction.PROPERTY_ORDERUOM);
    tsdhqlWhere.append(" , sd." + StorageDetail.PROPERTY_PRODUCT);
    tsdhqlWhere.append(" , sd." + StorageDetail.PROPERTY_STORAGEBIN);
    tsdhqlWhere.append(" , sd." + StorageDetail.PROPERTY_ATTRIBUTESETVALUE);
    tsdhqlWhere.append(" , sd." + StorageDetail.PROPERTY_UOM);
    tsdhqlWhere.append(" , sd." + StorageDetail.PROPERTY_ORDERUOM);
    tsdhqlWhere.append(" having (coalesce(sum(t." + MaterialTransaction.PROPERTY_MOVEMENTQUANTITY
        + "), 0) <> coalesce(max(sd." + StorageDetail.PROPERTY_QUANTITYONHAND + "), 0)");
    tsdhqlWhere.append(" or coalesce(sum(t." + MaterialTransaction.PROPERTY_ORDERQUANTITY
        + "), 0) <> coalesce(max(sd." + StorageDetail.PROPERTY_ONHANDORDERQUANITY + "), 0))");
    final Query tsdhql = OBDal.getInstance().getSession().createQuery(tsdhqlWhere.toString());
    tsdhql.setMaxResults(1);
    Object transactionStorageDetail = tsdhql.uniqueResult();
    if (transactionStorageDetail != null) {
      throw new OBException("@InconsistenciesInStock@");
    }

  }

  private void updateLegacyCosts() {
    log4j.debug("UpdateLegacyCosts");
    // Reset costs in m_transaction and m_transaction_cost.
    Query queryDelete = OBDal.getInstance().getSession()
        .createQuery("delete from " + TransactionCost.ENTITY_NAME);
    queryDelete.executeUpdate();

    // FIXME: Update should be done with a loop based on scroll.
    StringBuffer update = new StringBuffer();
    update.append("update " + MaterialTransaction.ENTITY_NAME);
    update.append(" set " + MaterialTransaction.PROPERTY_ISCOSTCALCULATED + " = false,");
    update.append("     " + MaterialTransaction.PROPERTY_TRANSACTIONCOST + " = null");
    update.append(" where " + MaterialTransaction.PROPERTY_TRANSACTIONCOST + " <> 0");
    update.append("   or " + MaterialTransaction.PROPERTY_ISCOSTCALCULATED + " = true");
    Query updateQry = OBDal.getInstance().getSession().createQuery(update.toString());
    updateQry.executeUpdate();
    OBDal.getInstance().flush();
    fixLegacyCostingCurrency();

    for (Client client : getClients()) {
      OrganizationStructureProvider osp = OBContext.getOBContext()
          .getOrganizationStructureProvider(client.getId());
      String clientId = client.getId();
      // Reload client entity after session cleared to avoid No Session error.
      client = OBDal.getInstance().get(Client.class, clientId);
      Currency clientCur = client.getCurrency();
      int stdPrecission = clientCur.getStandardPrecision().intValue();
      log4j.debug("** Processing client: " + client.getIdentifier() + " with currency: "
          + clientCur.getIdentifier());
      for (Organization legalEntity : osp.getLegalEntitiesList()) {
        log4j.debug("** Processing organization: " + legalEntity.getIdentifier());
        Set<String> naturalTree = osp.getNaturalTree(legalEntity.getId());
        ScrollableResults legacyCosts = getLegacyCostScroll(clientId, naturalTree);
        int i = 0;
        try {
          while (legacyCosts.next()) {
            Costing cost = (Costing) legacyCosts.get(0);

            updateTrxLegacyCosts(cost, stdPrecission, naturalTree);

            if ((i % 100) == 0) {
              OBDal.getInstance().flush();
              OBDal.getInstance().getSession().clear();
            }
          }
        } finally {
          legacyCosts.close();
        }
        SessionHandler.getInstance().commitAndStart();
      }
    }

    updateWithZeroCostRemainingTrx();

    insertTrxCosts();
    insertStandardCosts();
  }

  private void fixLegacyCostingCurrency() {
    StringBuffer where = new StringBuffer();
    where.append(" as c");
    where.append("   join c." + Costing.PROPERTY_CLIENT + " as cl");
    where.append(" where c." + Costing.PROPERTY_CURRENCY + " <> cl." + Client.PROPERTY_CURRENCY);
    final OBQuery<Costing> costQry = OBDal.getInstance().createQuery(Costing.class,
        where.toString());
    costQry.setFilterOnActive(false);
    costQry.setFilterOnReadableClients(false);
    costQry.setFilterOnReadableOrganization(false);
    costQry.setFetchSize(1000);

    final ScrollableResults costs = costQry.scroll(ScrollMode.FORWARD_ONLY);
    int i = 0;
    try {
      while (costs.next()) {
        Costing cost = (Costing) costs.get(0);
        cost.setCurrency(cost.getClient().getCurrency());
        OBDal.getInstance().save(cost);
        if ((i % 100) == 0) {
          OBDal.getInstance().flush();
          OBDal.getInstance().getSession().clear();
        }
        i++;
      }
    } finally {
      costs.close();
    }
  }

  private void createRules() throws Exception {
    // Delete manually created rules.
    Query delQry = OBDal.getInstance().getSession()
        .createQuery("delete from " + CostingRule.ENTITY_NAME);
    delQry.executeUpdate();

    List<Client> clients = getClients();
    for (Client client : clients) {
      client = OBDal.getInstance().get(Client.class, client.getId());
      OrganizationStructureProvider osp = OBContext.getOBContext()
          .getOrganizationStructureProvider(client.getId());
      for (Organization org : osp.getLegalEntitiesList()) {
        CostingRule rule = createCostingRule(org);
        processRule(rule);
      }
      for (Organization org : osp.getLegalEntitiesList()) {
        calculateCosts(org);
      }
    }

  }

  private void processRule(CostingRule rule) {
    OrganizationStructureProvider osp = OBContext.getOBContext().getOrganizationStructureProvider(
        (String) DalUtil.getId(rule.getClient()));
    final Set<String> childOrgs = osp.getChildTree(rule.getOrganization().getId(), true);
    CostingRuleProcess crp = new CostingRuleProcess();
    crp.createCostingRuleInits(rule.getId(), childOrgs, null);

    // Set valid from date
    Date startingDate = new Date();
    rule.setStartingDate(startingDate);
    log4j.debug("setting starting date " + startingDate);
    OBDal.getInstance().flush();
  }

  private void calculateCosts(Organization org) {
    Currency cur = FinancialUtils.getLegalEntityCurrency(org);
    String curId = cur.getId();
    Set<String> orgs = OBContext.getOBContext()
        .getOrganizationStructureProvider(org.getClient().getId()).getChildTree(org.getId(), true);
    String orgId = org.getId();

    int costPrecision = cur.getCostingPrecision().intValue();
    int stdPrecision = cur.getStandardPrecision().intValue();
    CostingRuleProcess crp = new CostingRuleProcess();
    // Update cost of inventories and process starting physical inventories.
    ScrollableResults icls = getCloseInventoryLines(orgs);
    String productId = "";
    BigDecimal totalCost = BigDecimal.ZERO;
    BigDecimal totalStock = BigDecimal.ZERO;
    int i = 0;
    try {
      while (icls.next()) {
        InventoryCountLine icl = (InventoryCountLine) icls.get(0);
        if (!productId.equals(icl.getProduct().getId())) {
          productId = icl.getProduct().getId();
          HashMap<String, BigDecimal> stock = getCurrentValuedStock(productId, curId, orgs, orgId);
          totalCost = stock.get("cost");
          totalStock = stock.get("stock");
        }

        MaterialTransaction trx = crp.getInventoryLineTransaction(icl);
        trx.setTransactionProcessDate(DateUtils.addSeconds(trx.getTransactionProcessDate(), -1));
        trx.setCurrency(OBDal.getInstance().get(Currency.class, curId));

        BigDecimal trxCost = BigDecimal.ZERO;
        if (totalStock.compareTo(BigDecimal.ZERO) != 0) {
          trxCost = totalCost.multiply(trx.getMovementQuantity().abs()).divide(totalStock,
              stdPrecision, BigDecimal.ROUND_HALF_UP);
        }
        if (trx.getMovementQuantity().compareTo(totalStock) == 0) {
          // Last transaction adjusts remaining cost amount.
          trxCost = totalCost;
        }
        trx.setTransactionCost(trxCost);
        trx.setCostCalculated(true);
        trx.setCostingStatus("CC");
        trx.setProcessed(true);
        OBDal.getInstance().save(trx);
        Currency legalEntityCur = FinancialUtils.getLegalEntityCurrency(trx.getOrganization());
        BigDecimal cost = BigDecimal.ZERO;
        if (BigDecimal.ZERO.compareTo(trx.getMovementQuantity()) != 0) {
          cost = trxCost.divide(trx.getMovementQuantity().abs(), costPrecision,
              BigDecimal.ROUND_HALF_UP);
        }
        if (!legalEntityCur.equals(cur)) {
          cost = FinancialUtils.getConvertedAmount(cost, cur, legalEntityCur, new Date(),
              icl.getOrganization(), FinancialUtils.PRECISION_COSTING);
        }

        InventoryCountLine initICL = icl.getRelatedInventory();
        initICL.setCost(cost);
        OBDal.getInstance().save(initICL);

        totalCost = totalCost.subtract(trxCost);
        // MovementQty is already negative so add to totalStock to decrease it.
        totalStock = totalStock.add(trx.getMovementQuantity());

        if ((i % 100) == 0) {
          OBDal.getInstance().flush();
          OBDal.getInstance().getSession().clear();
          cur = OBDal.getInstance().get(Currency.class, curId);
        }
        i++;
      }
    } finally {
      icls.close();
    }

    OBDal.getInstance().flush();
    insertTrxCosts();

  }

  private HashMap<String, BigDecimal> getCurrentValuedStock(String productId, String curId,
      Set<String> orgs, String orgId) {
    Currency currency = OBDal.getInstance().get(Currency.class, curId);
    StringBuffer select = new StringBuffer();
    select.append(" select sum(case");
    select.append("     when trx." + MaterialTransaction.PROPERTY_MOVEMENTQUANTITY
        + " < 0 then -tc." + TransactionCost.PROPERTY_COST);
    select.append("     else tc." + TransactionCost.PROPERTY_COST + " end ) as cost,");
    select.append("  tc." + TransactionCost.PROPERTY_CURRENCY + ".id as currency,");
    select.append("  coalesce(sr." + ShipmentInOut.PROPERTY_ACCOUNTINGDATE + ", trx."
        + MaterialTransaction.PROPERTY_MOVEMENTDATE + ") as mdate,");
    select.append("  sum(trx." + MaterialTransaction.PROPERTY_MOVEMENTQUANTITY + ") as stock");

    select.append(" from " + TransactionCost.ENTITY_NAME + " as tc");
    select.append("  join tc." + TransactionCost.PROPERTY_INVENTORYTRANSACTION + " as trx");
    select.append("  join trx." + MaterialTransaction.PROPERTY_STORAGEBIN + " as locator");
    select.append("  left join trx." + MaterialTransaction.PROPERTY_GOODSSHIPMENTLINE + " as line");
    select.append("  left join line." + ShipmentInOutLine.PROPERTY_SHIPMENTRECEIPT + " as sr");

    select.append(" where trx." + MaterialTransaction.PROPERTY_PRODUCT + ".id = :product");
    // Include only transactions that have its cost calculated
    select.append("   and trx." + MaterialTransaction.PROPERTY_ISCOSTCALCULATED + " = true");
    select.append("   and trx." + MaterialTransaction.PROPERTY_ORGANIZATION + ".id in (:orgs)");
    select.append(" group by tc." + TransactionCost.PROPERTY_CURRENCY + ",");
    select.append("   coalesce(sr." + ShipmentInOut.PROPERTY_ACCOUNTINGDATE + ", trx."
        + MaterialTransaction.PROPERTY_MOVEMENTDATE + ")");

    Query trxQry = OBDal.getInstance().getSession().createQuery(select.toString());
    trxQry.setParameter("product", productId);
    trxQry.setParameterList("orgs", orgs);
    @SuppressWarnings("unchecked")
    List<Object[]> stocks = trxQry.list();
    BigDecimal totalAmt = BigDecimal.ZERO;
    BigDecimal totalQty = BigDecimal.ZERO;
    HashMap<String, BigDecimal> retStock = new HashMap<String, BigDecimal>();
    if (stocks.size() > 0) {
      for (Object[] resultSet : stocks) {
        BigDecimal costAmt = (BigDecimal) resultSet[0];
        Currency origCur = OBDal.getInstance().get(Currency.class, resultSet[1]);
        Date convDate = (Date) resultSet[2];
        BigDecimal qty = (BigDecimal) resultSet[3];

        if (origCur != currency) {
          totalAmt = totalAmt.add(FinancialUtils.getConvertedAmount(costAmt, origCur, currency,
              convDate, OBDal.getInstance().get(Organization.class, orgId),
              FinancialUtils.PRECISION_COSTING));
        } else {
          totalAmt = totalAmt.add(costAmt);
        }
        totalQty = totalQty.add(qty);
      }
    }
    retStock.put("cost", totalAmt);
    retStock.put("stock", totalQty);
    return retStock;
  }

  private ScrollableResults getCloseInventoryLines(Set<String> orgs) {
    StringBuffer where = new StringBuffer();
    where.append(" as il");
    where.append(" where exists (select 1 from " + CostingRuleInit.ENTITY_NAME + " as cri");
    where.append("               where cri." + CostingRuleInit.PROPERTY_CLOSEINVENTORY + " = il."
        + InventoryCountLine.PROPERTY_PHYSINVENTORY + ")");
    where.append("   and il." + InventoryCountLine.PROPERTY_ORGANIZATION + ".id IN (:orgs)");
    where.append(" order by " + InventoryCountLine.PROPERTY_PRODUCT + ", il."
        + InventoryCountLine.PROPERTY_BOOKQUANTITY);

    OBQuery<InventoryCountLine> iclQry = OBDal.getInstance().createQuery(InventoryCountLine.class,
        where.toString());
    iclQry.setNamedParameter("orgs", orgs);
    iclQry.setFilterOnActive(false);
    iclQry.setFilterOnReadableClients(false);
    iclQry.setFilterOnReadableOrganization(false);
    iclQry.setFetchSize(1000);
    return iclQry.scroll(ScrollMode.FORWARD_ONLY);
  }

  private boolean isMigrationFirstPhaseCompleted() {
    OBQuery<Preference> prefQry = OBDal.getInstance().createQuery(Preference.class,
        Preference.PROPERTY_ATTRIBUTE + " = 'CostingMigrationFirstPhaseCompleted'");
    prefQry.setFilterOnReadableClients(false);
    prefQry.setFilterOnReadableOrganization(false);

    return prefQry.count() > 0;
  }

  private AlertRule getLegacyCostAvailableAlert() {
    String where = AlertRule.PROPERTY_NAME + " = '" + alertRuleName + "'";
    OBQuery<AlertRule> alertQry = OBDal.getInstance().createQuery(AlertRule.class, where);
    alertQry.setFilterOnActive(false);

    return alertQry.uniqueResult();
  }

  private ScrollableResults getLegacyCostScroll(String clientId, Set<String> naturalTree) {
    StringBuffer where = new StringBuffer();
    where.append(" as c");
    where.append(" where c." + Costing.PROPERTY_CLIENT + ".id = :client");
    where.append("   and exists (select 1 from " + MaterialTransaction.ENTITY_NAME + " as trx");
    where.append("     where trx." + MaterialTransaction.PROPERTY_ORGANIZATION + ".id in (:orgs)");
    where.append("       and trx." + MaterialTransaction.PROPERTY_TRANSACTIONCOST + " is null");
    where.append("       and trx." + MaterialTransaction.PROPERTY_MOVEMENTDATE + " >= c."
        + Costing.PROPERTY_STARTINGDATE);
    where.append("       and trx." + MaterialTransaction.PROPERTY_PRODUCT + " = c."
        + Costing.PROPERTY_PRODUCT);
    where.append("       and trx." + MaterialTransaction.PROPERTY_MOVEMENTDATE + " -1 < (c."
        + Costing.PROPERTY_ENDINGDATE + ") ");
    where.append("     )");
    where.append("   and " + Costing.PROPERTY_COST + " is not null");
    where.append(" order by " + Costing.PROPERTY_PRODUCT + ", " + Costing.PROPERTY_STARTINGDATE
        + ", " + Costing.PROPERTY_ENDINGDATE + " desc");

    OBQuery<Costing> costingQry = OBDal.getInstance().createQuery(Costing.class, where.toString());
    costingQry.setFilterOnReadableClients(false);
    costingQry.setFilterOnReadableOrganization(false);
    costingQry.setFilterOnActive(false);
    costingQry.setNamedParameter("client", clientId);
    costingQry.setNamedParameter("orgs", naturalTree);
    costingQry.setFetchSize(1000);
    return costingQry.scroll(ScrollMode.FORWARD_ONLY);
  }

  private void updateTrxLegacyCosts(Costing _cost, int standardPrecision, Set<String> naturalTree) {
    log4j.debug("****** UpdateTrxLegacyCosts");
    Costing cost = OBDal.getInstance().get(Costing.class, _cost.getId());

    StringBuffer where = new StringBuffer();
    where.append(MaterialTransaction.PROPERTY_PRODUCT + ".id = :product");
    where.append("   and " + MaterialTransaction.PROPERTY_ORGANIZATION + ".id in (:orgs)");
    where.append("   and " + MaterialTransaction.PROPERTY_MOVEMENTDATE + " >= :dateFrom");
    where.append("   and " + MaterialTransaction.PROPERTY_MOVEMENTDATE + " < :dateTo");
    OBQuery<MaterialTransaction> trxQry = OBDal.getInstance().createQuery(
        MaterialTransaction.class, where.toString());
    trxQry.setFilterOnActive(false);
    trxQry.setFilterOnReadableClients(false);
    trxQry.setFilterOnReadableOrganization(false);
    trxQry.setNamedParameter("product", DalUtil.getId(cost.getProduct()));
    trxQry.setNamedParameter("orgs", naturalTree);
    trxQry.setNamedParameter("dateFrom", cost.getStartingDate());
    trxQry.setNamedParameter("dateTo", cost.getEndingDate());
    trxQry.setFetchSize(1000);

    ScrollableResults trxs = trxQry.scroll(ScrollMode.FORWARD_ONLY);
    int i = 0;
    try {
      while (trxs.next()) {
        MaterialTransaction trx = (MaterialTransaction) trxs.get(0);
        log4j.debug("********** UpdateTrxLegacyCosts process trx:" + trx.getIdentifier());

        if (trx.getGoodsShipmentLine() != null
            && trx.getGoodsShipmentLine().getShipmentReceipt().getAccountingDate()
                .compareTo(trx.getMovementDate()) != 0) {
          // Shipments with accounting date different than the movement date gets the cost valid on
          // the accounting date.
          BigDecimal unitCost = new BigDecimal(new ProductInfo(cost.getProduct().getId(),
              new DalConnectionProvider(false)).getProductItemCost(OBDateUtils.formatDate(trx
              .getGoodsShipmentLine().getShipmentReceipt().getAccountingDate()), null, "AV",
              new DalConnectionProvider(false), OBDal.getInstance().getConnection()));
          BigDecimal trxCost = unitCost.multiply(trx.getMovementQuantity().abs()).setScale(
              standardPrecision, BigDecimal.ROUND_HALF_UP);

          trx.setTransactionCost(trxCost);
        } else {
          trx.setTransactionCost(cost.getCost().multiply(trx.getMovementQuantity().abs())
              .setScale(standardPrecision, BigDecimal.ROUND_HALF_UP));
        }

        trx.setCurrency(cost.getCurrency());
        trx.setCostCalculated(true);
        trx.setCostingStatus("CC");
        trx.setProcessed(true);

        if ((i % 100) == 0) {
          OBDal.getInstance().flush();
          OBDal.getInstance().getSession().clear();
          cost = OBDal.getInstance().get(Costing.class, cost.getId());
        }
        i++;
      }
    } finally {
      trxs.close();
    }

    log4j.debug("****** UpdateTrxLegacyCosts updated:" + i);
  }

  /**
   * Initializes with zero cost those transactions that haven't been calculated by previous methods
   * because they don't have any cost available. This transactions are checked by the alert rule.
   * But if this alert is deactivated the process continues forcing to initialize the transactions
   * with zero cost.
   */
  private void updateWithZeroCostRemainingTrx() {
    log4j.debug("****** updateWithCeroRemainingTrx");
    StringBuffer where = new StringBuffer();
    where.append(MaterialTransaction.PROPERTY_TRANSACTIONCOST + " is null");
    where.append(" order by " + MaterialTransaction.PROPERTY_ORGANIZATION);
    OBQuery<MaterialTransaction> trxsQry = OBDal.getInstance().createQuery(
        MaterialTransaction.class, where.toString());
    trxsQry.setFilterOnActive(false);
    trxsQry.setFilterOnReadableClients(false);
    trxsQry.setFilterOnReadableOrganization(false);
    trxsQry.setFetchSize(1000);
    ScrollableResults trxs = trxsQry.scroll(ScrollMode.FORWARD_ONLY);
    int i = 0;
    String orgId = "";
    String curId = "";
    try {
      while (trxs.next()) {
        MaterialTransaction trx = (MaterialTransaction) trxs.get(0);
        if (!orgId.equals(DalUtil.getId(trx.getOrganization()))) {
          orgId = (String) DalUtil.getId(trx.getOrganization());
          Currency cur = FinancialUtils.getLegalEntityCurrency(trx.getOrganization());
          curId = cur.getId();
        }
        trx.setTransactionCost(BigDecimal.ZERO);
        trx.setCurrency((Currency) OBDal.getInstance().getProxy(Currency.ENTITY_NAME, curId));
        trx.setCostCalculated(true);
        trx.setCostingStatus("CC");
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
    log4j.debug("****** updateWithCeroRemainingTrx updated:" + i);
  }

  private void insertAlertRecipients(AlertRule alertRule) {
    // FIXME: Insert should be done with a loop based on scroll.
    StringBuffer insert = new StringBuffer();
    insert.append("insert into " + AlertRecipient.ENTITY_NAME);
    insert.append(" (id ");
    insert.append(", " + AlertRecipient.PROPERTY_ACTIVE);
    insert.append(", " + AlertRecipient.PROPERTY_CLIENT);
    insert.append(", " + AlertRecipient.PROPERTY_ORGANIZATION);
    insert.append(", " + AlertRecipient.PROPERTY_CREATIONDATE);
    insert.append(", " + AlertRecipient.PROPERTY_CREATEDBY);
    insert.append(", " + AlertRecipient.PROPERTY_UPDATED);
    insert.append(", " + AlertRecipient.PROPERTY_UPDATEDBY);
    insert.append(", " + AlertRecipient.PROPERTY_ROLE);
    insert.append(", " + AlertRecipient.PROPERTY_ALERTRULE);
    insert.append(" )\n select get_uuid()");
    insert.append(", r." + Role.PROPERTY_ACTIVE);
    insert.append(", r." + Role.PROPERTY_CLIENT);
    insert.append(", r." + Role.PROPERTY_ORGANIZATION);
    insert.append(", now()");
    insert.append(", u");
    insert.append(", now()");
    insert.append(", u");
    insert.append(", r");
    insert.append(", ar");
    insert.append(" from " + Role.ENTITY_NAME + " as r");
    insert.append(", " + User.ENTITY_NAME + " as u");
    insert.append(", " + AlertRule.ENTITY_NAME + " as ar");
    insert.append("  where r." + Role.PROPERTY_MANUAL + " = false");
    insert.append("    and r." + Role.PROPERTY_CLIENT + ".id <> '0'");
    insert.append("    and u.id = '0'");
    insert.append("    and ar.id = :ar");

    Query queryInsert = OBDal.getInstance().getSession().createQuery(insert.toString());
    queryInsert.setString("ar", alertRule.getId());
    int inserted = queryInsert.executeUpdate();
    log4j.debug("** inserted alert recipients: " + inserted);
  }

  private void insertTrxCosts() {
    // FIXME: Insert should be done with a loop based on scroll.
    StringBuffer insert = new StringBuffer();
    insert.append("insert into " + TransactionCost.ENTITY_NAME);
    insert.append(" (id ");
    insert.append(", " + TransactionCost.PROPERTY_ACTIVE);
    insert.append(", " + TransactionCost.PROPERTY_CLIENT);
    insert.append(", " + TransactionCost.PROPERTY_ORGANIZATION);
    insert.append(", " + TransactionCost.PROPERTY_CREATIONDATE);
    insert.append(", " + TransactionCost.PROPERTY_CREATEDBY);
    insert.append(", " + TransactionCost.PROPERTY_UPDATED);
    insert.append(", " + TransactionCost.PROPERTY_UPDATEDBY);
    insert.append(", " + TransactionCost.PROPERTY_INVENTORYTRANSACTION);
    insert.append(", " + TransactionCost.PROPERTY_COST);
    insert.append(", " + TransactionCost.PROPERTY_COSTDATE);
    insert.append(", " + TransactionCost.PROPERTY_CURRENCY);
    insert.append(", " + TransactionCost.PROPERTY_ACCOUNTINGDATE);
    insert.append(" )\n select get_uuid()");
    insert.append(", t." + MaterialTransaction.PROPERTY_ACTIVE);
    insert.append(", t." + MaterialTransaction.PROPERTY_CLIENT);
    insert.append(", t." + MaterialTransaction.PROPERTY_ORGANIZATION);
    insert.append(", now()");
    insert.append(", u");
    insert.append(", now()");
    insert.append(", u");
    insert.append(", t");
    insert.append(", t." + MaterialTransaction.PROPERTY_TRANSACTIONCOST);
    insert.append(", t." + MaterialTransaction.PROPERTY_TRANSACTIONPROCESSDATE);
    insert.append(", t." + MaterialTransaction.PROPERTY_CURRENCY);
    insert.append(", coalesce(io." + ShipmentInOut.PROPERTY_ACCOUNTINGDATE + ", t."
        + MaterialTransaction.PROPERTY_MOVEMENTDATE + ")");
    insert.append(" from  " + TransactionCost.ENTITY_NAME + " as tc ");
    insert.append("   right join tc." + TransactionCost.PROPERTY_INVENTORYTRANSACTION + " as t");
    insert.append("   left join t." + MaterialTransaction.PROPERTY_GOODSSHIPMENTLINE + " as iol");
    insert.append("   left join iol." + ShipmentInOutLine.PROPERTY_SHIPMENTRECEIPT + " as io");
    insert.append(", " + User.ENTITY_NAME + " as u");
    insert.append("  where t." + MaterialTransaction.PROPERTY_TRANSACTIONCOST + " is not null");
    insert.append("    and tc.id is null");
    insert.append("    and u.id = :user");

    Query queryInsert = OBDal.getInstance().getSession().createQuery(insert.toString());
    queryInsert.setString("user", (String) DalUtil.getId(OBContext.getOBContext().getUser()));
    queryInsert.executeUpdate();
  }

  private void insertStandardCosts() {
    // Insert STANDARD cost for products with costtype = 'ST'.
    // FIXME: Insert should be done with a loop based on scroll.
    StringBuffer insert = new StringBuffer();
    insert.append("insert into " + Costing.ENTITY_NAME);
    insert.append(" (id ");
    insert.append(", " + Costing.PROPERTY_ACTIVE);
    insert.append(", " + Costing.PROPERTY_CLIENT);
    insert.append(", " + Costing.PROPERTY_ORGANIZATION);
    insert.append(", " + Costing.PROPERTY_CREATIONDATE);
    insert.append(", " + Costing.PROPERTY_CREATEDBY);
    insert.append(", " + Costing.PROPERTY_UPDATED);
    insert.append(", " + Costing.PROPERTY_UPDATEDBY);
    insert.append(", " + Costing.PROPERTY_PRODUCT);
    insert.append(", " + Costing.PROPERTY_COSTTYPE);
    insert.append(", " + Costing.PROPERTY_COST);
    insert.append(", " + Costing.PROPERTY_STARTINGDATE);
    insert.append(", " + Costing.PROPERTY_ENDINGDATE);
    insert.append(", " + Costing.PROPERTY_MANUAL);
    insert.append(", " + Costing.PROPERTY_PERMANENT);
    insert.append(", " + Costing.PROPERTY_CURRENCY);
    insert.append(" )\n select get_uuid()");
    insert.append(", c." + Costing.PROPERTY_ACTIVE);
    insert.append(", c." + Costing.PROPERTY_CLIENT);
    insert.append(", org");
    insert.append(", now()");
    insert.append(", u");
    insert.append(", now()");
    insert.append(", u");
    insert.append(", c." + Costing.PROPERTY_PRODUCT);
    insert.append(", 'STA'");
    insert.append(", c." + Costing.PROPERTY_COST);
    insert.append(", to_date(to_char(:startingDate), to_char('DD-MM-YYYY HH24:MI:SS'))");

    insert.append(", c." + Costing.PROPERTY_ENDINGDATE);
    insert.append(", c." + Costing.PROPERTY_MANUAL);
    insert.append(", c." + Costing.PROPERTY_PERMANENT);
    insert.append(", c." + Costing.PROPERTY_CURRENCY);
    insert.append("\n from " + Costing.ENTITY_NAME + " as c");
    insert.append("   join c." + Costing.PROPERTY_PRODUCT + " as p");
    insert.append(", " + User.ENTITY_NAME + " as u");
    insert.append(", " + Organization.ENTITY_NAME + " as org");
    insert.append("   join org." + Organization.PROPERTY_ORGANIZATIONTYPE + " as ot");
    insert.append("\n where c." + Costing.PROPERTY_COSTTYPE + " = 'ST'");
    insert.append("   and c." + Costing.PROPERTY_STARTINGDATE
        + " <= to_date(to_char(:limitDate), to_char('DD-MM-YYYY HH24:MI:SS'))");
    insert.append("   and c." + Costing.PROPERTY_ENDINGDATE
        + " > to_date(to_char(:limitDate2), to_char('DD-MM-YYYY HH24:MI:SS'))");
    insert.append("   and u.id = :user");
    insert.append("   and ot." + OrganizationType.PROPERTY_LEGALENTITY + " = true");
    insert.append("   and org." + Organization.PROPERTY_CLIENT + " = c." + Costing.PROPERTY_CLIENT);
    insert.append("   and (ad_isorgincluded(c." + Costing.PROPERTY_ORGANIZATION + ".id, org."
        + Organization.PROPERTY_ID + ", c." + Costing.PROPERTY_CLIENT + ".id) <> -1");
    insert.append("   or ad_isorgincluded(org." + Organization.PROPERTY_ID + ".id, c."
        + Costing.PROPERTY_ORGANIZATION + ", c." + Costing.PROPERTY_CLIENT + ".id) <> -1)");
    insert.append("   and (ad_isorgincluded(p." + Product.PROPERTY_ORGANIZATION + ".id, org."
        + Organization.PROPERTY_ID + ", p." + Product.PROPERTY_CLIENT + ".id) <> -1");
    insert.append("   or ad_isorgincluded(org." + Organization.PROPERTY_ID + ".id, p."
        + Product.PROPERTY_ORGANIZATION + ", p." + Product.PROPERTY_CLIENT + ".id) <> -1)");

    Query queryInsert = OBDal.getInstance().getSession().createQuery(insert.toString());
    final SimpleDateFormat dateFormatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
    String startingDate = dateFormatter.format(new Date());
    queryInsert.setString("startingDate", startingDate);
    queryInsert.setString("limitDate", startingDate);
    queryInsert.setString("limitDate2", startingDate);
    queryInsert.setString("user", (String) DalUtil.getId(OBContext.getOBContext().getUser()));
    queryInsert.executeUpdate();

  }

  private CostingRule createCostingRule(Organization org) {
    CostingRule rule = OBProvider.getInstance().get(CostingRule.class);
    rule.setClient(org.getClient());
    rule.setOrganization(org);
    rule.setCostingAlgorithm(getAverageAlgorithm());
    rule.setValidated(false);
    rule.setStartingDate(null);
    OBDal.getInstance().save(rule);
    return rule;
  }

  private void checkAllInventoriesAreProcessed() {
    StringBuffer where = new StringBuffer();
    where.append(" as cri ");
    where.append("   join cri." + CostingRuleInit.PROPERTY_INITINVENTORY + " as ipi");
    where.append(" where ipi." + InventoryCount.PROPERTY_PROCESSED + " = false");
    where.append(" order by ipi." + InventoryCount.PROPERTY_CLIENT + ", ipi."
        + InventoryCount.PROPERTY_ORGANIZATION);

    OBQuery<CostingRuleInit> criQry = OBDal.getInstance().createQuery(CostingRuleInit.class,
        where.toString());
    criQry.setFilterOnReadableClients(false);
    criQry.setFilterOnReadableOrganization(false);
    List<CostingRuleInit> criList = criQry.list();
    if (criList.isEmpty()) {
      return;
    }
    List<String> inventoryList = new ArrayList<String>();
    String client = "";
    String msg = "";
    for (CostingRuleInit cri : criList) {
      if (!client.equals(cri.getClient().getIdentifier())) {
        client = cri.getClient().getIdentifier();
        msg = msg + "@Client@: " + cri.getClient().getIdentifier() + "<br>";
      }
      msg = msg + cri.getOrganization().getIdentifier() + " - "
          + cri.getWarehouse().getIdentifier();
      inventoryList.add(msg);
      msg = "<br>";
    }
    throw new OBException("@unprocessedInventories@: <br>" + inventoryList.toString());
  }

  private List<CostingRule> getRules() {
    OBCriteria<CostingRule> crCrit = OBDal.getInstance().createCriteria(CostingRule.class);
    crCrit.setFilterOnReadableClients(false);
    crCrit.setFilterOnReadableOrganization(false);

    return crCrit.list();
  }

  /**
   * Create a preference to be able to determine that the migration first phase is completed.
   */
  private void createMigrationFirstPhaseCompletedPreference() {
    createPreference("CostingMigrationFirstPhaseCompleted", null);
  }

  private void createPreference(String attribute, String value) {
    Organization org0 = OBDal.getInstance().get(Organization.class, "0");
    Client client0 = OBDal.getInstance().get(Client.class, "0");

    Preference newPref = OBProvider.getInstance().get(Preference.class);
    newPref.setClient(client0);
    newPref.setOrganization(org0);
    newPref.setPropertyList(false);
    newPref.setAttribute(attribute);
    newPref.setSearchKey(value);

    OBDal.getInstance().save(newPref);
  }

  private void deleteAlertRule() {
    AlertRule legacyCostAvailableAlert = getLegacyCostAvailableAlert();
    OBDal.getInstance().remove(legacyCostAvailableAlert);
  }

  private void updateReportRoles() {
    OBContext.setAdminMode(false);
    try {
      StringBuffer where = new StringBuffer();
      where.append(" as ra");
      where.append("  join ra." + ProcessAccess.PROPERTY_ROLE + " as r");
      where.append(" where r." + Role.PROPERTY_MANUAL + " = true");
      where.append("   and ra." + ProcessAccess.PROPERTY_PROCESS + ".id IN ('" + paretoLegacy
          + "', '" + mUpdateParetoLegacy + "', '" + dimensionalLegacy + "', '" + valuedLegacy
          + "')");
      OBQuery<ProcessAccess> obcRoleAccess = OBDal.getInstance().createQuery(ProcessAccess.class,
          where.toString());
      obcRoleAccess.setFilterOnReadableClients(false);
      obcRoleAccess.setFilterOnReadableOrganization(false);
      for (ProcessAccess processAccess : obcRoleAccess.list()) {
        String idprocess = (String) DalUtil.getId(processAccess.getProcess());

        if (paretoLegacy.equals(idprocess)) {
          processAccess.setProcess((org.openbravo.model.ad.ui.Process) OBDal.getInstance()
              .getProxy(processEntity, pareto));
        } else if (mUpdateParetoLegacy.equals(idprocess)) {
          processAccess.setProcess((org.openbravo.model.ad.ui.Process) OBDal.getInstance()
              .getProxy(processEntity, mUpdatePareto));
        } else if (dimensionalLegacy.equals(idprocess)) {
          processAccess.setProcess((org.openbravo.model.ad.ui.Process) OBDal.getInstance()
              .getProxy(processEntity, dimensional));
        } else if (valuedLegacy.equals(idprocess)) {
          processAccess.setProcess((org.openbravo.model.ad.ui.Process) OBDal.getInstance()
              .getProxy(processEntity, valued));
        }

        OBDal.getInstance().save(processAccess);
      }

    } catch (Exception e) {

    }

    finally {
      OBContext.restorePreviousMode();
    }
  }

  private void deleteMigrationFirstPhaseCompletedPreference() {
    OBQuery<Preference> prefQry = OBDal.getInstance().createQuery(Preference.class,
        Preference.PROPERTY_ATTRIBUTE + " = 'CostingMigrationFirstPhaseCompleted'");
    prefQry.setFilterOnReadableClients(false);
    prefQry.setFilterOnReadableOrganization(false);

    if (!prefQry.list().isEmpty()) {
      OBDal.getInstance().remove(prefQry.list().get(0));
    }
  }

  private static CostingAlgorithm getAverageAlgorithm() {
    if (averageAlgorithm != null) {
      return averageAlgorithm;
    }
    OBCriteria<CostingAlgorithm> costalgCrit = OBDal.getInstance().createCriteria(
        CostingAlgorithm.class);
    costalgCrit.add(Restrictions.eq(CostingAlgorithm.PROPERTY_JAVACLASSNAME,
        "org.openbravo.costing.AverageAlgorithm"));
    costalgCrit.add(Restrictions.eq(CostingAlgorithm.PROPERTY_CLIENT,
        OBDal.getInstance().get(Client.class, "0")));
    costalgCrit.setFilterOnReadableClients(false);
    costalgCrit.setFilterOnReadableOrganization(false);
    averageAlgorithm = (CostingAlgorithm) costalgCrit.uniqueResult();
    return averageAlgorithm;
  }

  private static List<Client> getClients() {
    OBCriteria<Client> obcClient = OBDal.getInstance().createCriteria(Client.class);
    obcClient.setFilterOnReadableClients(false);
    obcClient.add(Restrictions.ne(Client.PROPERTY_ID, "0"));
    return obcClient.list();
  }
}
