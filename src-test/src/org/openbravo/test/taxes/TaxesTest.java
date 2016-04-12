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
 * All portions are Copyright (C) 2015 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.test.taxes;

import static org.hamcrest.Matchers.closeTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;

import org.hibernate.criterion.Restrictions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.SequenceIdData;
import org.openbravo.model.ad.access.InvoiceLineTax;
import org.openbravo.model.ad.access.OrderLineTax;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.invoice.InvoiceLine;
import org.openbravo.model.common.invoice.InvoiceTax;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.order.OrderTax;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.financialmgmt.tax.TaxRate;
import org.openbravo.model.pricing.pricelist.PriceList;
import org.openbravo.test.base.OBBaseTest;
import org.openbravo.test.taxes.data.TaxesTestData;
import org.openbravo.test.taxes.data.TaxesTestData1;
import org.openbravo.test.taxes.data.TaxesTestData10;
import org.openbravo.test.taxes.data.TaxesTestData11;
import org.openbravo.test.taxes.data.TaxesTestData12;
import org.openbravo.test.taxes.data.TaxesTestData13;
import org.openbravo.test.taxes.data.TaxesTestData14;
import org.openbravo.test.taxes.data.TaxesTestData15;
import org.openbravo.test.taxes.data.TaxesTestData16;
import org.openbravo.test.taxes.data.TaxesTestData17;
import org.openbravo.test.taxes.data.TaxesTestData18;
import org.openbravo.test.taxes.data.TaxesTestData19;
import org.openbravo.test.taxes.data.TaxesTestData2;
import org.openbravo.test.taxes.data.TaxesTestData20;
import org.openbravo.test.taxes.data.TaxesTestData21;
import org.openbravo.test.taxes.data.TaxesTestData22;
import org.openbravo.test.taxes.data.TaxesTestData23;
import org.openbravo.test.taxes.data.TaxesTestData24;
import org.openbravo.test.taxes.data.TaxesTestData25;
import org.openbravo.test.taxes.data.TaxesTestData26;
import org.openbravo.test.taxes.data.TaxesTestData27;
import org.openbravo.test.taxes.data.TaxesTestData28;
import org.openbravo.test.taxes.data.TaxesTestData29;
import org.openbravo.test.taxes.data.TaxesTestData3;
import org.openbravo.test.taxes.data.TaxesTestData30;
import org.openbravo.test.taxes.data.TaxesTestData31;
import org.openbravo.test.taxes.data.TaxesTestData32;
import org.openbravo.test.taxes.data.TaxesTestData33;
import org.openbravo.test.taxes.data.TaxesTestData34;
import org.openbravo.test.taxes.data.TaxesTestData4;
import org.openbravo.test.taxes.data.TaxesTestData5;
import org.openbravo.test.taxes.data.TaxesTestData6;
import org.openbravo.test.taxes.data.TaxesTestData7;
import org.openbravo.test.taxes.data.TaxesTestData8;
import org.openbravo.test.taxes.data.TaxesTestData9;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tests cases to check taxes computation
 * 
 * 
 */
@RunWith(Parameterized.class)
public class TaxesTest extends OBBaseTest {
  final static private Logger log = LoggerFactory.getLogger(TaxesTest.class);
  // User Openbravo
  private final String USER_ID = "100";
  // Client QA Testing
  private final String CLIENT_ID = "4028E6C72959682B01295A070852010D";
  // Organization Spain
  private final String ORGANIZATION_ID = "357947E87C284935AD1D783CF6F099A1";
  // Role QA Testing Admin
  private final String ROLE_ID = "4028E6C72959682B01295A071429011E";
  // Purchase Invoice with documentNo: 10000017
  private final String PURCHASEINVOICE_ID = "9D0F6E57E59247F6AB6D063951811F51";
  // Sales Invoice with documentNo: 10000017
  private final String SALESINVOICE_ID = "F889F6E61CA6454EA50BDD6DD75582E3";
  // PriceList: Price including taxes
  private final String PRICEINCLUDINGTAXES_PRICELIST_SALES = "62C67BFD306C4BEF9F2738C27353380B";
  private final String PRICEINCLUDINGTAXES_PRICELIST_PURCHASE = "83BD2A678D30447983755C4E46C6F69A";
  // Sales order: 50012
  private final String SALESORDER_ID = "8B53B7E6CF3B4D8D9BCF3A49EED6FCB4";
  // Purchase Order: 800010
  private final String PURCHASEORDER_ID = "2C9CEDC0761A41DCB276A5124F8AAA90";

  private String testNumber;
  private String testDescription;
  private String productId;
  private BigDecimal quantity;
  private BigDecimal price;
  private BigDecimal lineNet;
  private BigDecimal quantityUpdated;
  private BigDecimal priceUpdated;
  private BigDecimal lineNetUpdated;
  private String bpartnerId;
  private String taxid;
  private HashMap<String, String[]> linetaxes;
  private HashMap<String, String[]> doctaxes;
  private boolean isSalesTest;
  private boolean isPriceIncludingTaxes;

