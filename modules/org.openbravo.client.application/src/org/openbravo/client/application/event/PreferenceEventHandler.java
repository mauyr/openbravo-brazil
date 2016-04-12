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

package org.openbravo.client.application.event;

import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.event.EntityDeleteEvent;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.model.ad.domain.Preference;
import org.openbravo.service.json.UnpagedRequestCachedPreference;

/**
 * Listens to delete, update and save events for the {@link Preference} entity. If it detects a
 * change in the 'OBJSON_AllowUnpagedDatasourceManualRequest' preference it invalidates its cached
 * value by setting it to null.
 * 
 * {@link "https://issues.openbravo.com/view.php?id=30204"}
 * 
 */
public class PreferenceEventHandler extends EntityPersistenceEventObserver {

  private static Entity[] entities = { ModelProvider.getInstance()
      .getEntity(Preference.ENTITY_NAME) };
  protected Logger logger = Logger.getLogger(this.getClass());
  @Inject
  private UnpagedRequestCachedPreference unpagedRequestPreference;

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    final Preference preference = (Preference) event.getTargetInstance();
    if (unpagedRequestPreference.getProperty().equals(preference.getProperty())) {
      unpagedRequestPreference.setPreferenceValue(null);
    }
  }

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    final Preference preference = (Preference) event.getTargetInstance();
    if (unpagedRequestPreference.getProperty().equals(preference.getProperty())) {
      unpagedRequestPreference.setPreferenceValue(null);
    }
  }

  public void onDelete(@Observes EntityDeleteEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    final Preference preference = (Preference) event.getTargetInstance();
    if (unpagedRequestPreference.getProperty().equals(preference.getProperty())) {
      unpagedRequestPreference.setPreferenceValue(null);
    }
  }
}
