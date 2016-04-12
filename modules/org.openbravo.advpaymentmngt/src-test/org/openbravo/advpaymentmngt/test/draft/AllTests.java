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
 * All portions are Copyright (C) 2011-2014 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.advpaymentmngt.test.draft;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.openbravo.advpaymentmngt.test.draft.FinancialAccountTest;
import org.openbravo.advpaymentmngt.test.draft.PaymentMethodTest;
import org.openbravo.advpaymentmngt.test.draft.PaymentTest_01;
import org.openbravo.advpaymentmngt.test.draft.PaymentTest_02;
import org.openbravo.advpaymentmngt.test.draft.PaymentTest_03;
import org.openbravo.advpaymentmngt.test.draft.PaymentTest_04;
import org.openbravo.advpaymentmngt.test.draft.PaymentTest_05;
import org.openbravo.advpaymentmngt.test.draft.PaymentTest_06;
import org.openbravo.advpaymentmngt.test.draft.PaymentTest_07;
import org.openbravo.advpaymentmngt.test.draft.PaymentTest_08;
import org.openbravo.advpaymentmngt.test.draft.PaymentTest_09;
import org.openbravo.advpaymentmngt.test.draft.PaymentTest_10;
import org.openbravo.advpaymentmngt.test.draft.PaymentTest_11;

/**
 * 
 * Test for org.openbravo.advpaymentmngt
 * 
 */

@RunWith(Suite.class)
@Suite.SuiteClasses({
  
    // Master Data Configuration
    FinancialAccountTest.class, //
    PaymentMethodTest.class,
    
    // Payment scenarios
    PaymentTest_01.class, //
    PaymentTest_02.class, //
    PaymentTest_03.class, //
    PaymentTest_04.class, //
    PaymentTest_05.class, //
    PaymentTest_06.class, //
    PaymentTest_07.class, //
    PaymentTest_08.class, //
    PaymentTest_09.class, //
    PaymentTest_10.class, //
    PaymentTest_11.class })
public class AllTests {

}
