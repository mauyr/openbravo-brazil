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
// = On Change Functions =
//
// Contains on change functions that are defined in the fields.
OB = window.OB || {};
OB.OnChange = window.OB.OnChange || {};


//** {{{OB.OnChange.organizationCurrency}}} **
// Used in the 'Organization' window, in the 'Currency' onchange field
// It shows a warning dialog when currency is changed
OB.OnChange.organizationCurrency = function (item, view, form, grid) {
  if (view && view.messageBar) {
    view.messageBar.setMessage('warning', null, OB.I18N.getLabel('OBUIAPP_OrgCurrencyChange'));
  }
};

//** {{{OB.OnChange.processDefinitionUIPattern}}} **
//Used in the 'Process Definition' window, in the 'UI Pattern' field
//When OBUIAPP_Report is selected and the Action Handler is empty it sets
//the BaseReportActionHandler as default value.
OB.OnChange.processDefinitionUIPattern = function (item, view, form, grid) {
  var classNameItem = form.getItem('javaClassName');
  if (item.getValue() === 'OBUIAPP_Report' && !classNameItem.getValue()) {
    classNameItem.setValue('org.openbravo.client.application.report.BaseReportActionHandler');
  }
};