  public TaxesTest(String testNumber, String testDescription, TaxesTestData data) {
    this.testNumber = testNumber;
    this.testDescription = testDescription;
    this.productId = data.getProductId();
    this.quantityUpdated = data.getQuantityUpdated();
    this.priceUpdated = data.getPriceUpdated();
    this.quantity = data.getQuantity();
    this.price = data.getPrice();
    this.bpartnerId = data.getBpartnerId();
    this.taxid = data.getTaxid();
    this.linetaxes = data.getLinetaxes();
    this.doctaxes = data.getDoctaxes();
    this.isSalesTest = data.isSalesTest();
    this.isPriceIncludingTaxes = data.isPriceIncludingTaxes();
    this.lineNet = data.getLineNet();
    this.lineNetUpdated = data.getLineNetUpdated();
  }

  /** parameterized possible combinations for taxes computation */
  @Parameters(name = "idx:{0} name:{1}")
  public static Collection<Object[]> params() {
    return Arrays.asList(new Object[][] {
        { "01", "Regular pricelist 01: Purchase Exempt positive", new TaxesTestData1() },
        { "02", "Regular pricelist 02: Purchase Exempt negative", new TaxesTestData2() }, //
        { "03", "Regular pricelist 03: Purchase 10% positive", new TaxesTestData3() }, //
        { "04", "Regular pricelist 04: Purchase 10% negative", new TaxesTestData4() }, //
        { "05", "Regular pricelist 05: Purchase 3% + Charge positive", new TaxesTestData5() }, //
        { "06", "Regular pricelist 06: Purchase 3% + Charge negative", new TaxesTestData6() }, //
        { "07", "Regular pricelist 07: Sales Exempt positive", new TaxesTestData7() }, //
        { "08", "Regular pricelist 08: Sales Exempt negative", new TaxesTestData8() }, //
        { "09", "Regular pricelist 09: Sales 10% Positive", new TaxesTestData9() }, //
        { "10", "Regular pricelist 10: Sales 10% negative", new TaxesTestData10() }, //
        { "11", "Regular pricelist 11: Sales 3% + charge positive", new TaxesTestData11() }, //
        { "12", "Regular pricelist 12: Sales 3% + charge negative", new TaxesTestData12() }, //
        { "13", "Regular pricelist 13: Sales BOM Taxes Positive", new TaxesTestData13() }, //
        { "14", "Regular pricelist 14: Sales BOM Taxes Negative", new TaxesTestData14() }, //
        { "15", "Regular pricelist 15: Purchase BOM Taxes Positive", new TaxesTestData15() }, //
        { "16", "Regular pricelist 16: Purchase BOM Taxes Negative", new TaxesTestData16() }, //
        { "17", "Price including taxes 17: Purchase Exempt positive", new TaxesTestData17() }, //
        { "18", "Price including taxes 18: Purchase Exempt negative", new TaxesTestData18() }, //
        { "19", "Price including taxes 19: Purchase 10% positive", new TaxesTestData19() }, //
        { "20", "Price including taxes 20: Purchase 10% negative", new TaxesTestData20() }, //
        { "21", "Price including taxes 21: Purchase 3% + charge positive", new TaxesTestData21() }, //
        { "22", "Price including taxes 22: Purchase 3% + charge negative", new TaxesTestData22() }, //
        { "23", "Price including taxes 23: Sales Exempt positive", new TaxesTestData23() }, //
        { "24", "Price including taxes 24: Sales Exempt negative", new TaxesTestData24() }, //
        { "25", "Price including taxes 25: Sales 10 % positive", new TaxesTestData25() }, //
        { "26", "Price including taxes 26: Sales 10% negative", new TaxesTestData26() }, //
        { "27", "Price including taxes 27: Sales 3% + charge positive", new TaxesTestData27() },//
        { "28", "Price including taxes 28: Sales 3% + charge negative", new TaxesTestData28() },//

        //

        { "29", "Price including taxes 29: Purchase BOM Taxes negative", new TaxesTestData29() },//
        { "30", "Price including taxes 30: Sales BOM Taxes negative", new TaxesTestData30() }, //
        { "31", "Price including taxes 31: Purchase BOM Taxes positive", new TaxesTestData31() }, //
        { "32", "Price including taxes 32: Sales BOM Taxes negative", new TaxesTestData32() },//
        { "33", "Price including taxes 33: Sales BOM+Exempt negative", new TaxesTestData33() }, //
        { "34", "Price including taxes 34: Sales BOM+Exempt positive", new TaxesTestData34() } //
        });
  }

