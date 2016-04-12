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
 * All portions are Copyright (C) 2009-2015 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.openbravo.base.weld.test.testinfrastructure.CdiInfrastructure;
import org.openbravo.base.weld.test.testinfrastructure.DalPersistanceEventTest;
import org.openbravo.base.weld.test.testinfrastructure.ParameterizedCdi;
import org.openbravo.client.application.test.ApplicationTest;
import org.openbravo.client.application.test.DynamicExpressionParserTest;
import org.openbravo.client.application.test.GenerateTypesJSTest;
import org.openbravo.client.application.test.MenuTemplateTest;
import org.openbravo.client.application.test.MenuTest;
import org.openbravo.client.kernel.freemarker.test.FreemarkerTemplateProcessorTest;
import org.openbravo.client.kernel.freemarker.test.GenerateComponentTest;
import org.openbravo.client.kernel.freemarker.test.LabelTest;
import org.openbravo.erpCommon.info.ClassicSelectorTest;
import org.openbravo.test.accounting.PostDocumentTest;
import org.openbravo.test.accounting.RecordID2Test;
import org.openbravo.test.costing.TestCosting;
import org.openbravo.test.dal.AdminContextTest;
import org.openbravo.test.dal.ComputedColumnsTest;
import org.openbravo.test.dal.DalComplexQueryRequisitionTest;
import org.openbravo.test.dal.DalComplexQueryTestOrderLine;
import org.openbravo.test.dal.DalConnectionProviderTest;
import org.openbravo.test.dal.DalFilterTest;
import org.openbravo.test.dal.DalPerformanceInventoryLineTest;
import org.openbravo.test.dal.DalPerformanceProductTest;
import org.openbravo.test.dal.DalPerformanceProxyTest;
import org.openbravo.test.dal.DalQueryTest;
import org.openbravo.test.dal.DalStoredProcedureTest;
import org.openbravo.test.dal.DalTest;
import org.openbravo.test.dal.DalUtilTest;
import org.openbravo.test.dal.DynamicEntityTest;
import org.openbravo.test.dal.HiddenUpdateTest;
import org.openbravo.test.dal.IssuesTest;
import org.openbravo.test.dal.MappingGenerationTest;
import org.openbravo.test.dal.OBContextTest;
import org.openbravo.test.dal.ReadByNameTest;
import org.openbravo.test.dal.ValidationTest;
import org.openbravo.test.dal.ViewTest;
import org.openbravo.test.db.model.functions.SqlCallableStatement;
import org.openbravo.test.expression.EvaluationTest;
import org.openbravo.test.model.ClassLoaderTest;
import org.openbravo.test.model.IndexesTest;
import org.openbravo.test.model.OneToManyTest;
import org.openbravo.test.model.RuntimeModelTest;
import org.openbravo.test.model.TrlColumnsOraTypeTest;
import org.openbravo.test.model.UniqueConstraintTest;
import org.openbravo.test.modularity.DBPrefixTest;
import org.openbravo.test.modularity.DatasetServiceTest;
import org.openbravo.test.modularity.MergePropertiesTest;
import org.openbravo.test.modularity.ModuleScriptsVersions;
import org.openbravo.test.modularity.TableNameTest;
import org.openbravo.test.preference.PreferenceTest;
import org.openbravo.test.role.inheritance.RoleInheritanceTestSuite;
import org.openbravo.test.scheduling.ProcessSchedulingTest;
import org.openbravo.test.security.AccessLevelTest;
import org.openbravo.test.security.AllowedOrganizationsTest;
import org.openbravo.test.security.EntityAccessTest;
import org.openbravo.test.security.WritableReadableOrganizationClientTest;
import org.openbravo.test.system.ErrorTextParserTest;
import org.openbravo.test.system.ImportEntrySizeTest;
import org.openbravo.test.system.Issue29934Test;
import org.openbravo.test.system.SystemServiceTest;
import org.openbravo.test.system.SystemValidatorTest;
import org.openbravo.test.system.TestInfrastructure;
import org.openbravo.test.taxes.TaxesTest;
import org.openbravo.test.views.ViewGenerationWithDifferentConfigLevelTest;
import org.openbravo.test.xml.ClientExportImportTest;
import org.openbravo.test.xml.DatasetExportTest;
import org.openbravo.test.xml.DefaultsDataset;
import org.openbravo.test.xml.EntityXMLImportTestBusinessObject;
import org.openbravo.test.xml.EntityXMLImportTestReference;
import org.openbravo.test.xml.EntityXMLImportTestSingle;
import org.openbravo.test.xml.EntityXMLImportTestWarning;
import org.openbravo.test.xml.EntityXMLIssues;
import org.openbravo.test.xml.UniqueConstraintImportTest;

