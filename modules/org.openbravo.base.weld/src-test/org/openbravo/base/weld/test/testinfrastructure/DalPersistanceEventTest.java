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

package org.openbravo.base.weld.test.testinfrastructure;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.geography.Country;

/**
 * Persistance observers require of cdi. Test cases covering observers are executed when using
 * WeldBaseTest.
 * 
 * @author alostale
 *
 */
public class DalPersistanceEventTest extends WeldBaseTest {
  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Test
  public void persisntaceObserversShouldBeExecuted() {
    try {
      setSystemAdministratorContext();
      Country newCountry = OBProvider.getInstance().get(Country.class);
      newCountry.setName("Wonderland");
      newCountry.setISOCountryCode("WL");
      newCountry.setAddressPrintFormat("-");

      newCountry.setDateformat("invalid date format");

      // expecting exception thrown by by persistance observer, it will be thrown only if it is
      // executed
      exception.expect(OBException.class);
      exception.expectMessage(OBMessageUtils.messageBD("InvalidDateFormat"));

      OBDal.getInstance().save(newCountry);
      OBDal.getInstance().flush();
    } finally {
      OBDal.getInstance().rollbackAndClose();
    }
  }
}