  /**
   * Verifies taxes computation for invoices. Regular PriceList: Add a line, update it and delete
   * it. Review tax computation is correct
   */
  @Test
  public void testTax_Invoiceline_RegularPriceList() {
    assumeFalse(this.isPriceIncludingTaxes);
    // Set QA context
    OBContext.setOBContext(USER_ID, ROLE_ID, CLIENT_ID, ORGANIZATION_ID);
    try {
      Invoice invoice;
      if (isSalesTest) {
        invoice = OBDal.getInstance().get(Invoice.class, SALESINVOICE_ID);
      } else {
        invoice = OBDal.getInstance().get(Invoice.class, PURCHASEINVOICE_ID);
      }
      Invoice testInvoice = (Invoice) DalUtil.copy(invoice, false);
      testInvoice.setDocumentNo("RegularPriceList " + testNumber);
      testInvoice.setDescription(testDescription);
      testInvoice.setBusinessPartner(OBDal.getInstance().get(BusinessPartner.class, bpartnerId));
      testInvoice.setSummedLineAmount(BigDecimal.ZERO);
      testInvoice.setGrandTotalAmount(BigDecimal.ZERO);
      testInvoice.setId(SequenceIdData.getUUID());
      testInvoice.setNewOBObject(true);
      OBDal.getInstance().save(testInvoice);
      OBDal.getInstance().flush();
      log.debug("Invoice Created:" + testInvoice.getDocumentNo());
      log.debug(testDescription);
      InvoiceLine invoiceLine = invoice.getInvoiceLineList().get(0);
      InvoiceLine testInvoiceLine = (InvoiceLine) DalUtil.copy(invoiceLine, false);
      Product product = OBDal.getInstance().get(Product.class, productId);
      testInvoiceLine.setProduct(product);
      testInvoiceLine.setUOM(product.getUOM());
      testInvoiceLine.setInvoicedQuantity(quantity);
      testInvoiceLine.setUnitPrice(price);
      testInvoiceLine.setListPrice(price);
      testInvoiceLine.setStandardPrice(price);
      testInvoiceLine.setLineNetAmount(quantity.multiply(price));
      testInvoiceLine.setTax(OBDal.getInstance().get(TaxRate.class, taxid));
      testInvoiceLine.setTaxAmount(BigDecimal.ZERO);
      testInvoiceLine.setTaxableAmount(quantity.multiply(price));
      if (bpartnerId != null) {
        testInvoiceLine.setBusinessPartner(OBDal.getInstance().get(BusinessPartner.class,
            bpartnerId));
      }
      testInvoiceLine.setInvoice(testInvoice);
      testInvoice.getInvoiceLineList().add(testInvoiceLine);
      testInvoiceLine.setId(SequenceIdData.getUUID());
      testInvoiceLine.setNewOBObject(true);
      OBDal.getInstance().save(testInvoiceLine);
      OBDal.getInstance().save(testInvoice);
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(testInvoice);
      OBDal.getInstance().refresh(testInvoiceLine);
      // Now test taxes for inserted line
      BigDecimal totalTax = testTaxes(testInvoice, false);
      testLineTaxes(testInvoiceLine, false);
      // Test update of a line
      OBDal.getInstance().getSession().clear();
      testInvoice = OBDal.getInstance().get(Invoice.class, testInvoice.getId());
      // Asserts for grandTotal and TotalLines
      assertThat("Wrong Invoice GrandTotal", testInvoice.getGrandTotalAmount(),
          closeTo(quantity.multiply(price).add(totalTax), BigDecimal.ZERO));
      assertThat("Wrong Invoice TotalLines", testInvoice.getSummedLineAmount(),
          closeTo(quantity.multiply(price), BigDecimal.ZERO));
      // UpdateLine
      testInvoiceLine.setInvoicedQuantity(quantityUpdated);
      testInvoiceLine.setLineNetAmount(quantityUpdated.multiply(priceUpdated));
      testInvoiceLine.setTaxableAmount(quantityUpdated.multiply(priceUpdated));
      OBDal.getInstance().save(testInvoiceLine);
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(testInvoice);
      // Now test taxes for inserted line
      totalTax = testTaxes(testInvoice, true);
      testLineTaxes(testInvoiceLine, true);
      assertThat("Wrong Invoice GrandTotal", testInvoice.getGrandTotalAmount(),
          closeTo(quantityUpdated.multiply(priceUpdated).add(totalTax), BigDecimal.ZERO));
      assertThat("Wrong Invoice TotalLines", testInvoice.getSummedLineAmount(),
          closeTo(quantityUpdated.multiply(priceUpdated), BigDecimal.ZERO));
      // Now we can remove created data
      OBDal.getInstance().remove(testInvoiceLine);
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(testInvoice);
      if (testInvoice.getInvoiceTaxList().size() > 0) {
        assertTrue("Document Taxes not properly removed", false);
      }
      assertThat("GrandTotal holds an amount when invoice has no lines",
          testInvoice.getGrandTotalAmount(), closeTo(BigDecimal.ZERO, BigDecimal.ZERO));
      assertThat("TotalLines holds an amount when invoice has no lines",
          testInvoice.getSummedLineAmount(), closeTo(BigDecimal.ZERO, BigDecimal.ZERO));
      OBDal.getInstance().remove(testInvoice);
      OBDal.getInstance().flush();
      log.debug("Invoice Deleted:" + testInvoice.getDocumentNo());
      // log.info("Test Completed successfully");
    } catch (Exception e) {
      log.error("Error when executing testTax_Invoiceline_case1", e);
      assertFalse(true);
    }
  }

