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

package org.openbravo.test.modularity;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.openbravo.modulescript.ModuleScript;
import org.openbravo.modulescript.ModuleScriptExecutionLimits;
import org.openbravo.modulescript.OpenbravoVersion;

/**
 * Test cases covering module script executions when updating database regarding the original
 * version the module is being updated from.
 * 
 * @author alostale
 * 
 */
@RunWith(Parameterized.class)
public class ModuleScriptsVersions {

  /** current module version we are updating from */
  final static Map<String, OpenbravoVersion> modulesVersionMap;
  static {
    String currentCoreVersion = "3.0.10000";

    modulesVersionMap = new HashMap<String, OpenbravoVersion>();
    modulesVersionMap.put("0", new OpenbravoVersion(currentCoreVersion));
  }

  private String moduleId;
  private String fromVersion;
  private String toVersion;
  private boolean onInstall;
  private boolean shouldExecute;

  /**
   * @param moduleId
   *          UUID of the module to establish the dependency with
   * @param fromVersion
   *          defines the first module version of the execution dependency
   * @param toVersion
   *          defines the last module version of the execution dependency
   * @param onInstall
   *          flag to indicate if the moduleScript should be executed on install
   * @param shouldExecute
   *          flag to indicate the final result, i.e., if the moduleScript should be executed or not
   * @param description
   *          description for the test case
   */
  public ModuleScriptsVersions(String moduleId, String fromVersion, String toVersion,
      boolean onInstall, boolean shouldExecute, String description) {
    this.moduleId = moduleId;
    this.fromVersion = fromVersion;
    this.toVersion = toVersion;
    this.onInstall = onInstall;
    this.shouldExecute = shouldExecute;
  }

  @Parameters(name = "{index}: ''{5}'' -- moduleId: {0} - version limits: [{1}-{2}] - on install: {3} - should execute: {4}")
  public static Collection<Object[]> data() {
    return Arrays
        .asList(new Object[][] {
            { "AAA", null, null, true, true, "New module should be executed on install" },
            { "AAA", null, null, false, false, "New module should not be executed on install" },

            // fromVersion defined without toVersion
            { "0", "3.0.9000", null, true, true,
                "Minimum version does not affect, is lower than current one, should execute" },
            { "0", "3.0.20000", null, true, false,
                "Minimum version is higher than current one, should not execute (issue does not exist yet)" },

            // toVersion defined without fromVersion
            { "0", null, "3.0.9000", true, false,
                "Updating from newer, should not execute (issue was already fixed)" },
            { "0", null, "3.0.20000", true, true,
                "Updating from older, should execute (issue was not yet fixed)" },

            // both boundaries defined
            { "0", "3.0.20000", "3.0.9000", true, false, "Incorrect definition" },
            { "0", "3.0.8000", "3.0.20000", true, true, "---" },
            { "0", "3.0.8000", "3.0.9000", true, false, "---" },
            { "0", "3.0.15000", "3.0.20000", true, false, "---" } });
  }

  /** Executes the module script with current version boundaries */
  @Test
  public void moduleScriptIsExecutedBasedOnVersionLimits() {
    FakeModuleScript ms = new FakeModuleScript();
    ms.preExecute(modulesVersionMap);
    assertThat("Script was executed", ms.wasExecuted, is(shouldExecute));
  }

  /** Fake module script with version limits, it simply flags when it is executed */
  public class FakeModuleScript extends ModuleScript {
    /** flag set when the script has been executed */
    boolean wasExecuted = false;

    @Override
    public void execute() {
      wasExecuted = true;
    }

    @Override
    public ModuleScriptExecutionLimits getModuleScriptExecutionLimits() {
      if (moduleId == null) {
        return null;
      }
      return new ModuleScriptExecutionLimits(moduleId, //
          fromVersion == null ? null : new OpenbravoVersion(fromVersion),//
          toVersion == null ? null : new OpenbravoVersion(toVersion));
    }

    @Override
    public boolean executeOnInstall() {
      return onInstall;
    }
  }
}
