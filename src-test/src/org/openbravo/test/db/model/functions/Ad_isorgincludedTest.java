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

package org.openbravo.test.db.model.functions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.openbravo.service.db.CallStoredProcedure;
import org.openbravo.test.base.OBBaseTest;

public class Ad_isorgincludedTest extends OBBaseTest {

  /**
   * All Organization *
   */
  private static final String ORG_O = "0";

  /**
   * QA Testing Client
   */
  private static final String CLIENT_A = "4028E6C72959682B01295A070852010D";

  /**
   * Main Organization
   */
  private static final String ORG_A = "43D590B4814049C6B85C6545E8264E37";

  /**
   * Spain Organization
   */
  private static final String ORG_A1 = "357947E87C284935AD1D783CF6F099A1";

  /**
   * USA Organization
   */
  private static final String ORG_A2 = "5EFF95EB540740A3B10510D9814EFAD5";

  /**
   * F&B International Group Client
   */
  private static final String CLIENT_B = "23C59575B9CF467C9620760EB255B389";

  /**
   * F&B International Group Organization
   */
  private static final String ORG_B = "19404EAD144C49A0AF37D54377CF452D";

  /**
   * F&B US, Inc.
   */
  private static final String ORG_B1 = "2E60544D37534C0B89E765FE29BC0B43";

  /**
   * F&B US East Coast
   */
  private static final String ORG_B11 = "7BABA5FF80494CAFA54DEBD22EC46F01";

  /**
   * F&B US West Coast
   */
  private static final String ORG_B12 = "BAE22373FEBE4CCCA24517E23F0C8A48";

  /**
   * F&B España, S.A.
   */
  private static final String ORG_B2 = "B843C30461EA4501935CB1D125C9C25A";

  /**
   * F&B España - Región Norte
   */
  private static final String ORG_B21 = "E443A31992CB4635AFCAEABE7183CE85";

  /**
   * F&B España - Región Sur
   */
  private static final String ORG_B22 = "DC206C91AA6A4897B44DA897936E0EC3";

  /**
   * Case I: Distinct Organization in the same branch with different levels.
   */

  /**
   * Case II: Distinct Organization in the different branch with different levels.
   */

  /**
   * Case III: Swap parent/child order
   */

  /**
   * Case IV: Organization with different clients
   */

  /**
   * Case V: Same Organization
   */

  /**
   * Case VI: Organization that does not exists.
   */

  @Test
  public void testIsOrgIncluded() {

    // Case I
    assertEquals("Level 1 Organization", 1, isOrgIncluded(ORG_O, ORG_O, CLIENT_B));
    assertEquals("Level 2 Organization", 2, isOrgIncluded(ORG_B, ORG_O, CLIENT_B));
    assertEquals("Level 3 Organization", 3, isOrgIncluded(ORG_B1, ORG_O, CLIENT_B));
    assertEquals("Level 4 Organization", 4, isOrgIncluded(ORG_B12, ORG_O, CLIENT_B));

    // Case II
    assertTrue(isOrgIncluded(ORG_B12, ORG_B2, CLIENT_A) == -1);
    assertTrue(isOrgIncluded(ORG_B11, ORG_B2, CLIENT_A) == -1);
    assertTrue(isOrgIncluded(ORG_B21, ORG_B1, CLIENT_A) == -1);
    assertTrue(isOrgIncluded(ORG_B22, ORG_B1, CLIENT_A) == isOrgIncluded(ORG_B11, ORG_B2, CLIENT_A));

    // Case III
    assertTrue(isOrgIncluded(ORG_A, ORG_A1, CLIENT_A) == -1);
    assertTrue(isOrgIncluded(ORG_B1, ORG_B12, CLIENT_A) == -1);

    // Case IV
    assertTrue(isOrgIncluded(ORG_A1, ORG_A, CLIENT_B) == -1);
    assertTrue(isOrgIncluded(ORG_B1, ORG_B, CLIENT_A) == -1);

    // Case V
    assertTrue(isOrgIncluded(ORG_A, ORG_A, CLIENT_A) == 1);

    // Case VI
    assertTrue(isOrgIncluded("ABC", ORG_B, CLIENT_A) == -1);

  }

  private int isOrgIncluded(String orgId, String parentOrgId, String clientId) {
    int value = 0;
    try {
      final List<Object> parameters = new ArrayList<Object>();
      parameters.add(orgId);
      parameters.add(parentOrgId);
      parameters.add(clientId);
      value = ((BigDecimal) CallStoredProcedure.getInstance().call("AD_ISORGINCLUDED", parameters,
          null)).intValue();
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
    return value;
  }
}