  /**
   * Verifies taxes computation for invoices. Price including taxes: Add a line, update it and
   * delete it. Review tax computation is correct
   */
  @Test
  public void testTax_Invoiceline_PriceIncludingTaxes() {
    assumeTrue(this.isPriceIncludingTaxes);
    // Set QA context
    OBContext.setOBContext(USER_ID, ROLE_ID, CLIENT_ID, ORGANIZATION_ID);
    try {
      Invoice invoice;
      if (isSalesTest) {
        invoice = OBDal.getInstance().get(Invoice.class, SALESINVOICE_ID);
      } else {
        invoice = OBDal.getInstance().get(Invoice.class, PURCHASEINVOICE_ID);
      }
      Invoice testInvoice = (Invoice) DalUtil.copy(invoice, false);
      testInvoice.setDocumentNo("PriceIncludingTaxes" + testNumber);
      testInvoice.setBusinessPartner(OBDal.getInstance().get(BusinessPartner.class, bpartnerId));
      testInvoice.setPriceIncludesTax(true);
      if (isSalesTest) {
        testInvoice.setPriceList(OBDal.getInstance().get(PriceList.class,
            PRICEINCLUDINGTAXES_PRICELIST_SALES));
      } else {
        testInvoice.setPriceList(OBDal.getInstance().get(PriceList.class,
            PRICEINCLUDINGTAXES_PRICELIST_PURCHASE));
      }
      testInvoice.setSummedLineAmount(BigDecimal.ZERO);
      testInvoice.setGrandTotalAmount(BigDecimal.ZERO);
      testInvoice.setId(SequenceIdData.getUUID());
      testInvoice.setNewOBObject(true);
      OBDal.getInstance().save(testInvoice);
      OBDal.getInstance().flush();
      log.debug("Invoice Created:" + testInvoice.getDocumentNo());
      log.debug(testDescription);
      InvoiceLine invoiceLine = invoice.getInvoiceLineList().get(0);
      InvoiceLine testInvoiceLine = (InvoiceLine) DalUtil.copy(invoiceLine, false);
      Product product = OBDal.getInstance().get(Product.class, productId);
      testInvoiceLine.setProduct(product);
      testInvoiceLine.setUOM(product.getUOM());
      testInvoiceLine.setInvoicedQuantity(quantity);
      testInvoiceLine.setUnitPrice(price);
      testInvoiceLine.setListPrice(price);
      testInvoiceLine.setGrossListPrice(price);
      testInvoiceLine.setGrossUnitPrice(price);
      testInvoiceLine.setStandardPrice(price);
      testInvoiceLine.setLineNetAmount(lineNet);
      testInvoiceLine.setGrossAmount(price.multiply(quantity));
      testInvoiceLine.setTax(OBDal.getInstance().get(TaxRate.class, taxid));
      testInvoiceLine.setTaxAmount(price.multiply(quantity).subtract(lineNet));
      testInvoiceLine.setTaxableAmount(lineNet);
      if (bpartnerId != null) {
        testInvoiceLine.setBusinessPartner(OBDal.getInstance().get(BusinessPartner.class,
            bpartnerId));
      }
      testInvoiceLine.setInvoice(testInvoice);
      testInvoice.getInvoiceLineList().add(testInvoiceLine);
      testInvoiceLine.setId(SequenceIdData.getUUID());
      testInvoiceLine.setNewOBObject(true);
      OBDal.getInstance().save(testInvoiceLine);
      OBDal.getInstance().save(testInvoice);
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(testInvoice);
      OBDal.getInstance().refresh(testInvoiceLine);
      // Now test taxes for inserted line
      testTaxes(testInvoice, false);
      testLineTaxes(testInvoiceLine, false);
      OBDal.getInstance().getSession().clear();
      testInvoice = OBDal.getInstance().get(Invoice.class, testInvoice.getId());
      assertThat("Wrong Invoice GrandTotal", testInvoice.getGrandTotalAmount(),
          closeTo(quantity.multiply(price), BigDecimal.ZERO));
      assertThat("Wrong Invoice TotalLines", testInvoice.getSummedLineAmount(),
          closeTo(lineNet, BigDecimal.ZERO));
      // Test update of a line
      testInvoiceLine = OBDal.getInstance().get(InvoiceLine.class, testInvoiceLine.getId());
      testInvoiceLine.setInvoicedQuantity(quantityUpdated);
      testInvoiceLine.setLineNetAmount(lineNetUpdated);
      testInvoiceLine.setTaxableAmount(lineNetUpdated);
      testInvoiceLine.setTaxAmount(priceUpdated.multiply(quantityUpdated).subtract(lineNetUpdated));
      testInvoiceLine.setGrossAmount(priceUpdated.multiply(quantityUpdated));
      OBDal.getInstance().save(testInvoiceLine);
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(testInvoice);
      // Now test taxes for inserted line
      testTaxes(testInvoice, true);
      testLineTaxes(testInvoiceLine, true);
      assertThat("Wrong Invoice GrandTotal", testInvoice.getGrandTotalAmount(),
          closeTo(quantityUpdated.multiply(priceUpdated), BigDecimal.ZERO));
      assertThat("Wrong Invoice TotalLines", testInvoice.getSummedLineAmount(),
          closeTo(lineNetUpdated, BigDecimal.ZERO));
      // Now we can remove created data
      OBDal.getInstance().remove(testInvoiceLine);
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(testInvoice);
      if (testInvoice.getInvoiceTaxList().size() > 0) {
        assertTrue(testDescription + ". Document Taxes not properly removed", false);
      }
      assertThat("GrandTotal holds an amount when invoice has no lines",
          testInvoice.getGrandTotalAmount(), closeTo(BigDecimal.ZERO, BigDecimal.ZERO));
      assertThat("TotalLines holds an amount when invoice has no lines",
          testInvoice.getSummedLineAmount(), closeTo(BigDecimal.ZERO, BigDecimal.ZERO));
      OBDal.getInstance().remove(testInvoice);
      OBDal.getInstance().flush();
      log.debug("Invoice Deleted:" + testInvoice.getDocumentNo());
      log.debug("Test Completed successfully");
    } catch (Exception e) {
      log.error("Error when executing: " + testDescription, e);
      assertFalse(true);
    }
  }

