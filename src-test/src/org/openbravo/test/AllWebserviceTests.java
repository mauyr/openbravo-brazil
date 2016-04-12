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
 * All portions are Copyright (C) 2010-2015 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.openbravo.test.datasource.ExtendedNavigationModelTest;
import org.openbravo.test.datasource.FICTest;
import org.openbravo.test.datasource.FKDropDownDatasource;
import org.openbravo.test.datasource.HQLDataSourceTest;
import org.openbravo.test.datasource.ProductSelectorDataSourceTest;
import org.openbravo.test.datasource.SelectorFieldPropertySelectorDSTest;
import org.openbravo.test.datasource.TestAllowUnpagedDatasourcePreference;
import org.openbravo.test.datasource.TestCSVEncoding;
import org.openbravo.test.datasource.TestComboDatasource;
import org.openbravo.test.datasource.TestNoteDatasource;
import org.openbravo.test.webservice.JSONWebServices;
import org.openbravo.test.webservice.PerformanceTest;
import org.openbravo.test.webservice.WSAddRecordWithComputedColumns;
import org.openbravo.test.webservice.WSReadTest;
import org.openbravo.test.webservice.WSUpdateTest;
import org.openbravo.test.webservice.WebServicesWithNoActiveFilterTest;

/**
 * This test suite should only contain test cases which are to run the webservices included in core.
 * 
 * 
 */

@RunWith(Suite.class)
@Suite.SuiteClasses({

WSReadTest.class, //
    WSUpdateTest.class, //
    PerformanceTest.class, //
    WSAddRecordWithComputedColumns.class, //
    TestCSVEncoding.class, //
    SelectorFieldPropertySelectorDSTest.class, //
    ProductSelectorDataSourceTest.class, //
    TestComboDatasource.class, //
    FKDropDownDatasource.class, //
    JSONWebServices.class, //
    FICTest.class, //
    HQLDataSourceTest.class, //
    TestAllowUnpagedDatasourcePreference.class, //
    TestNoteDatasource.class, //
    WebServicesWithNoActiveFilterTest.class, //
    ExtendedNavigationModelTest.class })
public class AllWebserviceTests {
}
