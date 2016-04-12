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

package org.openbravo.service.datasource;

import java.util.Map;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.structure.OrganizationEnabled;
import org.openbravo.client.application.Note;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.security.SecurityChecker;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.datamodel.Table;
import org.openbravo.service.json.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A data source for the notes which manages the security. Checks if a user has access to the record
 * of the note.
 * 
 * @author NaroaIriarte
 * 
 */
public class NoteDataSource extends DefaultDataSourceService {
  private static final Logger log = LoggerFactory.getLogger(NoteDataSource.class);

  @Override
  public String fetch(Map<String, String> parameters) {
    String noteFetch = "";
    try {
      JSONObject jsonCriteria = JsonUtils.buildCriteria(parameters);
      JSONArray notesCriteria;
      String tableId;
      String recordId;
      notesCriteria = jsonCriteria.getJSONArray("criteria");
      tableId = notesCriteria.getJSONObject(0).getString("value");
      recordId = notesCriteria.getJSONObject(1).getString("value");
      readableAccesForUser(tableId, recordId);
      noteFetch = super.fetch(parameters, false);
    } catch (JSONException ex) {
      log.error("Exception while trying to perform a fetch", ex);
      throw new OBException(ex);
    }
    return noteFetch;
  }

  @Override
  public String add(Map<String, String> parameters, String content) {
    String noteAdd = "";
    try {
      JSONObject noteData;
      String tableId;
      String recordId;

      final JSONObject jsonObject = new JSONObject(content);
      noteData = jsonObject.getJSONObject("data");
      tableId = noteData.getString("table");
      recordId = noteData.getString("record");
      readableAccesForUser(tableId, recordId);
      noteAdd = super.add(parameters, content, false);
    } catch (JSONException ex) {
      log.error("Exception while trying to add a new note", ex);
      throw new OBException(ex);
    }
    return noteAdd;
  }

  @Override
  public String remove(Map<String, String> parameters) {
    String noteRemove = "";
    OBContext.setAdminMode(false);
    try {
      String noteId = parameters.get("id");
      Note note = OBDal.getInstance().get(Note.class, noteId);
      Table table = note.getTable();
      String tableId = table.getId();
      String recordId = note.getRecord();
      readableAccesForUser(tableId, recordId);
      noteRemove = super.remove(parameters, false);
    } catch (Exception ex) {
      log.error("Exception while trying to remove a note", ex);
      throw new OBException(ex);
    } finally {
      OBContext.restorePreviousMode();
    }
    return noteRemove;
  }

  /**
   * Checks if the user has readable access to the record where the note is
   */
  private void readableAccesForUser(String tableId, String recordId) {
    Entity entity = ModelProvider.getInstance().getEntityByTableId(tableId);
    if (entity != null) {
      Object object = OBDal.getInstance().get(entity.getMappingClass(), recordId);
      if (object instanceof OrganizationEnabled) {
        SecurityChecker.getInstance().checkReadableAccess((OrganizationEnabled) object);
      }
    }
  }
}