  /**
   * Verifies taxes computation for orders. Price including taxes: Add a line, update it and delete
   * it. Review tax computation is correct
   */
  @Test
  public void testTax_Orderline_PriceIncludingTaxes() {
    assumeTrue(this.isPriceIncludingTaxes);
    // Set QA context
    OBContext.setOBContext(USER_ID, ROLE_ID, CLIENT_ID, ORGANIZATION_ID);
    try {
      Order order;
      if (isSalesTest) {
        order = OBDal.getInstance().get(Order.class, SALESORDER_ID);
      } else {
        order = OBDal.getInstance().get(Order.class, PURCHASEORDER_ID);
      }
      Order testOrder = (Order) DalUtil.copy(order, false);
      testOrder.setDocumentNo("PriceIncludingTaxes" + testNumber);
      testOrder.setBusinessPartner(OBDal.getInstance().get(BusinessPartner.class, bpartnerId));
      testOrder.setPriceIncludesTax(true);
      if (isSalesTest) {
        testOrder.setPriceList(OBDal.getInstance().get(PriceList.class,
            PRICEINCLUDINGTAXES_PRICELIST_SALES));
      } else {
        testOrder.setPriceList(OBDal.getInstance().get(PriceList.class,
            PRICEINCLUDINGTAXES_PRICELIST_PURCHASE));
      }
      testOrder.setSummedLineAmount(BigDecimal.ZERO);
      testOrder.setGrandTotalAmount(BigDecimal.ZERO);
      testOrder.setId(SequenceIdData.getUUID());
      testOrder.setNewOBObject(true);
      OBDal.getInstance().save(testOrder);
      OBDal.getInstance().flush();
      log.debug("Order Created:" + testOrder.getDocumentNo());
      log.debug(testDescription);
      OrderLine orderLine = order.getOrderLineList().get(0);
      OrderLine testOrderLine = (OrderLine) DalUtil.copy(orderLine, false);
      Product product = OBDal.getInstance().get(Product.class, productId);
      testOrderLine.setProduct(product);
      testOrderLine.setUOM(product.getUOM());
      testOrderLine.setOrderedQuantity(quantity);
      testOrderLine.setInvoicedQuantity(BigDecimal.ZERO);
      testOrderLine.setUnitPrice(price);
      testOrderLine.setListPrice(price);
      testOrderLine.setGrossListPrice(price);
      testOrderLine.setGrossUnitPrice(price);
      testOrderLine.setStandardPrice(price);
      testOrderLine.setLineNetAmount(lineNet);
      testOrderLine.setLineGrossAmount(price.multiply(quantity));
      testOrderLine.setTax(OBDal.getInstance().get(TaxRate.class, taxid));
      // testOrderLine.setTaxAmount(price.multiply(quantity).subtract(lineNet));
      testOrderLine.setTaxableAmount(lineNet);
      if (bpartnerId != null) {
        testOrderLine
            .setBusinessPartner(OBDal.getInstance().get(BusinessPartner.class, bpartnerId));
      }
      testOrderLine.setSalesOrder(testOrder);
      testOrder.getOrderLineList().add(testOrderLine);
      testOrderLine.setId(SequenceIdData.getUUID());
      testOrderLine.setNewOBObject(true);
      OBDal.getInstance().save(testOrderLine);
      OBDal.getInstance().save(testOrder);
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(testOrder);
      OBDal.getInstance().refresh(testOrderLine);
      // Now test taxes for inserted line
      testTaxes(testOrder, false);
      testLineTaxes(testOrderLine, false);
      // Test update of a line
      OBDal.getInstance().getSession().clear();
      testOrder = OBDal.getInstance().get(Order.class, testOrder.getId());
      assertThat("Wrong Order GrandTotal", testOrder.getGrandTotalAmount(),
          closeTo(quantity.multiply(price), BigDecimal.ZERO));
      assertThat("Wrong Invoice TotalLines", testOrder.getSummedLineAmount(),
          closeTo(lineNet, BigDecimal.ZERO));
      testOrderLine = OBDal.getInstance().get(OrderLine.class, testOrderLine.getId());
      testOrderLine.setOrderedQuantity(quantityUpdated);
      testOrderLine.setLineNetAmount(lineNetUpdated);
      testOrderLine.setTaxableAmount(lineNetUpdated);
      testOrderLine.setLineGrossAmount(priceUpdated.multiply(quantityUpdated));
      OBDal.getInstance().save(testOrderLine);
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(testOrder);
      // Now test taxes for inserted line
      testTaxes(testOrder, true);
      testLineTaxes(testOrderLine, true);
      assertThat("Wrong Order GrandTotal", testOrder.getGrandTotalAmount(),
          closeTo(quantityUpdated.multiply(priceUpdated), BigDecimal.ZERO));
      assertThat("Wrong Invoice TotalLines", testOrder.getSummedLineAmount(),
          closeTo(lineNetUpdated, BigDecimal.ZERO));
      // Now we can remove created data
      OBDal.getInstance().remove(testOrderLine);
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(testOrder);
      if (testOrder.getOrderTaxList().size() > 0) {
        assertTrue(testDescription + ". Document Taxes not properly removed", false);
      }
      assertThat("GrandTotal holds an amount when order has no lines",
          testOrder.getGrandTotalAmount(), closeTo(BigDecimal.ZERO, BigDecimal.ZERO));
      assertThat("TotalLines holds an amount when order has no lines",
          testOrder.getSummedLineAmount(), closeTo(BigDecimal.ZERO, BigDecimal.ZERO));
      OBDal.getInstance().remove(testOrder);
      OBDal.getInstance().flush();
      log.debug("Order Deleted:" + testOrder.getDocumentNo());
      log.debug("Test Completed successfully");
    } catch (Exception e) {
      log.error("Error when executing: " + testDescription, e);
      assertFalse(true);
    }
  }