/**
 * This test class is called from the ant task run.tests. It contains all the testcases which are
 * runnable and valid.
 * 
 * @author mtaal
 */

@RunWith(Suite.class)
@Suite.SuiteClasses({

// dal
    DalComplexQueryRequisitionTest.class, //
    DalComplexQueryTestOrderLine.class, //
    DalPerformanceInventoryLineTest.class, //
    DalPerformanceProductTest.class, //
    DalPerformanceProxyTest.class, //
    DalQueryTest.class, //
    DalFilterTest.class, //
    DalTest.class, //
    DalUtilTest.class, //
    IssuesTest.class, //
    DalConnectionProviderTest.class, //
    DynamicEntityTest.class, //
    HiddenUpdateTest.class, //
    MappingGenerationTest.class, //
    ValidationTest.class, //
    OBContextTest.class, //
    DalStoredProcedureTest.class, //
    ReadByNameTest.class, //
    AdminContextTest.class, //
    ViewTest.class, //
    ComputedColumnsTest.class,

    // expression
    EvaluationTest.class,

    // model
    RuntimeModelTest.class, //
    OneToManyTest.class, //
    UniqueConstraintTest.class, //
    ClassLoaderTest.class, //
    IndexesTest.class, //
    TrlColumnsOraTypeTest.class,

    // modularity
    DatasetServiceTest.class, //
    DBPrefixTest.class, //
    MergePropertiesTest.class, //
    TableNameTest.class,

    // security
    AccessLevelTest.class, //
    AllowedOrganizationsTest.class, //
    EntityAccessTest.class, //
    WritableReadableOrganizationClientTest.class,

    // system
    SystemServiceTest.class, //
    SystemValidatorTest.class, //
    ErrorTextParserTest.class, //
    TestInfrastructure.class, //
    Issue29934Test.class, //
    ImportEntrySizeTest.class, //

    // xml
    ClientExportImportTest.class, //
    EntityXMLImportTestBusinessObject.class, //
    EntityXMLImportTestReference.class, //
    EntityXMLImportTestSingle.class, //
    EntityXMLImportTestWarning.class, //
    EntityXMLIssues.class, //
    UniqueConstraintImportTest.class, //
    DatasetExportTest.class, //
    DefaultsDataset.class, //

    // preferences
    PreferenceTest.class, //
    ClassicSelectorTest.class,

    // Accounting
    RecordID2Test.class, //
    PostDocumentTest.class, //
    // Taxes
    TaxesTest.class, //

    // costing
    TestCosting.class, //

    // scheduling
    ProcessSchedulingTest.class, //

    // cdi
    CdiInfrastructure.class, //
    ParameterizedCdi.class, //
    DalPersistanceEventTest.class, //

    // client application
    ApplicationTest.class, //
    DynamicExpressionParserTest.class, //
    GenerateTypesJSTest.class, //
    MenuTest.class, //
    MenuTemplateTest.class, //

    // client kernel
    FreemarkerTemplateProcessorTest.class, //
    GenerateComponentTest.class, //
    LabelTest.class, //

    // moduleScripts
    ModuleScriptsVersions.class, //

    // role inheritance
    RoleInheritanceTestSuite.class, //

    // db
    SqlCallableStatement.class,

    // grid configuration
    ViewGenerationWithDifferentConfigLevelTest.class

})
public class AllAntTaskTests {
}
