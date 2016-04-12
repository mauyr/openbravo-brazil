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
package org.openbravo.common.actionhandler;

import java.util.List;
import java.util.Map;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.client.application.process.BaseProcessActionHandler;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.scheduling.DefaultJob;
import org.openbravo.scheduling.KillableProcess;
import org.openbravo.scheduling.OBScheduler;
import org.openbravo.service.db.DbUtility;
import org.quartz.JobExecutionContext;
import org.quartz.Scheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * Kill Process is launched from kill button in the Process Monitor Window. It will try to execute
 * the kill method in the process instance.
 * 
 */
public class KillProcess extends BaseProcessActionHandler {

  private static final Logger log = LoggerFactory.getLogger(KillProcess.class);

  @Override
  protected JSONObject doExecute(Map<String, Object> parameters, String content) {

    JSONObject result = new JSONObject();

    try {
      JSONObject request = new JSONObject(content);
      String strProcessRunId = request.getString("inpadProcessRunId");

      // Get Jobs
      Scheduler scheduler = OBScheduler.getInstance().getScheduler();
      @SuppressWarnings("unchecked")
      List<JobExecutionContext> jobs = scheduler.getCurrentlyExecutingJobs();
      if (jobs.size() == 0) {
        throw new Exception(OBMessageUtils.getI18NMessage("ProcessNotFound", null));
      }

      // Look for the job
      for (JobExecutionContext job : jobs) {
        String jobProcessRunId = (String) job.get(org.openbravo.scheduling.Process.EXECUTION_ID);
        if (jobProcessRunId.equals(strProcessRunId)) {
          // Job Found
          DefaultJob jobInstance = (DefaultJob) job.getJobInstance();
          org.openbravo.scheduling.Process process = jobInstance.getProcessInstance();
          if (process instanceof KillableProcess) {
            // Kill Process
            ((KillableProcess) process).kill(jobInstance.getBundle());
            jobInstance.setKilled(true);
            return buildResult("info", "Info", "ProcessKilled");
          } else {
            // KillableProcess not implemented
            return buildResult("warning", "Info", "KillableProcessNotImplemented");
          }

        }
      }

      throw new Exception(OBMessageUtils.getI18NMessage("ProcessNotFound", null));

    } catch (Exception ex) {
      Throwable e = DbUtility.getUnderlyingSQLException(ex);
      log.error("Error in Kill Process", e);
      try {
        result = buildResult("error", "Error", e.getMessage());
      } catch (Exception ignoreException) {
        // do nothing
      }
    }

    return result;

  }

  private JSONObject buildResult(String type, String title, String messagetext) throws Exception {
    JSONObject result = new JSONObject();
    JSONObject message = new JSONObject();
    JSONObject msgTotalAction = new JSONObject();
    JSONArray actions = new JSONArray();

    message.put("msgType", type);
    message.put("msgTitle", OBMessageUtils.getI18NMessage(title, null));
    String msgText = OBMessageUtils.getI18NMessage(messagetext, null);
    message.put("msgText", msgText == null ? messagetext : msgText);

    msgTotalAction.put("showMsgInProcessView", message);
    actions.put(msgTotalAction);
    result.put("responseActions", actions);

    return result;
  }
}