  /**
   * Verifies taxes computation for orders. Price including taxes: Add a line, update it and delete
   * it. Review tax computation is correct
   */
  @Test
  public void testTax_Orderline_RegularPriceList() {
    assumeFalse(this.isPriceIncludingTaxes);
    // Set QA context
    OBContext.setOBContext(USER_ID, ROLE_ID, CLIENT_ID, ORGANIZATION_ID);
    try {
      Order order;
      if (isSalesTest) {
        order = OBDal.getInstance().get(Order.class, SALESORDER_ID);
      } else {
        order = OBDal.getInstance().get(Order.class, PURCHASEORDER_ID);
      }
      Order testOrder = (Order) DalUtil.copy(order, false);
      testOrder.setDocumentNo("RegularPriceList " + testNumber);
      testOrder.setBusinessPartner(OBDal.getInstance().get(BusinessPartner.class, bpartnerId));
      testOrder.setPriceIncludesTax(false);
      testOrder.setSummedLineAmount(BigDecimal.ZERO);
      testOrder.setGrandTotalAmount(BigDecimal.ZERO);
      testOrder.setId(SequenceIdData.getUUID());
      testOrder.setNewOBObject(true);
      OBDal.getInstance().save(testOrder);
      OBDal.getInstance().flush();
      log.debug("Order Created:" + testOrder.getDocumentNo());
      log.debug(testDescription);
      OrderLine orderLine = order.getOrderLineList().get(0);
      OrderLine testOrderLine = (OrderLine) DalUtil.copy(orderLine, false);
      Product product = OBDal.getInstance().get(Product.class, productId);
      testOrderLine.setProduct(product);
      testOrderLine.setUOM(product.getUOM());
      testOrderLine.setOrderedQuantity(quantity);
      testOrderLine.setInvoicedQuantity(BigDecimal.ZERO);
      testOrderLine.setUnitPrice(price);
      testOrderLine.setListPrice(price);
      testOrderLine.setGrossListPrice(BigDecimal.ZERO);
      testOrderLine.setGrossUnitPrice(BigDecimal.ZERO);
      testOrderLine.setStandardPrice(price);
      testOrderLine.setLineNetAmount(price.multiply(quantity));
      testOrderLine.setLineGrossAmount(BigDecimal.ZERO);
      testOrderLine.setTax(OBDal.getInstance().get(TaxRate.class, taxid));
      testOrderLine.setTaxableAmount(price.multiply(quantity));
      if (bpartnerId != null) {
        testOrderLine
            .setBusinessPartner(OBDal.getInstance().get(BusinessPartner.class, bpartnerId));
      }
      testOrderLine.setSalesOrder(testOrder);
      testOrder.getOrderLineList().add(testOrderLine);
      testOrderLine.setId(SequenceIdData.getUUID());
      testOrderLine.setNewOBObject(true);
      OBDal.getInstance().save(testOrderLine);
      OBDal.getInstance().save(testOrder);
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(testOrder);
      OBDal.getInstance().refresh(testOrderLine);
      // Now test taxes for inserted line
      BigDecimal totalTax = testTaxes(testOrder, false);
      testLineTaxes(testOrderLine, false);
      // Test update of a line
      OBDal.getInstance().getSession().clear();
      testOrder = OBDal.getInstance().get(Order.class, testOrder.getId());
      // Asserts
      assertThat("Wrong Order GrandTotal", testOrder.getGrandTotalAmount(),
          closeTo(quantity.multiply(price).add(totalTax), BigDecimal.ZERO));
      assertThat("Wrong Order TotalLines", testOrder.getSummedLineAmount(),
          closeTo(price.multiply(quantity), BigDecimal.ZERO));
      testOrderLine = OBDal.getInstance().get(OrderLine.class, testOrderLine.getId());
      testOrderLine.setOrderedQuantity(quantityUpdated);
      testOrderLine.setLineNetAmount(priceUpdated.multiply(quantityUpdated));
      testOrderLine.setTaxableAmount(priceUpdated.multiply(quantityUpdated));
      testOrderLine.setLineGrossAmount(BigDecimal.ZERO);
      OBDal.getInstance().save(testOrderLine);
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(testOrder);
      // Now test taxes for inserted line
      totalTax = testTaxes(testOrder, true);
      testLineTaxes(testOrderLine, true);
      assertThat("Wrong Order GrandTotal", testOrder.getGrandTotalAmount(),
          closeTo(quantityUpdated.multiply(priceUpdated).add(totalTax), BigDecimal.ZERO));
      assertThat("Wrong Order TotalLines", testOrder.getSummedLineAmount(),
          closeTo(priceUpdated.multiply(quantityUpdated), BigDecimal.ZERO));
      // Now we can remove created data
      OBDal.getInstance().remove(testOrderLine);
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(testOrder);
      if (testOrder.getOrderTaxList().size() > 0) {
        assertTrue(testDescription + ". Document Taxes not properly removed", false);
      }
      assertThat("GrandTotal holds an amount when invoice has no lines",
          testOrder.getGrandTotalAmount(), closeTo(BigDecimal.ZERO, BigDecimal.ZERO));
      assertThat("Wrong Order TotalLines", testOrder.getSummedLineAmount(),
          closeTo(BigDecimal.ZERO, BigDecimal.ZERO));
      OBDal.getInstance().remove(testOrder);
      OBDal.getInstance().flush();
      log.debug("Order Deleted:" + testOrder.getDocumentNo());
      log.debug("Test Completed successfully");
    } catch (Exception e) {
      log.error("Error when executing: " + testDescription, e);
      assertFalse(true);
    }
  }

