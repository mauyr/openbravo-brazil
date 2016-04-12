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
 * All portions are Copyright (C) 2014-2015 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.test.costing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.time.DateUtils;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Query;
import org.hibernate.criterion.Restrictions;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.costing.CancelCostAdjustment;
import org.openbravo.costing.CostingBackground;
import org.openbravo.costing.CostingRuleProcess;
import org.openbravo.costing.InventoryAmountUpdateProcess;
import org.openbravo.costing.LCCostMatchFromInvoiceHandler;
import org.openbravo.costing.LCMatchingCancelHandler;
import org.openbravo.costing.LCMatchingProcessHandler;
import org.openbravo.costing.LandedCostProcessHandler;
import org.openbravo.costing.ManualCostAdjustmentProcessHandler;
import org.openbravo.costing.PriceDifferenceBackground;
import org.openbravo.costing.ReactivateLandedCost;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.ad_forms.AcctServer;
import org.openbravo.erpCommon.ad_process.VerifyBOM;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.datamodel.Table;
import org.openbravo.model.ad.process.ProcessInstance;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.ad.ui.Process;
import org.openbravo.model.ad.utility.Sequence;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.currency.ConversionRate;
import org.openbravo.model.common.currency.ConversionRateDoc;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.enterprise.Locator;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.invoice.InvoiceLine;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.plm.AttributeSetInstance;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.common.plm.ProductAccounts;
import org.openbravo.model.common.plm.ProductBOM;
import org.openbravo.model.common.uom.UOM;
import org.openbravo.model.financialmgmt.accounting.AccountingFact;
import org.openbravo.model.financialmgmt.accounting.coa.AcctSchema;
import org.openbravo.model.financialmgmt.accounting.coa.AcctSchemaTable;
import org.openbravo.model.financialmgmt.accounting.coa.ElementValue;
import org.openbravo.model.financialmgmt.calendar.Period;
import org.openbravo.model.financialmgmt.gl.GLItem;
import org.openbravo.model.financialmgmt.tax.TaxRate;
import org.openbravo.model.materialmgmt.cost.CostAdjustment;
import org.openbravo.model.materialmgmt.cost.CostAdjustmentLine;
import org.openbravo.model.materialmgmt.cost.Costing;
import org.openbravo.model.materialmgmt.cost.CostingAlgorithm;
import org.openbravo.model.materialmgmt.cost.CostingRule;
import org.openbravo.model.materialmgmt.cost.InventoryAmountUpdate;
import org.openbravo.model.materialmgmt.cost.InventoryAmountUpdateLine;
import org.openbravo.model.materialmgmt.cost.LCDistributionAlgorithm;
import org.openbravo.model.materialmgmt.cost.LCMatched;
import org.openbravo.model.materialmgmt.cost.LCReceipt;
import org.openbravo.model.materialmgmt.cost.LCReceiptLineAmt;
import org.openbravo.model.materialmgmt.cost.LandedCost;
import org.openbravo.model.materialmgmt.cost.LandedCostCost;
import org.openbravo.model.materialmgmt.cost.TransactionCost;
import org.openbravo.model.materialmgmt.transaction.InternalConsumption;
import org.openbravo.model.materialmgmt.transaction.InternalConsumptionLine;
import org.openbravo.model.materialmgmt.transaction.InternalMovement;
import org.openbravo.model.materialmgmt.transaction.InternalMovementLine;
import org.openbravo.model.materialmgmt.transaction.InventoryCount;
import org.openbravo.model.materialmgmt.transaction.MaterialTransaction;
import org.openbravo.model.materialmgmt.transaction.ProductionLine;
import org.openbravo.model.materialmgmt.transaction.ProductionPlan;
import org.openbravo.model.materialmgmt.transaction.ProductionTransaction;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;
import org.openbravo.model.pricing.pricelist.ProductPrice;
import org.openbravo.model.procurement.ReceiptInvoiceMatch;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.CallStoredProcedure;
import org.openbravo.service.db.DalConnectionProvider;

/**
 * Test cases to verify Cost Adjustment Project
 * 
 * @author aferraz
 */

public class TestCosting extends WeldBaseTest {

  // User System
  private static String USERADMIN_ID = "0";
  // User Openbravo
  private static String USER_ID = "100";
  // Client QA Testing
  private static String CLIENT_ID = "4028E6C72959682B01295A070852010D";
  // Organization Spain
  private static String ORGANIZATION_ID = "357947E87C284935AD1D783CF6F099A1";
  // Role QA Testing Admin
  private static String ROLE_ID = "4028E6C72959682B01295A071429011E";
  // Warehouse with name: Spain warehouse
  private static String WAREHOUSE1_ID = "4028E6C72959682B01295ECFEF4502A0";
  // Warehouse with name: Spain East warehouse
  private static String WAREHOUSE2_ID = "4D7B97565A024DB7B4C61650FA2B9560";

  // Document Sequence with name: DocumentNo_M_InOut
  private static String SHIPMENTIN_SEQUENCE_ID = "910E14E8BA4A419B92DF9973ACDB8A8F";
  // Document Sequence with name: DocumentNo_C_Invoice
  private static String INVOICEIN_SEQUENCE_ID = "766DC632FDCE485B88F7535CF2A3422E";
  // Document Sequence with name: DocumentNo_M_Movement
  private static String MOVEMENT_SEQUENCE_ID = "07FD646511E14BADB5C1BB5CF2FCAC57";
  // Currency with name: EUR
  private static String CURRENCY1_ID = "102";
  // Currency with name: USD
  private static String CURRENCY2_ID = "100";
  // Storage Bin with name: L01
  private static String LOCATOR1_ID = "193476BDD14E4A11B651B4E3E8D767C8";
  // Storage Bin with name: L02
  private static String LOCATOR2_ID = "1A11102F318D4720957B52C8719A34F2";
  // Storage Bin with name: L03
  private static String LOCATOR3_ID = "FB4D5926A1B443E68CC2DB2BBAE3315D";
  // Storage Bin with name: M01
  private static String LOCATOR4_ID = "96DEDCC179504711A81497DE68900F49";
  // UOM with name: Unit
  private static String UOM_ID = "100";
  // Document sequence with name: DocumentNo_M_Production
  private static String PRODUCTION_DOCUMENTSEQUENCE_ID = "617CDE87DFC24C2FBFF278F7B8D22B82";
  // Document type with name: RFC Order
  private static String RFCORDER_DOCUMENTTYPE_ID = "C789FE062AA8480BAD91543A0C6B41AB";
  // Document type with name: RFC Receipt
  private static String RFCRECEIPT_DOCUMENTTYPE_ID = "4683C39FF3B242CD8A5B5825550C4472";
  // Document type with name: Landed Cost
  private static String LANDEDCOST_DOCUMENTTYPE_ID = "38E131FE95F949CA97C95A9B03B3D6A8";
  // Document type with name: Landed Cost Cost
  private static String LANDEDCOSTCOST_DOCUMENTTYPE_ID = "F66B960D26C64215B1F4A09C3417FB16";
  // Landed Cost Distribution Algorithm with name: Distribution by Amount
  private static String LANDEDCOSTCOST_ALGORITHM_ID = "CF9B55BD159B474A9F79849C48715540";
  // Process with name: Validate Costing Rule
  private static String VALIDATECOSTINGRULE_PROCESS_ID = "A269DCA4DE114E438695B66E166999B4";
  // Process with name: Verify BOM
  private static String VERIFYBOM_PROCESS_ID = "136";
  // Process with name: Create/Process Production
  private static String PROCESSPRODUCTION_PROCESS_ID = "137";
  // Process request with name: Process Movements
  private static String PROCESSMOVEMENT_PROCESS_ID = "122";
  // Process request with name: Process Internal Consumption
  private static String PROCESSCONSUMPTION_PROCESS_ID = "800131";
  // G/L Item with name: Fees
  private static String LANDEDCOSTTYPE1_ID = "1DA4C24347EA4494BBA4466FF23ECAA5";
  // Product with name: Transportation Cost
  private static String LANDEDCOSTTYPE2_ID = "5557E7C0FD064FD7A1CCB8C0E824DEE6";
  // Product with name: USD Cost
  private static String LANDEDCOSTTYPE3_ID = "CB473A64934B4D1583008D52DD0FBC49";
  // Business partner with name: Vendor USA
  private static String BUSINESSPARTNER_ID = "C8AD0EAF3052415BB1E15EFDEFBFD4AF";
  // Costing Algorithm with name: Average Algorithm
  private static String COSTINGALGORITHM_ID = "B069080A0AE149A79CF1FA0E24F16AB6";
  // General Ledger Configuration with name: Main US/A/Euro
  private static String GENERALLEDGER_ID = "9A68A0F8D72D4580B3EC3CAA00A5E1F0";
  // Table with name: MaterialMgmtInternalConsumption
  private static String TABLE1_ID = "800168";
  // Table with name: MaterialMgmtInternalMovement
  private static String TABLE2_ID = "323";
  // Table with name: MaterialMgmtInventoryCount
  private static String TABLE3_ID = "321";
  // Table with name: MaterialMgmtShipmentInOut
  private static String TABLE4_ID = "319";
  // Table with name: MaterialMgmtProductionTransaction
  private static String TABLE5_ID = "325";
  // Table with name: ProcurementReceiptInvoiceMatch
  private static String TABLE6_ID = "472";

  // Product with name: costing Product 1
  private static String PRODUCT_ID = "A8B10A097DBD4BF5865BA3C844A2299C";
  // Purchase Order with documentNo: 800010
  private static String ORDERIN_ID = "2C9CEDC0761A41DCB276A5124F8AAA90";
  // Sales Order with documentNo: 50012
  private static String ORDEROUT_ID = "8B53B7E6CF3B4D8D9BCF3A49EED6FCB4";
  // Purchase Invoice with documentNo: 10000017
  private static String INVOICEIN_ID = "9D0F6E57E59247F6AB6D063951811F51";
  // Goods Receipt with documentNo: 10000012
  private static String MOVEMENTIN_ID = "0450583047434254835B2B36B2E5B018";
  // Goods Shipment with documentNo: 500014
  private static String MOVEMENTOUT_ID = "2BCCC64DA82A48C3976B4D007315C2C9";

  /********************************************** Automated tests **********************************************/

  @BeforeClass
  public static void setInitialConfiguration() {
    try {

      // Set System context
      OBContext.setOBContext(USERADMIN_ID);
      OBContext.setAdminMode(true);

      // Set EUR currency costing precision
      Currency currrencyEur = OBDal.getInstance().get(Currency.class, CURRENCY1_ID);
      currrencyEur.setCostingPrecision(4L);
      OBDal.getInstance().save(currrencyEur);

      // Set USD currency costing precision
      Currency currrencyUsd = OBDal.getInstance().get(Currency.class, CURRENCY2_ID);
      currrencyUsd.setCostingPrecision(4L);
      OBDal.getInstance().save(currrencyUsd);

      OBDal.getInstance().flush();

      // Set QA context
      OBContext.setOBContext(USER_ID, ROLE_ID, CLIENT_ID, ORGANIZATION_ID);

      // Set Spain organization currency
      Organization organization = OBDal.getInstance().get(Organization.class, ORGANIZATION_ID);
      organization.setCurrency(OBDal.getInstance().get(Currency.class, CURRENCY1_ID));
      OBDal.getInstance().save(organization);

      // Set allow negatives in General Ledger
      AcctSchema acctSchema = OBDal.getInstance().get(AcctSchema.class, GENERALLEDGER_ID);
      acctSchema.setAllowNegative(false);
      OBDal.getInstance().save(acctSchema);

      // Active tables in General Ledger Configuration
      List<Table> tableList = new ArrayList<Table>();
      tableList.add(OBDal.getInstance().get(Table.class, TABLE1_ID));
      tableList.add(OBDal.getInstance().get(Table.class, TABLE2_ID));
      tableList.add(OBDal.getInstance().get(Table.class, TABLE3_ID));
      tableList.add(OBDal.getInstance().get(Table.class, TABLE4_ID));
      tableList.add(OBDal.getInstance().get(Table.class, TABLE5_ID));
      tableList.add(OBDal.getInstance().get(Table.class, TABLE6_ID));
      final OBCriteria<AcctSchemaTable> criteria1 = OBDal.getInstance().createCriteria(
          AcctSchemaTable.class);
      criteria1.add(Restrictions.eq(AcctSchemaTable.PROPERTY_ACCOUNTINGSCHEMA, acctSchema));
      criteria1.add(Restrictions.in(AcctSchemaTable.PROPERTY_TABLE, tableList));
      criteria1.setFilterOnActive(false);
      criteria1.setFilterOnReadableClients(false);
      criteria1.setFilterOnReadableOrganization(false);
      for (AcctSchemaTable acctSchemaTable : criteria1.list()) {
        acctSchemaTable.setActive(true);
        OBDal.getInstance().save(acctSchemaTable);
      }

      OBDal.getInstance().flush();

      // Create costing rule
      CostingRule costingRule = OBProvider.getInstance().get(CostingRule.class);
      setGeneralData(costingRule);
      costingRule.setCostingAlgorithm(OBDal.getInstance().get(CostingAlgorithm.class,
          COSTINGALGORITHM_ID));
      costingRule.setWarehouseDimension(true);
      costingRule.setBackdatedTransactionsFixed(true);
      costingRule.setValidated(false);
      costingRule.setStartingDate(null);
      costingRule.setEndingDate(null);
      OBDal.getInstance().save(costingRule);
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(costingRule);
      runCostingBackground();
      validateCostingRule(costingRule.getId());

      OBDal.getInstance().commitAndClose();
    } catch (Exception e) {
      System.out.println(e.getMessage());
      throw new OBException(e);
    }

    finally {
      OBContext.restorePreviousMode();
    }
  }

  @AfterClass
  public static void setFinalConfiguration() {
    try {

      // Set System context
      OBContext.setOBContext(USERADMIN_ID);
      OBContext.setAdminMode(true);

      // Set EUR currency costing precision
      Currency currrencyEur = OBDal.getInstance().get(Currency.class, CURRENCY1_ID);
      currrencyEur.setCostingPrecision(2L);
      OBDal.getInstance().save(currrencyEur);

      // Set USD currency costing precision
      Currency currrencyUsd = OBDal.getInstance().get(Currency.class, CURRENCY2_ID);
      currrencyUsd.setCostingPrecision(2L);
      OBDal.getInstance().save(currrencyUsd);

      OBDal.getInstance().flush();

      // Set QA context
      OBContext.setOBContext(USER_ID, ROLE_ID, CLIENT_ID, ORGANIZATION_ID);

      // Set Spain organization currency
      Organization organization = OBDal.getInstance().get(Organization.class, ORGANIZATION_ID);
      organization.setCurrency(null);
      OBDal.getInstance().save(organization);

      // Set allow negatives in General Ledger
      AcctSchema acctSchema = OBDal.getInstance().get(AcctSchema.class, GENERALLEDGER_ID);
      acctSchema.setAllowNegative(true);
      OBDal.getInstance().save(acctSchema);

      // Active tables in General Ledger Configuration
      List<Table> tableList = new ArrayList<Table>();
      tableList.add(OBDal.getInstance().get(Table.class, TABLE1_ID));
      tableList.add(OBDal.getInstance().get(Table.class, TABLE2_ID));
      tableList.add(OBDal.getInstance().get(Table.class, TABLE3_ID));
      tableList.add(OBDal.getInstance().get(Table.class, TABLE4_ID));
      tableList.add(OBDal.getInstance().get(Table.class, TABLE5_ID));
      tableList.add(OBDal.getInstance().get(Table.class, TABLE6_ID));
      final OBCriteria<AcctSchemaTable> criteria = OBDal.getInstance().createCriteria(
          AcctSchemaTable.class);
      criteria.add(Restrictions.eq(AcctSchemaTable.PROPERTY_ACCOUNTINGSCHEMA, acctSchema));
      criteria.add(Restrictions.in(AcctSchemaTable.PROPERTY_TABLE, tableList));
      for (AcctSchemaTable acctSchemaTable : criteria.list()) {
        acctSchemaTable.setActive(false);
        OBDal.getInstance().save(acctSchemaTable);
      }

      OBDal.getInstance().flush();
      OBDal.getInstance().commitAndClose();
    } catch (Exception e) {
      System.out.println(e.getMessage());
      throw new OBException(e);
    }

    finally {
      OBContext.restorePreviousMode();
    }
  }

  @Test
  public void testCostingAAA() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final int day3 = 15;
    final int day4 = 20;
    final int day5 = 25;
    final BigDecimal price1 = new BigDecimal("50.00");
    final BigDecimal price2 = new BigDecimal("70.00");
    final BigDecimal price3 = new BigDecimal("80.00");
    final BigDecimal price4 = new BigDecimal("62.00");
    final BigDecimal price5 = new BigDecimal("68.00");
    final BigDecimal quantity1 = new BigDecimal("100");
    final BigDecimal quantity2 = new BigDecimal("150");

    try {

      OBContext.setOBContext(USER_ID, ROLE_ID, CLIENT_ID, ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product = createProduct("testCostingAAA", price1);

      // Create purchase order and book it
      Order purchaseOrder1 = createPurchaseOrder(product, price1, quantity1, day0);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt1 = createGoodsReceipt(purchaseOrder1, price1, quantity1, day1);

      // Create purchase invoice, post it and assert it
      createPurchaseInvoice(goodsReceipt1, price1, quantity1, day2);

      // Create purchase order and book it
      Order purchaseOrder2 = createPurchaseOrder(product, price2, quantity2, day3);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt2 = createGoodsReceipt(purchaseOrder2, price2, quantity2, day4);

      // Create purchase invoice, post it and assert it
      createPurchaseInvoice(goodsReceipt2, price3, quantity2, day5);

      // Run price correction background
      runPriceBackground();

      // Assert product transactions
      List<ProductTransactionAssert> productTransactionAssertList = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt1.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price1));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt2.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price2, price3));
      assertProductTransaction(product.getId(), productTransactionAssertList);

      // Assert product costing
      List<MaterialTransaction> transactionList = getProductTransactions(product.getId());
      List<ProductCostingAssert> productCostingAssertList = new ArrayList<ProductCostingAssert>();
      productCostingAssertList.add(new ProductCostingAssert(transactionList.get(0), price1, null,
          price1, quantity1));
      productCostingAssertList.add(new ProductCostingAssert(transactionList.get(1), price3, price4,
          price5, quantity1.add(quantity2)));
      assertProductCosting(product.getId(), productCostingAssertList);

      // Assert cost adjustment
      List<CostAdjustment> costAdjustmentList = getCostAdjustment(product.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList.add(new CostAdjustmentAssert(transactionList.get(1), "PDC",
          quantity2.multiply(price3).add(quantity2.multiply(price2).negate()), day5, true));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList);
      assertCostAdjustment(costAdjustmentList, costAdjustmentAssertList);

      // Post cost adjustment and assert it
      postDocument(costAdjustmentList.get(0));
      List<DocumentPostAssert> documentPostAssertList = new ArrayList<DocumentPostAssert>();
      documentPostAssertList.add(new DocumentPostAssert("99904", BigDecimal.ZERO, quantity2
          .multiply(price3).add(quantity2.multiply(price2).negate()), null));
      documentPostAssertList.add(new DocumentPostAssert("35000", quantity2.multiply(price3).add(
          quantity2.multiply(price2).negate()), BigDecimal.ZERO, null));
      CostAdjustment costAdjustment = OBDal.getInstance().get(CostAdjustment.class,
          costAdjustmentList.get(0).getId());
      assertDocumentPost(costAdjustment, product.getId(), documentPostAssertList);

      OBDal.getInstance().commitAndClose();

    } catch (Exception e) {
      System.out.println(e.getMessage());
      throw new OBException(e);
    }

    finally {
      OBContext.restorePreviousMode();
    }
  }

  @Test
  public void testCostingCC() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final int day3 = 15;
    final BigDecimal price1 = new BigDecimal("15.00");
    final BigDecimal price2 = new BigDecimal("25.00");
    final BigDecimal quantity1 = new BigDecimal("180");
    final BigDecimal quantity2 = new BigDecimal("80");

    try {

      OBContext.setOBContext(USER_ID, ROLE_ID, CLIENT_ID, ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product = createProduct("testCostingCC", price1);

      // Create purchase order and book it
      Order purchaseOrder = createPurchaseOrder(product, price1, quantity1, day0);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt = createGoodsReceipt(purchaseOrder, price1, quantity1, day1);

      // Create purchase invoice, post it and assert it
      Invoice purchaseInvoice = createPurchaseInvoice(goodsReceipt, price1, quantity1, day2);

      // Create goods shipment, run costing background, post it and assert it
      ShipmentInOut goodsShipment = createGoodsShipment(product, price1, quantity2, day3);

      // Update purchase order line product price
      updatePurchaseOrder(purchaseOrder, price2);

      // Update purchase invoice line product price
      updatePurchaseInvoice(purchaseInvoice, price2);

      // Run price correction background
      runPriceBackground();

      // Assert product transactions
      List<ProductTransactionAssert> productTransactionAssertList = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price2));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsShipment.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price2));
      assertProductTransaction(product.getId(), productTransactionAssertList);

      // Assert product costing
      List<MaterialTransaction> transactionList = getProductTransactions(product.getId());
      List<ProductCostingAssert> productCostingAssertList = new ArrayList<ProductCostingAssert>();
      productCostingAssertList.add(new ProductCostingAssert(transactionList.get(0), price2, price1,
          price2, quantity1));
      assertProductCosting(product.getId(), productCostingAssertList);

      // Assert cost adjustment
      List<CostAdjustment> costAdjustmentList = getCostAdjustment(product.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList.add(new CostAdjustmentAssert(transactionList.get(0), "PDC",
          quantity1.multiply(price2).add(quantity1.multiply(price1).negate()), day2, true));
      costAdjustmentAssertLineList.add(new CostAdjustmentAssert(transactionList.get(1), "PDC",
          quantity2.multiply(price2).add(quantity2.multiply(price1).negate()), day3, false));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList);
      assertCostAdjustment(costAdjustmentList, costAdjustmentAssertList);

      // Post cost adjustment and assert it
      postDocument(costAdjustmentList.get(0));
      List<DocumentPostAssert> documentPostAssertList = new ArrayList<DocumentPostAssert>();
      documentPostAssertList.add(new DocumentPostAssert("99904", BigDecimal.ZERO, quantity1
          .multiply(price2).add(quantity1.multiply(price1).negate()), null));
      documentPostAssertList.add(new DocumentPostAssert("35000", quantity1.multiply(price2).add(
          quantity1.multiply(price1).negate()), BigDecimal.ZERO, null));
      documentPostAssertList.add(new DocumentPostAssert("99900", quantity2.multiply(price2).add(
          quantity2.multiply(price1).negate()), BigDecimal.ZERO, null));
      documentPostAssertList.add(new DocumentPostAssert("35000", BigDecimal.ZERO, quantity2
          .multiply(price2).add(quantity2.multiply(price1).negate()), null));
      CostAdjustment costAdjustment = OBDal.getInstance().get(CostAdjustment.class,
          costAdjustmentList.get(0).getId());
      assertDocumentPost(costAdjustment, product.getId(), documentPostAssertList);

      OBDal.getInstance().commitAndClose();

    } catch (Exception e) {
      System.out.println(e.getMessage());
      throw new OBException(e);
    }

    finally {
      OBContext.restorePreviousMode();
    }
  }

  @Test
  public void testCostingC1() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final int day3 = 15;
    final BigDecimal price1 = new BigDecimal("15.00");
    final BigDecimal price2 = new BigDecimal("25.00");
    final BigDecimal quantity1 = new BigDecimal("180");
    final BigDecimal quantity2 = new BigDecimal("80");

    try {

      OBContext.setOBContext(USER_ID, ROLE_ID, CLIENT_ID, ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product = createProduct("testCostingC1", price1);

      // Create purchase order and book it
      Order purchaseOrder = createPurchaseOrder(product, price1, quantity1, day0);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt = createGoodsReceipt(purchaseOrder, price1, quantity1, day1);

      // Create goods shipment, run costing background, post it and assert it
      ShipmentInOut goodsShipment = createGoodsShipment(product, price1, quantity2, day2);

      // Update purchase order line product price
      updatePurchaseOrder(purchaseOrder, price2);

      // Create purchase invoice, post it and assert it
      createPurchaseInvoice(goodsReceipt, price2, quantity1, day3);

      // Run price correction background
      runPriceBackground();

      // Assert product transactions
      List<ProductTransactionAssert> productTransactionAssertList = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price2));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsShipment.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price2));
      assertProductTransaction(product.getId(), productTransactionAssertList);

      // Assert product costing
      List<MaterialTransaction> transactionList = getProductTransactions(product.getId());
      List<ProductCostingAssert> productCostingAssertList = new ArrayList<ProductCostingAssert>();
      productCostingAssertList.add(new ProductCostingAssert(transactionList.get(0), price2, price1,
          price2, quantity1));
      assertProductCosting(product.getId(), productCostingAssertList);

      // Assert cost adjustment
      List<CostAdjustment> costAdjustmentList = getCostAdjustment(product.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList.add(new CostAdjustmentAssert(transactionList.get(0), "PDC",
          quantity1.multiply(price2).add(quantity1.multiply(price1).negate()), day3, true));
      costAdjustmentAssertLineList.add(new CostAdjustmentAssert(transactionList.get(1), "PDC",
          quantity2.multiply(price2).add(quantity2.multiply(price1).negate()), day3, false));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList);
      assertCostAdjustment(costAdjustmentList, costAdjustmentAssertList);

      // Post cost adjustment and assert it
      postDocument(costAdjustmentList.get(0));
      List<DocumentPostAssert> documentPostAssertList = new ArrayList<DocumentPostAssert>();
      documentPostAssertList.add(new DocumentPostAssert("99904", BigDecimal.ZERO, quantity1
          .multiply(price2).add(quantity1.multiply(price1).negate()), null));
      documentPostAssertList.add(new DocumentPostAssert("35000", quantity1.multiply(price2).add(
          quantity1.multiply(price1).negate()), BigDecimal.ZERO, null));
      documentPostAssertList.add(new DocumentPostAssert("99900", quantity2.multiply(price2).add(
          quantity2.multiply(price1).negate()), BigDecimal.ZERO, null));
      documentPostAssertList.add(new DocumentPostAssert("35000", BigDecimal.ZERO, quantity2
          .multiply(price2).add(quantity2.multiply(price1).negate()), null));
      CostAdjustment costAdjustment = OBDal.getInstance().get(CostAdjustment.class,
          costAdjustmentList.get(0).getId());
      assertDocumentPost(costAdjustment, product.getId(), documentPostAssertList);

      OBDal.getInstance().commitAndClose();

    } catch (Exception e) {
      System.out.println(e.getMessage());
      throw new OBException(e);
    }

    finally {
      OBContext.restorePreviousMode();
    }
  }

  @Test
  public void testCostingDDD() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final int day3 = 15;
    final BigDecimal price1 = new BigDecimal("25.00");
    final BigDecimal price2 = new BigDecimal("20.00");
    final BigDecimal price3 = new BigDecimal("24.3103");
    final BigDecimal quantity1 = new BigDecimal("580");
    final BigDecimal quantity2 = new BigDecimal("80");
    final BigDecimal quantity3 = new BigDecimal("500");

    try {

      OBContext.setOBContext(USER_ID, ROLE_ID, CLIENT_ID, ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product = createProduct("testCostingDDD", price1);

      // Create purchase order and book it
      Order purchaseOrder = createPurchaseOrder(product, price1, quantity1, day0);

      // Create purchase invoice, post it and assert it
      Invoice purchaseInvoice = createPurchaseInvoice(purchaseOrder, price1, quantity1, day1);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt1 = createGoodsReceipt(purchaseInvoice, price1, quantity2, day2);

      // Update purchase invoice line product price
      updatePurchaseInvoice(purchaseInvoice, price2);

      // Run price correction background
      runPriceBackground();

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt2 = createGoodsReceipt(purchaseInvoice, price1, quantity3, day3);

      // Run price correction background
      runPriceBackground();

      // Assert product transactions
      List<ProductTransactionAssert> productTransactionAssertList = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt1.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price2));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt2.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price2));
      assertProductTransaction(product.getId(), productTransactionAssertList);

      // Assert product costing
      List<MaterialTransaction> transactionList = getProductTransactions(product.getId());
      List<ProductCostingAssert> productCostingAssertList = new ArrayList<ProductCostingAssert>();
      productCostingAssertList.add(new ProductCostingAssert(transactionList.get(0), price2, price1,
          price2, quantity2));
      productCostingAssertList.add(new ProductCostingAssert(transactionList.get(1), price2, price3,
          price2, quantity1));
      assertProductCosting(product.getId(), productCostingAssertList);

      // Assert cost adjustment
      List<CostAdjustment> costAdjustmentList = getCostAdjustment(product.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList1 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList1.add(new CostAdjustmentAssert(transactionList.get(0), "PDC",
          quantity2.multiply(price2).add(quantity2.multiply(price1).negate()), day1, true));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList1);
      List<CostAdjustmentAssert> costAdjustmentAssertLineList2 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList2.add(new CostAdjustmentAssert(transactionList.get(1), "PDC",
          quantity3.multiply(price2).add(quantity3.multiply(price1).negate()), day1, true));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList2);
      assertCostAdjustment(costAdjustmentList, costAdjustmentAssertList);

      // Post cost adjustment 1 and assert it
      postDocument(costAdjustmentList.get(0));
      List<DocumentPostAssert> documentPostAssertList1 = new ArrayList<DocumentPostAssert>();
      documentPostAssertList1.add(new DocumentPostAssert("99904", quantity2.multiply(price1).add(
          quantity2.multiply(price2).negate()), BigDecimal.ZERO, null));
      documentPostAssertList1.add(new DocumentPostAssert("35000", BigDecimal.ZERO, quantity2
          .multiply(price1).add(quantity2.multiply(price2).negate()), null));
      CostAdjustment costAdjustment1 = OBDal.getInstance().get(CostAdjustment.class,
          costAdjustmentList.get(0).getId());
      assertDocumentPost(costAdjustment1, product.getId(), documentPostAssertList1);

      // Post cost adjustment 2 and assert it
      postDocument(costAdjustmentList.get(1));
      List<DocumentPostAssert> documentPostAssertList2 = new ArrayList<DocumentPostAssert>();
      documentPostAssertList2.add(new DocumentPostAssert("99904", quantity3.multiply(price1).add(
          quantity3.multiply(price2).negate()), BigDecimal.ZERO, null));
      documentPostAssertList2.add(new DocumentPostAssert("35000", BigDecimal.ZERO, quantity3
          .multiply(price1).add(quantity3.multiply(price2).negate()), null));
      CostAdjustment costAdjustment2 = OBDal.getInstance().get(CostAdjustment.class,
          costAdjustmentList.get(1).getId());
      assertDocumentPost(costAdjustment2, product.getId(), documentPostAssertList2);

      OBDal.getInstance().commitAndClose();

    } catch (Exception e) {
      System.out.println(e.getMessage());
      throw new OBException(e);
    }

    finally {
      OBContext.restorePreviousMode();
    }
  }

  @Test
  public void testCostingV911() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final int day3 = 15;
    final BigDecimal price1 = new BigDecimal("15.00");
    final BigDecimal price2 = new BigDecimal("25.00");
    final BigDecimal price3 = new BigDecimal("35.00");
    final BigDecimal quantity1 = new BigDecimal("500");
    final BigDecimal quantity2 = new BigDecimal("400");
    final BigDecimal quantity3 = new BigDecimal("200");
    final BigDecimal quantity4 = new BigDecimal("300");
    final String costType = "AVA";

    try {

      OBContext.setOBContext(USER_ID, ROLE_ID, CLIENT_ID, ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product = createProduct("testCostingV911", price1, price2, costType);

      // Create purchase invoice, post it and assert it
      Invoice purchaseInvoice = createPurchaseInvoice(product, price1, quantity1, day0);

      // Create goods shipment, run costing background, post it and assert it
      ShipmentInOut goodsShipment = createGoodsShipment(product, price2, quantity2, day1);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt1 = createGoodsReceipt(purchaseInvoice, price2, quantity3, day2);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt2 = createGoodsReceipt(purchaseInvoice, price1, quantity4, day3);

      // Assert product transactions
      List<ProductTransactionAssert> productTransactionAssertList = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsShipment.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price2, price2));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt1.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price2, price3, price1));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt2.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price1));
      assertProductTransaction(product.getId(), productTransactionAssertList);

      // Assert product costing
      List<MaterialTransaction> transactionList = getProductTransactions(product.getId());
      List<ProductCostingAssert> productCostingAssertList = new ArrayList<ProductCostingAssert>();
      productCostingAssertList.add(new ProductCostingAssert(null, null, null, price2, null,
          costType));
      productCostingAssertList.add(new ProductCostingAssert(transactionList.get(1), price1, price2,
          price1, quantity2.negate().add(quantity3)));
      productCostingAssertList.add(new ProductCostingAssert(transactionList.get(2), price1, null,
          price1, quantity2.negate().add(quantity3).add(quantity4)));
      assertProductCosting(product.getId(), productCostingAssertList);

      // Assert cost adjustment
      List<CostAdjustment> costAdjustmentList = getCostAdjustment(product.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList1 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList1.add(new CostAdjustmentAssert(transactionList.get(1), "PDC",
          quantity3.multiply(price1).add(quantity3.multiply(price2).negate()), day0, true));
      costAdjustmentAssertLineList1.add(new CostAdjustmentAssert(transactionList.get(1), "NSC",
          quantity3.multiply(price3).add(quantity3.multiply(price1).negate()), day2, false, false));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList1);
      List<CostAdjustmentAssert> costAdjustmentAssertLineList2 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList2.add(new CostAdjustmentAssert(transactionList.get(2), "NSC",
          BigDecimal.ZERO, day3, true, false));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList2);
      assertCostAdjustment(costAdjustmentList, costAdjustmentAssertList);

      // Post cost adjustment 1 and assert it
      postDocument(costAdjustmentList.get(0));
      List<DocumentPostAssert> documentPostAssertList1 = new ArrayList<DocumentPostAssert>();
      documentPostAssertList1.add(new DocumentPostAssert("99904", quantity3.multiply(price2).add(
          quantity3.multiply(price1).negate()), BigDecimal.ZERO, null));
      documentPostAssertList1.add(new DocumentPostAssert("35000", BigDecimal.ZERO, quantity3
          .multiply(price2).add(quantity3.multiply(price1).negate()), null));
      documentPostAssertList1.add(new DocumentPostAssert("61000", BigDecimal.ZERO, quantity3
          .multiply(price3).add(quantity3.multiply(price1).negate()), null));
      documentPostAssertList1.add(new DocumentPostAssert("35000", quantity3.multiply(price3).add(
          quantity3.multiply(price1).negate()), BigDecimal.ZERO, null));
      CostAdjustment costAdjustment1 = OBDal.getInstance().get(CostAdjustment.class,
          costAdjustmentList.get(0).getId());
      assertDocumentPost(costAdjustment1, product.getId(), documentPostAssertList1);

      OBDal.getInstance().commitAndClose();

    } catch (Exception e) {
      System.out.println(e.getMessage());
      throw new OBException(e);
    }

    finally {
      OBContext.restorePreviousMode();
    }
  }

  @Test
  public void testCostingV10() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final int day3 = 15;
    final int day4 = 20;
    final int day5 = 25;
    final int day6 = 30;
    final int day7 = 35;
    final int day8 = 40;
    final int day9 = 45;
    final BigDecimal price1 = new BigDecimal("8.00");
    final BigDecimal price2 = new BigDecimal("9.00");
    final BigDecimal price3 = new BigDecimal("8.5493");
    final BigDecimal price4 = new BigDecimal("10.00");
    final BigDecimal price5 = new BigDecimal("8.6610");
    final BigDecimal price6 = new BigDecimal("9.3390");
    final BigDecimal price7 = new BigDecimal("9.4507");
    final BigDecimal price8 = new BigDecimal("9.0186");
    final BigDecimal quantity1 = new BigDecimal("1000");
    final BigDecimal quantity2 = new BigDecimal("150");
    final BigDecimal quantity3 = new BigDecimal("250");
    final BigDecimal quantity4 = new BigDecimal("10");
    final BigDecimal quantity5 = new BigDecimal("200");
    final BigDecimal quantity6 = new BigDecimal("120");
    final BigDecimal quantity7 = new BigDecimal("50");
    final BigDecimal quantity8 = new BigDecimal("280");

    try {

      OBContext.setOBContext(USER_ID, ROLE_ID, CLIENT_ID, ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product = createProduct("testCostingV10", price1);

      // Create purchase order and book it
      Order purchaseOrder = createPurchaseOrder(product, price1, quantity1, day0);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt1 = createGoodsReceipt(purchaseOrder, price1, quantity2, day1);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt2 = createGoodsReceipt(purchaseOrder, price1, quantity3, day2);

      // Create goods shipment, run costing background, post it and assert it
      ShipmentInOut goodsShipment1 = createGoodsShipment(product, price1, quantity4, day3);

      // Create purchase invoice, post it and assert it
      List<ShipmentInOut> goodsReceiptList1 = new ArrayList<ShipmentInOut>();
      goodsReceiptList1.add(goodsReceipt1);
      goodsReceiptList1.add(goodsReceipt2);
      List<BigDecimal> priceList1 = new ArrayList<BigDecimal>();
      priceList1.add(price2);
      priceList1.add(price2);
      createPurchaseInvoice(goodsReceiptList1, priceList1, quantity2.add(quantity3), day4);

      // Run price correction background
      runPriceBackground();

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt3 = createGoodsReceipt(purchaseOrder, price1, quantity5, day5);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt4 = createGoodsReceipt(purchaseOrder, price1, quantity6, day6);

      // Create goods shipment, run costing background, post it and assert it
      ShipmentInOut goodsShipment2 = createGoodsShipment(product, price3, quantity7, day7);

      // Create purchase invoice, post it and assert it
      List<ShipmentInOut> goodsReceiptList2 = new ArrayList<ShipmentInOut>();
      goodsReceiptList2.add(goodsReceipt3);
      goodsReceiptList2.add(goodsReceipt4);
      List<BigDecimal> priceList2 = new ArrayList<BigDecimal>();
      priceList2.add(price4);
      priceList2.add(price4);
      createPurchaseInvoice(goodsReceiptList2, priceList2, quantity5.add(quantity6), day8);

      // Run price correction background
      runPriceBackground();

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt5 = createGoodsReceipt(purchaseOrder, price1, quantity8, day9);

      // Run price correction background
      runPriceBackground();

      // Assert product transactions
      List<ProductTransactionAssert> productTransactionAssertList = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt1.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price2));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt2.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price2));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsShipment1.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price2));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt3.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price4));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt4.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price4));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsShipment2.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price3, price7));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt5.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price1));
      assertProductTransaction(product.getId(), productTransactionAssertList);

      // Assert product costing
      List<MaterialTransaction> transactionList = getProductTransactions(product.getId());
      List<ProductCostingAssert> productCostingAssertList = new ArrayList<ProductCostingAssert>();
      productCostingAssertList.add(new ProductCostingAssert(transactionList.get(0), price2, price1,
          price2, quantity2));
      productCostingAssertList.add(new ProductCostingAssert(transactionList.get(1), price2, price1,
          price2, quantity2.add(quantity3)));
      productCostingAssertList.add(new ProductCostingAssert(transactionList.get(3), price4, price5,
          price6, quantity2.add(quantity3).add((quantity4).negate()).add(quantity5)));
      productCostingAssertList
          .add(new ProductCostingAssert(transactionList.get(4), price4, price3, price7, quantity2
              .add(quantity3).add((quantity4).negate()).add(quantity5).add(quantity6)));
      productCostingAssertList.add(new ProductCostingAssert(transactionList.get(6), price1, null,
          price8, quantity2.add(quantity3).add((quantity4).negate()).add(quantity5).add(quantity6)
              .add((quantity7).negate()).add(quantity8)));
      assertProductCosting(product.getId(), productCostingAssertList);

      // Assert cost adjustment
      List<CostAdjustment> costAdjustmentList = getCostAdjustment(product.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList1 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList1.add(new CostAdjustmentAssert(transactionList.get(0), "PDC",
          quantity2.multiply(price2).add(quantity2.multiply(price1).negate()), day4, true));
      costAdjustmentAssertLineList1.add(new CostAdjustmentAssert(transactionList.get(1), "PDC",
          quantity3.multiply(price2).add(quantity3.multiply(price1).negate()), day4, true));
      costAdjustmentAssertLineList1.add(new CostAdjustmentAssert(transactionList.get(2), "PDC",
          quantity4.multiply(price2).add(quantity4.multiply(price1).negate()), day4, false));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList1);
      List<CostAdjustmentAssert> costAdjustmentAssertLineList2 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList2.add(new CostAdjustmentAssert(transactionList.get(3), "PDC",
          quantity5.multiply(price4).add(quantity5.multiply(price1).negate()), day8, true));
      costAdjustmentAssertLineList2.add(new CostAdjustmentAssert(transactionList.get(4), "PDC",
          quantity6.multiply(price4).add(quantity6.multiply(price1).negate()), day8, true));
      costAdjustmentAssertLineList2.add(new CostAdjustmentAssert(transactionList.get(5), "PDC",
          quantity7.multiply(price7).add(quantity7.multiply(price3).negate()), day8, false));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList2);
      assertCostAdjustment(costAdjustmentList, costAdjustmentAssertList);

      // Post cost adjustment 1 and assert it
      postDocument(costAdjustmentList.get(0));
      List<DocumentPostAssert> documentPostAssertList1 = new ArrayList<DocumentPostAssert>();
      documentPostAssertList1.add(new DocumentPostAssert("99904", BigDecimal.ZERO, quantity2
          .multiply(price2).add(quantity2.multiply(price1).negate()), null));
      documentPostAssertList1.add(new DocumentPostAssert("35000", quantity2.multiply(price2).add(
          quantity2.multiply(price1).negate()), BigDecimal.ZERO, null));
      documentPostAssertList1.add(new DocumentPostAssert("99904", BigDecimal.ZERO, quantity3
          .multiply(price2).add(quantity3.multiply(price1).negate()), null));
      documentPostAssertList1.add(new DocumentPostAssert("35000", quantity3.multiply(price2).add(
          quantity3.multiply(price1).negate()), BigDecimal.ZERO, null));
      documentPostAssertList1.add(new DocumentPostAssert("99900", quantity4.multiply(price2).add(
          quantity4.multiply(price1).negate()), BigDecimal.ZERO, null));
      documentPostAssertList1.add(new DocumentPostAssert("35000", BigDecimal.ZERO, quantity4
          .multiply(price2).add(quantity4.multiply(price1).negate()), null));
      CostAdjustment costAdjustment1 = OBDal.getInstance().get(CostAdjustment.class,
          costAdjustmentList.get(0).getId());
      assertDocumentPost(costAdjustment1, product.getId(), documentPostAssertList1);

      // Post cost adjustment 2 and assert it
      postDocument(costAdjustmentList.get(1));
      List<DocumentPostAssert> documentPostAssertList2 = new ArrayList<DocumentPostAssert>();
      documentPostAssertList2.add(new DocumentPostAssert("99904", BigDecimal.ZERO, quantity5
          .multiply(price4).add(quantity5.multiply(price1).negate()), null));
      documentPostAssertList2.add(new DocumentPostAssert("35000", quantity5.multiply(price4).add(
          quantity5.multiply(price1).negate()), BigDecimal.ZERO, null));
      documentPostAssertList2.add(new DocumentPostAssert("99904", BigDecimal.ZERO, quantity6
          .multiply(price4).add(quantity6.multiply(price1).negate()), null));
      documentPostAssertList2.add(new DocumentPostAssert("35000", quantity6.multiply(price4).add(
          quantity6.multiply(price1).negate()), BigDecimal.ZERO, null));
      documentPostAssertList2.add(new DocumentPostAssert("99900", quantity7.multiply(price7).add(
          quantity7.multiply(price3).negate()), BigDecimal.ZERO, null));
      documentPostAssertList2.add(new DocumentPostAssert("35000", BigDecimal.ZERO, quantity7
          .multiply(price7).add(quantity7.multiply(price3).negate()), null));
      CostAdjustment costAdjustment2 = OBDal.getInstance().get(CostAdjustment.class,
          costAdjustmentList.get(1).getId());
      assertDocumentPost(costAdjustment2, product.getId(), documentPostAssertList2);

      OBDal.getInstance().commitAndClose();

    } catch (Exception e) {
      System.out.println(e.getMessage());
      throw new OBException(e);
    }

    finally {
      OBContext.restorePreviousMode();
    }
  }

  @Test
  public void testCostingBD3() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final int day3 = 15;
    final int day4 = 20;
    final int day5 = 25;
    final int day6 = 30;
    final int day7 = 35;
    final BigDecimal price1 = new BigDecimal("120.00");
    final BigDecimal price2 = new BigDecimal("135.00");
    final BigDecimal price3 = new BigDecimal("127.50");
    final BigDecimal price4 = new BigDecimal("128.0357");
    final BigDecimal price5 = new BigDecimal("132.50");
    final BigDecimal price6 = new BigDecimal("135.00");
    final BigDecimal price7 = new BigDecimal("133.00");
    final BigDecimal quantity1 = new BigDecimal("75");
    final BigDecimal quantity2 = new BigDecimal("10");
    final BigDecimal quantity3 = new BigDecimal("50");
    final BigDecimal quantity4 = new BigDecimal("25");
    final BigDecimal quantity5 = new BigDecimal("15");

    try {

      OBContext.setOBContext(USER_ID, ROLE_ID, CLIENT_ID, ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product = createProduct("testCostingBD3", price1);

      // Create purchase order and book it
      Order purchaseOrder1 = createPurchaseOrder(product, price1, quantity1, day0);

      // Create purchase order and book it
      Order purchaseOrder2 = createPurchaseOrder(product, price2, quantity1, day1);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt1 = createGoodsReceipt(purchaseOrder2, price2, quantity1, day6);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt2 = createGoodsReceipt(purchaseOrder1, price1, quantity1, day2);

      // Create goods shipment, run costing background, post it and assert it
      ShipmentInOut goodsShipment1 = createGoodsShipment(product, price3, quantity2, day3);

      // Create goods shipment, run costing background, post it and assert it
      ShipmentInOut goodsShipment2 = createGoodsShipment(product, price4, quantity3, day4);

      // Create goods shipment, run costing background, post it and assert it
      ShipmentInOut goodsShipment3 = createGoodsShipment(product, price5, quantity4, day5);

      // Create goods shipment, run costing background, post it and assert it
      ShipmentInOut goodsShipment4 = createGoodsShipment(product, price6, quantity5, day7);

      // Run price correction background
      runPriceBackground();

      // Assert product transactions
      List<ProductTransactionAssert> productTransactionAssertList = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt2.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price1));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsShipment1.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price3, price1));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsShipment2.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price4, price1));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsShipment3.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price5, price1));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt1.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price2, price7, price2));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsShipment4.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price6, price6));
      assertProductTransaction(product.getId(), productTransactionAssertList);

      // Assert product costing
      List<MaterialTransaction> transactionList = getProductTransactions(product.getId());
      List<ProductCostingAssert> productCostingAssertList = new ArrayList<ProductCostingAssert>();
      productCostingAssertList.add(new ProductCostingAssert(transactionList.get(0), price1, price3,
          price1, quantity1));
      productCostingAssertList.add(new ProductCostingAssert(transactionList.get(4), price2, price2,
          price2, quantity1.subtract(quantity2)));
      assertProductCosting(product.getId(), productCostingAssertList);

      // Assert cost adjustment
      List<CostAdjustment> costAdjustmentList = getCostAdjustment(product.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList1 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList1.add(new CostAdjustmentAssert(transactionList.get(0), "BDT",
          BigDecimal.ZERO, day2, true));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList1);
      List<CostAdjustmentAssert> costAdjustmentAssertLineList2 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList2.add(new CostAdjustmentAssert(transactionList.get(1), "BDT",
          quantity2.multiply(price1).add(quantity2.multiply(price3).negate()), day3, true));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList2);
      List<CostAdjustmentAssert> costAdjustmentAssertLineList3 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList3.add(new CostAdjustmentAssert(transactionList.get(2), "BDT",
          quantity3.multiply(price1).add(quantity3.multiply(price4).negate()), day4, true));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList3);
      List<CostAdjustmentAssert> costAdjustmentAssertLineList4 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList4.add(new CostAdjustmentAssert(transactionList.get(3), "BDT",
          quantity4.multiply(price1).add(quantity4.multiply(price5).negate()), day5, true));
      costAdjustmentAssertLineList4.add(new CostAdjustmentAssert(transactionList.get(4), "NSC",
          quantity1.multiply(price7).add(quantity1.multiply(price2).negate()), day6, false, false));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList4);
      assertCostAdjustment(costAdjustmentList, costAdjustmentAssertList);

      // Post cost adjustment 1 and assert it
      postDocument(costAdjustmentList.get(1));
      List<DocumentPostAssert> documentPostAssertList1 = new ArrayList<DocumentPostAssert>();
      documentPostAssertList1.add(new DocumentPostAssert("99900", BigDecimal.ZERO, quantity2
          .multiply(price3).add(quantity2.multiply(price1).negate()), null));
      documentPostAssertList1.add(new DocumentPostAssert("35000", quantity2.multiply(price3).add(
          quantity2.multiply(price1).negate()), BigDecimal.ZERO, null));
      CostAdjustment costAdjustment1 = OBDal.getInstance().get(CostAdjustment.class,
          costAdjustmentList.get(1).getId());
      assertDocumentPost(costAdjustment1, product.getId(), documentPostAssertList1);

      // Post cost adjustment 2 and assert it
      postDocument(costAdjustmentList.get(2));
      List<DocumentPostAssert> documentPostAssertList2 = new ArrayList<DocumentPostAssert>();
      documentPostAssertList2.add(new DocumentPostAssert("99900", BigDecimal.ZERO, quantity3
          .multiply(price4).add(quantity3.multiply(price1).negate()), null));
      documentPostAssertList2.add(new DocumentPostAssert("35000", quantity3.multiply(price4).add(
          quantity3.multiply(price1).negate()), BigDecimal.ZERO, null));
      CostAdjustment costAdjustment2 = OBDal.getInstance().get(CostAdjustment.class,
          costAdjustmentList.get(2).getId());
      assertDocumentPost(costAdjustment2, product.getId(), documentPostAssertList2);

      // Post cost adjustment 3 and assert it
      postDocument(costAdjustmentList.get(3));
      List<DocumentPostAssert> documentPostAssertList3 = new ArrayList<DocumentPostAssert>();
      documentPostAssertList3.add(new DocumentPostAssert("99900", BigDecimal.ZERO, quantity4
          .multiply(price5).add(quantity4.multiply(price1).negate()), null));
      documentPostAssertList3.add(new DocumentPostAssert("35000", quantity4.multiply(price5).add(
          quantity4.multiply(price1).negate()), BigDecimal.ZERO, null));
      documentPostAssertList3.add(new DocumentPostAssert("61000", quantity1.multiply(price2).add(
          quantity1.multiply(price7).negate()), BigDecimal.ZERO, null));
      documentPostAssertList3.add(new DocumentPostAssert("35000", BigDecimal.ZERO, quantity1
          .multiply(price2).add(quantity1.multiply(price7).negate()), null));
      CostAdjustment costAdjustment3 = OBDal.getInstance().get(CostAdjustment.class,
          costAdjustmentList.get(3).getId());
      assertDocumentPost(costAdjustment3, product.getId(), documentPostAssertList3);

      OBDal.getInstance().commitAndClose();

    } catch (Exception e) {
      System.out.println(e.getMessage());
      throw new OBException(e);
    }

    finally {
      OBContext.restorePreviousMode();
    }
  }

  @Test
  public void testCostingE1() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final int day3 = 15;
    final int day4 = 20;
    final BigDecimal price1 = new BigDecimal("111.00");
    final BigDecimal price2 = new BigDecimal("110.00");
    final BigDecimal quantity1 = new BigDecimal("250");
    final BigDecimal quantity2 = new BigDecimal("150");

    try {

      OBContext.setOBContext(USER_ID, ROLE_ID, CLIENT_ID, ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product = createProduct("testCostingE1", price1);

      // Create purchase order and book it
      Order purchaseOrder = createPurchaseOrder(product, price1, quantity1, day0);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt = createGoodsReceipt(purchaseOrder, price1, quantity1, day1);

      // Add sleep to avoid assert errors
      Thread.sleep(1000);

      // Create inventory amount update and run costing background
      InventoryAmountUpdate inventoryAmountUpdate = createInventoryAmountUpdate(product, price1,
          price2, quantity1, day2);

      // Create goods shipment, run costing background, post it and assert it
      ShipmentInOut goodsShipment = createGoodsShipment(product, price2, quantity2, day4);

      // Create purchase invoice, post it and assert it
      createPurchaseInvoice(goodsReceipt, price1, quantity1, day3);

      // Run price correction background
      runPriceBackground();

      // Assert product transactions
      List<ProductTransactionAssert> productTransactionAssertList = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price1));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(InventoryAmountUpdate.class, inventoryAmountUpdate.getId())
          .getInventoryAmountUpdateLineList().get(0), price1, price1));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(InventoryAmountUpdate.class, inventoryAmountUpdate.getId())
          .getInventoryAmountUpdateLineList().get(0), price2, price2, true));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsShipment.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price2, price2));
      assertProductTransaction(product.getId(), productTransactionAssertList);

      // Assert product costing
      List<MaterialTransaction> transactionList = getProductTransactions(product.getId());
      List<ProductCostingAssert> productCostingAssertList = new ArrayList<ProductCostingAssert>();
      productCostingAssertList.add(new ProductCostingAssert(transactionList.get(0), price1, null,
          price1, quantity1));
      productCostingAssertList.add(new ProductCostingAssert(transactionList.get(2), price2, null,
          price2, quantity1));
      assertProductCosting(product.getId(), productCostingAssertList);

      // Assert cost adjustment
      assertEquals(getCostAdjustment(product.getId()), null);

      OBDal.getInstance().commitAndClose();

    } catch (Exception e) {
      System.out.println(e.getMessage());
      throw new OBException(e);
    }

    finally {
      OBContext.restorePreviousMode();
    }
  }

  @Test
  public void testCostingF2() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final int day3 = 15;
    final int day4 = 20;
    final BigDecimal price1 = new BigDecimal("1230.00");
    final BigDecimal price2 = new BigDecimal("1200.00");
    final BigDecimal price3 = new BigDecimal("1500.00");
    final BigDecimal price4 = new BigDecimal("1174.50");
    final BigDecimal quantity1 = new BigDecimal("185");
    final BigDecimal quantity2 = new BigDecimal("85");

    try {

      OBContext.setOBContext(USER_ID, ROLE_ID, CLIENT_ID, ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product = createProduct("testCostingF2", price1);

      // Create purchase order and book it
      Order purchaseOrder = createPurchaseOrder(product, price1, quantity1, day0);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt = createGoodsReceipt(purchaseOrder, price1, quantity1, day1);

      // Add sleep to avoid assert errors
      Thread.sleep(1000);

      // Create goods shipment, run costing background, post it and assert it
      ShipmentInOut goodsShipment = createGoodsShipment(product, price1, quantity2, day4);

      // Add sleep to avoid assert errors
      Thread.sleep(1000);

      // Create inventory amount update and run costing background
      InventoryAmountUpdate inventoryAmountUpdate = createInventoryAmountUpdate(product, price1,
          price2, quantity1, day2);

      // Create purchase invoice, post it and assert it
      createPurchaseInvoice(goodsReceipt, price3, quantity1, day3);

      // Run price correction background
      runPriceBackground();

      // Assert product transactions
      List<ProductTransactionAssert> productTransactionAssertList = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price3));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(InventoryAmountUpdate.class, inventoryAmountUpdate.getId())
          .getInventoryAmountUpdateLineList().get(0), price1, price3));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(InventoryAmountUpdate.class, inventoryAmountUpdate.getId())
          .getInventoryAmountUpdateLineList().get(0), price2, price2, true));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsShipment.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price2));
      assertProductTransaction(product.getId(), productTransactionAssertList);

      // Assert product costing
      List<MaterialTransaction> transactionList = getProductTransactions(product.getId());
      List<ProductCostingAssert> productCostingAssertList = new ArrayList<ProductCostingAssert>();
      productCostingAssertList.add(new ProductCostingAssert(transactionList.get(0), price3, price1,
          price3, quantity1));
      productCostingAssertList.add(new ProductCostingAssert(transactionList.get(2), price2, price4,
          price2, quantity1));
      assertProductCosting(product.getId(), productCostingAssertList);

      // Assert cost adjustment
      List<CostAdjustment> costAdjustmentList = getCostAdjustment(product.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList1 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList1.add(new CostAdjustmentAssert(transactionList.get(1), "BDT",
          BigDecimal.ZERO, day2, true));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList1);
      List<CostAdjustmentAssert> costAdjustmentAssertLineList2 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList2.add(new CostAdjustmentAssert(transactionList.get(2), "BDT",
          BigDecimal.ZERO, day2, true));
      costAdjustmentAssertLineList2.add(new CostAdjustmentAssert(transactionList.get(3), "PDC",
          quantity2.multiply(price2).add(quantity2.multiply(price1).negate()), day4, false));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList2);
      List<CostAdjustmentAssert> costAdjustmentAssertLineList3 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList3.add(new CostAdjustmentAssert(transactionList.get(0), "PDC",
          quantity1.multiply(price3).add(quantity1.multiply(price1).negate()), day3, true));
      costAdjustmentAssertLineList3.add(new CostAdjustmentAssert(transactionList.get(1), "PDC",
          quantity1.multiply(price3).add(quantity1.multiply(price1).negate()), day3, false));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList3);
      assertCostAdjustment(costAdjustmentList, costAdjustmentAssertList);

      // Post cost adjustment 2 and assert it
      postDocument(costAdjustmentList.get(1));
      List<DocumentPostAssert> documentPostAssertList1 = new ArrayList<DocumentPostAssert>();
      documentPostAssertList1.add(new DocumentPostAssert("99900", BigDecimal.ZERO, quantity2
          .multiply(price1).add(quantity2.multiply(price2).negate()), null));
      documentPostAssertList1.add(new DocumentPostAssert("35000", quantity2.multiply(price1).add(
          quantity2.multiply(price2).negate()), BigDecimal.ZERO, null));
      CostAdjustment costAdjustment1 = OBDal.getInstance().get(CostAdjustment.class,
          costAdjustmentList.get(1).getId());
      assertDocumentPost(costAdjustment1, product.getId(), documentPostAssertList1);

      // Post cost adjustment 3 and assert it
      postDocument(costAdjustmentList.get(2));
      List<DocumentPostAssert> documentPostAssertList2 = new ArrayList<DocumentPostAssert>();
      documentPostAssertList2.add(new DocumentPostAssert("99904", BigDecimal.ZERO, quantity1
          .multiply(price3).add(quantity1.multiply(price1).negate()), null));
      documentPostAssertList2.add(new DocumentPostAssert("35000", quantity1.multiply(price3).add(
          quantity1.multiply(price1).negate()), BigDecimal.ZERO, null));
      documentPostAssertList2.add(new DocumentPostAssert("61000", BigDecimal.ZERO, quantity1
          .multiply(price3).add(quantity1.multiply(price1).negate()), null));
      documentPostAssertList2.add(new DocumentPostAssert("35000", quantity1.multiply(price3).add(
          quantity1.multiply(price1).negate()), BigDecimal.ZERO, null));
      CostAdjustment costAdjustment2 = OBDal.getInstance().get(CostAdjustment.class,
          costAdjustmentList.get(2).getId());
      assertDocumentPost(costAdjustment2, product.getId(), documentPostAssertList2);

      OBDal.getInstance().commitAndClose();

    } catch (Exception e) {
      System.out.println(e.getMessage());
      throw new OBException(e);
    }

    finally {
      OBContext.restorePreviousMode();
    }
  }

  @Test
  public void testCostingGG() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final int day3 = 15;
    final int day4 = 20;
    final int day5 = 25;
    final BigDecimal price1 = new BigDecimal("15.00");
    final BigDecimal price2 = new BigDecimal("12.00");
    final BigDecimal price3 = new BigDecimal("13.50");
    final BigDecimal quantity1 = new BigDecimal("100");
    final BigDecimal quantity2 = new BigDecimal("200");

    try {

      OBContext.setOBContext(USER_ID, ROLE_ID, CLIENT_ID, ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product = createProduct("testCostingGG", price1);

      // Create purchase order and book it
      Order purchaseOrder1 = createPurchaseOrder(product, price1, quantity1, day0);

      // Create purchase order and book it
      Order purchaseOrder2 = createPurchaseOrder(product, price2, quantity1, day1);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt1 = createGoodsReceipt(purchaseOrder1, price1, quantity1, day2);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt2 = createGoodsReceipt(purchaseOrder2, price2, quantity1, day3);

      // Create purchase invoice, post it and assert it
      List<ShipmentInOut> goodsReceiptList = new ArrayList<ShipmentInOut>();
      goodsReceiptList.add(goodsReceipt1);
      goodsReceiptList.add(goodsReceipt2);
      List<BigDecimal> priceList = new ArrayList<BigDecimal>();
      priceList.add(price1);
      priceList.add(price2);
      createPurchaseInvoice(goodsReceiptList, priceList, quantity2, day5);

      // Update transaction total cost amount
      manualCostAdjustment(getProductTransactions(product.getId()).get(1),
          quantity1.multiply(price1), false, day4);

      // Assert product transactions
      List<ProductTransactionAssert> productTransactionAssertList = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt1.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price1, true, false));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt2.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price2, price1, true, true));
      assertProductTransaction(product.getId(), productTransactionAssertList);

      // Assert product costing
      List<MaterialTransaction> transactionList = getProductTransactions(product.getId());
      List<ProductCostingAssert> productCostingAssertList = new ArrayList<ProductCostingAssert>();
      productCostingAssertList.add(new ProductCostingAssert(transactionList.get(0), price1, null,
          price1, quantity1));
      productCostingAssertList.add(new ProductCostingAssert(transactionList.get(1), price1, price3,
          price1, quantity2));
      assertProductCosting(product.getId(), productCostingAssertList);

      // Assert cost adjustment
      List<CostAdjustment> costAdjustmentList = getCostAdjustment(product.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList.add(new CostAdjustmentAssert(transactionList.get(1), "MCC",
          quantity1.multiply(price1).add(quantity1.multiply(price2).negate()), day4, true));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList);
      assertCostAdjustment(costAdjustmentList, costAdjustmentAssertList);

      // Post cost adjustment 1 and assert it
      postDocument(costAdjustmentList.get(0));
      List<DocumentPostAssert> documentPostAssertList = new ArrayList<DocumentPostAssert>();
      documentPostAssertList.add(new DocumentPostAssert("61000", BigDecimal.ZERO, quantity1
          .multiply(price1).add(quantity1.multiply(price2).negate()), null));
      documentPostAssertList.add(new DocumentPostAssert("35000", quantity1.multiply(price1).add(
          quantity1.multiply(price2).negate()), BigDecimal.ZERO, null));
      CostAdjustment costAdjustment = OBDal.getInstance().get(CostAdjustment.class,
          costAdjustmentList.get(0).getId());
      assertDocumentPost(costAdjustment, product.getId(), documentPostAssertList);

      OBDal.getInstance().commitAndClose();

    } catch (Exception e) {
      System.out.println(e.getMessage());
      throw new OBException(e);
    }

    finally {
      OBContext.restorePreviousMode();
    }
  }

  @Test
  public void testCostingH1() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final int day3 = 15;
    final int day4 = 20;
    final int day5 = 25;
    final int day6 = 30;
    final BigDecimal price1 = new BigDecimal("105.00");
    final BigDecimal price2 = new BigDecimal("105.50");
    final BigDecimal price3 = new BigDecimal("105.25");
    final BigDecimal price4 = new BigDecimal("106.00");
    final BigDecimal quantity1 = new BigDecimal("100");
    final BigDecimal quantity2 = new BigDecimal("200");
    final BigDecimal quantity3 = new BigDecimal("150");

    try {

      OBContext.setOBContext(USER_ID, ROLE_ID, CLIENT_ID, ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product = createProduct("testCostingH1", price1);

      // Create purchase order and book it
      Order purchaseOrder1 = createPurchaseOrder(product, price1, quantity1, day0);

      // Create purchase order and book it
      Order purchaseOrder2 = createPurchaseOrder(product, price2, quantity1, day1);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt1 = createGoodsReceipt(purchaseOrder1, price1, quantity1, day2);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt2 = createGoodsReceipt(purchaseOrder2, price2, quantity1, day3);

      // Create purchase invoice, post it and assert it
      List<ShipmentInOut> goodsReceiptList = new ArrayList<ShipmentInOut>();
      goodsReceiptList.add(goodsReceipt1);
      goodsReceiptList.add(goodsReceipt2);
      List<BigDecimal> priceList = new ArrayList<BigDecimal>();
      priceList.add(price1);
      priceList.add(price2);
      createPurchaseInvoice(goodsReceiptList, priceList, quantity2, day4);

      // Create goods shipment, run costing background, post it and assert it
      ShipmentInOut goodsShipment = createGoodsShipment(product, price3, quantity3, day6);

      // Update transaction total cost amount
      manualCostAdjustment(getProductTransactions(product.getId()).get(1),
          quantity1.multiply(price4), false, day5);

      // Assert product transactions
      List<ProductTransactionAssert> productTransactionAssertList = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt1.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price1, true, false));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt2.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price2, price4, true, true));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsShipment.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price3, price2));
      assertProductTransaction(product.getId(), productTransactionAssertList);

      // Assert product costing
      List<MaterialTransaction> transactionList = getProductTransactions(product.getId());
      List<ProductCostingAssert> productCostingAssertList = new ArrayList<ProductCostingAssert>();
      productCostingAssertList.add(new ProductCostingAssert(transactionList.get(0), price1, null,
          price1, quantity1));
      productCostingAssertList.add(new ProductCostingAssert(transactionList.get(1), price4, price3,
          price2, quantity2));
      assertProductCosting(product.getId(), productCostingAssertList);

      // Assert cost adjustment
      List<CostAdjustment> costAdjustmentList = getCostAdjustment(product.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList.add(new CostAdjustmentAssert(transactionList.get(1), "MCC",
          quantity1.multiply(price4).add(quantity1.multiply(price2).negate()), day5, true));
      costAdjustmentAssertLineList.add(new CostAdjustmentAssert(transactionList.get(2), "MCC",
          quantity3.multiply(price2).add(quantity3.multiply(price3).negate()), day6, false));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList);
      assertCostAdjustment(costAdjustmentList, costAdjustmentAssertList);

      // Post cost adjustment 1 and assert it
      postDocument(costAdjustmentList.get(0));
      List<DocumentPostAssert> documentPostAssertList = new ArrayList<DocumentPostAssert>();
      documentPostAssertList.add(new DocumentPostAssert("61000", BigDecimal.ZERO, quantity1
          .multiply(price4).add(quantity1.multiply(price2).negate()), null));
      documentPostAssertList.add(new DocumentPostAssert("35000", quantity1.multiply(price4).add(
          quantity1.multiply(price2).negate()), BigDecimal.ZERO, null));
      documentPostAssertList.add(new DocumentPostAssert("99900", quantity3.multiply(price2).add(
          quantity3.multiply(price3).negate()), BigDecimal.ZERO, null));
      documentPostAssertList.add(new DocumentPostAssert("35000", BigDecimal.ZERO, quantity3
          .multiply(price2).add(quantity3.multiply(price3).negate()), null));
      CostAdjustment costAdjustment = OBDal.getInstance().get(CostAdjustment.class,
          costAdjustmentList.get(0).getId());
      assertDocumentPost(costAdjustment, product.getId(), documentPostAssertList);

      OBDal.getInstance().commitAndClose();

    } catch (Exception e) {
      System.out.println(e.getMessage());
      throw new OBException(e);
    }

    finally {
      OBContext.restorePreviousMode();
    }
  }

  @Test
  public void testCostingII() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final int year = 1;
    final BigDecimal price1 = new BigDecimal("95.00");
    final BigDecimal price2 = new BigDecimal("100.00");
    final BigDecimal quantity1 = new BigDecimal("1500");
    final BigDecimal quantity2 = new BigDecimal("100");
    final String costType = "STA";

    try {

      OBContext.setOBContext(USER_ID, ROLE_ID, CLIENT_ID, ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product = createProduct("testCostingII", price1, price2, costType, year);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt = createGoodsReceipt(product, price2, quantity1, day0);

      // Create goods shipment, run costing background, post it and assert it
      ShipmentInOut goodsShipment = createGoodsShipment(product, price2, quantity2, day2);

      // Create purchase invoice, post it and assert it
      createPurchaseInvoice(goodsReceipt, price1, quantity1, day1);

      // Run price correction background
      runPriceBackground();

      // Assert product transactions
      List<ProductTransactionAssert> productTransactionAssertList = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price2, price1));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsShipment.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price2, price1));
      assertProductTransaction(product.getId(), productTransactionAssertList);

      // Assert product costing
      List<MaterialTransaction> transactionList = getProductTransactions(product.getId());
      List<ProductCostingAssert> productCostingAssertList = new ArrayList<ProductCostingAssert>();
      productCostingAssertList.add(new ProductCostingAssert(null, null, null, price2, null,
          costType, year));
      productCostingAssertList.add(new ProductCostingAssert(transactionList.get(0), price1, price2,
          price1, quantity1));
      assertProductCosting(product.getId(), productCostingAssertList);

      // Assert cost adjustment
      List<CostAdjustment> costAdjustmentList = getCostAdjustment(product.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList.add(new CostAdjustmentAssert(transactionList.get(0), "PDC",
          quantity1.multiply(price1).add(quantity1.multiply(price2).negate()), day1, true));
      costAdjustmentAssertLineList.add(new CostAdjustmentAssert(transactionList.get(1), "PDC",
          quantity2.multiply(price1).add(quantity2.multiply(price2).negate()), day2, false));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList);
      assertCostAdjustment(costAdjustmentList, costAdjustmentAssertList);

      // Post cost adjustment and assert it
      postDocument(costAdjustmentList.get(0));
      List<DocumentPostAssert> documentPostAssertList = new ArrayList<DocumentPostAssert>();
      documentPostAssertList.add(new DocumentPostAssert("99904", quantity1.multiply(price2).add(
          quantity1.multiply(price1).negate()), BigDecimal.ZERO, null));
      documentPostAssertList.add(new DocumentPostAssert("35000", BigDecimal.ZERO, quantity1
          .multiply(price2).add(quantity1.multiply(price1).negate()), null));
      documentPostAssertList.add(new DocumentPostAssert("99900", BigDecimal.ZERO, quantity2
          .multiply(price2).add(quantity2.multiply(price1).negate()), null));
      documentPostAssertList.add(new DocumentPostAssert("35000", quantity2.multiply(price2).add(
          quantity2.multiply(price1).negate()), BigDecimal.ZERO, null));
      CostAdjustment costAdjustment = OBDal.getInstance().get(CostAdjustment.class,
          costAdjustmentList.get(0).getId());
      assertDocumentPost(costAdjustment, product.getId(), documentPostAssertList);

      OBDal.getInstance().commitAndClose();

    } catch (Exception e) {
      System.out.println(e.getMessage());
      throw new OBException(e);
    }

    finally {
      OBContext.restorePreviousMode();
    }
  }

  @Test
  public void testCostingJJ() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final int year = -1;
    final BigDecimal price1 = new BigDecimal("95.00");
    final BigDecimal price2 = new BigDecimal("100.00");
    final BigDecimal price3 = new BigDecimal("195.00");
    final BigDecimal quantity1 = new BigDecimal("1500");
    final BigDecimal quantity2 = new BigDecimal("500");
    final String costType = "STA";

    try {

      OBContext.setOBContext(USER_ID, ROLE_ID, CLIENT_ID, ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product = createProduct("testCostingJJ", price1, price2, costType, year);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt = createGoodsReceipt(product, price1, quantity1, day0);

      // Create goods shipment, run costing background, post it and assert it
      ShipmentInOut goodsShipment = createGoodsShipment(product, price1, quantity2, day2);

      // Create purchase invoice, post it and assert it
      createPurchaseInvoice(goodsReceipt, price3, quantity1, day1);

      // Run price correction background
      runPriceBackground();

      // Assert product transactions
      List<ProductTransactionAssert> productTransactionAssertList = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price3));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsShipment.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price3));
      assertProductTransaction(product.getId(), productTransactionAssertList);

      // Assert product costing
      List<MaterialTransaction> transactionList = getProductTransactions(product.getId());
      List<ProductCostingAssert> productCostingAssertList = new ArrayList<ProductCostingAssert>();
      productCostingAssertList.add(new ProductCostingAssert(null, null, null, price2, null,
          costType, year));
      productCostingAssertList.add(new ProductCostingAssert(transactionList.get(0), price3, price1,
          price3, quantity1));
      assertProductCosting(product.getId(), productCostingAssertList);

      // Assert cost adjustment
      List<CostAdjustment> costAdjustmentList = getCostAdjustment(product.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList.add(new CostAdjustmentAssert(transactionList.get(0), "PDC",
          quantity1.multiply(price3).add(quantity1.multiply(price1).negate()), day1, true));
      costAdjustmentAssertLineList.add(new CostAdjustmentAssert(transactionList.get(1), "PDC",
          quantity2.multiply(price3).add(quantity2.multiply(price1).negate()), day2, false));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList);
      assertCostAdjustment(costAdjustmentList, costAdjustmentAssertList);

      // Post cost adjustment and assert it
      postDocument(costAdjustmentList.get(0));
      List<DocumentPostAssert> documentPostAssertList = new ArrayList<DocumentPostAssert>();
      documentPostAssertList.add(new DocumentPostAssert("99904", BigDecimal.ZERO, quantity1
          .multiply(price3).add(quantity1.multiply(price1).negate()), null));
      documentPostAssertList.add(new DocumentPostAssert("35000", quantity1.multiply(price3).add(
          quantity1.multiply(price1).negate()), BigDecimal.ZERO, null));
      documentPostAssertList.add(new DocumentPostAssert("99900", quantity2.multiply(price3).add(
          quantity2.multiply(price1).negate()), BigDecimal.ZERO, null));
      documentPostAssertList.add(new DocumentPostAssert("35000", BigDecimal.ZERO, quantity2
          .multiply(price3).add(quantity2.multiply(price1).negate()), null));
      CostAdjustment costAdjustment = OBDal.getInstance().get(CostAdjustment.class,
          costAdjustmentList.get(0).getId());
      assertDocumentPost(costAdjustment, product.getId(), documentPostAssertList);

      OBDal.getInstance().commitAndClose();

    } catch (Exception e) {
      System.out.println(e.getMessage());
      throw new OBException(e);
    }

    finally {
      OBContext.restorePreviousMode();
    }
  }

  @Test
  public void testCostingJJJ() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final int day3 = 15;
    final int year = -1;
    final BigDecimal price1 = new BigDecimal("95.00");
    final BigDecimal price2 = new BigDecimal("100.00");
    final BigDecimal price3 = new BigDecimal("195.00");
    final BigDecimal quantity1 = new BigDecimal("1500");
    final BigDecimal quantity2 = new BigDecimal("500");
    final BigDecimal quantity3 = new BigDecimal("50");
    final String costType = "STA";

    try {

      OBContext.setOBContext(USER_ID, ROLE_ID, CLIENT_ID, ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product = createProduct("testCostingJJJ", price1, price2, costType, year);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt = createGoodsReceipt(product, price1, quantity1, day0);

      // Create goods shipment, run costing background, post it and assert it
      ShipmentInOut goodsShipment1 = createGoodsShipment(product, price1, quantity2, day1);

      // Create purchase invoice, post it and assert it
      createPurchaseInvoice(goodsReceipt, price3, quantity1, day2);

      // Run price correction background
      runPriceBackground();

      // Create goods shipment, run costing background, post it and assert it
      ShipmentInOut goodsShipment2 = createGoodsShipment(product, price3, quantity3, day3);

      // Run price correction background
      runPriceBackground();

      // Assert product transactions
      List<ProductTransactionAssert> productTransactionAssertList = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price3));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsShipment1.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price3));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsShipment2.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price3, price3));
      assertProductTransaction(product.getId(), productTransactionAssertList);

      // Assert product costing
      List<MaterialTransaction> transactionList = getProductTransactions(product.getId());
      List<ProductCostingAssert> productCostingAssertList = new ArrayList<ProductCostingAssert>();
      productCostingAssertList.add(new ProductCostingAssert(null, null, null, price2, null,
          costType, year));
      productCostingAssertList.add(new ProductCostingAssert(transactionList.get(0), price3, price1,
          price3, quantity1));
      assertProductCosting(product.getId(), productCostingAssertList);

      // Assert cost adjustment
      List<CostAdjustment> costAdjustmentList = getCostAdjustment(product.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList.add(new CostAdjustmentAssert(transactionList.get(0), "PDC",
          quantity1.multiply(price3).add(quantity1.multiply(price1).negate()), day2, true));
      costAdjustmentAssertLineList.add(new CostAdjustmentAssert(transactionList.get(1), "PDC",
          quantity2.multiply(price3).add(quantity2.multiply(price1).negate()), day2, false));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList);
      assertCostAdjustment(costAdjustmentList, costAdjustmentAssertList);

      // Post cost adjustment and assert it
      postDocument(costAdjustmentList.get(0));
      List<DocumentPostAssert> documentPostAssertList = new ArrayList<DocumentPostAssert>();
      documentPostAssertList.add(new DocumentPostAssert("99904", BigDecimal.ZERO, quantity1
          .multiply(price3).add(quantity1.multiply(price1).negate()), null));
      documentPostAssertList.add(new DocumentPostAssert("35000", quantity1.multiply(price3).add(
          quantity1.multiply(price1).negate()), BigDecimal.ZERO, null));
      documentPostAssertList.add(new DocumentPostAssert("99900", quantity2.multiply(price3).add(
          quantity2.multiply(price1).negate()), BigDecimal.ZERO, null));
      documentPostAssertList.add(new DocumentPostAssert("35000", BigDecimal.ZERO, quantity2
          .multiply(price3).add(quantity2.multiply(price1).negate()), null));
      CostAdjustment costAdjustment = OBDal.getInstance().get(CostAdjustment.class,
          costAdjustmentList.get(0).getId());
      assertDocumentPost(costAdjustment, product.getId(), documentPostAssertList);

      OBDal.getInstance().commitAndClose();

    } catch (Exception e) {
      System.out.println(e.getMessage());
      throw new OBException(e);
    }

    finally {
      OBContext.restorePreviousMode();
    }
  }

  @Test
  public void testCostingK2() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final int year = 1;
    final BigDecimal price1 = new BigDecimal("95.00");
    final BigDecimal price2 = new BigDecimal("100.00");
    final BigDecimal price3 = new BigDecimal("80.00");
    final BigDecimal quantity1 = new BigDecimal("100");
    final BigDecimal quantity2 = new BigDecimal("90");
    final BigDecimal quantity3 = new BigDecimal("45");
    final String costType = "STA";

    try {

      OBContext.setOBContext(USER_ID, ROLE_ID, CLIENT_ID, ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product = createProduct("testCostingK2", price1, price2, costType, year);

      // Create purchase order and book it
      createPurchaseOrder(product, price3, quantity1, day0);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt = createGoodsReceipt(product, price3, quantity2, day1);

      // Create goods shipment, run costing background, post it and assert it
      ShipmentInOut goodsShipment = createGoodsShipment(product, price3, quantity3, day2);

      // Create purchase invoice, post it and assert it
      createPurchaseInvoice(goodsReceipt, price1, quantity2, day2);

      // Run price correction background
      runPriceBackground();

      // Assert product transactions
      List<ProductTransactionAssert> productTransactionAssertList1 = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList1.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price3, price1));
      productTransactionAssertList1.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsShipment.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price3, price1));
      assertProductTransaction(product.getId(), productTransactionAssertList1);

      // Assert product costing
      List<MaterialTransaction> transactionList1 = getProductTransactions(product.getId());
      List<ProductCostingAssert> productCostingAssertList1 = new ArrayList<ProductCostingAssert>();
      productCostingAssertList1.add(new ProductCostingAssert(null, null, null, price2, null,
          costType, year));
      productCostingAssertList1.add(new ProductCostingAssert(transactionList1.get(0), price1,
          price3, price1, quantity2));
      assertProductCosting(product.getId(), productCostingAssertList1);

      // Assert cost adjustment
      List<CostAdjustment> costAdjustmentList1 = getCostAdjustment(product.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList1 = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList1 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList1.add(new CostAdjustmentAssert(transactionList1.get(0), "PDC",
          quantity2.multiply(price1).add(quantity2.multiply(price3).negate()), day2, true));
      costAdjustmentAssertLineList1.add(new CostAdjustmentAssert(transactionList1.get(1), "PDC",
          quantity3.multiply(price1).add(quantity3.multiply(price3).negate()), day2, false));
      costAdjustmentAssertList1.add(costAdjustmentAssertLineList1);
      assertCostAdjustment(costAdjustmentList1, costAdjustmentAssertList1);

      // Post cost adjustment and assert it
      postDocument(costAdjustmentList1.get(0));
      List<DocumentPostAssert> documentPostAssertList1 = new ArrayList<DocumentPostAssert>();
      documentPostAssertList1.add(new DocumentPostAssert("99904", BigDecimal.ZERO, quantity2
          .multiply(price1).add(quantity2.multiply(price3).negate()), null));
      documentPostAssertList1.add(new DocumentPostAssert("35000", quantity2.multiply(price1).add(
          quantity2.multiply(price3).negate()), BigDecimal.ZERO, null));
      documentPostAssertList1.add(new DocumentPostAssert("99900", quantity3.multiply(price1).add(
          quantity3.multiply(price3).negate()), BigDecimal.ZERO, null));
      documentPostAssertList1.add(new DocumentPostAssert("35000", BigDecimal.ZERO, quantity3
          .multiply(price1).add(quantity3.multiply(price3).negate()), null));
      CostAdjustment costAdjustment = OBDal.getInstance().get(CostAdjustment.class,
          costAdjustmentList1.get(0).getId());
      assertDocumentPost(costAdjustment, product.getId(), documentPostAssertList1);

      // Cancel Cost Adjustment
      cancelCostAdjustment(costAdjustment);

      // Assert product transactions
      List<ProductTransactionAssert> productTransactionAssertList2 = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList2.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price3, price3));
      productTransactionAssertList2.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsShipment.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price3, price3));
      assertProductTransaction(product.getId(), productTransactionAssertList2);

      // Assert product costing
      List<MaterialTransaction> transactionList2 = getProductTransactions(product.getId());
      List<ProductCostingAssert> productCostingAssertList2 = new ArrayList<ProductCostingAssert>();
      productCostingAssertList2.add(new ProductCostingAssert(null, null, null, price2, null,
          costType, year));
      productCostingAssertList2.add(new ProductCostingAssert(transactionList2.get(0), price3,
          price3, price3, quantity2));
      assertProductCosting(product.getId(), productCostingAssertList2);

      // Assert cost adjustment
      List<CostAdjustment> costAdjustmentList2 = getCostAdjustment(product.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList2 = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList21 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList21.add(new CostAdjustmentAssert(transactionList2.get(0), "PDC",
          quantity2.multiply(price1).add(quantity2.multiply(price3).negate()), day2, true, "VO"));
      costAdjustmentAssertLineList21.add(new CostAdjustmentAssert(transactionList2.get(1), "PDC",
          quantity3.multiply(price1).add(quantity3.multiply(price3).negate()), day2, false, "VO"));
      costAdjustmentAssertList2.add(costAdjustmentAssertLineList21);
      List<CostAdjustmentAssert> costAdjustmentAssertLineList22 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList22.add(new CostAdjustmentAssert(transactionList2.get(0), "PDC",
          quantity2.multiply(price3).add(quantity2.multiply(price1).negate()), day2, true, "VO"));
      costAdjustmentAssertLineList22.add(new CostAdjustmentAssert(transactionList2.get(1), "PDC",
          quantity3.multiply(price3).add(quantity3.multiply(price1).negate()), day2, false, "VO"));
      costAdjustmentAssertList2.add(costAdjustmentAssertLineList22);
      assertCostAdjustment(costAdjustmentList2, costAdjustmentAssertList2);

      // Post cost adjustment 1 and assert it
      List<DocumentPostAssert> documentPostAssertList21 = new ArrayList<DocumentPostAssert>();
      documentPostAssertList21.add(new DocumentPostAssert("99904", BigDecimal.ZERO, quantity2
          .multiply(price1).add(quantity2.multiply(price3).negate()), null));
      documentPostAssertList21.add(new DocumentPostAssert("35000", quantity2.multiply(price1).add(
          quantity2.multiply(price3).negate()), BigDecimal.ZERO, null));
      documentPostAssertList21.add(new DocumentPostAssert("99900", quantity3.multiply(price1).add(
          quantity3.multiply(price3).negate()), BigDecimal.ZERO, null));
      documentPostAssertList21.add(new DocumentPostAssert("35000", BigDecimal.ZERO, quantity3
          .multiply(price1).add(quantity3.multiply(price3).negate()), null));
      CostAdjustment costAdjustment21 = OBDal.getInstance().get(CostAdjustment.class,
          costAdjustmentList2.get(0).getId());
      assertDocumentPost(costAdjustment21, product.getId(), documentPostAssertList21);

      // Post cost adjustment 2 and assert it
      postDocument(costAdjustmentList2.get(1));
      List<DocumentPostAssert> documentPostAssertList22 = new ArrayList<DocumentPostAssert>();
      documentPostAssertList22.add(new DocumentPostAssert("99904", quantity2.multiply(price1).add(
          quantity2.multiply(price3).negate()), BigDecimal.ZERO, null));
      documentPostAssertList22.add(new DocumentPostAssert("35000", BigDecimal.ZERO, quantity2
          .multiply(price1).add(quantity2.multiply(price3).negate()), null));
      documentPostAssertList22.add(new DocumentPostAssert("99900", BigDecimal.ZERO, quantity3
          .multiply(price1).add(quantity3.multiply(price3).negate()), null));
      documentPostAssertList22.add(new DocumentPostAssert("35000", quantity3.multiply(price1).add(
          quantity3.multiply(price3).negate()), BigDecimal.ZERO, null));
      CostAdjustment costAdjustment22 = OBDal.getInstance().get(CostAdjustment.class,
          costAdjustmentList2.get(1).getId());
      assertDocumentPost(costAdjustment22, product.getId(), documentPostAssertList22);

      OBDal.getInstance().commitAndClose();

    } catch (Exception e) {
      System.out.println(e.getMessage());
      throw new OBException(e);
    }

    finally {
      OBContext.restorePreviousMode();
    }
  }

  @Test
  public void testCostingN0() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final int day3 = 15;
    final int day4 = 20;
    final BigDecimal price1 = new BigDecimal("10.00");
    final BigDecimal price2 = new BigDecimal("20.00");
    final BigDecimal price3 = new BigDecimal("30.00");
    final BigDecimal price4 = new BigDecimal("15.00");
    final BigDecimal quantity1 = new BigDecimal("100");
    final BigDecimal quantity2 = new BigDecimal("5");
    final BigDecimal quantity3 = new BigDecimal("10");

    try {

      OBContext.setOBContext(USER_ID, ROLE_ID, CLIENT_ID, ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product = createProduct("testCostingN0", price1);

      // Create purchase order and book it
      Order purchaseOrder1 = createPurchaseOrder(product, price1, quantity1, day0);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt1 = createGoodsReceipt(purchaseOrder1, price1, quantity1, day1);

      // Create goods shipment, run costing background, post it and assert it
      ShipmentInOut goodsShipment1 = createGoodsShipment(product, price1, quantity1, day2);

      // Create goods shipment, run costing background, post it and assert it
      ShipmentInOut goodsShipment2 = createGoodsShipment(product, price1, quantity2, day3);

      // Create purchase order and book it
      Order purchaseOrder2 = createPurchaseOrder(product, price2, quantity3, day4);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt2 = createGoodsReceipt(purchaseOrder2, price2, quantity3, day4);

      // Assert product transactions
      List<ProductTransactionAssert> productTransactionAssertList = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt1.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price1));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsShipment1.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price1));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsShipment2.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price1));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt2.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price2, price4, price2));
      assertProductTransaction(product.getId(), productTransactionAssertList);

      // Assert product costing
      List<MaterialTransaction> transactionList = getProductTransactions(product.getId());
      List<ProductCostingAssert> productCostingAssertList = new ArrayList<ProductCostingAssert>();
      productCostingAssertList.add(new ProductCostingAssert(transactionList.get(0), price1, null,
          price1, quantity1));
      productCostingAssertList.add(new ProductCostingAssert(transactionList.get(3), price2, price3,
          price2, quantity2));
      assertProductCosting(product.getId(), productCostingAssertList);

      // Assert cost adjustment
      List<CostAdjustment> costAdjustmentList = getCostAdjustment(product.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList.add(new CostAdjustmentAssert(transactionList.get(3), "NSC",
          quantity3.multiply(price4).add(quantity3.multiply(price2).negate()), day4, true, false));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList);
      assertCostAdjustment(costAdjustmentList, costAdjustmentAssertList);

      // Post cost adjustment and assert it
      postDocument(costAdjustmentList.get(0));
      List<DocumentPostAssert> documentPostAssertList1 = new ArrayList<DocumentPostAssert>();
      documentPostAssertList1.add(new DocumentPostAssert("61000", quantity3.multiply(price2).add(
          quantity3.multiply(price4).negate()), BigDecimal.ZERO, null));
      documentPostAssertList1.add(new DocumentPostAssert("35000", BigDecimal.ZERO, quantity3
          .multiply(price2).add(quantity3.multiply(price4).negate()), null));
      CostAdjustment costAdjustment = OBDal.getInstance().get(CostAdjustment.class,
          costAdjustmentList.get(0).getId());
      assertDocumentPost(costAdjustment, product.getId(), documentPostAssertList1);

      OBDal.getInstance().commitAndClose();

    } catch (Exception e) {
      System.out.println(e.getMessage());
      throw new OBException(e);
    }

    finally {
      OBContext.restorePreviousMode();
    }
  }

  @Test
  public void testCostingN1() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int year = 1;
    final BigDecimal price1 = new BigDecimal("20.00");
    final BigDecimal price2 = new BigDecimal("15.00");
    final BigDecimal price3 = new BigDecimal("10.00");
    final BigDecimal price4 = new BigDecimal("17.50");
    final BigDecimal quantity1 = new BigDecimal("100");
    final BigDecimal quantity2 = new BigDecimal("200");
    final String costType = "AVA";

    try {

      OBContext.setOBContext(USER_ID, ROLE_ID, CLIENT_ID, ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product = createProduct("testCostingN0", price1, price1, costType, year);

      // Create goods shipment, run costing background, post it and assert it
      ShipmentInOut goodsShipment = createGoodsShipment(product, price1, quantity1, day0);

      // Create purchase order and book it
      Order purchaseOrder = createPurchaseOrder(product, price2, quantity2, day1);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt = createGoodsReceipt(purchaseOrder, price2, quantity2, day1);

      // Assert product transactions
      List<ProductTransactionAssert> productTransactionAssertList = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsShipment.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price1));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price2, price4, price2));
      assertProductTransaction(product.getId(), productTransactionAssertList);

      // Assert product costing
      List<MaterialTransaction> transactionList = getProductTransactions(product.getId());
      List<ProductCostingAssert> productCostingAssertList = new ArrayList<ProductCostingAssert>();
      productCostingAssertList.add(new ProductCostingAssert(null, null, null, price1, null,
          costType, year));
      productCostingAssertList.add(new ProductCostingAssert(transactionList.get(1), price2, price3,
          price2, quantity1));
      assertProductCosting(product.getId(), productCostingAssertList);

      // Assert cost adjustment
      List<CostAdjustment> costAdjustmentList = getCostAdjustment(product.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList.add(new CostAdjustmentAssert(transactionList.get(1), "NSC",
          quantity2.multiply(price4).add(quantity2.multiply(price2).negate()), day1, true, false));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList);
      assertCostAdjustment(costAdjustmentList, costAdjustmentAssertList);

      // Post cost adjustment and assert it
      postDocument(costAdjustmentList.get(0));
      List<DocumentPostAssert> documentPostAssertList1 = new ArrayList<DocumentPostAssert>();
      documentPostAssertList1.add(new DocumentPostAssert("61000", BigDecimal.ZERO, quantity2
          .multiply(price4).add(quantity2.multiply(price2).negate()), null));
      documentPostAssertList1.add(new DocumentPostAssert("35000", quantity2.multiply(price4).add(
          quantity2.multiply(price2).negate()), BigDecimal.ZERO, null));
      CostAdjustment costAdjustment = OBDal.getInstance().get(CostAdjustment.class,
          costAdjustmentList.get(0).getId());
      assertDocumentPost(costAdjustment, product.getId(), documentPostAssertList1);

      OBDal.getInstance().commitAndClose();

    } catch (Exception e) {
      System.out.println(e.getMessage());
      throw new OBException(e);
    }

    finally {
      OBContext.restorePreviousMode();
    }
  }

  @Test
  public void testCostingN2() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final int day3 = 15;
    final BigDecimal price1 = new BigDecimal("5.00");
    final BigDecimal quantity1 = new BigDecimal("10");
    final BigDecimal quantity2 = new BigDecimal("8");
    final BigDecimal quantity3 = new BigDecimal("12");

    try {

      OBContext.setOBContext(USER_ID, ROLE_ID, CLIENT_ID, ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product = createProduct("testCostingN2", price1);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt1 = createGoodsReceipt(product, price1, quantity1, day0);

      // Create goods shipment, run costing background, post it and assert it
      ShipmentInOut goodsShipment1 = createGoodsShipment(product, price1, quantity2, day2);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt2 = createGoodsReceipt(product, price1, quantity1, day3);

      // Create goods shipment, run costing background, post it and assert it
      ShipmentInOut goodsShipment2 = createGoodsShipment(product, price1, quantity1, day1);

      // Assert product transactions
      List<ProductTransactionAssert> productTransactionAssertList = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt1.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price1));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsShipment2.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price1));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsShipment1.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price1));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt2.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price1));
      assertProductTransaction(product.getId(), productTransactionAssertList);

      // Assert product costing
      List<MaterialTransaction> transactionList = getProductTransactions(product.getId());
      List<ProductCostingAssert> productCostingAssertList = new ArrayList<ProductCostingAssert>();
      productCostingAssertList.add(new ProductCostingAssert(transactionList.get(0), price1, null,
          price1, quantity1));
      productCostingAssertList.add(new ProductCostingAssert(transactionList.get(3), price1, null,
          price1, quantity3.subtract(quantity1)));
      assertProductCosting(product.getId(), productCostingAssertList);

      // Assert cost adjustment
      List<CostAdjustment> costAdjustmentList = getCostAdjustment(product.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList.add(new CostAdjustmentAssert(transactionList.get(1), "BDT",
          BigDecimal.ZERO, day1, true));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList);
      assertCostAdjustment(costAdjustmentList, costAdjustmentAssertList);

      OBDal.getInstance().commitAndClose();

    } catch (Exception e) {
      System.out.println(e.getMessage());
      throw new OBException(e);
    }

    finally {
      OBContext.restorePreviousMode();
    }
  }

  @Test
  public void testCostingN5() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final int day3 = 15;
    final int day4 = 20;
    final BigDecimal price1 = new BigDecimal("5.00");
    final BigDecimal price2 = new BigDecimal("7.00");
    final BigDecimal price3 = new BigDecimal("6.6667");
    final BigDecimal price4 = new BigDecimal("5.40");
    final BigDecimal quantity1 = new BigDecimal("10");
    final BigDecimal quantity2 = new BigDecimal("8");
    final BigDecimal quantity3 = new BigDecimal("12");

    try {

      OBContext.setOBContext(USER_ID, ROLE_ID, CLIENT_ID, ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product = createProduct("testCostingN5", price1);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt1 = createGoodsReceipt(product, price1, quantity1, day0);

      // Create goods shipment, run costing background, post it and assert it
      ShipmentInOut goodsShipment1 = createGoodsShipment(product, price1, quantity2, day2);

      // Create purchase order and book it
      Order purchaseOrder = createPurchaseOrder(product, price2, quantity1, day3);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt2 = createGoodsReceipt(purchaseOrder, price2, quantity1, day4);

      // Create goods shipment, run costing background, post it and assert it
      ShipmentInOut goodsShipment2 = createGoodsShipment(product, price3, quantity1, day1);

      // Run price correction background
      runPriceBackground();

      // Assert product transactions
      List<ProductTransactionAssert> productTransactionAssertList = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt1.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price1));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsShipment2.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price3, price1));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsShipment1.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price1));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt2.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price2, price4, price2));
      assertProductTransaction(product.getId(), productTransactionAssertList);

      // Assert product costing
      List<MaterialTransaction> transactionList = getProductTransactions(product.getId());
      List<ProductCostingAssert> productCostingAssertList = new ArrayList<ProductCostingAssert>();
      productCostingAssertList.add(new ProductCostingAssert(transactionList.get(0), price1, null,
          price1, quantity1));
      productCostingAssertList.add(new ProductCostingAssert(transactionList.get(3), price2, price3,
          price2, quantity3.subtract(quantity1)));
      assertProductCosting(product.getId(), productCostingAssertList);

      // Assert cost adjustment
      List<CostAdjustment> costAdjustmentList = getCostAdjustment(product.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList.add(new CostAdjustmentAssert(transactionList.get(1), "BDT",
          quantity1.multiply(price1).add(quantity1.multiply(price3).negate()), day1, true));
      costAdjustmentAssertLineList.add(new CostAdjustmentAssert(transactionList.get(3), "NSC",
          quantity1.multiply(price4).add(quantity1.multiply(price2).negate()), day4, false, false));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList);
      assertCostAdjustment(costAdjustmentList, costAdjustmentAssertList);

      // Post cost adjustment and assert it
      postDocument(costAdjustmentList.get(0));
      List<DocumentPostAssert> documentPostAssertList1 = new ArrayList<DocumentPostAssert>();
      documentPostAssertList1.add(new DocumentPostAssert("99900", BigDecimal.ZERO, quantity1
          .multiply(price3).add(quantity1.multiply(price1).negate()), null));
      documentPostAssertList1.add(new DocumentPostAssert("35000", quantity1.multiply(price3).add(
          quantity1.multiply(price1).negate()), BigDecimal.ZERO, null));
      documentPostAssertList1.add(new DocumentPostAssert("61000", quantity1.multiply(price2).add(
          quantity1.multiply(price4).negate()), BigDecimal.ZERO, null));
      documentPostAssertList1.add(new DocumentPostAssert("35000", BigDecimal.ZERO, quantity1
          .multiply(price2).add(quantity1.multiply(price4).negate()), null));
      CostAdjustment costAdjustment = OBDal.getInstance().get(CostAdjustment.class,
          costAdjustmentList.get(0).getId());
      assertDocumentPost(costAdjustment, product.getId(), documentPostAssertList1);

      OBDal.getInstance().commitAndClose();

    } catch (Exception e) {
      System.out.println(e.getMessage());
      throw new OBException(e);
    }

    finally {
      OBContext.restorePreviousMode();
    }
  }

  @Test
  public void testCostingV11() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final int day3 = 15;
    final int day4 = 20;
    final int day5 = 25;
    final BigDecimal price1 = new BigDecimal("20.00");
    final BigDecimal price2 = new BigDecimal("15.00");
    final BigDecimal price3 = new BigDecimal("16.6667");
    final BigDecimal price4 = new BigDecimal("25.00");
    final BigDecimal price5 = new BigDecimal("18.3333");
    final BigDecimal price6 = new BigDecimal("21.6667");
    final BigDecimal quantity1 = new BigDecimal("100");
    final BigDecimal quantity2 = new BigDecimal("200");
    final BigDecimal quantity3 = new BigDecimal("300");
    final BigDecimal quantity4 = new BigDecimal("150");

    try {

      OBContext.setOBContext(USER_ID, ROLE_ID, CLIENT_ID, ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product = createProduct("testCostingV11", price1);

      // Create purchase order and book it
      Order purchaseOrder1 = createPurchaseOrder(product, price1, quantity1, day0);

      // Create purchase order and book it
      Order purchaseOrder2 = createPurchaseOrder(product, price2, quantity2, day1);

      // Create goods receipt, run costing background, post it and assert it
      List<Order> purchaseOrderList = new ArrayList<Order>();
      purchaseOrderList.add(purchaseOrder1);
      purchaseOrderList.add(purchaseOrder2);
      ShipmentInOut goodsReceipt = createGoodsReceipt(purchaseOrderList, price3, quantity3, day2);

      // Create goods shipment, run costing background, post it and assert it
      ShipmentInOut goodsShipment = createGoodsShipment(product, price3, quantity4, day5);

      // Create purchase invoice, post it and assert it
      createPurchaseInvoice(purchaseOrder1, price4, quantity1, day3);

      // Run price correction background
      runPriceBackground();

      // Create purchase invoice, post it and assert it
      createPurchaseInvoice(purchaseOrder2, price1, quantity2, day4);

      // Run price correction background
      runPriceBackground();

      // Assert product transactions
      List<ProductTransactionAssert> productTransactionAssertList = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price4));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(1), price2, price1));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsShipment.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price3, price6));
      assertProductTransaction(product.getId(), productTransactionAssertList);

      // Assert product costing
      List<MaterialTransaction> transactionList = getProductTransactions(product.getId());
      List<ProductCostingAssert> productCostingAssertList = new ArrayList<ProductCostingAssert>();
      productCostingAssertList.add(new ProductCostingAssert(transactionList.get(1), price1, price2,
          price1, quantity2));
      productCostingAssertList.add(new ProductCostingAssert(transactionList.get(0), price4, price3,
          price6, quantity3));
      assertProductCosting(product.getId(), productCostingAssertList);

      // Assert cost adjustment
      List<CostAdjustment> costAdjustmentList = getCostAdjustment(product.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList1 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList1.add(new CostAdjustmentAssert(transactionList.get(0), "PDC",
          quantity1.multiply(price4).add(quantity1.multiply(price1).negate()), day3, true));
      costAdjustmentAssertLineList1.add(new CostAdjustmentAssert(transactionList.get(2), "PDC",
          quantity4.multiply(price5).add(quantity4.multiply(price3).negate()), day5, false));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList1);
      List<CostAdjustmentAssert> costAdjustmentAssertLineList2 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList2.add(new CostAdjustmentAssert(transactionList.get(1), "PDC",
          quantity2.multiply(price1).add(quantity2.multiply(price2).negate()), day4, true));
      costAdjustmentAssertLineList2.add(new CostAdjustmentAssert(transactionList.get(2), "PDC",
          quantity4.multiply(price6).add(quantity4.multiply(price5).negate()), day5, false));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList2);
      assertCostAdjustment(costAdjustmentList, costAdjustmentAssertList);

      // Post cost adjustment 1 and assert it
      postDocument(costAdjustmentList.get(0));
      List<DocumentPostAssert> documentPostAssertList1 = new ArrayList<DocumentPostAssert>();
      documentPostAssertList1.add(new DocumentPostAssert("99904", BigDecimal.ZERO, quantity1
          .multiply(price4).add(quantity1.multiply(price1).negate()), null));
      documentPostAssertList1.add(new DocumentPostAssert("35000", quantity1.multiply(price4).add(
          quantity1.multiply(price1).negate()), BigDecimal.ZERO, null));
      documentPostAssertList1.add(new DocumentPostAssert("99900", quantity4.multiply(price5).add(
          quantity4.multiply(price3).negate()), BigDecimal.ZERO, null));
      documentPostAssertList1.add(new DocumentPostAssert("35000", BigDecimal.ZERO, quantity4
          .multiply(price5).add(quantity4.multiply(price3).negate()), null));
      CostAdjustment costAdjustment1 = OBDal.getInstance().get(CostAdjustment.class,
          costAdjustmentList.get(0).getId());
      assertDocumentPost(costAdjustment1, product.getId(), documentPostAssertList1);

      // Post cost adjustment 2 and assert it
      postDocument(costAdjustmentList.get(1));
      List<DocumentPostAssert> documentPostAssertList2 = new ArrayList<DocumentPostAssert>();
      documentPostAssertList2.add(new DocumentPostAssert("99904", BigDecimal.ZERO, quantity2
          .multiply(price1).add(quantity2.multiply(price2).negate()), null));
      documentPostAssertList2.add(new DocumentPostAssert("35000", quantity2.multiply(price1).add(
          quantity2.multiply(price2).negate()), BigDecimal.ZERO, null));
      documentPostAssertList2.add(new DocumentPostAssert("99900", quantity4.multiply(price6).add(
          quantity4.multiply(price5).negate()), BigDecimal.ZERO, null));
      documentPostAssertList2.add(new DocumentPostAssert("35000", BigDecimal.ZERO, quantity4
          .multiply(price6).add(quantity4.multiply(price5).negate()), null));
      CostAdjustment costAdjustment2 = OBDal.getInstance().get(CostAdjustment.class,
          costAdjustmentList.get(1).getId());
      assertDocumentPost(costAdjustment2, product.getId(), documentPostAssertList2);

      OBDal.getInstance().commitAndClose();

    } catch (Exception e) {
      System.out.println(e.getMessage());
      throw new OBException(e);
    }

    finally {
      OBContext.restorePreviousMode();
    }
  }

  @Test
  public void testCostingV221() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final int day3 = 15;
    final int day4 = 20;
    final BigDecimal price1 = new BigDecimal("15.00");
    final BigDecimal price2 = new BigDecimal("12.00");
    final BigDecimal price3 = new BigDecimal("20.00");
    final BigDecimal price4 = new BigDecimal("18.00");
    final BigDecimal price5 = new BigDecimal("19.80");
    final BigDecimal quantity1 = new BigDecimal("500");
    final BigDecimal quantity2 = new BigDecimal("700");
    final BigDecimal quantity3 = new BigDecimal("150");
    final BigDecimal quantity4 = new BigDecimal("600");
    final BigDecimal quantity5 = new BigDecimal("50");
    final BigDecimal amount1 = new BigDecimal("1350.00");
    final BigDecimal amount2 = new BigDecimal("-450.00");

    try {

      OBContext.setOBContext(USER_ID, ROLE_ID, CLIENT_ID, ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product = createProduct("testCostingV221", price1);

      // Create purchase order and book it
      Order purchaseOrder1 = createPurchaseOrder(product, price1, quantity1, day0);

      // Create purchase order and book it
      Order purchaseOrder2 = createPurchaseOrder(product, price2, quantity2, day1);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt1 = createGoodsReceipt(purchaseOrder1, price1, quantity3, day2);

      // Create goods shipment, run costing background, post it and assert it
      ShipmentInOut goodsShipment = createGoodsShipment(product, price1, quantity4, day3);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt2 = createGoodsReceipt(purchaseOrder2, price2, quantity1, day4);

      // Create purchase invoice, post it and assert it
      List<Order> purchaseOrderList = new ArrayList<Order>();
      purchaseOrderList.add(purchaseOrder1);
      purchaseOrderList.add(purchaseOrder2);
      List<BigDecimal> priceList = new ArrayList<BigDecimal>();
      priceList.add(price3);
      priceList.add(price4);
      List<BigDecimal> quantityList = new ArrayList<BigDecimal>();
      quantityList.add(quantity3);
      quantityList.add(quantity1);
      createPurchaseInvoice(purchaseOrderList, priceList, quantityList, day4);

      // Run price correction background
      runPriceBackground();

      // Assert product transactions
      List<ProductTransactionAssert> productTransactionAssertList = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt1.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price3));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsShipment.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price3));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt2.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price2, price5, price4));
      assertProductTransaction(product.getId(), productTransactionAssertList);

      // Assert product costing
      List<MaterialTransaction> transactionList = getProductTransactions(product.getId());
      List<ProductCostingAssert> productCostingAssertList = new ArrayList<ProductCostingAssert>();
      productCostingAssertList.add(new ProductCostingAssert(transactionList.get(0), price3, price1,
          price3, quantity3));
      productCostingAssertList.add(new ProductCostingAssert(transactionList.get(2), price4, price1
          .negate(), price4, quantity5));
      assertProductCosting(product.getId(), productCostingAssertList);

      // Assert cost adjustment
      List<CostAdjustment> costAdjustmentList = getCostAdjustment(product.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList1 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList1.add(new CostAdjustmentAssert(transactionList.get(2), "NSC",
          amount1, day4, true, false));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList1);
      List<CostAdjustmentAssert> costAdjustmentAssertLineList2 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList2.add(new CostAdjustmentAssert(transactionList.get(0), "PDC",
          quantity3.multiply(price3).add(quantity3.multiply(price1).negate()), day4, true));
      costAdjustmentAssertLineList2.add(new CostAdjustmentAssert(transactionList.get(2), "PDC",
          quantity4.multiply(price3).add(quantity4.multiply(price1).negate()), day4, true));
      costAdjustmentAssertLineList2.add(new CostAdjustmentAssert(transactionList.get(1), "PDC",
          quantity1.multiply(price4).add(quantity1.multiply(price2).negate()), day4, false));
      costAdjustmentAssertLineList2.add(new CostAdjustmentAssert(transactionList.get(2), "NSC",
          amount2, day4, false, false));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList2);
      assertCostAdjustment(costAdjustmentList, costAdjustmentAssertList);

      // Post cost adjustment and assert it
      postDocument(costAdjustmentList.get(0));
      List<DocumentPostAssert> documentPostAssertList1 = new ArrayList<DocumentPostAssert>();
      documentPostAssertList1.add(new DocumentPostAssert("61000", BigDecimal.ZERO, amount1, null));
      documentPostAssertList1.add(new DocumentPostAssert("35000", amount1, BigDecimal.ZERO, null));
      CostAdjustment costAdjustment1 = OBDal.getInstance().get(CostAdjustment.class,
          costAdjustmentList.get(0).getId());
      assertDocumentPost(costAdjustment1, product.getId(), documentPostAssertList1);

      // Post cost adjustment and assert it
      postDocument(costAdjustmentList.get(1));
      List<DocumentPostAssert> documentPostAssertList2 = new ArrayList<DocumentPostAssert>();
      documentPostAssertList2.add(new DocumentPostAssert("99904", BigDecimal.ZERO, quantity3
          .multiply(price3).add(quantity3.multiply(price1).negate()), null));
      documentPostAssertList2.add(new DocumentPostAssert("35000", quantity3.multiply(price3).add(
          quantity3.multiply(price1).negate()), BigDecimal.ZERO, null));
      documentPostAssertList2.add(new DocumentPostAssert("99904", BigDecimal.ZERO, quantity4
          .multiply(price3).add(quantity4.multiply(price1).negate()), null));
      documentPostAssertList2.add(new DocumentPostAssert("35000", quantity4.multiply(price3).add(
          quantity4.multiply(price1).negate()), BigDecimal.ZERO, null));
      documentPostAssertList2.add(new DocumentPostAssert("99900", quantity1.multiply(price4).add(
          quantity1.multiply(price2).negate()), BigDecimal.ZERO, null));
      documentPostAssertList2.add(new DocumentPostAssert("35000", BigDecimal.ZERO, quantity1
          .multiply(price4).add(quantity1.multiply(price2).negate()), null));
      documentPostAssertList2.add(new DocumentPostAssert("61000", amount2.negate(),
          BigDecimal.ZERO, null));
      documentPostAssertList2.add(new DocumentPostAssert("35000", BigDecimal.ZERO,
          amount2.negate(), null));
      CostAdjustment costAdjustment2 = OBDal.getInstance().get(CostAdjustment.class,
          costAdjustmentList.get(1).getId());
      assertDocumentPost(costAdjustment2, product.getId(), documentPostAssertList2);

      OBDal.getInstance().commitAndClose();

    } catch (Exception e) {
      System.out.println(e.getMessage());
      throw new OBException(e);
    }

    finally {
      OBContext.restorePreviousMode();
    }
  }

  @Test
  public void testCostingGM11() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final int day3 = 15;
    final int day4 = 20;
    final BigDecimal price1 = new BigDecimal("11.00");
    final BigDecimal price2 = new BigDecimal("10.00");
    final BigDecimal price3 = new BigDecimal("15.00");
    final BigDecimal price4 = new BigDecimal("19.7619");
    final BigDecimal price5 = new BigDecimal("11.9524");
    final BigDecimal quantity1 = new BigDecimal("820");
    final BigDecimal quantity2 = new BigDecimal("400");
    final BigDecimal quantity3 = new BigDecimal("420");

    try {

      OBContext.setOBContext(USER_ID, ROLE_ID, CLIENT_ID, ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product = createProduct("testCostingGM11", price1);

      // Create purchase order and book it
      Order purchaseOrder = createPurchaseOrder(product, price1, quantity1, day0);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt = createGoodsReceipt(purchaseOrder, price1, quantity1, day1);

      // Create goods movement, run costing background, post it and assert it
      InternalMovement goodsMovement = createGoodsMovement(product, price1, quantity2, LOCATOR1_ID,
          LOCATOR4_ID, day3);

      // Update transaction total cost amount
      manualCostAdjustment(getProductTransactions(product.getId()).get(1),
          quantity2.multiply(price2), false, day4);

      // Create purchase invoice, post it and assert it
      createPurchaseInvoice(goodsReceipt, price3, quantity1, day2);

      // Run price correction background
      runPriceBackground();

      // Assert product transactions
      List<ProductTransactionAssert> productTransactionAssertList = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price3));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(InternalMovement.class, goodsMovement.getId())
          .getMaterialMgmtInternalMovementLineList().get(0), price1, price2, true));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(InternalMovement.class, goodsMovement.getId())
          .getMaterialMgmtInternalMovementLineList().get(0), price1, price2));
      assertProductTransaction(product.getId(), productTransactionAssertList);

      // Assert product costing
      List<MaterialTransaction> transactionList = getProductTransactions(product.getId());
      List<ProductCostingAssert> productCostingAssertList = new ArrayList<ProductCostingAssert>();
      productCostingAssertList.add(new ProductCostingAssert(transactionList.get(0), WAREHOUSE1_ID,
          price3, price1, price3, quantity1));
      productCostingAssertList.add(new ProductCostingAssert(transactionList.get(1), WAREHOUSE1_ID,
          price2, price5, price4, quantity3));
      productCostingAssertList.add(new ProductCostingAssert(transactionList.get(2), WAREHOUSE2_ID,
          price2, price1, price2, quantity2));
      assertProductCosting(product.getId(), productCostingAssertList);

      // Assert cost adjustment
      List<CostAdjustment> costAdjustmentList = getCostAdjustment(product.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList1 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList1.add(new CostAdjustmentAssert(transactionList.get(1), "MCC",
          quantity2.multiply(price2).add(quantity2.multiply(price1).negate()), day4, true, true));
      costAdjustmentAssertLineList1.add(new CostAdjustmentAssert(transactionList.get(2), "MCC",
          quantity2.multiply(price2).add(quantity2.multiply(price1).negate()), day4, false, true));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList1);
      List<CostAdjustmentAssert> costAdjustmentAssertLineList2 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList2.add(new CostAdjustmentAssert(transactionList.get(0), "PDC",
          quantity1.multiply(price3).add(quantity1.multiply(price1).negate()), day2, true));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList2);
      assertCostAdjustment(costAdjustmentList, costAdjustmentAssertList);

      // Post cost adjustment and assert it
      postDocument(costAdjustmentList.get(0));
      List<DocumentPostAssert> documentPostAssertList1 = new ArrayList<DocumentPostAssert>();
      documentPostAssertList1.add(new DocumentPostAssert("35000", quantity2.multiply(price1).add(
          quantity2.multiply(price2).negate()), BigDecimal.ZERO, null));
      documentPostAssertList1.add(new DocumentPostAssert("61000", BigDecimal.ZERO, quantity2
          .multiply(price1).add(quantity2.multiply(price2).negate()), null));
      documentPostAssertList1.add(new DocumentPostAssert("61000", quantity2.multiply(price1).add(
          quantity2.multiply(price2).negate()), BigDecimal.ZERO, null));
      documentPostAssertList1.add(new DocumentPostAssert("35000", BigDecimal.ZERO, quantity2
          .multiply(price1).add(quantity2.multiply(price2).negate()), null));
      CostAdjustment costAdjustment1 = OBDal.getInstance().get(CostAdjustment.class,
          costAdjustmentList.get(0).getId());
      assertDocumentPost(costAdjustment1, product.getId(), documentPostAssertList1);

      // Post cost adjustment and assert it
      postDocument(costAdjustmentList.get(1));
      List<DocumentPostAssert> documentPostAssertList2 = new ArrayList<DocumentPostAssert>();
      documentPostAssertList2.add(new DocumentPostAssert("99904", BigDecimal.ZERO, quantity1
          .multiply(price3).add(quantity1.multiply(price1).negate()), null));
      documentPostAssertList2.add(new DocumentPostAssert("35000", quantity1.multiply(price3).add(
          quantity1.multiply(price1).negate()), BigDecimal.ZERO, null));
      CostAdjustment costAdjustment2 = OBDal.getInstance().get(CostAdjustment.class,
          costAdjustmentList.get(1).getId());
      assertDocumentPost(costAdjustment2, product.getId(), documentPostAssertList2);

      OBDal.getInstance().commitAndClose();

    } catch (Exception e) {
      System.out.println(e.getMessage());
      throw new OBException(e);
    }

    finally {
      OBContext.restorePreviousMode();
    }
  }

  @Test
  public void testCostingGM12() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final int day3 = 15;
    final int day4 = 20;
    final BigDecimal price1 = new BigDecimal("11.00");
    final BigDecimal price2 = new BigDecimal("10.00");
    final BigDecimal price3 = new BigDecimal("15.00");
    final BigDecimal price4 = new BigDecimal("14.00");
    final BigDecimal price5 = new BigDecimal("15.9524");
    final BigDecimal price6 = new BigDecimal("11.9524");
    final BigDecimal quantity1 = new BigDecimal("820");
    final BigDecimal quantity2 = new BigDecimal("400");
    final BigDecimal quantity3 = new BigDecimal("420");
    final BigDecimal amount1 = new BigDecimal("-400");

    try {

      OBContext.setOBContext(USER_ID, ROLE_ID, CLIENT_ID, ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product = createProduct("testCostingGM12", price1);

      // Create purchase order and book it
      Order purchaseOrder = createPurchaseOrder(product, price1, quantity1, day0);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt = createGoodsReceipt(purchaseOrder, price1, quantity1, day1);

      // Create goods movement, run costing background, post it and assert it
      InternalMovement goodsMovement = createGoodsMovement(product, price1, quantity2, LOCATOR1_ID,
          LOCATOR4_ID, day3);

      // Update transaction total cost amount
      manualCostAdjustment(getProductTransactions(product.getId()).get(1), amount1, true, false,
          day4);

      // Create purchase invoice, post it and assert it
      createPurchaseInvoice(goodsReceipt, price3, quantity1, day2);

      // Run price correction background
      runPriceBackground();

      // Assert product transactions
      List<ProductTransactionAssert> productTransactionAssertList = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price3));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(InternalMovement.class, goodsMovement.getId())
          .getMaterialMgmtInternalMovementLineList().get(0), price1, price4, price3));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(InternalMovement.class, goodsMovement.getId())
          .getMaterialMgmtInternalMovementLineList().get(0), price1, price4));
      assertProductTransaction(product.getId(), productTransactionAssertList);

      // Assert product costing
      List<MaterialTransaction> transactionList = getProductTransactions(product.getId());
      List<ProductCostingAssert> productCostingAssertList = new ArrayList<ProductCostingAssert>();
      productCostingAssertList.add(new ProductCostingAssert(transactionList.get(0), WAREHOUSE1_ID,
          price3, price1, price3, quantity1));
      productCostingAssertList.add(new ProductCostingAssert(transactionList.get(1), WAREHOUSE1_ID,
          price3, price6, price5, quantity3));
      productCostingAssertList.add(new ProductCostingAssert(transactionList.get(2), WAREHOUSE2_ID,
          price4, price1, price4, quantity2));
      assertProductCosting(product.getId(), productCostingAssertList);

      // Assert cost adjustment
      List<CostAdjustment> costAdjustmentList = getCostAdjustment(product.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList1 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList1.add(new CostAdjustmentAssert(transactionList.get(1), "MCC",
          quantity2.multiply(price2).add(quantity2.multiply(price1).negate()), day4, true, false));
      costAdjustmentAssertLineList1.add(new CostAdjustmentAssert(transactionList.get(2), "MCC",
          quantity2.multiply(price2).add(quantity2.multiply(price1).negate()), day4, false, true));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList1);
      List<CostAdjustmentAssert> costAdjustmentAssertLineList2 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList2.add(new CostAdjustmentAssert(transactionList.get(0), "PDC",
          quantity1.multiply(price3).add(quantity1.multiply(price1).negate()), day2, true));
      costAdjustmentAssertLineList2.add(new CostAdjustmentAssert(transactionList.get(1), "PDC",
          quantity2.multiply(price3).add(quantity2.multiply(price1).negate()), day3, false));
      costAdjustmentAssertLineList2.add(new CostAdjustmentAssert(transactionList.get(2), "PDC",
          quantity2.multiply(price3).add(quantity2.multiply(price1).negate()), day3, false));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList2);
      assertCostAdjustment(costAdjustmentList, costAdjustmentAssertList);

      // Post cost adjustment and assert it
      postDocument(costAdjustmentList.get(0));
      List<DocumentPostAssert> documentPostAssertList1 = new ArrayList<DocumentPostAssert>();
      documentPostAssertList1.add(new DocumentPostAssert("35000", quantity2.multiply(price1).add(
          quantity2.multiply(price2).negate()), BigDecimal.ZERO, null));
      documentPostAssertList1.add(new DocumentPostAssert("61000", BigDecimal.ZERO, quantity2
          .multiply(price1).add(quantity2.multiply(price2).negate()), null));
      documentPostAssertList1.add(new DocumentPostAssert("61000", quantity2.multiply(price1).add(
          quantity2.multiply(price2).negate()), BigDecimal.ZERO, null));
      documentPostAssertList1.add(new DocumentPostAssert("35000", BigDecimal.ZERO, quantity2
          .multiply(price1).add(quantity2.multiply(price2).negate()), null));
      CostAdjustment costAdjustment1 = OBDal.getInstance().get(CostAdjustment.class,
          costAdjustmentList.get(0).getId());
      assertDocumentPost(costAdjustment1, product.getId(), documentPostAssertList1);

      // Post cost adjustment and assert it
      postDocument(costAdjustmentList.get(1));
      List<DocumentPostAssert> documentPostAssertList2 = new ArrayList<DocumentPostAssert>();
      documentPostAssertList2.add(new DocumentPostAssert("99904", BigDecimal.ZERO, quantity1
          .multiply(price3).add(quantity1.multiply(price1).negate()), null));
      documentPostAssertList2.add(new DocumentPostAssert("35000", quantity1.multiply(price3).add(
          quantity1.multiply(price1).negate()), BigDecimal.ZERO, null));
      documentPostAssertList2.add(new DocumentPostAssert("35000", BigDecimal.ZERO, quantity2
          .multiply(price3).add(quantity2.multiply(price1).negate()), null));
      documentPostAssertList2.add(new DocumentPostAssert("61000", quantity2.multiply(price3).add(
          quantity2.multiply(price1).negate()), BigDecimal.ZERO, null));
      documentPostAssertList2.add(new DocumentPostAssert("61000", BigDecimal.ZERO, quantity2
          .multiply(price3).add(quantity2.multiply(price1).negate()), null));
      documentPostAssertList2.add(new DocumentPostAssert("35000", quantity2.multiply(price3).add(
          quantity2.multiply(price1).negate()), BigDecimal.ZERO, null));
      CostAdjustment costAdjustment2 = OBDal.getInstance().get(CostAdjustment.class,
          costAdjustmentList.get(1).getId());
      assertDocumentPost(costAdjustment2, product.getId(), documentPostAssertList2);

      OBDal.getInstance().commitAndClose();

    } catch (Exception e) {
      System.out.println(e.getMessage());
      throw new OBException(e);
    }

    finally {
      OBContext.restorePreviousMode();
    }
  }

  @Test
  public void testCostingGM13() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final int day3 = 15;
    final int day4 = 20;
    final BigDecimal price1 = new BigDecimal("11.00");
    final BigDecimal price2 = new BigDecimal("10.00");
    final BigDecimal price3 = new BigDecimal("15.00");
    final BigDecimal price4 = new BigDecimal("11.9524");
    final BigDecimal quantity1 = new BigDecimal("820");
    final BigDecimal quantity2 = new BigDecimal("400");
    final BigDecimal quantity3 = new BigDecimal("420");
    final BigDecimal amount1 = new BigDecimal("-400");

    try {

      OBContext.setOBContext(USER_ID, ROLE_ID, CLIENT_ID, ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product = createProduct("testCostingGM13", price1);

      // Create purchase order and book it
      Order purchaseOrder = createPurchaseOrder(product, price1, quantity1, day0);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt = createGoodsReceipt(purchaseOrder, price1, quantity1, day1);

      // Create goods movement, run costing background, post it and assert it
      InternalMovement goodsMovement = createGoodsMovement(product, price1, quantity2, LOCATOR1_ID,
          LOCATOR4_ID, day3);

      // Update transaction total cost amount
      manualCostAdjustment(getProductTransactions(product.getId()).get(1), amount1, true, true,
          day4);

      // Create purchase invoice, post it and assert it
      createPurchaseInvoice(goodsReceipt, price3, quantity1, day2);

      // Run price correction background
      runPriceBackground();

      // Assert product transactions
      List<ProductTransactionAssert> productTransactionAssertList = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price3));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(InternalMovement.class, goodsMovement.getId())
          .getMaterialMgmtInternalMovementLineList().get(0), price1, price3));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(InternalMovement.class, goodsMovement.getId())
          .getMaterialMgmtInternalMovementLineList().get(0), price1, price3));
      assertProductTransaction(product.getId(), productTransactionAssertList);

      // Assert product costing
      List<MaterialTransaction> transactionList = getProductTransactions(product.getId());
      List<ProductCostingAssert> productCostingAssertList = new ArrayList<ProductCostingAssert>();
      productCostingAssertList.add(new ProductCostingAssert(transactionList.get(0), WAREHOUSE1_ID,
          price3, price1, price3, quantity1));
      productCostingAssertList.add(new ProductCostingAssert(transactionList.get(1), WAREHOUSE1_ID,
          price3, price4, price3, quantity3));
      productCostingAssertList.add(new ProductCostingAssert(transactionList.get(2), WAREHOUSE2_ID,
          price3, price1, price3, quantity2));
      assertProductCosting(product.getId(), productCostingAssertList);

      // Assert cost adjustment
      List<CostAdjustment> costAdjustmentList = getCostAdjustment(product.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList1 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList1.add(new CostAdjustmentAssert(transactionList.get(1), "MCC",
          quantity2.multiply(price2).add(quantity2.multiply(price1).negate()), day4, true, true));
      costAdjustmentAssertLineList1.add(new CostAdjustmentAssert(transactionList.get(2), "MCC",
          quantity2.multiply(price2).add(quantity2.multiply(price1).negate()), day4, false, true));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList1);
      List<CostAdjustmentAssert> costAdjustmentAssertLineList2 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList2.add(new CostAdjustmentAssert(transactionList.get(0), "PDC",
          quantity1.multiply(price3).add(quantity1.multiply(price1).negate()), day2, true));
      costAdjustmentAssertLineList2.add(new CostAdjustmentAssert(transactionList.get(1), "PDC",
          quantity2.multiply(price3).add(quantity2.multiply(price2).negate()), day3, false));
      costAdjustmentAssertLineList2.add(new CostAdjustmentAssert(transactionList.get(2), "PDC",
          quantity2.multiply(price3).add(quantity2.multiply(price2).negate()), day3, false));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList2);
      assertCostAdjustment(costAdjustmentList, costAdjustmentAssertList);

      // Post cost adjustment and assert it
      postDocument(costAdjustmentList.get(0));
      List<DocumentPostAssert> documentPostAssertList1 = new ArrayList<DocumentPostAssert>();
      documentPostAssertList1.add(new DocumentPostAssert("35000", quantity2.multiply(price1).add(
          quantity2.multiply(price2).negate()), BigDecimal.ZERO, null));
      documentPostAssertList1.add(new DocumentPostAssert("61000", BigDecimal.ZERO, quantity2
          .multiply(price1).add(quantity2.multiply(price2).negate()), null));
      documentPostAssertList1.add(new DocumentPostAssert("61000", quantity2.multiply(price1).add(
          quantity2.multiply(price2).negate()), BigDecimal.ZERO, null));
      documentPostAssertList1.add(new DocumentPostAssert("35000", BigDecimal.ZERO, quantity2
          .multiply(price1).add(quantity2.multiply(price2).negate()), null));
      CostAdjustment costAdjustment1 = OBDal.getInstance().get(CostAdjustment.class,
          costAdjustmentList.get(0).getId());
      assertDocumentPost(costAdjustment1, product.getId(), documentPostAssertList1);

      // Post cost adjustment and assert it
      postDocument(costAdjustmentList.get(1));
      List<DocumentPostAssert> documentPostAssertList2 = new ArrayList<DocumentPostAssert>();
      documentPostAssertList2.add(new DocumentPostAssert("99904", BigDecimal.ZERO, quantity1
          .multiply(price3).add(quantity1.multiply(price1).negate()), null));
      documentPostAssertList2.add(new DocumentPostAssert("35000", quantity1.multiply(price3).add(
          quantity1.multiply(price1).negate()), BigDecimal.ZERO, null));
      documentPostAssertList2.add(new DocumentPostAssert("35000", BigDecimal.ZERO, quantity2
          .multiply(price3).add(quantity2.multiply(price2).negate()), null));
      documentPostAssertList2.add(new DocumentPostAssert("61000", quantity2.multiply(price3).add(
          quantity2.multiply(price2).negate()), BigDecimal.ZERO, null));
      documentPostAssertList2.add(new DocumentPostAssert("61000", BigDecimal.ZERO, quantity2
          .multiply(price3).add(quantity2.multiply(price2).negate()), null));
      documentPostAssertList2.add(new DocumentPostAssert("35000", quantity2.multiply(price3).add(
          quantity2.multiply(price2).negate()), BigDecimal.ZERO, null));
      CostAdjustment costAdjustment2 = OBDal.getInstance().get(CostAdjustment.class,
          costAdjustmentList.get(1).getId());
      assertDocumentPost(costAdjustment2, product.getId(), documentPostAssertList2);

      OBDal.getInstance().commitAndClose();

    } catch (Exception e) {
      System.out.println(e.getMessage());
      throw new OBException(e);
    }

    finally {
      OBContext.restorePreviousMode();
    }
  }

  @Test
  public void testCostingGM5() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final int day3 = 15;
    final int day4 = 20;
    final int day5 = 25;
    final int day6 = 30;
    final int day7 = 35;
    final int day8 = 40;
    final int day9 = 45;
    final BigDecimal price1 = new BigDecimal("120.00");
    final BigDecimal price2 = new BigDecimal("150.00");
    final BigDecimal price3 = new BigDecimal("100.00");
    final BigDecimal price4 = new BigDecimal("95.00");
    final BigDecimal price5 = new BigDecimal("5.00");
    final BigDecimal price6 = new BigDecimal("95.6897");
    final BigDecimal price7 = new BigDecimal("88.4921");
    final BigDecimal price8 = new BigDecimal("96.0317");
    final BigDecimal price9 = new BigDecimal("337.50");
    final BigDecimal quantity1 = new BigDecimal("100");
    final BigDecimal quantity2 = new BigDecimal("20");
    final BigDecimal quantity3 = new BigDecimal("30");
    final BigDecimal quantity4 = new BigDecimal("500");
    final BigDecimal quantity5 = new BigDecimal("50");
    final BigDecimal quantity6 = new BigDecimal("580");
    final BigDecimal quantity7 = new BigDecimal("630");

    try {

      OBContext.setOBContext(USER_ID, ROLE_ID, CLIENT_ID, ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product = createProduct("testCostingGM5", price1);

      // Create purchase order and book it
      Order purchaseOrder1 = createPurchaseOrder(product, price1, quantity1, day0);

      // Create purchase order and book it
      Order purchaseOrder2 = createPurchaseOrder(product, price2, quantity1, day0);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt1 = createGoodsReceipt(purchaseOrder1, price1, quantity1,
          LOCATOR1_ID, day1);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt2 = createGoodsReceipt(purchaseOrder2, price2, quantity1,
          LOCATOR4_ID, day2);

      // Create goods shipment, run costing background, post it and assert it
      ShipmentInOut goodsShipment1 = createGoodsShipment(product, price1, quantity2, LOCATOR1_ID,
          day3);

      // Create goods shipment, run costing background, post it and assert it
      ShipmentInOut goodsShipment2 = createGoodsShipment(product, price2, quantity3, LOCATOR4_ID,
          day4);

      // Create purchase invoice, post it and assert it
      List<ShipmentInOut> goodsReceiptList = new ArrayList<ShipmentInOut>();
      goodsReceiptList.add(goodsReceipt1);
      goodsReceiptList.add(goodsReceipt2);
      List<BigDecimal> priceList = new ArrayList<BigDecimal>();
      priceList.add(price3);
      priceList.add(price3);
      createPurchaseInvoice(goodsReceiptList, priceList, quantity1.add(quantity1), day5);

      // Run price correction background
      runPriceBackground();

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt3 = createGoodsReceipt(product, price3, quantity4, LOCATOR1_ID,
          day6);

      // Create purchase invoice, post it and assert it
      createPurchaseInvoice(goodsReceipt3, price4, quantity4, day7);

      // Run price correction background
      runPriceBackground();

      // Create goods movement, run costing background, post it and assert it
      InternalMovement goodsMovement = createGoodsMovement(product, price3, quantity5, LOCATOR4_ID,
          LOCATOR1_ID, day8);

      // Update transaction total cost amount
      manualCostAdjustment(getProductTransactions(product.getId()).get(5),
          quantity5.multiply(price5), false, day9);

      // Assert product transactions
      List<ProductTransactionAssert> productTransactionAssertList = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt1.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price3));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt2.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price2, price3));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsShipment1.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price3));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsShipment2.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price2, price3));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt3.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price3, price4));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(InternalMovement.class, goodsMovement.getId())
          .getMaterialMgmtInternalMovementLineList().get(0), price3, price5, true));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(InternalMovement.class, goodsMovement.getId())
          .getMaterialMgmtInternalMovementLineList().get(0), price3, price5));
      assertProductTransaction(product.getId(), productTransactionAssertList);

      // Assert product costing
      List<MaterialTransaction> transactionList = getProductTransactions(product.getId());
      List<ProductCostingAssert> productCostingAssertList = new ArrayList<ProductCostingAssert>();
      productCostingAssertList.add(new ProductCostingAssert(transactionList.get(0), WAREHOUSE1_ID,
          price3, price1, price3, quantity1));
      productCostingAssertList.add(new ProductCostingAssert(transactionList.get(4), WAREHOUSE1_ID,
          price4, price3, price6, quantity6));
      productCostingAssertList.add(new ProductCostingAssert(transactionList.get(6), WAREHOUSE1_ID,
          price5, price8, price7, quantity7));
      productCostingAssertList.add(new ProductCostingAssert(transactionList.get(1), WAREHOUSE2_ID,
          price3, price2, price3, quantity1));
      productCostingAssertList.add(new ProductCostingAssert(transactionList.get(5), WAREHOUSE2_ID,
          price9, null, price9, quantity2));
      assertProductCosting(product.getId(), productCostingAssertList);

      // Assert cost adjustment
      List<CostAdjustment> costAdjustmentList = getCostAdjustment(product.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList1 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList1.add(new CostAdjustmentAssert(transactionList.get(0), "PDC",
          quantity1.multiply(price3).add(quantity1.multiply(price1).negate()), day5, true));
      costAdjustmentAssertLineList1.add(new CostAdjustmentAssert(transactionList.get(1), "PDC",
          quantity1.multiply(price3).add(quantity1.multiply(price2).negate()), day5, true));
      costAdjustmentAssertLineList1.add(new CostAdjustmentAssert(transactionList.get(2), "PDC",
          quantity2.multiply(price3).add(quantity2.multiply(price1).negate()), day5, false));
      costAdjustmentAssertLineList1.add(new CostAdjustmentAssert(transactionList.get(3), "PDC",
          quantity3.multiply(price3).add(quantity3.multiply(price2).negate()), day5, false));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList1);
      List<CostAdjustmentAssert> costAdjustmentAssertLineList2 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList2.add(new CostAdjustmentAssert(transactionList.get(4), "PDC",
          quantity4.multiply(price4).add(quantity4.multiply(price3).negate()), day7, true));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList2);
      List<CostAdjustmentAssert> costAdjustmentAssertLineList3 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList3.add(new CostAdjustmentAssert(transactionList.get(5), "MCC",
          quantity5.multiply(price5).add(quantity5.multiply(price3).negate()), day9, true));
      costAdjustmentAssertLineList3.add(new CostAdjustmentAssert(transactionList.get(6), "MCC",
          quantity5.multiply(price5).add(quantity5.multiply(price3).negate()), day9, false));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList3);
      assertCostAdjustment(costAdjustmentList, costAdjustmentAssertList);

      // Post cost adjustment and assert it
      postDocument(costAdjustmentList.get(0));
      List<DocumentPostAssert> documentPostAssertList1 = new ArrayList<DocumentPostAssert>();
      documentPostAssertList1.add(new DocumentPostAssert("99904", quantity1.multiply(price1).add(
          quantity1.multiply(price3).negate()), BigDecimal.ZERO, null));
      documentPostAssertList1.add(new DocumentPostAssert("35000", BigDecimal.ZERO, quantity1
          .multiply(price1).add(quantity1.multiply(price3).negate()), null));
      documentPostAssertList1.add(new DocumentPostAssert("99904", quantity1.multiply(price2).add(
          quantity1.multiply(price3).negate()), BigDecimal.ZERO, null));
      documentPostAssertList1.add(new DocumentPostAssert("35000", BigDecimal.ZERO, quantity1
          .multiply(price2).add(quantity1.multiply(price3).negate()), null));
      documentPostAssertList1.add(new DocumentPostAssert("99900", BigDecimal.ZERO, quantity2
          .multiply(price1).add(quantity2.multiply(price3).negate()), null));
      documentPostAssertList1.add(new DocumentPostAssert("35000", quantity2.multiply(price1).add(
          quantity2.multiply(price3).negate()), BigDecimal.ZERO, null));
      documentPostAssertList1.add(new DocumentPostAssert("99900", BigDecimal.ZERO, quantity3
          .multiply(price2).add(quantity3.multiply(price3).negate()), null));
      documentPostAssertList1.add(new DocumentPostAssert("35000", quantity3.multiply(price2).add(
          quantity3.multiply(price3).negate()), BigDecimal.ZERO, null));
      CostAdjustment costAdjustment1 = OBDal.getInstance().get(CostAdjustment.class,
          costAdjustmentList.get(0).getId());
      assertDocumentPost(costAdjustment1, product.getId(), documentPostAssertList1);

      // Post cost adjustment and assert it
      postDocument(costAdjustmentList.get(1));
      List<DocumentPostAssert> documentPostAssertList2 = new ArrayList<DocumentPostAssert>();
      documentPostAssertList2.add(new DocumentPostAssert("99904", quantity4.multiply(price3).add(
          quantity4.multiply(price4).negate()), BigDecimal.ZERO, null));
      documentPostAssertList2.add(new DocumentPostAssert("35000", BigDecimal.ZERO, quantity4
          .multiply(price3).add(quantity4.multiply(price4).negate()), null));
      CostAdjustment costAdjustment2 = OBDal.getInstance().get(CostAdjustment.class,
          costAdjustmentList.get(1).getId());
      assertDocumentPost(costAdjustment2, product.getId(), documentPostAssertList2);

      // Post cost adjustment and assert it
      postDocument(costAdjustmentList.get(2));
      List<DocumentPostAssert> documentPostAssertList3 = new ArrayList<DocumentPostAssert>();
      documentPostAssertList3.add(new DocumentPostAssert("35000", quantity5.multiply(price3).add(
          quantity5.multiply(price5).negate()), BigDecimal.ZERO, null));
      documentPostAssertList3.add(new DocumentPostAssert("61000", BigDecimal.ZERO, quantity5
          .multiply(price3).add(quantity5.multiply(price5).negate()), null));
      documentPostAssertList3.add(new DocumentPostAssert("61000", quantity5.multiply(price3).add(
          quantity5.multiply(price5).negate()), BigDecimal.ZERO, null));
      documentPostAssertList3.add(new DocumentPostAssert("35000", BigDecimal.ZERO, quantity5
          .multiply(price3).add(quantity5.multiply(price5).negate()), null));
      CostAdjustment costAdjustment3 = OBDal.getInstance().get(CostAdjustment.class,
          costAdjustmentList.get(2).getId());
      assertDocumentPost(costAdjustment3, product.getId(), documentPostAssertList3);

      OBDal.getInstance().commitAndClose();

    } catch (Exception e) {
      System.out.println(e.getMessage());
      throw new OBException(e);
    }

    finally {
      OBContext.restorePreviousMode();
    }
  }

  @Test
  public void testCostingIC4() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final int day3 = 15;
    final int day4 = 20;
    final BigDecimal price1 = new BigDecimal("35.00");
    final BigDecimal price2 = new BigDecimal("9.00");
    final BigDecimal price3 = new BigDecimal("5.00");
    final BigDecimal price4 = new BigDecimal("40.00");
    final BigDecimal quantity1 = new BigDecimal("1000");
    final BigDecimal quantity2 = new BigDecimal("250");

    try {

      OBContext.setOBContext(USER_ID, ROLE_ID, CLIENT_ID, ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product = createProduct("testCostingIC4", price1);

      // Create purchase order and book it
      Order purchaseOrder = createPurchaseOrder(product, price1, quantity1, day0);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt = createGoodsReceipt(purchaseOrder, price1, quantity1, day1);

      // Create internal consumption, run costing background, post it and assert it
      InternalConsumption internalConsumtpion = createInternalConsumption(product, price1,
          quantity2, day4);

      // Update transaction total cost amount
      manualCostAdjustment(getProductTransactions(product.getId()).get(0),
          quantity1.multiply(price2), false, day2);

      // Assert product transactions
      List<ProductTransactionAssert> productTransactionAssertList1 = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList1.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price2, true));
      productTransactionAssertList1.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(InternalConsumption.class, internalConsumtpion.getId())
          .getMaterialMgmtInternalConsumptionLineList().get(0), price1, price2));
      assertProductTransaction(product.getId(), productTransactionAssertList1);

      // Assert product costing
      List<ProductCostingAssert> productCostingAssertList1 = new ArrayList<ProductCostingAssert>();
      List<MaterialTransaction> transactionList1 = getProductTransactions(product.getId());
      productCostingAssertList1.add(new ProductCostingAssert(transactionList1.get(0), price2,
          price1, price2, quantity1));
      assertProductCosting(product.getId(), productCostingAssertList1);

      // Assert cost adjustment
      List<CostAdjustment> costAdjustmentList1 = getCostAdjustment(product.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList1 = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList1 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList1.add(new CostAdjustmentAssert(transactionList1.get(0), "MCC",
          quantity1.multiply(price2).add(quantity1.multiply(price1).negate()), day2, true));
      costAdjustmentAssertLineList1.add(new CostAdjustmentAssert(transactionList1.get(1), "MCC",
          quantity2.multiply(price2).add(quantity2.multiply(price1).negate()), day4, false));
      costAdjustmentAssertList1.add(costAdjustmentAssertLineList1);
      assertCostAdjustment(costAdjustmentList1, costAdjustmentAssertList1);

      // Post cost adjustment and assert it
      postDocument(costAdjustmentList1.get(0));
      List<DocumentPostAssert> documentPostAssertList1 = new ArrayList<DocumentPostAssert>();
      documentPostAssertList1.add(new DocumentPostAssert("61000", quantity1.multiply(price1).add(
          quantity1.multiply(price2).negate()), BigDecimal.ZERO, null));
      documentPostAssertList1.add(new DocumentPostAssert("35000", BigDecimal.ZERO, quantity1
          .multiply(price1).add(quantity1.multiply(price2).negate()), null));
      documentPostAssertList1.add(new DocumentPostAssert("35000", quantity2.multiply(price1).add(
          quantity2.multiply(price2).negate()), BigDecimal.ZERO, null));
      documentPostAssertList1.add(new DocumentPostAssert("61000", BigDecimal.ZERO, quantity2
          .multiply(price1).add(quantity2.multiply(price2).negate()), null));
      CostAdjustment costAdjustment = OBDal.getInstance().get(CostAdjustment.class,
          costAdjustmentList1.get(0).getId());
      assertDocumentPost(costAdjustment, product.getId(), documentPostAssertList1);

      // Cancel Cost Adjustment
      cancelCostAdjustment(costAdjustment);

      // Update transaction total cost amount
      manualCostAdjustment(getProductTransactions(product.getId()).get(0),
          quantity1.multiply(price3), true, false, day3);

      // Assert product transactions
      List<ProductTransactionAssert> productTransactionAssertList2 = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList2.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price4, price1));
      productTransactionAssertList2.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(InternalConsumption.class, internalConsumtpion.getId())
          .getMaterialMgmtInternalConsumptionLineList().get(0), price1, price4));
      assertProductTransaction(product.getId(), productTransactionAssertList2);

      // Assert product costing
      List<MaterialTransaction> transactionList2 = getProductTransactions(product.getId());
      List<ProductCostingAssert> productCostingAssertList2 = new ArrayList<ProductCostingAssert>();
      productCostingAssertList2.add(new ProductCostingAssert(transactionList2.get(0), price1,
          price1, price4, quantity1));
      assertProductCosting(product.getId(), productCostingAssertList2);

      // Assert cost adjustment
      List<CostAdjustment> costAdjustmentList2 = getCostAdjustment(product.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList2 = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList21 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList21.add(new CostAdjustmentAssert(transactionList2.get(0), "MCC",
          quantity1.multiply(price2).add(quantity1.multiply(price1).negate()), day2, true, "VO"));
      costAdjustmentAssertLineList21.add(new CostAdjustmentAssert(transactionList2.get(1), "MCC",
          quantity2.multiply(price2).add(quantity2.multiply(price1).negate()), day4, false, "VO"));
      costAdjustmentAssertList2.add(costAdjustmentAssertLineList21);
      List<CostAdjustmentAssert> costAdjustmentAssertLineList22 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList22.add(new CostAdjustmentAssert(transactionList2.get(0), "MCC",
          quantity1.multiply(price1).add(quantity1.multiply(price2).negate()), day2, true, "VO"));
      costAdjustmentAssertLineList22.add(new CostAdjustmentAssert(transactionList2.get(1), "MCC",
          quantity2.multiply(price1).add(quantity2.multiply(price2).negate()), day4, false, "VO"));
      costAdjustmentAssertList2.add(costAdjustmentAssertLineList22);
      List<CostAdjustmentAssert> costAdjustmentAssertLineList3 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList3.add(new CostAdjustmentAssert(transactionList2.get(0), "MCC",
          quantity1.multiply(price4).add(quantity1.multiply(price1).negate()), day3, true, false));
      costAdjustmentAssertLineList3.add(new CostAdjustmentAssert(transactionList2.get(1), "MCC",
          quantity2.multiply(price4).add(quantity2.multiply(price1).negate()), day4, false));
      costAdjustmentAssertList2.add(costAdjustmentAssertLineList3);
      assertCostAdjustment(costAdjustmentList2, costAdjustmentAssertList2);

      // Post cost adjustment 1 and assert it
      postDocument(costAdjustmentList2.get(0));
      List<DocumentPostAssert> documentPostAssertList21 = new ArrayList<DocumentPostAssert>();
      documentPostAssertList21.add(new DocumentPostAssert("61000", quantity1.multiply(price1).add(
          quantity1.multiply(price2).negate()), BigDecimal.ZERO, null));
      documentPostAssertList21.add(new DocumentPostAssert("35000", BigDecimal.ZERO, quantity1
          .multiply(price1).add(quantity1.multiply(price2).negate()), null));
      documentPostAssertList21.add(new DocumentPostAssert("35000", quantity2.multiply(price1).add(
          quantity2.multiply(price2).negate()), BigDecimal.ZERO, null));
      documentPostAssertList21.add(new DocumentPostAssert("61000", BigDecimal.ZERO, quantity2
          .multiply(price1).add(quantity2.multiply(price2).negate()), null));
      CostAdjustment costAdjustment21 = OBDal.getInstance().get(CostAdjustment.class,
          costAdjustmentList2.get(0).getId());
      assertDocumentPost(costAdjustment21, product.getId(), documentPostAssertList21);

      // Post cost adjustment 2 and assert it
      postDocument(costAdjustmentList2.get(1));
      List<DocumentPostAssert> documentPostAssertList22 = new ArrayList<DocumentPostAssert>();
      documentPostAssertList22.add(new DocumentPostAssert("61000", BigDecimal.ZERO, quantity1
          .multiply(price1).add(quantity1.multiply(price2).negate()), null));
      documentPostAssertList22.add(new DocumentPostAssert("35000", quantity1.multiply(price1).add(
          quantity1.multiply(price2).negate()), BigDecimal.ZERO, null));
      documentPostAssertList22.add(new DocumentPostAssert("35000", BigDecimal.ZERO, quantity2
          .multiply(price1).add(quantity2.multiply(price2).negate()), null));
      documentPostAssertList22.add(new DocumentPostAssert("61000", quantity2.multiply(price1).add(
          quantity2.multiply(price2).negate()), BigDecimal.ZERO, null));
      CostAdjustment costAdjustment22 = OBDal.getInstance().get(CostAdjustment.class,
          costAdjustmentList2.get(1).getId());
      assertDocumentPost(costAdjustment22, product.getId(), documentPostAssertList22);

      // Post cost adjustment 3 and assert it
      postDocument(costAdjustmentList2.get(2));
      List<DocumentPostAssert> documentPostAssertList3 = new ArrayList<DocumentPostAssert>();
      documentPostAssertList3.add(new DocumentPostAssert("61000", BigDecimal.ZERO, quantity1
          .multiply(price4).add(quantity1.multiply(price1).negate()), null));
      documentPostAssertList3.add(new DocumentPostAssert("35000", quantity1.multiply(price4).add(
          quantity1.multiply(price1).negate()), BigDecimal.ZERO, null));
      documentPostAssertList3.add(new DocumentPostAssert("35000", BigDecimal.ZERO, quantity2
          .multiply(price4).add(quantity2.multiply(price1).negate()), null));
      documentPostAssertList3.add(new DocumentPostAssert("61000", quantity2.multiply(price4).add(
          quantity2.multiply(price1).negate()), BigDecimal.ZERO, null));
      CostAdjustment costAdjustment3 = OBDal.getInstance().get(CostAdjustment.class,
          costAdjustmentList2.get(2).getId());
      assertDocumentPost(costAdjustment3, product.getId(), documentPostAssertList3);

      OBDal.getInstance().commitAndClose();

    } catch (Exception e) {
      System.out.println(e.getMessage());
      throw new OBException(e);
    }

    finally {
      OBContext.restorePreviousMode();
    }
  }

  @Test
  public void testCostingR10() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final int day3 = 15;
    final int day4 = 20;
    final int day5 = 25;
    final BigDecimal price1 = new BigDecimal("15.00");
    final BigDecimal price2 = new BigDecimal("40.00");
    final BigDecimal price3 = new BigDecimal("9.00");
    final BigDecimal quantity1 = new BigDecimal("180");
    final BigDecimal quantity2 = new BigDecimal("80");
    final BigDecimal quantity3 = new BigDecimal("40");

    try {

      OBContext.setOBContext(USER_ID, ROLE_ID, CLIENT_ID, ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product = createProduct("testCostingR10", price1, price2);

      // Create purchase order and book it
      Order purchaseOrder = createPurchaseOrder(product, price1, quantity1, day0);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt = createGoodsReceipt(purchaseOrder, price1, quantity1, day1);

      // Create purchase order and book it
      Order saleseOrder = createSalesOrder(product, price2, quantity2, day2);

      // Create goods shipment, run costing background, post it and assert it
      ShipmentInOut goodsShipment = createGoodsShipment(saleseOrder, price1, quantity2, day3);

      // Update purchase order line product price
      updatePurchaseOrder(purchaseOrder, price3);

      // Run price correction background
      runPriceBackground();

      // Create purchase invoice, post it and assert it
      createPurchaseInvoice(goodsReceipt, price3, quantity1, day5);

      // Run price correction background
      runPriceBackground();

      // Create return from customer, run costing background, post it and assert it
      Order returnFromCustomer = createReturnFromCustomer(goodsShipment, price2, quantity3, day3);

      // Create return material receipt, run costing background, post it and assert it
      ShipmentInOut returnMaterialReceipt = createReturnMaterialReceipt(returnFromCustomer, price3,
          quantity3, day4);

      // Assert product transactions
      List<ProductTransactionAssert> productTransactionAssertList = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price3));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsShipment.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price3));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, returnMaterialReceipt.getId())
          .getMaterialMgmtShipmentInOutLineList().get(0), price3, price3));
      assertProductTransaction(product.getId(), productTransactionAssertList);

      // Assert product costing
      List<MaterialTransaction> transactionList = getProductTransactions(product.getId());
      List<ProductCostingAssert> productCostingAssertList = new ArrayList<ProductCostingAssert>();
      productCostingAssertList.add(new ProductCostingAssert(transactionList.get(0), price3, price1,
          price3, quantity1));
      productCostingAssertList.add(new ProductCostingAssert(transactionList.get(2), price3, null,
          price3, quantity1.add(quantity2.negate()).add(quantity3)));
      assertProductCosting(product.getId(), productCostingAssertList);

      // Assert cost adjustment
      List<CostAdjustment> costAdjustmentList = getCostAdjustment(product.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList1 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList1.add(new CostAdjustmentAssert(transactionList.get(0), "PDC",
          quantity1.multiply(price3).add(quantity1.multiply(price1).negate()), day1, true));
      costAdjustmentAssertLineList1.add(new CostAdjustmentAssert(transactionList.get(1), "PDC",
          quantity2.multiply(price3).add(quantity2.multiply(price1).negate()), day3, false));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList1);
      ;
      assertCostAdjustment(costAdjustmentList, costAdjustmentAssertList);

      // Post cost adjustment and assert it
      postDocument(costAdjustmentList.get(0));
      List<DocumentPostAssert> documentPostAssertList1 = new ArrayList<DocumentPostAssert>();
      documentPostAssertList1.add(new DocumentPostAssert("99904", quantity1.multiply(price1).add(
          quantity1.multiply(price3).negate()), BigDecimal.ZERO, null));
      documentPostAssertList1.add(new DocumentPostAssert("35000", BigDecimal.ZERO, quantity1
          .multiply(price1).add(quantity1.multiply(price3).negate()), null));
      documentPostAssertList1.add(new DocumentPostAssert("99900", BigDecimal.ZERO, quantity2
          .multiply(price1).add(quantity2.multiply(price3).negate()), null));
      documentPostAssertList1.add(new DocumentPostAssert("35000", quantity2.multiply(price1).add(
          quantity2.multiply(price3).negate()), BigDecimal.ZERO, null));
      CostAdjustment costAdjustment1 = OBDal.getInstance().get(CostAdjustment.class,
          costAdjustmentList.get(0).getId());
      assertDocumentPost(costAdjustment1, product.getId(), documentPostAssertList1);

      OBDal.getInstance().commitAndClose();

    } catch (Exception e) {
      System.out.println(e.getMessage());
      throw new OBException(e);
    }

    finally {
      OBContext.restorePreviousMode();
    }
  }

  @Test
  public void testCostingIC3() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final BigDecimal price1 = new BigDecimal("35.00");
    final BigDecimal quantity1 = new BigDecimal("1000");
    final BigDecimal quantity2 = new BigDecimal("250");

    try {

      OBContext.setOBContext(USER_ID, ROLE_ID, CLIENT_ID, ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product = createProduct("testCostingIC3", price1);

      // Create purchase order and book it
      Order purchaseOrder = createPurchaseOrder(product, price1, quantity1, day0);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt = createGoodsReceipt(purchaseOrder, price1, quantity1, day1);

      // Create internal consumption, run costing background, post it and assert it
      InternalConsumption internalConsumtpion = createInternalConsumption(product, price1,
          quantity2, day2);

      // Cancel Cost Adjustment
      cancelInternalConsumption(internalConsumtpion);

      // Assert product transactions
      List<ProductTransactionAssert> productTransactionAssertList = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price1, false));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(InternalConsumption.class, internalConsumtpion.getId())
          .getMaterialMgmtInternalConsumptionLineList().get(0), price1, price1, true));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(InternalConsumption.class, internalConsumtpion.getId())
          .getMaterialMgmtInternalConsumptionLineList().get(0)
          .getMaterialMgmtInternalConsumptionLineVoidedInternalConsumptionLineList().get(0),
          price1, price1, true));
      assertProductTransaction(product.getId(), productTransactionAssertList);

      // Assert product costing
      List<MaterialTransaction> transactionList = getProductTransactions(product.getId());
      List<ProductCostingAssert> productCostingAssertList = new ArrayList<ProductCostingAssert>();
      productCostingAssertList.add(new ProductCostingAssert(transactionList.get(0), price1, null,
          price1, quantity1));
      productCostingAssertList.add(new ProductCostingAssert(transactionList.get(2), price1, null,
          price1, quantity1));
      assertProductCosting(product.getId(), productCostingAssertList);

      // Assert cost adjustment
      assertEquals(getCostAdjustment(product.getId()), null);

      OBDal.getInstance().commitAndClose();

    } catch (Exception e) {
      System.out.println(e.getMessage());
      throw new OBException(e);
    }

    finally {
      OBContext.restorePreviousMode();
    }
  }

  @Test
  public void testCostingR2() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final BigDecimal price1 = new BigDecimal("15.00");
    final BigDecimal price2 = new BigDecimal("30.00");
    final BigDecimal quantity1 = new BigDecimal("180");
    final BigDecimal quantity2 = new BigDecimal("80");

    try {

      OBContext.setOBContext(USER_ID, ROLE_ID, CLIENT_ID, ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product = createProduct("testCostingR2", price1);

      // Create purchase order and book it
      Order purchaseOrder = createPurchaseOrder(product, price1, quantity1, day0);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt = createGoodsReceipt(purchaseOrder, price1, quantity1, day1);

      // Create goods shipment, run costing background, post it and assert it
      ShipmentInOut goodsShipment1 = createGoodsShipment(product, price1, quantity2, day2);

      // Update purchase order line product price
      updatePurchaseOrder(purchaseOrder, price2);

      // Run price correction background
      runPriceBackground();

      // Cancel goods shipment
      ShipmentInOut goodsShipment2 = cancelGoodsShipment(goodsShipment1, price2);

      // Assert product transactions
      List<ProductTransactionAssert> productTransactionAssertList = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price2));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsShipment1.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price2, true));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsShipment2.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price2, price2, true));
      assertProductTransaction(product.getId(), productTransactionAssertList);

      // Assert product costing
      List<MaterialTransaction> transactionList = getProductTransactions(product.getId());
      List<ProductCostingAssert> productCostingAssertList = new ArrayList<ProductCostingAssert>();
      productCostingAssertList.add(new ProductCostingAssert(transactionList.get(0), price2, price1,
          price2, quantity1));
      productCostingAssertList.add(new ProductCostingAssert(transactionList.get(2), price2, null,
          price2, quantity1));
      assertProductCosting(product.getId(), productCostingAssertList);

      // Assert cost adjustment
      List<CostAdjustment> costAdjustmentList = getCostAdjustment(product.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList.add(new CostAdjustmentAssert(transactionList.get(0), "PDC",
          quantity1.multiply(price2).add(quantity1.multiply(price1).negate()), day1, true));
      costAdjustmentAssertLineList.add(new CostAdjustmentAssert(transactionList.get(1), "PDC",
          quantity2.multiply(price2).add(quantity2.multiply(price1).negate()), day2, false));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList);
      assertCostAdjustment(costAdjustmentList, costAdjustmentAssertList);

      // Post cost adjustment 1 and assert it
      postDocument(costAdjustmentList.get(0));
      List<DocumentPostAssert> documentPostAssertList = new ArrayList<DocumentPostAssert>();
      documentPostAssertList.add(new DocumentPostAssert("99904", BigDecimal.ZERO, quantity1
          .multiply(price2).add(quantity1.multiply(price1).negate()), null));
      documentPostAssertList.add(new DocumentPostAssert("35000", quantity1.multiply(price2).add(
          quantity1.multiply(price1).negate()), BigDecimal.ZERO, null));
      documentPostAssertList.add(new DocumentPostAssert("99900", quantity2.multiply(price2).add(
          quantity2.multiply(price1).negate()), BigDecimal.ZERO, null));
      documentPostAssertList.add(new DocumentPostAssert("35000", BigDecimal.ZERO, quantity2
          .multiply(price2).add(quantity2.multiply(price1).negate()), null));
      CostAdjustment costAdjustment = OBDal.getInstance().get(CostAdjustment.class,
          costAdjustmentList.get(0).getId());
      assertDocumentPost(costAdjustment, product.getId(), documentPostAssertList);

      OBDal.getInstance().commitAndClose();

    } catch (Exception e) {
      System.out.println(e.getMessage());
      throw new OBException(e);
    }

    finally {
      OBContext.restorePreviousMode();
    }
  }

  @Test
  public void testCostingR22() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final int day3 = 15;
    final BigDecimal price1 = new BigDecimal("25.00");
    final BigDecimal price2 = new BigDecimal("20.00");
    final BigDecimal quantity1 = new BigDecimal("330");
    final BigDecimal quantity2 = new BigDecimal("170");

    try {

      OBContext.setOBContext(USER_ID, ROLE_ID, CLIENT_ID, ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product = createProduct("testCostingR22", price1);

      // Create purchase order and book it
      Order purchaseOrder = createPurchaseOrder(product, price1, quantity1, day0);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt = createGoodsReceipt(purchaseOrder, price1, quantity1, day1);

      // Create goods shipment, run costing background, post it and assert it
      ShipmentInOut goodsShipment1 = createGoodsShipment(product, price1, quantity2, day3);

      // Add sleep to avoid assert errors
      Thread.sleep(1000);

      // Cancel goods shipment
      ShipmentInOut goodsShipment2 = cancelGoodsShipment(goodsShipment1, price1);

      // Create purchase invoice, post it and assert it
      createPurchaseInvoice(goodsReceipt, price2, quantity1, day2);

      // Run price correction background
      runPriceBackground();

      // Assert product transactions
      List<ProductTransactionAssert> productTransactionAssertList = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price2));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsShipment1.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price1, true));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsShipment2.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price1, true));
      assertProductTransaction(product.getId(), productTransactionAssertList);

      // Assert product costing
      List<MaterialTransaction> transactionList = getProductTransactions(product.getId());
      List<ProductCostingAssert> productCostingAssertList = new ArrayList<ProductCostingAssert>();
      productCostingAssertList.add(new ProductCostingAssert(transactionList.get(0), price2, price1,
          price2, quantity1));
      productCostingAssertList.add(new ProductCostingAssert(transactionList.get(2), price1, price1,
          price2, quantity1));
      assertProductCosting(product.getId(), productCostingAssertList);

      // Assert cost adjustment
      List<CostAdjustment> costAdjustmentList = getCostAdjustment(product.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList.add(new CostAdjustmentAssert(transactionList.get(0), "PDC",
          quantity1.multiply(price2).add(quantity1.multiply(price1).negate()), day2, true));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList);
      assertCostAdjustment(costAdjustmentList, costAdjustmentAssertList);

      // Post cost adjustment 1 and assert it
      postDocument(costAdjustmentList.get(0));
      List<DocumentPostAssert> documentPostAssertList = new ArrayList<DocumentPostAssert>();
      documentPostAssertList.add(new DocumentPostAssert("99904", quantity1.multiply(price1).add(
          quantity1.multiply(price2).negate()), BigDecimal.ZERO, null));
      documentPostAssertList.add(new DocumentPostAssert("35000", BigDecimal.ZERO, quantity1
          .multiply(price1).add(quantity1.multiply(price2).negate()), null));
      CostAdjustment costAdjustment = OBDal.getInstance().get(CostAdjustment.class,
          costAdjustmentList.get(0).getId());
      assertDocumentPost(costAdjustment, product.getId(), documentPostAssertList);

      OBDal.getInstance().commitAndClose();

    } catch (Exception e) {
      System.out.println(e.getMessage());
      throw new OBException(e);
    }

    finally {
      OBContext.restorePreviousMode();
    }
  }

  @Test
  public void testCostingR3() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final BigDecimal price1 = new BigDecimal("15.00");
    final BigDecimal price2 = new BigDecimal("7.50");
    final BigDecimal quantity1 = new BigDecimal("180");

    try {

      OBContext.setOBContext(USER_ID, ROLE_ID, CLIENT_ID, ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product = createProduct("testCostingR3", price1);

      // Create purchase order and book it
      Order purchaseOrder = createPurchaseOrder(product, price1, quantity1, day0);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt1 = createGoodsReceipt(purchaseOrder, price1, quantity1, day1);

      // Update purchase order line product price
      updatePurchaseOrder(purchaseOrder, price2);

      // Run price correction background
      runPriceBackground();

      // Cancel goods receipt
      ShipmentInOut goodsReceipt2 = cancelGoodsReceipt(goodsReceipt1, price2);

      // Assert product transactions
      List<ProductTransactionAssert> productTransactionAssertList = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt2.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price2, price2, true));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt1.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price2, true));
      assertProductTransaction(product.getId(), productTransactionAssertList);

      // Assert product costing
      List<MaterialTransaction> transactionList = getProductTransactions(product.getId());
      List<ProductCostingAssert> productCostingAssertList = new ArrayList<ProductCostingAssert>();
      productCostingAssertList.add(new ProductCostingAssert(transactionList.get(1), price2, price1,
          price2, quantity1));
      productCostingAssertList.add(new ProductCostingAssert(transactionList.get(0), price2, null,
          price2, BigDecimal.ZERO));
      assertProductCosting(product.getId(), productCostingAssertList);

      // Assert cost adjustment
      List<CostAdjustment> costAdjustmentList = getCostAdjustment(product.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList.add(new CostAdjustmentAssert(transactionList.get(1), "PDC",
          quantity1.multiply(price2).add(quantity1.multiply(price1).negate()), day1, true));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList);
      assertCostAdjustment(costAdjustmentList, costAdjustmentAssertList);

      // Post cost adjustment 1 and assert it
      postDocument(costAdjustmentList.get(0));
      List<DocumentPostAssert> documentPostAssertList = new ArrayList<DocumentPostAssert>();
      documentPostAssertList.add(new DocumentPostAssert("99904", quantity1.multiply(price1).add(
          quantity1.multiply(price2).negate()), BigDecimal.ZERO, null));
      documentPostAssertList.add(new DocumentPostAssert("35000", BigDecimal.ZERO, quantity1
          .multiply(price1).add(quantity1.multiply(price2).negate()), null));
      CostAdjustment costAdjustment = OBDal.getInstance().get(CostAdjustment.class,
          costAdjustmentList.get(0).getId());
      assertDocumentPost(costAdjustment, product.getId(), documentPostAssertList);

      OBDal.getInstance().commitAndClose();

    } catch (Exception e) {
      System.out.println(e.getMessage());
      throw new OBException(e);
    }

    finally {
      OBContext.restorePreviousMode();
    }
  }

  @Test
  public void testCostingMCC1() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final int day3 = 15;
    final int day4 = 20;
    final int day5 = 25;
    final BigDecimal price1 = new BigDecimal("11.50");
    final BigDecimal price2 = new BigDecimal("15.00");
    final BigDecimal price3 = new BigDecimal("15.0714");
    final BigDecimal price4 = new BigDecimal("14.9375");
    final BigDecimal price5 = new BigDecimal("11.4375");
    final BigDecimal quantity1 = new BigDecimal("15");
    final BigDecimal quantity2 = new BigDecimal("7");
    final BigDecimal quantity3 = new BigDecimal("3");
    final BigDecimal amount1 = new BigDecimal("0.50");

    try {

      OBContext.setOBContext(USER_ID, ROLE_ID, CLIENT_ID, ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product = createProduct("testCostingMCC1", price1);

      // Create purchase order and book it
      Order purchaseOrder = createPurchaseOrder(product, price1, quantity1, day1);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt = createGoodsReceipt(purchaseOrder, price1, quantity1, day0);

      // Create goods shipment, run costing background, post it and assert it
      ShipmentInOut goodsShipment1 = createGoodsShipment(product, price1, quantity2, day2);

      // Update transaction total cost amount
      manualCostAdjustment(getProductTransactions(product.getId()).get(1), amount1, true, false,
          day3);

      // Create purchase invoice, post it and assert it
      createPurchaseInvoice(goodsReceipt, price2, quantity1, day4);

      // Run price correction background
      runPriceBackground();

      // Create goods shipment, run costing background, post it and assert it
      ShipmentInOut goodsShipment2 = createGoodsShipment(product, price4, quantity3, day5);

      // Assert product transactions
      List<ProductTransactionAssert> productTransactionAssertList = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price2));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsShipment1.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price3, price2));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsShipment2.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price4, price4));
      assertProductTransaction(product.getId(), productTransactionAssertList);

      // Assert product costing
      List<MaterialTransaction> transactionList = getProductTransactions(product.getId());
      List<ProductCostingAssert> productCostingAssertList = new ArrayList<ProductCostingAssert>();
      productCostingAssertList.add(new ProductCostingAssert(transactionList.get(0), price2, price1,
          price2, quantity1));
      productCostingAssertList.add(new ProductCostingAssert(transactionList.get(1), price2, price5,
          price4, quantity1.add(quantity2.negate())));
      assertProductCosting(product.getId(), productCostingAssertList);

      // Assert cost adjustment
      List<CostAdjustment> costAdjustmentList = getCostAdjustment(product.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList1 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList1.add(new CostAdjustmentAssert(transactionList.get(1), "MCC",
          amount1, day3, true, false));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList1);
      List<CostAdjustmentAssert> costAdjustmentAssertLineList2 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList2.add(new CostAdjustmentAssert(transactionList.get(0), "PDC",
          quantity1.multiply(price2).add(quantity1.multiply(price1).negate()), day4, true));
      costAdjustmentAssertLineList2.add(new CostAdjustmentAssert(transactionList.get(1), "PDC",
          quantity2.multiply(price2).add(quantity2.multiply(price1).negate()), day4, false));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList2);
      assertCostAdjustment(costAdjustmentList, costAdjustmentAssertList);

      // Post cost adjustment 1 and assert it
      postDocument(costAdjustmentList.get(0));
      List<DocumentPostAssert> documentPostAssertList1 = new ArrayList<DocumentPostAssert>();
      documentPostAssertList1.add(new DocumentPostAssert("99900", amount1, BigDecimal.ZERO, null));
      documentPostAssertList1.add(new DocumentPostAssert("35000", BigDecimal.ZERO, amount1, null));
      CostAdjustment costAdjustment1 = OBDal.getInstance().get(CostAdjustment.class,
          costAdjustmentList.get(0).getId());
      assertDocumentPost(costAdjustment1, product.getId(), documentPostAssertList1);

      // Post cost adjustment 1 and assert it
      postDocument(costAdjustmentList.get(1));
      List<DocumentPostAssert> documentPostAssertList2 = new ArrayList<DocumentPostAssert>();
      documentPostAssertList2.add(new DocumentPostAssert("99904", BigDecimal.ZERO, quantity1
          .multiply(price2).add(quantity1.multiply(price1).negate()), null));
      documentPostAssertList2.add(new DocumentPostAssert("35000", quantity1.multiply(price2).add(
          quantity1.multiply(price1).negate()), BigDecimal.ZERO, null));
      documentPostAssertList2.add(new DocumentPostAssert("99900", quantity2.multiply(price2).add(
          quantity2.multiply(price1).negate()), BigDecimal.ZERO, null));
      documentPostAssertList2.add(new DocumentPostAssert("35000", BigDecimal.ZERO, quantity2
          .multiply(price2).add(quantity2.multiply(price1).negate()), null));
      CostAdjustment costAdjustment2 = OBDal.getInstance().get(CostAdjustment.class,
          costAdjustmentList.get(1).getId());
      assertDocumentPost(costAdjustment2, product.getId(), documentPostAssertList2);

      OBDal.getInstance().commitAndClose();

    } catch (Exception e) {
      System.out.println(e.getMessage());
      throw new OBException(e);
    }

    finally {
      OBContext.restorePreviousMode();
    }
  }

  @Test
  public void testCostingBOM() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final int day3 = 15;
    final int day4 = 20;
    final BigDecimal price1 = new BigDecimal("15.00");
    final BigDecimal price2 = new BigDecimal("25.00");
    final BigDecimal price3 = new BigDecimal("17.50");
    final BigDecimal price4 = new BigDecimal("31.50");
    final BigDecimal price5 = new BigDecimal("95.00");
    final BigDecimal price6 = new BigDecimal("115.50");
    final BigDecimal quantity1 = new BigDecimal("3");
    final BigDecimal quantity2 = new BigDecimal("2");
    final BigDecimal quantity3 = new BigDecimal("30");
    final BigDecimal quantity4 = new BigDecimal("20");
    final BigDecimal quantity5 = new BigDecimal("10");

    try {

      OBContext.setOBContext(USER_ID, ROLE_ID, CLIENT_ID, ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product1 = createProduct("testCostingBOMA", price1);

      // Create a new product for the test
      Product product2 = createProduct("testCostingBOMB", price2);

      // Create a new product for the test
      List<Product> productList = new ArrayList<Product>();
      productList.add(product1);
      productList.add(product2);
      List<BigDecimal> quantityList = new ArrayList<BigDecimal>();
      quantityList.add(quantity1);
      quantityList.add(quantity2);
      Product product3 = createProduct("testCostingBOMC", productList, quantityList);

      // Create purchase order and book it
      Order purchaseOrder1 = createPurchaseOrder(product1, price1, quantity3, day0);

      // Create purchase order and book it
      Order purchaseOrder2 = createPurchaseOrder(product2, price2, quantity4, day0);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt1 = createGoodsReceipt(purchaseOrder1, price1, quantity3,
          LOCATOR4_ID, day1);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt2 = createGoodsReceipt(purchaseOrder2, price2, quantity4,
          LOCATOR4_ID, day2);

      // Create bill of materials production, run costing background, post it and assert it
      ProductionTransaction billOfMaterialsProduction = createBillOfMaterialsProduction(product3,
          quantity5, LOCATOR1_ID, day3);

      // Create purchase invoice, post it and assert it
      List<ShipmentInOut> goodsReceiptList = new ArrayList<ShipmentInOut>();
      goodsReceiptList.add(goodsReceipt1);
      goodsReceiptList.add(goodsReceipt2);
      List<BigDecimal> priceList = new ArrayList<BigDecimal>();
      priceList.add(price3);
      priceList.add(price4);
      createPurchaseInvoice(goodsReceiptList, priceList, quantity3.add(quantity4), day4);

      // Run price correction background
      runPriceBackground();

      // Assert product transactions 1
      List<ProductTransactionAssert> productTransactionAssertList1 = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList1.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt1.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price3));
      productTransactionAssertList1.add(new ProductTransactionAssert(getProductionLines(
          billOfMaterialsProduction.getId()).get(0), price1, price3));
      assertProductTransaction(product1.getId(), productTransactionAssertList1);

      // Assert product transactions 2
      List<ProductTransactionAssert> productTransactionAssertList2 = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList2.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt2.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price2, price4));
      productTransactionAssertList2.add(new ProductTransactionAssert(getProductionLines(
          billOfMaterialsProduction.getId()).get(1), price2, price4));
      assertProductTransaction(product2.getId(), productTransactionAssertList2);

      // Assert product transactions 3
      List<ProductTransactionAssert> productTransactionAssertList3 = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList3.add(new ProductTransactionAssert(getProductionLines(
          billOfMaterialsProduction.getId()).get(2), price5, price6));
      assertProductTransaction(product3.getId(), productTransactionAssertList3);

      // Assert product costing 1
      List<MaterialTransaction> transactionList1 = getProductTransactions(product1.getId());
      List<ProductCostingAssert> productCostingAssertList1 = new ArrayList<ProductCostingAssert>();
      productCostingAssertList1.add(new ProductCostingAssert(transactionList1.get(0),
          WAREHOUSE2_ID, price3, price1, price3, quantity3));
      assertProductCosting(product1.getId(), productCostingAssertList1);

      // Assert product costing 2
      List<MaterialTransaction> transactionList2 = getProductTransactions(product2.getId());
      List<ProductCostingAssert> productCostingAssertList2 = new ArrayList<ProductCostingAssert>();
      productCostingAssertList2.add(new ProductCostingAssert(transactionList2.get(0),
          WAREHOUSE2_ID, price4, price2, price4, quantity4));
      assertProductCosting(product2.getId(), productCostingAssertList2);

      // Assert product costing 3
      List<MaterialTransaction> transactionList3 = getProductTransactions(product3.getId());
      List<ProductCostingAssert> productCostingAssertList3 = new ArrayList<ProductCostingAssert>();
      productCostingAssertList3.add(new ProductCostingAssert(transactionList3.get(0),
          WAREHOUSE1_ID, price6, price5, price6, quantity5));
      assertProductCosting(product3.getId(), productCostingAssertList3);

      // Assert cost adjustment 1
      List<CostAdjustment> costAdjustmentList1 = getCostAdjustment(product1.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList1 = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList11 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList11.add(new CostAdjustmentAssert(transactionList1.get(0), "PDC",
          quantity3.multiply(price3).add(quantity3.multiply(price1).negate()), day4, true));
      costAdjustmentAssertLineList11.add(new CostAdjustmentAssert(transactionList2.get(0), "PDC",
          quantity4.multiply(price4).add(quantity4.multiply(price2).negate()), day4, true));
      costAdjustmentAssertLineList11.add(new CostAdjustmentAssert(transactionList1.get(1), "PDC",
          quantity3.multiply(price3).add(quantity3.multiply(price1).negate()), day4, false));
      costAdjustmentAssertLineList11.add(new CostAdjustmentAssert(transactionList3.get(0), "PDC",
          quantity3.multiply(price3).add(quantity3.multiply(price1).negate()), day4, false));
      costAdjustmentAssertLineList11.add(new CostAdjustmentAssert(transactionList2.get(1), "PDC",
          quantity4.multiply(price4).add(quantity4.multiply(price2).negate()), day4, false));
      costAdjustmentAssertLineList11.add(new CostAdjustmentAssert(transactionList3.get(0), "PDC",
          quantity4.multiply(price4).add(quantity4.multiply(price2).negate()), day4, false));
      costAdjustmentAssertList1.add(costAdjustmentAssertLineList11);
      assertCostAdjustment(costAdjustmentList1, costAdjustmentAssertList1);

      // Assert cost adjustment 2
      List<CostAdjustment> costAdjustmentList2 = getCostAdjustment(product2.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList2 = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList21 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList21.add(new CostAdjustmentAssert(transactionList1.get(0), "PDC",
          quantity3.multiply(price3).add(quantity3.multiply(price1).negate()), day4, true));
      costAdjustmentAssertLineList21.add(new CostAdjustmentAssert(transactionList2.get(0), "PDC",
          quantity4.multiply(price4).add(quantity4.multiply(price2).negate()), day4, true));
      costAdjustmentAssertLineList21.add(new CostAdjustmentAssert(transactionList1.get(1), "PDC",
          quantity3.multiply(price3).add(quantity3.multiply(price1).negate()), day4, false));
      costAdjustmentAssertLineList21.add(new CostAdjustmentAssert(transactionList3.get(0), "PDC",
          quantity3.multiply(price3).add(quantity3.multiply(price1).negate()), day4, false));
      costAdjustmentAssertLineList21.add(new CostAdjustmentAssert(transactionList2.get(1), "PDC",
          quantity4.multiply(price4).add(quantity4.multiply(price2).negate()), day4, false));
      costAdjustmentAssertLineList21.add(new CostAdjustmentAssert(transactionList3.get(0), "PDC",
          quantity4.multiply(price4).add(quantity4.multiply(price2).negate()), day4, false));
      costAdjustmentAssertList2.add(costAdjustmentAssertLineList21);
      assertCostAdjustment(costAdjustmentList2, costAdjustmentAssertList2);

      // Assert cost adjustment 3
      List<CostAdjustment> costAdjustmentList3 = getCostAdjustment(product3.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList3 = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList31 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList31.add(new CostAdjustmentAssert(transactionList1.get(0), "PDC",
          quantity3.multiply(price3).add(quantity3.multiply(price1).negate()), day4, true));
      costAdjustmentAssertLineList31.add(new CostAdjustmentAssert(transactionList2.get(0), "PDC",
          quantity4.multiply(price4).add(quantity4.multiply(price2).negate()), day4, true));
      costAdjustmentAssertLineList31.add(new CostAdjustmentAssert(transactionList1.get(1), "PDC",
          quantity3.multiply(price3).add(quantity3.multiply(price1).negate()), day4, false));
      costAdjustmentAssertLineList31.add(new CostAdjustmentAssert(transactionList3.get(0), "PDC",
          quantity3.multiply(price3).add(quantity3.multiply(price1).negate()), day4, false));
      costAdjustmentAssertLineList31.add(new CostAdjustmentAssert(transactionList2.get(1), "PDC",
          quantity4.multiply(price4).add(quantity4.multiply(price2).negate()), day4, false));
      costAdjustmentAssertLineList31.add(new CostAdjustmentAssert(transactionList3.get(0), "PDC",
          quantity4.multiply(price4).add(quantity4.multiply(price2).negate()), day4, false));
      costAdjustmentAssertList3.add(costAdjustmentAssertLineList31);
      assertCostAdjustment(costAdjustmentList3, costAdjustmentAssertList3);

      // Post cost adjustment 1 and assert it
      postDocument(costAdjustmentList1.get(0));
      List<DocumentPostAssert> documentPostAssertList1 = new ArrayList<DocumentPostAssert>();
      documentPostAssertList1.add(new DocumentPostAssert(product1.getId(), "99904",
          BigDecimal.ZERO, quantity3.multiply(price3).add(quantity3.multiply(price1).negate()),
          null));
      documentPostAssertList1.add(new DocumentPostAssert(product1.getId(), "35000", quantity3
          .multiply(price3).add(quantity3.multiply(price1).negate()), BigDecimal.ZERO, null));
      documentPostAssertList1.add(new DocumentPostAssert(product2.getId(), "99904",
          BigDecimal.ZERO, quantity4.multiply(price4).add(quantity4.multiply(price2).negate()),
          null));
      documentPostAssertList1.add(new DocumentPostAssert(product2.getId(), "35000", quantity4
          .multiply(price4).add(quantity4.multiply(price2).negate()), BigDecimal.ZERO, null));
      documentPostAssertList1.add(new DocumentPostAssert(product1.getId(), "61000", quantity3
          .multiply(price3).add(quantity3.multiply(price1).negate()), BigDecimal.ZERO, null));
      documentPostAssertList1.add(new DocumentPostAssert(product1.getId(), "35000",
          BigDecimal.ZERO, quantity3.multiply(price3).add(quantity3.multiply(price1).negate()),
          null));
      documentPostAssertList1.add(new DocumentPostAssert(product3.getId(), "61000",
          BigDecimal.ZERO, quantity3.multiply(price3).add(quantity3.multiply(price1).negate()),
          null));
      documentPostAssertList1.add(new DocumentPostAssert(product3.getId(), "35000", quantity3
          .multiply(price3).add(quantity3.multiply(price1).negate()), BigDecimal.ZERO, null));
      documentPostAssertList1.add(new DocumentPostAssert(product2.getId(), "61000", quantity4
          .multiply(price4).add(quantity4.multiply(price2).negate()), BigDecimal.ZERO, null));
      documentPostAssertList1.add(new DocumentPostAssert(product2.getId(), "35000",
          BigDecimal.ZERO, quantity4.multiply(price4).add(quantity4.multiply(price2).negate()),
          null));
      documentPostAssertList1.add(new DocumentPostAssert(product3.getId(), "61000",
          BigDecimal.ZERO, quantity4.multiply(price4).add(quantity4.multiply(price2).negate()),
          null));
      documentPostAssertList1.add(new DocumentPostAssert(product3.getId(), "35000", quantity4
          .multiply(price4).add(quantity4.multiply(price2).negate()), BigDecimal.ZERO, null));
      CostAdjustment costAdjustment1 = OBDal.getInstance().get(CostAdjustment.class,
          costAdjustmentList1.get(0).getId());
      assertDocumentPost(costAdjustment1, null, documentPostAssertList1);

      OBDal.getInstance().commitAndClose();

    } catch (Exception e) {
      System.out.println(e.getMessage());
      throw new OBException(e);
    }

    finally {
      OBContext.restorePreviousMode();
    }
  }

  @Test
  public void testCostingLC1() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final BigDecimal price1 = new BigDecimal("135.00");
    final BigDecimal price2 = new BigDecimal("145.00");
    final BigDecimal price3 = new BigDecimal("80.00");
    final BigDecimal price4 = new BigDecimal("105.00");
    final BigDecimal price5 = new BigDecimal("145.37");
    final BigDecimal quantity1 = new BigDecimal("500");
    final BigDecimal quantity2 = BigDecimal.ONE;

    try {

      OBContext.setOBContext(USER_ID, ROLE_ID, CLIENT_ID, ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product = createProduct("testCostingLC1", price1);

      // Create purchase order and book it
      Order purchaseOrder = createPurchaseOrder(product, price1, quantity1, day0);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt = createGoodsReceipt(purchaseOrder, price1, quantity1, day1);

      // Create purchase invoice, post it and assert it
      createPurchaseInvoice(purchaseOrder, price2, quantity1, day2);

      // Run price correction background
      runPriceBackground();

      // Create purchase invoice with landed cost, post it and assert it
      Invoice purchaseInvoiceLandedCost1 = createPurchaseInvoiceLandedCost(LANDEDCOSTTYPE1_ID,
          price3, quantity2, day2);

      // Create purchase invoice with landed cost, post it and assert it
      Invoice purchaseInvoiceLandedCost2 = createPurchaseInvoiceLandedCost(LANDEDCOSTTYPE2_ID,
          price4, quantity2, day2);

      // Create Landed Cost
      List<Invoice> invoiceList = new ArrayList<Invoice>();
      invoiceList.add(purchaseInvoiceLandedCost1);
      invoiceList.add(purchaseInvoiceLandedCost2);
      List<ShipmentInOut> receiptList = new ArrayList<ShipmentInOut>();
      receiptList.add(goodsReceipt);
      createLandedCost(invoiceList, receiptList, day2);

      // Assert product transactions
      List<ProductTransactionAssert> productTransactionAssertList = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price5, price2));
      assertProductTransaction(product.getId(), productTransactionAssertList);

      // Assert product costing
      List<MaterialTransaction> transactionList = getProductTransactions(product.getId());
      List<ProductCostingAssert> productCostingAssertList = new ArrayList<ProductCostingAssert>();
      productCostingAssertList.add(new ProductCostingAssert(transactionList.get(0), price2, price1,
          price5, quantity1));
      assertProductCosting(product.getId(), productCostingAssertList);

      // Assert cost adjustment
      List<CostAdjustment> costAdjustmentList = getCostAdjustment(product.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList1 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList1.add(new CostAdjustmentAssert(transactionList.get(0), "PDC",
          quantity1.multiply(price2).add(quantity1.multiply(price1).negate()), day2, true));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList1);
      List<CostAdjustmentAssert> costAdjustmentAssertLineList2 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList2.add(new CostAdjustmentAssert(transactionList.get(0), "LC",
          quantity2.multiply(price3).add(quantity2.multiply(price4)), day2, true, false));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList2);
      assertCostAdjustment(costAdjustmentList, costAdjustmentAssertList);

      // Post cost adjustment 1 and assert it
      postDocument(costAdjustmentList.get(0));
      List<DocumentPostAssert> documentPostAssertList1 = new ArrayList<DocumentPostAssert>();
      documentPostAssertList1.add(new DocumentPostAssert("99904", BigDecimal.ZERO, quantity1
          .multiply(price2).add(quantity1.multiply(price1).negate()), null));
      documentPostAssertList1.add(new DocumentPostAssert("35000", quantity1.multiply(price2).add(
          quantity1.multiply(price1).negate()), BigDecimal.ZERO, null));
      CostAdjustment costAdjustment1 = OBDal.getInstance().get(CostAdjustment.class,
          costAdjustmentList.get(0).getId());
      assertDocumentPost(costAdjustment1, product.getId(), documentPostAssertList1);

      OBDal.getInstance().commitAndClose();

    } catch (Exception e) {
      System.out.println(e.getMessage());
      throw new OBException(e);
    }

    finally {
      OBContext.restorePreviousMode();
    }
  }

  @Test
  public void testCostingLC2() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final int day3 = 15;
    final int day4 = 20;
    final BigDecimal price1 = new BigDecimal("120.00");
    final BigDecimal price2 = new BigDecimal("95.00");
    final BigDecimal price3 = new BigDecimal("105.00");
    final BigDecimal price4 = new BigDecimal("130.00");
    final BigDecimal price5 = new BigDecimal("130.75");
    final BigDecimal quantity1 = new BigDecimal("1500");
    final BigDecimal quantity2 = new BigDecimal("320");
    final BigDecimal quantity3 = new BigDecimal("180");
    final BigDecimal quantity4 = new BigDecimal("300");
    final BigDecimal quantity5 = new BigDecimal("3");

    try {

      OBContext.setOBContext(USER_ID, ROLE_ID, CLIENT_ID, ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product = createProduct("testCostingLC2", price1);

      // Create purchase order and book it
      Order purchaseOrder = createPurchaseOrder(product, price1, quantity1, day0);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt1 = createGoodsReceipt(purchaseOrder, price1, quantity2, day1);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt2 = createGoodsReceipt(purchaseOrder, price1, quantity3, day2);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt3 = createGoodsReceipt(purchaseOrder, price1, quantity4, day3);

      // Create purchase invoice with landed cost, post it and assert it
      Invoice purchaseInvoiceLandedCost1 = createPurchaseInvoiceLandedCost(LANDEDCOSTTYPE1_ID,
          price2, quantity5, day0);

      // Create purchase invoice with landed cost, post it and assert it
      Invoice purchaseInvoiceLandedCost2 = createPurchaseInvoiceLandedCost(LANDEDCOSTTYPE2_ID,
          price3, quantity5, day0);

      // Create Landed Cost
      List<Invoice> invoiceList = new ArrayList<Invoice>();
      invoiceList.add(purchaseInvoiceLandedCost1);
      invoiceList.add(purchaseInvoiceLandedCost2);
      List<ShipmentInOut> receiptList = new ArrayList<ShipmentInOut>();
      receiptList.add(goodsReceipt1);
      receiptList.add(goodsReceipt2);
      receiptList.add(goodsReceipt3);
      LandedCost landedCost = createLandedCost(invoiceList, receiptList, day0);

      // Create purchase invoice, post it and assert it
      List<BigDecimal> priceList = new ArrayList<BigDecimal>();
      priceList.add(price4);
      priceList.add(price4);
      priceList.add(price4);
      createPurchaseInvoice(receiptList, priceList, quantity2.add(quantity3).add(quantity4), day4);

      // Run price correction background
      runPriceBackground();

      // Assert product transactions
      List<ProductTransactionAssert> productTransactionAssertList = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt1.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price5, price4));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt2.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price5, price4));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt3.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price5, price4));
      assertProductTransaction(product.getId(), productTransactionAssertList);

      // Assert product costing
      List<MaterialTransaction> transactionList = getProductTransactions(product.getId());
      List<ProductCostingAssert> productCostingAssertList = new ArrayList<ProductCostingAssert>();
      productCostingAssertList.add(new ProductCostingAssert(transactionList.get(0), price4, price1,
          price5, quantity2));
      productCostingAssertList.add(new ProductCostingAssert(transactionList.get(1), price4, price1,
          price5, quantity2.add(quantity3)));
      productCostingAssertList.add(new ProductCostingAssert(transactionList.get(2), price4, price1,
          price5, quantity2.add(quantity3).add(quantity4)));
      assertProductCosting(product.getId(), productCostingAssertList);

      // Assert cost adjustment
      List<CostAdjustment> costAdjustmentList = getCostAdjustment(product.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList1 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList1.add(new CostAdjustmentAssert(transactionList.get(0), "LC",
          quantity2.multiply(price5).add(quantity2.multiply(price4).negate()), day0, true, false));
      costAdjustmentAssertLineList1.add(new CostAdjustmentAssert(transactionList.get(1), "LC",
          quantity3.multiply(price5).add(quantity3.multiply(price4).negate()), day0, true, false));
      costAdjustmentAssertLineList1.add(new CostAdjustmentAssert(transactionList.get(2), "LC",
          quantity4.multiply(price5).add(quantity4.multiply(price4).negate()), day0, true, false));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList1);
      List<CostAdjustmentAssert> costAdjustmentAssertLineList2 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList2.add(new CostAdjustmentAssert(transactionList.get(0), "PDC",
          quantity2.multiply(price4).add(quantity2.multiply(price1).negate()), day4, true));
      costAdjustmentAssertLineList2.add(new CostAdjustmentAssert(transactionList.get(1), "PDC",
          quantity3.multiply(price4).add(quantity3.multiply(price1).negate()), day4, true));
      costAdjustmentAssertLineList2.add(new CostAdjustmentAssert(transactionList.get(2), "PDC",
          quantity4.multiply(price4).add(quantity4.multiply(price1).negate()), day4, true));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList2);
      assertCostAdjustment(costAdjustmentList, costAdjustmentAssertList);

      // Post cost adjustment 1 and assert it
      postDocument(costAdjustmentList.get(1));
      List<DocumentPostAssert> documentPostAssertList1 = new ArrayList<DocumentPostAssert>();
      documentPostAssertList1.add(new DocumentPostAssert("99904", BigDecimal.ZERO, quantity2
          .multiply(price4).add(quantity2.multiply(price1).negate()), null));
      documentPostAssertList1.add(new DocumentPostAssert("35000", quantity2.multiply(price4).add(
          quantity2.multiply(price1).negate()), BigDecimal.ZERO, null));
      documentPostAssertList1.add(new DocumentPostAssert("99904", BigDecimal.ZERO, quantity3
          .multiply(price4).add(quantity3.multiply(price1).negate()), null));
      documentPostAssertList1.add(new DocumentPostAssert("35000", quantity3.multiply(price4).add(
          quantity3.multiply(price1).negate()), BigDecimal.ZERO, null));
      documentPostAssertList1.add(new DocumentPostAssert("99904", BigDecimal.ZERO, quantity4
          .multiply(price4).add(quantity4.multiply(price1).negate()), null));
      documentPostAssertList1.add(new DocumentPostAssert("35000", quantity4.multiply(price4).add(
          quantity4.multiply(price1).negate()), BigDecimal.ZERO, null));
      CostAdjustment costAdjustment1 = OBDal.getInstance().get(CostAdjustment.class,
          costAdjustmentList.get(1).getId());
      assertDocumentPost(costAdjustment1, product.getId(), documentPostAssertList1);

      // Reactivate landed cost
      cancelLandedCost(landedCost);

      // Assert product transactions
      List<ProductTransactionAssert> productTransactionAssertList2 = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList2.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt1.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price4));
      productTransactionAssertList2.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt2.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price4));
      productTransactionAssertList2.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt3.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price4));
      assertProductTransaction(product.getId(), productTransactionAssertList2);

      // Assert product costing
      List<MaterialTransaction> transactionList2 = getProductTransactions(product.getId());
      List<ProductCostingAssert> productCostingAssertList2 = new ArrayList<ProductCostingAssert>();
      productCostingAssertList2.add(new ProductCostingAssert(transactionList2.get(0), price4,
          price1, price4, quantity2));
      productCostingAssertList2.add(new ProductCostingAssert(transactionList2.get(1), price4,
          price1, price4, quantity2.add(quantity3)));
      productCostingAssertList2.add(new ProductCostingAssert(transactionList2.get(2), price4,
          price1, price4, quantity2.add(quantity3).add(quantity4)));
      assertProductCosting(product.getId(), productCostingAssertList2);

      // Assert cost adjustment
      List<CostAdjustment> costAdjustmentList2 = getCostAdjustment(product.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList2 = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList21 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList21.add(new CostAdjustmentAssert(transactionList2.get(0), "LC",
          quantity2.multiply(price5).add(quantity2.multiply(price4).negate()), day0, true, false,
          "VO"));
      costAdjustmentAssertLineList21.add(new CostAdjustmentAssert(transactionList2.get(1), "LC",
          quantity3.multiply(price5).add(quantity3.multiply(price4).negate()), day0, true, false,
          "VO"));
      costAdjustmentAssertLineList21.add(new CostAdjustmentAssert(transactionList2.get(2), "LC",
          quantity4.multiply(price5).add(quantity4.multiply(price4).negate()), day0, true, false,
          "VO"));
      costAdjustmentAssertList2.add(costAdjustmentAssertLineList21);
      List<CostAdjustmentAssert> costAdjustmentAssertLineList22 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList22.add(new CostAdjustmentAssert(transactionList2.get(0), "PDC",
          quantity2.multiply(price4).add(quantity2.multiply(price1).negate()), day4, true));
      costAdjustmentAssertLineList22.add(new CostAdjustmentAssert(transactionList2.get(1), "PDC",
          quantity3.multiply(price4).add(quantity3.multiply(price1).negate()), day4, true));
      costAdjustmentAssertLineList22.add(new CostAdjustmentAssert(transactionList2.get(2), "PDC",
          quantity4.multiply(price4).add(quantity4.multiply(price1).negate()), day4, true));
      costAdjustmentAssertList2.add(costAdjustmentAssertLineList22);
      List<CostAdjustmentAssert> costAdjustmentAssertLineList23 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList23.add(new CostAdjustmentAssert(transactionList2.get(0), "LC",
          quantity2.multiply(price4).add(quantity2.multiply(price5).negate()), day0, true, false,
          "VO"));
      costAdjustmentAssertLineList23.add(new CostAdjustmentAssert(transactionList2.get(1), "LC",
          quantity3.multiply(price4).add(quantity3.multiply(price5).negate()), day0, true, false,
          "VO"));
      costAdjustmentAssertLineList23.add(new CostAdjustmentAssert(transactionList2.get(2), "LC",
          quantity4.multiply(price4).add(quantity4.multiply(price5).negate()), day0, true, false,
          "VO"));
      costAdjustmentAssertList2.add(costAdjustmentAssertLineList23);
      assertCostAdjustment(costAdjustmentList2, costAdjustmentAssertList2);

      OBDal.getInstance().commitAndClose();

    } catch (Exception e) {
      System.out.println(e.getMessage());
      throw new OBException(e);
    }

    finally {
      OBContext.restorePreviousMode();
    }
  }

  @Test
  public void testCostingLC3LC4() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final int day3 = 15;
    final int day4 = 20;
    final int day5 = 25;
    final int day6 = 30;
    final BigDecimal price1 = new BigDecimal("100.00");
    final BigDecimal price2 = new BigDecimal("110.00");
    final BigDecimal price3 = new BigDecimal("1500.00");
    final BigDecimal price4 = new BigDecimal("210.00");
    final BigDecimal price5 = new BigDecimal("130.00");
    final BigDecimal price6 = new BigDecimal("126.0936");
    final BigDecimal price7 = new BigDecimal("115.0600");
    final BigDecimal price8 = new BigDecimal("126.0933");
    final BigDecimal price9 = new BigDecimal("138.7029");
    final BigDecimal price10 = new BigDecimal("146.5664");
    final BigDecimal price11 = new BigDecimal("138.7030");
    final BigDecimal price12 = new BigDecimal("119.7281");
    final BigDecimal price13 = new BigDecimal("121.7382");
    final BigDecimal price14 = new BigDecimal("144.8463");
    final BigDecimal price15 = new BigDecimal("142.2134");
    final BigDecimal price16 = new BigDecimal("84.9400");
    final BigDecimal price17 = new BigDecimal("93.4338");
    final BigDecimal price18 = new BigDecimal("88.9665");
    final BigDecimal price19 = new BigDecimal("97.8632");
    final BigDecimal quantity1 = new BigDecimal("11");
    final BigDecimal quantity2 = new BigDecimal("7");
    final BigDecimal quantity3 = new BigDecimal("15");
    final BigDecimal quantity4 = new BigDecimal("25");
    final BigDecimal quantity5 = new BigDecimal("12");
    final BigDecimal quantity6 = new BigDecimal("24");
    final BigDecimal quantity7 = BigDecimal.ONE;
    final BigDecimal quantity8 = new BigDecimal("3");
    final BigDecimal amount1 = new BigDecimal("500");
    final String costType = "AVA";

    try {

      OBContext.setOBContext(USER_ID, ROLE_ID, CLIENT_ID, ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product1 = createProduct("testCostingLC3LC4A", price1, price1, costType);

      // Create a new product for the test
      Product product2 = createProduct("testCostingLC3LC4B", price2, price2, costType);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt11 = createGoodsReceipt(product1, price1, quantity1, day1);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt21 = createGoodsReceipt(product2, price2, quantity2, day1);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt12 = createGoodsReceipt(product1, price1, quantity3, day2);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt22 = createGoodsReceipt(product2, price2, quantity4, day2);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt13 = createGoodsReceipt(product1, price1, quantity5, day3);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt23 = createGoodsReceipt(product2, price2, quantity6, day3);

      // Create purchase invoice with landed cost, post it and assert it
      Invoice purchaseInvoiceLandedCost1 = createPurchaseInvoiceLandedCost(LANDEDCOSTTYPE1_ID,
          price3, quantity7, day0);

      // Create purchase invoice with landed cost, post it and assert it
      Invoice purchaseInvoiceLandedCost2 = createPurchaseInvoiceLandedCost(LANDEDCOSTTYPE2_ID,
          price4, quantity8, day4);

      // Create Landed Cost
      List<Invoice> invoiceList1 = new ArrayList<Invoice>();
      invoiceList1.add(purchaseInvoiceLandedCost1);
      List<ShipmentInOut> receiptList1 = new ArrayList<ShipmentInOut>();
      receiptList1.add(goodsReceipt11);
      receiptList1.add(goodsReceipt21);
      receiptList1.add(goodsReceipt12);
      receiptList1.add(goodsReceipt22);
      receiptList1.add(goodsReceipt13);
      receiptList1.add(goodsReceipt23);
      createLandedCost(invoiceList1, receiptList1, day0);

      // Create Landed Cost
      List<Invoice> invoiceList2 = new ArrayList<Invoice>();
      invoiceList2.add(purchaseInvoiceLandedCost2);
      List<ShipmentInOut> receiptList2 = new ArrayList<ShipmentInOut>();
      receiptList2.add(goodsReceipt11);
      receiptList2.add(goodsReceipt21);
      receiptList2.add(goodsReceipt13);
      receiptList2.add(goodsReceipt23);
      createLandedCost(invoiceList2, receiptList2, day5);

      // Update transaction total cost amount
      manualCostAdjustment(getProductTransactions(product2.getId()).get(1), amount1, true, true,
          day6);

      // Assert product transactions 1
      List<ProductTransactionAssert> productTransactionAssertList1 = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList1.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt11.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price6, price1));
      productTransactionAssertList1.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt12.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price7, price1));
      productTransactionAssertList1.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt13.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price8, price1));
      assertProductTransaction(product1.getId(), productTransactionAssertList1);

      // Assert product transactions 2
      List<ProductTransactionAssert> productTransactionAssertList2 = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList2.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt21.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price2, price9, price2));
      productTransactionAssertList2.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt22.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price2, price10, price5));
      productTransactionAssertList2.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt23.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price2, price11, price2));
      assertProductTransaction(product2.getId(), productTransactionAssertList2);

      // Assert product costing 1
      List<MaterialTransaction> transactionList1 = getProductTransactions(product1.getId());
      List<ProductCostingAssert> productCostingAssertList1 = new ArrayList<ProductCostingAssert>();
      productCostingAssertList1.add(new ProductCostingAssert(null, null, null, price1, null,
          costType));
      productCostingAssertList1.add(new ProductCostingAssert(transactionList1.get(0), price1,
          price1, price6, quantity1));
      productCostingAssertList1.add(new ProductCostingAssert(transactionList1.get(1), price1,
          price1, price12, quantity1.add(quantity3)));
      productCostingAssertList1.add(new ProductCostingAssert(transactionList1.get(2), price1,
          price1, price13, quantity1.add(quantity3).add(quantity5)));
      assertProductCosting(product1.getId(), productCostingAssertList1);

      // Assert product costing 2
      List<MaterialTransaction> transactionList2 = getProductTransactions(product2.getId());
      List<ProductCostingAssert> productCostingAssertList2 = new ArrayList<ProductCostingAssert>();
      productCostingAssertList2.add(new ProductCostingAssert(null, null, null, price2, null,
          costType));
      productCostingAssertList2.add(new ProductCostingAssert(transactionList2.get(0), price2,
          price2, price9, quantity2));
      productCostingAssertList2.add(new ProductCostingAssert(transactionList2.get(1), price5,
          price2, price14, quantity2.add(quantity4)));
      productCostingAssertList2.add(new ProductCostingAssert(transactionList2.get(2), price2,
          price2, price15, quantity2.add(quantity4).add(quantity6)));
      assertProductCosting(product2.getId(), productCostingAssertList2);

      // Assert cost adjustment 1
      List<CostAdjustment> costAdjustmentList1 = getCostAdjustment(product1.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList1 = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList11 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList11.add(new CostAdjustmentAssert(transactionList1.get(0), "LC",
          quantity1.multiply(price1).add(quantity1.multiply(price16).negate()), day0, true, false));
      costAdjustmentAssertLineList11.add(new CostAdjustmentAssert(transactionList2.get(0), "LC",
          quantity2.multiply(price2).add(quantity2.multiply(price17).negate()), day0, true, false));
      costAdjustmentAssertLineList11.add(new CostAdjustmentAssert(transactionList1.get(1), "LC",
          quantity3.multiply(price1).add(quantity3.multiply(price16).negate()), day0, true, false));
      costAdjustmentAssertLineList11.add(new CostAdjustmentAssert(transactionList2.get(1), "LC",
          quantity4.multiply(price2).add(quantity4.multiply(price17).negate()), day0, true, false));
      costAdjustmentAssertLineList11.add(new CostAdjustmentAssert(transactionList1.get(2), "LC",
          quantity5.multiply(price1).add(quantity5.multiply(price16).negate()), day0, true, false));
      costAdjustmentAssertLineList11.add(new CostAdjustmentAssert(transactionList2.get(2), "LC",
          quantity6.multiply(price2).add(quantity6.multiply(price17).negate()), day0, true, false));
      costAdjustmentAssertList1.add(costAdjustmentAssertLineList11);
      List<CostAdjustmentAssert> costAdjustmentAssertLineList12 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList12.add(new CostAdjustmentAssert(transactionList1.get(0), "LC",
          quantity1.multiply(price1).add(quantity1.multiply(price18).negate()), day5, true, false));
      costAdjustmentAssertLineList12.add(new CostAdjustmentAssert(transactionList2.get(0), "LC",
          quantity2.multiply(price2).add(quantity2.multiply(price19).negate()), day5, true, false));
      costAdjustmentAssertLineList12.add(new CostAdjustmentAssert(transactionList1.get(2), "LC",
          quantity5.multiply(price1).add(quantity5.multiply(price18).negate()), day5, true, false));
      costAdjustmentAssertLineList12.add(new CostAdjustmentAssert(transactionList2.get(2), "LC",
          quantity6.multiply(price2).add(quantity6.multiply(price19).negate()), day5, true, false));
      costAdjustmentAssertList1.add(costAdjustmentAssertLineList12);
      assertCostAdjustment(costAdjustmentList1, costAdjustmentAssertList1);

      // Assert cost adjustment 2
      List<CostAdjustment> costAdjustmentList2 = getCostAdjustment(product2.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList2 = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList21 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList21.add(new CostAdjustmentAssert(transactionList1.get(0), "LC",
          quantity1.multiply(price1).add(quantity1.multiply(price16).negate()), day0, true, false));
      costAdjustmentAssertLineList21.add(new CostAdjustmentAssert(transactionList2.get(0), "LC",
          quantity2.multiply(price2).add(quantity2.multiply(price17).negate()), day0, true, false));
      costAdjustmentAssertLineList21.add(new CostAdjustmentAssert(transactionList1.get(1), "LC",
          quantity3.multiply(price1).add(quantity3.multiply(price16).negate()), day0, true, false));
      costAdjustmentAssertLineList21.add(new CostAdjustmentAssert(transactionList2.get(1), "LC",
          quantity4.multiply(price2).add(quantity4.multiply(price17).negate()), day0, true, false));
      costAdjustmentAssertLineList21.add(new CostAdjustmentAssert(transactionList1.get(2), "LC",
          quantity5.multiply(price1).add(quantity5.multiply(price16).negate()), day0, true, false));
      costAdjustmentAssertLineList21.add(new CostAdjustmentAssert(transactionList2.get(2), "LC",
          quantity6.multiply(price2).add(quantity6.multiply(price17).negate()), day0, true, false));
      costAdjustmentAssertList2.add(costAdjustmentAssertLineList21);
      List<CostAdjustmentAssert> costAdjustmentAssertLineList22 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList22.add(new CostAdjustmentAssert(transactionList1.get(0), "LC",
          quantity1.multiply(price1).add(quantity1.multiply(price18).negate()), day5, true, false));
      costAdjustmentAssertLineList22.add(new CostAdjustmentAssert(transactionList2.get(0), "LC",
          quantity2.multiply(price2).add(quantity2.multiply(price19).negate()), day5, true, false));
      costAdjustmentAssertLineList22.add(new CostAdjustmentAssert(transactionList1.get(2), "LC",
          quantity5.multiply(price1).add(quantity5.multiply(price18).negate()), day5, true, false));
      costAdjustmentAssertLineList22.add(new CostAdjustmentAssert(transactionList2.get(2), "LC",
          quantity6.multiply(price2).add(quantity6.multiply(price19).negate()), day5, true, false));
      costAdjustmentAssertList2.add(costAdjustmentAssertLineList22);
      List<CostAdjustmentAssert> costAdjustmentAssertLineList23 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList23.add(new CostAdjustmentAssert(transactionList2.get(1), "MCC",
          amount1, day6, true));
      costAdjustmentAssertList2.add(costAdjustmentAssertLineList23);
      assertCostAdjustment(costAdjustmentList2, costAdjustmentAssertList2);

      // Post cost adjustment 3 and assert it
      postDocument(costAdjustmentList2.get(2));
      List<DocumentPostAssert> documentPostAssertList1 = new ArrayList<DocumentPostAssert>();
      documentPostAssertList1.add(new DocumentPostAssert("61000", BigDecimal.ZERO, amount1, null));
      documentPostAssertList1.add(new DocumentPostAssert("35000", amount1, BigDecimal.ZERO, null));
      CostAdjustment costAdjustment1 = OBDal.getInstance().get(CostAdjustment.class,
          costAdjustmentList2.get(2).getId());
      assertDocumentPost(costAdjustment1, product2.getId(), documentPostAssertList1);

      OBDal.getInstance().commitAndClose();

    } catch (Exception e) {
      System.out.println(e.getMessage());
      throw new OBException(e);
    }

    finally {
      OBContext.restorePreviousMode();
    }
  }

  @Test
  public void testCostingLC100LC200() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final int day3 = 15;
    final BigDecimal price1 = new BigDecimal("150.00");
    final BigDecimal price2 = new BigDecimal("185.00");
    final BigDecimal price3 = new BigDecimal("535.00");
    final BigDecimal price4 = new BigDecimal("105.00");
    final BigDecimal price5 = new BigDecimal("151.8462");
    final BigDecimal price6 = new BigDecimal("187.2769");
    final BigDecimal quantity1 = new BigDecimal("100");
    final BigDecimal quantity2 = new BigDecimal("200");
    final BigDecimal quantity3 = BigDecimal.ONE;

    try {

      OBContext.setOBContext(USER_ID, ROLE_ID, CLIENT_ID, ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product1 = createProduct("testCostingLC100LC200A", price1);

      // Create a new product for the test
      Product product2 = createProduct("testCostingLC100LC200B", price2);

      // Create purchase order and book it
      Order purchaseOrder1 = createPurchaseOrder(product1, price1, quantity1, day0);

      // Create purchase order and book it
      Order purchaseOrder2 = createPurchaseOrder(product2, price2, quantity2, day0);

      // Create purchase invoice with landed cost, post it and assert it
      Invoice purchaseInvoiceLandedCost1 = createPurchaseInvoiceLandedCost(LANDEDCOSTTYPE1_ID,
          price3, quantity3, day1);

      // Create purchase invoice with landed cost, post it and assert it
      Invoice purchaseInvoiceLandedCost2 = createPurchaseInvoiceLandedCost(LANDEDCOSTTYPE2_ID,
          price4, quantity3, day2);

      // Create goods receipt, run costing background, post it and assert it
      List<Order> purchaseOrderList = new ArrayList<Order>();
      purchaseOrderList.add(purchaseOrder1);
      purchaseOrderList.add(purchaseOrder2);
      List<Invoice> invoiceList = new ArrayList<Invoice>();
      invoiceList.add(purchaseInvoiceLandedCost1);
      invoiceList.add(purchaseInvoiceLandedCost2);
      ShipmentInOut goodsReceipt = createGoodsReceipt(purchaseOrderList, null,
          quantity1.add(quantity2), day3, invoiceList);

      // Post landed cost and assert it
      List<ShipmentInOut> receiptList = new ArrayList<ShipmentInOut>();
      receiptList.add(goodsReceipt);
      postLandedCost(OBDal.getInstance().get(LandedCost.class,
          goodsReceipt.getLandedCostCostList().get(0).getLandedCost().getId()));

      // Assert product transactions 1
      List<ProductTransactionAssert> productTransactionAssertList1 = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList1.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price5, price1));
      assertProductTransaction(product1.getId(), productTransactionAssertList1);

      // Assert product transactions 2
      List<ProductTransactionAssert> productTransactionAssertList2 = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList2.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(1), price2, price6, price2));
      assertProductTransaction(product2.getId(), productTransactionAssertList2);

      // Assert product costing 1
      List<MaterialTransaction> transactionList1 = getProductTransactions(product1.getId());
      List<ProductCostingAssert> productCostingAssertList1 = new ArrayList<ProductCostingAssert>();
      productCostingAssertList1.add(new ProductCostingAssert(transactionList1.get(0), price1,
          price1, price5, quantity1));
      assertProductCosting(product1.getId(), productCostingAssertList1);

      // Assert product costing 2
      List<MaterialTransaction> transactionList2 = getProductTransactions(product2.getId());
      List<ProductCostingAssert> productCostingAssertList2 = new ArrayList<ProductCostingAssert>();
      productCostingAssertList2.add(new ProductCostingAssert(transactionList2.get(0), price2,
          price2, price6, quantity2));
      assertProductCosting(product2.getId(), productCostingAssertList2);

      // Assert cost adjustment 1
      List<CostAdjustment> costAdjustmentList1 = getCostAdjustment(product1.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList1 = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList11 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList11.add(new CostAdjustmentAssert(transactionList1.get(0), "LC",
          quantity1.multiply(price5).add(quantity1.multiply(price1).negate()), day0, true, false));
      costAdjustmentAssertLineList11.add(new CostAdjustmentAssert(transactionList2.get(0), "LC",
          quantity2.multiply(price6).add(quantity2.multiply(price2).negate()), day0, true, false));
      costAdjustmentAssertList1.add(costAdjustmentAssertLineList11);
      assertCostAdjustment(costAdjustmentList1, costAdjustmentAssertList1);

      // Assert cost adjustment 2
      List<CostAdjustment> costAdjustmentList2 = getCostAdjustment(product2.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList2 = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList21 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList21.add(new CostAdjustmentAssert(transactionList1.get(0), "LC",
          quantity1.multiply(price5).add(quantity1.multiply(price1).negate()), day0, true, false));
      costAdjustmentAssertLineList21.add(new CostAdjustmentAssert(transactionList2.get(0), "LC",
          quantity2.multiply(price6).add(quantity2.multiply(price2).negate()), day0, true, false));
      costAdjustmentAssertList2.add(costAdjustmentAssertLineList21);
      assertCostAdjustment(costAdjustmentList2, costAdjustmentAssertList2);

      OBDal.getInstance().commitAndClose();

    } catch (Exception e) {
      System.out.println(e.getMessage());
      throw new OBException(e);
    }

    finally {
      OBContext.restorePreviousMode();
    }
  }

  @Test
  public void testCostingLC101LC201() throws Exception {

    final int day0 = 0;
    final BigDecimal price1 = new BigDecimal("150.00");
    final BigDecimal price2 = new BigDecimal("185.00");
    final BigDecimal price3 = new BigDecimal("535.00");
    final BigDecimal price4 = new BigDecimal("105.00");
    final BigDecimal price5 = new BigDecimal("20.00");
    final BigDecimal price6 = new BigDecimal("151.8693");
    final BigDecimal price7 = new BigDecimal("187.30535");
    final BigDecimal price8 = new BigDecimal("149.9423");
    final BigDecimal price9 = new BigDecimal("184.92885");
    final BigDecimal price10 = new BigDecimal("148.1538");
    final BigDecimal price11 = new BigDecimal("182.7231");
    final BigDecimal quantity1 = new BigDecimal("100");
    final BigDecimal quantity2 = new BigDecimal("200");
    final BigDecimal quantity3 = BigDecimal.ONE;

    try {

      OBContext.setOBContext(USER_ID, ROLE_ID, CLIENT_ID, ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product1 = createProduct("testCostingLC101LC201A", price1);

      // Create a new product for the test
      Product product2 = createProduct("testCostingLC101LC201B", price2);

      // Create purchase order and book it
      Order purchaseOrder1 = createPurchaseOrder(product1, price1, quantity1, day0);

      // Create purchase order and book it
      Order purchaseOrder2 = createPurchaseOrder(product2, price2, quantity2, day0);

      // Create goods receipt, run costing background, post it and assert it
      List<Order> purchaseOrderList = new ArrayList<Order>();
      purchaseOrderList.add(purchaseOrder1);
      purchaseOrderList.add(purchaseOrder2);
      ShipmentInOut goodsReceipt = createGoodsReceipt(purchaseOrderList, null,
          quantity1.add(quantity2), day0);

      // Create Landed Cost
      List<String> landedCostTypeIdList = new ArrayList<String>();
      landedCostTypeIdList.add(LANDEDCOSTTYPE1_ID);
      landedCostTypeIdList.add(LANDEDCOSTTYPE2_ID);
      landedCostTypeIdList.add(LANDEDCOSTTYPE3_ID);
      List<BigDecimal> amountList = new ArrayList<BigDecimal>();
      amountList.add(quantity3.multiply(price3));
      amountList.add(quantity3.multiply(price4));
      amountList.add(quantity3.multiply(price5));
      List<ShipmentInOut> receiptList = new ArrayList<ShipmentInOut>();
      receiptList.add(goodsReceipt);
      createLandedCost(landedCostTypeIdList, amountList, receiptList, null, day0);

      // Assert product transactions 1
      List<ProductTransactionAssert> productTransactionAssertList1 = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList1.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price6, price1));
      assertProductTransaction(product1.getId(), productTransactionAssertList1);

      // Assert product transactions 2
      List<ProductTransactionAssert> productTransactionAssertList2 = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList2.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(1), price2, price7, price2));
      assertProductTransaction(product2.getId(), productTransactionAssertList2);

      // Assert product costing 1
      List<MaterialTransaction> transactionList1 = getProductTransactions(product1.getId());
      List<ProductCostingAssert> productCostingAssertList1 = new ArrayList<ProductCostingAssert>();
      productCostingAssertList1.add(new ProductCostingAssert(transactionList1.get(0), price1,
          price1, price6, quantity1));
      assertProductCosting(product1.getId(), productCostingAssertList1);

      // Assert product costing 2
      List<MaterialTransaction> transactionList2 = getProductTransactions(product2.getId());
      List<ProductCostingAssert> productCostingAssertList2 = new ArrayList<ProductCostingAssert>();
      productCostingAssertList2.add(new ProductCostingAssert(transactionList2.get(0), price2,
          price2, price7, quantity2));
      assertProductCosting(product2.getId(), productCostingAssertList2);

      // Assert cost adjustment 1
      List<CostAdjustment> costAdjustmentList1 = getCostAdjustment(product1.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList1 = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList11 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList11.add(new CostAdjustmentAssert(transactionList1.get(0),
          CURRENCY2_ID, "LC", quantity1.multiply(price1).add(quantity1.multiply(price8).negate()),
          day0, true, false));
      costAdjustmentAssertLineList11.add(new CostAdjustmentAssert(transactionList1.get(0),
          CURRENCY1_ID, "LC", quantity1.multiply(price1).add(quantity1.multiply(price10).negate()),
          day0, true, false));
      costAdjustmentAssertLineList11.add(new CostAdjustmentAssert(transactionList2.get(0),
          CURRENCY2_ID, "LC", quantity2.multiply(price2).add(quantity2.multiply(price9).negate()),
          day0, true, false));
      costAdjustmentAssertLineList11.add(new CostAdjustmentAssert(transactionList2.get(0),
          CURRENCY1_ID, "LC", quantity2.multiply(price2).add(quantity2.multiply(price11).negate()),
          day0, true, false));
      costAdjustmentAssertList1.add(costAdjustmentAssertLineList11);
      assertCostAdjustment(costAdjustmentList1, costAdjustmentAssertList1);

      // Assert cost adjustment 2
      List<CostAdjustment> costAdjustmentList2 = getCostAdjustment(product2.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList2 = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList21 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList21.add(new CostAdjustmentAssert(transactionList1.get(0),
          CURRENCY2_ID, "LC", quantity1.multiply(price1).add(quantity1.multiply(price8).negate()),
          day0, true, false));
      costAdjustmentAssertLineList21.add(new CostAdjustmentAssert(transactionList1.get(0),
          CURRENCY1_ID, "LC", quantity1.multiply(price1).add(quantity1.multiply(price10).negate()),
          day0, true, false));
      costAdjustmentAssertLineList21.add(new CostAdjustmentAssert(transactionList2.get(0),
          CURRENCY2_ID, "LC", quantity2.multiply(price2).add(quantity2.multiply(price9).negate()),
          day0, true, false));
      costAdjustmentAssertLineList21.add(new CostAdjustmentAssert(transactionList2.get(0),
          CURRENCY1_ID, "LC", quantity2.multiply(price2).add(quantity2.multiply(price11).negate()),
          day0, true, false));
      costAdjustmentAssertList2.add(costAdjustmentAssertLineList21);
      assertCostAdjustment(costAdjustmentList2, costAdjustmentAssertList2);

      OBDal.getInstance().commitAndClose();

    } catch (Exception e) {
      System.out.println(e.getMessage());
      throw new OBException(e);
    }

    finally {
      OBContext.restorePreviousMode();
    }
  }

  @Test
  public void testCostingLC400() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final int day3 = 15;
    final int day4 = 20;
    final int day5 = 25;
    final BigDecimal price1 = new BigDecimal("150.00");
    final BigDecimal price2 = new BigDecimal("400.00");
    final BigDecimal price3 = new BigDecimal("535.00");
    final BigDecimal price4 = new BigDecimal("105.00");
    final BigDecimal price5 = new BigDecimal("20.00");
    final BigDecimal price6 = new BigDecimal("600.00");
    final BigDecimal price7 = new BigDecimal("150.00");
    final BigDecimal price8 = new BigDecimal("25.00");
    final BigDecimal price9 = new BigDecimal("153.7377");
    final BigDecimal price10 = new BigDecimal("160.00");
    final BigDecimal price11 = new BigDecimal("163.9868");
    final BigDecimal quantity1 = new BigDecimal("50");
    final BigDecimal quantity2 = new BigDecimal("150");
    final BigDecimal quantity3 = BigDecimal.ONE;
    final BigDecimal amount1 = new BigDecimal("5.25");
    final BigDecimal amount2 = new BigDecimal("167.87");
    final BigDecimal amount3 = new BigDecimal("14.75");
    final BigDecimal amount4 = new BigDecimal("472.13");
    final BigDecimal amount5 = new BigDecimal("17.05");
    final BigDecimal amount6 = new BigDecimal("47.95");
    final BigDecimal amount7 = new BigDecimal("11.80");
    final BigDecimal amount8 = new BigDecimal("33.20");
    final BigDecimal amount9 = new BigDecimal("1.31");
    final BigDecimal amount10 = new BigDecimal("3.69");

    try {

      OBContext.setOBContext(USER_ID, ROLE_ID, CLIENT_ID, ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product1 = createProduct("testCostingLC400A", price1, CURRENCY1_ID);

      // Create a new product for the test
      Product product2 = createProduct("testCostingLC400B", price2, CURRENCY2_ID);

      // Create purchase order and book it
      Order purchaseOrder = createPurchaseOrder(product2, price2, quantity1, day0);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt1 = createGoodsReceipt(purchaseOrder, price2, quantity1, day1);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt2 = createGoodsReceipt(product1, price1, quantity2, day2);

      // Create Landed Cost
      List<String> landedCostTypeIdList = new ArrayList<String>();
      landedCostTypeIdList.add(LANDEDCOSTTYPE1_ID);
      landedCostTypeIdList.add(LANDEDCOSTTYPE2_ID);
      landedCostTypeIdList.add(LANDEDCOSTTYPE3_ID);
      List<BigDecimal> amountList = new ArrayList<BigDecimal>();
      amountList.add(quantity3.multiply(price3));
      amountList.add(quantity3.multiply(price4));
      amountList.add(quantity3.multiply(price5));
      List<ShipmentInOut> receiptList = new ArrayList<ShipmentInOut>();
      receiptList.add(goodsReceipt1);
      receiptList.add(goodsReceipt2);
      LandedCost landedCost = createLandedCost(landedCostTypeIdList, amountList, receiptList, null,
          day3);

      // Create purchase invoice with landed cost, post it and assert it
      Invoice purchaseInvoiceLandedCost1 = createPurchaseInvoiceLandedCost(LANDEDCOSTTYPE1_ID,
          price6, quantity3, day4);

      // Match invoice landed cost
      matchInvoiceLandedCost(purchaseInvoiceLandedCost1.getInvoiceLineList().get(0), landedCost
          .getLandedCostCostList().get(0), true);

      // Post landed cost cost and assert it
      postLandedCostLine(landedCost.getLandedCostCostList().get(0), purchaseInvoiceLandedCost1
          .getInvoiceLineList().get(0));

      // Create purchase invoice with landed cost, post it and assert it
      Invoice purchaseInvoiceLandedCost2 = createPurchaseInvoiceLandedCost(LANDEDCOSTTYPE2_ID,
          price7, quantity3, day4);

      // Match invoice landed cost
      matchInvoiceLandedCost(purchaseInvoiceLandedCost2.getInvoiceLineList().get(0), landedCost
          .getLandedCostCostList().get(1), true);

      // Post landed cost cost and assert it
      postLandedCostLine(landedCost.getLandedCostCostList().get(1), purchaseInvoiceLandedCost2
          .getInvoiceLineList().get(0));

      // Create purchase invoice with landed cost, post it and assert it
      Invoice purchaseInvoiceLandedCost3 = createPurchaseInvoiceLandedCost(LANDEDCOSTTYPE3_ID,
          price8, quantity3, day5);

      // Match invoice landed cost
      matchInvoiceLandedCost(purchaseInvoiceLandedCost3.getInvoiceLineList().get(0), landedCost
          .getLandedCostCostList().get(2), true);

      // Post landed cost cost and assert it
      postLandedCostLine(landedCost.getLandedCostCostList().get(2), purchaseInvoiceLandedCost3
          .getInvoiceLineList().get(0));

      // Assert product transactions 1
      List<ProductTransactionAssert> productTransactionAssertList1 = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList1.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt2.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price9, price1));
      assertProductTransaction(product1.getId(), productTransactionAssertList1);

      // Assert product transactions 2
      List<ProductTransactionAssert> productTransactionAssertList2 = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList2.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt1.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price10, price11, price10));
      assertProductTransaction(product2.getId(), productTransactionAssertList2);

      // Assert product costing 1
      List<MaterialTransaction> transactionList1 = getProductTransactions(product1.getId());
      List<ProductCostingAssert> productCostingAssertList1 = new ArrayList<ProductCostingAssert>();
      productCostingAssertList1.add(new ProductCostingAssert(transactionList1.get(0), price1,
          price1, price9, quantity2));
      assertProductCosting(product1.getId(), productCostingAssertList1);

      // Assert product costing 2
      List<MaterialTransaction> transactionList2 = getProductTransactions(product2.getId());
      List<ProductCostingAssert> productCostingAssertList2 = new ArrayList<ProductCostingAssert>();
      productCostingAssertList2.add(new ProductCostingAssert(transactionList2.get(0), price10,
          price10, price11, quantity1));
      assertProductCosting(product2.getId(), productCostingAssertList2);

      // Assert cost adjustment 1
      List<CostAdjustment> costAdjustmentList1 = getCostAdjustment(product1.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList1 = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList11 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList11.add(new CostAdjustmentAssert(transactionList2.get(0),
          CURRENCY2_ID, "LC", amount1, day3, true, false));
      costAdjustmentAssertLineList11.add(new CostAdjustmentAssert(transactionList2.get(0),
          CURRENCY1_ID, "LC", amount2, day3, true, false));
      costAdjustmentAssertLineList11.add(new CostAdjustmentAssert(transactionList1.get(0),
          CURRENCY2_ID, "LC", amount3, day3, true, false));
      costAdjustmentAssertLineList11.add(new CostAdjustmentAssert(transactionList1.get(0),
          CURRENCY1_ID, "LC", amount4, day3, true, false));
      costAdjustmentAssertList1.add(costAdjustmentAssertLineList11);
      List<CostAdjustmentAssert> costAdjustmentAssertLineList12 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList12.add(new CostAdjustmentAssert(transactionList2.get(0),
          CURRENCY1_ID, "LC", amount5, day3, true, false));
      costAdjustmentAssertLineList12.add(new CostAdjustmentAssert(transactionList1.get(0),
          CURRENCY1_ID, "LC", amount6, day3, true, false));
      costAdjustmentAssertList1.add(costAdjustmentAssertLineList12);
      List<CostAdjustmentAssert> costAdjustmentAssertLineList13 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList13.add(new CostAdjustmentAssert(transactionList2.get(0),
          CURRENCY1_ID, "LC", amount7, day3, true, false));
      costAdjustmentAssertLineList13.add(new CostAdjustmentAssert(transactionList1.get(0),
          CURRENCY1_ID, "LC", amount8, day3, true, false));
      costAdjustmentAssertList1.add(costAdjustmentAssertLineList13);
      List<CostAdjustmentAssert> costAdjustmentAssertLineList14 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList14.add(new CostAdjustmentAssert(transactionList2.get(0),
          CURRENCY2_ID, "LC", amount9, day3, true, false));
      costAdjustmentAssertLineList14.add(new CostAdjustmentAssert(transactionList1.get(0),
          CURRENCY2_ID, "LC", amount10, day3, true, false));
      costAdjustmentAssertList1.add(costAdjustmentAssertLineList14);
      assertCostAdjustment(costAdjustmentList1, costAdjustmentAssertList1);

      // Assert cost adjustment 2
      List<CostAdjustment> costAdjustmentList2 = getCostAdjustment(product2.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList2 = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList21 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList21.add(new CostAdjustmentAssert(transactionList2.get(0),
          CURRENCY2_ID, "LC", amount1, day3, true, false));
      costAdjustmentAssertLineList21.add(new CostAdjustmentAssert(transactionList2.get(0),
          CURRENCY1_ID, "LC", amount2, day3, true, false));
      costAdjustmentAssertLineList21.add(new CostAdjustmentAssert(transactionList1.get(0),
          CURRENCY2_ID, "LC", amount3, day3, true, false));
      costAdjustmentAssertLineList21.add(new CostAdjustmentAssert(transactionList1.get(0),
          CURRENCY1_ID, "LC", amount4, day3, true, false));
      costAdjustmentAssertList2.add(costAdjustmentAssertLineList21);
      List<CostAdjustmentAssert> costAdjustmentAssertLineList22 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList22.add(new CostAdjustmentAssert(transactionList2.get(0),
          CURRENCY1_ID, "LC", amount5, day3, true, false));
      costAdjustmentAssertLineList22.add(new CostAdjustmentAssert(transactionList1.get(0),
          CURRENCY1_ID, "LC", amount6, day3, true, false));
      costAdjustmentAssertList2.add(costAdjustmentAssertLineList22);
      List<CostAdjustmentAssert> costAdjustmentAssertLineList23 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList23.add(new CostAdjustmentAssert(transactionList2.get(0),
          CURRENCY1_ID, "LC", amount7, day3, true, false));
      costAdjustmentAssertLineList23.add(new CostAdjustmentAssert(transactionList1.get(0),
          CURRENCY1_ID, "LC", amount8, day3, true, false));
      costAdjustmentAssertList2.add(costAdjustmentAssertLineList23);
      List<CostAdjustmentAssert> costAdjustmentAssertLineList24 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList24.add(new CostAdjustmentAssert(transactionList2.get(0),
          CURRENCY2_ID, "LC", amount9, day3, true, false));
      costAdjustmentAssertLineList24.add(new CostAdjustmentAssert(transactionList1.get(0),
          CURRENCY2_ID, "LC", amount10, day3, true, false));
      costAdjustmentAssertList2.add(costAdjustmentAssertLineList24);
      assertCostAdjustment(costAdjustmentList2, costAdjustmentAssertList2);

      OBDal.getInstance().commitAndClose();

    } catch (Exception e) {
      System.out.println(e.getMessage());
      throw new OBException(e);
    }

    finally {
      OBContext.restorePreviousMode();
    }
  }

  @Test
  public void testCostingLC300() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final BigDecimal price1 = new BigDecimal("300.00");
    final BigDecimal price2 = new BigDecimal("27.50");
    final BigDecimal price3 = new BigDecimal("105.00");
    final BigDecimal price4 = new BigDecimal("326.50");
    final BigDecimal quantity1 = new BigDecimal("5");
    final BigDecimal quantity2 = BigDecimal.ONE;

    try {

      OBContext.setOBContext(USER_ID, ROLE_ID, CLIENT_ID, ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product = createProduct("testCostingLC300", price1);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt = createGoodsReceipt(product, price1, quantity1, day0);

      // Create Landed Cost
      List<String> landedCostTypeIdList = new ArrayList<String>();
      landedCostTypeIdList.add(LANDEDCOSTTYPE1_ID);
      landedCostTypeIdList.add(LANDEDCOSTTYPE2_ID);
      List<BigDecimal> amountList = new ArrayList<BigDecimal>();
      amountList.add(quantity2.multiply(price2));
      amountList.add(quantity2.multiply(price3));
      List<ShipmentInOut> receiptList = new ArrayList<ShipmentInOut>();
      receiptList.add(goodsReceipt);
      LandedCost landedCost = createLandedCost(landedCostTypeIdList, amountList, receiptList, null,
          day1);

      // Create purchase invoice with landed cost, post it and assert it
      Invoice purchaseInvoiceLandedCost1 = createPurchaseInvoiceLandedCost(LANDEDCOSTTYPE1_ID,
          price2, quantity2, day2);

      // Match invoice landed cost
      matchInvoiceLandedCost(purchaseInvoiceLandedCost1.getInvoiceLineList().get(0), landedCost
          .getLandedCostCostList().get(0), true);

      // Post landed cost cost and assert it
      postLandedCostLine(landedCost.getLandedCostCostList().get(0), purchaseInvoiceLandedCost1
          .getInvoiceLineList().get(0));

      // Create purchase invoice with landed cost, post it and assert it
      Invoice purchaseInvoiceLandedCost2 = createPurchaseInvoiceLandedCost(LANDEDCOSTTYPE2_ID,
          price3, quantity2, day2);

      // Match invoice landed cost
      matchInvoiceLandedCost(purchaseInvoiceLandedCost2.getInvoiceLineList().get(0), landedCost
          .getLandedCostCostList().get(1), true);

      // Post landed cost cost and assert it
      postLandedCostLine(landedCost.getLandedCostCostList().get(1), purchaseInvoiceLandedCost2
          .getInvoiceLineList().get(0));

      // Assert product transactions
      List<ProductTransactionAssert> productTransactionAssertList1 = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList1.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price4, price1));
      assertProductTransaction(product.getId(), productTransactionAssertList1);

      // Assert product costing
      List<MaterialTransaction> transactionList = getProductTransactions(product.getId());
      List<ProductCostingAssert> productCostingAssertList1 = new ArrayList<ProductCostingAssert>();
      productCostingAssertList1.add(new ProductCostingAssert(transactionList.get(0), price1,
          price1, price4, quantity1));
      assertProductCosting(product.getId(), productCostingAssertList1);

      // Assert cost adjustment
      List<CostAdjustment> costAdjustmentList = getCostAdjustment(product.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList1 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList1.add(new CostAdjustmentAssert(transactionList.get(0), "LC",
          quantity1.multiply(price4).add(quantity1.multiply(price1).negate()), day1, true, false));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList1);
      assertCostAdjustment(costAdjustmentList, costAdjustmentAssertList);

      OBDal.getInstance().commitAndClose();

    } catch (Exception e) {
      System.out.println(e.getMessage());
      throw new OBException(e);
    }

    finally {
      OBContext.restorePreviousMode();
    }
  }

  @Test
  public void testCostingLC500() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final BigDecimal price1 = new BigDecimal("500.00");
    final BigDecimal price2 = new BigDecimal("210.00");
    final BigDecimal price3 = new BigDecimal("300.00");
    final BigDecimal price4 = new BigDecimal("520.40");
    final BigDecimal quantity1 = new BigDecimal("25");
    final BigDecimal quantity2 = BigDecimal.ONE;

    try {

      OBContext.setOBContext(USER_ID, ROLE_ID, CLIENT_ID, ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product = createProduct("testCostingLC500", price1);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt = createGoodsReceipt(product, price1, quantity1, day0);

      // Create purchase invoice with landed cost, post it and assert it
      Invoice purchaseInvoiceLandedCost1 = createPurchaseInvoiceLandedCost(LANDEDCOSTTYPE2_ID,
          price2, quantity2, day1);

      // Create purchase invoice with landed cost, post it and assert it
      Invoice purchaseInvoiceLandedCost2 = createPurchaseInvoiceLandedCost(LANDEDCOSTTYPE1_ID,
          price3, quantity2, day1);

      // Create Landed Cost
      List<Invoice> invoiceList = new ArrayList<Invoice>();
      invoiceList.add(purchaseInvoiceLandedCost1);
      invoiceList.add(purchaseInvoiceLandedCost2);
      List<ShipmentInOut> receiptList = new ArrayList<ShipmentInOut>();
      receiptList.add(goodsReceipt);
      createLandedCost(invoiceList, receiptList, day2);

      // Assert product transactions
      List<ProductTransactionAssert> productTransactionAssertList1 = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList1.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price4, price1));
      assertProductTransaction(product.getId(), productTransactionAssertList1);

      // Assert product costing
      List<MaterialTransaction> transactionList = getProductTransactions(product.getId());
      List<ProductCostingAssert> productCostingAssertList1 = new ArrayList<ProductCostingAssert>();
      productCostingAssertList1.add(new ProductCostingAssert(transactionList.get(0), price1,
          price1, price4, quantity1));
      assertProductCosting(product.getId(), productCostingAssertList1);

      // Assert cost adjustment
      List<CostAdjustment> costAdjustmentList = getCostAdjustment(product.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList1 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList1.add(new CostAdjustmentAssert(transactionList.get(0), "LC",
          quantity1.multiply(price4).add(quantity1.multiply(price1).negate()), day2, true, false));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList1);
      assertCostAdjustment(costAdjustmentList, costAdjustmentAssertList);

      OBDal.getInstance().commitAndClose();

    } catch (Exception e) {
      System.out.println(e.getMessage());
      throw new OBException(e);
    }

    finally {
      OBContext.restorePreviousMode();
    }
  }

  @Test
  public void testCostingLC600() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final BigDecimal price1 = new BigDecimal("600.00");
    final BigDecimal price2 = new BigDecimal("315.00");
    final BigDecimal price3 = new BigDecimal("1110.00");
    final BigDecimal price4 = new BigDecimal("350.00");
    final BigDecimal price5 = new BigDecimal("1500.00");
    final BigDecimal price6 = new BigDecimal("643.1818");
    final BigDecimal quantity1 = new BigDecimal("33");
    final BigDecimal quantity2 = BigDecimal.ONE;

    try {

      OBContext.setOBContext(USER_ID, ROLE_ID, CLIENT_ID, ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product = createProduct("testCostingLC600", price1);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt = createGoodsReceipt(product, price1, quantity1, day0);

      // Create Landed Cost
      List<String> landedCostTypeIdList = new ArrayList<String>();
      landedCostTypeIdList.add(LANDEDCOSTTYPE2_ID);
      landedCostTypeIdList.add(LANDEDCOSTTYPE1_ID);
      List<BigDecimal> amountList = new ArrayList<BigDecimal>();
      amountList.add(quantity2.multiply(price2));
      amountList.add(quantity2.multiply(price3));
      List<ShipmentInOut> receiptList = new ArrayList<ShipmentInOut>();
      receiptList.add(goodsReceipt);
      LandedCost landedCost = createLandedCost(landedCostTypeIdList, amountList, receiptList, null,
          day1);

      // Create purchase invoice with landed cost, post it and assert it
      Invoice purchaseInvoiceLandedCost1 = createPurchaseInvoiceLandedCost(LANDEDCOSTTYPE2_ID,
          price4, quantity2, day2);

      // Match invoice landed cost
      matchInvoiceLandedCost(purchaseInvoiceLandedCost1.getInvoiceLineList().get(0), landedCost
          .getLandedCostCostList().get(0), false);

      // Post landed cost cost and assert it
      postLandedCostLine(landedCost.getLandedCostCostList().get(0), purchaseInvoiceLandedCost1
          .getInvoiceLineList().get(0));

      // Create purchase invoice with landed cost, post it and assert it
      Invoice purchaseInvoiceLandedCost2 = createPurchaseInvoiceLandedCost(LANDEDCOSTTYPE1_ID,
          price5, quantity2, day2);

      // Match invoice landed cost
      matchInvoiceLandedCost(purchaseInvoiceLandedCost2.getInvoiceLineList().get(0), landedCost
          .getLandedCostCostList().get(1), false);

      // Post landed cost cost and assert it
      postLandedCostLine(landedCost.getLandedCostCostList().get(1), purchaseInvoiceLandedCost2
          .getInvoiceLineList().get(0));

      // Assert product transactions
      List<ProductTransactionAssert> productTransactionAssertList1 = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList1.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price6, price1));
      assertProductTransaction(product.getId(), productTransactionAssertList1);

      // Assert product costing
      List<MaterialTransaction> transactionList = getProductTransactions(product.getId());
      List<ProductCostingAssert> productCostingAssertList1 = new ArrayList<ProductCostingAssert>();
      productCostingAssertList1.add(new ProductCostingAssert(transactionList.get(0), price1,
          price1, price6, quantity1));
      assertProductCosting(product.getId(), productCostingAssertList1);

      // Assert cost adjustment
      List<CostAdjustment> costAdjustmentList = getCostAdjustment(product.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList1 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList1.add(new CostAdjustmentAssert(transactionList.get(0), "LC",
          quantity1.multiply(price6).add(quantity1.multiply(price1).negate()), day1, true, false));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList1);
      assertCostAdjustment(costAdjustmentList, costAdjustmentAssertList);

      OBDal.getInstance().commitAndClose();

    } catch (Exception e) {
      System.out.println(e.getMessage());
      throw new OBException(e);
    }

    finally {
      OBContext.restorePreviousMode();
    }
  }

  @Test
  public void testCostingLC701() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final BigDecimal price1 = new BigDecimal("700.00");
    final BigDecimal price2 = new BigDecimal("315.00");
    final BigDecimal price3 = new BigDecimal("1110.00");
    final BigDecimal price4 = new BigDecimal("250.00");
    final BigDecimal price5 = new BigDecimal("1000.00");
    final BigDecimal price6 = new BigDecimal("757.00");
    final BigDecimal quantity1 = new BigDecimal("25");
    final BigDecimal quantity2 = BigDecimal.ONE;

    try {

      OBContext.setOBContext(USER_ID, ROLE_ID, CLIENT_ID, ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product = createProduct("testCostingLC701", price1);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt = createGoodsReceipt(product, price1, quantity1, day0);

      // Create Landed Cost
      List<String> landedCostTypeIdList = new ArrayList<String>();
      landedCostTypeIdList.add(LANDEDCOSTTYPE2_ID);
      landedCostTypeIdList.add(LANDEDCOSTTYPE1_ID);
      List<BigDecimal> amountList = new ArrayList<BigDecimal>();
      amountList.add(quantity2.multiply(price2));
      amountList.add(quantity2.multiply(price3));
      List<ShipmentInOut> receiptList = new ArrayList<ShipmentInOut>();
      receiptList.add(goodsReceipt);
      LandedCost landedCost = createLandedCost(landedCostTypeIdList, amountList, receiptList, null,
          day1);

      // Create purchase invoice with landed cost, post it and assert it
      Invoice purchaseInvoiceLandedCost1 = createPurchaseInvoiceLandedCost(LANDEDCOSTTYPE2_ID,
          price4, quantity2, day2);

      // Match invoice landed cost
      matchInvoiceLandedCost(purchaseInvoiceLandedCost1.getInvoiceLineList().get(0), landedCost
          .getLandedCostCostList().get(0), false);

      // Post landed cost cost and assert it
      postLandedCostLine(landedCost.getLandedCostCostList().get(0), purchaseInvoiceLandedCost1
          .getInvoiceLineList().get(0));

      // Create purchase invoice with landed cost, post it and assert it
      Invoice purchaseInvoiceLandedCost2 = createPurchaseInvoiceLandedCost(LANDEDCOSTTYPE1_ID,
          price5, quantity2, day2);

      // Match invoice landed cost
      matchInvoiceLandedCost(purchaseInvoiceLandedCost2.getInvoiceLineList().get(0), landedCost
          .getLandedCostCostList().get(1), false);

      // Post landed cost cost and assert it
      postLandedCostLine(landedCost.getLandedCostCostList().get(1), purchaseInvoiceLandedCost2
          .getInvoiceLineList().get(0));

      // Assert product transactions
      List<ProductTransactionAssert> productTransactionAssertList1 = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList1.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price6, price1));
      assertProductTransaction(product.getId(), productTransactionAssertList1);

      // Assert product costing
      List<MaterialTransaction> transactionList = getProductTransactions(product.getId());
      List<ProductCostingAssert> productCostingAssertList1 = new ArrayList<ProductCostingAssert>();
      productCostingAssertList1.add(new ProductCostingAssert(transactionList.get(0), price1,
          price1, price6, quantity1));
      assertProductCosting(product.getId(), productCostingAssertList1);

      // Assert cost adjustment
      List<CostAdjustment> costAdjustmentList = getCostAdjustment(product.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList1 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList1.add(new CostAdjustmentAssert(transactionList.get(0), "LC",
          quantity1.multiply(price6).add(quantity1.multiply(price1).negate()), day1, true, false));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList1);
      assertCostAdjustment(costAdjustmentList, costAdjustmentAssertList);

      OBDal.getInstance().commitAndClose();

    } catch (Exception e) {
      System.out.println(e.getMessage());
      throw new OBException(e);
    }

    finally {
      OBContext.restorePreviousMode();
    }
  }

  @Test
  public void testCostingLC801() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final BigDecimal price1 = new BigDecimal("800.00");
    final BigDecimal price2 = new BigDecimal("315.00");
    final BigDecimal price3 = new BigDecimal("1110.00");
    final BigDecimal price4 = new BigDecimal("250.00");
    final BigDecimal price5 = new BigDecimal("1000.00");
    final BigDecimal price6 = new BigDecimal("850.00");
    final BigDecimal quantity1 = new BigDecimal("25");
    final BigDecimal quantity2 = BigDecimal.ONE;
    final BigDecimal amount1 = new BigDecimal("1425.00");
    final BigDecimal amount2 = new BigDecimal("-65.00");
    final BigDecimal amount3 = new BigDecimal("-110.00");

    try {

      OBContext.setOBContext(USER_ID, ROLE_ID, CLIENT_ID, ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product = createProduct("testCostingLC801", price1);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt = createGoodsReceipt(product, price1, quantity1, day0);

      // Create Landed Cost
      List<String> landedCostTypeIdList = new ArrayList<String>();
      landedCostTypeIdList.add(LANDEDCOSTTYPE2_ID);
      landedCostTypeIdList.add(LANDEDCOSTTYPE1_ID);
      List<BigDecimal> amountList = new ArrayList<BigDecimal>();
      amountList.add(quantity2.multiply(price2));
      amountList.add(quantity2.multiply(price3));
      List<ShipmentInOut> receiptList = new ArrayList<ShipmentInOut>();
      receiptList.add(goodsReceipt);
      LandedCost landedCost = createLandedCost(landedCostTypeIdList, amountList, receiptList, null,
          day1);

      // Create purchase invoice with landed cost, post it and assert it
      Invoice purchaseInvoiceLandedCost1 = createPurchaseInvoiceLandedCost(LANDEDCOSTTYPE2_ID,
          price4, quantity2, day2);

      // Match invoice landed cost
      matchInvoiceLandedCost(purchaseInvoiceLandedCost1.getInvoiceLineList().get(0), landedCost
          .getLandedCostCostList().get(0), true);

      // Post landed cost cost and assert it
      postLandedCostLine(landedCost.getLandedCostCostList().get(0), purchaseInvoiceLandedCost1
          .getInvoiceLineList().get(0));

      // Create purchase invoice with landed cost, post it and assert it
      Invoice purchaseInvoiceLandedCost2 = createPurchaseInvoiceLandedCost(LANDEDCOSTTYPE1_ID,
          price5, quantity2, day2);

      // Match invoice landed cost
      matchInvoiceLandedCost(purchaseInvoiceLandedCost2.getInvoiceLineList().get(0), landedCost
          .getLandedCostCostList().get(1), true);

      // Post landed cost cost and assert it
      postLandedCostLine(landedCost.getLandedCostCostList().get(1), purchaseInvoiceLandedCost2
          .getInvoiceLineList().get(0));

      // Assert product transactions
      List<ProductTransactionAssert> productTransactionAssertList1 = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList1.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price6, price1));
      assertProductTransaction(product.getId(), productTransactionAssertList1);

      // Assert product costing
      List<MaterialTransaction> transactionList = getProductTransactions(product.getId());
      List<ProductCostingAssert> productCostingAssertList1 = new ArrayList<ProductCostingAssert>();
      productCostingAssertList1.add(new ProductCostingAssert(transactionList.get(0), price1,
          price1, price6, quantity1));
      assertProductCosting(product.getId(), productCostingAssertList1);

      // Assert cost adjustment
      List<CostAdjustment> costAdjustmentList = getCostAdjustment(product.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList1 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList1.add(new CostAdjustmentAssert(transactionList.get(0), "LC",
          amount1, day1, true, false));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList1);
      List<CostAdjustmentAssert> costAdjustmentAssertLineList2 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList2.add(new CostAdjustmentAssert(transactionList.get(0), "LC",
          amount2, day1, true, false));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList2);
      List<CostAdjustmentAssert> costAdjustmentAssertLineList3 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList3.add(new CostAdjustmentAssert(transactionList.get(0), "LC",
          amount3, day1, true, false));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList3);
      assertCostAdjustment(costAdjustmentList, costAdjustmentAssertList);

      OBDal.getInstance().commitAndClose();

    } catch (Exception e) {
      System.out.println(e.getMessage());
      throw new OBException(e);
    }

    finally {
      OBContext.restorePreviousMode();
    }
  }

  @Test
  public void testCostingLC900() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final BigDecimal price1 = new BigDecimal("900.00");
    final BigDecimal price2 = new BigDecimal("315.00");
    final BigDecimal price3 = new BigDecimal("1110.00");
    final BigDecimal price4 = new BigDecimal("250.00");
    final BigDecimal price5 = new BigDecimal("1000.00");
    final BigDecimal price6 = new BigDecimal("940.7143");
    final BigDecimal quantity1 = new BigDecimal("35");
    final BigDecimal quantity2 = BigDecimal.ONE;

    try {

      OBContext.setOBContext(USER_ID, ROLE_ID, CLIENT_ID, ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product = createProduct("testCostingLC900", price1);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt = createGoodsReceipt(product, price1, quantity1, day0);

      // Create Landed Cost
      List<String> landedCostTypeIdList = new ArrayList<String>();
      landedCostTypeIdList.add(LANDEDCOSTTYPE2_ID);
      landedCostTypeIdList.add(LANDEDCOSTTYPE1_ID);
      List<BigDecimal> amountList = new ArrayList<BigDecimal>();
      amountList.add(quantity2.multiply(price2));
      amountList.add(quantity2.multiply(price3));
      List<ShipmentInOut> receiptList = new ArrayList<ShipmentInOut>();
      receiptList.add(goodsReceipt);
      LandedCost landedCost = createLandedCost(landedCostTypeIdList, amountList, receiptList, null,
          day1);

      // Create purchase invoice with landed cost, post it and assert it
      createPurchaseInvoiceLandedCost(LANDEDCOSTTYPE2_ID, price4, quantity2, day2);

      // Create purchase invoice with landed cost, post it and assert it
      createPurchaseInvoiceLandedCost(LANDEDCOSTTYPE1_ID, price5, quantity2, day2);

      // Match invoice landed cost
      matchInvoiceLandedCost(landedCost.getLandedCostCostList().get(0), true,
          "The Landed Cost Cost does not have any matching available.");

      // Match invoice landed cost
      matchInvoiceLandedCost(landedCost.getLandedCostCostList().get(1), true,
          "The Landed Cost Cost does not have any matching available.");

      // Assert product transactions
      List<ProductTransactionAssert> productTransactionAssertList1 = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList1.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price6, price1));
      assertProductTransaction(product.getId(), productTransactionAssertList1);

      // Assert product costing
      List<MaterialTransaction> transactionList = getProductTransactions(product.getId());
      List<ProductCostingAssert> productCostingAssertList1 = new ArrayList<ProductCostingAssert>();
      productCostingAssertList1.add(new ProductCostingAssert(transactionList.get(0), price1,
          price1, price6, quantity1));
      assertProductCosting(product.getId(), productCostingAssertList1);

      // Assert cost adjustment
      List<CostAdjustment> costAdjustmentList = getCostAdjustment(product.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList1 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList1.add(new CostAdjustmentAssert(transactionList.get(0), "LC",
          quantity1.multiply(price6).add(quantity1.multiply(price1).negate()), day1, true, false));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList1);
      assertCostAdjustment(costAdjustmentList, costAdjustmentAssertList);

      OBDal.getInstance().commitAndClose();

    } catch (Exception e) {
      System.out.println(e.getMessage());
      throw new OBException(e);
    }

    finally {
      OBContext.restorePreviousMode();
    }
  }

  @Test
  public void testCostingLC1000() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int year = -1;
    final BigDecimal price1 = new BigDecimal("1000.00");
    final BigDecimal price2 = new BigDecimal("650.00");
    final BigDecimal price3 = new BigDecimal("500.00");
    final BigDecimal price4 = new BigDecimal("1022.2222");
    final BigDecimal quantity1 = new BigDecimal("45");
    final BigDecimal quantity2 = new BigDecimal("7");
    final BigDecimal quantity3 = BigDecimal.ONE;
    final String productType = "S";
    final String costType = "STA";

    try {

      OBContext.setOBContext(USER_ID, ROLE_ID, CLIENT_ID, ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product1 = createProduct("testCostingLC1000A", price1);

      // Create a new product for the test
      Product product2 = createProduct("testCostingLC1000B", productType, price2, price3, costType,
          year);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt1 = createGoodsReceipt(product1, price1, quantity1, day0);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt2 = createGoodsReceipt(product2, price3, quantity2, day0);

      // Create Landed Cost
      List<String> landedCostTypeIdList = new ArrayList<String>();
      landedCostTypeIdList.add(LANDEDCOSTTYPE1_ID);
      List<BigDecimal> amountList = new ArrayList<BigDecimal>();
      amountList.add(quantity3.multiply(price1));
      List<ShipmentInOut> receiptList = new ArrayList<ShipmentInOut>();
      receiptList.add(goodsReceipt1);
      receiptList.add(goodsReceipt2);
      createLandedCost(landedCostTypeIdList, amountList, receiptList, null, day1);

      // Assert product transactions 1
      List<ProductTransactionAssert> productTransactionAssertList1 = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList1.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt1.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price4, price1));
      assertProductTransaction(product1.getId(), productTransactionAssertList1);

      // Assert product transactions 2
      assertTrue(getProductTransactions(product2.getId()).isEmpty());

      // Assert product costing 1
      List<MaterialTransaction> transactionList1 = getProductTransactions(product1.getId());
      List<ProductCostingAssert> productCostingAssertList1 = new ArrayList<ProductCostingAssert>();
      productCostingAssertList1.add(new ProductCostingAssert(transactionList1.get(0), price1,
          price1, price4, quantity1));
      assertProductCosting(product1.getId(), productCostingAssertList1);

      // Assert product costing 2
      List<ProductCostingAssert> productCostingAssertList2 = new ArrayList<ProductCostingAssert>();
      productCostingAssertList2.add(new ProductCostingAssert(null, null, null, price3, null,
          costType, year));
      assertProductCosting(product2.getId(), productCostingAssertList2);

      // Assert cost adjustment 1
      List<CostAdjustment> costAdjustmentList1 = getCostAdjustment(product1.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList1 = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList11 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList11.add(new CostAdjustmentAssert(transactionList1.get(0), "LC",
          quantity1.multiply(price4).add(quantity1.multiply(price1).negate()), day1, true, false));
      costAdjustmentAssertList1.add(costAdjustmentAssertLineList11);
      assertCostAdjustment(costAdjustmentList1, costAdjustmentAssertList1);

      // Assert cost adjustment 2
      assertEquals(getCostAdjustment(product2.getId()), null);

      OBDal.getInstance().commitAndClose();

    } catch (Exception e) {
      System.out.println(e.getMessage());
      throw new OBException(e);
    }

    finally {
      OBContext.restorePreviousMode();
    }
  }

  @Test
  public void testCostingLC802() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final BigDecimal price1 = new BigDecimal("800.00");
    final BigDecimal price2 = new BigDecimal("315.00");
    final BigDecimal price3 = new BigDecimal("1110.00");
    final BigDecimal price4 = new BigDecimal("250.00");
    final BigDecimal price5 = new BigDecimal("1000.00");
    final BigDecimal price6 = new BigDecimal("850.00");
    final BigDecimal quantity1 = new BigDecimal("25");
    final BigDecimal quantity2 = BigDecimal.ONE;
    final BigDecimal amount1 = new BigDecimal("1425.00");
    final BigDecimal amount2 = new BigDecimal("-65.00");
    final BigDecimal amount3 = new BigDecimal("-110.00");

    try {

      OBContext.setOBContext(USER_ID, ROLE_ID, CLIENT_ID, ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product = createProduct("testCostingLC802", price1);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt = createGoodsReceipt(product, price1, quantity1, day0);

      // Create Landed Cost
      List<String> landedCostTypeIdList = new ArrayList<String>();
      landedCostTypeIdList.add(LANDEDCOSTTYPE2_ID);
      landedCostTypeIdList.add(LANDEDCOSTTYPE1_ID);
      List<BigDecimal> amountList = new ArrayList<BigDecimal>();
      amountList.add(quantity2.multiply(price2));
      amountList.add(quantity2.multiply(price3));
      List<ShipmentInOut> receiptList = new ArrayList<ShipmentInOut>();
      receiptList.add(goodsReceipt);
      LandedCost landedCost = createLandedCost(landedCostTypeIdList, amountList, receiptList, null,
          day1);

      // Create purchase invoice with landed cost, post it and assert it
      Invoice purchaseInvoiceLandedCost1 = createPurchaseInvoiceLandedCost(LANDEDCOSTTYPE2_ID,
          price4, quantity2, day2);

      // Match invoice landed cost
      matchInvoiceLandedCost(purchaseInvoiceLandedCost1.getInvoiceLineList().get(0), landedCost
          .getLandedCostCostList().get(0), true);

      // Post landed cost cost and assert it
      postLandedCostLine(landedCost.getLandedCostCostList().get(0), purchaseInvoiceLandedCost1
          .getInvoiceLineList().get(0));

      // Create purchase invoice with landed cost, post it and assert it
      Invoice purchaseInvoiceLandedCost2 = createPurchaseInvoiceLandedCost(LANDEDCOSTTYPE1_ID,
          price5, quantity2, day2);

      // Match invoice landed cost
      matchInvoiceLandedCost(purchaseInvoiceLandedCost2.getInvoiceLineList().get(0), landedCost
          .getLandedCostCostList().get(1), true);

      // Post landed cost cost and assert it
      postLandedCostLine(landedCost.getLandedCostCostList().get(1), purchaseInvoiceLandedCost2
          .getInvoiceLineList().get(0));

      // Assert product transactions
      List<ProductTransactionAssert> productTransactionAssertList1 = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList1.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price6, price1));
      assertProductTransaction(product.getId(), productTransactionAssertList1);

      // Assert product costing
      List<MaterialTransaction> transactionList = getProductTransactions(product.getId());
      List<ProductCostingAssert> productCostingAssertList1 = new ArrayList<ProductCostingAssert>();
      productCostingAssertList1.add(new ProductCostingAssert(transactionList.get(0), price1,
          price1, price6, quantity1));
      assertProductCosting(product.getId(), productCostingAssertList1);

      // Assert cost adjustment
      List<CostAdjustment> costAdjustmentList = getCostAdjustment(product.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList1 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList1.add(new CostAdjustmentAssert(transactionList.get(0), "LC",
          amount1, day1, true, false));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList1);
      List<CostAdjustmentAssert> costAdjustmentAssertLineList2 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList2.add(new CostAdjustmentAssert(transactionList.get(0), "LC",
          amount2, day1, true, false));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList2);
      List<CostAdjustmentAssert> costAdjustmentAssertLineList3 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList3.add(new CostAdjustmentAssert(transactionList.get(0), "LC",
          amount3, day1, true, false));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList3);
      assertCostAdjustment(costAdjustmentList, costAdjustmentAssertList);

      // Reactivate landed cost
      reactivateLandedCost(landedCost.getId(), "This document is posted");

      // Unpost landed cost
      unpostDocument(landedCost);

      // Reactivate landed cost
      reactivateLandedCost(landedCost.getId(), "This document is posted: tab Cost - line 10");

      // Unpost landed cost cost
      unpostDocument(landedCost.getLandedCostCostList().get(0));

      // Reactivate landed cost
      reactivateLandedCost(landedCost.getId(), "This document is posted: tab Cost - line 20");

      // Unpost landed cost cost
      unpostDocument(landedCost.getLandedCostCostList().get(1));

      // Reactivate landed cost
      reactivateLandedCost(landedCost.getId(), null);

      // Assert product transactions
      List<ProductTransactionAssert> productTransactionAssertList2 = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList2.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price1, price1));
      assertProductTransaction(product.getId(), productTransactionAssertList2);

      // Assert product costing
      List<MaterialTransaction> transactionList2 = getProductTransactions(product.getId());
      List<ProductCostingAssert> productCostingAssertList2 = new ArrayList<ProductCostingAssert>();
      productCostingAssertList2.add(new ProductCostingAssert(transactionList2.get(0), price1,
          price1, price1, quantity1));
      assertProductCosting(product.getId(), productCostingAssertList2);

      // Assert cost adjustment
      List<CostAdjustment> costAdjustmentList2 = getCostAdjustment(product.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList2 = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList21 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList21.add(new CostAdjustmentAssert(transactionList2.get(0), "LC",
          amount1, day1, true, false, "VO"));
      costAdjustmentAssertList2.add(costAdjustmentAssertLineList21);
      List<CostAdjustmentAssert> costAdjustmentAssertLineList22 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList22.add(new CostAdjustmentAssert(transactionList2.get(0), "LC",
          amount2, day1, true, false, "VO"));
      costAdjustmentAssertList2.add(costAdjustmentAssertLineList22);
      List<CostAdjustmentAssert> costAdjustmentAssertLineList23 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList23.add(new CostAdjustmentAssert(transactionList2.get(0), "LC",
          amount3, day1, true, false, "VO"));
      costAdjustmentAssertList2.add(costAdjustmentAssertLineList23);
      List<CostAdjustmentAssert> costAdjustmentAssertLineList24 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList24.add(new CostAdjustmentAssert(transactionList2.get(0), "LC",
          amount1.negate(), day1, true, false, "VO"));
      costAdjustmentAssertList2.add(costAdjustmentAssertLineList24);
      List<CostAdjustmentAssert> costAdjustmentAssertLineList25 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList25.add(new CostAdjustmentAssert(transactionList2.get(0), "LC",
          amount2.negate(), day1, true, false, "VO"));
      costAdjustmentAssertList2.add(costAdjustmentAssertLineList25);
      List<CostAdjustmentAssert> costAdjustmentAssertLineList26 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList26.add(new CostAdjustmentAssert(transactionList2.get(0), "LC",
          amount3.negate(), day1, true, false, "VO"));
      costAdjustmentAssertList2.add(costAdjustmentAssertLineList26);
      assertCostAdjustment(costAdjustmentList2, costAdjustmentAssertList2);

      OBDal.getInstance().commitAndClose();

    } catch (Exception e) {
      System.out.println(e.getMessage());
      throw new OBException(e);
    }

    finally {
      OBContext.restorePreviousMode();
    }
  }

  @Test
  public void testCostingLC702() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final BigDecimal price1 = new BigDecimal("700.00");
    final BigDecimal price2 = new BigDecimal("315.00");
    final BigDecimal price3 = new BigDecimal("1110.00");
    final BigDecimal price4 = new BigDecimal("250.00");
    final BigDecimal price5 = new BigDecimal("1000.00");
    final BigDecimal price6 = new BigDecimal("757.00");
    final BigDecimal price7 = new BigDecimal("750.00");
    final BigDecimal quantity1 = new BigDecimal("25");
    final BigDecimal quantity2 = BigDecimal.ONE;
    final BigDecimal amount1 = new BigDecimal("-65.00");
    final BigDecimal amount2 = new BigDecimal("-110.00");

    try {

      OBContext.setOBContext(USER_ID, ROLE_ID, CLIENT_ID, ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product = createProduct("testCostingLC702", price1);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt = createGoodsReceipt(product, price1, quantity1, day0);

      // Create Landed Cost
      List<String> landedCostTypeIdList = new ArrayList<String>();
      landedCostTypeIdList.add(LANDEDCOSTTYPE2_ID);
      landedCostTypeIdList.add(LANDEDCOSTTYPE1_ID);
      List<BigDecimal> amountList = new ArrayList<BigDecimal>();
      amountList.add(quantity2.multiply(price2));
      amountList.add(quantity2.multiply(price3));
      List<ShipmentInOut> receiptList = new ArrayList<ShipmentInOut>();
      receiptList.add(goodsReceipt);
      LandedCost landedCost = createLandedCost(landedCostTypeIdList, amountList, receiptList, null,
          day1);

      // Create purchase invoice with landed cost, post it and assert it
      Invoice purchaseInvoiceLandedCost1 = createPurchaseInvoiceLandedCost(LANDEDCOSTTYPE2_ID,
          price4, quantity2, day2);

      // Match invoice landed cost
      matchInvoiceLandedCost(purchaseInvoiceLandedCost1.getInvoiceLineList().get(0), landedCost
          .getLandedCostCostList().get(0), false);

      // Post landed cost cost and assert it
      postLandedCostLine(landedCost.getLandedCostCostList().get(0), purchaseInvoiceLandedCost1
          .getInvoiceLineList().get(0));

      // Create purchase invoice with landed cost, post it and assert it
      Invoice purchaseInvoiceLandedCost2 = createPurchaseInvoiceLandedCost(LANDEDCOSTTYPE1_ID,
          price5, quantity2, day2);

      // Match invoice landed cost
      matchInvoiceLandedCost(purchaseInvoiceLandedCost2.getInvoiceLineList().get(0), landedCost
          .getLandedCostCostList().get(1), false);

      // Post landed cost cost and assert it
      postLandedCostLine(landedCost.getLandedCostCostList().get(1), purchaseInvoiceLandedCost2
          .getInvoiceLineList().get(0));

      // Assert product transactions
      List<ProductTransactionAssert> productTransactionAssertList1 = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList1.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price6, price1));
      assertProductTransaction(product.getId(), productTransactionAssertList1);

      // Assert product costing
      List<MaterialTransaction> transactionList = getProductTransactions(product.getId());
      List<ProductCostingAssert> productCostingAssertList1 = new ArrayList<ProductCostingAssert>();
      productCostingAssertList1.add(new ProductCostingAssert(transactionList.get(0), price1,
          price1, price6, quantity1));
      assertProductCosting(product.getId(), productCostingAssertList1);

      // Assert cost adjustment
      List<CostAdjustment> costAdjustmentList = getCostAdjustment(product.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList1 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList1.add(new CostAdjustmentAssert(transactionList.get(0), "LC",
          quantity1.multiply(price6).add(quantity1.multiply(price1).negate()), day1, true, false));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList1);
      assertCostAdjustment(costAdjustmentList, costAdjustmentAssertList);

      // Cancel landed cost cost
      cancelLandedCostCost(landedCost.getLandedCostCostList().get(0).getId(),
          "This document is posted");

      // Unpost landed cost cost
      unpostDocument(landedCost.getLandedCostCostList().get(0));

      // Cancel landed cost cost
      cancelLandedCostCost(landedCost.getLandedCostCostList().get(0).getId(), null);

      // Cancel landed cost cost
      cancelLandedCostCost(landedCost.getLandedCostCostList().get(1).getId(),
          "This document is posted");

      // Unpost landed cost cost
      unpostDocument(landedCost.getLandedCostCostList().get(1));

      // Cancel landed cost cost
      cancelLandedCostCost(landedCost.getLandedCostCostList().get(1).getId(), null);

      // Match invoice landed cost
      matchInvoiceLandedCost(
          purchaseInvoiceLandedCost1.getInvoiceLineList().get(0),
          landedCost.getLandedCostCostList().get(0),
          OBDal.getInstance()
              .get(LandedCostCost.class, landedCost.getLandedCostCostList().get(0).getId())
              .getLandedCostMatchedList().get(0), true);

      // Match invoice landed cost
      matchInvoiceLandedCost(
          purchaseInvoiceLandedCost2.getInvoiceLineList().get(0),
          landedCost.getLandedCostCostList().get(1),
          OBDal.getInstance()
              .get(LandedCostCost.class, landedCost.getLandedCostCostList().get(1).getId())
              .getLandedCostMatchedList().get(0), true);

      // Assert product transactions
      List<ProductTransactionAssert> productTransactionAssertList2 = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList2.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price7, price1));
      assertProductTransaction(product.getId(), productTransactionAssertList2);

      // Assert product costing
      List<MaterialTransaction> transactionList2 = getProductTransactions(product.getId());
      List<ProductCostingAssert> productCostingAssertList2 = new ArrayList<ProductCostingAssert>();
      productCostingAssertList2.add(new ProductCostingAssert(transactionList2.get(0), price1,
          price1, price7, quantity1));
      assertProductCosting(product.getId(), productCostingAssertList2);

      // Assert cost adjustment
      List<CostAdjustment> costAdjustmentList2 = getCostAdjustment(product.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList2 = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList21 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList21.add(new CostAdjustmentAssert(transactionList2.get(0), "LC",
          quantity1.multiply(price6).add(quantity1.multiply(price1).negate()), day1, true, false));
      costAdjustmentAssertList2.add(costAdjustmentAssertLineList21);
      List<CostAdjustmentAssert> costAdjustmentAssertLineList22 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList22.add(new CostAdjustmentAssert(transactionList2.get(0), "LC",
          amount1, day1, true, false));
      costAdjustmentAssertList2.add(costAdjustmentAssertLineList22);
      List<CostAdjustmentAssert> costAdjustmentAssertLineList23 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList23.add(new CostAdjustmentAssert(transactionList2.get(0), "LC",
          amount2, day1, true, false));
      costAdjustmentAssertList2.add(costAdjustmentAssertLineList23);
      assertCostAdjustment(costAdjustmentList2, costAdjustmentAssertList2);

      OBDal.getInstance().commitAndClose();

    } catch (Exception e) {
      System.out.println(e.getMessage());
      throw new OBException(e);
    }

    finally {
      OBContext.restorePreviousMode();
    }
  }

  @Test
  public void testCostingLC1111() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final int day3 = 15;
    final BigDecimal price1 = new BigDecimal("111.00");
    final BigDecimal price2 = new BigDecimal("222.00");
    final BigDecimal price3 = new BigDecimal("333.00");
    final BigDecimal price4 = new BigDecimal("325.00");
    final BigDecimal price5 = new BigDecimal("425.00");
    final BigDecimal price6 = new BigDecimal("113.1428");
    final BigDecimal price7 = new BigDecimal("111.9740");
    final BigDecimal price8 = new BigDecimal("226.2857");
    final BigDecimal price9 = new BigDecimal("224.8572");
    final BigDecimal quantity1 = new BigDecimal("50");
    final BigDecimal quantity2 = new BigDecimal("150");
    final BigDecimal quantity3 = new BigDecimal("75");
    final BigDecimal quantity4 = new BigDecimal("125");
    final BigDecimal quantity5 = new BigDecimal("80");
    final BigDecimal quantity6 = new BigDecimal("60");
    final BigDecimal quantity7 = BigDecimal.ONE;

    try {

      OBContext.setOBContext(USER_ID, ROLE_ID, CLIENT_ID, ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product1 = createProduct("testCostingLC1111A", price1);

      // Create a new product for the test
      Product product2 = createProduct("testCostingLC1111B", price2);

      // Create a new product for the test
      Product product3 = createProduct("testCostingLC1111C", price3);

      // Create purchase order and book it
      Order purchaseOrder1 = createPurchaseOrder(product1, price1, quantity1, day0);

      // Create purchase order and book it
      Order purchaseOrder2 = createPurchaseOrder(product2, price2, quantity2, day0);

      // Create purchase order and book it
      Order purchaseOrder3 = createPurchaseOrder(product2, price2, quantity3, day0);

      // Create purchase order and book it
      Order purchaseOrder4 = createPurchaseOrder(product3, price3, quantity4, day0);

      // Create purchase order and book it
      Order purchaseOrder5 = createPurchaseOrder(product3, price3, quantity5, day0);

      // Create purchase order and book it
      Order purchaseOrder6 = createPurchaseOrder(product1, price1, quantity6, day0);

      // Create goods receipt, run costing background, post it and assert it
      List<Order> purchaseOrderList1 = new ArrayList<Order>();
      purchaseOrderList1.add(purchaseOrder1);
      purchaseOrderList1.add(purchaseOrder2);
      ShipmentInOut goodsReceipt1 = createGoodsReceipt(purchaseOrderList1, null,
          quantity1.add(quantity2), day0);

      // Create goods receipt, run costing background, post it and assert it
      List<Order> purchaseOrderList2 = new ArrayList<Order>();
      purchaseOrderList2.add(purchaseOrder3);
      purchaseOrderList2.add(purchaseOrder4);
      ShipmentInOut goodsReceipt2 = createGoodsReceipt(purchaseOrderList2, null,
          quantity3.add(quantity4), day1);

      // Create goods receipt, run costing background, post it and assert it
      List<Order> purchaseOrderList3 = new ArrayList<Order>();
      purchaseOrderList3.add(purchaseOrder5);
      purchaseOrderList3.add(purchaseOrder6);
      ShipmentInOut goodsReceipt3 = createGoodsReceipt(purchaseOrderList3, null,
          quantity5.add(quantity6), day2);

      // Create Landed Cost
      List<String> landedCostTypeIdList = new ArrayList<String>();
      landedCostTypeIdList.add(LANDEDCOSTTYPE2_ID);
      landedCostTypeIdList.add(LANDEDCOSTTYPE1_ID);
      List<BigDecimal> amountList = new ArrayList<BigDecimal>();
      amountList.add(quantity7.multiply(price4));
      amountList.add(quantity7.multiply(price5));
      List<ShipmentInOut> receiptList = new ArrayList<ShipmentInOut>();
      receiptList.add(goodsReceipt1);
      createLandedCost(landedCostTypeIdList, amountList, receiptList, null, day3);

      // Assert product transactions 1
      List<ProductTransactionAssert> productTransactionAssertList1 = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList1.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt1.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price6, price1));
      productTransactionAssertList1.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt3.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(1), price1, price1));
      assertProductTransaction(product1.getId(), productTransactionAssertList1);

      // Assert product transactions 2
      List<ProductTransactionAssert> productTransactionAssertList2 = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList2.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt1.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(1), price2, price8, price2));
      productTransactionAssertList2.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt2.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price2, price2));
      assertProductTransaction(product2.getId(), productTransactionAssertList2);

      // Assert product transactions 3
      List<ProductTransactionAssert> productTransactionAssertList3 = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList3.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt2.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(1), price3, price3));
      productTransactionAssertList3.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt3.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price3, price3));
      assertProductTransaction(product3.getId(), productTransactionAssertList3);

      // Assert product costing 1
      List<MaterialTransaction> transactionList1 = getProductTransactions(product1.getId());
      List<ProductCostingAssert> productCostingAssertList1 = new ArrayList<ProductCostingAssert>();
      productCostingAssertList1.add(new ProductCostingAssert(transactionList1.get(0), price1,
          price1, price6, quantity1));
      productCostingAssertList1.add(new ProductCostingAssert(transactionList1.get(1), price1,
          price1, price7, quantity1.add(quantity6)));
      assertProductCosting(product1.getId(), productCostingAssertList1);

      // Assert product costing 2
      List<MaterialTransaction> transactionList2 = getProductTransactions(product2.getId());
      List<ProductCostingAssert> productCostingAssertList2 = new ArrayList<ProductCostingAssert>();
      productCostingAssertList2.add(new ProductCostingAssert(transactionList2.get(0), price2,
          price2, price8, quantity2));
      productCostingAssertList2.add(new ProductCostingAssert(transactionList2.get(1), price2,
          price2, price9, quantity2.add(quantity3)));
      assertProductCosting(product2.getId(), productCostingAssertList2);

      // Assert product costing 3
      List<MaterialTransaction> transactionList3 = getProductTransactions(product3.getId());
      List<ProductCostingAssert> productCostingAssertList3 = new ArrayList<ProductCostingAssert>();
      productCostingAssertList3.add(new ProductCostingAssert(transactionList3.get(0), price3, null,
          price3, quantity4));
      productCostingAssertList3.add(new ProductCostingAssert(transactionList3.get(1), price3, null,
          price3, quantity4.add(quantity5)));
      assertProductCosting(product3.getId(), productCostingAssertList3);

      // Assert cost adjustment 1
      List<CostAdjustment> costAdjustmentList1 = getCostAdjustment(product1.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList1 = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList11 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList11.add(new CostAdjustmentAssert(transactionList1.get(0), "LC",
          quantity1.multiply(price6).add(quantity1.multiply(price1).negate()), day3, true, false));
      costAdjustmentAssertLineList11.add(new CostAdjustmentAssert(transactionList2.get(0), "LC",
          quantity2.multiply(price8).add(quantity2.multiply(price2).negate()), day3, true, false));
      costAdjustmentAssertList1.add(costAdjustmentAssertLineList11);
      assertCostAdjustment(costAdjustmentList1, costAdjustmentAssertList1);

      // Assert cost adjustment 2
      List<CostAdjustment> costAdjustmentList2 = getCostAdjustment(product2.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList2 = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList12 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList12.add(new CostAdjustmentAssert(transactionList1.get(0), "LC",
          quantity1.multiply(price6).add(quantity1.multiply(price1).negate()), day3, true, false));
      costAdjustmentAssertLineList12.add(new CostAdjustmentAssert(transactionList2.get(0), "LC",
          quantity2.multiply(price8).add(quantity2.multiply(price2).negate()), day3, true, false));
      costAdjustmentAssertList2.add(costAdjustmentAssertLineList12);
      assertCostAdjustment(costAdjustmentList2, costAdjustmentAssertList2);

      // Assert cost adjustment 3
      assertEquals(getCostAdjustment(product3.getId()), null);

      OBDal.getInstance().commitAndClose();

    } catch (Exception e) {
      System.out.println(e.getMessage());
      throw new OBException(e);
    }

    finally {
      OBContext.restorePreviousMode();
    }
  }

  @Test
  public void testCostingLC1112() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final int day3 = 15;
    final BigDecimal price1 = new BigDecimal("111.00");
    final BigDecimal price2 = new BigDecimal("222.00");
    final BigDecimal price3 = new BigDecimal("333.00");
    final BigDecimal price4 = new BigDecimal("325.00");
    final BigDecimal price5 = new BigDecimal("425.00");
    final BigDecimal price6 = new BigDecimal("113.1428");
    final BigDecimal price7 = new BigDecimal("111.9740");
    final BigDecimal price8 = new BigDecimal("226.2857");
    final BigDecimal price9 = new BigDecimal("224.8572");
    final BigDecimal quantity1 = new BigDecimal("50");
    final BigDecimal quantity2 = new BigDecimal("150");
    final BigDecimal quantity3 = new BigDecimal("75");
    final BigDecimal quantity4 = new BigDecimal("125");
    final BigDecimal quantity5 = new BigDecimal("80");
    final BigDecimal quantity6 = new BigDecimal("60");
    final BigDecimal quantity7 = BigDecimal.ONE;

    try {

      OBContext.setOBContext(USER_ID, ROLE_ID, CLIENT_ID, ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product1 = createProduct("testCostingLC1112A", price1);

      // Create a new product for the test
      Product product2 = createProduct("testCostingLC1112B", price2);

      // Create a new product for the test
      Product product3 = createProduct("testCostingLC1112C", price3);

      // Create purchase order and book it
      Order purchaseOrder1 = createPurchaseOrder(product1, price1, quantity1, day0);

      // Create purchase order and book it
      Order purchaseOrder2 = createPurchaseOrder(product2, price2, quantity2, day0);

      // Create purchase order and book it
      Order purchaseOrder3 = createPurchaseOrder(product2, price2, quantity3, day0);

      // Create purchase order and book it
      Order purchaseOrder4 = createPurchaseOrder(product3, price3, quantity4, day0);

      // Create purchase order and book it
      Order purchaseOrder5 = createPurchaseOrder(product3, price3, quantity5, day0);

      // Create purchase order and book it
      Order purchaseOrder6 = createPurchaseOrder(product1, price1, quantity6, day0);

      // Create goods receipt, run costing background, post it and assert it
      List<Order> purchaseOrderList1 = new ArrayList<Order>();
      purchaseOrderList1.add(purchaseOrder1);
      purchaseOrderList1.add(purchaseOrder2);
      ShipmentInOut goodsReceipt1 = createGoodsReceipt(purchaseOrderList1, null,
          quantity1.add(quantity2), day0);

      // Create goods receipt, run costing background, post it and assert it
      List<Order> purchaseOrderList2 = new ArrayList<Order>();
      purchaseOrderList2.add(purchaseOrder3);
      purchaseOrderList2.add(purchaseOrder4);
      ShipmentInOut goodsReceipt2 = createGoodsReceipt(purchaseOrderList2, null,
          quantity3.add(quantity4), day1);

      // Create goods receipt, run costing background, post it and assert it
      List<Order> purchaseOrderList3 = new ArrayList<Order>();
      purchaseOrderList3.add(purchaseOrder5);
      purchaseOrderList3.add(purchaseOrder6);
      ShipmentInOut goodsReceipt3 = createGoodsReceipt(purchaseOrderList3, null,
          quantity5.add(quantity6), day2);

      // Create Landed Cost
      List<String> landedCostTypeIdList = new ArrayList<String>();
      landedCostTypeIdList.add(LANDEDCOSTTYPE2_ID);
      landedCostTypeIdList.add(LANDEDCOSTTYPE1_ID);
      List<BigDecimal> amountList = new ArrayList<BigDecimal>();
      amountList.add(quantity7.multiply(price4));
      amountList.add(quantity7.multiply(price5));
      List<ShipmentInOut> receiptList = new ArrayList<ShipmentInOut>();
      receiptList.add(goodsReceipt1);
      receiptList.add(goodsReceipt1);
      List<ShipmentInOutLine> receiptLineList = new ArrayList<ShipmentInOutLine>();
      receiptLineList.add(goodsReceipt1.getMaterialMgmtShipmentInOutLineList().get(0));
      receiptLineList.add(goodsReceipt1.getMaterialMgmtShipmentInOutLineList().get(1));
      createLandedCost(landedCostTypeIdList, amountList, receiptList, receiptLineList, day3);

      // Assert product transactions 1
      List<ProductTransactionAssert> productTransactionAssertList1 = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList1.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt1.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price6, price1));
      productTransactionAssertList1.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt3.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(1), price1, price1));
      assertProductTransaction(product1.getId(), productTransactionAssertList1);

      // Assert product transactions 2
      List<ProductTransactionAssert> productTransactionAssertList2 = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList2.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt1.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(1), price2, price8, price2));
      productTransactionAssertList2.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt2.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price2, price2));
      assertProductTransaction(product2.getId(), productTransactionAssertList2);

      // Assert product transactions 3
      List<ProductTransactionAssert> productTransactionAssertList3 = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList3.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt2.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(1), price3, price3));
      productTransactionAssertList3.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt3.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price3, price3));
      assertProductTransaction(product3.getId(), productTransactionAssertList3);

      // Assert product costing 1
      List<MaterialTransaction> transactionList1 = getProductTransactions(product1.getId());
      List<ProductCostingAssert> productCostingAssertList1 = new ArrayList<ProductCostingAssert>();
      productCostingAssertList1.add(new ProductCostingAssert(transactionList1.get(0), price1,
          price1, price6, quantity1));
      productCostingAssertList1.add(new ProductCostingAssert(transactionList1.get(1), price1,
          price1, price7, quantity1.add(quantity6)));
      assertProductCosting(product1.getId(), productCostingAssertList1);

      // Assert product costing 2
      List<MaterialTransaction> transactionList2 = getProductTransactions(product2.getId());
      List<ProductCostingAssert> productCostingAssertList2 = new ArrayList<ProductCostingAssert>();
      productCostingAssertList2.add(new ProductCostingAssert(transactionList2.get(0), price2,
          price2, price8, quantity2));
      productCostingAssertList2.add(new ProductCostingAssert(transactionList2.get(1), price2,
          price2, price9, quantity2.add(quantity3)));
      assertProductCosting(product2.getId(), productCostingAssertList2);

      // Assert product costing 3
      List<MaterialTransaction> transactionList3 = getProductTransactions(product3.getId());
      List<ProductCostingAssert> productCostingAssertList3 = new ArrayList<ProductCostingAssert>();
      productCostingAssertList3.add(new ProductCostingAssert(transactionList3.get(0), price3, null,
          price3, quantity4));
      productCostingAssertList3.add(new ProductCostingAssert(transactionList3.get(1), price3, null,
          price3, quantity4.add(quantity5)));
      assertProductCosting(product3.getId(), productCostingAssertList3);

      // Assert cost adjustment 1
      List<CostAdjustment> costAdjustmentList1 = getCostAdjustment(product1.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList1 = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList11 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList11.add(new CostAdjustmentAssert(transactionList1.get(0), "LC",
          quantity1.multiply(price6).add(quantity1.multiply(price1).negate()), day3, true, false));
      costAdjustmentAssertLineList11.add(new CostAdjustmentAssert(transactionList2.get(0), "LC",
          quantity2.multiply(price8).add(quantity2.multiply(price2).negate()), day3, true, false));
      costAdjustmentAssertList1.add(costAdjustmentAssertLineList11);
      assertCostAdjustment(costAdjustmentList1, costAdjustmentAssertList1);

      // Assert cost adjustment 2
      List<CostAdjustment> costAdjustmentList2 = getCostAdjustment(product2.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList2 = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList12 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList12.add(new CostAdjustmentAssert(transactionList1.get(0), "LC",
          quantity1.multiply(price6).add(quantity1.multiply(price1).negate()), day3, true, false));
      costAdjustmentAssertLineList12.add(new CostAdjustmentAssert(transactionList2.get(0), "LC",
          quantity2.multiply(price8).add(quantity2.multiply(price2).negate()), day3, true, false));
      costAdjustmentAssertList2.add(costAdjustmentAssertLineList12);
      assertCostAdjustment(costAdjustmentList2, costAdjustmentAssertList2);

      // Assert cost adjustment 3
      assertEquals(getCostAdjustment(product3.getId()), null);

      OBDal.getInstance().commitAndClose();

    } catch (Exception e) {
      System.out.println(e.getMessage());
      throw new OBException(e);
    }

    finally {
      OBContext.restorePreviousMode();
    }
  }

  @Test
  public void testCostingLC1113() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final int day3 = 15;
    final BigDecimal price1 = new BigDecimal("111.00");
    final BigDecimal price2 = new BigDecimal("222.00");
    final BigDecimal price3 = new BigDecimal("333.00");
    final BigDecimal price4 = new BigDecimal("325.00");
    final BigDecimal price5 = new BigDecimal("425.00");
    final BigDecimal price6 = new BigDecimal("112.50");
    final BigDecimal price7 = new BigDecimal("111.6818");
    final BigDecimal price8 = new BigDecimal("225.00");
    final BigDecimal quantity1 = new BigDecimal("50");
    final BigDecimal quantity2 = new BigDecimal("150");
    final BigDecimal quantity3 = new BigDecimal("75");
    final BigDecimal quantity4 = new BigDecimal("125");
    final BigDecimal quantity5 = new BigDecimal("80");
    final BigDecimal quantity6 = new BigDecimal("60");
    final BigDecimal quantity7 = BigDecimal.ONE;

    try {

      OBContext.setOBContext(USER_ID, ROLE_ID, CLIENT_ID, ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product1 = createProduct("testCostingLC1113A", price1);

      // Create a new product for the test
      Product product2 = createProduct("testCostingLC1113B", price2);

      // Create a new product for the test
      Product product3 = createProduct("testCostingLC1113C", price3);

      // Create purchase order and book it
      Order purchaseOrder1 = createPurchaseOrder(product1, price1, quantity1, day0);

      // Create purchase order and book it
      Order purchaseOrder2 = createPurchaseOrder(product2, price2, quantity2, day0);

      // Create purchase order and book it
      Order purchaseOrder3 = createPurchaseOrder(product2, price2, quantity3, day0);

      // Create purchase order and book it
      Order purchaseOrder4 = createPurchaseOrder(product3, price3, quantity4, day0);

      // Create purchase order and book it
      Order purchaseOrder5 = createPurchaseOrder(product3, price3, quantity5, day0);

      // Create purchase order and book it
      Order purchaseOrder6 = createPurchaseOrder(product1, price1, quantity6, day0);

      // Create goods receipt, run costing background, post it and assert it
      List<Order> purchaseOrderList1 = new ArrayList<Order>();
      purchaseOrderList1.add(purchaseOrder1);
      purchaseOrderList1.add(purchaseOrder2);
      ShipmentInOut goodsReceipt1 = createGoodsReceipt(purchaseOrderList1, null,
          quantity1.add(quantity2), day0);

      // Create goods receipt, run costing background, post it and assert it
      List<Order> purchaseOrderList2 = new ArrayList<Order>();
      purchaseOrderList2.add(purchaseOrder3);
      purchaseOrderList2.add(purchaseOrder4);
      ShipmentInOut goodsReceipt2 = createGoodsReceipt(purchaseOrderList2, null,
          quantity3.add(quantity4), day1);

      // Create goods receipt, run costing background, post it and assert it
      List<Order> purchaseOrderList3 = new ArrayList<Order>();
      purchaseOrderList3.add(purchaseOrder5);
      purchaseOrderList3.add(purchaseOrder6);
      ShipmentInOut goodsReceipt3 = createGoodsReceipt(purchaseOrderList3, null,
          quantity5.add(quantity6), day2);

      // Create Landed Cost
      List<String> landedCostTypeIdList = new ArrayList<String>();
      landedCostTypeIdList.add(LANDEDCOSTTYPE2_ID);
      landedCostTypeIdList.add(LANDEDCOSTTYPE1_ID);
      List<BigDecimal> amountList = new ArrayList<BigDecimal>();
      amountList.add(quantity7.multiply(price4));
      amountList.add(quantity7.multiply(price5));
      List<ShipmentInOut> receiptList = new ArrayList<ShipmentInOut>();
      receiptList.add(goodsReceipt1);
      receiptList.add(goodsReceipt2);
      List<ShipmentInOutLine> receiptLineList = new ArrayList<ShipmentInOutLine>();
      receiptLineList.add(null);
      receiptLineList.add(goodsReceipt2.getMaterialMgmtShipmentInOutLineList().get(0));
      createLandedCost(landedCostTypeIdList, amountList, receiptList, receiptLineList, day3);

      // Assert product transactions 1
      List<ProductTransactionAssert> productTransactionAssertList1 = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList1.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt1.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price6, price1));
      productTransactionAssertList1.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt3.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(1), price1, price1));
      assertProductTransaction(product1.getId(), productTransactionAssertList1);

      // Assert product transactions 2
      List<ProductTransactionAssert> productTransactionAssertList2 = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList2.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt1.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(1), price2, price8, price2));
      productTransactionAssertList2.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt2.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price2, price8, price2));
      assertProductTransaction(product2.getId(), productTransactionAssertList2);

      // Assert product transactions 3
      List<ProductTransactionAssert> productTransactionAssertList3 = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList3.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt2.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(1), price3, price3));
      productTransactionAssertList3.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt3.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price3, price3));
      assertProductTransaction(product3.getId(), productTransactionAssertList3);

      // Assert product costing 1
      List<MaterialTransaction> transactionList1 = getProductTransactions(product1.getId());
      List<ProductCostingAssert> productCostingAssertList1 = new ArrayList<ProductCostingAssert>();
      productCostingAssertList1.add(new ProductCostingAssert(transactionList1.get(0), price1,
          price1, price6, quantity1));
      productCostingAssertList1.add(new ProductCostingAssert(transactionList1.get(1), price1,
          price1, price7, quantity1.add(quantity6)));
      assertProductCosting(product1.getId(), productCostingAssertList1);

      // Assert product costing 2
      List<MaterialTransaction> transactionList2 = getProductTransactions(product2.getId());
      List<ProductCostingAssert> productCostingAssertList2 = new ArrayList<ProductCostingAssert>();
      productCostingAssertList2.add(new ProductCostingAssert(transactionList2.get(0), price2,
          price2, price8, quantity2));
      productCostingAssertList2.add(new ProductCostingAssert(transactionList2.get(1), price2,
          price2, price8, quantity2.add(quantity3)));
      assertProductCosting(product2.getId(), productCostingAssertList2);

      // Assert product costing 3
      List<MaterialTransaction> transactionList3 = getProductTransactions(product3.getId());
      List<ProductCostingAssert> productCostingAssertList3 = new ArrayList<ProductCostingAssert>();
      productCostingAssertList3.add(new ProductCostingAssert(transactionList3.get(0), price3, null,
          price3, quantity4));
      productCostingAssertList3.add(new ProductCostingAssert(transactionList3.get(1), price3, null,
          price3, quantity4.add(quantity5)));
      assertProductCosting(product3.getId(), productCostingAssertList3);

      // Assert cost adjustment 1
      List<CostAdjustment> costAdjustmentList1 = getCostAdjustment(product1.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList1 = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList11 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList11.add(new CostAdjustmentAssert(transactionList1.get(0), "LC",
          quantity1.multiply(price6).add(quantity1.multiply(price1).negate()), day3, true, false));
      costAdjustmentAssertLineList11.add(new CostAdjustmentAssert(transactionList2.get(0), "LC",
          quantity2.multiply(price8).add(quantity2.multiply(price2).negate()), day3, true, false));
      costAdjustmentAssertLineList11.add(new CostAdjustmentAssert(transactionList2.get(1), "LC",
          quantity3.multiply(price8).add(quantity3.multiply(price2).negate()), day3, true, false));
      costAdjustmentAssertList1.add(costAdjustmentAssertLineList11);
      assertCostAdjustment(costAdjustmentList1, costAdjustmentAssertList1);

      // Assert cost adjustment 2
      List<CostAdjustment> costAdjustmentList2 = getCostAdjustment(product2.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList2 = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList12 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList12.add(new CostAdjustmentAssert(transactionList1.get(0), "LC",
          quantity1.multiply(price6).add(quantity1.multiply(price1).negate()), day3, true, false));
      costAdjustmentAssertLineList12.add(new CostAdjustmentAssert(transactionList2.get(0), "LC",
          quantity2.multiply(price8).add(quantity2.multiply(price2).negate()), day3, true, false));
      costAdjustmentAssertLineList12.add(new CostAdjustmentAssert(transactionList2.get(1), "LC",
          quantity3.multiply(price8).add(quantity3.multiply(price2).negate()), day3, true, false));
      costAdjustmentAssertList2.add(costAdjustmentAssertLineList12);
      assertCostAdjustment(costAdjustmentList2, costAdjustmentAssertList2);

      // Assert cost adjustment 3
      assertEquals(getCostAdjustment(product3.getId()), null);

      OBDal.getInstance().commitAndClose();

    } catch (Exception e) {
      System.out.println(e.getMessage());
      throw new OBException(e);
    }

    finally {
      OBContext.restorePreviousMode();
    }
  }

  @Test
  public void testCostingLC1114() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final int day3 = 15;
    final BigDecimal price1 = new BigDecimal("111.00");
    final BigDecimal price2 = new BigDecimal("222.00");
    final BigDecimal price3 = new BigDecimal("333.00");
    final BigDecimal price4 = new BigDecimal("325.00");
    final BigDecimal price5 = new BigDecimal("425.00");
    final BigDecimal price6 = new BigDecimal("112.50");
    final BigDecimal price7 = new BigDecimal("111.6818");
    final BigDecimal price8 = new BigDecimal("225.00");
    final BigDecimal quantity1 = new BigDecimal("50");
    final BigDecimal quantity2 = new BigDecimal("150");
    final BigDecimal quantity3 = new BigDecimal("75");
    final BigDecimal quantity4 = new BigDecimal("125");
    final BigDecimal quantity5 = new BigDecimal("80");
    final BigDecimal quantity6 = new BigDecimal("60");
    final BigDecimal quantity7 = BigDecimal.ONE;

    try {

      OBContext.setOBContext(USER_ID, ROLE_ID, CLIENT_ID, ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product1 = createProduct("testCostingLC1114A", price1);

      // Create a new product for the test
      Product product2 = createProduct("testCostingLC1114B", price2);

      // Create a new product for the test
      Product product3 = createProduct("testCostingLC1114C", price3);

      // Create purchase order and book it
      Order purchaseOrder1 = createPurchaseOrder(product1, price1, quantity1, day0);

      // Create purchase order and book it
      Order purchaseOrder2 = createPurchaseOrder(product2, price2, quantity2, day0);

      // Create purchase order and book it
      Order purchaseOrder3 = createPurchaseOrder(product2, price2, quantity3, day0);

      // Create purchase order and book it
      Order purchaseOrder4 = createPurchaseOrder(product3, price3, quantity4, day0);

      // Create purchase order and book it
      Order purchaseOrder5 = createPurchaseOrder(product3, price3, quantity5, day0);

      // Create purchase order and book it
      Order purchaseOrder6 = createPurchaseOrder(product1, price1, quantity6, day0);

      // Create goods receipt, run costing background, post it and assert it
      List<Order> purchaseOrderList1 = new ArrayList<Order>();
      purchaseOrderList1.add(purchaseOrder1);
      purchaseOrderList1.add(purchaseOrder2);
      ShipmentInOut goodsReceipt1 = createGoodsReceipt(purchaseOrderList1, null,
          quantity1.add(quantity2), day0);

      // Create goods receipt, run costing background, post it and assert it
      List<Order> purchaseOrderList2 = new ArrayList<Order>();
      purchaseOrderList2.add(purchaseOrder3);
      purchaseOrderList2.add(purchaseOrder4);
      ShipmentInOut goodsReceipt2 = createGoodsReceipt(purchaseOrderList2, null,
          quantity3.add(quantity4), day1);

      // Create goods receipt, run costing background, post it and assert it
      List<Order> purchaseOrderList3 = new ArrayList<Order>();
      purchaseOrderList3.add(purchaseOrder5);
      purchaseOrderList3.add(purchaseOrder6);
      ShipmentInOut goodsReceipt3 = createGoodsReceipt(purchaseOrderList3, null,
          quantity5.add(quantity6), day2);

      // Create Landed Cost
      List<String> landedCostTypeIdList = new ArrayList<String>();
      landedCostTypeIdList.add(LANDEDCOSTTYPE2_ID);
      landedCostTypeIdList.add(LANDEDCOSTTYPE1_ID);
      List<BigDecimal> amountList = new ArrayList<BigDecimal>();
      amountList.add(quantity7.multiply(price4));
      amountList.add(quantity7.multiply(price5));
      List<ShipmentInOut> receiptList = new ArrayList<ShipmentInOut>();
      receiptList.add(goodsReceipt1);
      receiptList.add(goodsReceipt1);
      receiptList.add(goodsReceipt2);
      List<ShipmentInOutLine> receiptLineList = new ArrayList<ShipmentInOutLine>();
      receiptLineList.add(goodsReceipt1.getMaterialMgmtShipmentInOutLineList().get(0));
      receiptLineList.add(goodsReceipt1.getMaterialMgmtShipmentInOutLineList().get(1));
      receiptLineList.add(goodsReceipt2.getMaterialMgmtShipmentInOutLineList().get(0));
      createLandedCost(landedCostTypeIdList, amountList, receiptList, receiptLineList, day3);

      // Assert product transactions 1
      List<ProductTransactionAssert> productTransactionAssertList1 = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList1.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt1.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price6, price1));
      productTransactionAssertList1.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt3.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(1), price1, price1));
      assertProductTransaction(product1.getId(), productTransactionAssertList1);

      // Assert product transactions 2
      List<ProductTransactionAssert> productTransactionAssertList2 = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList2.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt1.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(1), price2, price8, price2));
      productTransactionAssertList2.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt2.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price2, price8, price2));
      assertProductTransaction(product2.getId(), productTransactionAssertList2);

      // Assert product transactions 3
      List<ProductTransactionAssert> productTransactionAssertList3 = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList3.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt2.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(1), price3, price3));
      productTransactionAssertList3.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt3.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price3, price3));
      assertProductTransaction(product3.getId(), productTransactionAssertList3);

      // Assert product costing 1
      List<MaterialTransaction> transactionList1 = getProductTransactions(product1.getId());
      List<ProductCostingAssert> productCostingAssertList1 = new ArrayList<ProductCostingAssert>();
      productCostingAssertList1.add(new ProductCostingAssert(transactionList1.get(0), price1,
          price1, price6, quantity1));
      productCostingAssertList1.add(new ProductCostingAssert(transactionList1.get(1), price1,
          price1, price7, quantity1.add(quantity6)));
      assertProductCosting(product1.getId(), productCostingAssertList1);

      // Assert product costing 2
      List<MaterialTransaction> transactionList2 = getProductTransactions(product2.getId());
      List<ProductCostingAssert> productCostingAssertList2 = new ArrayList<ProductCostingAssert>();
      productCostingAssertList2.add(new ProductCostingAssert(transactionList2.get(0), price2,
          price2, price8, quantity2));
      productCostingAssertList2.add(new ProductCostingAssert(transactionList2.get(1), price2,
          price2, price8, quantity2.add(quantity3)));
      assertProductCosting(product2.getId(), productCostingAssertList2);

      // Assert product costing 3
      List<MaterialTransaction> transactionList3 = getProductTransactions(product3.getId());
      List<ProductCostingAssert> productCostingAssertList3 = new ArrayList<ProductCostingAssert>();
      productCostingAssertList3.add(new ProductCostingAssert(transactionList3.get(0), price3, null,
          price3, quantity4));
      productCostingAssertList3.add(new ProductCostingAssert(transactionList3.get(1), price3, null,
          price3, quantity4.add(quantity5)));
      assertProductCosting(product3.getId(), productCostingAssertList3);

      // Assert cost adjustment 1
      List<CostAdjustment> costAdjustmentList1 = getCostAdjustment(product1.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList1 = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList11 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList11.add(new CostAdjustmentAssert(transactionList1.get(0), "LC",
          quantity1.multiply(price6).add(quantity1.multiply(price1).negate()), day3, true, false));
      costAdjustmentAssertLineList11.add(new CostAdjustmentAssert(transactionList2.get(0), "LC",
          quantity2.multiply(price8).add(quantity2.multiply(price2).negate()), day3, true, false));
      costAdjustmentAssertLineList11.add(new CostAdjustmentAssert(transactionList2.get(1), "LC",
          quantity3.multiply(price8).add(quantity3.multiply(price2).negate()), day3, true, false));
      costAdjustmentAssertList1.add(costAdjustmentAssertLineList11);
      assertCostAdjustment(costAdjustmentList1, costAdjustmentAssertList1);

      // Assert cost adjustment 2
      List<CostAdjustment> costAdjustmentList2 = getCostAdjustment(product2.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList2 = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList12 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList12.add(new CostAdjustmentAssert(transactionList1.get(0), "LC",
          quantity1.multiply(price6).add(quantity1.multiply(price1).negate()), day3, true, false));
      costAdjustmentAssertLineList12.add(new CostAdjustmentAssert(transactionList2.get(0), "LC",
          quantity2.multiply(price8).add(quantity2.multiply(price2).negate()), day3, true, false));
      costAdjustmentAssertLineList12.add(new CostAdjustmentAssert(transactionList2.get(1), "LC",
          quantity3.multiply(price8).add(quantity3.multiply(price2).negate()), day3, true, false));
      costAdjustmentAssertList2.add(costAdjustmentAssertLineList12);
      assertCostAdjustment(costAdjustmentList2, costAdjustmentAssertList2);

      // Assert cost adjustment 3
      assertEquals(getCostAdjustment(product3.getId()), null);

      OBDal.getInstance().commitAndClose();

    } catch (Exception e) {
      System.out.println(e.getMessage());
      throw new OBException(e);
    }

    finally {
      OBContext.restorePreviousMode();
    }
  }

  @Test
  public void testCostingLC1115() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final int day3 = 15;
    final BigDecimal price1 = new BigDecimal("111.00");
    final BigDecimal price2 = new BigDecimal("222.00");
    final BigDecimal price3 = new BigDecimal("333.00");
    final BigDecimal price4 = new BigDecimal("325.00");
    final BigDecimal price5 = new BigDecimal("425.00");
    final BigDecimal price6 = new BigDecimal("111.6726");
    final BigDecimal price7 = new BigDecimal("111.3057");
    final BigDecimal price8 = new BigDecimal("223.345267");
    final BigDecimal price9 = new BigDecimal("335.0179");
    final BigDecimal price10 = new BigDecimal("335.0180");
    final BigDecimal quantity1 = new BigDecimal("50");
    final BigDecimal quantity2 = new BigDecimal("150");
    final BigDecimal quantity3 = new BigDecimal("75");
    final BigDecimal quantity4 = new BigDecimal("125");
    final BigDecimal quantity5 = new BigDecimal("80");
    final BigDecimal quantity6 = new BigDecimal("60");
    final BigDecimal quantity7 = BigDecimal.ONE;

    try {

      OBContext.setOBContext(USER_ID, ROLE_ID, CLIENT_ID, ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product1 = createProduct("testCostingLC1115A", price1);

      // Create a new product for the test
      Product product2 = createProduct("testCostingLC1115B", price2);

      // Create a new product for the test
      Product product3 = createProduct("testCostingLC1115C", price3);

      // Create purchase order and book it
      Order purchaseOrder1 = createPurchaseOrder(product1, price1, quantity1, day0);

      // Create purchase order and book it
      Order purchaseOrder2 = createPurchaseOrder(product2, price2, quantity2, day0);

      // Create purchase order and book it
      Order purchaseOrder3 = createPurchaseOrder(product2, price2, quantity3, day0);

      // Create purchase order and book it
      Order purchaseOrder4 = createPurchaseOrder(product3, price3, quantity4, day0);

      // Create purchase order and book it
      Order purchaseOrder5 = createPurchaseOrder(product3, price3, quantity5, day0);

      // Create purchase order and book it
      Order purchaseOrder6 = createPurchaseOrder(product1, price1, quantity6, day0);

      // Create goods receipt, run costing background, post it and assert it
      List<Order> purchaseOrderList1 = new ArrayList<Order>();
      purchaseOrderList1.add(purchaseOrder1);
      purchaseOrderList1.add(purchaseOrder2);
      ShipmentInOut goodsReceipt1 = createGoodsReceipt(purchaseOrderList1, null,
          quantity1.add(quantity2), day0);

      // Create goods receipt, run costing background, post it and assert it
      List<Order> purchaseOrderList2 = new ArrayList<Order>();
      purchaseOrderList2.add(purchaseOrder3);
      purchaseOrderList2.add(purchaseOrder4);
      ShipmentInOut goodsReceipt2 = createGoodsReceipt(purchaseOrderList2, null,
          quantity3.add(quantity4), day1);

      // Create goods receipt, run costing background, post it and assert it
      List<Order> purchaseOrderList3 = new ArrayList<Order>();
      purchaseOrderList3.add(purchaseOrder5);
      purchaseOrderList3.add(purchaseOrder6);
      ShipmentInOut goodsReceipt3 = createGoodsReceipt(purchaseOrderList3, null,
          quantity5.add(quantity6), day2);

      // Create Landed Cost
      List<String> landedCostTypeIdList = new ArrayList<String>();
      landedCostTypeIdList.add(LANDEDCOSTTYPE2_ID);
      landedCostTypeIdList.add(LANDEDCOSTTYPE1_ID);
      List<BigDecimal> amountList = new ArrayList<BigDecimal>();
      amountList.add(quantity7.multiply(price4));
      amountList.add(quantity7.multiply(price5));
      List<ShipmentInOut> receiptList = new ArrayList<ShipmentInOut>();
      receiptList.add(goodsReceipt1);
      receiptList.add(goodsReceipt2);
      receiptList.add(goodsReceipt2);
      receiptList.add(goodsReceipt3);
      List<ShipmentInOutLine> receiptLineList = new ArrayList<ShipmentInOutLine>();
      receiptLineList.add(null);
      receiptLineList.add(goodsReceipt2.getMaterialMgmtShipmentInOutLineList().get(0));
      receiptLineList.add(goodsReceipt2.getMaterialMgmtShipmentInOutLineList().get(1));
      receiptLineList.add(goodsReceipt3.getMaterialMgmtShipmentInOutLineList().get(0));
      createLandedCost(landedCostTypeIdList, amountList, receiptList, receiptLineList, day3);

      // Assert product transactions 1
      List<ProductTransactionAssert> productTransactionAssertList1 = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList1.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt1.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price6, price1));
      productTransactionAssertList1.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt3.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(1), price1, price1));
      assertProductTransaction(product1.getId(), productTransactionAssertList1);

      // Assert product transactions 2
      List<ProductTransactionAssert> productTransactionAssertList2 = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList2.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt1.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(1), price2, price8, price2));
      productTransactionAssertList2.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt2.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price2, price8, price2));
      assertProductTransaction(product2.getId(), productTransactionAssertList2);

      // Assert product transactions 3
      List<ProductTransactionAssert> productTransactionAssertList3 = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList3.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt2.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(1), price3, price9, price3));
      productTransactionAssertList3.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt3.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price3, price10, price3));
      assertProductTransaction(product3.getId(), productTransactionAssertList3);

      // Assert product costing 1
      List<MaterialTransaction> transactionList1 = getProductTransactions(product1.getId());
      List<ProductCostingAssert> productCostingAssertList1 = new ArrayList<ProductCostingAssert>();
      productCostingAssertList1.add(new ProductCostingAssert(transactionList1.get(0), price1,
          price1, price6, quantity1));
      productCostingAssertList1.add(new ProductCostingAssert(transactionList1.get(1), price1,
          price1, price7, quantity1.add(quantity6)));
      assertProductCosting(product1.getId(), productCostingAssertList1);

      // Assert product costing 2
      List<MaterialTransaction> transactionList2 = getProductTransactions(product2.getId());
      List<ProductCostingAssert> productCostingAssertList2 = new ArrayList<ProductCostingAssert>();
      productCostingAssertList2.add(new ProductCostingAssert(transactionList2.get(0), price2,
          price2, price8, quantity2));
      productCostingAssertList2.add(new ProductCostingAssert(transactionList2.get(1), price2,
          price2, price8, quantity2.add(quantity3)));
      assertProductCosting(product2.getId(), productCostingAssertList2);

      // Assert product costing 3
      List<MaterialTransaction> transactionList3 = getProductTransactions(product3.getId());
      List<ProductCostingAssert> productCostingAssertList3 = new ArrayList<ProductCostingAssert>();
      productCostingAssertList3.add(new ProductCostingAssert(transactionList3.get(0), price3,
          price3, price9, quantity4));
      productCostingAssertList3.add(new ProductCostingAssert(transactionList3.get(1), price3,
          price3, price10, quantity4.add(quantity5)));
      assertProductCosting(product3.getId(), productCostingAssertList3);

      // Assert cost adjustment 1
      List<CostAdjustment> costAdjustmentList1 = getCostAdjustment(product1.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList1 = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList11 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList11.add(new CostAdjustmentAssert(transactionList1.get(0), "LC",
          quantity1.multiply(price6).add(quantity1.multiply(price1).negate()), day3, true, false));
      costAdjustmentAssertLineList11.add(new CostAdjustmentAssert(transactionList2.get(0), "LC",
          quantity2.multiply(price8).add(quantity2.multiply(price2).negate()), day3, true, false));
      costAdjustmentAssertLineList11.add(new CostAdjustmentAssert(transactionList2.get(1), "LC",
          quantity3.multiply(price8).add(quantity3.multiply(price2).negate()), day3, true, false));
      costAdjustmentAssertLineList11.add(new CostAdjustmentAssert(transactionList3.get(0), "LC",
          quantity4.multiply(price9).add(quantity4.multiply(price3).negate()), day3, true, false));
      costAdjustmentAssertLineList11.add(new CostAdjustmentAssert(transactionList3.get(1), "LC",
          quantity5.multiply(price10).add(quantity5.multiply(price3).negate()), day3, true, false));
      costAdjustmentAssertList1.add(costAdjustmentAssertLineList11);
      assertCostAdjustment(costAdjustmentList1, costAdjustmentAssertList1);

      // Assert cost adjustment 2
      List<CostAdjustment> costAdjustmentList2 = getCostAdjustment(product2.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList2 = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList12 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList12.add(new CostAdjustmentAssert(transactionList1.get(0), "LC",
          quantity1.multiply(price6).add(quantity1.multiply(price1).negate()), day3, true, false));
      costAdjustmentAssertLineList12.add(new CostAdjustmentAssert(transactionList2.get(0), "LC",
          quantity2.multiply(price8).add(quantity2.multiply(price2).negate()), day3, true, false));
      costAdjustmentAssertLineList12.add(new CostAdjustmentAssert(transactionList2.get(1), "LC",
          quantity3.multiply(price8).add(quantity3.multiply(price2).negate()), day3, true, false));
      costAdjustmentAssertLineList12.add(new CostAdjustmentAssert(transactionList3.get(0), "LC",
          quantity4.multiply(price9).add(quantity4.multiply(price3).negate()), day3, true, false));
      costAdjustmentAssertLineList12.add(new CostAdjustmentAssert(transactionList3.get(1), "LC",
          quantity5.multiply(price10).add(quantity5.multiply(price3).negate()), day3, true, false));
      costAdjustmentAssertList2.add(costAdjustmentAssertLineList12);
      assertCostAdjustment(costAdjustmentList2, costAdjustmentAssertList2);

      // Assert cost adjustment 3
      List<CostAdjustment> costAdjustmentList3 = getCostAdjustment(product3.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList3 = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList13 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList13.add(new CostAdjustmentAssert(transactionList1.get(0), "LC",
          quantity1.multiply(price6).add(quantity1.multiply(price1).negate()), day3, true, false));
      costAdjustmentAssertLineList13.add(new CostAdjustmentAssert(transactionList2.get(0), "LC",
          quantity2.multiply(price8).add(quantity2.multiply(price2).negate()), day3, true, false));
      costAdjustmentAssertLineList13.add(new CostAdjustmentAssert(transactionList2.get(1), "LC",
          quantity3.multiply(price8).add(quantity3.multiply(price2).negate()), day3, true, false));
      costAdjustmentAssertLineList13.add(new CostAdjustmentAssert(transactionList3.get(0), "LC",
          quantity4.multiply(price9).add(quantity4.multiply(price3).negate()), day3, true, false));
      costAdjustmentAssertLineList13.add(new CostAdjustmentAssert(transactionList3.get(1), "LC",
          quantity5.multiply(price10).add(quantity5.multiply(price3).negate()), day3, true, false));
      costAdjustmentAssertList3.add(costAdjustmentAssertLineList13);
      assertCostAdjustment(costAdjustmentList3, costAdjustmentAssertList3);

      OBDal.getInstance().commitAndClose();

    } catch (Exception e) {
      System.out.println(e.getMessage());
      throw new OBException(e);
    }

    finally {
      OBContext.restorePreviousMode();
    }
  }

  @Test
  public void testCostingLC1116() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final int day3 = 15;
    final BigDecimal price1 = new BigDecimal("111.00");
    final BigDecimal price2 = new BigDecimal("222.00");
    final BigDecimal price3 = new BigDecimal("333.00");
    final BigDecimal price4 = new BigDecimal("325.00");
    final BigDecimal price5 = new BigDecimal("425.00");
    final BigDecimal price6 = new BigDecimal("111.6726");
    final BigDecimal price7 = new BigDecimal("111.3057");
    final BigDecimal price8 = new BigDecimal("223.345267");
    final BigDecimal price9 = new BigDecimal("335.0179");
    final BigDecimal price10 = new BigDecimal("335.0180");
    final BigDecimal quantity1 = new BigDecimal("50");
    final BigDecimal quantity2 = new BigDecimal("150");
    final BigDecimal quantity3 = new BigDecimal("75");
    final BigDecimal quantity4 = new BigDecimal("125");
    final BigDecimal quantity5 = new BigDecimal("80");
    final BigDecimal quantity6 = new BigDecimal("60");
    final BigDecimal quantity7 = BigDecimal.ONE;

    try {

      OBContext.setOBContext(USER_ID, ROLE_ID, CLIENT_ID, ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product1 = createProduct("testCostingLC1116A", price1);

      // Create a new product for the test
      Product product2 = createProduct("testCostingLC1116B", price2);

      // Create a new product for the test
      Product product3 = createProduct("testCostingLC1116C", price3);

      // Create purchase order and book it
      Order purchaseOrder1 = createPurchaseOrder(product1, price1, quantity1, day0);

      // Create purchase order and book it
      Order purchaseOrder2 = createPurchaseOrder(product2, price2, quantity2, day0);

      // Create purchase order and book it
      Order purchaseOrder3 = createPurchaseOrder(product2, price2, quantity3, day0);

      // Create purchase order and book it
      Order purchaseOrder4 = createPurchaseOrder(product3, price3, quantity4, day0);

      // Create purchase order and book it
      Order purchaseOrder5 = createPurchaseOrder(product3, price3, quantity5, day0);

      // Create purchase order and book it
      Order purchaseOrder6 = createPurchaseOrder(product1, price1, quantity6, day0);

      // Create goods receipt, run costing background, post it and assert it
      List<Order> purchaseOrderList1 = new ArrayList<Order>();
      purchaseOrderList1.add(purchaseOrder1);
      purchaseOrderList1.add(purchaseOrder2);
      ShipmentInOut goodsReceipt1 = createGoodsReceipt(purchaseOrderList1, null,
          quantity1.add(quantity2), day0);

      // Create goods receipt, run costing background, post it and assert it
      List<Order> purchaseOrderList2 = new ArrayList<Order>();
      purchaseOrderList2.add(purchaseOrder3);
      purchaseOrderList2.add(purchaseOrder4);
      ShipmentInOut goodsReceipt2 = createGoodsReceipt(purchaseOrderList2, null,
          quantity3.add(quantity4), day1);

      // Create goods receipt, run costing background, post it and assert it
      List<Order> purchaseOrderList3 = new ArrayList<Order>();
      purchaseOrderList3.add(purchaseOrder5);
      purchaseOrderList3.add(purchaseOrder6);
      ShipmentInOut goodsReceipt3 = createGoodsReceipt(purchaseOrderList3, null,
          quantity5.add(quantity6), day2);

      // Create Landed Cost
      List<String> landedCostTypeIdList = new ArrayList<String>();
      landedCostTypeIdList.add(LANDEDCOSTTYPE2_ID);
      landedCostTypeIdList.add(LANDEDCOSTTYPE1_ID);
      List<BigDecimal> amountList = new ArrayList<BigDecimal>();
      amountList.add(quantity7.multiply(price4));
      amountList.add(quantity7.multiply(price5));
      List<ShipmentInOut> receiptList = new ArrayList<ShipmentInOut>();
      receiptList.add(goodsReceipt1);
      receiptList.add(goodsReceipt2);
      receiptList.add(goodsReceipt3);
      List<ShipmentInOutLine> receiptLineList = new ArrayList<ShipmentInOutLine>();
      receiptLineList.add(null);
      receiptLineList.add(null);
      receiptLineList.add(goodsReceipt3.getMaterialMgmtShipmentInOutLineList().get(0));
      createLandedCost(landedCostTypeIdList, amountList, receiptList, receiptLineList, day3);

      // Assert product transactions 1
      List<ProductTransactionAssert> productTransactionAssertList1 = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList1.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt1.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price6, price1));
      productTransactionAssertList1.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt3.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(1), price1, price1));
      assertProductTransaction(product1.getId(), productTransactionAssertList1);

      // Assert product transactions 2
      List<ProductTransactionAssert> productTransactionAssertList2 = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList2.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt1.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(1), price2, price8, price2));
      productTransactionAssertList2.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt2.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price2, price8, price2));
      assertProductTransaction(product2.getId(), productTransactionAssertList2);

      // Assert product transactions 3
      List<ProductTransactionAssert> productTransactionAssertList3 = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList3.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt2.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(1), price3, price9, price3));
      productTransactionAssertList3.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt3.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price3, price10, price3));
      assertProductTransaction(product3.getId(), productTransactionAssertList3);

      // Assert product costing 1
      List<MaterialTransaction> transactionList1 = getProductTransactions(product1.getId());
      List<ProductCostingAssert> productCostingAssertList1 = new ArrayList<ProductCostingAssert>();
      productCostingAssertList1.add(new ProductCostingAssert(transactionList1.get(0), price1,
          price1, price6, quantity1));
      productCostingAssertList1.add(new ProductCostingAssert(transactionList1.get(1), price1,
          price1, price7, quantity1.add(quantity6)));
      assertProductCosting(product1.getId(), productCostingAssertList1);

      // Assert product costing 2
      List<MaterialTransaction> transactionList2 = getProductTransactions(product2.getId());
      List<ProductCostingAssert> productCostingAssertList2 = new ArrayList<ProductCostingAssert>();
      productCostingAssertList2.add(new ProductCostingAssert(transactionList2.get(0), price2,
          price2, price8, quantity2));
      productCostingAssertList2.add(new ProductCostingAssert(transactionList2.get(1), price2,
          price2, price8, quantity2.add(quantity3)));
      assertProductCosting(product2.getId(), productCostingAssertList2);

      // Assert product costing 3
      List<MaterialTransaction> transactionList3 = getProductTransactions(product3.getId());
      List<ProductCostingAssert> productCostingAssertList3 = new ArrayList<ProductCostingAssert>();
      productCostingAssertList3.add(new ProductCostingAssert(transactionList3.get(0), price3,
          price3, price9, quantity4));
      productCostingAssertList3.add(new ProductCostingAssert(transactionList3.get(1), price3,
          price3, price10, quantity4.add(quantity5)));
      assertProductCosting(product3.getId(), productCostingAssertList3);

      // Assert cost adjustment 1
      List<CostAdjustment> costAdjustmentList1 = getCostAdjustment(product1.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList1 = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList11 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList11.add(new CostAdjustmentAssert(transactionList1.get(0), "LC",
          quantity1.multiply(price6).add(quantity1.multiply(price1).negate()), day3, true, false));
      costAdjustmentAssertLineList11.add(new CostAdjustmentAssert(transactionList2.get(0), "LC",
          quantity2.multiply(price8).add(quantity2.multiply(price2).negate()), day3, true, false));
      costAdjustmentAssertLineList11.add(new CostAdjustmentAssert(transactionList2.get(1), "LC",
          quantity3.multiply(price8).add(quantity3.multiply(price2).negate()), day3, true, false));
      costAdjustmentAssertLineList11.add(new CostAdjustmentAssert(transactionList3.get(0), "LC",
          quantity4.multiply(price9).add(quantity4.multiply(price3).negate()), day3, true, false));
      costAdjustmentAssertLineList11.add(new CostAdjustmentAssert(transactionList3.get(1), "LC",
          quantity5.multiply(price10).add(quantity5.multiply(price3).negate()), day3, true, false));
      costAdjustmentAssertList1.add(costAdjustmentAssertLineList11);
      assertCostAdjustment(costAdjustmentList1, costAdjustmentAssertList1);

      // Assert cost adjustment 2
      List<CostAdjustment> costAdjustmentList2 = getCostAdjustment(product2.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList2 = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList12 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList12.add(new CostAdjustmentAssert(transactionList1.get(0), "LC",
          quantity1.multiply(price6).add(quantity1.multiply(price1).negate()), day3, true, false));
      costAdjustmentAssertLineList12.add(new CostAdjustmentAssert(transactionList2.get(0), "LC",
          quantity2.multiply(price8).add(quantity2.multiply(price2).negate()), day3, true, false));
      costAdjustmentAssertLineList12.add(new CostAdjustmentAssert(transactionList2.get(1), "LC",
          quantity3.multiply(price8).add(quantity3.multiply(price2).negate()), day3, true, false));
      costAdjustmentAssertLineList12.add(new CostAdjustmentAssert(transactionList3.get(0), "LC",
          quantity4.multiply(price9).add(quantity4.multiply(price3).negate()), day3, true, false));
      costAdjustmentAssertLineList12.add(new CostAdjustmentAssert(transactionList3.get(1), "LC",
          quantity5.multiply(price10).add(quantity5.multiply(price3).negate()), day3, true, false));
      costAdjustmentAssertList2.add(costAdjustmentAssertLineList12);
      assertCostAdjustment(costAdjustmentList2, costAdjustmentAssertList2);

      // Assert cost adjustment 3
      List<CostAdjustment> costAdjustmentList3 = getCostAdjustment(product3.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList3 = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList13 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList13.add(new CostAdjustmentAssert(transactionList1.get(0), "LC",
          quantity1.multiply(price6).add(quantity1.multiply(price1).negate()), day3, true, false));
      costAdjustmentAssertLineList13.add(new CostAdjustmentAssert(transactionList2.get(0), "LC",
          quantity2.multiply(price8).add(quantity2.multiply(price2).negate()), day3, true, false));
      costAdjustmentAssertLineList13.add(new CostAdjustmentAssert(transactionList2.get(1), "LC",
          quantity3.multiply(price8).add(quantity3.multiply(price2).negate()), day3, true, false));
      costAdjustmentAssertLineList13.add(new CostAdjustmentAssert(transactionList3.get(0), "LC",
          quantity4.multiply(price9).add(quantity4.multiply(price3).negate()), day3, true, false));
      costAdjustmentAssertLineList13.add(new CostAdjustmentAssert(transactionList3.get(1), "LC",
          quantity5.multiply(price10).add(quantity5.multiply(price3).negate()), day3, true, false));
      costAdjustmentAssertList3.add(costAdjustmentAssertLineList13);
      assertCostAdjustment(costAdjustmentList3, costAdjustmentAssertList3);

      OBDal.getInstance().commitAndClose();

    } catch (Exception e) {
      System.out.println(e.getMessage());
      throw new OBException(e);
    }

    finally {
      OBContext.restorePreviousMode();
    }
  }

  @Test
  public void testCostingLC5551() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final BigDecimal price1 = new BigDecimal("555.00");
    final BigDecimal price2 = new BigDecimal("100.00");
    final BigDecimal price3 = new BigDecimal("645.00");
    final BigDecimal price4 = new BigDecimal("125.00");
    final BigDecimal quantity1 = BigDecimal.ONE;
    final BigDecimal rate = new BigDecimal("0.90");

    try {

      OBContext.setOBContext(USER_ID, ROLE_ID, CLIENT_ID, ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product = createProduct("testCostingLC5551", price1);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt = createGoodsReceipt(product, price1, quantity1, day0);

      // Create Landed Cost
      List<String> landedCostTypeIdList = new ArrayList<String>();
      landedCostTypeIdList.add(LANDEDCOSTTYPE3_ID);
      List<BigDecimal> amountList = new ArrayList<BigDecimal>();
      amountList.add(quantity1.multiply(price2));
      List<ShipmentInOut> receiptList = new ArrayList<ShipmentInOut>();
      receiptList.add(goodsReceipt);
      LandedCost landedCost = createLandedCost(landedCostTypeIdList, amountList, receiptList, null,
          day1);

      // Create purchase invoice with landed cost, post it and assert it
      Invoice purchaseInvoiceLandedCost1 = createPurchaseInvoiceLandedCost(LANDEDCOSTTYPE3_ID,
          price2, quantity1, rate, day2);

      // Match invoice landed cost
      matchInvoiceLandedCost(purchaseInvoiceLandedCost1.getInvoiceLineList().get(0), landedCost
          .getLandedCostCostList().get(0), true);

      // Post landed cost cost and assert it
      postLandedCostLine(landedCost.getLandedCostCostList().get(0), purchaseInvoiceLandedCost1
          .getInvoiceLineList().get(0));

      // Assert product transactions
      List<ProductTransactionAssert> productTransactionAssertList1 = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList1.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price3, price1));
      assertProductTransaction(product.getId(), productTransactionAssertList1);

      // Assert product costing
      List<MaterialTransaction> transactionList = getProductTransactions(product.getId());
      List<ProductCostingAssert> productCostingAssertList1 = new ArrayList<ProductCostingAssert>();
      productCostingAssertList1.add(new ProductCostingAssert(transactionList.get(0), price1,
          price1, price3, quantity1));
      assertProductCosting(product.getId(), productCostingAssertList1);

      // Assert cost adjustment
      List<CostAdjustment> costAdjustmentList = getCostAdjustment(product.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList1 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList1.add(new CostAdjustmentAssert(transactionList.get(0),
          CURRENCY2_ID, "LC", quantity1.multiply(price2), day1, true, false));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList1);
      List<CostAdjustmentAssert> costAdjustmentAssertLineList2 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList2.add(new CostAdjustmentAssert(transactionList.get(0),
          CURRENCY2_ID, "LC", quantity1.multiply(price4), day1, true, false));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList2);
      assertCostAdjustment(costAdjustmentList, costAdjustmentAssertList);

      OBDal.getInstance().commitAndClose();

    } catch (Exception e) {
      System.out.println(e.getMessage());
      throw new OBException(e);
    }

    finally {
      OBContext.restorePreviousMode();
    }
  }

  @Test
  public void testCostingLC5552() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final BigDecimal price1 = new BigDecimal("555.00");
    final BigDecimal price2 = new BigDecimal("100.00");
    final BigDecimal price3 = new BigDecimal("645.00");
    final BigDecimal price4 = new BigDecimal("125.00");
    final BigDecimal quantity1 = BigDecimal.ONE;
    final BigDecimal rate = new BigDecimal("0.90");

    try {

      OBContext.setOBContext(USER_ID, ROLE_ID, CLIENT_ID, ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product = createProduct("testCostingLC5552", price1);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt = createGoodsReceipt(product, price1, quantity1, day0);

      // Create purchase invoice with landed cost, post it and assert it
      Invoice purchaseInvoiceLandedCost1 = createPurchaseInvoiceLandedCost(LANDEDCOSTTYPE3_ID,
          price2, quantity1, rate, day1);

      // Create Landed Cost
      List<Invoice> invoiceList = new ArrayList<Invoice>();
      invoiceList.add(purchaseInvoiceLandedCost1);
      List<ShipmentInOut> receiptList = new ArrayList<ShipmentInOut>();
      receiptList.add(goodsReceipt);
      createLandedCost(invoiceList, receiptList, day2);

      // Assert product transactions
      List<ProductTransactionAssert> productTransactionAssertList1 = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList1.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price3, price1));
      assertProductTransaction(product.getId(), productTransactionAssertList1);

      // Assert product costing
      List<MaterialTransaction> transactionList = getProductTransactions(product.getId());
      List<ProductCostingAssert> productCostingAssertList1 = new ArrayList<ProductCostingAssert>();
      productCostingAssertList1.add(new ProductCostingAssert(transactionList.get(0), price1,
          price1, price3, quantity1));
      assertProductCosting(product.getId(), productCostingAssertList1);

      // Assert cost adjustment
      List<CostAdjustment> costAdjustmentList = getCostAdjustment(product.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList1 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList1.add(new CostAdjustmentAssert(transactionList.get(0),
          CURRENCY2_ID, "LC", quantity1.multiply(price4), day2, true, false));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList1);
      List<CostAdjustmentAssert> costAdjustmentAssertLineList2 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList2.add(new CostAdjustmentAssert(transactionList.get(0),
          CURRENCY2_ID, "LC", quantity1.multiply(price2), day2, true, false));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList2);
      assertCostAdjustment(costAdjustmentList, costAdjustmentAssertList);

      OBDal.getInstance().commitAndClose();

    } catch (Exception e) {
      System.out.println(e.getMessage());
      throw new OBException(e);
    }

    finally {
      OBContext.restorePreviousMode();
    }
  }

  @Test
  public void testCostingLC5553() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final BigDecimal price1 = new BigDecimal("555.00");
    final BigDecimal price2 = new BigDecimal("100.00");
    final BigDecimal price3 = new BigDecimal("585.00");
    final BigDecimal price4 = new BigDecimal("-25.00");
    final BigDecimal quantity1 = BigDecimal.ONE;
    final BigDecimal rate = new BigDecimal("0.30");

    try {

      OBContext.setOBContext(USER_ID, ROLE_ID, CLIENT_ID, ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product = createProduct("testCostingLC5553", price1);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt = createGoodsReceipt(product, price1, quantity1, day0);

      // Create Landed Cost
      List<String> landedCostTypeIdList = new ArrayList<String>();
      landedCostTypeIdList.add(LANDEDCOSTTYPE3_ID);
      List<BigDecimal> amountList = new ArrayList<BigDecimal>();
      amountList.add(quantity1.multiply(price2));
      List<ShipmentInOut> receiptList = new ArrayList<ShipmentInOut>();
      receiptList.add(goodsReceipt);
      LandedCost landedCost = createLandedCost(landedCostTypeIdList, amountList, receiptList, null,
          day1);

      // Create purchase invoice with landed cost, post it and assert it
      Invoice purchaseInvoiceLandedCost1 = createPurchaseInvoiceLandedCost(LANDEDCOSTTYPE3_ID,
          price2, quantity1, rate, day2);

      // Match invoice landed cost
      matchInvoiceLandedCost(purchaseInvoiceLandedCost1.getInvoiceLineList().get(0), landedCost
          .getLandedCostCostList().get(0), true);

      // Post landed cost cost and assert it
      postLandedCostLine(landedCost.getLandedCostCostList().get(0), purchaseInvoiceLandedCost1
          .getInvoiceLineList().get(0));

      // Assert product transactions
      List<ProductTransactionAssert> productTransactionAssertList1 = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList1.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price3, price1));
      assertProductTransaction(product.getId(), productTransactionAssertList1);

      // Assert product costing
      List<MaterialTransaction> transactionList = getProductTransactions(product.getId());
      List<ProductCostingAssert> productCostingAssertList1 = new ArrayList<ProductCostingAssert>();
      productCostingAssertList1.add(new ProductCostingAssert(transactionList.get(0), price1,
          price1, price3, quantity1));
      assertProductCosting(product.getId(), productCostingAssertList1);

      // Assert cost adjustment
      List<CostAdjustment> costAdjustmentList = getCostAdjustment(product.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList1 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList1.add(new CostAdjustmentAssert(transactionList.get(0),
          CURRENCY2_ID, "LC", quantity1.multiply(price2), day1, true, false));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList1);
      List<CostAdjustmentAssert> costAdjustmentAssertLineList2 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList2.add(new CostAdjustmentAssert(transactionList.get(0),
          CURRENCY2_ID, "LC", quantity1.multiply(price4), day1, true, false));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList2);
      assertCostAdjustment(costAdjustmentList, costAdjustmentAssertList);

      OBDal.getInstance().commitAndClose();

    } catch (Exception e) {
      System.out.println(e.getMessage());
      throw new OBException(e);
    }

    finally {
      OBContext.restorePreviousMode();
    }
  }

  @Test
  public void testCostingLC5554() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final BigDecimal price1 = new BigDecimal("555.00");
    final BigDecimal price2 = new BigDecimal("100.00");
    final BigDecimal price3 = new BigDecimal("585.00");
    final BigDecimal price4 = new BigDecimal("-25.00");
    final BigDecimal quantity1 = BigDecimal.ONE;
    final BigDecimal rate = new BigDecimal("0.30");

    try {

      OBContext.setOBContext(USER_ID, ROLE_ID, CLIENT_ID, ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product = createProduct("testCostingLC5554", price1);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt = createGoodsReceipt(product, price1, quantity1, day0);

      // Create purchase invoice with landed cost, post it and assert it
      Invoice purchaseInvoiceLandedCost1 = createPurchaseInvoiceLandedCost(LANDEDCOSTTYPE3_ID,
          price2, quantity1, rate, day1);

      // Create Landed Cost
      List<Invoice> invoiceList = new ArrayList<Invoice>();
      invoiceList.add(purchaseInvoiceLandedCost1);
      List<ShipmentInOut> receiptList = new ArrayList<ShipmentInOut>();
      receiptList.add(goodsReceipt);
      createLandedCost(invoiceList, receiptList, day2);

      // Assert product transactions
      List<ProductTransactionAssert> productTransactionAssertList1 = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList1.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price3, price1));
      assertProductTransaction(product.getId(), productTransactionAssertList1);

      // Assert product costing
      List<MaterialTransaction> transactionList = getProductTransactions(product.getId());
      List<ProductCostingAssert> productCostingAssertList1 = new ArrayList<ProductCostingAssert>();
      productCostingAssertList1.add(new ProductCostingAssert(transactionList.get(0), price1,
          price1, price3, quantity1));
      assertProductCosting(product.getId(), productCostingAssertList1);

      // Assert cost adjustment
      List<CostAdjustment> costAdjustmentList = getCostAdjustment(product.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList1 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList1.add(new CostAdjustmentAssert(transactionList.get(0),
          CURRENCY2_ID, "LC", quantity1.multiply(price4), day2, true, false));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList1);
      List<CostAdjustmentAssert> costAdjustmentAssertLineList2 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList2.add(new CostAdjustmentAssert(transactionList.get(0),
          CURRENCY2_ID, "LC", quantity1.multiply(price2), day2, true, false));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList2);
      assertCostAdjustment(costAdjustmentList, costAdjustmentAssertList);

      OBDal.getInstance().commitAndClose();

    } catch (Exception e) {
      System.out.println(e.getMessage());
      throw new OBException(e);
    }

    finally {
      OBContext.restorePreviousMode();
    }
  }

  @Test
  public void testCostingLC9() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final int day3 = 15;
    final int day4 = 20;
    final int day5 = 25;
    final int day6 = 30;
    final BigDecimal price1 = new BigDecimal("120.00");
    final BigDecimal price2 = new BigDecimal("210.00");
    final BigDecimal price3 = new BigDecimal("120.21");
    final BigDecimal price4 = new BigDecimal("150.00");
    final BigDecimal price5 = new BigDecimal("120.30");
    final BigDecimal price6 = new BigDecimal("120.3250");
    final BigDecimal price7 = new BigDecimal("120.2938");
    final BigDecimal quantity1 = new BigDecimal("1000");
    final BigDecimal quantity2 = BigDecimal.ONE;
    final BigDecimal quantity3 = new BigDecimal("200");
    final BigDecimal quantity4 = new BigDecimal("2");
    final BigDecimal amount1 = new BigDecimal("5.00");

    try {

      OBContext.setOBContext(USER_ID, ROLE_ID, CLIENT_ID, ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product = createProduct("testCostingLC9", price1);

      // Create purchase order and book it
      Order purchaseOrder = createPurchaseOrder(product, price1, quantity1, day0);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt = createGoodsReceipt(purchaseOrder, price1, quantity1, day1);

      // Create goods shipment, run costing background, post it and assert it
      ShipmentInOut goodsShipment1 = createGoodsShipment(product, price1, quantity3, day2);

      // Create Landed Cost
      List<String> landedCostTypeIdList = new ArrayList<String>();
      landedCostTypeIdList.add(LANDEDCOSTTYPE2_ID);
      List<BigDecimal> amountList = new ArrayList<BigDecimal>();
      amountList.add(quantity2.multiply(price2));
      List<ShipmentInOut> receiptList = new ArrayList<ShipmentInOut>();
      receiptList.add(goodsReceipt);
      LandedCost landedCost = createLandedCost(landedCostTypeIdList, amountList, receiptList, null,
          day3);

      // Create goods shipment, run costing background, post it and assert it
      ShipmentInOut goodsShipment2 = createGoodsShipment(product, price3, quantity3, day4);

      // Create purchase invoice with landed cost, post it and assert it
      Invoice purchaseInvoiceLandedCost = createPurchaseInvoiceLandedCost(LANDEDCOSTTYPE2_ID,
          price4, quantity4, day5);

      // Match invoice landed cost
      matchInvoiceLandedCost(purchaseInvoiceLandedCost.getInvoiceLineList().get(0), landedCost
          .getLandedCostCostList().get(0), true);

      // Post landed cost cost and assert it
      postLandedCostLine(landedCost.getLandedCostCostList().get(0), purchaseInvoiceLandedCost
          .getInvoiceLineList().get(0));

      // Update transaction total cost amount
      manualCostAdjustment(getProductTransactions(product.getId()).get(1), amount1, true, true,
          day6);

      // Assert product transactions
      List<ProductTransactionAssert> productTransactionAssertList1 = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList1.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price5, price1));
      productTransactionAssertList1.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsShipment1.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price6));
      productTransactionAssertList1.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsShipment2.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price3, price7));
      assertProductTransaction(product.getId(), productTransactionAssertList1);

      // Assert product costing
      List<MaterialTransaction> transactionList = getProductTransactions(product.getId());
      List<ProductCostingAssert> productCostingAssertList1 = new ArrayList<ProductCostingAssert>();
      productCostingAssertList1.add(new ProductCostingAssert(transactionList.get(0), price1,
          price1, price5, quantity1));
      productCostingAssertList1.add(new ProductCostingAssert(transactionList.get(1), price7, null,
          price7, quantity1.add(quantity3.negate())));
      assertProductCosting(product.getId(), productCostingAssertList1);

      // Assert cost adjustment
      List<CostAdjustment> costAdjustmentList = getCostAdjustment(product.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList1 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList1.add(new CostAdjustmentAssert(transactionList.get(0), "LC",
          quantity2.multiply(price2), day3, true, false));
      costAdjustmentAssertLineList1.add(new CostAdjustmentAssert(transactionList.get(1), "PDC",
          quantity3.multiply(price3).add(quantity3.multiply(price1).negate()), day3, false));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList1);
      List<CostAdjustmentAssert> costAdjustmentAssertLineList2 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList2.add(new CostAdjustmentAssert(transactionList.get(0), "LC",
          quantity4.multiply(price4).add(quantity2.multiply(price2).negate()), day3, true, false));
      costAdjustmentAssertLineList2.add(new CostAdjustmentAssert(transactionList.get(1), "PDC",
          quantity3.multiply(price5).add(quantity3.multiply(price3).negate()), day3, false));
      costAdjustmentAssertLineList2.add(new CostAdjustmentAssert(transactionList.get(2), "PDC",
          quantity3.multiply(price5).add(quantity3.multiply(price3).negate()), day4, false));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList2);
      List<CostAdjustmentAssert> costAdjustmentAssertLineList3 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList3.add(new CostAdjustmentAssert(transactionList.get(1), "MCC",
          amount1, day6, true));
      costAdjustmentAssertLineList3.add(new CostAdjustmentAssert(transactionList.get(2), "PDC",
          quantity3.multiply(price7).add(quantity3.multiply(price5).negate()), day6, false));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList3);
      assertCostAdjustment(costAdjustmentList, costAdjustmentAssertList);

      // Post cost adjustment and assert it
      postDocument(costAdjustmentList.get(0));
      List<DocumentPostAssert> documentPostAssertList1 = new ArrayList<DocumentPostAssert>();
      documentPostAssertList1.add(new DocumentPostAssert("99900", quantity3.multiply(price3).add(
          quantity3.multiply(price1).negate()), BigDecimal.ZERO, null));
      documentPostAssertList1.add(new DocumentPostAssert("35000", BigDecimal.ZERO, quantity3
          .multiply(price3).add(quantity3.multiply(price1).negate()), null));
      CostAdjustment costAdjustment1 = OBDal.getInstance().get(CostAdjustment.class,
          costAdjustmentList.get(0).getId());
      assertDocumentPost(costAdjustment1, product.getId(), documentPostAssertList1);

      // Post cost adjustment and assert it
      postDocument(costAdjustmentList.get(1));
      List<DocumentPostAssert> documentPostAssertList2 = new ArrayList<DocumentPostAssert>();
      documentPostAssertList2.add(new DocumentPostAssert("99900", quantity3.multiply(price5).add(
          quantity3.multiply(price3).negate()), BigDecimal.ZERO, null));
      documentPostAssertList2.add(new DocumentPostAssert("35000", BigDecimal.ZERO, quantity3
          .multiply(price5).add(quantity3.multiply(price3).negate()), null));
      documentPostAssertList2.add(new DocumentPostAssert("99900", quantity3.multiply(price5).add(
          quantity3.multiply(price3).negate()), BigDecimal.ZERO, null));
      documentPostAssertList2.add(new DocumentPostAssert("35000", BigDecimal.ZERO, quantity3
          .multiply(price5).add(quantity3.multiply(price3).negate()), null));
      CostAdjustment costAdjustment2 = OBDal.getInstance().get(CostAdjustment.class,
          costAdjustmentList.get(1).getId());
      assertDocumentPost(costAdjustment2, product.getId(), documentPostAssertList2);

      // Post cost adjustment and assert it
      postDocument(costAdjustmentList.get(2));
      List<DocumentPostAssert> documentPostAssertList3 = new ArrayList<DocumentPostAssert>();
      documentPostAssertList3.add(new DocumentPostAssert("99900", amount1, BigDecimal.ZERO, null));
      documentPostAssertList3.add(new DocumentPostAssert("35000", BigDecimal.ZERO, amount1, null));
      documentPostAssertList3.add(new DocumentPostAssert("99900", BigDecimal.ZERO, quantity3
          .multiply(price5).add(quantity3.multiply(price7).negate()), null));
      documentPostAssertList3.add(new DocumentPostAssert("35000", quantity3.multiply(price5).add(
          quantity3.multiply(price7).negate()), BigDecimal.ZERO, null));
      CostAdjustment costAdjustment3 = OBDal.getInstance().get(CostAdjustment.class,
          costAdjustmentList.get(2).getId());
      assertDocumentPost(costAdjustment3, product.getId(), documentPostAssertList3);

      OBDal.getInstance().commitAndClose();

    } catch (Exception e) {
      System.out.println(e.getMessage());
      throw new OBException(e);
    }

    finally {
      OBContext.restorePreviousMode();
    }
  }

  @Test
  public void testCostingMC444() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final BigDecimal price1 = new BigDecimal("444.00");
    final BigDecimal price2 = new BigDecimal("355.20");
    final BigDecimal price3 = new BigDecimal("177.60");
    final BigDecimal quantity1 = BigDecimal.ONE;
    final BigDecimal rate = new BigDecimal("0.80");

    try {

      OBContext.setOBContext(USER_ID, ROLE_ID, CLIENT_ID, ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product = createProduct("testCostingMC444", price1, CURRENCY2_ID);

      // Create purchase order and book it
      Order purchaseOrder = createPurchaseOrder(product, price1, quantity1, day0);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt = createGoodsReceipt(purchaseOrder, price1, quantity1, day1);

      // Create purchase invoice, post it and assert it
      createPurchaseInvoice(goodsReceipt, price1, quantity1, rate, day2);

      // Run price correction background
      runPriceBackground();

      // Assert product transactions
      List<ProductTransactionAssert> productTransactionAssertList1 = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList1.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price3, price2));
      assertProductTransaction(product.getId(), productTransactionAssertList1);

      // Assert product costing
      List<MaterialTransaction> transactionList = getProductTransactions(product.getId());
      List<ProductCostingAssert> productCostingAssertList1 = new ArrayList<ProductCostingAssert>();
      productCostingAssertList1.add(new ProductCostingAssert(transactionList.get(0), price2,
          price3, price2, quantity1));
      assertProductCosting(product.getId(), productCostingAssertList1);

      // Assert cost adjustment
      List<CostAdjustment> costAdjustmentList = getCostAdjustment(product.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList1 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList1.add(new CostAdjustmentAssert(transactionList.get(0), "PDC",
          quantity1.multiply(price2).add(quantity1.multiply(price3).negate()), day2, true));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList1);
      assertCostAdjustment(costAdjustmentList, costAdjustmentAssertList);

      // Post cost adjustment and assert it
      postDocument(costAdjustmentList.get(0));
      List<DocumentPostAssert> documentPostAssertList1 = new ArrayList<DocumentPostAssert>();
      documentPostAssertList1.add(new DocumentPostAssert("99904", BigDecimal.ZERO, quantity1
          .multiply(price2).add(quantity1.multiply(price3).negate()), null));
      documentPostAssertList1.add(new DocumentPostAssert("35000", quantity1.multiply(price2).add(
          quantity1.multiply(price3).negate()), BigDecimal.ZERO, null));
      CostAdjustment costAdjustment1 = OBDal.getInstance().get(CostAdjustment.class,
          costAdjustmentList.get(0).getId());
      assertDocumentPost(costAdjustment1, product.getId(), documentPostAssertList1);

      OBDal.getInstance().commitAndClose();

    } catch (Exception e) {
      System.out.println(e.getMessage());
      throw new OBException(e);
    }

    finally {
      OBContext.restorePreviousMode();
    }
  }

  @Test
  public void testCostingMC445() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final int day3 = 15;
    final int day4 = 20;
    final int day5 = 25;
    final BigDecimal price1 = new BigDecimal("500.00");
    final BigDecimal price2 = new BigDecimal("600.00");
    final BigDecimal price3 = new BigDecimal("700.00");
    final BigDecimal price4 = new BigDecimal("1500.00");
    final BigDecimal price5 = new BigDecimal("1750.00");
    final BigDecimal price6 = new BigDecimal("1375.00");
    final BigDecimal quantity1 = BigDecimal.ONE;

    try {

      OBContext.setOBContext(USER_ID, ROLE_ID, CLIENT_ID, ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product = createProduct("testCostingMC445", price1);

      // Create purchase order and book it
      Order purchaseOrder1 = createPurchaseOrder(product, price1, quantity1, day0);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt1 = createGoodsReceipt(purchaseOrder1, price1, quantity1, day1);

      // Create purchase invoice, post it and assert it
      createPurchaseInvoice(goodsReceipt1, price1, quantity1, day2);

      // Change organization currency
      changeOrganizationCurrency(ORGANIZATION_ID, CURRENCY2_ID);

      // Create purchase order and book it
      Order purchaseOrder2 = createPurchaseOrder(product, price2, quantity1, day3);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt2 = createGoodsReceipt(purchaseOrder2, price2, quantity1, day4);

      // Create purchase invoice, post it and assert it
      createPurchaseInvoice(goodsReceipt2, price3, quantity1, day5);

      // Run price correction background
      runPriceBackground();

      // Assert product transactions
      List<ProductTransactionAssert> productTransactionAssertList1 = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList1.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt1.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), CURRENCY1_ID, price1, price1));
      productTransactionAssertList1.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt2.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), CURRENCY2_ID, price4, price5));
      assertProductTransaction(product.getId(), productTransactionAssertList1);

      // Assert product costing
      List<MaterialTransaction> transactionList = getProductTransactions(product.getId());
      List<ProductCostingAssert> productCostingAssertList1 = new ArrayList<ProductCostingAssert>();
      productCostingAssertList1.add(new ProductCostingAssert(transactionList.get(0), price1, null,
          price1, quantity1));
      productCostingAssertList1.add(new ProductCostingAssert(transactionList.get(1), price5,
          price6, price4, quantity1.add(quantity1)));
      assertProductCosting(product.getId(), productCostingAssertList1);

      // Assert cost adjustment
      List<CostAdjustment> costAdjustmentList = getCostAdjustment(product.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList1 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList1.add(new CostAdjustmentAssert(transactionList.get(1),
          CURRENCY2_ID, "PDC", quantity1.multiply(price5).add(quantity1.multiply(price4).negate()),
          day5, true));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList1);
      assertCostAdjustment(costAdjustmentList, costAdjustmentAssertList);

      // Post cost adjustment and assert it
      postDocument(costAdjustmentList.get(0));
      List<DocumentPostAssert> documentPostAssertList1 = new ArrayList<DocumentPostAssert>();
      documentPostAssertList1.add(new DocumentPostAssert("99904", BigDecimal.ZERO, quantity1
          .multiply(price3).add(quantity1.multiply(price2).negate()), null));
      documentPostAssertList1.add(new DocumentPostAssert("35000", quantity1.multiply(price3).add(
          quantity1.multiply(price2).negate()), BigDecimal.ZERO, null));
      CostAdjustment costAdjustment1 = OBDal.getInstance().get(CostAdjustment.class,
          costAdjustmentList.get(0).getId());
      assertDocumentPost(costAdjustment1, product.getId(), documentPostAssertList1);

      OBDal.getInstance().commitAndClose();

    } catch (Exception e) {
      System.out.println(e.getMessage());
      throw new OBException(e);
    }

    finally {
      // Change organization currency
      changeOrganizationCurrency(ORGANIZATION_ID, CURRENCY1_ID);

      OBContext.restorePreviousMode();
    }
  }

  @Test
  public void testCostingE2() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final int day3 = 15;
    final int day4 = 20;
    final int day5 = 25;
    final int day6 = 30;
    final BigDecimal price1 = new BigDecimal("4.00");
    final BigDecimal price2 = new BigDecimal("3.00");
    final BigDecimal price3 = new BigDecimal("3.3333");
    final BigDecimal price4 = new BigDecimal("8.00");
    final BigDecimal price5 = new BigDecimal("7.99");
    final BigDecimal price6 = new BigDecimal("3.50");
    final BigDecimal price7 = new BigDecimal("8.01");
    final BigDecimal quantity1 = new BigDecimal("1");

    try {

      OBContext.setOBContext(USER_ID, ROLE_ID, CLIENT_ID, ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product = createProduct("testCostingE2", price1);

      // Create purchase order and book it
      Order purchaseOrder1 = createPurchaseOrder(product, price1, quantity1, day0);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt1 = createGoodsReceipt(purchaseOrder1, price1, quantity1,
          LOCATOR1_ID, day0);

      // Create purchase order and book it
      Order purchaseOrder2 = createPurchaseOrder(product, price2, quantity1, day1);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt2 = createGoodsReceipt(purchaseOrder2, price2, quantity1,
          LOCATOR2_ID, day1);

      // Create purchase order and book it
      Order purchaseOrder3 = createPurchaseOrder(product, price2, quantity1, day2);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt3 = createGoodsReceipt(purchaseOrder3, price2, quantity1,
          LOCATOR3_ID, day2);

      // Create goods shipment, run costing background, post it and assert it
      ShipmentInOut goodsShipment1 = createGoodsShipment(product, price3, quantity1, LOCATOR1_ID,
          day3);

      // Create goods shipment, run costing background, post it and assert it
      ShipmentInOut goodsShipment2 = createGoodsShipment(product, price3, quantity1, LOCATOR2_ID,
          day4);

      // Create goods shipment, run costing background, post it and assert it
      ShipmentInOut goodsShipment3 = createGoodsShipment(product, price3, quantity1, LOCATOR3_ID,
          day5);

      // Create purchase order and book it
      Order purchaseOrder4 = createPurchaseOrder(product, price4, quantity1, day6);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt4 = createGoodsReceipt(purchaseOrder4, price4, quantity1,
          LOCATOR1_ID, day6);

      // Assert product transactions
      List<ProductTransactionAssert> productTransactionAssertList = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt1.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price1));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt2.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price2, price2));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt3.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price2, price2));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsShipment1.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price3, price3));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsShipment2.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price3, price3));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsShipment3.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price3, price3));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt4.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price4, price5, price4));
      assertProductTransaction(product.getId(), productTransactionAssertList);

      // Assert product costing
      List<MaterialTransaction> transactionList = getProductTransactions(product.getId());
      List<ProductCostingAssert> productCostingAssertList = new ArrayList<ProductCostingAssert>();
      productCostingAssertList.add(new ProductCostingAssert(transactionList.get(0), price1, null,
          price1, quantity1));
      productCostingAssertList.add(new ProductCostingAssert(transactionList.get(1), price2, null,
          price6, quantity1.add(quantity1)));
      productCostingAssertList.add(new ProductCostingAssert(transactionList.get(2), price2, null,
          price3, quantity1.add(quantity1).add(quantity1)));
      productCostingAssertList.add(new ProductCostingAssert(transactionList.get(6), price4, price7,
          price4, quantity1));
      assertProductCosting(product.getId(), productCostingAssertList);

      // Assert cost adjustment
      List<CostAdjustment> costAdjustmentList = getCostAdjustment(product.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList1 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList1.add(new CostAdjustmentAssert(transactionList.get(6), "NSC",
          quantity1.multiply(price5).add(quantity1.multiply(price4).negate()), day6, true, false));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList1);
      assertCostAdjustment(costAdjustmentList, costAdjustmentAssertList);

      // Post cost adjustment and assert it
      postDocument(costAdjustmentList.get(0));
      List<DocumentPostAssert> documentPostAssertList1 = new ArrayList<DocumentPostAssert>();
      documentPostAssertList1.add(new DocumentPostAssert("61000", quantity1.multiply(price4).add(
          quantity1.multiply(price5).negate()), BigDecimal.ZERO, null));
      documentPostAssertList1.add(new DocumentPostAssert("35000", BigDecimal.ZERO, quantity1
          .multiply(price4).add(quantity1.multiply(price5).negate()), null));
      CostAdjustment costAdjustment1 = OBDal.getInstance().get(CostAdjustment.class,
          costAdjustmentList.get(0).getId());
      assertDocumentPost(costAdjustment1, product.getId(), documentPostAssertList1);

      OBDal.getInstance().commitAndClose();

    } catch (Exception e) {
      System.out.println(e.getMessage());
      throw new OBException(e);
    }

    finally {
      OBContext.restorePreviousMode();
    }
  }

  @Test
  public void testCostingE3() throws Exception {

    final int day0 = 0;
    final int day1 = 5;
    final int day2 = 10;
    final int day3 = 15;
    final int day4 = 20;
    final int day5 = 25;
    final BigDecimal price1 = new BigDecimal("4.00");
    final BigDecimal price2 = new BigDecimal("3.00");
    final BigDecimal price3 = new BigDecimal("3.3333");
    final BigDecimal price4 = new BigDecimal("3.34");
    final BigDecimal price5 = new BigDecimal("8.00");
    final BigDecimal price6 = new BigDecimal("3.34");
    final BigDecimal price7 = new BigDecimal("3.50");
    final BigDecimal quantity1 = new BigDecimal("1");

    try {

      OBContext.setOBContext(USER_ID, ROLE_ID, CLIENT_ID, ORGANIZATION_ID);
      OBContext.setAdminMode(true);

      // Create a new product for the test
      Product product = createProduct("testCostingE3", price1);

      // Create purchase order and book it
      Order purchaseOrder1 = createPurchaseOrder(product, price1, quantity1, day0);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt1 = createGoodsReceipt(purchaseOrder1, price1, quantity1, day0);

      // Create purchase order and book it
      Order purchaseOrder2 = createPurchaseOrder(product, price2, quantity1, day1);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt2 = createGoodsReceipt(purchaseOrder2, price2, quantity1, day1);

      // Create purchase order and book it
      Order purchaseOrder3 = createPurchaseOrder(product, price2, quantity1, day2);

      // Create goods receipt, run costing background, post it and assert it
      ShipmentInOut goodsReceipt3 = createGoodsReceipt(purchaseOrder3, price2, quantity1, day2);

      // Create goods shipment, run costing background, post it and assert it
      ShipmentInOut goodsShipment1 = createGoodsShipment(product, price3, quantity1, day3);

      // Create goods shipment, run costing background, post it and assert it
      ShipmentInOut goodsShipment2 = createGoodsShipment(product, price3, quantity1, day4);

      // Add sleep to avoid assert errors
      Thread.sleep(1000);

      // Create inventory amount update and run costing background
      InventoryAmountUpdate inventoryAmountUpdate = createInventoryAmountUpdate(product, price3,
          price4, price5, quantity1, day5);

      // Assert product transactions
      List<ProductTransactionAssert> productTransactionAssertList = new ArrayList<ProductTransactionAssert>();
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt1.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price1, price1));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt2.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price2, price2));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsReceipt3.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price2, price2));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsShipment1.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price3, price3));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(ShipmentInOut.class, goodsShipment2.getId()).getMaterialMgmtShipmentInOutLineList()
          .get(0), price3, price3));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(InventoryAmountUpdate.class, inventoryAmountUpdate.getId())
          .getInventoryAmountUpdateLineList().get(0), price3, price6, price3));
      productTransactionAssertList.add(new ProductTransactionAssert(OBDal.getInstance()
          .get(InventoryAmountUpdate.class, inventoryAmountUpdate.getId())
          .getInventoryAmountUpdateLineList().get(0), price5, price5, true));
      assertProductTransaction(product.getId(), productTransactionAssertList);

      // Assert product costing
      List<MaterialTransaction> transactionList = getProductTransactions(product.getId());
      List<ProductCostingAssert> productCostingAssertList = new ArrayList<ProductCostingAssert>();
      productCostingAssertList.add(new ProductCostingAssert(transactionList.get(0), price1, null,
          price1, quantity1));
      productCostingAssertList.add(new ProductCostingAssert(transactionList.get(1), price2, null,
          price7, quantity1.add(quantity1)));
      productCostingAssertList.add(new ProductCostingAssert(transactionList.get(2), price2, null,
          price3, quantity1.add(quantity1).add(quantity1)));
      productCostingAssertList.add(new ProductCostingAssert(transactionList.get(6), price5, null,
          price5, quantity1));
      assertProductCosting(product.getId(), productCostingAssertList);

      // Assert cost adjustment
      List<CostAdjustment> costAdjustmentList = getCostAdjustment(product.getId());
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList = new ArrayList<List<CostAdjustmentAssert>>();
      List<CostAdjustmentAssert> costAdjustmentAssertLineList1 = new ArrayList<CostAdjustmentAssert>();
      costAdjustmentAssertLineList1.add(new CostAdjustmentAssert(transactionList.get(5), "NSC",
          quantity1.multiply(price4).add(quantity1.multiply(price3).negate()), day5, true, false));
      costAdjustmentAssertList.add(costAdjustmentAssertLineList1);
      assertCostAdjustment(costAdjustmentList, costAdjustmentAssertList);

      // Post cost adjustment and assert it
      postDocument(costAdjustmentList.get(0));
      List<DocumentPostAssert> documentPostAssertList1 = new ArrayList<DocumentPostAssert>();
      documentPostAssertList1.add(new DocumentPostAssert("61000", BigDecimal.ZERO, quantity1
          .multiply(price4).add(quantity1.multiply(price3).negate()), null));
      documentPostAssertList1.add(new DocumentPostAssert("35000", quantity1.multiply(price4).add(
          quantity1.multiply(price3).negate()), BigDecimal.ZERO, null));
      CostAdjustment costAdjustment1 = OBDal.getInstance().get(CostAdjustment.class,
          costAdjustmentList.get(0).getId());
      assertDocumentPost(costAdjustment1, product.getId(), documentPostAssertList1);

      OBDal.getInstance().commitAndClose();

    } catch (Exception e) {
      System.out.println(e.getMessage());
      throw new OBException(e);
    }

    finally {
      OBContext.restorePreviousMode();
    }
  }

  /********************************************** General methods for tests **********************************************/

  // Create a Product cloning a created one
  private Product createProduct(String name, BigDecimal purchasePrice) {
    return createProduct(name, purchasePrice, purchasePrice);
  }

  // Create a Product cloning a created one
  private Product createProduct(String name, BigDecimal purchasePrice, String currencyId) {
    return createProduct(name, "I", purchasePrice, purchasePrice, null, null, 0, currencyId, null,
        null);
  }

  // Create a Product cloning a created one
  private Product createProduct(String name, BigDecimal purchasePrice, BigDecimal salesPrice) {
    return createProduct(name, "I", purchasePrice, salesPrice, null, null, 0, CURRENCY1_ID, null,
        null);
  }

  // Create a Product cloning a created one
  private Product createProduct(String name, BigDecimal purchasePrice, BigDecimal cost,
      String costType) {
    return createProduct(name, purchasePrice, cost, costType, 0);
  }

  // Create a Product cloning a created one
  private Product createProduct(String name, BigDecimal purchasePrice, BigDecimal cost,
      String costType, int year) {
    return createProduct(name, "I", purchasePrice, purchasePrice, cost, costType, year,
        CURRENCY1_ID, null, null);
  }

  // Create a Product cloning a created one
  private Product createProduct(String name, String productType, BigDecimal purchasePrice,
      BigDecimal cost, String costType, int year) {
    return createProduct(name, productType, purchasePrice, purchasePrice, cost, costType, year,
        CURRENCY1_ID, null, null);
  }

  // Create a Product cloning a created one
  private Product createProduct(String name, List<Product> productList,
      List<BigDecimal> quantityList) {
    return createProduct(name, null, null, null, null, null, 0, CURRENCY1_ID, productList,
        quantityList);
  }

  // Create a Product cloning a created one
  private Product createProduct(String name, String productType, BigDecimal purchasePrice,
      BigDecimal salesPrice, BigDecimal cost, String costType, int year, String currencyId,
      List<Product> productList, List<BigDecimal> quantityList) {
    List<String> productIdList = new ArrayList<String>();
    if (productList != null)
      for (Product product : productList)
        productIdList.add(product.getId());
    return cloneProduct(name, getNumberOfCostingProducts(name) + 1, productType, purchasePrice,
        salesPrice, cost, costType, year, currencyId, productIdList, quantityList);
  }

  // Create a Purchase Order cloning a created one and book it
  private Order createPurchaseOrder(Product product, BigDecimal price, BigDecimal quantity, int day) {
    try {
      Order purchaseOrder = cloneOrder(product.getId(), false, price, quantity, day);
      bookOrder(purchaseOrder);
      return purchaseOrder;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Create a Sales Order cloning a created one and book it
  private Order createSalesOrder(Product product, BigDecimal price, BigDecimal quantity, int day) {
    try {
      Order salesOrder = cloneOrder(product.getId(), true, price, quantity, day);
      bookOrder(salesOrder);
      return salesOrder;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Reactivate purchase order, update product price and book it
  private void updatePurchaseOrder(Order order, BigDecimal price) {
    try {
      Order purchaseOrder = reactivateOrder(order);
      purchaseOrder = updateOrderProductPrice(purchaseOrder, price);
      bookOrder(purchaseOrder);
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Create a Purchase Invoice cloning a created one, complete it and post it
  private Invoice createPurchaseInvoice(Product product, BigDecimal price, BigDecimal quantity,
      int day) {
    try {
      Invoice purchaseInvoice = cloneInvoice(product.getId(), false, price, quantity, day);
      return postPurchaseInvoice(purchaseInvoice, product.getId(), price, quantity);
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Create a Purchase Invoice from a purchase order, complete it and post it
  private Invoice createPurchaseInvoice(Order purchaseOrder, BigDecimal price, BigDecimal quantity,
      int day) {
    try {
      Invoice purchaseInvoice = createInvoiceFromOrder(purchaseOrder.getId(), false, price,
          quantity, day);
      String productId = purchaseInvoice.getInvoiceLineList().get(0).getProduct().getId();
      return postPurchaseInvoice(purchaseInvoice, productId, price, quantity);
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Create a Purchase Invoice from a goods receipt, complete it and post it
  private Invoice createPurchaseInvoice(ShipmentInOut goodsReceipt, BigDecimal price,
      BigDecimal quantity, int day) {
    try {
      return createPurchaseInvoice(goodsReceipt, price, quantity, null, day);
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Create a Purchase Invoice from a goods receipt, complete it and post it
  private Invoice createPurchaseInvoice(ShipmentInOut goodsReceipt, BigDecimal price,
      BigDecimal quantity, BigDecimal conversion, int day) {
    try {
      Invoice purchaseInvoice = createInvoiceFromMovement(goodsReceipt.getId(), false, price,
          quantity, day);
      if (conversion != null)
        createConversion(purchaseInvoice, conversion);
      String productId = purchaseInvoice.getInvoiceLineList().get(0).getProduct().getId();
      return postPurchaseInvoice(purchaseInvoice, productId, price, quantity);
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Create a Purchase Invoice from many purchase orders, complete it and post it
  private Invoice createPurchaseInvoice(List<Order> purchaseOrderList, List<BigDecimal> priceList,
      List<BigDecimal> quantityList, int day) {
    try {
      List<String> purchaseOrderIdList = new ArrayList<String>();
      for (Order purchaseOrder : purchaseOrderList)
        purchaseOrderIdList.add(purchaseOrder.getId());

      Invoice purchaseInvoice = createInvoiceFromOrders(purchaseOrderIdList, false, priceList,
          quantityList, day);
      String productId = purchaseInvoice.getInvoiceLineList().get(0).getProduct().getId();
      return postPurchaseInvoice(purchaseInvoice, productId,
          getAveragePrice(priceList, quantityList), getTotalQuantity(quantityList));
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Create a Purchase Invoice from many goods receipts, complete it and post it
  private Invoice createPurchaseInvoice(List<ShipmentInOut> goodsReceiptList,
      List<BigDecimal> priceList, BigDecimal quantity, int day) {
    try {
      List<String> goodsReceipIdtList = new ArrayList<String>();
      List<BigDecimal> quantityList = new ArrayList<BigDecimal>();
      for (ShipmentInOut goodsReceipt : goodsReceiptList) {
        goodsReceipIdtList.add(goodsReceipt.getId());
        quantityList.add(goodsReceipt.getMaterialMgmtShipmentInOutLineList().get(0)
            .getMovementQuantity());
      }
      Invoice purchaseInvoice = createInvoiceFromMovements(goodsReceipIdtList, false, priceList,
          quantity, day);
      String productId = purchaseInvoice.getInvoiceLineList().get(0).getProduct().getId();

      return postPurchaseInvoice(purchaseInvoice, productId,
          getAveragePrice(priceList, quantityList), quantity);
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Reactivate purchase order, update product price and book it
  private void updatePurchaseInvoice(Invoice invoice, BigDecimal price) {
    try {
      Invoice purchaseInvoice = reactivateInvoice(invoice);
      purchaseInvoice = updateInvoiceProductPrice(purchaseInvoice, price);
      InvoiceLine purchaseInvoiceLine = purchaseInvoice.getInvoiceLineList().get(0);
      String productId = purchaseInvoiceLine.getProduct().getId();
      postPurchaseInvoice(purchaseInvoice, productId, price,
          purchaseInvoiceLine.getInvoicedQuantity(), false);
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Complete a Purchase Invoice and post it
  private Invoice postPurchaseInvoice(Invoice purchaseInvoice, String productId, BigDecimal price,
      BigDecimal quantity) {
    try {
      return postPurchaseInvoice(purchaseInvoice, productId, price, quantity, true);
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Complete a Purchase Invoice and post it
  private Invoice postPurchaseInvoice(Invoice purchaseInvoice, String productId, BigDecimal price,
      BigDecimal quantity, boolean assertMatchedInvoice) {
    try {
      completeDocument(purchaseInvoice);
      OBDal.getInstance().commitAndClose();
      postDocument(purchaseInvoice);
      Invoice invoice = OBDal.getInstance().get(Invoice.class, purchaseInvoice.getId());
      List<DocumentPostAssert> documentPostAssertList = new ArrayList<DocumentPostAssert>();
      documentPostAssertList.add(new DocumentPostAssert("40000", BigDecimal.ZERO, quantity
          .multiply(price), null));
      for (InvoiceLine purchaseInvoiceLine : invoice.getInvoiceLineList())
        documentPostAssertList.add(new DocumentPostAssert(purchaseInvoiceLine.getProduct().getId(),
            "60000", purchaseInvoiceLine.getInvoicedQuantity().multiply(
                purchaseInvoiceLine.getUnitPrice()), BigDecimal.ZERO, purchaseInvoiceLine
                .getInvoicedQuantity()));
      assertDocumentPost(invoice, null, documentPostAssertList);

      if (invoice.getInvoiceLineList().get(0).getGoodsShipmentLine() != null
          && assertMatchedInvoice)
        for (InvoiceLine purchaseInvoiceLine : invoice.getInvoiceLineList()) {
          purchaseInvoiceLine = OBDal.getInstance().get(InvoiceLine.class,
              purchaseInvoiceLine.getId());
          postMatchedPurchaseInvoice(purchaseInvoiceLine,
              purchaseInvoiceLine.getGoodsShipmentLine());
        }
      return invoice;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Create a Goods Receipt cloning a created one, complete it and post it
  private ShipmentInOut createGoodsReceipt(Product product, BigDecimal price, BigDecimal quantity,
      int day) {
    try {
      return createGoodsReceipt(product, price, quantity, LOCATOR1_ID, day);
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Create a Goods Receipt cloning a created one, complete it and post it
  private ShipmentInOut createGoodsReceipt(Product product, BigDecimal price, BigDecimal quantity,
      String locatorId, int day) {
    try {
      ShipmentInOut goodsReceipt = cloneMovement(product.getId(), false, quantity, locatorId, day);
      return postGoodsReceipt(goodsReceipt, product.getId(), price, quantity);
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Create a Goods Receipt from a purchase order, complete it and post it
  private ShipmentInOut createGoodsReceipt(Order purchaseOrder, BigDecimal price,
      BigDecimal quantity, int day) {
    try {
      return createGoodsReceipt(purchaseOrder, price, quantity, LOCATOR1_ID, day);
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Create a Goods Receipt from a purchase order, complete it and post it
  private ShipmentInOut createGoodsReceipt(Order purchaseOrder, BigDecimal price,
      BigDecimal quantity, String locatorId, int day) {
    try {
      ShipmentInOut goodsReceipt = createMovementFromOrder(purchaseOrder.getId(), false, quantity,
          locatorId, day);
      String productId = goodsReceipt.getMaterialMgmtShipmentInOutLineList().get(0).getProduct()
          .getId();
      return postGoodsReceipt(goodsReceipt, productId, price, quantity);
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Create a Goods Receipt from many purchase orders, complete it and post it
  private ShipmentInOut createGoodsReceipt(List<Order> purchaseOrderList, BigDecimal price,
      BigDecimal quantity, int day) {
    try {
      return createGoodsReceipt(purchaseOrderList, price, quantity, LOCATOR1_ID, day, null);
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Create a Goods Receipt from many purchase orders, complete it and post it
  private ShipmentInOut createGoodsReceipt(List<Order> purchaseOrderList, BigDecimal price,
      BigDecimal quantity, int day, List<Invoice> invoiceList) {
    try {
      return createGoodsReceipt(purchaseOrderList, price, quantity, LOCATOR1_ID, day, invoiceList);
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Create a Goods Receipt from many purchase orders, complete it and post it
  private ShipmentInOut createGoodsReceipt(List<Order> purchaseOrderList, BigDecimal price,
      BigDecimal quantity, String locatorId, int day, List<Invoice> invoiceList) {
    try {
      List<String> purchaseOrderIdList = new ArrayList<String>();
      for (Order purchaseOrder : purchaseOrderList)
        purchaseOrderIdList.add(purchaseOrder.getId());

      ShipmentInOut goodsReceipt = createMovementFromOrders(purchaseOrderIdList, false, quantity,
          locatorId, day);
      String productId = goodsReceipt.getMaterialMgmtShipmentInOutLineList().get(0).getProduct()
          .getId();
      if (invoiceList != null)
        createLandedCostCost(invoiceList, goodsReceipt);
      return postGoodsReceipt(goodsReceipt, productId, price, quantity);
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Create a Goods Receipt from a purchase invoice, complete it and post it
  private ShipmentInOut createGoodsReceipt(Invoice purchaseInvoice, BigDecimal price,
      BigDecimal quantity, int day) {
    try {
      return createGoodsReceipt(purchaseInvoice, price, quantity, LOCATOR1_ID, day);
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Create a Goods Receipt from a purchase invoice, complete it and post it
  private ShipmentInOut createGoodsReceipt(Invoice purchaseInvoice, BigDecimal price,
      BigDecimal quantity, String locatorId, int day) {
    try {
      ShipmentInOut goodsReceipt = createMovementFromInvoice(purchaseInvoice.getId(), false,
          quantity, locatorId, day);
      String productId = goodsReceipt.getMaterialMgmtShipmentInOutLineList().get(0).getProduct()
          .getId();
      return postGoodsReceipt(goodsReceipt, productId, price, quantity);
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Complete a Goods Receipt and post it
  private ShipmentInOut postGoodsReceipt(ShipmentInOut goodsReceipt, String productId,
      BigDecimal price, BigDecimal quantity) {
    try {
      completeDocument(goodsReceipt);
      runCostingBackground();
      ShipmentInOut receipt = OBDal.getInstance().get(ShipmentInOut.class, goodsReceipt.getId());
      postDocument(receipt);
      receipt = OBDal.getInstance().get(ShipmentInOut.class, goodsReceipt.getId());
      List<DocumentPostAssert> documentPostAssertList = new ArrayList<DocumentPostAssert>();
      for (ShipmentInOutLine goodsReceiptLine : receipt.getMaterialMgmtShipmentInOutLineList()) {
        if (receipt.getMaterialMgmtShipmentInOutLineList().size() == 1) {
          documentPostAssertList.add(new DocumentPostAssert(goodsReceiptLine.getProduct().getId(),
              "35000", goodsReceiptLine.getMovementQuantity().multiply(price), BigDecimal.ZERO,
              goodsReceiptLine.getMovementQuantity()));
          documentPostAssertList.add(new DocumentPostAssert(goodsReceiptLine.getProduct().getId(),
              "40090", BigDecimal.ZERO, goodsReceiptLine.getMovementQuantity().multiply(price),
              goodsReceiptLine.getMovementQuantity()));
        } else {
          documentPostAssertList.add(new DocumentPostAssert(goodsReceiptLine.getProduct().getId(),
              "35000", goodsReceiptLine.getMovementQuantity().multiply(
                  goodsReceiptLine.getSalesOrderLine().getUnitPrice()), BigDecimal.ZERO,
              goodsReceiptLine.getMovementQuantity()));
          documentPostAssertList.add(new DocumentPostAssert(goodsReceiptLine.getProduct().getId(),
              "40090", BigDecimal.ZERO, goodsReceiptLine.getMovementQuantity().multiply(
                  goodsReceiptLine.getSalesOrderLine().getUnitPrice()), goodsReceiptLine
                  .getMovementQuantity()));
        }
      }
      assertDocumentPost(receipt, null, documentPostAssertList);

      if (receipt.getInvoice() != null) {
        int i = 0;
        for (InvoiceLine purchaseInvoiceLine : receipt.getInvoice().getInvoiceLineList()) {
          postMatchedPurchaseInvoice(purchaseInvoiceLine, receipt
              .getMaterialMgmtShipmentInOutLineList().get(i));
          i++;
        }
      }
      return receipt;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Assert and post Purchase Invoice Matched Invoices
  private void postMatchedPurchaseInvoice(InvoiceLine purchaseInvoiceLine,
      ShipmentInOutLine goodsReceiptLine) {
    try {
      OBCriteria<ReceiptInvoiceMatch> criteria1 = OBDal.getInstance().createCriteria(
          ReceiptInvoiceMatch.class);
      criteria1.add(Restrictions.eq(ReceiptInvoiceMatch.PROPERTY_INVOICELINE, purchaseInvoiceLine));
      criteria1.add(Restrictions.eq(ReceiptInvoiceMatch.PROPERTY_GOODSSHIPMENTLINE,
          goodsReceiptLine));
      ReceiptInvoiceMatch receiptInvoiceMatch = criteria1.list().get(0);
      assertMatchedInvoice(receiptInvoiceMatch, new MatchedInvoicesAssert(purchaseInvoiceLine,
          goodsReceiptLine));

      postDocument(receiptInvoiceMatch);
      receiptInvoiceMatch = OBDal.getInstance().get(ReceiptInvoiceMatch.class,
          receiptInvoiceMatch.getId());

      BigDecimal invoicePrice = OBDal.getInstance()
          .get(Invoice.class, purchaseInvoiceLine.getInvoice().getId())
          .getCurrencyConversionRateDocList().size() == 0 ? purchaseInvoiceLine.getUnitPrice()
          : purchaseInvoiceLine.getUnitPrice().multiply(
              OBDal.getInstance().get(Invoice.class, purchaseInvoiceLine.getInvoice().getId())
                  .getCurrencyConversionRateDocList().get(0).getRate());
      OBCriteria<AccountingFact> criteria2 = OBDal.getInstance().createCriteria(
          AccountingFact.class);
      criteria2.add(Restrictions.eq(AccountingFact.PROPERTY_RECORDID, goodsReceiptLine
          .getShipmentReceipt().getId()));
      criteria2.add(Restrictions.eq(AccountingFact.PROPERTY_LINEID, goodsReceiptLine.getId()));
      criteria2.addOrderBy(AccountingFact.PROPERTY_SEQUENCENUMBER, true);
      BigDecimal receiptPrice = criteria2.list().get(0).getDebit()
          .divide(receiptInvoiceMatch.getQuantity());

      List<DocumentPostAssert> documentPostAssertList = new ArrayList<DocumentPostAssert>();
      documentPostAssertList.add(new DocumentPostAssert("40090", receiptPrice
          .multiply(receiptInvoiceMatch.getQuantity()), BigDecimal.ZERO, goodsReceiptLine
          .getMovementQuantity()));
      documentPostAssertList.add(new DocumentPostAssert("60000", BigDecimal.ZERO, invoicePrice
          .multiply(receiptInvoiceMatch.getQuantity()), purchaseInvoiceLine.getInvoicedQuantity()));
      if (!invoicePrice.equals(receiptPrice))
        if (invoicePrice.compareTo(receiptPrice) > 0)
          documentPostAssertList.add(new DocumentPostAssert("99904", invoicePrice.multiply(
              receiptInvoiceMatch.getQuantity()).add(
              receiptPrice.multiply(receiptInvoiceMatch.getQuantity()).negate()), BigDecimal.ZERO,
              goodsReceiptLine.getMovementQuantity()));
        else
          documentPostAssertList.add(new DocumentPostAssert("99904", BigDecimal.ZERO, receiptPrice
              .multiply(receiptInvoiceMatch.getQuantity()).add(
                  invoicePrice.multiply(receiptInvoiceMatch.getQuantity()).negate()),
              goodsReceiptLine.getMovementQuantity()));
      assertDocumentPost(receiptInvoiceMatch, purchaseInvoiceLine.getProduct().getId(),
          documentPostAssertList);
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Create a Goods Shipment cloning a created one, complete it and post it
  private ShipmentInOut createGoodsShipment(Product product, BigDecimal price, BigDecimal quantity,
      int day) {
    try {
      return createGoodsShipment(product, price, quantity, LOCATOR1_ID, day);
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Create a Goods Shipment cloning a created one, complete it and post it
  private ShipmentInOut createGoodsShipment(Product product, BigDecimal price, BigDecimal quantity,
      String locatorId, int day) {
    try {
      ShipmentInOut goodsShipment = cloneMovement(product.getId(), true, quantity, locatorId, day);
      return postGoodsShipment(goodsShipment, product.getId(), price, quantity);
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Create a Goods Receipt from a purchase order, complete it and post it
  private ShipmentInOut createGoodsShipment(Order salesOrder, BigDecimal price,
      BigDecimal quantity, int day) {
    try {
      return createGoodsShipment(salesOrder, price, quantity, LOCATOR1_ID, day);
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Create a Goods Receipt from a purchase order, complete it and post it
  private ShipmentInOut createGoodsShipment(Order salesOrder, BigDecimal price,
      BigDecimal quantity, String locatorId, int day) {
    try {
      ShipmentInOut goodsShipment = createMovementFromOrder(salesOrder.getId(), true, quantity,
          locatorId, day);
      String productId = goodsShipment.getMaterialMgmtShipmentInOutLineList().get(0).getProduct()
          .getId();
      return postGoodsShipment(goodsShipment, productId, price, quantity);
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Complete a Goods Receipt and post it
  private ShipmentInOut postGoodsShipment(ShipmentInOut goodsShipment, String productId,
      BigDecimal price, BigDecimal quantity) {
    try {
      completeDocument(goodsShipment);
      runCostingBackground();
      ShipmentInOut shipment = OBDal.getInstance().get(ShipmentInOut.class, goodsShipment.getId());
      postDocument(shipment);
      shipment = OBDal.getInstance().get(ShipmentInOut.class, goodsShipment.getId());
      List<DocumentPostAssert> documentPostAssertList = new ArrayList<DocumentPostAssert>();
      for (ShipmentInOutLine goodsShipmentLine : shipment.getMaterialMgmtShipmentInOutLineList()) {
        if (shipment.getMaterialMgmtShipmentInOutLineList().size() == 1) {
          documentPostAssertList.add(new DocumentPostAssert("99900", goodsShipmentLine
              .getMovementQuantity().multiply(price), BigDecimal.ZERO, goodsShipmentLine
              .getMovementQuantity()));
          documentPostAssertList.add(new DocumentPostAssert("35000", BigDecimal.ZERO,
              goodsShipmentLine.getMovementQuantity().multiply(price), goodsShipmentLine
                  .getMovementQuantity()));
        } else {
          documentPostAssertList.add(new DocumentPostAssert("99900",
              goodsShipmentLine.getMovementQuantity().multiply(
                  goodsShipmentLine.getSalesOrderLine().getUnitPrice()), BigDecimal.ZERO,
              goodsShipmentLine.getMovementQuantity()));
          documentPostAssertList.add(new DocumentPostAssert("35000", BigDecimal.ZERO,
              goodsShipmentLine.getMovementQuantity().multiply(
                  goodsShipmentLine.getSalesOrderLine().getUnitPrice()), goodsShipmentLine
                  .getMovementQuantity()));
        }
      }
      assertDocumentPost(shipment, productId, documentPostAssertList);
      return shipment;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Cancel Goods Receipt
  private ShipmentInOut cancelGoodsReceipt(ShipmentInOut goodsReceipt, BigDecimal price) {
    try {
      ShipmentInOut gReceipt = OBDal.getInstance().get(ShipmentInOut.class, goodsReceipt.getId());
      gReceipt.setDocumentAction("RC");
      OBDal.getInstance().save(gReceipt);
      OBDal.getInstance().flush();
      OBDal.getInstance().commitAndClose();
      gReceipt = OBDal.getInstance().get(ShipmentInOut.class, gReceipt.getId());
      OBDal.getInstance().refresh(gReceipt);
      gReceipt = (ShipmentInOut) completeDocument(gReceipt);
      OBDal.getInstance().refresh(gReceipt);

      runCostingBackground();
      ShipmentInOut receipt = OBDal.getInstance().get(ShipmentInOut.class, gReceipt.getId())
          .getMaterialMgmtShipmentInOutLineList().get(0)
          .getMaterialMgmtShipmentInOutLineCanceledInoutLineList().get(0).getShipmentReceipt();
      String productId = receipt.getMaterialMgmtShipmentInOutLineList().get(0).getProduct().getId();

      postDocument(receipt);
      receipt = OBDal.getInstance().get(ShipmentInOut.class, receipt.getId());
      List<DocumentPostAssert> documentPostAssertList = new ArrayList<DocumentPostAssert>();
      for (ShipmentInOutLine goodsReceiptLine : receipt.getMaterialMgmtShipmentInOutLineList()) {
        documentPostAssertList.add(new DocumentPostAssert("35000", BigDecimal.ZERO,
            goodsReceiptLine.getMovementQuantity().negate().multiply(price), goodsReceiptLine
                .getMovementQuantity()));
        documentPostAssertList.add(new DocumentPostAssert("40090", goodsReceiptLine
            .getMovementQuantity().negate().multiply(price), BigDecimal.ZERO, goodsReceiptLine
            .getMovementQuantity()));
      }
      assertDocumentPost(receipt, productId, documentPostAssertList);
      return receipt;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Cancel Goods Shipment
  private ShipmentInOut cancelGoodsShipment(ShipmentInOut goodsShipment, BigDecimal price) {
    try {
      ShipmentInOut gShipment = OBDal.getInstance().get(ShipmentInOut.class, goodsShipment.getId());
      gShipment.setDocumentAction("RC");
      OBDal.getInstance().save(gShipment);
      OBDal.getInstance().flush();
      OBDal.getInstance().commitAndClose();
      gShipment = OBDal.getInstance().get(ShipmentInOut.class, gShipment.getId());
      OBDal.getInstance().refresh(gShipment);
      gShipment = (ShipmentInOut) completeDocument(gShipment);
      OBDal.getInstance().refresh(gShipment);

      runCostingBackground();
      ShipmentInOut shipment = OBDal.getInstance().get(ShipmentInOut.class, gShipment.getId())
          .getMaterialMgmtShipmentInOutLineList().get(0)
          .getMaterialMgmtShipmentInOutLineCanceledInoutLineList().get(0).getShipmentReceipt();
      String productId = shipment.getMaterialMgmtShipmentInOutLineList().get(0).getProduct()
          .getId();

      postDocument(shipment);
      shipment = OBDal.getInstance().get(ShipmentInOut.class, shipment.getId());
      List<DocumentPostAssert> documentPostAssertList = new ArrayList<DocumentPostAssert>();
      for (ShipmentInOutLine goodsShipmentLine : shipment.getMaterialMgmtShipmentInOutLineList()) {
        documentPostAssertList.add(new DocumentPostAssert("99900", BigDecimal.ZERO,
            goodsShipmentLine.getMovementQuantity().negate().multiply(price), goodsShipmentLine
                .getMovementQuantity()));
        documentPostAssertList.add(new DocumentPostAssert("35000", goodsShipmentLine
            .getMovementQuantity().negate().multiply(price), BigDecimal.ZERO, goodsShipmentLine
            .getMovementQuantity()));
      }
      assertDocumentPost(shipment, productId, documentPostAssertList);
      return shipment;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Create a Goods Movement, complete it and post it
  private InternalMovement createGoodsMovement(Product product, BigDecimal price,
      BigDecimal quantity, String locatorFromId, String locatorToId, int day) {
    try {
      InternalMovement goodsMovement = createGoodsMovement(product.getId(), quantity,
          locatorFromId, locatorToId, day);
      OBDal.getInstance().commitAndClose();
      completeDocument(goodsMovement, PROCESSMOVEMENT_PROCESS_ID);
      goodsMovement.setProcessed(true);
      runCostingBackground();
      InternalMovement movement = OBDal.getInstance().get(InternalMovement.class,
          goodsMovement.getId());
      postDocument(movement);
      movement = OBDal.getInstance().get(InternalMovement.class, goodsMovement.getId());
      List<DocumentPostAssert> documentPostAssertList = new ArrayList<DocumentPostAssert>();
      documentPostAssertList.add(new DocumentPostAssert("35000", BigDecimal.ZERO, quantity
          .multiply(price), quantity));
      documentPostAssertList.add(new DocumentPostAssert("35000", quantity.multiply(price),
          BigDecimal.ZERO, quantity));
      assertDocumentPost(movement, product.getId(), documentPostAssertList);
      return movement;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Create a Internal Consumption, complete it and post it
  private InternalConsumption createInternalConsumption(Product product, BigDecimal price,
      BigDecimal quantity, int day) {
    try {
      return createInternalConsumption(product, price, quantity, LOCATOR1_ID, day);
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Create a Internal Consumption, complete it and post it
  private InternalConsumption createInternalConsumption(Product product, BigDecimal price,
      BigDecimal quantity, String locatorId, int day) {
    try {
      InternalConsumption internalConsumption = createInternalConsumption(product.getId(),
          quantity, locatorId, day);
      OBDal.getInstance().commitAndClose();
      completeDocument(internalConsumption, PROCESSCONSUMPTION_PROCESS_ID);
      internalConsumption.setProcessed(true);
      internalConsumption.setStatus("CO");
      runCostingBackground();
      InternalConsumption consumption = OBDal.getInstance().get(InternalConsumption.class,
          internalConsumption.getId());
      postDocument(consumption);
      consumption = OBDal.getInstance().get(InternalConsumption.class, consumption.getId());
      List<DocumentPostAssert> documentPostAssertList = new ArrayList<DocumentPostAssert>();
      documentPostAssertList.add(new DocumentPostAssert("99900", quantity.multiply(price),
          BigDecimal.ZERO, quantity));
      documentPostAssertList.add(new DocumentPostAssert("35000", BigDecimal.ZERO, quantity
          .multiply(price), quantity));
      assertDocumentPost(consumption, product.getId(), documentPostAssertList);
      return consumption;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Cancel a internal consumption
  private InternalConsumption cancelInternalConsumption(InternalConsumption internalConsumption) {
    try {
      cancelInternalConsumption(internalConsumption.getId());
      internalConsumption.setStatus("VO");
      runCostingBackground();
      return internalConsumption;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Create a Inventory Amount Update and process it
  private InventoryAmountUpdate createInventoryAmountUpdate(Product product,
      BigDecimal originalPrice, BigDecimal finalPrice, BigDecimal quantity, int day) {
    try {
      return createInventoryAmountUpdate(product, originalPrice, originalPrice, finalPrice,
          quantity, day);
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Create a Inventory Amount Update and process it
  private InventoryAmountUpdate createInventoryAmountUpdate(Product product, BigDecimal cost,
      BigDecimal originalPrice, BigDecimal finalPrice, BigDecimal quantity, int day) {
    try {
      InventoryAmountUpdate inventoryAmountUpdate = createInventoryAmountUpdate(product.getId(),
          originalPrice, finalPrice, quantity, day);
      processInventoryAmountUpdate(inventoryAmountUpdate.getId());
      runCostingBackground();

      List<InventoryCount> inventoryCountList = getPhysicalInventory(inventoryAmountUpdate.getId());
      assertPhysicalInventory(inventoryCountList, new PhysicalInventoryAssert(product, finalPrice,
          quantity, day));

      postDocument(inventoryCountList.get(0));
      List<DocumentPostAssert> documentPostAssertList1 = new ArrayList<DocumentPostAssert>();
      documentPostAssertList1.add(new DocumentPostAssert("35000", BigDecimal.ZERO, quantity
          .multiply(cost), quantity.negate()));
      documentPostAssertList1.add(new DocumentPostAssert("61000", quantity.multiply(cost),
          BigDecimal.ZERO, quantity.negate()));
      assertDocumentPost(inventoryCountList.get(0), product.getId(), documentPostAssertList1);
      postDocument(inventoryCountList.get(1));
      List<DocumentPostAssert> documentPostAssertList2 = new ArrayList<DocumentPostAssert>();
      documentPostAssertList2.add(new DocumentPostAssert("35000", quantity.multiply(finalPrice),
          BigDecimal.ZERO, quantity));
      documentPostAssertList2.add(new DocumentPostAssert("61000", BigDecimal.ZERO, quantity
          .multiply(finalPrice), quantity));
      assertDocumentPost(inventoryCountList.get(1), product.getId(), documentPostAssertList2);
      return inventoryAmountUpdate;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Create a Inventory Amount Update and process it
  private ProductionTransaction createBillOfMaterialsProduction(Product product,
      BigDecimal quantity, String locatorId, int day) {
    try {
      ProductionTransaction billOfMaterialsProduction = createBillOfMaterialsProduction(
          product.getId(), quantity, locatorId, day);
      processBillOfMaterialsProduction(billOfMaterialsProduction);
      billOfMaterialsProduction.setRecordsCreated(true);
      OBDal.getInstance().save(billOfMaterialsProduction);
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(billOfMaterialsProduction);
      OBDal.getInstance().commitAndClose();
      processBillOfMaterialsProduction(billOfMaterialsProduction);
      billOfMaterialsProduction.setProcessed(true);
      OBDal.getInstance().save(billOfMaterialsProduction);
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(billOfMaterialsProduction);
      OBDal.getInstance().commitAndClose();
      runCostingBackground();

      ProductionTransaction bMaterialsProduction = OBDal.getInstance().get(
          ProductionTransaction.class, billOfMaterialsProduction.getId());
      postDocument(bMaterialsProduction);
      List<DocumentPostAssert> documentPostAssertList1 = new ArrayList<DocumentPostAssert>();
      List<ProductionLine> productionLinesList = getProductionLines(billOfMaterialsProduction
          .getId());
      productionLinesList.add(0, productionLinesList.get(productionLinesList.size() - 1));
      productionLinesList.remove(productionLinesList.size() - 1);

      final OBCriteria<AccountingFact> criteria1 = OBDal.getInstance().createCriteria(
          AccountingFact.class);
      criteria1.add(Restrictions.eq(AccountingFact.PROPERTY_RECORDID,
          billOfMaterialsProduction.getId()));
      criteria1.addOrderBy(AccountingFact.PROPERTY_SEQUENCENUMBER, true);

      if (!criteria1.list().get(2).getQuantity()
          .equals(productionLinesList.get(1).getMovementQuantity())) {
        productionLinesList.add(1, productionLinesList.get(productionLinesList.size() - 1));
        productionLinesList.remove(productionLinesList.size() - 1);
      }

      int i = 0;
      for (ProductionLine productionLine : productionLinesList) {
        BigDecimal amountTotal = BigDecimal.ZERO;

        if (i == 0) {
          OBCriteria<ProductBOM> criteria2 = OBDal.getInstance().createCriteria(ProductBOM.class);
          criteria2.add(Restrictions.eq(ProductBOM.PROPERTY_PRODUCT, productionLine.getProduct()));
          for (ProductBOM productBOM : criteria2.list()) {
            amountTotal = amountTotal.add(productBOM.getBOMQuantity().multiply(
                productBOM.getBOMProduct().getPricingProductPriceList().get(0).getStandardPrice()));
          }
          amountTotal = amountTotal.multiply(productionLine.getMovementQuantity());
          documentPostAssertList1.add(new DocumentPostAssert(productionLine.getProduct().getId(),
              "35000", amountTotal, BigDecimal.ZERO, productionLine.getMovementQuantity()));
          documentPostAssertList1.add(new DocumentPostAssert(productionLine.getProduct().getId(),
              "61000", BigDecimal.ZERO, amountTotal, productionLine.getMovementQuantity()));
        }

        else {
          amountTotal = amountTotal.add(productionLine
              .getMovementQuantity()
              .negate()
              .multiply(
                  productionLine.getProduct().getPricingProductPriceList().get(0)
                      .getStandardPrice()));
          documentPostAssertList1.add(new DocumentPostAssert(productionLine.getProduct().getId(),
              "35000", BigDecimal.ZERO, amountTotal, productionLine.getMovementQuantity()));
          documentPostAssertList1.add(new DocumentPostAssert(productionLine.getProduct().getId(),
              "61000", amountTotal, BigDecimal.ZERO, productionLine.getMovementQuantity()));
        }

        i++;
      }

      bMaterialsProduction = OBDal.getInstance().get(ProductionTransaction.class,
          billOfMaterialsProduction.getId());
      assertDocumentPost(bMaterialsProduction, null, documentPostAssertList1);

      return bMaterialsProduction;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Update transaction total cost amount
  private void manualCostAdjustment(MaterialTransaction materialTransaction, BigDecimal amount,
      boolean incremental, int day) {
    try {
      manualCostAdjustment(materialTransaction, amount, incremental, true, day);
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Update transaction total cost amount
  private void manualCostAdjustment(MaterialTransaction materialTransaction, BigDecimal amount,
      boolean incremental, boolean unitCost, int day) {
    try {
      manualCostAdjustment(materialTransaction.getId(), amount, incremental, unitCost, day);
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Cancel cost adjustment
  private void cancelCostAdjustment(CostAdjustment costAdjustment) {
    try {
      cancelCostAdjustment(costAdjustment.getId());
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Create a Sales Order cloning a created one and book it
  private Order createReturnFromCustomer(ShipmentInOut goodsShipment, BigDecimal price,
      BigDecimal quantity, int day) {
    try {
      Order returnFromCustomer = createReturnFromCustomer(goodsShipment.getId(), price, quantity,
          day);
      bookOrder(returnFromCustomer);
      return returnFromCustomer;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Create a Sales Order cloning a created one and book it
  private ShipmentInOut createReturnMaterialReceipt(Order returnFromCustomer, BigDecimal price,
      BigDecimal quantity, int day) {
    try {
      ShipmentInOut returnMaterialReceipt = createReturnMaterialReceipt(returnFromCustomer.getId(),
          price, quantity, LOCATOR1_ID, day);
      String productId = returnMaterialReceipt.getMaterialMgmtShipmentInOutLineList().get(0)
          .getProduct().getId();

      completeDocument(returnMaterialReceipt);
      runCostingBackground();
      ShipmentInOut returnReceipt = OBDal.getInstance().get(ShipmentInOut.class,
          returnMaterialReceipt.getId());
      postDocument(returnReceipt);
      returnReceipt = OBDal.getInstance().get(ShipmentInOut.class, returnMaterialReceipt.getId());
      List<DocumentPostAssert> documentPostAssertList = new ArrayList<DocumentPostAssert>();
      documentPostAssertList.add(new DocumentPostAssert("99900", BigDecimal.ZERO, quantity
          .multiply(price), quantity.negate()));
      documentPostAssertList.add(new DocumentPostAssert("35000", quantity.multiply(price),
          BigDecimal.ZERO, quantity.negate()));
      assertDocumentPost(returnReceipt, productId, documentPostAssertList);
      return returnReceipt;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Create a Purchase Invoice from a landed cost, complete it and post it
  private Invoice createPurchaseInvoiceLandedCost(String landedCostTypeId, BigDecimal price,
      BigDecimal quantity, int day) {
    try {
      return createPurchaseInvoiceLandedCost(landedCostTypeId, price, quantity, null, day);
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Create a Purchase Invoice from a landed cost, complete it and post it
  private Invoice createPurchaseInvoiceLandedCost(String landedCostTypeId, BigDecimal price,
      BigDecimal quantity, BigDecimal conversion, int day) {
    try {
      Invoice purchaseInvoice = createInvoiceLandedCost(landedCostTypeId, price, quantity, day);
      if (conversion != null)
        createConversion(purchaseInvoice, conversion);
      completeDocument(purchaseInvoice);
      OBDal.getInstance().commitAndClose();
      postDocument(purchaseInvoice);
      Invoice invoice = OBDal.getInstance().get(Invoice.class, purchaseInvoice.getId());

      List<DocumentPostAssert> documentPostAssertList = new ArrayList<DocumentPostAssert>();
      if (landedCostTypeId.equals(LANDEDCOSTTYPE1_ID)) {
        documentPostAssertList.add(new DocumentPostAssert("40000", BigDecimal.ZERO, quantity
            .multiply(price), null));
        documentPostAssertList.add(new DocumentPostAssert("62900", quantity.multiply(price),
            BigDecimal.ZERO, quantity));
        assertDocumentPost(invoice, null, documentPostAssertList);
      }

      else if (landedCostTypeId.equals(LANDEDCOSTTYPE2_ID)) {
        documentPostAssertList.add(new DocumentPostAssert("40000", BigDecimal.ZERO, quantity
            .multiply(price).add(quantity.multiply(price).divide(new BigDecimal("10"))), null));
        documentPostAssertList.add(new DocumentPostAssert("47200", quantity.multiply(price).divide(
            new BigDecimal("10")), BigDecimal.ZERO, null));
        documentPostAssertList.add(new DocumentPostAssert("62400", quantity.multiply(price),
            BigDecimal.ZERO, quantity));
        assertDocumentPost(invoice, landedCostTypeId, documentPostAssertList);
      }

      else {
        documentPostAssertList.add(new DocumentPostAssert("40000", BigDecimal.ZERO, quantity
            .multiply(price), null));
        documentPostAssertList.add(new DocumentPostAssert("62800", quantity.multiply(price),
            BigDecimal.ZERO, quantity));
        assertDocumentPost(invoice, landedCostTypeId, documentPostAssertList);
      }

      return invoice;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Create a Landed Cost from a list of landed cost types, complete it and post
  // it
  private LandedCost createLandedCost(List<String> landedCostTypeId, List<BigDecimal> amountList,
      List<ShipmentInOut> receiptList, List<ShipmentInOutLine> receiptLineList, int day) {
    try {
      List<String> receiptIdList = new ArrayList<String>();
      List<String> receiptLineIdList = new ArrayList<String>();

      if (receiptList == null)
        for (ShipmentInOutLine receiptLine : receiptLineList) {
          receiptIdList.add(null);
          receiptLineIdList.add(receiptLine.getId());
        }

      else if (receiptLineList == null)
        for (ShipmentInOut receipt : receiptList) {
          receiptIdList.add(receipt.getId());
          receiptLineIdList.add(null);
        }

      else
        for (int i = 0; i < receiptList.size(); i++) {
          receiptIdList.add(receiptList.get(i) != null ? receiptList.get(i).getId() : null);
          receiptLineIdList.add(receiptLineList.get(i) != null ? receiptLineList.get(i).getId()
              : null);
        }

      LandedCost landedCost = createLandedCost(landedCostTypeId, amountList, null, receiptIdList,
          receiptLineIdList, day);
      processLandedCost(landedCost.getId());
      return postLandedCostHeader(landedCost);
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Create a Landed Cost from a list of purchase invoices and goods receipt, complete it and post
  // it
  private LandedCost createLandedCost(List<Invoice> invoiceList, List<ShipmentInOut> receiptList,
      int day) {
    try {
      List<String> invoiceIdList = new ArrayList<String>();
      List<String> receiptLineIdList = new ArrayList<String>();
      for (Invoice invoice : invoiceList)
        invoiceIdList.add(invoice.getId());
      List<String> receiptIdList = new ArrayList<String>();
      for (ShipmentInOut receipt : receiptList) {
        receiptIdList.add(receipt.getId());
        receiptLineIdList.add(null);
      }

      LandedCost landedCost = createLandedCost(null, null, invoiceIdList, receiptIdList,
          receiptLineIdList, day);
      processLandedCost(landedCost.getId());

      return postLandedCost(landedCost);
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Create a Landed Cost from a list of purchase invoices and goods receipt and complete it
  // it
  private List<LandedCostCost> createLandedCostCost(List<Invoice> invoiceList, ShipmentInOut receipt) {
    try {
      List<String> invoiceIdList = new ArrayList<String>();
      for (Invoice invoice : invoiceList)
        invoiceIdList.add(invoice.getId());

      List<LandedCostCost> landedCostCostList = createLandedCostCost(invoiceIdList, receipt.getId());
      return landedCostCostList;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Complete a Landed Cost and post it
  private LandedCost postLandedCost(LandedCost landedCost) {
    try {
      postLandedCostHeader(landedCost);
      LandedCost lc = OBDal.getInstance().get(LandedCost.class, landedCost.getId());
      for (LandedCostCost landedCostCost : lc.getLandedCostCostList())
        postLandedCostLine(landedCostCost,
            OBDal.getInstance().get(LandedCostCost.class, landedCostCost.getId()).getInvoiceLine());
      return landedCost;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Post landed cost header
  private LandedCost postLandedCostHeader(LandedCost landedCost) {
    try {
      StringBuffer where = new StringBuffer();
      where.append(" as t1 ");
      where.append("\n left join t1." + LCReceipt.PROPERTY_GOODSSHIPMENT + " t2");
      where.append("\n where t1." + LCReceipt.PROPERTY_LANDEDCOST + " = :landedCost");
      where.append("\n order by t2." + ShipmentInOut.PROPERTY_DOCUMENTNO);
      where.append("\n , t1." + LCReceipt.PROPERTY_CREATIONDATE);
      OBQuery<LCReceipt> criteria1 = OBDal.getInstance().createQuery(LCReceipt.class,
          where.toString());
      criteria1.setNamedParameter("landedCost",
          OBDal.getInstance().get(LandedCost.class, landedCost.getId()));
      List<LCReceipt> landedCostReceiptList = criteria1.list();

      OBCriteria<LandedCostCost> criteria2 = OBDal.getInstance().createCriteria(
          LandedCostCost.class);
      criteria2.add(Restrictions.eq(LandedCostCost.PROPERTY_LANDEDCOST, landedCost));
      criteria2.addOrderBy(LandedCostCost.PROPERTY_LINENO, true);
      List<LandedCostCost> landedCostCostList = criteria2.list();

      BigDecimal receiptTotalAmount = BigDecimal.ZERO;
      for (LCReceipt landedCostReceipt : landedCostReceiptList)
        if (!landedCostReceipt.getGoodsShipment().getMaterialMgmtShipmentInOutLineList().get(0)
            .getProduct().getProductType().equals("S"))
          if (landedCostReceipt.getGoodsShipmentLine() == null)
            receiptTotalAmount = receiptTotalAmount.add(getTransactionAmount(landedCostReceipt
                .getGoodsShipment()));
          else
            receiptTotalAmount = receiptTotalAmount.add(getTransactionLineAmount(landedCostReceipt
                .getGoodsShipmentLine()));

      List<List<LandedCostReceiptLineAmountAssert>> landedCostReceiptLineAmountAssertListList = new ArrayList<List<LandedCostReceiptLineAmountAssert>>();

      for (LCReceipt landedCostReceipt : landedCostReceiptList) {

        List<LandedCostReceiptLineAmountAssert> landedCostReceiptLineAmountAssertList = new ArrayList<LandedCostReceiptLineAmountAssert>();

        if (!landedCostReceipt.getGoodsShipment().getMaterialMgmtShipmentInOutLineList().get(0)
            .getProduct().getProductType().equals("S")) {

          for (LandedCostCost landedCostCost : landedCostCostList) {

            if (landedCostCost.getLandedCostMatchedList().isEmpty()) {

              if (landedCostReceipt.getGoodsShipmentLine() != null) {

                BigDecimal amount = landedCostCost.getAmount()
                    .multiply(getTransactionLineAmount(landedCostReceipt.getGoodsShipmentLine()))
                    .divide(receiptTotalAmount, 4, BigDecimal.ROUND_HALF_UP);

                landedCostReceiptLineAmountAssertList.add(new LandedCostReceiptLineAmountAssert(
                    OBDal.getInstance().get(LandedCostCost.class, landedCostCost.getId()), OBDal
                        .getInstance().get(ShipmentInOutLine.class,
                            landedCostReceipt.getGoodsShipmentLine().getId()), amount));

              } else {
                for (ShipmentInOutLine receiptLine : landedCostReceipt.getGoodsShipment()
                    .getMaterialMgmtShipmentInOutLineList()) {

                  BigDecimal amount = landedCostCost.getAmount()
                      .multiply(getTransactionLineAmount(receiptLine))
                      .divide(receiptTotalAmount, 4, BigDecimal.ROUND_HALF_UP);

                  landedCostReceiptLineAmountAssertList
                      .add(new LandedCostReceiptLineAmountAssert(OBDal.getInstance().get(
                          LandedCostCost.class, landedCostCost.getId()), OBDal.getInstance().get(
                          ShipmentInOutLine.class, receiptLine.getId()), amount));

                }
              }
            }

            else {

              OBCriteria<LCMatched> criteria3 = OBDal.getInstance().createCriteria(LCMatched.class);
              criteria3.add(Restrictions.eq(LCMatched.PROPERTY_LANDEDCOSTCOST, landedCostCost));
              criteria3.addOrderBy(LCMatched.PROPERTY_CREATIONDATE, true);

              for (LCMatched landedCostMatched : criteria3.list()) {

                if (landedCostReceipt.getGoodsShipmentLine() != null) {

                  BigDecimal amount = landedCostMatched.getAmount()
                      .multiply(getTransactionLineAmount(landedCostReceipt.getGoodsShipmentLine()))
                      .divide(receiptTotalAmount, 4, BigDecimal.ROUND_HALF_UP);

                  landedCostReceiptLineAmountAssertList.add(new LandedCostReceiptLineAmountAssert(
                      OBDal.getInstance().get(LandedCostCost.class, landedCostCost.getId()), OBDal
                          .getInstance().get(ShipmentInOutLine.class,
                              landedCostReceipt.getGoodsShipmentLine().getId()), amount));

                } else {
                  for (ShipmentInOutLine receiptLine : landedCostReceipt.getGoodsShipment()
                      .getMaterialMgmtShipmentInOutLineList()) {

                    BigDecimal amount = landedCostMatched.getAmount()
                        .multiply(getTransactionLineAmount(receiptLine))
                        .divide(receiptTotalAmount, 4, BigDecimal.ROUND_HALF_UP);

                    landedCostReceiptLineAmountAssertList
                        .add(new LandedCostReceiptLineAmountAssert(OBDal.getInstance().get(
                            LandedCostCost.class, landedCostCost.getId()), OBDal.getInstance().get(
                            ShipmentInOutLine.class, receiptLine.getId()), amount));

                  }
                }
              }
            }
          }
        }

        landedCostReceiptLineAmountAssertListList.add(landedCostReceiptLineAmountAssertList);
      }

      BigDecimal landedCostCostAmount = BigDecimal.ZERO;
      for (LandedCostCost landedCostCost : landedCostCostList)
        if (landedCostCost.getLandedCostMatchedList().size() == 1)
          landedCostCostAmount = landedCostCost.isMatchingAdjusted() ? landedCostCost
              .getMatchingAmount() : landedCostCost.getAmount();
        else
          for (LCMatched landedCostMatched : landedCostCost.getLandedCostMatchedList())
            landedCostCostAmount = landedCostCostAmount.add(landedCostMatched.getAmount());

      for (LandedCostCost landedCostCost : landedCostCostList) {
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal maxAmount = BigDecimal.ZERO;
        int maxI = 0;
        int maxJ = 0;
        int i = 0;
        for (List<LandedCostReceiptLineAmountAssert> landedCostReceiptLineAmountAssertList : landedCostReceiptLineAmountAssertListList) {
          int j = 0;
          for (LandedCostReceiptLineAmountAssert landedCostReceiptLineAmountAssert : landedCostReceiptLineAmountAssertList) {
            if (landedCostReceiptLineAmountAssert.getLandedCostCost().equals(landedCostCost)) {
              totalAmount = totalAmount.add(landedCostReceiptLineAmountAssert.getAmount());
              if (landedCostReceiptLineAmountAssert.getAmount().compareTo(maxAmount) > 0) {
                maxAmount = landedCostReceiptLineAmountAssert.getAmount();
                maxI = i;
                maxJ = j;
              }
            }
            j++;
          }
          i++;
        }

        if (!totalAmount.setScale(4, BigDecimal.ROUND_HALF_UP).equals(
            landedCostCostAmount.setScale(4, BigDecimal.ROUND_HALF_UP))) {
          landedCostReceiptLineAmountAssertListList.get(maxI).set(
              maxJ,
              new LandedCostReceiptLineAmountAssert(landedCostReceiptLineAmountAssertListList
                  .get(maxI).get(maxJ).getLandedCostCost(),
                  landedCostReceiptLineAmountAssertListList.get(maxI).get(maxJ).getReceiptLine(),
                  landedCostReceiptLineAmountAssertListList
                      .get(maxI)
                      .get(maxJ)
                      .getAmount()
                      .add(
                          totalAmount.add(
                              (landedCostCost.isMatchingAdjusted() ? landedCostCost
                                  .getMatchingAmount() : landedCostCost.getAmount()).negate())
                              .negate())));
        }
      }

      int i = 0;
      for (LCReceipt landedCostReceipt : landedCostReceiptList) {
        assertLandedCostReceiptLineAmount(landedCostReceipt.getId(),
            landedCostReceiptLineAmountAssertListList.get(i));
        i++;
      }

      List<LCReceipt> lCReceiptList = new ArrayList<LCReceipt>(landedCostReceiptList);
      i = 0;
      for (LCReceipt landedCostReceipt : landedCostReceiptList) {
        if (landedCostReceipt.getGoodsShipmentLine() != null
            && i < landedCostReceiptList.size() - 1
            && landedCostReceiptList.get(i + 1).getGoodsShipmentLine() != null
            && landedCostReceipt.getGoodsShipment().getDocumentNo()
                .equals(landedCostReceiptList.get(i + 1).getGoodsShipment().getDocumentNo())
            && landedCostReceipt.getGoodsShipmentLine().getLineNo()
                .compareTo(landedCostReceiptList.get(i + 1).getGoodsShipmentLine().getLineNo()) > 0) {
          lCReceiptList.set(i, landedCostReceiptList.get(i + 1));
          lCReceiptList.set(i + 1, landedCostReceipt);

        }
        i++;
      }

      List<DocumentPostAssert> documentPostAssertList = new ArrayList<DocumentPostAssert>();

      for (LandedCostCost landedCostCost : landedCostCostList) {

        String account;
        if (landedCostCost.getLandedCostType().equals(
            OBDal.getInstance().get(Product.class, LANDEDCOSTTYPE2_ID).getLandedCostTypeList()
                .get(0)))
          account = "62400";
        else if (landedCostCost.getLandedCostType().equals(
            OBDal.getInstance().get(Product.class, LANDEDCOSTTYPE3_ID).getLandedCostTypeList()
                .get(0)))
          account = "62800";
        else
          account = "62900";

        for (LCReceipt landedCostReceipt : lCReceiptList) {

          if (!landedCostReceipt.getGoodsShipment().getMaterialMgmtShipmentInOutLineList().get(0)
              .getProduct().getProductType().equals("S")) {

            if (landedCostReceipt.getGoodsShipmentLine() != null) {

              BigDecimal amount = landedCostCost.getAmount()
                  .multiply(getTransactionLineAmount(landedCostReceipt.getGoodsShipmentLine()))
                  .divide(receiptTotalAmount, 4, BigDecimal.ROUND_HALF_UP);

              documentPostAssertList.add(new DocumentPostAssert(landedCostReceipt
                  .getGoodsShipmentLine().getProduct().getId(), "35000", amount, BigDecimal.ZERO,
                  null));
              documentPostAssertList.add(new DocumentPostAssert(account, BigDecimal.ZERO, amount,
                  null));

            } else {

              OBCriteria<ShipmentInOutLine> criteria3 = OBDal.getInstance().createCriteria(
                  ShipmentInOutLine.class);
              criteria3.add(Restrictions.eq(ShipmentInOutLine.PROPERTY_SHIPMENTRECEIPT,
                  landedCostReceipt.getGoodsShipment()));
              criteria3.addOrderBy(ShipmentInOutLine.PROPERTY_LINENO, true);

              for (ShipmentInOutLine receiptLine : criteria3.list()) {

                BigDecimal amount = landedCostCost.getAmount()
                    .multiply(getTransactionLineAmount(receiptLine))
                    .divide(receiptTotalAmount, 4, BigDecimal.ROUND_HALF_UP);

                documentPostAssertList.add(new DocumentPostAssert(receiptLine.getProduct().getId(),
                    "35000", amount, BigDecimal.ZERO, null));
                documentPostAssertList.add(new DocumentPostAssert(account, BigDecimal.ZERO, amount,
                    null));

              }
            }
          }
        }
      }

      postDocument(landedCost);
      assertDocumentPost(landedCost, null, documentPostAssertList);

      return landedCost;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Post landed cost line
  private LandedCostCost postLandedCostLine(LandedCostCost landedCostCost, InvoiceLine invoiceLine) {
    try {
      List<LandedCostCostMatchedAssert> landedCostCostMatchedAssertList = new ArrayList<LandedCostCostMatchedAssert>();
      InvoiceLine iLine = OBDal.getInstance().get(InvoiceLine.class, invoiceLine.getId());
      landedCostCostMatchedAssertList.add(new LandedCostCostMatchedAssert(iLine));
      if (!iLine.getInvoice().getCurrencyConversionRateDocList().isEmpty())
        landedCostCostMatchedAssertList.add(new LandedCostCostMatchedAssert(iLine));
      assertLandedCostCostMatched(landedCostCost.getId(), landedCostCostMatchedAssertList);

      postDocument(landedCostCost);
      LandedCostCost lcCost = OBDal.getInstance().get(LandedCostCost.class, landedCostCost.getId());
      List<DocumentPostAssert> documentPostAssertList = new ArrayList<DocumentPostAssert>();

      String account;
      String productId;
      if (lcCost.getLandedCostType()
          .equals(
              OBDal.getInstance().get(Product.class, LANDEDCOSTTYPE2_ID).getLandedCostTypeList()
                  .get(0))) {
        account = "62400";
        productId = LANDEDCOSTTYPE2_ID;
      } else if (lcCost.getLandedCostType()
          .equals(
              OBDal.getInstance().get(Product.class, LANDEDCOSTTYPE3_ID).getLandedCostTypeList()
                  .get(0))) {
        account = "62800";
        productId = LANDEDCOSTTYPE3_ID;
      } else {
        account = "62900";
        productId = null;
      }

      if (lcCost.getLandedCostMatchedList().size() == 1) {

        documentPostAssertList.add(new DocumentPostAssert(productId, account, BigDecimal.ZERO,
            lcCost.getMatchingAmount(), null));

        if (!lcCost.getAmount().setScale(2, BigDecimal.ROUND_HALF_UP)
            .equals(lcCost.getMatchingAmount().setScale(2, BigDecimal.ROUND_HALF_UP))
            && lcCost.isMatchingAdjusted()) {

          documentPostAssertList.add(new DocumentPostAssert(account, lcCost.getAmount(),
              BigDecimal.ZERO, null));

          if (OBDal.getInstance().get(LandedCost.class, landedCostCost.getLandedCost().getId())
              .getLandedCostReceiptList().size() > 1
              && !OBDal
                  .getInstance()
                  .get(LandedCost.class, landedCostCost.getLandedCost().getId())
                  .getLandedCostReceiptList()
                  .get(0)
                  .getGoodsShipment()
                  .getMaterialMgmtShipmentInOutLineList()
                  .get(0)
                  .getProduct()
                  .equals(
                      OBDal.getInstance()
                          .get(LandedCost.class, landedCostCost.getLandedCost().getId())
                          .getLandedCostReceiptList().get(1).getGoodsShipment()
                          .getMaterialMgmtShipmentInOutLineList().get(0).getProduct())) {

            final OBCriteria<AccountingFact> criteria = OBDal.getInstance().createCriteria(
                AccountingFact.class);
            criteria.add(Restrictions.eq(AccountingFact.PROPERTY_RECORDID, lcCost.getId()));
            criteria.addOrderBy(AccountingFact.PROPERTY_SEQUENCENUMBER, true);

            if (criteria
                .list()
                .get(2)
                .getForeignCurrencyDebit()
                .setScale(2, BigDecimal.ROUND_HALF_UP)
                .equals(
                    lcCost
                        .getMatchingAmount()
                        .add(lcCost.getAmount().negate())
                        .multiply(
                            getTransactionAmount(OBDal.getInstance()
                                .get(LandedCost.class, landedCostCost.getLandedCost().getId())
                                .getLandedCostReceiptList().get(0).getGoodsShipment()))
                        .divide(
                            getTransactionAmount(
                                OBDal.getInstance()
                                    .get(LandedCost.class, landedCostCost.getLandedCost().getId())
                                    .getLandedCostReceiptList().get(0).getGoodsShipment()).add(
                                getTransactionAmount(OBDal.getInstance()
                                    .get(LandedCost.class, landedCostCost.getLandedCost().getId())
                                    .getLandedCostReceiptList().get(1).getGoodsShipment())), 2,
                            BigDecimal.ROUND_HALF_UP))) {

              documentPostAssertList.add(new DocumentPostAssert(OBDal.getInstance()
                  .get(LandedCost.class, landedCostCost.getLandedCost().getId())
                  .getLandedCostReceiptList().get(0).getGoodsShipment()
                  .getMaterialMgmtShipmentInOutLineList().get(0).getProduct().getId(), "35000",
                  lcCost
                      .getMatchingAmount()
                      .add(lcCost.getAmount().negate())
                      .multiply(
                          getTransactionAmount(OBDal.getInstance()
                              .get(LandedCost.class, landedCostCost.getLandedCost().getId())
                              .getLandedCostReceiptList().get(0).getGoodsShipment()))
                      .divide(
                          getTransactionAmount(
                              OBDal.getInstance()
                                  .get(LandedCost.class, landedCostCost.getLandedCost().getId())
                                  .getLandedCostReceiptList().get(0).getGoodsShipment()).add(
                              getTransactionAmount(OBDal.getInstance()
                                  .get(LandedCost.class, landedCostCost.getLandedCost().getId())
                                  .getLandedCostReceiptList().get(1).getGoodsShipment())), 2,
                          BigDecimal.ROUND_HALF_UP), BigDecimal.ZERO, null));

              documentPostAssertList.add(new DocumentPostAssert(OBDal.getInstance()
                  .get(LandedCost.class, landedCostCost.getLandedCost().getId())
                  .getLandedCostReceiptList().get(1).getGoodsShipment()
                  .getMaterialMgmtShipmentInOutLineList().get(0).getProduct().getId(), "35000",
                  lcCost
                      .getMatchingAmount()
                      .add(lcCost.getAmount().negate())
                      .multiply(
                          getTransactionAmount(OBDal.getInstance()
                              .get(LandedCost.class, landedCostCost.getLandedCost().getId())
                              .getLandedCostReceiptList().get(1).getGoodsShipment()))
                      .divide(
                          getTransactionAmount(
                              OBDal.getInstance()
                                  .get(LandedCost.class, landedCostCost.getLandedCost().getId())
                                  .getLandedCostReceiptList().get(0).getGoodsShipment()).add(
                              getTransactionAmount(OBDal.getInstance()
                                  .get(LandedCost.class, landedCostCost.getLandedCost().getId())
                                  .getLandedCostReceiptList().get(1).getGoodsShipment())), 2,
                          BigDecimal.ROUND_HALF_UP), BigDecimal.ZERO, null));
            }

            else {
              documentPostAssertList.add(new DocumentPostAssert(OBDal.getInstance()
                  .get(LandedCost.class, landedCostCost.getLandedCost().getId())
                  .getLandedCostReceiptList().get(1).getGoodsShipment()
                  .getMaterialMgmtShipmentInOutLineList().get(0).getProduct().getId(), "35000",
                  lcCost
                      .getMatchingAmount()
                      .add(lcCost.getAmount().negate())
                      .multiply(
                          getTransactionAmount(OBDal.getInstance()
                              .get(LandedCost.class, landedCostCost.getLandedCost().getId())
                              .getLandedCostReceiptList().get(1).getGoodsShipment()))
                      .divide(
                          getTransactionAmount(
                              OBDal.getInstance()
                                  .get(LandedCost.class, landedCostCost.getLandedCost().getId())
                                  .getLandedCostReceiptList().get(0).getGoodsShipment()).add(
                              getTransactionAmount(OBDal.getInstance()
                                  .get(LandedCost.class, landedCostCost.getLandedCost().getId())
                                  .getLandedCostReceiptList().get(1).getGoodsShipment())), 2,
                          BigDecimal.ROUND_HALF_UP), BigDecimal.ZERO, null));

              documentPostAssertList.add(new DocumentPostAssert(OBDal.getInstance()
                  .get(LandedCost.class, landedCostCost.getLandedCost().getId())
                  .getLandedCostReceiptList().get(0).getGoodsShipment()
                  .getMaterialMgmtShipmentInOutLineList().get(0).getProduct().getId(), "35000",
                  lcCost
                      .getMatchingAmount()
                      .add(lcCost.getAmount().negate())
                      .multiply(
                          getTransactionAmount(OBDal.getInstance()
                              .get(LandedCost.class, landedCostCost.getLandedCost().getId())
                              .getLandedCostReceiptList().get(0).getGoodsShipment()))
                      .divide(
                          getTransactionAmount(
                              OBDal.getInstance()
                                  .get(LandedCost.class, landedCostCost.getLandedCost().getId())
                                  .getLandedCostReceiptList().get(0).getGoodsShipment()).add(
                              getTransactionAmount(OBDal.getInstance()
                                  .get(LandedCost.class, landedCostCost.getLandedCost().getId())
                                  .getLandedCostReceiptList().get(1).getGoodsShipment())), 2,
                          BigDecimal.ROUND_HALF_UP), BigDecimal.ZERO, null));
            }

          } else {
            if (lcCost.getAmount().add(lcCost.getMatchingAmount().negate())
                .compareTo(BigDecimal.ZERO) > 0)
              documentPostAssertList.add(new DocumentPostAssert(OBDal.getInstance()
                  .get(LandedCost.class, landedCostCost.getLandedCost().getId())
                  .getLandedCostReceiptList().get(0).getGoodsShipment()
                  .getMaterialMgmtShipmentInOutLineList().get(0).getProduct().getId(), "35000",
                  BigDecimal.ZERO, lcCost.getAmount().add(lcCost.getMatchingAmount().negate()),
                  null));
            else
              documentPostAssertList.add(new DocumentPostAssert(OBDal.getInstance()
                  .get(LandedCost.class, landedCostCost.getLandedCost().getId())
                  .getLandedCostReceiptList().get(0).getGoodsShipment()
                  .getMaterialMgmtShipmentInOutLineList().get(0).getProduct().getId(), "35000",
                  lcCost.getMatchingAmount().add(lcCost.getAmount().negate()), BigDecimal.ZERO,
                  null));
          }
        } else {
          documentPostAssertList.add(new DocumentPostAssert(account, lcCost.getMatchingAmount(),
              BigDecimal.ZERO, null));
        }
      }

      else {

        final OBCriteria<LCMatched> criteria1 = OBDal.getInstance().createCriteria(LCMatched.class);
        criteria1.add(Restrictions.eq(LCMatched.PROPERTY_LANDEDCOSTCOST, landedCostCost));
        criteria1.addOrderBy(LCMatched.PROPERTY_CREATIONDATE, true);
        List<LCMatched> landedCostCostMatchedList = criteria1.list();

        final OBCriteria<AccountingFact> criteria2 = OBDal.getInstance().createCriteria(
            AccountingFact.class);
        criteria2.add(Restrictions.eq(AccountingFact.PROPERTY_RECORDID, lcCost.getId()));
        criteria2.addOrderBy(AccountingFact.PROPERTY_SEQUENCENUMBER, true);

        if (!criteria2
            .list()
            .get(0)
            .getForeignCurrencyDebit()
            .setScale(2, BigDecimal.ROUND_HALF_UP)
            .equals(
                landedCostCostMatchedList.get(0).getAmount().setScale(2, BigDecimal.ROUND_HALF_UP))
            && !criteria2
                .list()
                .get(0)
                .getForeignCurrencyCredit()
                .setScale(2, BigDecimal.ROUND_HALF_UP)
                .equals(
                    landedCostCostMatchedList.get(0).getAmount()
                        .setScale(2, BigDecimal.ROUND_HALF_UP)))
          Collections.reverse(landedCostCostMatchedList);

        for (LCMatched landedCostCostMatched : landedCostCostMatchedList) {
          if (landedCostCostMatched.getAmount().compareTo(BigDecimal.ZERO) < 0)
            documentPostAssertList.add(new DocumentPostAssert(productId, account,
                landedCostCostMatched.getAmount().negate(), BigDecimal.ZERO, null));
          else
            documentPostAssertList.add(new DocumentPostAssert(productId, account, BigDecimal.ZERO,
                landedCostCostMatched.getAmount(), null));
        }

        if (!criteria2
            .list()
            .get(2)
            .getForeignCurrencyDebit()
            .setScale(2, BigDecimal.ROUND_HALF_UP)
            .equals(
                landedCostCostMatchedList.get(0).getAmount().setScale(2, BigDecimal.ROUND_HALF_UP))
            && !criteria2
                .list()
                .get(2)
                .getForeignCurrencyCredit()
                .setScale(2, BigDecimal.ROUND_HALF_UP)
                .equals(
                    landedCostCostMatchedList.get(0).getAmount()
                        .setScale(2, BigDecimal.ROUND_HALF_UP)))
          Collections.reverse(landedCostCostMatchedList);

        int i = 0;
        for (LCMatched landedCostCostMatched : landedCostCostMatchedList) {
          if (i == 0) {
            if (landedCostCostMatched.getAmount().compareTo(BigDecimal.ZERO) < 0)
              documentPostAssertList.add(new DocumentPostAssert(account, BigDecimal.ZERO,
                  landedCostCostMatched.getAmount().negate(), null));
            else
              documentPostAssertList.add(new DocumentPostAssert(account, landedCostCostMatched
                  .getAmount(), BigDecimal.ZERO, null));
          } else {
            if (landedCostCostMatched.getAmount().compareTo(BigDecimal.ZERO) < 0)
              documentPostAssertList.add(new DocumentPostAssert(lcCost
                  .getLandedCostReceiptLineAmtList().get(0).getGoodsShipmentLine().getProduct()
                  .getId(), "35000", BigDecimal.ZERO, landedCostCostMatched.getAmount().negate(),
                  null));
            else
              documentPostAssertList.add(new DocumentPostAssert(lcCost
                  .getLandedCostReceiptLineAmtList().get(0).getGoodsShipmentLine().getProduct()
                  .getId(), "35000", landedCostCostMatched.getAmount(), BigDecimal.ZERO, null));
          }
          i++;
        }
      }

      assertDocumentPost(lcCost, null, documentPostAssertList);

      return lcCost;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Reactivate landed cost
  private void cancelLandedCost(LandedCost landedCost) {
    try {
      for (LandedCostCost landedCostCost : landedCost.getLandedCostCostList()) {
        unpostDocument(landedCostCost);
        cancelLandedCostCost(landedCostCost.getId(), null);
      }
      unpostDocument(landedCost);
      reactivateLandedCost(landedCost.getId(), null);
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Match a purchase invoice landed cost with a landed cost cost
  private void matchInvoiceLandedCost(InvoiceLine purchaseInvoiceLineLandedCost,
      LandedCostCost landedCostCost, boolean matching) {
    try {
      matchInvoiceLandedCost(purchaseInvoiceLineLandedCost.getId(), landedCostCost.getId(),
          purchaseInvoiceLineLandedCost.getLineNetAmount(), null, matching);
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Match a purchase invoice landed cost with a landed cost cost
  private void matchInvoiceLandedCost(InvoiceLine purchaseInvoiceLineLandedCost,
      LandedCostCost landedCostCost, LCMatched landedCostMatched, boolean matching) {
    try {
      matchInvoiceLandedCost(purchaseInvoiceLineLandedCost.getId(), landedCostCost.getId(),
          purchaseInvoiceLineLandedCost.getLineNetAmount(), landedCostMatched.getId(), matching);
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Match a landed cost cost
  private void matchInvoiceLandedCost(LandedCostCost landedCostCost, boolean matching, String error) {
    try {
      matchInvoiceLandedCost(landedCostCost.getId(), matching, error);
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Change organization currency
  private void changeOrganizationCurrency(String organizationId, String currencyId) {
    try {
      changeOrganizationCurrency(OBDal.getInstance().get(Organization.class, organizationId),
          currencyId == null ? null : OBDal.getInstance().get(Currency.class, currencyId));
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  /********************************************** Specific methods for tests **********************************************/

  // Create a new product cloning costing Product 1
  private Product cloneProduct(String name, int num, String productType, BigDecimal purchasePrice,
      BigDecimal salesPrice, BigDecimal cost, String costType, int year, String currencyId,
      List<String> productIdList, List<BigDecimal> quantityList) {
    try {
      Product product = OBDal.getInstance().get(Product.class, PRODUCT_ID);
      Product productClone = (Product) DalUtil.copy(product, false);
      setGeneralData(productClone);

      productClone.setSearchKey(name + "-" + num);
      productClone.setName(name + "-" + num);
      productClone.setMaterialMgmtMaterialTransactionList(null);
      productClone.setProductType(productType);
      OBDal.getInstance().save(productClone);

      if (productIdList.isEmpty()) {

        OBCriteria<ProductPrice> criteria = OBDal.getInstance().createCriteria(ProductPrice.class);
        criteria.add(Restrictions.eq(ProductPrice.PROPERTY_PRODUCT, product));
        criteria.addOrderBy(ProductPrice.PROPERTY_CREATIONDATE, true);
        int i = 0;
        for (ProductPrice productPrice : criteria.list()) {
          ProductPrice productPriceClone = (ProductPrice) DalUtil.copy(productPrice, false);
          setGeneralData(productPriceClone);
          if (i % 2 == 0) {
            if (currencyId.equals(CURRENCY2_ID))
              productPriceClone.setPriceListVersion(OBDal.getInstance()
                  .get(Product.class, LANDEDCOSTTYPE3_ID).getPricingProductPriceList().get(0)
                  .getPriceListVersion());
            productPriceClone.setStandardPrice(purchasePrice);
            productPriceClone.setListPrice(purchasePrice);
          } else {
            productPriceClone.setStandardPrice(salesPrice);
            productPriceClone.setListPrice(salesPrice);
          }
          productPriceClone.setProduct(productClone);
          productClone.getPricingProductPriceList().add(productPriceClone);
          i++;
        }

        if (cost != null) {
          Costing productCosting = OBProvider.getInstance().get(Costing.class);
          setGeneralData(productCosting);
          if (year != 0)
            productCosting.setStartingDate(DateUtils.addYears(product.getPricingProductPriceList()
                .get(0).getPriceListVersion().getValidFromDate(), year));
          else
            productCosting.setStartingDate(new Date());
          Calendar calendar = Calendar.getInstance();
          calendar.set(9999, 11, 31);
          productCosting.setEndingDate(calendar.getTime());
          productCosting.setManual(true);
          productCosting.setCostType(costType);
          productCosting.setCost(cost);
          productCosting.setCurrency(OBDal.getInstance().get(Currency.class, CURRENCY1_ID));
          productCosting.setWarehouse(OBDal.getInstance().get(Warehouse.class, WAREHOUSE1_ID));
          productCosting.setProduct(productClone);
          productClone.getMaterialMgmtCostingList().add(productCosting);
        }
      }

      else {
        productClone.setBillOfMaterials(true);
        int i = 0;
        for (String productBOMId : productIdList) {
          ProductBOM productBOMClone = OBProvider.getInstance().get(ProductBOM.class);
          setGeneralData(productBOMClone);
          productBOMClone.setLineNo(new Long((i + 1) * 10));
          productBOMClone.setProduct(productClone);
          productBOMClone.setBOMProduct(OBDal.getInstance().get(Product.class, productBOMId));
          productBOMClone.setBOMQuantity(quantityList.get(i));
          i++;

          OBDal.getInstance().save(productBOMClone);
          OBDal.getInstance().flush();
          OBDal.getInstance().refresh(productBOMClone);
        }
        OBDal.getInstance().save(productClone);
        OBDal.getInstance().flush();
        OBDal.getInstance().refresh(productClone);

        verifyBOM(productClone.getId());
        productClone.setBOMVerified(true);
      }

      OBDal.getInstance().save(productClone);
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(productClone);

      OBCriteria<ProductAccounts> criteria = OBDal.getInstance().createCriteria(
          ProductAccounts.class);
      criteria.add(Restrictions.eq(ProductAccounts.PROPERTY_PRODUCT, product));
      criteria.add(Restrictions.isNotNull(ProductAccounts.PROPERTY_INVOICEPRICEVARIANCE));
      productClone.getProductAccountsList().get(0)
          .setInvoicePriceVariance(criteria.list().get(0).getInvoicePriceVariance());

      OBDal.getInstance().save(productClone);
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(productClone);

      return productClone;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Returns the number of products with name costing Product
  private int getNumberOfCostingProducts(String name) {
    try {
      final OBCriteria<Product> criteria = OBDal.getInstance().createCriteria(Product.class);
      criteria.add(Restrictions.like(Product.PROPERTY_NAME, name + "-%"));
      return criteria.list().size();
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Create a new order cloning a previous created one
  private Order cloneOrder(String productId, boolean issotrx, BigDecimal price,
      BigDecimal quantity, int day) {
    try {
      Order order = issotrx ? OBDal.getInstance().get(Order.class, ORDEROUT_ID) : OBDal
          .getInstance().get(Order.class, ORDERIN_ID);

      Order orderClone = (Order) DalUtil.copy(order, false);
      Product product = OBDal.getInstance().get(Product.class, productId);
      setGeneralData(orderClone);

      orderClone
          .setDocumentNo(getDocumentNo(order.getDocumentType().getDocumentSequence().getId()));
      orderClone.setOrderDate(DateUtils.addDays(new Date(), day));
      orderClone.setScheduledDeliveryDate(DateUtils.addDays(new Date(), day));
      orderClone.setSummedLineAmount(BigDecimal.ZERO);
      orderClone.setGrandTotalAmount(BigDecimal.ZERO);

      // Get the first line associated with the order and clone it to the new order
      OrderLine orderLine = order.getOrderLineList().get(0);
      OrderLine orderCloneLine = (OrderLine) DalUtil.copy(orderLine, false);

      setGeneralData(orderCloneLine);
      orderCloneLine.setOrderDate(DateUtils.addDays(new Date(), day));
      orderCloneLine.setScheduledDeliveryDate(DateUtils.addDays(new Date(), day));

      orderCloneLine.setProduct(product);
      orderCloneLine.setOrderedQuantity(quantity);
      orderCloneLine.setUnitPrice(price);
      orderCloneLine.setListPrice(price);
      orderCloneLine.setStandardPrice(price);
      orderCloneLine.setLineNetAmount(quantity.multiply(price));
      orderCloneLine.setTaxableAmount(quantity.multiply(price));

      if (product
          .getPricingProductPriceList()
          .get(0)
          .getPriceListVersion()
          .equals(
              OBDal.getInstance().get(Product.class, LANDEDCOSTTYPE3_ID)
                  .getPricingProductPriceList().get(0).getPriceListVersion())) {
        orderClone.setCurrency(OBDal.getInstance().get(Currency.class, CURRENCY2_ID));
        orderClone.setPriceList(product.getPricingProductPriceList().get(0).getPriceListVersion()
            .getPriceList());
        orderClone.setBusinessPartner(OBDal.getInstance().get(BusinessPartner.class,
            BUSINESSPARTNER_ID));
        orderClone
            .setPartnerAddress(OBDal.getInstance().get(BusinessPartner.class, BUSINESSPARTNER_ID)
                .getBusinessPartnerLocationList().get(0));
        orderCloneLine.setCurrency(OBDal.getInstance().get(Currency.class, CURRENCY2_ID));
        orderCloneLine.setBusinessPartner(OBDal.getInstance().get(BusinessPartner.class,
            BUSINESSPARTNER_ID));
        orderCloneLine
            .setPartnerAddress(OBDal.getInstance().get(BusinessPartner.class, BUSINESSPARTNER_ID)
                .getBusinessPartnerLocationList().get(0));
      }

      orderCloneLine.setSalesOrder(orderClone);
      orderClone.getOrderLineList().add(orderCloneLine);

      OBDal.getInstance().save(orderClone);
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(orderClone);

      return orderClone;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Book a order
  private void bookOrder(Order order) {
    try {
      final List<Object> parameters = new ArrayList<Object>();
      parameters.add(null);
      parameters.add(order.getId());
      parameters.add("N");
      final String procedureName = "c_order_post1";
      CallStoredProcedure.getInstance().call(procedureName, parameters, null, true, false);

      OBDal.getInstance().save(order);
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(order);
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Reactivate a order
  private Order reactivateOrder(Order order) {
    try {
      order.setDocumentStatus("CO");
      order.setDocumentAction("RE");
      OBDal.getInstance().save(order);
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(order);
      bookOrder(order);
      return order;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Update order product price
  private Order updateOrderProductPrice(Order order, BigDecimal price) {
    try {
      OrderLine orderLine = order.getOrderLineList().get(0);

      orderLine.setUpdated(new Date());
      orderLine.setUnitPrice(price);
      orderLine.setListPrice(price);
      orderLine.setStandardPrice(price);
      orderLine.setLineNetAmount(orderLine.getOrderedQuantity().multiply(price));
      orderLine.setTaxableAmount(orderLine.getOrderedQuantity().multiply(price));

      OBDal.getInstance().save(order);
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(order);
      return order;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Create a new invoice cloning a previous created one
  private Invoice cloneInvoice(String productId, boolean issotrx, BigDecimal price,
      BigDecimal quantity, int day) {
    try {
      return cloneInvoice(productId, issotrx, price, quantity, null, day);
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Create a new invoice cloning a previous created one
  private Invoice cloneInvoice(String productId, boolean issotrx, BigDecimal price,
      BigDecimal quantity, String bpartnerId, int day) {
    try {
      Invoice invoice = null;
      if (!issotrx)
        invoice = OBDal.getInstance().get(Invoice.class, INVOICEIN_ID);

      Invoice invoiceClone = (Invoice) DalUtil.copy(invoice, false);
      setGeneralData(invoiceClone);

      if (issotrx)
        invoiceClone.setDocumentNo(getDocumentNo(invoiceClone.getDocumentType()
            .getDocumentSequence().getId()));
      else
        invoiceClone.setDocumentNo(getDocumentNo(INVOICEIN_SEQUENCE_ID));

      invoiceClone.setInvoiceDate(DateUtils.addDays(new Date(), day));
      invoiceClone.setAccountingDate(DateUtils.addDays(new Date(), day));
      invoiceClone.setSummedLineAmount(BigDecimal.ZERO);
      invoiceClone.setGrandTotalAmount(BigDecimal.ZERO);
      invoiceClone.setPriceList(OBDal.getInstance().get(Product.class, productId)
          .getPricingProductPriceList().get(0).getPriceListVersion().getPriceList());
      if (bpartnerId != null) {
        invoiceClone.setBusinessPartner(OBDal.getInstance().get(BusinessPartner.class, bpartnerId));
        invoiceClone.setPartnerAddress(OBDal.getInstance().get(BusinessPartner.class, bpartnerId)
            .getBusinessPartnerLocationList().get(0));
      }

      // Get the first line associated with the invoice and clone it to the new invoice
      InvoiceLine invoiceLine = invoice.getInvoiceLineList().get(0);
      InvoiceLine invoiceLineClone = (InvoiceLine) DalUtil.copy(invoiceLine, false);

      setGeneralData(invoiceLineClone);

      invoiceLineClone.setProduct(OBDal.getInstance().get(Product.class, productId));
      invoiceLineClone.setInvoicedQuantity(quantity);
      invoiceLineClone.setUnitPrice(price);
      invoiceLineClone.setListPrice(price);
      invoiceLineClone.setStandardPrice(price);
      invoiceLineClone.setLineNetAmount(quantity.multiply(price));
      invoiceLineClone.setTaxAmount(quantity.multiply(price));
      invoiceLineClone.setTaxableAmount(quantity.multiply(price));
      if (bpartnerId != null)
        invoiceLineClone.setBusinessPartner(OBDal.getInstance().get(BusinessPartner.class,
            bpartnerId));

      invoiceLineClone.setInvoice(invoiceClone);
      invoiceClone.getInvoiceLineList().add(invoiceLineClone);

      OBDal.getInstance().save(invoiceClone);
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(invoiceClone);

      return invoiceClone;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Create a new invoice from a order
  private Invoice createInvoiceFromOrder(String orderId, boolean issotrx, BigDecimal price,
      BigDecimal quantity, int day) {
    try {
      Order order = OBDal.getInstance().get(Order.class, orderId);
      Invoice invoice = cloneInvoice(order.getOrderLineList().get(0).getProduct().getId(), issotrx,
          price, quantity, day);

      invoice.setSalesOrder(order);
      invoice.setOrderDate(order.getOrderDate());

      int i = 0;
      for (OrderLine orderLine : order.getOrderLineList()) {
        InvoiceLine invoiceLine;

        if (i == 0) {
          invoiceLine = invoice.getInvoiceLineList().get(i);
        }

        else {
          invoiceLine = (InvoiceLine) DalUtil.copy(invoice.getInvoiceLineList().get(0), false);
          setGeneralData(invoiceLine);
          invoiceLine.setInvoice(invoice);
          invoice.getInvoiceLineList().add(invoiceLine);
        }

        invoiceLine.setSalesOrderLine(orderLine);
        invoiceLine.setLineNo(new Long((i + 1) * 10));
        invoiceLine.setInvoicedQuantity(orderLine.getOrderedQuantity());

        if (order.getOrderLineList().size() == 1) {
          invoiceLine.setUnitPrice(price);
          invoiceLine.setListPrice(price);
          invoiceLine.setStandardPrice(price);
          invoiceLine.setLineNetAmount(orderLine.getOrderedQuantity().multiply(price));
          invoiceLine.setTaxAmount(orderLine.getOrderedQuantity().multiply(price));
        }

        else {
          invoiceLine.setUnitPrice(orderLine.getUnitPrice());
          invoiceLine.setListPrice(orderLine.getUnitPrice());
          invoiceLine.setStandardPrice(orderLine.getUnitPrice());
          invoiceLine.setLineNetAmount(orderLine.getOrderedQuantity().multiply(
              orderLine.getUnitPrice()));
          invoiceLine.setTaxAmount(orderLine.getOrderedQuantity()
              .multiply(orderLine.getUnitPrice()));
        }

        OBCriteria<ShipmentInOutLine> criteria = OBDal.getInstance().createCriteria(
            ShipmentInOutLine.class);
        criteria.add(Restrictions.eq(ShipmentInOutLine.PROPERTY_SALESORDERLINE, orderLine));

        if (criteria.list().size() > 0) {
          invoiceLine.setGoodsShipmentLine(criteria.list().get(0));
        }

        orderLine.getInvoiceLineList().add(invoiceLine);
        orderLine.setInvoiceDate(invoice.getInvoiceDate());
        orderLine.setInvoicedQuantity(invoiceLine.getInvoicedQuantity());

        i++;
      }

      OBDal.getInstance().save(invoice);
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(invoice);

      order.getInvoiceList().add(invoice);
      order.setReinvoice(true);

      OBDal.getInstance().save(order);
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(order);
      return invoice;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Create a new invoice from a movement
  private Invoice createInvoiceFromMovement(String movementId, boolean issotrx, BigDecimal price,
      BigDecimal quantity, int day) {
    try {

      ShipmentInOut movement = OBDal.getInstance().get(ShipmentInOut.class, movementId);
      Invoice invoice = cloneInvoice(movement.getMaterialMgmtShipmentInOutLineList().get(0)
          .getProduct().getId(), issotrx, price, quantity, movement.getBusinessPartner().getId(),
          day);
      invoice.getMaterialMgmtShipmentInOutList().add(movement);

      int i = 0;
      for (ShipmentInOutLine movementLine : movement.getMaterialMgmtShipmentInOutLineList()) {
        InvoiceLine invoiceLine;

        if (i == 0) {
          invoiceLine = invoice.getInvoiceLineList().get(i);
        }

        else {
          invoiceLine = (InvoiceLine) DalUtil.copy(invoice.getInvoiceLineList().get(0), false);
          setGeneralData(invoiceLine);
          invoiceLine.setInvoice(invoice);
          invoice.getInvoiceLineList().add(invoiceLine);
        }

        invoiceLine.setGoodsShipmentLine(movementLine);
        invoiceLine.setLineNo(new Long((i + 1) * 10));
        invoiceLine.setInvoicedQuantity(movementLine.getMovementQuantity());

        if (movement.getMaterialMgmtShipmentInOutLineList().size() == 1) {
          invoiceLine.setUnitPrice(price);
          invoiceLine.setListPrice(price);
          invoiceLine.setStandardPrice(price);
          invoiceLine.setLineNetAmount(movementLine.getMovementQuantity().multiply(price));
          invoiceLine.setTaxAmount(movementLine.getMovementQuantity().multiply(price));
        }

        else {
          invoiceLine.setUnitPrice(movementLine.getSalesOrderLine().getUnitPrice());
          invoiceLine.setListPrice(movementLine.getSalesOrderLine().getUnitPrice());
          invoiceLine.setStandardPrice(movementLine.getSalesOrderLine().getUnitPrice());
          invoiceLine.setLineNetAmount(movementLine.getMovementQuantity().multiply(
              movementLine.getSalesOrderLine().getUnitPrice()));
          invoiceLine.setTaxAmount(movementLine.getMovementQuantity().multiply(
              movementLine.getSalesOrderLine().getUnitPrice()));
        }

        if (movement.getSalesOrder() != null) {
          invoiceLine.setSalesOrderLine(movementLine.getSalesOrderLine());

          movementLine.getSalesOrderLine().getInvoiceLineList().add(invoiceLine);
          movementLine.getSalesOrderLine().setInvoiceDate(invoice.getInvoiceDate());
          movementLine.getSalesOrderLine().setInvoicedQuantity(invoiceLine.getInvoicedQuantity());
        }

        movementLine.setReinvoice(true);
        movementLine.getInvoiceLineList().add(invoiceLine);

        i++;
      }

      if (movement.getSalesOrder() != null) {
        invoice.setSalesOrder(movement.getSalesOrder());
        invoice.setOrderDate(movement.getSalesOrder().getOrderDate());
        invoice.setCurrency(movement.getSalesOrder().getCurrency());

        movement.getSalesOrder().getInvoiceList().add(invoice);
        movement.getSalesOrder().setReinvoice(true);

        OBDal.getInstance().save(movement.getSalesOrder());
        OBDal.getInstance().flush();
        OBDal.getInstance().refresh(movement.getSalesOrder());
      }

      OBDal.getInstance().save(invoice);
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(invoice);
      return invoice;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Create a new invoice from many movements
  private Invoice createInvoiceFromOrders(List<String> orderIdList, boolean issotrx,
      List<BigDecimal> priceList, List<BigDecimal> quantityList, int day) {
    try {
      Order order = OBDal.getInstance().get(Order.class, orderIdList.get(0));
      Invoice invoice = cloneInvoice(order.getOrderLineList().get(0).getProduct().getId(), issotrx,
          priceList.get(0), quantityList.get(0), day);

      invoice.setSalesOrder(order);
      invoice.setOrderDate(order.getOrderDate());

      int i = 0;
      for (String orderId : orderIdList) {
        order = OBDal.getInstance().get(Order.class, orderId);
        OrderLine orderLine = order.getOrderLineList().get(0);
        InvoiceLine invoiceLine;

        if (i == 0) {
          invoiceLine = invoice.getInvoiceLineList().get(i);
        }

        else {
          invoiceLine = (InvoiceLine) DalUtil.copy(invoice.getInvoiceLineList().get(0), false);
          setGeneralData(invoiceLine);
          invoiceLine.setInvoice(invoice);
          invoice.getInvoiceLineList().add(invoiceLine);
        }

        invoiceLine.setSalesOrderLine(orderLine);
        invoiceLine.setLineNo(new Long((i + 1) * 10));
        invoiceLine.setInvoicedQuantity(quantityList.get(i));
        invoiceLine.setUnitPrice(priceList.get(i));
        invoiceLine.setListPrice(priceList.get(i));
        invoiceLine.setStandardPrice(priceList.get(i));
        invoiceLine.setLineNetAmount(quantityList.get(i).multiply(priceList.get(i)));
        invoiceLine.setTaxAmount(quantityList.get(i).multiply(priceList.get(i)));

        OBCriteria<ShipmentInOutLine> criteria = OBDal.getInstance().createCriteria(
            ShipmentInOutLine.class);
        criteria.add(Restrictions.eq(ShipmentInOutLine.PROPERTY_SALESORDERLINE, orderLine));
        if (criteria.list().size() > 0)
          invoiceLine.setGoodsShipmentLine(criteria.list().get(0));

        order.getInvoiceList().add(invoice);
        order.setReinvoice(true);
        orderLine.getInvoiceLineList().add(invoiceLine);
        orderLine.setInvoiceDate(invoice.getInvoiceDate());
        orderLine.setInvoicedQuantity(invoiceLine.getInvoicedQuantity());

        OBDal.getInstance().save(order);
        OBDal.getInstance().flush();
        OBDal.getInstance().refresh(order);

        i++;
      }

      OBDal.getInstance().save(invoice);
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(invoice);
      return invoice;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Create a new invoice from many movements
  private Invoice createInvoiceFromMovements(List<String> movementIdList, boolean issotrx,
      List<BigDecimal> priceList, BigDecimal quantity, int day) {
    try {
      BigDecimal priceAvg = getAveragePrice(priceList);

      Invoice invoice = cloneInvoice(
          OBDal.getInstance().get(ShipmentInOut.class, movementIdList.get(0))
              .getMaterialMgmtShipmentInOutLineList().get(0).getProduct().getId(), issotrx,
          priceAvg, quantity, day);

      int i = 0;
      for (String movementId : movementIdList) {
        ShipmentInOut movement = OBDal.getInstance().get(ShipmentInOut.class, movementId);
        ShipmentInOutLine movementLine = movement.getMaterialMgmtShipmentInOutLineList().get(0);
        InvoiceLine invoiceLine;

        if (i == 0) {
          invoiceLine = invoice.getInvoiceLineList().get(i);
        }

        else {
          invoiceLine = (InvoiceLine) DalUtil.copy(invoice.getInvoiceLineList().get(0), false);
          setGeneralData(invoiceLine);
          invoiceLine.setInvoice(invoice);
          invoice.getInvoiceLineList().add(invoiceLine);
        }

        invoice.getMaterialMgmtShipmentInOutList().add(movement);
        invoiceLine.setProduct(movementLine.getProduct());
        invoiceLine.setGoodsShipmentLine(movementLine);
        invoiceLine.setLineNo(new Long((i + 1) * 10));
        invoiceLine.setInvoicedQuantity(movementLine.getMovementQuantity());
        invoiceLine.setUnitPrice(priceList.get(i));
        invoiceLine.setListPrice(priceList.get(i));
        invoiceLine.setStandardPrice(priceList.get(i));
        invoiceLine.setLineNetAmount(movementLine.getMovementQuantity().multiply(priceList.get(i)));
        invoiceLine.setTaxAmount(movementLine.getMovementQuantity().multiply(priceList.get(i)));
        invoiceLine.setTaxableAmount(movementLine.getMovementQuantity().multiply(priceList.get(i)));

        if (movement.getSalesOrder() != null) {
          invoice.setSalesOrder(movement.getSalesOrder());
          invoice.setOrderDate(movement.getSalesOrder().getOrderDate());
          invoiceLine.setSalesOrderLine(movementLine.getSalesOrderLine());

          movement.getSalesOrder().getInvoiceList().add(invoice);
          movement.getSalesOrder().setReinvoice(true);
          movementLine.getSalesOrderLine().getInvoiceLineList().add(invoiceLine);
          movementLine.getSalesOrderLine().setInvoiceDate(invoice.getInvoiceDate());
          movementLine.getSalesOrderLine().setInvoicedQuantity(invoiceLine.getInvoicedQuantity());

          OBDal.getInstance().save(movement.getSalesOrder());
          OBDal.getInstance().flush();
          OBDal.getInstance().refresh(movement.getSalesOrder());
        }

        OBDal.getInstance().save(invoice);
        OBDal.getInstance().flush();
        OBDal.getInstance().refresh(invoice);

        movement.setInvoice(invoice);
        movementLine.setReinvoice(true);
        movementLine.getInvoiceLineList().add(invoiceLine);

        OBDal.getInstance().save(movement);
        OBDal.getInstance().flush();
        OBDal.getInstance().refresh(movement);

        i++;
      }
      return invoice;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Reactivate a invoice
  private Invoice reactivateInvoice(Invoice purchaseInvoice) {
    try {
      Invoice invoice = OBDal.getInstance().get(Invoice.class, purchaseInvoice.getId());
      invoice.setDocumentStatus("CO");
      invoice.setDocumentAction("RE");
      invoice.setPosted("N");
      OBDal.getInstance().save(invoice);
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(invoice);
      completeDocument(invoice);
      return invoice;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Update invoice product price
  private Invoice updateInvoiceProductPrice(Invoice invoice, BigDecimal price) {
    try {
      InvoiceLine invoiceLine = invoice.getInvoiceLineList().get(0);

      invoiceLine.setUpdated(new Date());
      invoiceLine.setUnitPrice(price);
      invoiceLine.setStandardPrice(price);
      invoiceLine.setLineNetAmount(invoiceLine.getInvoicedQuantity().multiply(price));

      OBDal.getInstance().save(invoice);
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(invoice);
      return invoice;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Create a new movement cloning a previous created one
  private ShipmentInOut cloneMovement(String productId, boolean issotrx, BigDecimal quantity,
      String locatorId, int day) {
    try {
      return cloneMovement(productId, issotrx, quantity, locatorId, null, day);
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Create a new movement cloning a previous created one
  private ShipmentInOut cloneMovement(String productId, boolean issotrx, BigDecimal quantity,
      String locatorId, String bpartnerId, int day) {
    try {
      ShipmentInOut movement;
      if (issotrx)
        movement = OBDal.getInstance().get(ShipmentInOut.class, MOVEMENTOUT_ID);
      else
        movement = OBDal.getInstance().get(ShipmentInOut.class, MOVEMENTIN_ID);

      ShipmentInOut movementClone = (ShipmentInOut) DalUtil.copy(movement, false);
      setGeneralData(movement);

      if (issotrx)
        movementClone.setDocumentNo(getDocumentNo(movementClone.getDocumentType()
            .getDocumentSequence().getId()));
      else
        movementClone.setDocumentNo(getDocumentNo(SHIPMENTIN_SEQUENCE_ID));

      movementClone.setMovementDate(DateUtils.addDays(new Date(), day));
      movementClone.setAccountingDate(DateUtils.addDays(new Date(), day));
      movementClone.setWarehouse(OBDal.getInstance().get(Locator.class, locatorId).getWarehouse());
      if (bpartnerId != null) {
        movementClone
            .setBusinessPartner(OBDal.getInstance().get(BusinessPartner.class, bpartnerId));
        movementClone.setPartnerAddress(OBDal.getInstance().get(BusinessPartner.class, bpartnerId)
            .getBusinessPartnerLocationList().get(0));
      }

      // Get the first line associated with the movement and clone it to the new movement
      ShipmentInOutLine movementLine = movement.getMaterialMgmtShipmentInOutLineList().get(0);
      ShipmentInOutLine movementLineClone = (ShipmentInOutLine) DalUtil.copy(movementLine, false);

      setGeneralData(movementLineClone);

      movementLineClone.setProduct(OBDal.getInstance().get(Product.class, productId));
      movementLineClone.setMovementQuantity(quantity);
      movementLineClone.setStorageBin(OBDal.getInstance().get(Locator.class, locatorId));
      if (bpartnerId != null)
        movementLineClone.setBusinessPartner(OBDal.getInstance().get(BusinessPartner.class,
            bpartnerId));

      movementLineClone.setShipmentReceipt(movementClone);
      movementClone.getMaterialMgmtShipmentInOutLineList().add(movementLineClone);

      OBDal.getInstance().save(movementClone);
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(movementClone);

      return movementClone;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Create a new Goods Movement cloning a previous created one
  private InternalMovement createGoodsMovement(String productId, BigDecimal quantity,
      String locatorFromId, String locatorToId, int day) {
    try {
      InternalMovement movement = OBProvider.getInstance().get(InternalMovement.class);
      setGeneralData(movement);
      movement.setName(OBDal.getInstance().get(Product.class, productId).getName() + " - "
          + formatDate(DateUtils.addDays(new Date(), day)));
      movement.setMovementDate(DateUtils.addDays(new Date(), day));
      movement.setPosted("N");
      movement.setDocumentNo(getDocumentNo(MOVEMENT_SEQUENCE_ID));

      InternalMovementLine movementLine = OBProvider.getInstance().get(InternalMovementLine.class);
      setGeneralData(movementLine);
      movementLine.setStorageBin(OBDal.getInstance().get(Locator.class, locatorFromId));
      movementLine.setNewStorageBin(OBDal.getInstance().get(Locator.class, locatorToId));
      movementLine.setProduct(OBDal.getInstance().get(Product.class, productId));
      movementLine.setLineNo(10L);
      movementLine.setMovementQuantity(quantity);
      movementLine.setUOM(OBDal.getInstance().get(UOM.class, UOM_ID));

      movementLine.setMovement(movement);
      movement.getMaterialMgmtInternalMovementLineList().add(movementLine);

      OBDal.getInstance().save(movement);
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(movement);
      return movement;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Create a new Goods Movement cloning a previous created one
  private InternalConsumption createInternalConsumption(String productId, BigDecimal quantity,
      String locatorId, int day) {
    try {
      InternalConsumption consumption = OBProvider.getInstance().get(InternalConsumption.class);
      setGeneralData(consumption);
      consumption.setName(OBDal.getInstance().get(Product.class, productId).getName() + " - "
          + formatDate(DateUtils.addDays(new Date(), day)));
      consumption.setMovementDate(DateUtils.addDays(new Date(), day));

      InternalConsumptionLine consumptionLine = OBProvider.getInstance().get(
          InternalConsumptionLine.class);
      setGeneralData(consumptionLine);
      consumptionLine.setStorageBin(OBDal.getInstance().get(Locator.class, locatorId));
      consumptionLine.setProduct(OBDal.getInstance().get(Product.class, productId));
      consumptionLine.setLineNo(10L);
      consumptionLine.setMovementQuantity(quantity);
      consumptionLine.setUOM(OBDal.getInstance().get(UOM.class, UOM_ID));

      consumptionLine.setInternalConsumption(consumption);
      consumption.getMaterialMgmtInternalConsumptionLineList().add(consumptionLine);

      OBDal.getInstance().save(consumption);
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(consumption);
      return consumption;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Cancel a internal consumption
  private void cancelInternalConsumption(String internalConsumptionId) {
    try {
      OBDal.getInstance().commitAndClose();
      String procedureName = "m_internal_consumption_post1";
      final List<Object> parameters = new ArrayList<Object>();
      parameters.add(null);
      parameters.add(internalConsumptionId);
      parameters.add("VO");
      CallStoredProcedure.getInstance().call(procedureName, parameters, null, true, false);
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Create a new movement from a order
  private ShipmentInOut createMovementFromOrder(String orderId, boolean issotrx,
      BigDecimal quantity, String locatorId, int day) {
    try {
      Order order = OBDal.getInstance().get(Order.class, orderId);
      ShipmentInOut movement = cloneMovement(order.getOrderLineList().get(0).getProduct().getId(),
          issotrx, quantity, locatorId, order.getBusinessPartner().getId(), day);

      movement.setSalesOrder(order);
      movement.setOrderDate(order.getOrderDate());
      movement.getMaterialMgmtShipmentInOutLineList().get(0)
          .setSalesOrderLine(order.getOrderLineList().get(0));
      movement.getMaterialMgmtShipmentInOutLineList().get(0).getOrderLineList()
          .add(order.getOrderLineList().get(0));

      OBDal.getInstance().save(movement);
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(movement);
      return movement;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Create a new movement from a invoice
  private ShipmentInOut createMovementFromInvoice(String invoiceId, boolean issotrx,
      BigDecimal quantity, String locatorId, int day) {
    try {
      Invoice invoice = OBDal.getInstance().get(Invoice.class, invoiceId);
      ShipmentInOut movement = cloneMovement(invoice.getInvoiceLineList().get(0).getProduct()
          .getId(), issotrx, quantity, locatorId, day);

      movement.setInvoice(invoice);
      movement.getMaterialMgmtShipmentInOutLineList().get(0).setReinvoice(true);
      movement.getMaterialMgmtShipmentInOutLineList().get(0).getInvoiceLineList()
          .add(invoice.getInvoiceLineList().get(0));

      if (invoice.getSalesOrder() != null) {
        movement.getMaterialMgmtShipmentInOutLineList().get(0)
            .setSalesOrderLine(invoice.getSalesOrder().getOrderLineList().get(0));
      }

      OBDal.getInstance().save(movement);
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(movement);

      invoice.getMaterialMgmtShipmentInOutList().add(movement);
      invoice.getInvoiceLineList().get(0)
          .setGoodsShipmentLine(movement.getMaterialMgmtShipmentInOutLineList().get(0));

      OBDal.getInstance().save(invoice);
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(invoice);

      return movement;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Create a new movement from many orders
  private ShipmentInOut createMovementFromOrders(List<String> orderIdList, boolean issotrx,
      BigDecimal quantity, String locatorId, int day) {
    try {
      ShipmentInOut movement = cloneMovement(
          OBDal.getInstance().get(Order.class, orderIdList.get(0)).getOrderLineList().get(0)
              .getProduct().getId(), issotrx, quantity, locatorId, day);

      int i = 0;
      for (String orderId : orderIdList) {
        Order order = OBDal.getInstance().get(Order.class, orderId);

        if (i > 0) {
          ShipmentInOutLine movementLine = (ShipmentInOutLine) DalUtil.copy(movement
              .getMaterialMgmtShipmentInOutLineList().get(0), false);
          setGeneralData(movementLine);
          movementLine.setShipmentReceipt(movement);
          movement.getMaterialMgmtShipmentInOutLineList().add(movementLine);
        }

        movement.setSalesOrder(order);
        movement.setOrderDate(order.getOrderDate());
        movement.getMaterialMgmtShipmentInOutLineList().get(i)
            .setProduct(order.getOrderLineList().get(0).getProduct());
        movement.getMaterialMgmtShipmentInOutLineList().get(i)
            .setSalesOrderLine(order.getOrderLineList().get(0));
        movement.getMaterialMgmtShipmentInOutLineList().get(i).getOrderLineList()
            .add(order.getOrderLineList().get(0));
        movement.getMaterialMgmtShipmentInOutLineList().get(i).setLineNo(new Long((i + 1) * 10));
        movement.getMaterialMgmtShipmentInOutLineList().get(i)
            .setMovementQuantity(order.getOrderLineList().get(0).getOrderedQuantity());

        OBDal.getInstance().save(movement);
        OBDal.getInstance().flush();
        OBDal.getInstance().refresh(movement);

        order.getMaterialMgmtShipmentInOutList().add(movement);
        order.getOrderLineList().get(0)
            .setGoodsShipmentLine(movement.getMaterialMgmtShipmentInOutLineList().get(i));
        order.getOrderLineList().get(0).getMaterialMgmtShipmentInOutLineList()
            .add(movement.getMaterialMgmtShipmentInOutLineList().get(i));

        OBDal.getInstance().save(order);
        OBDal.getInstance().flush();
        OBDal.getInstance().refresh(order);

        i++;
      }

      return movement;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Create a Inventory Amount Update
  private InventoryAmountUpdate createInventoryAmountUpdate(String productId,
      BigDecimal originalPrice, BigDecimal finalPrice, BigDecimal quantity, int day) {
    try {
      InventoryAmountUpdate inventoryAmountUpdate = OBProvider.getInstance().get(
          InventoryAmountUpdate.class);
      setGeneralData(inventoryAmountUpdate);

      final OBCriteria<DocumentType> criteria = OBDal.getInstance().createCriteria(
          DocumentType.class);
      criteria.add(Restrictions.eq(DocumentType.PROPERTY_NAME, "Inventory Amount Update"));
      DocumentType documentType = criteria.list().get(0);

      inventoryAmountUpdate.setDocumentType(documentType);
      inventoryAmountUpdate.setDocumentNo(getDocumentNo(inventoryAmountUpdate.getDocumentType()
          .getDocumentSequence().getId()));
      inventoryAmountUpdate.setDocumentDate(DateUtils.addDays(new Date(), day));
      OBDal.getInstance().save(inventoryAmountUpdate);

      InventoryAmountUpdateLine inventoryAmountUpdateLine = OBProvider.getInstance().get(
          InventoryAmountUpdateLine.class);
      setGeneralData(inventoryAmountUpdateLine);
      inventoryAmountUpdateLine.setReferenceDate(DateUtils.addDays(new Date(), day));
      inventoryAmountUpdateLine.setProduct(OBDal.getInstance().get(Product.class, productId));
      inventoryAmountUpdateLine.setWarehouse(OBDal.getInstance()
          .get(Warehouse.class, WAREHOUSE1_ID));
      inventoryAmountUpdateLine.setInventoryAmount(quantity.multiply(finalPrice));
      inventoryAmountUpdateLine.setCurrentInventoryAmount(quantity.multiply(originalPrice));
      inventoryAmountUpdateLine.setOnHandQty(quantity);
      inventoryAmountUpdateLine.setUnitCost(finalPrice);
      inventoryAmountUpdateLine.setCurrentUnitCost(originalPrice);
      inventoryAmountUpdateLine.setCaInventoryamt(inventoryAmountUpdate);

      inventoryAmountUpdate.getInventoryAmountUpdateLineList().add(inventoryAmountUpdateLine);

      OBDal.getInstance().save(inventoryAmountUpdate);
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(inventoryAmountUpdate);

      return inventoryAmountUpdate;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Create a Inventory Amount Update
  private ProductionTransaction createBillOfMaterialsProduction(String productId,
      BigDecimal quantity, String locatorId, int day) {
    try {
      ProductionTransaction billOfMaterialsProduction = OBProvider.getInstance().get(
          ProductionTransaction.class);
      setGeneralData(billOfMaterialsProduction);

      billOfMaterialsProduction.setName("BOM - "
          + OBDal.getInstance().get(Product.class, productId).getName());
      billOfMaterialsProduction.setMovementDate(DateUtils.addDays(new Date(), day));
      billOfMaterialsProduction.setDocumentNo(getDocumentNo(PRODUCTION_DOCUMENTSEQUENCE_ID));
      billOfMaterialsProduction.setSalesTransaction(true);

      ProductionPlan productionPlan = OBProvider.getInstance().get(ProductionPlan.class);
      setGeneralData(productionPlan);
      productionPlan.setProduction(billOfMaterialsProduction);
      productionPlan.setLineNo(10L);
      productionPlan.setProduct(OBDal.getInstance().get(Product.class, productId));
      productionPlan.setProductionQuantity(quantity);
      productionPlan.setStorageBin(OBDal.getInstance().get(Locator.class, locatorId));

      billOfMaterialsProduction.getMaterialMgmtProductionPlanList().add(productionPlan);

      OBDal.getInstance().save(billOfMaterialsProduction);
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(billOfMaterialsProduction);

      return billOfMaterialsProduction;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Create a Sales Order cloning a created one and book it
  private Order createReturnFromCustomer(String goodsShipmentId, BigDecimal price,
      BigDecimal quantity, int day) {
    try {
      ShipmentInOut goodsShipment = OBDal.getInstance().get(ShipmentInOut.class, goodsShipmentId);

      Order order = goodsShipment.getMaterialMgmtShipmentInOutLineList().get(0).getSalesOrderLine()
          .getSalesOrder();
      order.getOrderLineList().get(0).setGoodsShipmentLine(null);

      OBDal.getInstance().save(order);
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(order);

      Order returnFromCustomer = cloneOrder(goodsShipment.getMaterialMgmtShipmentInOutLineList()
          .get(0).getProduct().getId(), true, price, quantity, day);

      returnFromCustomer.setDocumentType(OBDal.getInstance().get(DocumentType.class,
          RFCORDER_DOCUMENTTYPE_ID));
      returnFromCustomer.setTransactionDocument(OBDal.getInstance().get(DocumentType.class,
          RFCORDER_DOCUMENTTYPE_ID));
      returnFromCustomer.setDocumentNo(getDocumentNo(returnFromCustomer.getDocumentType()
          .getDocumentSequence().getId()));
      returnFromCustomer.setSummedLineAmount(price.multiply(quantity.negate()));
      returnFromCustomer.setGrandTotalAmount(price.multiply(quantity.negate()));

      returnFromCustomer.getOrderLineList().get(0).setOrderedQuantity(quantity.negate());
      returnFromCustomer.getOrderLineList().get(0).setReservedQuantity(quantity.negate());
      returnFromCustomer.getOrderLineList().get(0)
          .setLineNetAmount(price.multiply(quantity.negate()));
      returnFromCustomer.getOrderLineList().get(0)
          .setGoodsShipmentLine(goodsShipment.getMaterialMgmtShipmentInOutLineList().get(0));

      OBDal.getInstance().save(returnFromCustomer);
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(returnFromCustomer);

      return returnFromCustomer;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Create a Sales Order cloning a created one and book it
  private ShipmentInOut createReturnMaterialReceipt(String returnFromCustomerId, BigDecimal price,
      BigDecimal quantity, String locatorId, int day) {
    try {
      Order returnFromCustomer = OBDal.getInstance().get(Order.class, returnFromCustomerId);
      ShipmentInOut returnMaterialReceipt = cloneMovement(returnFromCustomer.getOrderLineList()
          .get(0).getProduct().getId(), true, quantity, locatorId, day);

      returnMaterialReceipt.setDocumentType(OBDal.getInstance().get(DocumentType.class,
          RFCRECEIPT_DOCUMENTTYPE_ID));
      returnMaterialReceipt.setDocumentNo(getDocumentNo(returnMaterialReceipt.getDocumentType()
          .getDocumentSequence().getId()));

      returnMaterialReceipt.getMaterialMgmtShipmentInOutLineList().get(0)
          .setMovementQuantity(quantity.negate());
      returnMaterialReceipt.getMaterialMgmtShipmentInOutLineList().get(0)
          .setSalesOrderLine(returnFromCustomer.getOrderLineList().get(0));

      OBDal.getInstance().save(returnMaterialReceipt);
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(returnMaterialReceipt);

      return returnMaterialReceipt;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Create a new invoice from a order
  private Invoice createInvoiceLandedCost(String landedCostTypeId, BigDecimal price,
      BigDecimal quantity, int day) {
    try {
      Invoice invoice = cloneInvoice(LANDEDCOSTTYPE2_ID, false, price, quantity, day);
      InvoiceLine invoiceLine = invoice.getInvoiceLineList().get(0);

      if (landedCostTypeId.equals(LANDEDCOSTTYPE1_ID)) {
        invoiceLine.setAccount(OBDal.getInstance().get(GLItem.class, landedCostTypeId));
        invoiceLine.setFinancialInvoiceLine(true);
        invoiceLine.setProduct(null);
      }

      else if (landedCostTypeId.equals(LANDEDCOSTTYPE2_ID)) {
        OBCriteria<TaxRate> criteria = OBDal.getInstance().createCriteria(TaxRate.class);
        criteria.add(Restrictions.eq(TaxRate.PROPERTY_TAXCATEGORY,
            OBDal.getInstance().get(Product.class, landedCostTypeId).getTaxCategory()));
        criteria.add(Restrictions.eq(TaxRate.PROPERTY_ORGANIZATION,
            OBDal.getInstance().get(Organization.class, ORGANIZATION_ID)));
        invoiceLine.setTax(criteria.list().get(0));
      }

      else {
        invoice.setCurrency(OBDal.getInstance().get(Currency.class, CURRENCY2_ID));
        invoiceLine.setProduct(OBDal.getInstance().get(Product.class, landedCostTypeId));
      }

      OBDal.getInstance().save(invoice);
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(invoice);
      return invoice;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Create a Landed Cost from a list of purchase invoices and goods receipt
  private LandedCost createLandedCost(List<String> landedCostTypeIdList,
      List<BigDecimal> amountList, List<String> invoiceIdList, List<String> receiptIdList,
      List<String> receiptLineIdList, int day) {
    try {
      LandedCost landedCost = OBProvider.getInstance().get(LandedCost.class);
      setGeneralData(landedCost);
      landedCost.setReferenceDate(DateUtils.addDays(new Date(), day));
      landedCost.setDocumentType(OBDal.getInstance().get(DocumentType.class,
          LANDEDCOST_DOCUMENTTYPE_ID));
      landedCost.setDocumentNo(getDocumentNo(landedCost.getDocumentType().getDocumentSequence()
          .getId()));
      OBDal.getInstance().save(landedCost);

      for (int i = 0; i < (landedCostTypeIdList != null ? landedCostTypeIdList.size()
          : invoiceIdList.size()); i++) {
        LandedCostCost landedCostCost = OBProvider.getInstance().get(LandedCostCost.class);
        setGeneralData(landedCostCost);

        if (landedCostTypeIdList != null) {
          String landedCostTypeId = landedCostTypeIdList.get(i);

          if (landedCostTypeId.equals(LANDEDCOSTTYPE1_ID))
            landedCostCost.setLandedCostType(OBDal.getInstance()
                .get(GLItem.class, landedCostTypeId).getLandedCostTypeAccountList().get(0));
          else
            landedCostCost.setLandedCostType(OBDal.getInstance()
                .get(Product.class, landedCostTypeId).getLandedCostTypeList().get(0));

          landedCostCost.setAmount(amountList.get(i));

          if (landedCostTypeId.equals(LANDEDCOSTTYPE3_ID))
            landedCostCost.setCurrency(OBDal.getInstance().get(Currency.class, CURRENCY2_ID));
          else
            landedCostCost.setCurrency(OBDal.getInstance().get(Currency.class, CURRENCY1_ID));
        }

        else {
          String invoiceId = invoiceIdList.get(i);
          InvoiceLine invoiceLine = OBDal.getInstance().get(Invoice.class, invoiceId)
              .getInvoiceLineList().get(0);

          if (invoiceLine.getAccount() != null)
            landedCostCost.setLandedCostType(invoiceLine.getAccount()
                .getLandedCostTypeAccountList().get(0));
          else
            landedCostCost.setLandedCostType(invoiceLine.getProduct().getLandedCostTypeList()
                .get(0));

          landedCostCost.setInvoiceLine(invoiceLine);
          landedCostCost.setAmount(invoiceLine.getLineNetAmount());
          landedCostCost.setCurrency(invoiceLine.getInvoice().getCurrency());
        }

        landedCostCost.setLandedCostDistributionAlgorithm(OBDal.getInstance().get(
            LCDistributionAlgorithm.class, LANDEDCOSTCOST_ALGORITHM_ID));
        landedCostCost.setAccountingDate(DateUtils.addDays(new Date(), day));
        landedCostCost.setLineNo(new Long((i + 1) * 10));
        landedCostCost.setDocumentType(OBDal.getInstance().get(DocumentType.class,
            LANDEDCOSTCOST_DOCUMENTTYPE_ID));

        landedCostCost.setLandedCost(landedCost);
        landedCost.getLandedCostCostList().add(landedCostCost);

        OBDal.getInstance().save(landedCostCost);
        OBDal.getInstance().flush();
        OBDal.getInstance().refresh(landedCostCost);
      }

      for (int i = 0; i < receiptIdList.size(); i++) {
        LCReceipt landedCostReceipt = OBProvider.getInstance().get(LCReceipt.class);
        setGeneralData(landedCostReceipt);
        if (receiptIdList.get(i) != null)
          landedCostReceipt.setGoodsShipment(OBDal.getInstance().get(ShipmentInOut.class,
              receiptIdList.get(i)));
        if (receiptLineIdList.get(i) != null)
          landedCostReceipt.setGoodsShipmentLine(OBDal.getInstance().get(ShipmentInOutLine.class,
              receiptLineIdList.get(i)));
        landedCostReceipt.setLandedCost(landedCost);
        landedCost.getLandedCostReceiptList().add(landedCostReceipt);

        OBDal.getInstance().save(landedCostReceipt);
        OBDal.getInstance().flush();
        OBDal.getInstance().refresh(landedCostReceipt);
      }

      OBDal.getInstance().save(landedCost);
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(landedCost);
      return landedCost;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Create a list of Landed Cost Cost from a list of purchase invoices and a goods receipt
  private List<LandedCostCost> createLandedCostCost(List<String> invoiceIdList, String receiptId) {
    try {
      List<LandedCostCost> landedCostCostList = new ArrayList<LandedCostCost>();
      int i = 0;
      for (String invoiceId : invoiceIdList) {
        LandedCostCost landedCostCost = OBProvider.getInstance().get(LandedCostCost.class);
        InvoiceLine invoiceLine = OBDal.getInstance().get(Invoice.class, invoiceId)
            .getInvoiceLineList().get(0);
        setGeneralData(landedCostCost);

        if (invoiceLine.getAccount() != null)
          landedCostCost.setLandedCostType(invoiceLine.getAccount().getLandedCostTypeAccountList()
              .get(0));
        else
          landedCostCost.setLandedCostType(invoiceLine.getProduct().getLandedCostTypeList().get(0));

        landedCostCost.setGoodsShipment(OBDal.getInstance().get(ShipmentInOut.class, receiptId));
        landedCostCost.setInvoiceLine(invoiceLine);
        landedCostCost.setAmount(invoiceLine.getLineNetAmount());
        landedCostCost.setLandedCostDistributionAlgorithm(OBDal.getInstance().get(
            LCDistributionAlgorithm.class, LANDEDCOSTCOST_ALGORITHM_ID));
        landedCostCost.setCurrency(invoiceLine.getInvoice().getCurrency());
        landedCostCost.setAccountingDate(new Date());
        landedCostCost.setLineNo(new Long((i + 1) * 10));

        landedCostCostList.add(landedCostCost);
        OBDal.getInstance().save(landedCostCost);
        OBDal.getInstance().flush();
        OBDal.getInstance().refresh(landedCostCost);

        i++;
      }

      return landedCostCostList;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Create a conversion rate for an invoice
  private ConversionRateDoc createConversion(Invoice purchaseInvoice, BigDecimal rate) {
    try {
      ConversionRateDoc conversion = OBProvider.getInstance().get(ConversionRateDoc.class);
      setGeneralData(conversion);
      conversion.setCurrency(purchaseInvoice.getCurrency());
      conversion.setToCurrency(purchaseInvoice.getCurrency().getId().equals(CURRENCY1_ID) ? OBDal
          .getInstance().get(Currency.class, CURRENCY2_ID) : OBDal.getInstance().get(
          Currency.class, CURRENCY1_ID));
      conversion.setInvoice(purchaseInvoice);
      conversion.setRate(rate);
      conversion.setForeignAmount(purchaseInvoice.getInvoiceLineList().get(0).getLineNetAmount()
          .multiply(rate));

      OBDal.getInstance().save(conversion);
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(conversion);
      return conversion;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Change organization currency
  private void changeOrganizationCurrency(Organization organization, Currency currency) {
    try {
      organization.setCurrency(currency);
      OBDal.getInstance().save(organization);
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(organization);
      OBDal.getInstance().commitAndClose();
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Cancel landed cost cost matching
  private void cancelLandedCostCost(String landedCostCostId, String error) {
    try {
      OBDal.getInstance().commitAndClose();
      HashMap<String, Object> parameters = new HashMap<String, Object>();
      String content = "{\r    'M_LC_Cost_ID':'" + landedCostCostId + "', \r}";
      Object object = new LCMatchingCancelHandler();
      Class<? extends Object> clazz = object.getClass();
      Method method = clazz.getDeclaredMethod("execute", Map.class, String.class);
      method.setAccessible(true);
      String response = ((JSONObject) method.invoke(object, parameters, content)).toString();
      if (error == null) {
        assertTrue(response.contains("success"));
        assertFalse(response.contains("error"));
      } else {
        assertTrue(response.contains(error));
        assertTrue(response.contains("error"));
        assertFalse(response.contains("success"));
      }
      OBDal.getInstance().commitAndClose();
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Reactivate landed cost
  private void reactivateLandedCost(String landedCostId, String error) {
    try {
      OBDal.getInstance().commitAndClose();
      HashMap<String, Object> parameters = new HashMap<String, Object>();
      String content = "{\r    'inpmLandedcostId':'" + landedCostId + "', \r}";
      Object object = new ReactivateLandedCost();
      Class<? extends Object> clazz = object.getClass();
      Method method = clazz.getDeclaredMethod("execute", Map.class, String.class);
      method.setAccessible(true);
      String response = ((JSONObject) method.invoke(object, parameters, content)).toString();
      if (error == null) {
        assertTrue(response.contains("success"));
        assertFalse(response.contains("error"));
      } else {
        assertTrue(response.contains(error));
        assertTrue(response.contains("error"));
        assertFalse(response.contains("success"));
      }
      OBDal.getInstance().commitAndClose();
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Match a purchase invoice landed cost with a landed cost cost
  private void matchInvoiceLandedCost(String purchaseInvoiceLineLandedCostId,
      String landedCostCostId, BigDecimal amount, String landedCostMatchedId, boolean matching) {
    try {
      OBDal.getInstance().commitAndClose();
      HashMap<String, Object> parameters = new HashMap<String, Object>();
      String content = "{\r    'C_InvoiceLine_ID':'" + purchaseInvoiceLineLandedCostId + "', \r ";
      content += "'_params':{\r 'LCCosts':{\r '_selection':[\r {\r 'matched':false, \r ";
      content += "'isMatchingAdjusted':" + matching + ", \r 'processMatching':true, \r ";
      content += "'matchedAmt':" + amount + ", \r ";
      content += "'landedCostCost':'" + landedCostCostId + "', \r ";
      content += "'matchedLandedCost':'";
      content += landedCostMatchedId == null ? "" : landedCostMatchedId;
      content += "', \r}\r ]\r }\r }\r }";
      Object object = new LCCostMatchFromInvoiceHandler();
      Class<? extends Object> clazz = object.getClass();
      Method method = clazz.getDeclaredMethod("doExecute", Map.class, String.class);
      method.setAccessible(true);
      String response = ((JSONObject) method.invoke(object, parameters, content)).toString();
      assertTrue(response.contains("success"));
      assertFalse(response.contains("error"));
      OBDal.getInstance().commitAndClose();
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Match a purchase invoice landed cost with a landed cost cost
  private void matchInvoiceLandedCost(String landedCostCostId, boolean matching, String error) {
    try {
      OBDal.getInstance().commitAndClose();
      HashMap<String, Object> parameters = new HashMap<String, Object>();
      String content = "{\r    'M_LC_Cost_ID':'" + landedCostCostId + "', \r ";
      content += "'_params':{\r 'IsMatchingAdjusted':" + matching + "\r }\r}";
      Object object = new LCMatchingProcessHandler();
      Class<? extends Object> clazz = object.getClass();
      Method method = clazz.getDeclaredMethod("execute", Map.class, String.class);
      method.setAccessible(true);
      String response = ((JSONObject) method.invoke(object, parameters, content)).toString();
      if (error == null) {
        assertTrue(response.contains("success"));
        assertFalse(response.contains("error"));
      } else {
        assertTrue(response.contains(error));
        assertTrue(response.contains("error"));
        assertFalse(response.contains("success"));
      }
      OBDal.getInstance().commitAndClose();
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Unpost landed cost
  private void unpostDocument(BaseOBObject document) {
    try {
      final OBCriteria<AccountingFact> criteria = OBDal.getInstance().createCriteria(
          AccountingFact.class);
      criteria.add(Restrictions.eq(AccountingFact.PROPERTY_RECORDID, document.getId()));
      for (AccountingFact accountingFact : criteria.list())
        OBDal.getInstance().remove(accountingFact);
      BaseOBObject doc = OBDal.getInstance().get(document.getClass(), document.getId());
      doc.set("posted", "N");
      OBDal.getInstance().save(doc);
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(doc);
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Set common fields in all tables
  private static void setGeneralData(BaseOBObject document) {
    try {
      document.set("client", OBDal.getInstance().get(Client.class, CLIENT_ID));
      document.set("organization", OBDal.getInstance().get(Organization.class, ORGANIZATION_ID));
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Complete a document
  private BaseOBObject completeDocument(BaseOBObject document) {
    try {
      return completeDocument(document, null);
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Complete a document
  private BaseOBObject completeDocument(BaseOBObject document, String processId) {
    try {
      final OBCriteria<Table> criteria = OBDal.getInstance().createCriteria(Table.class);
      criteria.add(Restrictions.eq(Table.PROPERTY_NAME, document.getEntityName()));
      String procedureName = criteria.list().get(0).getDBTableName() + "_post";

      final List<Object> parameters = new ArrayList<Object>();
      if (processId == null) {
        parameters.add(null);
        parameters.add(document.getId());
      }

      else {
        ProcessInstance processInstance = OBProvider.getInstance().get(ProcessInstance.class);
        setGeneralData(processInstance);
        processInstance.setProcess(OBDal.getInstance().get(Process.class, processId));
        processInstance.setRecordID(document.getId().toString());
        processInstance.setClient(OBDal.getInstance().get(Client.class, CLIENT_ID));
        processInstance.setUserContact(OBDal.getInstance().get(User.class, USER_ID));
        OBDal.getInstance().save(processInstance);
        OBDal.getInstance().flush();
        OBDal.getInstance().refresh(processInstance);
        OBDal.getInstance().commitAndClose();
        parameters.add(processInstance.getId());
      }

      CallStoredProcedure.getInstance().call(procedureName, parameters, null, true, false);

      OBDal.getInstance().save(document);
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(document);
      return document;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Post a document
  private void postDocument(BaseOBObject document) {
    ConnectionProvider conn = getConnectionProvider();
    Connection con = null;

    try {
      final OBCriteria<Table> criteria = OBDal.getInstance().createCriteria(Table.class);
      criteria.add(Restrictions.eq(Table.PROPERTY_NAME, document.getEntityName()));
      String tableId = criteria.list().get(0).getId();
      con = conn.getTransactionConnection();
      AcctServer acct = AcctServer.get(tableId, ((Client) document.get("client")).getId(),
          ((Organization) document.get("organization")).getId(), conn);

      if (acct == null) {
        conn.releaseRollbackConnection(con);
        return;
      } else if (!acct.post((String) document.getId(), false,
          new VariablesSecureApp("100", ((Client) document.get("client")).getId(),
              ((Organization) document.get("organization")).getId()), conn, con)
          || acct.errors != 0) {
        conn.releaseRollbackConnection(con);
        return;
      }

      document.set("posted", "Y");

      conn.releaseCommitConnection(con);
      OBDal.getInstance().commitAndClose();
    } catch (Exception e) {
      try {
        conn.releaseRollbackConnection(con);
      } catch (Exception e2) {
        throw new OBException(e2);
      }
    }
    return;
  }

  // Run Verify BOM process
  private void verifyBOM(String productId) {
    try {
      OBDal.getInstance().commitAndClose();
      VariablesSecureApp vars = null;
      vars = new VariablesSecureApp(OBContext.getOBContext().getUser().getId(), OBContext
          .getOBContext().getCurrentClient().getId(), OBContext.getOBContext()
          .getCurrentOrganization().getId(), OBContext.getOBContext().getRole().getId(), OBContext
          .getOBContext().getLanguage().getLanguage());
      ConnectionProvider conn = new DalConnectionProvider(true);
      ProcessBundle pb = new ProcessBundle(VERIFYBOM_PROCESS_ID, vars).init(conn);
      HashMap<String, Object> parameters = new HashMap<String, Object>();
      parameters.put("M_Product_ID", productId);
      pb.setParams(parameters);
      new VerifyBOM().execute(pb);
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Process a Inventory Amount Update
  private void processInventoryAmountUpdate(String inventoryAmountUpdateId) {
    try {
      OBDal.getInstance().commitAndClose();
      HashMap<String, Object> parameters = new HashMap<String, Object>();
      String content = "{\r    'M_Ca_Inventoryamt_ID':'" + inventoryAmountUpdateId
          + "', \r    'inpadOrgId':'" + ORGANIZATION_ID + "', \r}";
      Object object = new InventoryAmountUpdateProcess();
      Class<? extends Object> clazz = object.getClass();
      Method method = clazz.getDeclaredMethod("execute", Map.class, String.class);
      method.setAccessible(true);
      String response = ((JSONObject) method.invoke(object, parameters, content)).toString();
      assertTrue(response.contains("success"));
      assertFalse(response.contains("error"));
      OBDal.getInstance().commitAndClose();
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Process a Bill of Materials Production
  private void processBillOfMaterialsProduction(ProductionTransaction billOfMaterialsProduction) {
    try {
      String procedureName = "m_production_run";
      final List<Object> parameters = new ArrayList<Object>();
      ProcessInstance processInstance = OBProvider.getInstance().get(ProcessInstance.class);
      setGeneralData(processInstance);
      processInstance.setProcess(OBDal.getInstance().get(Process.class,
          PROCESSPRODUCTION_PROCESS_ID));
      processInstance.setRecordID(billOfMaterialsProduction.getId());
      processInstance.setClient(OBDal.getInstance().get(Client.class, CLIENT_ID));
      processInstance.setUserContact(OBDal.getInstance().get(User.class, USER_ID));
      OBDal.getInstance().save(processInstance);
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(processInstance);
      OBDal.getInstance().commitAndClose();
      parameters.add(processInstance.getId());

      CallStoredProcedure.getInstance().call(procedureName, parameters, null, true, false);

      OBDal.getInstance().save(billOfMaterialsProduction);
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(billOfMaterialsProduction);
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Update transaction total cost amount
  private void manualCostAdjustment(String materialTransactionId, BigDecimal amount,
      boolean incremental, boolean unitCost, int day) {
    try {
      OBDal.getInstance().commitAndClose();
      HashMap<String, Object> parameters = new HashMap<String, Object>();
      String content = "{\r    'M_Transaction_ID':'" + materialTransactionId
          + "', \r    '_params':{\r        'Cost':" + amount.toString()
          + ", \r        'DateAcct':'" + formatDate(DateUtils.addDays(new Date(), day))
          + "', \r        'IsIncremental':" + incremental + ", \r        'IsUnitCost':" + unitCost
          + "\r    }\r}";
      Object object = new ManualCostAdjustmentProcessHandler();
      Class<? extends Object> clazz = object.getClass();
      Method method = clazz.getDeclaredMethod("execute", Map.class, String.class);
      method.setAccessible(true);
      String response = ((JSONObject) method.invoke(object, parameters, content)).toString();
      assertTrue(response.contains("success"));
      assertFalse(response.contains("error"));
      OBDal.getInstance().commitAndClose();
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Cancel cost adjusment
  private void cancelCostAdjustment(String costAdjusmentId) {
    try {
      OBDal.getInstance().commitAndClose();
      HashMap<String, Object> parameters = new HashMap<String, Object>();
      String content = "{\r    'inpmCostadjustmentId':'" + costAdjusmentId + "', \r}";
      Object object = new CancelCostAdjustment();
      Class<? extends Object> clazz = object.getClass();
      Method method = clazz.getDeclaredMethod("execute", Map.class, String.class);
      method.setAccessible(true);
      String response = ((JSONObject) method.invoke(object, parameters, content)).toString();
      assertTrue(response.contains("success"));
      assertFalse(response.contains("error"));
      OBDal.getInstance().commitAndClose();
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Process Landed Cost
  private void processLandedCost(String landedCostId) {
    try {
      OBDal.getInstance().commitAndClose();
      HashMap<String, Object> parameters = new HashMap<String, Object>();
      String content = "{\r    'M_Landedcost_ID':'" + landedCostId + "', \r}";
      Object object = new LandedCostProcessHandler();
      Class<? extends Object> clazz = object.getClass();
      Method method = clazz.getDeclaredMethod("execute", Map.class, String.class);
      method.setAccessible(true);
      String response = ((JSONObject) method.invoke(object, parameters, content)).toString();
      assertTrue(response.contains("success"));
      assertFalse(response.contains("error"));
      OBDal.getInstance().commitAndClose();
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Run Costing Background process
  private static void runCostingBackground() {
    try {
      VariablesSecureApp vars = null;
      vars = new VariablesSecureApp(OBContext.getOBContext().getUser().getId(), OBContext
          .getOBContext().getCurrentClient().getId(), OBContext.getOBContext()
          .getCurrentOrganization().getId(), OBContext.getOBContext().getRole().getId(), OBContext
          .getOBContext().getLanguage().getLanguage());
      ConnectionProvider conn = new DalConnectionProvider(true);
      ProcessBundle pb = new ProcessBundle(CostingBackground.AD_PROCESS_ID, vars).init(conn);
      HashMap<String, Object> parameters = new HashMap<String, Object>();
      pb.setParams(parameters);
      new CostingBackground().execute(pb);
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Run Price Correction Background
  private static void runPriceBackground() {
    try {
      VariablesSecureApp vars = null;
      vars = new VariablesSecureApp(OBContext.getOBContext().getUser().getId(), OBContext
          .getOBContext().getCurrentClient().getId(), OBContext.getOBContext()
          .getCurrentOrganization().getId(), OBContext.getOBContext().getRole().getId(), OBContext
          .getOBContext().getLanguage().getLanguage());
      ConnectionProvider conn = new DalConnectionProvider(true);
      ProcessBundle pb = new ProcessBundle(PriceDifferenceBackground.AD_PROCESS_ID, vars)
          .init(conn);
      HashMap<String, Object> parameters = new HashMap<String, Object>();
      pb.setParams(parameters);
      new PriceDifferenceBackground().execute(pb);
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Validate Costing Rule
  private static void validateCostingRule(String costingRuleId) {
    try {
      OBDal.getInstance().commitAndClose();
      VariablesSecureApp vars = null;
      vars = new VariablesSecureApp(OBContext.getOBContext().getUser().getId(), OBContext
          .getOBContext().getCurrentClient().getId(), OBContext.getOBContext()
          .getCurrentOrganization().getId(), OBContext.getOBContext().getRole().getId(), OBContext
          .getOBContext().getLanguage().getLanguage());
      ConnectionProvider conn = new DalConnectionProvider(true);
      ProcessBundle pb = new ProcessBundle(VALIDATECOSTINGRULE_PROCESS_ID, vars).init(conn);
      HashMap<String, Object> parameters = new HashMap<String, Object>();
      parameters.put("M_Costing_Rule_ID", costingRuleId);
      pb.setParams(parameters);
      new CostingRuleProcess().execute(pb);
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Calculates the next document number for this sequence
  private String getDocumentNo(String sequenceId) {
    try {
      Sequence sequence = OBDal.getInstance().get(Sequence.class, sequenceId);
      String prefix = sequence.getPrefix() == null ? "" : sequence.getPrefix();
      String suffix = sequence.getSuffix() == null ? "" : sequence.getSuffix();
      String documentNo = prefix + sequence.getNextAssignedNumber().toString() + suffix;
      sequence.setNextAssignedNumber(sequence.getNextAssignedNumber() + sequence.getIncrementBy());
      return documentNo;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Calculates the average price of a price list
  private BigDecimal getAveragePrice(List<BigDecimal> priceList) {
    try {
      BigDecimal priceAvg = BigDecimal.ZERO;
      for (BigDecimal price : priceList)
        priceAvg = priceAvg.add(price);
      return priceAvg.divide(new BigDecimal(priceList.size()));
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Calculates the average price of a price list
  private BigDecimal getAveragePrice(List<BigDecimal> priceList, List<BigDecimal> quantityList) {
    try {
      BigDecimal priceTotal = BigDecimal.ZERO;
      for (int i = 0; i < quantityList.size(); i++)
        priceTotal = priceTotal.add(quantityList.get(i).multiply(priceList.get(i)));
      return priceTotal.divide(getTotalQuantity(quantityList), 5, BigDecimal.ROUND_HALF_UP);
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Calculates the total amount of a quantity list
  private BigDecimal getTotalQuantity(List<BigDecimal> quantityList) {
    try {
      BigDecimal quantityTotal = BigDecimal.ZERO;
      for (BigDecimal quantity : quantityList)
        quantityTotal = quantityTotal.add(quantity);
      return quantityTotal;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Get Cost Adjustments created for a product
  @SuppressWarnings("unchecked")
  private List<CostAdjustment> getCostAdjustment(String productId) {
    try {
      String myQuery = "SELECT DISTINCT t1 "
          + "FROM CostAdjustment t1 LEFT JOIN t1.costAdjustmentLineList t2 LEFT JOIN t2.inventoryTransaction t3 "
          + "WHERE t3.product.id = :productId " + "ORDER BY t1.documentNo";
      Query query = OBDal.getInstance().getSession().createQuery(myQuery);
      query.setString("productId", productId);
      List<CostAdjustment> costAdjustmentList = query.list();
      return costAdjustmentList.isEmpty() ? null : costAdjustmentList;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Get Physical Inventory
  @SuppressWarnings("unchecked")
  private List<InventoryCount> getPhysicalInventory(String inventoryAmountUpdateId) {
    try {
      String myQuery = "SELECT t1 "
          + "FROM MaterialMgmtInventoryCount t1, InventoryAmountUpdate t2 LEFT JOIN t2.inventoryAmountUpdateLineList t3 LEFT JOIN t3.inventoryAmountUpdateLineInventoriesList t4 "
          + "WHERE (t4.initInventory = t1 OR t4.closeInventory = t1) AND t2.id = :inventoryAmountUpdateId "
          + "ORDER BY t1.name";
      Query query = OBDal.getInstance().getSession().createQuery(myQuery);
      query.setString("inventoryAmountUpdateId", inventoryAmountUpdateId);
      return query.list();
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Get Product Transaction list
  private List<MaterialTransaction> getProductTransactions(String productId) {
    try {
      OBCriteria<MaterialTransaction> criteria = OBDal.getInstance().createCriteria(
          MaterialTransaction.class);
      criteria.add(Restrictions.eq(MaterialTransaction.PROPERTY_PRODUCT,
          OBDal.getInstance().get(Product.class, productId)));
      criteria.addOrderBy(MaterialTransaction.PROPERTY_MOVEMENTDATE, true);
      criteria.addOrderBy(MaterialTransaction.PROPERTY_MOVEMENTQUANTITY, true);
      return criteria.list();
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Get Product Transaction list
  private List<TransactionCost> getProductTransactionCosts(String transactionId) {
    try {
      StringBuffer where = new StringBuffer();
      where.append(" as t1 ");
      where.append("\n left join t1." + TransactionCost.PROPERTY_COSTADJUSTMENTLINE + " t2");
      where.append("\n left join t2." + CostAdjustmentLine.PROPERTY_COSTADJUSTMENT + " t3");
      where.append("\n where t1." + TransactionCost.PROPERTY_INVENTORYTRANSACTION
          + " = :transaction");
      where.append("\n order by t3." + CostAdjustment.PROPERTY_DOCUMENTNO + " desc");
      where.append("\n , t2." + CostAdjustmentLine.PROPERTY_LINENO + " desc");
      OBQuery<TransactionCost> hql = OBDal.getInstance().createQuery(TransactionCost.class,
          where.toString());
      hql.setNamedParameter("transaction",
          OBDal.getInstance().get(MaterialTransaction.class, transactionId));
      return hql.list();
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Get Product Costing list
  private List<Costing> getProductCostings(String productId) {
    try {
      // Ordenar por la inventory transaction también
      StringBuffer where = new StringBuffer();
      where.append(" as t1 ");
      where.append("\n join t1." + Costing.PROPERTY_WAREHOUSE + " t2");
      where.append("\n where t1." + Costing.PROPERTY_PRODUCT + " = :product");
      where.append("\n order by t1." + Costing.PROPERTY_MANUAL + " desc");
      where.append("\n , t1." + Costing.PROPERTY_COSTTYPE + " desc");
      where.append("\n , t2." + Warehouse.PROPERTY_NAME + " desc");
      where.append("\n , t1." + Costing.PROPERTY_ENDINGDATE);
      where.append("\n , t1." + Costing.PROPERTY_TOTALMOVEMENTQUANTITY);
      OBQuery<Costing> hql = OBDal.getInstance().createQuery(Costing.class, where.toString());
      hql.setNamedParameter("product", OBDal.getInstance().get(Product.class, productId));
      return hql.list();
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Get Production Line list
  private List<ProductionLine> getProductionLines(String productionTransactionId) {
    try {
      StringBuffer where = new StringBuffer();
      where.append(" as t1 ");
      where.append("\n left join t1." + ProductionLine.PROPERTY_PRODUCTIONPLAN + " t2");
      where.append("\n left join t1." + ProductionLine.PROPERTY_PRODUCT + " t3");
      where.append("\n where t2." + ProductionPlan.PROPERTY_PRODUCTION
          + " = :productionTransaction");
      where.append("\n order by t3." + Product.PROPERTY_NAME);
      OBQuery<ProductionLine> hql = OBDal.getInstance().createQuery(ProductionLine.class,
          where.toString());
      hql.setNamedParameter("productionTransaction",
          OBDal.getInstance().get(ProductionTransaction.class, productionTransactionId));
      return hql.list();
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Get transaction amount
  private BigDecimal getTransactionAmount(ShipmentInOut transaction) {
    try {
      BigDecimal amount = BigDecimal.ZERO;
      for (ShipmentInOutLine transactionLine : transaction.getMaterialMgmtShipmentInOutLineList())
        amount = amount.add(getTransactionLineAmount(transactionLine));
      return amount;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Get transaction line amount
  private BigDecimal getTransactionLineAmount(ShipmentInOutLine transactionLine) {
    try {
      OBCriteria<AccountingFact> criteria = OBDal.getInstance()
          .createCriteria(AccountingFact.class);
      criteria.add(Restrictions.eq(AccountingFact.PROPERTY_LINEID, transactionLine.getId()));
      criteria.addOrderBy(AccountingFact.PROPERTY_SEQUENCENUMBER, true);
      return criteria.list().get(transactionLine.getShipmentReceipt().isSalesTransaction() ? 1 : 0)
          .getDebit();
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Assert common fields in all tables
  private void assertGeneralData(BaseOBObject document) {
    try {
      assertEquals(((Client) document.get("client")).getId(), CLIENT_ID);
      assertEquals(((Organization) document.get("organization")).getName(), "Spain");
      assertTrue(((Boolean) document.get("active")));
      assertEquals(((User) document.get("createdBy")).getId(), USER_ID);
      assertEquals(((User) document.get("updatedBy")).getId(), USER_ID);
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Assert Matched Invoices
  private void assertMatchedInvoice(ReceiptInvoiceMatch receiptInvoiceMatch,
      MatchedInvoicesAssert matchedInvoicesAssert) {
    try {
      assertGeneralData(receiptInvoiceMatch);
      assertEquals(receiptInvoiceMatch.getGoodsShipmentLine(),
          matchedInvoicesAssert.getMovementLine());
      assertEquals(receiptInvoiceMatch.getInvoiceLine(), matchedInvoicesAssert.getInvoiceLine());
      assertEquals(receiptInvoiceMatch.getProduct(), matchedInvoicesAssert.getInvoiceLine()
          .getProduct());

      assertEquals(formatDate(receiptInvoiceMatch.getTransactionDate()),
          formatDate(matchedInvoicesAssert.getInvoiceLine().getInvoice().getInvoiceDate()));
      assertEquals(receiptInvoiceMatch.getQuantity(), matchedInvoicesAssert.getMovementLine()
          .getMovementQuantity());

      assertTrue(receiptInvoiceMatch.isProcessed());
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Assert Physical Inventory
  private void assertPhysicalInventory(List<InventoryCount> physicalInventoryList,
      PhysicalInventoryAssert physicalInventoryAssert) {
    try {
      int i = 0;
      for (InventoryCount physicalInventory : physicalInventoryList) {
        assertGeneralData(physicalInventory);
        assertGeneralData(physicalInventory.getMaterialMgmtInventoryCountLineList().get(0));

        if (i % 2 == 0) {
          assertEquals(physicalInventory.getName(), "Inventory Amount Update Closing Inventory");
          assertEquals(physicalInventory.getInventoryType(), "C");

          assertEquals(physicalInventory.getMaterialMgmtInventoryCountLineList().get(0)
              .getBookQuantity(), physicalInventoryAssert.getQuantity());
          assertEquals(physicalInventory.getMaterialMgmtInventoryCountLineList().get(0)
              .getQuantityCount(), BigDecimal.ZERO);
          assertEquals(physicalInventory.getMaterialMgmtInventoryCountLineList().get(0).getCost(),
              null);
          assertEquals(physicalInventory.getMaterialMgmtInventoryCountLineList().get(0)
              .getRelatedInventory(), physicalInventoryList.get(i + 1)
              .getMaterialMgmtInventoryCountLineList().get(0));
        }

        else {
          assertEquals(physicalInventory.getName(), "Inventory Amount Update Opening Inventory");
          assertEquals(physicalInventory.getInventoryType(), "O");

          assertEquals(physicalInventory.getMaterialMgmtInventoryCountLineList().get(0)
              .getBookQuantity(), BigDecimal.ZERO);
          assertEquals(physicalInventory.getMaterialMgmtInventoryCountLineList().get(0)
              .getQuantityCount(), physicalInventoryAssert.getQuantity());
          assertEquals(physicalInventory.getMaterialMgmtInventoryCountLineList().get(0).getCost()
              .setScale(2, BigDecimal.ROUND_HALF_UP), physicalInventoryAssert.getPrice());
          assertEquals(physicalInventory.getMaterialMgmtInventoryCountLineList().get(0)
              .getRelatedInventory(), null);
        }

        assertEquals(physicalInventory.getDescription(), null);
        assertEquals(physicalInventory.getWarehouse(),
            OBDal.getInstance().get(Warehouse.class, WAREHOUSE1_ID));
        assertEquals(formatDate(physicalInventory.getMovementDate()),
            formatDate(DateUtils.addDays(new Date(), physicalInventoryAssert.getDay())));
        assertTrue(physicalInventory.isProcessed());
        assertFalse(physicalInventory.isUpdateQuantities());
        assertFalse(physicalInventory.isGenerateList());
        assertEquals(physicalInventory.getTrxOrganization(), null);
        assertEquals(physicalInventory.getProject(), null);
        assertEquals(physicalInventory.getSalesCampaign(), null);
        assertEquals(physicalInventory.getActivity(), null);
        assertEquals(physicalInventory.getStDimension(), null);
        assertEquals(physicalInventory.getNdDimension(), null);
        assertEquals(physicalInventory.getCostCenter(), null);
        assertEquals(physicalInventory.getAsset(), null);

        assertEquals(physicalInventory.getMaterialMgmtInventoryCountLineList().get(0)
            .getPhysInventory(), physicalInventory);
        assertEquals(physicalInventory.getMaterialMgmtInventoryCountLineList().get(0)
            .getStorageBin(), OBDal.getInstance().get(Locator.class, LOCATOR1_ID));
        assertEquals(physicalInventory.getMaterialMgmtInventoryCountLineList().get(0).getProduct(),
            physicalInventoryAssert.getProduct());
        assertEquals(physicalInventory.getMaterialMgmtInventoryCountLineList().get(0).getLineNo(),
            new Long(10));
        assertEquals(physicalInventory.getMaterialMgmtInventoryCountLineList().get(0)
            .getDescription(), null);
        assertEquals(physicalInventory.getMaterialMgmtInventoryCountLineList().get(0)
            .getAttributeSetValue(), OBDal.getInstance().get(AttributeSetInstance.class, "0"));
        assertEquals(
            physicalInventory.getMaterialMgmtInventoryCountLineList().get(0).getOrderUOM(), null);
        assertEquals(physicalInventory.getMaterialMgmtInventoryCountLineList().get(0)
            .getOrderQuantity(), null);
        assertEquals(physicalInventory.getMaterialMgmtInventoryCountLineList().get(0).getUOM()
            .getName(), "Unit");
        assertEquals(physicalInventory.getMaterialMgmtInventoryCountLineList().get(0)
            .getQuantityOrderBook(), null);

        i++;
      }
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Assert Landed Cost Cost Matched
  private void assertLandedCostCostMatched(String landedCostCostId,
      List<LandedCostCostMatchedAssert> landedCostCostMatchedAssertList) {
    try {
      LandedCostCost landedCostCost = OBDal.getInstance().get(LandedCostCost.class,
          landedCostCostId);
      assertEquals(landedCostCost.getLandedCostMatchedList().size(),
          landedCostCostMatchedAssertList.size());

      OBCriteria<LCMatched> criteria1 = OBDal.getInstance().createCriteria(LCMatched.class);
      criteria1.add(Restrictions.eq(LCMatched.PROPERTY_LANDEDCOSTCOST, landedCostCost));
      criteria1.addOrderBy(LCMatched.PROPERTY_CREATIONDATE, true);
      List<LCMatched> landedCostCostMatchedList = criteria1.list();

      if (!landedCostCostMatchedList
          .get(0)
          .getAmount()
          .setScale(2, BigDecimal.ROUND_HALF_UP)
          .equals(
              landedCostCostMatchedAssertList.get(0).getInvoiceLine().getLineNetAmount()
                  .setScale(2, BigDecimal.ROUND_HALF_UP)))
        Collections.reverse(landedCostCostMatchedList);

      int i = 0;
      for (LCMatched landedCostCostMatched : landedCostCostMatchedList) {
        assertGeneralData(landedCostCostMatched);
        assertEquals(landedCostCostMatched.getLandedCostCost(), landedCostCost);
        assertEquals(landedCostCostMatched.getInvoiceLine(), landedCostCostMatchedAssertList.get(i)
            .getInvoiceLine());

        if (i == 0) {
          assertEquals(
              landedCostCostMatched.getAmount().setScale(2, BigDecimal.ROUND_HALF_UP),
              landedCostCostMatchedAssertList.get(i).getInvoiceLine().getLineNetAmount()
                  .setScale(2, BigDecimal.ROUND_HALF_UP));
          assertEquals(
              landedCostCostMatched.getAmountInInvoiceCurrency() == null ? landedCostCostMatched.getAmountInInvoiceCurrency()
                  : landedCostCostMatched.getAmountInInvoiceCurrency().setScale(2,
                      BigDecimal.ROUND_HALF_UP), landedCostCost.getInvoiceLine() != null ? null
                  : landedCostCostMatchedAssertList.get(i).getInvoiceLine().getLineNetAmount()
                      .setScale(2, BigDecimal.ROUND_HALF_UP));
          assertFalse(landedCostCostMatched.isConversionmatching());
        }

        else {
          Calendar calendar = Calendar.getInstance();
          calendar.set(9999, 0, 1);
          OBCriteria<ConversionRate> criteria2 = OBDal.getInstance().createCriteria(
              ConversionRate.class);
          criteria2.add(Restrictions.eq(ConversionRate.PROPERTY_CLIENT,
              OBDal.getInstance().get(Client.class, CLIENT_ID)));
          criteria2.add(Restrictions.eq(ConversionRate.PROPERTY_CURRENCY,
              OBDal.getInstance().get(Currency.class, CURRENCY2_ID)));
          criteria2.add(Restrictions.eq(ConversionRate.PROPERTY_TOCURRENCY, OBDal.getInstance()
              .get(Currency.class, CURRENCY1_ID)));
          criteria2.add(Restrictions.ge(ConversionRate.PROPERTY_VALIDTODATE, calendar.getTime()));
          BigDecimal rate = criteria2.list().get(0).getMultipleRateBy();

          assertEquals(
              landedCostCostMatched.getAmount().setScale(2, BigDecimal.ROUND_HALF_UP),
              landedCostCostMatchedAssertList
                  .get(i)
                  .getInvoiceLine()
                  .getLineNetAmount()
                  .multiply(
                      landedCostCostMatchedAssertList.get(i).getInvoiceLine().getInvoice()
                          .getCurrencyConversionRateDocList().get(0).getRate())
                  .add(
                      landedCostCostMatchedAssertList.get(i).getInvoiceLine().getLineNetAmount()
                          .multiply(rate).negate()).divide(rate, 2, BigDecimal.ROUND_HALF_UP));
          assertEquals(landedCostCostMatched.getAmountInInvoiceCurrency(), null);
          assertTrue(landedCostCostMatched.isConversionmatching());
        }

        i++;
      }
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Assert Landed Cost Receipt Line Amount
  private void assertLandedCostReceiptLineAmount(String landedCostReceiptId,
      List<LandedCostReceiptLineAmountAssert> landedCostReceiptLineAmountAssertList) {
    try {
      LCReceipt landedCostReceipt = OBDal.getInstance().get(LCReceipt.class, landedCostReceiptId);
      assertEquals(landedCostReceipt.getLandedCostReceiptLineAmtList().size(),
          landedCostReceiptLineAmountAssertList.size());

      StringBuffer where = new StringBuffer();
      where.append(" as t1 ");
      where.append("\n left join t1." + LCReceiptLineAmt.PROPERTY_LANDEDCOSTCOST + " t2");
      where.append("\n left join t1." + LCReceiptLineAmt.PROPERTY_GOODSSHIPMENTLINE + " t3");
      where.append("\n where t1." + LCReceiptLineAmt.PROPERTY_LANDEDCOSTRECEIPT
          + " = :landedCostReceipt");
      where.append("\n order by t2." + LandedCostCost.PROPERTY_LINENO);
      where.append("\n , t3." + ShipmentInOutLine.PROPERTY_LINENO);
      OBQuery<LCReceiptLineAmt> criteria = OBDal.getInstance().createQuery(LCReceiptLineAmt.class,
          where.toString());
      criteria.setNamedParameter("landedCostReceipt", landedCostReceipt);
      List<LCReceiptLineAmt> landedCostReceiptLineAmountList = criteria.list();

      if (landedCostReceiptLineAmountList.size() > 0
          && !landedCostReceiptLineAmountList
              .get(0)
              .getAmount()
              .setScale(4, BigDecimal.ROUND_HALF_UP)
              .equals(
                  landedCostReceiptLineAmountAssertList.get(0).getAmount()
                      .setScale(4, BigDecimal.ROUND_HALF_UP)))
        Collections.reverse(landedCostReceiptLineAmountList);

      int i = 0;
      for (LCReceiptLineAmt landedCostReceiptLineAmount : landedCostReceiptLineAmountList) {
        LandedCostReceiptLineAmountAssert landedCostReceiptLineAmountAssert = landedCostReceiptLineAmountAssertList
            .get(i);
        assertGeneralData(landedCostReceiptLineAmount);

        assertEquals(landedCostReceiptLineAmount.getAmount().setScale(4, BigDecimal.ROUND_HALF_UP),
            landedCostReceiptLineAmountAssert.getAmount().setScale(4, BigDecimal.ROUND_HALF_UP));
        assertEquals(landedCostReceiptLineAmount.getLandedCostReceipt(), landedCostReceipt);
        assertEquals(landedCostReceiptLineAmount.getLandedCostCost(),
            landedCostReceiptLineAmountAssert.getLandedCostCost());
        assertEquals(landedCostReceiptLineAmount.getGoodsShipmentLine(),
            landedCostReceiptLineAmountAssert.getReceiptLine());

        i++;
      }
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Assert Product Transactions
  private void assertProductTransaction(String productId,
      List<ProductTransactionAssert> productTransactionAssertList) {
    try {
      List<MaterialTransaction> materialTransactionList = getProductTransactions(productId);
      assertEquals(materialTransactionList.size(), productTransactionAssertList.size());

      int i = 0;
      int j = 0;
      for (MaterialTransaction materialTransaction : materialTransactionList) {
        ProductTransactionAssert productTransactionAssert = productTransactionAssertList.get(i);
        assertGeneralData(materialTransaction);

        assertEquals(materialTransaction.getProjectIssue(), null);
        assertEquals(materialTransaction.getAttributeSetValue(),
            OBDal.getInstance().get(AttributeSetInstance.class, "0"));
        assertEquals(materialTransaction.getOrderUOM(), null);
        assertEquals(materialTransaction.getOrderQuantity(), null);

        assertEquals(formatDate(materialTransaction.getTransactionProcessDate()),
            formatDate(new Date()));
        assertFalse(materialTransaction.isManualcostadjustment());
        assertEquals(materialTransaction.isCheckpricedifference(),
            productTransactionAssert.isPriceDifference());
        assertEquals(materialTransaction.isCostPermanent(), productTransactionAssert.isPermanent());

        if (productTransactionAssert.getOriginalPrice() != null) {
          assertEquals(materialTransaction.getCurrency(), productTransactionAssert.getCurrency());
          assertEquals(materialTransaction.getCostingAlgorithm().getName(), "Average Algorithm");
          assertTrue(materialTransaction.isCostCalculated());
          assertEquals(materialTransaction.getCostingStatus(), "CC");
          assertTrue(materialTransaction.isProcessed());
        }

        else {
          assertEquals(materialTransaction.getCurrency(), null);
          assertEquals(materialTransaction.getCostingAlgorithm(), null);
          assertFalse(materialTransaction.isCostCalculated());
          assertEquals(materialTransaction.getCostingStatus(), "NC");
          assertFalse(materialTransaction.isProcessed());
        }

        if (productTransactionAssert.getShipmentReceiptLine() != null) {

          if (!productTransactionAssert.getShipmentReceiptLine().getShipmentReceipt()
              .isSalesTransaction())
            assertEquals(materialTransaction.getMovementQuantity(), productTransactionAssert
                .getShipmentReceiptLine().getMovementQuantity());
          else
            assertEquals(materialTransaction.getMovementQuantity(), productTransactionAssert
                .getShipmentReceiptLine().getMovementQuantity().negate());

          if ((!productTransactionAssert.getShipmentReceiptLine().getShipmentReceipt()
              .isSalesTransaction() && productTransactionAssert.getShipmentReceiptLine()
              .getCanceledInoutLine() == null)
              || (productTransactionAssert.getShipmentReceiptLine().getShipmentReceipt()
                  .isSalesTransaction() && productTransactionAssert.getShipmentReceiptLine()
                  .getCanceledInoutLine() != null)
              || productTransactionAssert.getShipmentReceiptLine().getShipmentReceipt()
                  .getDocumentType().getName().equals("RFC Receipt")) {
            assertEquals(
                materialTransaction.getTransactionCost().setScale(2, BigDecimal.ROUND_HALF_UP),
                productTransactionAssert.getOriginalPrice().multiply(
                    materialTransaction.getMovementQuantity()));
            assertEquals(
                materialTransaction.getTotalCost().setScale(2, BigDecimal.ROUND_HALF_UP),
                productTransactionAssert.getTotalPrice()
                    .multiply(materialTransaction.getMovementQuantity())
                    .setScale(2, BigDecimal.ROUND_HALF_UP));
            assertEquals(
                materialTransaction.getUnitCost().setScale(2, BigDecimal.ROUND_HALF_UP),
                productTransactionAssert.getUnitPrice().multiply(
                    materialTransaction.getMovementQuantity()));
          }

          else {
            assertEquals(
                materialTransaction.getTransactionCost().setScale(2, BigDecimal.ROUND_HALF_UP),
                productTransactionAssert.getOriginalPrice()
                    .multiply(materialTransaction.getMovementQuantity().negate())
                    .setScale(2, BigDecimal.ROUND_HALF_UP));
            assertEquals(
                materialTransaction.getTotalCost().setScale(2, BigDecimal.ROUND_HALF_UP),
                productTransactionAssert.getTotalPrice()
                    .multiply(materialTransaction.getMovementQuantity().negate())
                    .setScale(2, BigDecimal.ROUND_HALF_UP));
            assertEquals(
                materialTransaction.getUnitCost().setScale(2, BigDecimal.ROUND_HALF_UP),
                productTransactionAssert.getUnitPrice()
                    .multiply(materialTransaction.getMovementQuantity().negate())
                    .setScale(2, BigDecimal.ROUND_HALF_UP));
          }

          assertEquals(materialTransaction.getGoodsShipmentLine(),
              productTransactionAssert.getShipmentReceiptLine());
          assertEquals(materialTransaction.getPhysicalInventoryLine(),
              productTransactionAssert.getInventoryLine());
          assertEquals(materialTransaction.getMovementLine(),
              productTransactionAssert.getMovementLine());
          assertEquals(materialTransaction.getInternalConsumptionLine(),
              productTransactionAssert.getConsumptionLine());
          assertEquals(materialTransaction.getProductionLine(),
              productTransactionAssert.getProductionLine());
          assertEquals(materialTransaction.getMovementType(), productTransactionAssert
              .getShipmentReceiptLine().getShipmentReceipt().getMovementType());
          assertEquals(materialTransaction.getStorageBin(), productTransactionAssert
              .getShipmentReceiptLine().getStorageBin());
          assertEquals(materialTransaction.getProduct(), productTransactionAssert
              .getShipmentReceiptLine().getProduct());
          assertEquals(formatDate(materialTransaction.getMovementDate()),
              formatDate(productTransactionAssert.getShipmentReceiptLine().getShipmentReceipt()
                  .getMovementDate()));
          assertEquals(materialTransaction.getUOM(), productTransactionAssert
              .getShipmentReceiptLine().getUOM());
          assertTrue(materialTransaction.isCheckReservedQuantity());
        }

        else if (productTransactionAssert.getInventoryLine() != null) {

          if (j % 2 == 0) {
            assertEquals(materialTransaction.getPhysicalInventoryLine(), productTransactionAssert
                .getInventoryLine().getInventoryAmountUpdateLineInventoriesList().get(0)
                .getCloseInventory().getMaterialMgmtInventoryCountLineList().get(0));
            assertEquals(materialTransaction.getMovementQuantity(), productTransactionAssert
                .getInventoryLine().getOnHandQty().negate());
            assertEquals(
                materialTransaction.getTransactionCost().setScale(2, BigDecimal.ROUND_HALF_UP),
                productTransactionAssert.getOriginalPrice()
                    .multiply(materialTransaction.getMovementQuantity().negate())
                    .setScale(2, BigDecimal.ROUND_HALF_UP));
            assertEquals(
                materialTransaction.getTotalCost().setScale(2, BigDecimal.ROUND_HALF_UP),
                productTransactionAssert.getTotalPrice()
                    .multiply(materialTransaction.getMovementQuantity().negate())
                    .setScale(2, BigDecimal.ROUND_HALF_UP));
            assertEquals(
                materialTransaction.getUnitCost().setScale(2, BigDecimal.ROUND_HALF_UP),
                productTransactionAssert.getUnitPrice()
                    .multiply(materialTransaction.getMovementQuantity().negate())
                    .setScale(2, BigDecimal.ROUND_HALF_UP));
            assertFalse(materialTransaction.isCheckReservedQuantity());
          }

          else {
            assertEquals(materialTransaction.getPhysicalInventoryLine(), productTransactionAssert
                .getInventoryLine().getInventoryAmountUpdateLineInventoriesList().get(0)
                .getInitInventory().getMaterialMgmtInventoryCountLineList().get(0));
            assertEquals(materialTransaction.getMovementQuantity(), productTransactionAssert
                .getInventoryLine().getOnHandQty());
            assertEquals(
                materialTransaction.getTransactionCost().setScale(2, BigDecimal.ROUND_HALF_UP),
                productTransactionAssert.getOriginalPrice().multiply(
                    materialTransaction.getMovementQuantity()));
            assertEquals(
                materialTransaction.getTotalCost().setScale(2, BigDecimal.ROUND_HALF_UP),
                productTransactionAssert.getTotalPrice().multiply(
                    materialTransaction.getMovementQuantity()));
            assertEquals(
                materialTransaction.getUnitCost().setScale(2, BigDecimal.ROUND_HALF_UP),
                productTransactionAssert.getUnitPrice().multiply(
                    materialTransaction.getMovementQuantity()));
            assertFalse(materialTransaction.isCheckReservedQuantity());
          }

          assertEquals(materialTransaction.getGoodsShipmentLine(),
              productTransactionAssert.getShipmentReceiptLine());
          assertEquals(materialTransaction.getMovementLine(),
              productTransactionAssert.getMovementLine());
          assertEquals(materialTransaction.getInternalConsumptionLine(),
              productTransactionAssert.getConsumptionLine());
          assertEquals(materialTransaction.getProductionLine(),
              productTransactionAssert.getProductionLine());
          assertEquals(materialTransaction.getMovementType(), "I+");
          assertEquals(materialTransaction.getStorageBin(), productTransactionAssert
              .getInventoryLine().getInventoryAmountUpdateLineInventoriesList().get(0)
              .getCloseInventory().getMaterialMgmtInventoryCountLineList().get(0).getStorageBin());
          assertEquals(materialTransaction.getProduct(), productTransactionAssert
              .getInventoryLine().getProduct());
          assertEquals(formatDate(materialTransaction.getMovementDate()),
              formatDate(productTransactionAssert.getInventoryLine().getCaInventoryamt()
                  .getDocumentDate()));
          assertEquals(materialTransaction.getUOM(), productTransactionAssert.getInventoryLine()
              .getInventoryAmountUpdateLineInventoriesList().get(0).getCloseInventory()
              .getMaterialMgmtInventoryCountLineList().get(0).getUOM());

          j++;
        }

        else if (productTransactionAssert.getMovementLine() != null) {

          if (j % 2 == 0) {
            assertEquals(materialTransaction.getMovementQuantity(), productTransactionAssert
                .getMovementLine().getMovementQuantity().negate());
            assertEquals(
                materialTransaction.getTransactionCost().setScale(2, BigDecimal.ROUND_HALF_UP),
                productTransactionAssert.getOriginalPrice().multiply(
                    materialTransaction.getMovementQuantity().negate()));
            assertEquals(
                materialTransaction.getTotalCost().setScale(2, BigDecimal.ROUND_HALF_UP),
                productTransactionAssert.getTotalPrice().multiply(
                    materialTransaction.getMovementQuantity().negate()));
            assertEquals(
                materialTransaction.getUnitCost().setScale(2, BigDecimal.ROUND_HALF_UP),
                productTransactionAssert.getUnitPrice().multiply(
                    materialTransaction.getMovementQuantity().negate()));
            assertEquals(materialTransaction.getMovementType(), "M-");
            assertEquals(materialTransaction.getStorageBin(), productTransactionAssert
                .getMovementLine().getStorageBin());
          }

          else {
            assertEquals(materialTransaction.getMovementQuantity(), productTransactionAssert
                .getMovementLine().getMovementQuantity());
            assertEquals(
                materialTransaction.getTransactionCost().setScale(2, BigDecimal.ROUND_HALF_UP),
                productTransactionAssert.getOriginalPrice().multiply(
                    materialTransaction.getMovementQuantity()));
            assertEquals(
                materialTransaction.getTotalCost().setScale(2, BigDecimal.ROUND_HALF_UP),
                productTransactionAssert.getTotalPrice().multiply(
                    materialTransaction.getMovementQuantity()));
            assertEquals(
                materialTransaction.getUnitCost().setScale(2, BigDecimal.ROUND_HALF_UP),
                productTransactionAssert.getUnitPrice().multiply(
                    materialTransaction.getMovementQuantity()));
            assertEquals(materialTransaction.getMovementType(), "M+");
            assertEquals(materialTransaction.getStorageBin(), productTransactionAssert
                .getMovementLine().getNewStorageBin());
          }

          assertEquals(materialTransaction.getGoodsShipmentLine(),
              productTransactionAssert.getShipmentReceiptLine());
          assertEquals(materialTransaction.getPhysicalInventoryLine(),
              productTransactionAssert.getInventoryLine());
          assertEquals(materialTransaction.getMovementLine(),
              productTransactionAssert.getMovementLine());
          assertEquals(materialTransaction.getInternalConsumptionLine(),
              productTransactionAssert.getConsumptionLine());
          assertEquals(materialTransaction.getProductionLine(),
              productTransactionAssert.getProductionLine());
          assertEquals(materialTransaction.getProduct(), productTransactionAssert.getMovementLine()
              .getProduct());
          assertEquals(
              formatDate(materialTransaction.getMovementDate()),
              formatDate(productTransactionAssert.getMovementLine().getMovement().getMovementDate()));
          assertEquals(materialTransaction.getUOM(), productTransactionAssert.getMovementLine()
              .getUOM());
          assertTrue(materialTransaction.isCheckReservedQuantity());

          j++;
        }

        else if (productTransactionAssert.getProductionLine() != null) {

          assertEquals(
              materialTransaction.getTransactionCost().setScale(2, BigDecimal.ROUND_HALF_UP),
              productTransactionAssert.getOriginalPrice().multiply(
                  materialTransaction.getMovementQuantity().abs()));
          assertEquals(
              materialTransaction.getTotalCost().setScale(2, BigDecimal.ROUND_HALF_UP),
              productTransactionAssert.getTotalPrice().multiply(
                  materialTransaction.getMovementQuantity().abs()));
          assertEquals(
              materialTransaction.getUnitCost().setScale(2, BigDecimal.ROUND_HALF_UP),
              productTransactionAssert.getUnitPrice().multiply(
                  materialTransaction.getMovementQuantity().abs()));

          assertEquals(materialTransaction.getMovementQuantity(), productTransactionAssert
              .getProductionLine().getMovementQuantity());
          assertEquals(materialTransaction.getMovementType(), "P+");
          assertEquals(materialTransaction.getStorageBin(), productTransactionAssert
              .getProductionLine().getStorageBin());

          assertEquals(materialTransaction.getGoodsShipmentLine(),
              productTransactionAssert.getShipmentReceiptLine());
          assertEquals(materialTransaction.getPhysicalInventoryLine(),
              productTransactionAssert.getInventoryLine());
          assertEquals(materialTransaction.getMovementLine(),
              productTransactionAssert.getMovementLine());
          assertEquals(materialTransaction.getInternalConsumptionLine(),
              productTransactionAssert.getConsumptionLine());
          assertEquals(materialTransaction.getProduct(), productTransactionAssert
              .getProductionLine().getProduct());
          assertEquals(formatDate(materialTransaction.getMovementDate()),
              formatDate(productTransactionAssert.getProductionLine().getProductionPlan()
                  .getProduction().getMovementDate()));
          assertEquals(materialTransaction.getUOM(), productTransactionAssert.getProductionLine()
              .getUOM());
          assertTrue(materialTransaction.isCheckReservedQuantity());

          j++;
        }

        else {

          if (j % 2 == 0) {
            assertEquals(materialTransaction.getMovementQuantity(), productTransactionAssert
                .getConsumptionLine().getMovementQuantity().negate());
            assertEquals(
                materialTransaction.getTransactionCost().setScale(2, BigDecimal.ROUND_HALF_UP),
                productTransactionAssert.getOriginalPrice().multiply(
                    productTransactionAssert.getConsumptionLine().getMovementQuantity()));
            assertEquals(
                materialTransaction.getTotalCost().setScale(2, BigDecimal.ROUND_HALF_UP),
                productTransactionAssert.getTotalPrice().multiply(
                    productTransactionAssert.getConsumptionLine().getMovementQuantity()));
            assertEquals(
                materialTransaction.getUnitCost().setScale(2, BigDecimal.ROUND_HALF_UP),
                productTransactionAssert.getUnitPrice().multiply(
                    productTransactionAssert.getConsumptionLine().getMovementQuantity()));
            assertTrue(materialTransaction.isCheckReservedQuantity());
          }

          else {
            assertEquals(materialTransaction.getMovementQuantity(), productTransactionAssert
                .getConsumptionLine().getMovementQuantity().negate());
            assertEquals(
                materialTransaction.getTransactionCost().setScale(2, BigDecimal.ROUND_HALF_UP),
                productTransactionAssert.getOriginalPrice().multiply(
                    productTransactionAssert.getConsumptionLine().getMovementQuantity().negate()));
            assertEquals(
                materialTransaction.getTotalCost().setScale(2, BigDecimal.ROUND_HALF_UP),
                productTransactionAssert.getTotalPrice().multiply(
                    productTransactionAssert.getConsumptionLine().getMovementQuantity().negate()));
            assertEquals(
                materialTransaction.getUnitCost().setScale(2, BigDecimal.ROUND_HALF_UP),
                productTransactionAssert.getUnitPrice().multiply(
                    productTransactionAssert.getConsumptionLine().getMovementQuantity().negate()));
            assertTrue(materialTransaction.isCheckReservedQuantity());
          }

          assertEquals(materialTransaction.getGoodsShipmentLine(),
              productTransactionAssert.getShipmentReceiptLine());
          assertEquals(materialTransaction.getPhysicalInventoryLine(),
              productTransactionAssert.getInventoryLine());
          assertEquals(materialTransaction.getMovementLine(),
              productTransactionAssert.getMovementLine());
          assertEquals(materialTransaction.getInternalConsumptionLine(),
              productTransactionAssert.getConsumptionLine());

          assertEquals(materialTransaction.getMovementType(), "D-");
          assertEquals(materialTransaction.getStorageBin(), productTransactionAssert
              .getConsumptionLine().getStorageBin());
          assertEquals(materialTransaction.getProduct(), productTransactionAssert
              .getConsumptionLine().getProduct());
          assertEquals(formatDate(materialTransaction.getMovementDate()),
              formatDate(productTransactionAssert.getConsumptionLine().getInternalConsumption()
                  .getMovementDate()));
          assertEquals(materialTransaction.getUOM(), productTransactionAssert.getConsumptionLine()
              .getUOM());

          j++;
        }

        StringBuffer where = new StringBuffer();
        where.append(" as t1 ");
        where.append("\n left join t1." + CostAdjustmentLine.PROPERTY_COSTADJUSTMENT + " t2");
        where.append("\n where t1." + CostAdjustmentLine.PROPERTY_INVENTORYTRANSACTION
            + " = :transaction");
        where.append("\n order by t2." + CostAdjustment.PROPERTY_DOCUMENTNO + " desc");
        where.append("\n , t1." + CostAdjustmentLine.PROPERTY_LINENO + " desc");
        OBQuery<CostAdjustmentLine> hql = OBDal.getInstance().createQuery(CostAdjustmentLine.class,
            where.toString());
        hql.setNamedParameter("transaction", materialTransaction);
        List<CostAdjustmentLine> costAdjustmentLineList = hql.list();

        if (productTransactionAssert.getOriginalPrice() != null)
          assertEquals(materialTransaction.getTransactionCostList().size(),
              costAdjustmentLineList.size() + 1);
        else
          assertEquals(materialTransaction.getTransactionCostList().size(),
              costAdjustmentLineList.size());

        int k = 0;
        for (TransactionCost materialTransactionCost : getProductTransactionCosts(materialTransaction
            .getId())) {
          assertGeneralData(materialTransactionCost);
          assertEquals(materialTransactionCost.getInventoryTransaction(), materialTransaction);
          assertEquals(formatDate(materialTransactionCost.getCostDate()),
              formatDate(materialTransaction.getTransactionProcessDate()));
          assertEquals(materialTransactionCost.getCurrency(),
              productTransactionAssert.getCurrency());

          if (k == 0) {
            assertEquals(materialTransactionCost.getCost(),
                materialTransaction.getTransactionCost());
            assertEquals(materialTransactionCost.getCostAdjustmentLine(), null);
            assertEquals(formatDate(materialTransactionCost.getAccountingDate()),
                formatDate(materialTransaction.getMovementDate()));
            assertTrue(materialTransactionCost.isUnitCost());
          }

          else {

            Calendar calendar = Calendar.getInstance();
            calendar.set(9999, 0, 1);
            OBCriteria<ConversionRate> criteria2 = OBDal.getInstance().createCriteria(
                ConversionRate.class);
            criteria2.add(Restrictions.eq(ConversionRate.PROPERTY_CLIENT,
                OBDal.getInstance().get(Client.class, CLIENT_ID)));
            criteria2.add(Restrictions.eq(ConversionRate.PROPERTY_CURRENCY, OBDal.getInstance()
                .get(Currency.class, CURRENCY2_ID)));
            criteria2.add(Restrictions.eq(ConversionRate.PROPERTY_TOCURRENCY, OBDal.getInstance()
                .get(Currency.class, CURRENCY1_ID)));
            criteria2.add(Restrictions.ge(ConversionRate.PROPERTY_VALIDTODATE, calendar.getTime()));
            BigDecimal rate = criteria2.list().get(0).getMultipleRateBy();

            if (productTransactionAssert.getCurrency().getId().equals(CURRENCY1_ID)
                && costAdjustmentLineList.get(k - 1).getCurrency().getId().equals(CURRENCY2_ID))
              assertEquals(
                  materialTransactionCost.getCost().setScale(4, BigDecimal.ROUND_HALF_UP),
                  costAdjustmentLineList.get(k - 1).getAdjustmentAmount().multiply(rate)
                      .setScale(4, BigDecimal.ROUND_HALF_UP));

            else
              assertEquals(materialTransactionCost.getCost(), costAdjustmentLineList.get(k - 1)
                  .getAdjustmentAmount());

            assertEquals(materialTransactionCost.getCostAdjustmentLine(),
                costAdjustmentLineList.get(k - 1));
            assertEquals(formatDate(materialTransactionCost.getAccountingDate()),
                formatDate(costAdjustmentLineList.get(k - 1).getAccountingDate()));
            assertEquals(materialTransactionCost.isUnitCost(), costAdjustmentLineList.get(k - 1)
                .isUnitCost());
          }

          k++;
        }

        i++;
      }
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Assert Product Costing
  private void assertProductCosting(String productId,
      List<ProductCostingAssert> productCostingAssertList) {
    try {
      Product product = OBDal.getInstance().get(Product.class, productId);
      List<Costing> productCostingList = getProductCostings(productId);
      assertEquals(productCostingList.size(), productCostingAssertList.size());

      List<ProductCostingAssert> productCostingAssertList2 = new ArrayList<ProductCostingAssert>(
          productCostingAssertList);
      Collections.reverse(productCostingAssertList2);
      int j = 0;
      for (ProductCostingAssert productCostingAssert : productCostingAssertList2) {
        if (productCostingAssert.getWarehouse().getId().equals(WAREHOUSE1_ID))
          break;
        else
          j++;
      }
      int indexWarehouse1 = productCostingAssertList2.size() - 1 - j;

      j = 0;
      for (ProductCostingAssert productCostingAssert : productCostingAssertList2) {
        if (productCostingAssert.getWarehouse().getId().equals(WAREHOUSE2_ID))
          break;
        else
          j++;
      }
      int indexWarehouse2 = productCostingAssertList2.size() - 1 - j;

      int i = 0;
      for (Costing productCosting : productCostingList) {

        ProductCostingAssert productCostingAssert = productCostingAssertList.get(i);
        assertGeneralData(productCosting);

        assertEquals(productCosting.getCost().setScale(4, BigDecimal.ROUND_HALF_UP),
            productCostingAssert.getFinalCost().setScale(4, BigDecimal.ROUND_HALF_UP));

        assertEquals(
            productCosting.getPrice() == null ? null : productCosting.getPrice().setScale(4,
                BigDecimal.ROUND_HALF_UP), productCostingAssert.getPrice() == null ? null
                : productCostingAssert.getPrice().setScale(4, BigDecimal.ROUND_HALF_UP));

        assertEquals(productCosting.getOriginalCost() == null ? null : productCosting
            .getOriginalCost().setScale(4, BigDecimal.ROUND_HALF_UP),
            productCostingAssert.getOriginalCost() == null ? null : productCostingAssert
                .getOriginalCost().setScale(4, BigDecimal.ROUND_HALF_UP));

        assertEquals(productCosting.getTotalMovementQuantity(), productCostingAssert.getQuantity());

        if (productCostingAssert.getQuantity() == null)
          assertEquals(productCosting.getQuantity(), null);
        else
          assertEquals(productCosting.getQuantity(), productCostingAssert.getTransaction()
              .getMovementQuantity());

        assertEquals(productCosting.isManual(), productCostingAssert.isManual());
        assertEquals(productCosting.isPermanent(), !productCostingAssert.isManual());

        assertEquals(productCosting.getGoodsShipmentLine(), null);
        assertEquals(productCosting.getInvoiceLine(), null);

        assertEquals(productCosting.getProductionLine(), null);
        assertFalse(productCosting.isProduction());
        assertEquals(productCosting.getWarehouse(), productCostingAssert.getWarehouse());
        assertEquals(productCosting.getInventoryTransaction(),
            productCostingAssert.getTransaction());
        assertEquals(productCosting.getCurrency(),
            productCostingAssert.getTransaction() != null ? productCostingAssert.getTransaction()
                .getCurrency() : OBDal.getInstance().get(Currency.class, CURRENCY1_ID));
        assertEquals(productCosting.getCostType(), productCostingAssert.getType());

        if (productCostingAssert.getYear() != 0)
          assertEquals(
              formatDate(productCosting.getStartingDate()),
              formatDate(DateUtils.addYears(product.getPricingProductPriceList().get(0)
                  .getPriceListVersion().getValidFromDate(), productCostingAssert.getYear())));

        else
          assertEquals(formatDate(productCosting.getStartingDate()), formatDate(new Date()));

        if (productCostingAssert.getType().equals("STA") || i == indexWarehouse1
            || i == indexWarehouse2) {
          Calendar calendar = Calendar.getInstance();
          calendar.set(9999, 11, 31);
          assertEquals(formatDate(productCosting.getEndingDate()), formatDate(calendar.getTime()));
        } else {
          assertEquals(formatDate(productCosting.getEndingDate()), formatDate(productCostingList
              .get(i + 1).getStartingDate()));
        }

        i++;
      }
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Assert Cost Adjustment
  private void assertCostAdjustment(List<CostAdjustment> costAdjustmentList,
      List<List<CostAdjustmentAssert>> costAdjustmentAssertList) {
    try {

      // Assert Cost Adjustment header data
      assertEquals(costAdjustmentList.size(), costAdjustmentAssertList.size());
      int i = 0;
      for (CostAdjustment costAdjustment : costAdjustmentList) {

        List<CostAdjustmentAssert> costAdjustmentAssertLineList = costAdjustmentAssertList.get(i);
        assertGeneralData(costAdjustment);
        assertEquals(costAdjustment.getDocumentType().getName(), "Cost Adjustment");
        assertEquals(formatDate(costAdjustment.getReferenceDate()), formatDate(new Date()));
        assertEquals(costAdjustment.getSourceProcess(), costAdjustmentAssertLineList.get(0)
            .getType());
        assertTrue(costAdjustment.isProcessed());
        assertFalse(costAdjustment.isProcess());
        assertEquals(costAdjustment.getDocumentStatus(), costAdjustmentAssertLineList.get(0)
            .getStatus());
        assertFalse(costAdjustment.isCancelProcess());
        assertEquals(costAdjustment.getCostAdjustmentLineList().size(),
            costAdjustmentAssertLineList.size());

        if (costAdjustmentAssertLineList.get(0).getStatus().equals("VO")) {
          OBCriteria<CostAdjustmentLine> criteria = OBDal.getInstance().createCriteria(
              CostAdjustmentLine.class);
          criteria.add(Restrictions.eq(CostAdjustmentLine.PROPERTY_INVENTORYTRANSACTION,
              costAdjustmentAssertLineList.get(0).getMaterialTransaction()));
          criteria.add(Restrictions.eq(CostAdjustmentLine.PROPERTY_ADJUSTMENTAMOUNT,
              costAdjustmentAssertLineList.get(0).getAmount().negate()));
          criteria.add(Restrictions.ne(CostAdjustmentLine.PROPERTY_COSTADJUSTMENT, costAdjustment));
          assertEquals(costAdjustment.getCostAdjustmentCancel(), criteria.list().get(0)
              .getCostAdjustment().getCostAdjustmentCancel() != null ? null : criteria.list()
              .get(0).getCostAdjustment());
        } else
          assertEquals(costAdjustment.getCostAdjustmentCancel(), null);

        // Assert Cost Adjustment lines data
        int j = 0;
        for (CostAdjustmentLine costAdjustmentLine : costAdjustment.getCostAdjustmentLineList()) {

          CostAdjustmentAssert costAdjustmentAssertLine = costAdjustmentAssertLineList.get(j);
          assertGeneralData(costAdjustment);

          assertEquals(costAdjustmentLine.getCostAdjustment(), costAdjustment);
          assertEquals(costAdjustmentLine.getInventoryTransaction(),
              costAdjustmentAssertLine.getMaterialTransaction());
          assertEquals(costAdjustmentLine.getLineNo(), new Long((j + 1) * 10));

          assertEquals(
              costAdjustmentLine.getAdjustmentAmount().setScale(2, BigDecimal.ROUND_HALF_UP),
              costAdjustmentAssertLine.getAmount().setScale(2, BigDecimal.ROUND_HALF_UP));
          assertEquals(costAdjustmentLine.isSource(), costAdjustmentAssertLine.isSource());
          assertEquals(costAdjustmentLine.isUnitCost(), costAdjustmentAssertLine.isUnit());
          assertEquals(formatDate(costAdjustmentLine.getAccountingDate()),
              formatDate(DateUtils.addDays(new Date(), costAdjustmentAssertLine.getDay())));
          assertTrue(costAdjustmentLine.isRelatedTransactionAdjusted());
          assertEquals(costAdjustmentLine.getCurrency(), costAdjustmentAssertLine.getCurrency());

          if (costAdjustmentAssertLine.getType().equals("NSC")) {
            assertFalse(costAdjustmentLine.isBackdatedTrx());
            assertTrue(costAdjustmentLine.isNegativeStockCorrection());
          } else if (costAdjustmentAssertLine.getType().equals("BDT")) {
            assertTrue(costAdjustmentLine.isBackdatedTrx());
            assertFalse(costAdjustmentLine.isNegativeStockCorrection());
          } else {
            assertFalse(costAdjustmentLine.isBackdatedTrx());
            assertFalse(costAdjustmentLine.isNegativeStockCorrection());
          }

          if (costAdjustmentAssertLine.getAmount().setScale(2, BigDecimal.ROUND_HALF_UP)
              .equals(BigDecimal.ZERO.setScale(2, BigDecimal.ROUND_HALF_UP))
              || costAdjustmentAssertLine.getType().equals("LC"))
            assertFalse(costAdjustmentLine.isNeedsPosting());
          else
            assertTrue(costAdjustmentLine.isNeedsPosting());

          if (j == 0
              || (j == 1 && costAdjustmentAssertLine.isSource() && !costAdjustmentLine
                  .getInventoryTransaction()
                  .getStorageBin()
                  .equals(
                      costAdjustment.getCostAdjustmentLineList().get(j - 1)
                          .getInventoryTransaction().getStorageBin()))
              || (j == 1 && !costAdjustmentLine
                  .getInventoryTransaction()
                  .getProduct()
                  .equals(
                      costAdjustment.getCostAdjustmentLineList().get(j - 1)
                          .getInventoryTransaction().getProduct()))
              || (j == 2
                  && !costAdjustmentLine
                      .getInventoryTransaction()
                      .getProduct()
                      .equals(
                          costAdjustment.getCostAdjustmentLineList().get(j - 2)
                              .getInventoryTransaction().getProduct()) && !costAdjustmentLine
                  .getInventoryTransaction()
                  .getProduct()
                  .equals(
                      costAdjustment.getCostAdjustmentLineList().get(j - 1)
                          .getInventoryTransaction().getProduct()))
              || (j == 3
                  && !costAdjustmentLine.getInventoryTransaction().getProduct().isBillOfMaterials()
                  && !costAdjustmentLine
                      .getInventoryTransaction()
                      .getProduct()
                      .equals(
                          costAdjustment.getCostAdjustmentLineList().get(j - 3)
                              .getInventoryTransaction().getProduct())
                  && !costAdjustmentLine
                      .getInventoryTransaction()
                      .getProduct()
                      .equals(
                          costAdjustment.getCostAdjustmentLineList().get(j - 2)
                              .getInventoryTransaction().getProduct()) && !costAdjustmentLine
                  .getInventoryTransaction()
                  .getProduct()
                  .equals(
                      costAdjustment.getCostAdjustmentLineList().get(j - 1)
                          .getInventoryTransaction().getProduct())))
            assertEquals(costAdjustmentLine.getParentCostAdjustmentLine(), null);
          else if (costAdjustmentLine
              .getInventoryTransaction()
              .getProduct()
              .equals(
                  costAdjustment.getCostAdjustmentLineList().get(0).getInventoryTransaction()
                      .getProduct())
              && (costAdjustmentLine
                  .getInventoryTransaction()
                  .getStorageBin()
                  .equals(
                      costAdjustment.getCostAdjustmentLineList().get(0).getInventoryTransaction()
                          .getStorageBin()) || costAdjustmentAssertLineList.size() == 2))
            assertEquals(costAdjustmentLine.getParentCostAdjustmentLine(), costAdjustment
                .getCostAdjustmentLineList().get(0));
          else if (costAdjustmentLine
              .getInventoryTransaction()
              .getProduct()
              .equals(
                  costAdjustment.getCostAdjustmentLineList().get(1).getInventoryTransaction()
                      .getProduct())
              && (costAdjustmentLine
                  .getInventoryTransaction()
                  .getStorageBin()
                  .equals(
                      costAdjustment.getCostAdjustmentLineList().get(1).getInventoryTransaction()
                          .getStorageBin()) || costAdjustmentAssertLineList.size() == 3))
            assertEquals(costAdjustmentLine.getParentCostAdjustmentLine(), costAdjustment
                .getCostAdjustmentLineList().get(1));
          else if ((costAdjustmentLine
              .getInventoryTransaction()
              .getProduct()
              .equals(
                  costAdjustment.getCostAdjustmentLineList().get(2).getInventoryTransaction()
                      .getProduct()) && (costAdjustmentLine
              .getInventoryTransaction()
              .getStorageBin()
              .equals(
                  costAdjustment.getCostAdjustmentLineList().get(2).getInventoryTransaction()
                      .getStorageBin()) || costAdjustmentAssertLineList.size() == 4))
              || costAdjustmentLine.getInventoryTransaction().getProduct().isBillOfMaterials()
              && costAdjustmentLine.getAdjustmentAmount().equals(
                  costAdjustment.getCostAdjustmentLineList().get(2).getAdjustmentAmount()))
            assertEquals(costAdjustmentLine.getParentCostAdjustmentLine(), costAdjustment
                .getCostAdjustmentLineList().get(2));
          else
            assertEquals(costAdjustmentLine.getParentCostAdjustmentLine(), costAdjustment
                .getCostAdjustmentLineList().get(3));

          j++;
        }
        i++;
      }
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Assert amounts and dates of a posted document
  @SuppressWarnings("unchecked")
  private void assertDocumentPost(BaseOBObject document, String productId,
      List<DocumentPostAssert> documentPostAssertList) {
    try {

      assertEquals(document.get("posted"), "Y");

      final OBCriteria<Table> criteria1 = OBDal.getInstance().createCriteria(Table.class);
      criteria1.add(Restrictions.eq(Table.PROPERTY_NAME, document.getEntityName()));
      Table table = criteria1.list().get(0);

      final OBCriteria<AccountingFact> criteria2 = OBDal.getInstance().createCriteria(
          AccountingFact.class);
      criteria2.add(Restrictions.eq(AccountingFact.PROPERTY_RECORDID, document.getId()));
      criteria2.add(Restrictions.eq(AccountingFact.PROPERTY_TABLE, table));
      criteria2.addOrderBy(AccountingFact.PROPERTY_SEQUENCENUMBER, true);
      String groupId = criteria2.list().get(0).getGroupID();

      assertEquals(criteria2.list().size(), documentPostAssertList.size());

      int i = 0;
      for (AccountingFact accountingFact : criteria2.list()) {

        String lineListProperty = Character.toLowerCase(document.getEntityName().charAt(0))
            + document.getEntityName().substring(1) + "LineList";

        BaseOBObject line = null;
        if (document.getEntityName().equals(ReceiptInvoiceMatch.ENTITY_NAME)) {
          if (i % 2 == 0) {
            line = ((ReceiptInvoiceMatch) document).getGoodsShipmentLine();
          } else {
            line = ((ReceiptInvoiceMatch) document).getInvoiceLine();
          }
        } else if (document.getEntityName().equals(ProductionTransaction.ENTITY_NAME)) {
          StringBuffer where = new StringBuffer();
          where.append(" as t1 ");
          where.append("\n left join t1." + ProductionLine.PROPERTY_PRODUCTIONPLAN + " t2");
          where.append("\n where t2." + ProductionPlan.PROPERTY_PRODUCTION
              + " = :productionTransaction");
          where.append("\n order by t1." + ProductionLine.PROPERTY_LINENO);
          OBQuery<ProductionLine> hql = OBDal.getInstance().createQuery(ProductionLine.class,
              where.toString());
          hql.setNamedParameter("productionTransaction",
              OBDal.getInstance().get(ProductionTransaction.class, document.getId()));
          line = hql.list().get(i / 2);
        } else if (document.getEntityName().equals(CostAdjustment.ENTITY_NAME)) {
          final OBCriteria<CostAdjustmentLine> criteria3 = OBDal.getInstance().createCriteria(
              CostAdjustmentLine.class);
          criteria3.add(Restrictions.eq(CostAdjustmentLine.PROPERTY_COSTADJUSTMENT, document));
          criteria3.add(Restrictions.eq(CostAdjustmentLine.PROPERTY_NEEDSPOSTING, true));
          criteria3.addOrderBy(CostAdjustmentLine.PROPERTY_LINENO, true);
          line = criteria3.list().get(i / 2);
        } else if (productId != null
            && (productId.equals(LANDEDCOSTTYPE1_ID) || productId.equals(LANDEDCOSTTYPE2_ID) || productId
                .equals(LANDEDCOSTTYPE3_ID))) {
          line = ((List<BaseOBObject>) OBDal.getInstance()
              .get(document.getClass(), document.getId()).get(lineListProperty)).get(0);
        } else if (document.getEntityName().equals(LandedCost.ENTITY_NAME)) {
          StringBuffer where = new StringBuffer();
          where.append(" as t1 ");
          where.append("\n join t1." + LCReceiptLineAmt.PROPERTY_LANDEDCOSTRECEIPT + " t2");
          where.append("\n join t1." + LCReceiptLineAmt.PROPERTY_LANDEDCOSTCOST + " t3");
          where.append("\n join t1." + LCReceiptLineAmt.PROPERTY_GOODSSHIPMENTLINE + " t4");
          where.append("\n left join t4." + ShipmentInOutLine.PROPERTY_SHIPMENTRECEIPT + " t5");
          where.append("\n where t2." + LCReceipt.PROPERTY_LANDEDCOST + " = :landedCost");
          where.append("\n order by t3." + LandedCostCost.PROPERTY_LINENO);
          where.append("\n , t5." + ShipmentInOut.PROPERTY_DOCUMENTNO);
          where.append("\n , t4." + ShipmentInOutLine.PROPERTY_LINENO);
          OBQuery<LCReceiptLineAmt> hql = OBDal.getInstance().createQuery(LCReceiptLineAmt.class,
              where.toString());
          LandedCost landedCost = OBDal.getInstance().get(LandedCost.class, document.getId());
          hql.setNamedParameter("landedCost", landedCost);
          line = hql.list().get(i / 2);
        } else if (document.getEntityName().equals(LandedCostCost.ENTITY_NAME)) {
          if (((LandedCostCost) document).getLandedCostMatchedList().size() == 1) {
            line = ((LandedCostCost) document).getLandedCostMatchedList().get(0);
          } else if (!((LandedCostCost) document)
              .getAmount()
              .setScale(2, BigDecimal.ROUND_HALF_UP)
              .equals(
                  ((LandedCostCost) document).getMatchingAmount().setScale(2,
                      BigDecimal.ROUND_HALF_UP))
              && ((LandedCostCost) document).isMatchingAdjusted()) {
            if (i == 0) {
              line = ((LandedCostCost) document).getLandedCostMatchedList().get(0);
            } else {
              line = ((LandedCostCost) document).getLandedCostMatchedList().get(1);
            }
          } else {
            line = ((LandedCostCost) document).getLandedCostMatchedList().get(i / 2);
          }
        } else if (document.getEntityName().equals(Invoice.ENTITY_NAME) && i > 0) {
          line = ((List<BaseOBObject>) OBDal.getInstance()
              .get(document.getClass(), document.getId()).get(lineListProperty)).get(i - 1);
        } else {
          line = ((List<BaseOBObject>) OBDal.getInstance()
              .get(document.getClass(), document.getId()).get(lineListProperty)).get(i / 2);
        }
        DocumentPostAssert documentPostAssert = documentPostAssertList.get(i);
        assertGeneralData(accountingFact);

        /* Accounting window fields assert */

        assertEquals(accountingFact.getTable(), table);
        assertEquals(accountingFact.getRecordID(), document.getId());
        assertEquals(accountingFact.getAccountingSchema().getName(), "Main US/A/Euro");

        assertEquals(accountingFact.getAccount().getSearchKey(), documentPostAssert.getAccount());
        assertEquals(accountingFact.getQuantity(), documentPostAssert.getQuantity());

        BigDecimal rate;
        if ((productId != null && productId.equals(LANDEDCOSTTYPE3_ID))
            || (document.getEntityName().equals(Invoice.ENTITY_NAME) && ((Invoice) document)
                .getCurrency().getId().equals(CURRENCY2_ID))
            || (document.getEntityName().equals(LandedCost.ENTITY_NAME) && ((LCReceiptLineAmt) line)
                .getLandedCostCost()
                .getLandedCostType()
                .equals(
                    OBDal.getInstance().get(Product.class, LANDEDCOSTTYPE3_ID)
                        .getLandedCostTypeList().get(0)))
            || (document.getEntityName().equals(LandedCostCost.ENTITY_NAME)
                && ((LCMatched) line).getInvoiceLine().getProduct() != null && ((LCMatched) line)
                .getInvoiceLine().getProduct().getId().equals(LANDEDCOSTTYPE3_ID))
            || (!document.getEntityName().equals(LandedCostCost.ENTITY_NAME)
                && !document.getEntityName().equals(LandedCost.ENTITY_NAME)
                && documentPostAssert.getProductId() != null
                && !OBDal.getInstance().get(Product.class, documentPostAssert.getProductId())
                    .getPricingProductPriceList().isEmpty() && OBDal
                .getInstance()
                .get(Product.class, documentPostAssert.getProductId())
                .getPricingProductPriceList()
                .get(0)
                .getPriceListVersion()
                .equals(
                    OBDal.getInstance().get(Product.class, LANDEDCOSTTYPE3_ID)
                        .getPricingProductPriceList().get(0).getPriceListVersion()))) {

          if (document.getEntityName().equals(Invoice.ENTITY_NAME)
              && ((Invoice) document).getCurrencyConversionRateDocList().size() != 0) {
            rate = ((Invoice) document).getCurrencyConversionRateDocList().get(0).getRate();
          } else {
            Calendar calendar = Calendar.getInstance();
            calendar.set(9999, 0, 1);
            OBCriteria<ConversionRate> criteria = OBDal.getInstance().createCriteria(
                ConversionRate.class);
            criteria.add(Restrictions.eq(ConversionRate.PROPERTY_CLIENT,
                OBDal.getInstance().get(Client.class, CLIENT_ID)));
            criteria.add(Restrictions.eq(ConversionRate.PROPERTY_CURRENCY,
                OBDal.getInstance().get(Currency.class, CURRENCY2_ID)));
            criteria.add(Restrictions.eq(ConversionRate.PROPERTY_TOCURRENCY, OBDal.getInstance()
                .get(Currency.class, CURRENCY1_ID)));
            criteria.add(Restrictions.ge(ConversionRate.PROPERTY_VALIDTODATE, calendar.getTime()));
            rate = criteria.list().get(0).getMultipleRateBy();
          }
        }

        else {
          rate = BigDecimal.ONE;
        }

        assertEquals(
            accountingFact.getDebit().setScale(2, BigDecimal.ROUND_HALF_UP),
            documentPostAssert
                .getDebit()
                .multiply(rate)
                .setScale(
                    2,
                    document.getEntityName().equals(LandedCost.ENTITY_NAME) ? BigDecimal.ROUND_HALF_EVEN
                        : BigDecimal.ROUND_HALF_UP));
        assertEquals(
            accountingFact.getCredit().setScale(2, BigDecimal.ROUND_HALF_UP),
            documentPostAssert
                .getCredit()
                .multiply(rate)
                .setScale(
                    2,
                    document.getEntityName().equals(LandedCost.ENTITY_NAME) ? BigDecimal.ROUND_HALF_EVEN
                        : BigDecimal.ROUND_HALF_UP));

        if ((productId != null && productId.equals(LANDEDCOSTTYPE3_ID))
            || (document.getEntityName().equals(Invoice.ENTITY_NAME) && ((Invoice) document)
                .getCurrency().getId().equals(CURRENCY2_ID))
            || (document.getEntityName().equals(LandedCost.ENTITY_NAME) && ((LCReceiptLineAmt) line)
                .getLandedCostCost()
                .getLandedCostType()
                .equals(
                    OBDal.getInstance().get(Product.class, LANDEDCOSTTYPE3_ID)
                        .getLandedCostTypeList().get(0)))
            || (document.getEntityName().equals(LandedCostCost.ENTITY_NAME)
                && ((LCMatched) line).getInvoiceLine().getProduct() != null && ((LCMatched) line)
                .getInvoiceLine().getProduct().getId().equals(LANDEDCOSTTYPE3_ID))) {
          rate = BigDecimal.ONE;
        }

        else if ((document.getEntityName().equals(ShipmentInOut.ENTITY_NAME) || document
            .getEntityName().equals(CostAdjustment.ENTITY_NAME))
            && OBDal.getInstance().get(Organization.class, ORGANIZATION_ID).getCurrency() != null
            && OBDal.getInstance().get(Organization.class, ORGANIZATION_ID).getCurrency().getId()
                .equals(CURRENCY2_ID)) {
          Calendar calendar = Calendar.getInstance();
          calendar.set(9999, 0, 1);
          OBCriteria<ConversionRate> criteria = OBDal.getInstance().createCriteria(
              ConversionRate.class);
          criteria.add(Restrictions.eq(ConversionRate.PROPERTY_CLIENT,
              OBDal.getInstance().get(Client.class, CLIENT_ID)));
          criteria.add(Restrictions.eq(ConversionRate.PROPERTY_CURRENCY,
              OBDal.getInstance().get(Currency.class, CURRENCY1_ID)));
          criteria.add(Restrictions.eq(ConversionRate.PROPERTY_TOCURRENCY,
              OBDal.getInstance().get(Currency.class, CURRENCY2_ID)));
          criteria.add(Restrictions.ge(ConversionRate.PROPERTY_VALIDTODATE, calendar.getTime()));
          rate = criteria.list().get(0).getMultipleRateBy();
        }

        assertEquals(
            accountingFact.getForeignCurrencyDebit().setScale(2, BigDecimal.ROUND_HALF_UP),
            documentPostAssert
                .getDebit()
                .multiply(rate)
                .setScale(
                    2,
                    document.getEntityName().equals(LandedCost.ENTITY_NAME) ? BigDecimal.ROUND_HALF_EVEN
                        : BigDecimal.ROUND_HALF_UP));
        assertEquals(
            accountingFact.getForeignCurrencyCredit().setScale(2, BigDecimal.ROUND_HALF_UP),
            documentPostAssert
                .getCredit()
                .multiply(rate)
                .setScale(
                    2,
                    document.getEntityName().equals(LandedCost.ENTITY_NAME) ? BigDecimal.ROUND_HALF_EVEN
                        : BigDecimal.ROUND_HALF_UP));

        Calendar calendar1 = Calendar.getInstance();
        calendar1.setTime(accountingFact.getAccountingDate());
        calendar1.set(Calendar.DAY_OF_MONTH, calendar1.getActualMinimum(Calendar.DAY_OF_MONTH));
        Calendar calendar2 = Calendar.getInstance();
        calendar2.setTime(accountingFact.getAccountingDate());
        calendar2.set(Calendar.DAY_OF_MONTH, calendar2.getActualMaximum(Calendar.DAY_OF_MONTH));
        final OBCriteria<Period> criteria3 = OBDal.getInstance().createCriteria(Period.class);
        criteria3.add(Restrictions.eq(Period.PROPERTY_STARTINGDATE, calendar1.getTime()));
        criteria3.add(Restrictions.eq(Period.PROPERTY_ENDINGDATE, calendar2.getTime()));
        assertEquals(accountingFact.getPeriod(), criteria3.list().get(0));

        if (document.getEntityName().equals(CostAdjustment.ENTITY_NAME)) {
          assertEquals(formatDate(accountingFact.getTransactionDate()), formatDate(new Date()));
          assertEquals(formatDate(accountingFact.getAccountingDate()),
              formatDate(((CostAdjustmentLine) line).getAccountingDate()));
          if (((CostAdjustmentLine) line).getInventoryTransaction().getGoodsShipmentLine() != null) {
            assertEquals(accountingFact.getBusinessPartner(), ((CostAdjustmentLine) line)
                .getInventoryTransaction().getGoodsShipmentLine().getShipmentReceipt()
                .getBusinessPartner());
          } else {
            assertEquals(accountingFact.getBusinessPartner(), null);
          }
        } else if (document.getEntityName().equals(InventoryCount.ENTITY_NAME)) {
          assertEquals(formatDate(accountingFact.getTransactionDate()),
              formatDate(((InventoryCount) document).getMovementDate()));
          assertEquals(formatDate(accountingFact.getAccountingDate()),
              formatDate(((InventoryCount) document).getMovementDate()));
          assertEquals(accountingFact.getBusinessPartner(), null);
        } else if (document.getEntityName().equals(ReceiptInvoiceMatch.ENTITY_NAME)) {
          assertEquals(formatDate(accountingFact.getTransactionDate()),
              formatDate(((ReceiptInvoiceMatch) document).getTransactionDate()));
          assertEquals(formatDate(accountingFact.getAccountingDate()),
              formatDate(((ReceiptInvoiceMatch) document).getTransactionDate()));
          assertEquals(accountingFact.getBusinessPartner(), ((ReceiptInvoiceMatch) document)
              .getInvoiceLine().getBusinessPartner());
        } else if (document.getEntityName().equals(InternalMovement.ENTITY_NAME)) {
          assertEquals(formatDate(accountingFact.getTransactionDate()),
              formatDate(((InternalMovement) document).getMovementDate()));
          assertEquals(formatDate(accountingFact.getAccountingDate()),
              formatDate(((InternalMovement) document).getMovementDate()));
          assertEquals(accountingFact.getBusinessPartner(), null);
        } else if (document.getEntityName().equals(InternalConsumption.ENTITY_NAME)) {
          assertEquals(formatDate(accountingFact.getTransactionDate()),
              formatDate(((InternalConsumption) document).getMovementDate()));
          assertEquals(formatDate(accountingFact.getAccountingDate()),
              formatDate(((InternalConsumption) document).getMovementDate()));
          assertEquals(accountingFact.getBusinessPartner(), null);
        } else if (document.getEntityName().equals(ProductionTransaction.ENTITY_NAME)) {
          assertEquals(formatDate(accountingFact.getTransactionDate()),
              formatDate(((ProductionTransaction) document).getMovementDate()));
          assertEquals(formatDate(accountingFact.getAccountingDate()),
              formatDate(((ProductionTransaction) document).getMovementDate()));
          assertEquals(accountingFact.getBusinessPartner(), null);
        } else if (document.getEntityName().equals(LandedCost.ENTITY_NAME)) {
          assertEquals(formatDate(accountingFact.getTransactionDate()),
              formatDate(((LandedCost) document).getReferenceDate()));
          assertEquals(formatDate(accountingFact.getAccountingDate()),
              formatDate(((LandedCost) document).getReferenceDate()));
          if (i % 2 == 0) {
            assertEquals(
                accountingFact.getBusinessPartner(),
                OBDal
                    .getInstance()
                    .get(ShipmentInOutLine.class,
                        ((LCReceiptLineAmt) line).getGoodsShipmentLine().getId())
                    .getBusinessPartner());
          } else {
            assertEquals(accountingFact.getBusinessPartner(), null);
          }
        } else if (document.getEntityName().equals(LandedCostCost.ENTITY_NAME)) {
          assertEquals(formatDate(accountingFact.getTransactionDate()),
              formatDate(((LandedCostCost) document).getAccountingDate()));
          assertEquals(formatDate(accountingFact.getAccountingDate()),
              formatDate(((LandedCostCost) document).getAccountingDate()));
          if (i == 0
              || (documentPostAssert.getProductId() != null
                  && OBDal
                      .getInstance()
                      .get(
                          InvoiceLine.class,
                          ((LandedCostCost) document).getLandedCostMatchedList().get(0)
                              .getInvoiceLine().getId()).getProduct() != null && documentPostAssert
                  .getProductId().equals(
                      OBDal
                          .getInstance()
                          .get(
                              InvoiceLine.class,
                              ((LandedCostCost) document).getLandedCostMatchedList().get(0)
                                  .getInvoiceLine().getId()).getProduct().getId()))) {
            assertEquals(
                accountingFact.getBusinessPartner(),
                OBDal
                    .getInstance()
                    .get(
                        InvoiceLine.class,
                        ((LandedCostCost) document).getLandedCostMatchedList().get(0)
                            .getInvoiceLine().getId()).getBusinessPartner());
          } else {
            assertEquals(accountingFact.getBusinessPartner(), null);
          }
        } else {
          assertEquals(formatDate(accountingFact.getTransactionDate()),
              formatDate((Date) document.get("accountingDate")));
          assertEquals(formatDate(accountingFact.getAccountingDate()),
              formatDate((Date) document.get("accountingDate")));
          assertEquals(accountingFact.getBusinessPartner(), document.get("businessPartner"));
        }

        if ((productId != null && productId.equals(LANDEDCOSTTYPE3_ID))
            || (document.getEntityName().equals(Invoice.ENTITY_NAME) && ((Invoice) document)
                .getCurrency().getId().equals(CURRENCY2_ID))
            || (document.getEntityName().equals(LandedCost.ENTITY_NAME) && ((LCReceiptLineAmt) line)
                .getLandedCostCost()
                .getLandedCostType()
                .equals(
                    OBDal.getInstance().get(Product.class, LANDEDCOSTTYPE3_ID)
                        .getLandedCostTypeList().get(0)))
            || (document.getEntityName().equals(LandedCostCost.ENTITY_NAME)
                && ((LCMatched) line).getInvoiceLine().getProduct() != null && ((LCMatched) line)
                .getInvoiceLine().getProduct().getId().equals(LANDEDCOSTTYPE3_ID))
            || (!document.getEntityName().equals(Invoice.ENTITY_NAME)
                && !document.getEntityName().equals(ReceiptInvoiceMatch.ENTITY_NAME)
                && OBDal.getInstance().get(Organization.class, ORGANIZATION_ID).getCurrency() != null && OBDal
                .getInstance().get(Organization.class, ORGANIZATION_ID).getCurrency().getId()
                .equals(CURRENCY2_ID))) {
          assertEquals(accountingFact.getCurrency(),
              OBDal.getInstance().get(Currency.class, CURRENCY2_ID));
        } else {
          assertEquals(accountingFact.getCurrency(),
              OBDal.getInstance().get(Currency.class, CURRENCY1_ID));
        }

        if (productId != null && productId.equals(LANDEDCOSTTYPE2_ID)) {
          if (i == 0) {
            assertEquals(accountingFact.getProduct(), null);
            assertEquals(accountingFact.getUOM(), null);
            assertEquals(accountingFact.getTax(), null);
          } else if (i == 1) {
            assertEquals(accountingFact.getProduct(), null);
            assertEquals(accountingFact.getUOM(), null);
            assertEquals(accountingFact.getLineID(), null);
            assertEquals(accountingFact.getRecordID2(), null);

            OBCriteria<TaxRate> criteria = OBDal.getInstance().createCriteria(TaxRate.class);
            criteria.add(Restrictions.eq(TaxRate.PROPERTY_TAXCATEGORY,
                OBDal.getInstance().get(Product.class, productId).getTaxCategory()));
            criteria.add(Restrictions.eq(TaxRate.PROPERTY_ORGANIZATION,
                OBDal.getInstance().get(Organization.class, ORGANIZATION_ID)));
            assertEquals(accountingFact.getTax(), criteria.list().get(0));
          } else {
            assertEquals(accountingFact.getProduct().getId(), productId);
            assertEquals(accountingFact.getUOM(), line.get("uOM"));
            assertEquals(accountingFact.getLineID(), line.getId());
            assertEquals(accountingFact.getRecordID2(), null);
            assertEquals(accountingFact.getTax(), null);
          }
        }

        else {
          if (document.getEntityName().equals(Invoice.ENTITY_NAME) && i == 0) {
            assertEquals(accountingFact.getProduct(), null);
            assertEquals(accountingFact.getUOM(), null);
            assertEquals(accountingFact.getTax(), null);
          } else {
            if (productId == null) {
              assertEquals(
                  accountingFact.getProduct(),
                  documentPostAssert.getProductId() == null ? null : OBDal.getInstance().get(
                      Product.class, documentPostAssert.getProductId()));
            } else {
              assertEquals(accountingFact.getProduct().getId(), productId);
            }
            if (line.getEntity().getProperty("uOM", false) == null) {
              assertEquals(accountingFact.getUOM(), null);
            } else {
              assertEquals(accountingFact.getUOM(), line.get("uOM"));
            }
            if (!document.getEntityName().equals(LandedCost.ENTITY_NAME)) {
              assertEquals(accountingFact.getLineID(), line.getId());
            }
            assertEquals(accountingFact.getRecordID2(), null);
            assertEquals(accountingFact.getTax(), null);
          }
        }

        assertEquals(accountingFact.getProject(), null);
        assertEquals(accountingFact.getCostcenter(), null);
        assertEquals(accountingFact.getAsset(), null);
        assertEquals(accountingFact.getStDimension(), null);
        assertEquals(accountingFact.getNdDimension(), null);

        /* Rest of fields assert */

        if (document.getEntityName().equals(ShipmentInOut.ENTITY_NAME)) {
          assertEquals(accountingFact.getGLCategory().getName(), "Material Management");
        } else if (document.getEntityName().equals(Invoice.ENTITY_NAME)) {
          assertEquals(accountingFact.getGLCategory().getName(), "AP Invoice");
        } else if (document.getEntityName().equals(CostAdjustment.ENTITY_NAME)) {
          assertEquals(accountingFact.getGLCategory().getName(), "None");
        } else {
          assertEquals(accountingFact.getGLCategory().getName(), "Standard");
        }

        assertEquals(accountingFact.getPostingType(), "A");

        if (document.getEntityName().equals(ReceiptInvoiceMatch.ENTITY_NAME)) {
          assertEquals(accountingFact.getStorageBin(), null);
        } else if (document.getEntityName().equals(InternalMovement.ENTITY_NAME)) {
          if (i % 2 == 0) {
            assertEquals(accountingFact.getStorageBin(),
                line.get(InternalMovementLine.PROPERTY_STORAGEBIN));
          } else {
            assertEquals(accountingFact.getStorageBin(),
                line.get(InternalMovementLine.PROPERTY_NEWSTORAGEBIN));
          }
        } else if (line.getEntity().getProperty("storageBin", false) == null) {
          assertEquals(accountingFact.getStorageBin(), null);
        } else {
          assertEquals(accountingFact.getStorageBin(), line.get("storageBin"));
        }

        if (document.getEntityName().equals(InventoryCount.ENTITY_NAME)) {
          assertEquals(accountingFact.getDocumentType(), null);
          assertEquals(accountingFact.getDocumentCategory(), "MMI");
        } else if (document.getEntityName().equals(ReceiptInvoiceMatch.ENTITY_NAME)) {
          assertEquals(accountingFact.getDocumentType(), null);
          assertEquals(accountingFact.getDocumentCategory(), "MXI");
        } else if (document.getEntityName().equals(InternalMovement.ENTITY_NAME)) {
          assertEquals(accountingFact.getDocumentType(), null);
          assertEquals(accountingFact.getDocumentCategory(), "MMM");
        } else if (document.getEntityName().equals(InternalConsumption.ENTITY_NAME)) {
          assertEquals(accountingFact.getDocumentType(), null);
          assertEquals(accountingFact.getDocumentCategory(), "MIC");
        } else if (document.getEntityName().equals(ProductionTransaction.ENTITY_NAME)) {
          assertEquals(accountingFact.getDocumentType(), null);
          assertEquals(accountingFact.getDocumentCategory(), "MMP");
        } else if (document.getEntityName().equals(LandedCost.ENTITY_NAME)) {
          assertEquals(accountingFact.getDocumentType(), null);
          assertEquals(accountingFact.getDocumentCategory(), "LDC");
        } else if (document.getEntityName().equals(LandedCostCost.ENTITY_NAME)) {
          assertEquals(accountingFact.getDocumentType(), null);
          assertEquals(accountingFact.getDocumentCategory(), "LCC");
        } else {
          assertEquals(accountingFact.getDocumentType(), document.get("documentType"));
          assertEquals(accountingFact.getDocumentCategory(),
              ((DocumentType) document.get("documentType")).getDocumentCategory());
        }

        assertEquals(accountingFact.getSalesRegion(), null);
        assertEquals(accountingFact.getSalesCampaign(), null);
        assertEquals(accountingFact.getActivity(), null);
        assertEquals(accountingFact.getGroupID(), groupId);
        assertEquals(accountingFact.getType(), "N");
        assertEquals(accountingFact.getValue(), documentPostAssert.getAccount());
        assertEquals(accountingFact.getWithholding(), null);
        assertFalse(accountingFact.isModify());
        assertEquals(accountingFact.getDateBalanced(), null);

        final OBCriteria<ElementValue> criteria4 = OBDal.getInstance().createCriteria(
            ElementValue.class);
        criteria4.add(Restrictions.eq(ElementValue.PROPERTY_SEARCHKEY,
            documentPostAssert.getAccount()));
        assertEquals(accountingFact.getAccountingEntryDescription(), criteria4.list().get(0)
            .getDescription());

        i++;
      }
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  // Format date
  private String formatDate(Date date) {
    return new SimpleDateFormat("yyyy-MM-dd").format(date);
  }

  /********************************************** Data classes for test assert **********************************************/

  class LandedCostCostMatchedAssert {

    final private InvoiceLine invoiceLine;

    public LandedCostCostMatchedAssert(InvoiceLine invoiceLine) {
      this.invoiceLine = invoiceLine;
    }

    public InvoiceLine getInvoiceLine() {
      return invoiceLine;
    }

  }

  class LandedCostReceiptLineAmountAssert {

    final private LandedCostCost landedCostCost;
    final private ShipmentInOutLine receiptLine;
    final private BigDecimal amount;

    public LandedCostReceiptLineAmountAssert(LandedCostCost landedCostCost,
        ShipmentInOutLine receiptLine, BigDecimal amount) {
      this.landedCostCost = landedCostCost;
      this.receiptLine = receiptLine;
      this.amount = amount;
    }

    public LandedCostCost getLandedCostCost() {
      return landedCostCost;
    }

    public ShipmentInOutLine getReceiptLine() {
      return receiptLine;
    }

    public BigDecimal getAmount() {
      return amount;
    }

  }

  class MatchedInvoicesAssert {

    final private InvoiceLine invoiceLine;
    final private ShipmentInOutLine movementLine;

    public MatchedInvoicesAssert(InvoiceLine invoiceLine, ShipmentInOutLine movementLine) {
      this.invoiceLine = invoiceLine;
      this.movementLine = movementLine;
    }

    public InvoiceLine getInvoiceLine() {
      return invoiceLine;
    }

    public ShipmentInOutLine getMovementLine() {
      return movementLine;
    }

  }

  class PhysicalInventoryAssert {

    final private Product product;
    final private BigDecimal price;
    final private BigDecimal quantity;
    final private int day;

    public PhysicalInventoryAssert(Product product, BigDecimal price, BigDecimal quantity, int day) {
      this.product = OBDal.getInstance().get(Product.class, product.getId());
      this.price = price;
      this.quantity = quantity;
      this.day = day;
    }

    public Product getProduct() {
      return product;
    }

    public BigDecimal getPrice() {
      return price;
    }

    public BigDecimal getQuantity() {
      return quantity;
    }

    public int getDay() {
      return day;
    }

  }

  class ProductTransactionAssert {

    final private ShipmentInOutLine shipmentReceiptLine;
    final private InventoryAmountUpdateLine inventoryLine;
    final private InternalMovementLine movementLine;
    final private InternalConsumptionLine consumptionLine;
    final private ProductionLine productionLine;
    final private Currency currency;
    final private BigDecimal originalPrice;
    final private BigDecimal totalPrice;
    final private BigDecimal unitPrice;
    final private boolean priceDifference;
    final private boolean permanent;

    public ProductTransactionAssert(ShipmentInOutLine shipmentReceiptLine,
        BigDecimal originalPrice, BigDecimal finalPrice) {
      this(shipmentReceiptLine, null, null, null, null, CURRENCY1_ID, originalPrice, finalPrice,
          finalPrice, false, false);
    }

    public ProductTransactionAssert(ShipmentInOutLine shipmentReceiptLine, String currencyId,
        BigDecimal originalPrice, BigDecimal finalPrice) {
      this(shipmentReceiptLine, null, null, null, null, currencyId, originalPrice, finalPrice,
          finalPrice, false, false);
    }

    public ProductTransactionAssert(ShipmentInOutLine shipmentReceiptLine,
        BigDecimal originalPrice, BigDecimal finalPrice, boolean permanent) {
      this(shipmentReceiptLine, null, null, null, null, CURRENCY1_ID, originalPrice, finalPrice,
          finalPrice, false, permanent);
    }

    public ProductTransactionAssert(ShipmentInOutLine shipmentReceiptLine,
        BigDecimal originalPrice, BigDecimal finalPrice, boolean priceDifference, boolean permanent) {
      this(shipmentReceiptLine, null, null, null, null, CURRENCY1_ID, originalPrice, finalPrice,
          finalPrice, priceDifference, permanent);
    }

    public ProductTransactionAssert(ShipmentInOutLine shipmentReceiptLine,
        BigDecimal originalPrice, BigDecimal totalPrice, BigDecimal unitPrice) {
      this(shipmentReceiptLine, null, null, null, null, CURRENCY1_ID, originalPrice, totalPrice,
          unitPrice, false, false);
    }

    public ProductTransactionAssert(InventoryAmountUpdateLine inventoryLine,
        BigDecimal originalPrice, BigDecimal finalPrice) {
      this(null, inventoryLine, null, null, null, CURRENCY1_ID, originalPrice, finalPrice,
          finalPrice, false, false);
    }

    public ProductTransactionAssert(InventoryAmountUpdateLine inventoryLine,
        BigDecimal originalPrice, BigDecimal totalPrice, BigDecimal unitPrice) {
      this(null, inventoryLine, null, null, null, CURRENCY1_ID, originalPrice, totalPrice,
          unitPrice, false, false);
    }

    public ProductTransactionAssert(InventoryAmountUpdateLine inventoryLine,
        BigDecimal originalPrice, BigDecimal finalPrice, boolean permanent) {
      this(null, inventoryLine, null, null, null, CURRENCY1_ID, originalPrice, finalPrice,
          finalPrice, false, permanent);
    }

    public ProductTransactionAssert(InternalMovementLine movementLine, BigDecimal originalPrice,
        BigDecimal finalPrice) {
      this(null, null, movementLine, null, null, CURRENCY1_ID, originalPrice, finalPrice,
          finalPrice, false, false);
    }

    public ProductTransactionAssert(InternalMovementLine movementLine, BigDecimal originalPrice,
        BigDecimal totalPrice, BigDecimal unitPrice) {
      this(null, null, movementLine, null, null, CURRENCY1_ID, originalPrice, totalPrice,
          unitPrice, false, false);
    }

    public ProductTransactionAssert(InternalMovementLine movementLine, BigDecimal originalPrice,
        BigDecimal finalPrice, boolean permanent) {
      this(null, null, movementLine, null, null, CURRENCY1_ID, originalPrice, finalPrice,
          finalPrice, false, permanent);
    }

    public ProductTransactionAssert(InternalConsumptionLine consumptionLine,
        BigDecimal originalPrice, BigDecimal finalPrice) {
      this(null, null, null, consumptionLine, null, CURRENCY1_ID, originalPrice, finalPrice,
          finalPrice, false, false);
    }

    public ProductTransactionAssert(InternalConsumptionLine consumptionLine,
        BigDecimal originalPrice, BigDecimal finalPrice, boolean permanent) {
      this(null, null, null, consumptionLine, null, CURRENCY1_ID, originalPrice, finalPrice,
          finalPrice, false, permanent);
    }

    public ProductTransactionAssert(ProductionLine productionLine, BigDecimal originalPrice,
        BigDecimal finalPrice) {
      this(null, null, null, null, productionLine, CURRENCY1_ID, originalPrice, finalPrice,
          finalPrice, false, false);
    }

    public ProductTransactionAssert(ShipmentInOutLine shipmentReceiptLine,
        InventoryAmountUpdateLine inventoryLine, InternalMovementLine movementLine,
        InternalConsumptionLine consumptionLine, ProductionLine productionLine, String currencyId,
        BigDecimal originalPrice, BigDecimal totalPrice, BigDecimal unitPrice,
        boolean priceDifference, boolean permanent) {
      this.shipmentReceiptLine = shipmentReceiptLine;
      this.inventoryLine = inventoryLine;
      this.movementLine = movementLine;
      this.consumptionLine = consumptionLine;
      this.productionLine = productionLine;
      this.currency = OBDal.getInstance().get(Currency.class, currencyId);
      this.originalPrice = originalPrice;
      this.totalPrice = totalPrice;
      this.unitPrice = unitPrice;
      this.priceDifference = priceDifference;
      this.permanent = permanent;
    }

    public ShipmentInOutLine getShipmentReceiptLine() {
      return shipmentReceiptLine;
    }

    public InventoryAmountUpdateLine getInventoryLine() {
      return inventoryLine;
    }

    public InternalMovementLine getMovementLine() {
      return movementLine;
    }

    public InternalConsumptionLine getConsumptionLine() {
      return consumptionLine;
    }

    public ProductionLine getProductionLine() {
      return productionLine;
    }

    public Currency getCurrency() {
      return currency;
    }

    public BigDecimal getOriginalPrice() {
      return originalPrice;
    }

    public BigDecimal getTotalPrice() {
      return totalPrice;
    }

    public BigDecimal getUnitPrice() {
      return unitPrice;
    }

    public boolean isPriceDifference() {
      return priceDifference;
    }

    public boolean isPermanent() {
      return permanent;
    }

  }

  class ProductCostingAssert {

    final private MaterialTransaction transaction;
    final private Warehouse warehouse;
    final private BigDecimal price;
    final private BigDecimal originalCost;
    final private BigDecimal finalCost;
    final private BigDecimal quantity;
    final private String type;
    final private int year;
    final private boolean manual;

    public ProductCostingAssert(MaterialTransaction transaction, BigDecimal price,
        BigDecimal originalCost, BigDecimal finalCost, BigDecimal quantity) {
      this(transaction, WAREHOUSE1_ID, price, originalCost, finalCost, quantity);
    }

    public ProductCostingAssert(MaterialTransaction transaction, String warehouseId,
        BigDecimal price, BigDecimal originalCost, BigDecimal finalCost, BigDecimal quantity) {
      this(transaction, warehouseId, price, originalCost, finalCost, quantity, "AVA", 0, false);
    }

    public ProductCostingAssert(MaterialTransaction transaction, BigDecimal price,
        BigDecimal originalCost, BigDecimal finalCost, BigDecimal quantity, String type) {
      this(transaction, price, originalCost, finalCost, quantity, type, 0);
    }

    public ProductCostingAssert(MaterialTransaction transaction, BigDecimal price,
        BigDecimal originalCost, BigDecimal finalCost, BigDecimal quantity, String type, int year) {
      this(transaction, WAREHOUSE1_ID, price, originalCost, finalCost, quantity, type, year, true);
    }

    public ProductCostingAssert(MaterialTransaction transaction, String warehouseId,
        BigDecimal price, BigDecimal originalCost, BigDecimal finalCost, BigDecimal quantity,
        String type, int year, boolean manual) {
      this.transaction = transaction;
      this.warehouse = OBDal.getInstance().get(Warehouse.class, warehouseId);
      this.price = price;
      this.originalCost = originalCost;
      this.finalCost = finalCost;
      this.quantity = quantity;
      this.type = type;
      this.year = year;
      this.manual = manual;
    }

    public MaterialTransaction getTransaction() {
      return transaction;
    }

    public Warehouse getWarehouse() {
      return warehouse;
    }

    public BigDecimal getPrice() {
      return price;
    }

    public BigDecimal getOriginalCost() {
      return originalCost;
    }

    public BigDecimal getFinalCost() {
      return finalCost;
    }

    public BigDecimal getQuantity() {
      return quantity;
    }

    public String getType() {
      return type;
    }

    public int getYear() {
      return year;
    }

    public boolean isManual() {
      return manual;
    }

  }

  class CostAdjustmentAssert {

    final private MaterialTransaction materialTransaction;
    final private Currency currency;
    final private String type;
    final private BigDecimal amount;
    final private int day;
    final private boolean source;
    final private boolean unit;
    final private String status;

    public CostAdjustmentAssert(MaterialTransaction materialTransaction, String type,
        BigDecimal amount, int day, boolean source) {
      this(materialTransaction, type, amount, day, source, true);
    }

    public CostAdjustmentAssert(MaterialTransaction materialTransaction, String currencyId,
        String type, BigDecimal amount, int day, boolean source) {
      this(materialTransaction, currencyId, type, amount, day, source, true);
    }

    public CostAdjustmentAssert(MaterialTransaction materialTransaction, String type,
        BigDecimal amount, int day, boolean source, boolean unit) {
      this(materialTransaction, CURRENCY1_ID, type, amount, day, source, unit);
    }

    public CostAdjustmentAssert(MaterialTransaction materialTransaction, String currencyId,
        String type, BigDecimal amount, int day, boolean source, boolean unit) {
      this(materialTransaction, currencyId, type, amount, day, source, unit, "CO");
    }

    public CostAdjustmentAssert(MaterialTransaction materialTransaction, String type,
        BigDecimal amount, int day, boolean source, String status) {
      this(materialTransaction, type, amount, day, source, true, status);
    }

    public CostAdjustmentAssert(MaterialTransaction materialTransaction, String type,
        BigDecimal amount, int day, boolean source, boolean unit, String status) {
      this(materialTransaction, CURRENCY1_ID, type, amount, day, source, unit, status);
    }

    public CostAdjustmentAssert(MaterialTransaction materialTransaction, String currencyId,
        String type, BigDecimal amount, int day, boolean source, boolean unit, String status) {
      this.materialTransaction = materialTransaction;
      this.currency = OBDal.getInstance().get(Currency.class, currencyId);
      this.type = type;
      this.amount = amount;
      this.day = day;
      this.source = source;
      this.unit = unit;
      this.status = status;
    }

    public MaterialTransaction getMaterialTransaction() {
      return materialTransaction;
    }

    public Currency getCurrency() {
      return currency;
    }

    public String getType() {
      return type;
    }

    public BigDecimal getAmount() {
      return amount;
    }

    public int getDay() {
      return day;
    }

    public boolean isSource() {
      return source;
    }

    public boolean isUnit() {
      return unit;
    }

    public String getStatus() {
      return status;
    }

  }

  class DocumentPostAssert {

    final private String productId;
    final private String account;
    final private BigDecimal debit;
    final private BigDecimal credit;
    final private BigDecimal quantity;

    public DocumentPostAssert(String account, BigDecimal debit, BigDecimal credit,
        BigDecimal quantity) {
      this(null, account, debit, credit, quantity);
    }

    public DocumentPostAssert(String productId, String account, BigDecimal debit,
        BigDecimal credit, BigDecimal quantity) {
      this.productId = productId;
      this.account = account;
      this.debit = debit;
      this.credit = credit;
      this.quantity = quantity;
    }

    public String getProductId() {
      return productId;
    }

    public String getAccount() {
      return account;
    }

    public BigDecimal getDebit() {
      return debit;
    }

    public BigDecimal getCredit() {
      return credit;
    }

    public BigDecimal getQuantity() {
      return quantity;
    }

  }

}