  private void testLineTaxes(OrderLine orderLine, boolean isUpdate) {
    int counter = 0;
    OBCriteria<OrderLineTax> obc = OBDal.getInstance().createCriteria(OrderLineTax.class);
    obc.add(Restrictions.eq(OrderLineTax.PROPERTY_SALESORDERLINE, orderLine));
    for (OrderLineTax linetax : obc.list()) {
      log.debug(linetax.getTax().getIdentifier());
      BigDecimal expectedTaxableAmount = new BigDecimal(isUpdate ? linetaxes.get(linetax.getTax()
          .getId())[2] : linetaxes.get(linetax.getTax().getId())[0]);
      BigDecimal expectedTaxAmount = new BigDecimal(isUpdate ? linetaxes.get(linetax.getTax()
          .getId())[3] : linetaxes.get(linetax.getTax().getId())[1]);
      assertThat("Wrong taxable amount for line in document", linetax.getTaxableAmount(),
          closeTo(expectedTaxableAmount, BigDecimal.ZERO));
      assertThat("Wrong tax amount for line in document", linetax.getTaxAmount(),
          closeTo(expectedTaxAmount, BigDecimal.ZERO));
      counter++;
    }
    if (linetaxes.size() != counter) {
      assertTrue(testDescription + ". Number of lines obtained(" + linetaxes.size()
          + ") different than expected (" + counter + ")", false);
    }
  }

  private void testLineTaxes(InvoiceLine invoiceLine, boolean isUpdate) {
    int counter = 0;
    OBCriteria<InvoiceLineTax> obc = OBDal.getInstance().createCriteria(InvoiceLineTax.class);
    obc.add(Restrictions.eq(InvoiceLineTax.PROPERTY_INVOICELINE, invoiceLine));
    for (InvoiceLineTax linetax : obc.list()) {
      log.debug(linetax.getTax().getIdentifier());
      log.debug(linetax.getTax().getId());
      log.debug(linetax.getTaxableAmount().toString());
      log.debug(linetax.getTaxAmount().toString());
      BigDecimal expectedTaxableAmount = new BigDecimal(isUpdate ? linetaxes.get(linetax.getTax()
          .getId())[2] : linetaxes.get(linetax.getTax().getId())[0]);
      BigDecimal expectedTaxAmount = new BigDecimal(isUpdate ? linetaxes.get(linetax.getTax()
          .getId())[3] : linetaxes.get(linetax.getTax().getId())[1]);
      assertThat("Wrong taxable amount for line in document", linetax.getTaxableAmount(),
          closeTo(expectedTaxableAmount, BigDecimal.ZERO));
      assertThat("Wrong tax amount for line in document", linetax.getTaxAmount(),
          closeTo(expectedTaxAmount, BigDecimal.ZERO));
      counter++;
    }
    if (linetaxes.size() != counter) {
      assertTrue(testDescription + ". Number of lines obtained(" + linetaxes.size()
          + ") different than expected (" + counter + ")", false);
    }
  }

  private BigDecimal testTaxes(Invoice invoice, boolean isUpdate) {
    int counter = 0;
    BigDecimal totalTax = BigDecimal.ZERO;
    OBCriteria<InvoiceTax> obc = OBDal.getInstance().createCriteria(InvoiceTax.class);
    obc.add(Restrictions.eq(InvoiceTax.PROPERTY_INVOICE, invoice));
    for (InvoiceTax tax : obc.list()) {
      log.debug(tax.getTax().getIdentifier());
      log.debug(tax.getTax().getId());
      log.debug(tax.getTaxableAmount().toString());
      log.debug(tax.getTaxAmount().toString());

      if (!doctaxes.containsKey(tax.getTax().getId())) {
        assertTrue(
            testDescription + ". Tax Should not be present: " + tax.getTax().getIdentifier(), false);
      }
      BigDecimal expectedTaxableAmount = new BigDecimal(isUpdate ? doctaxes.get(tax.getTax()
          .getId())[2] : doctaxes.get(tax.getTax().getId())[0]);
      BigDecimal expectedTaxAmount = new BigDecimal(
          isUpdate ? doctaxes.get(tax.getTax().getId())[3] : doctaxes.get(tax.getTax().getId())[1]);
      assertThat("Wrong taxable amount for document", tax.getTaxableAmount(),
          closeTo(expectedTaxableAmount, BigDecimal.ZERO));
      assertThat("Wrong tax amount for document", tax.getTaxAmount(),
          closeTo(expectedTaxAmount, BigDecimal.ZERO));
      totalTax = totalTax.add(expectedTaxAmount);
      counter++;
    }
    if (doctaxes.size() != counter) {
      assertTrue(testDescription + ". Number of lines obtained(" + doctaxes.size()
          + ") different than expected (" + counter + ")", false);
    }
    return totalTax;
  }

  private BigDecimal testTaxes(Order testOrder, boolean isUpdate) {
    int counter = 0;
    BigDecimal totalTax = BigDecimal.ZERO;
    OBCriteria<OrderTax> obc = OBDal.getInstance().createCriteria(OrderTax.class);
    obc.add(Restrictions.eq(OrderTax.PROPERTY_SALESORDER, testOrder));
    for (OrderTax tax : obc.list()) {
      log.debug(tax.getTax().getIdentifier());
      log.debug(tax.getTax().getId());
      log.debug(tax.getTaxableAmount().toString());
      log.debug(tax.getTaxAmount().toString());
      if (!doctaxes.containsKey(tax.getTax().getId())) {
        assertTrue(
            testDescription + ". Tax Should not be present: " + tax.getTax().getIdentifier(), false);
      }
      BigDecimal expectedTaxableAmount = new BigDecimal(isUpdate ? doctaxes.get(tax.getTax()
          .getId())[2] : doctaxes.get(tax.getTax().getId())[0]);
      BigDecimal expectedTaxAmount = new BigDecimal(
          isUpdate ? doctaxes.get(tax.getTax().getId())[3] : doctaxes.get(tax.getTax().getId())[1]);
      assertThat("Wrong taxable amount order", tax.getTaxableAmount(),
          closeTo(expectedTaxableAmount, BigDecimal.ZERO));
      assertThat("Wrong tax amount for order", tax.getTaxAmount(),
          closeTo(expectedTaxAmount, BigDecimal.ZERO));
      totalTax = totalTax.add(expectedTaxAmount);
      counter++;
    }
    if (doctaxes.size() != counter) {
      assertTrue(testDescription + ". Number of lines obtained(" + doctaxes.size()
          + ") different than expected (" + counter + ")", false);
    }
    return totalTax;
  }
}
