/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.0  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License.
 * The Original Code is Openbravo ERP.
 * The Initial Developer of the Original Code is Openbravo SLU
 * All portions are Copyright (C) 2013-2016 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 *************************************************************************
 */
package org.openbravo.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.enterprise.event.Observes;

import org.apache.log4j.Logger;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.client.kernel.event.EntityDeleteEvent;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.client.kernel.event.TransactionBeginEvent;
import org.openbravo.client.kernel.event.TransactionCompletedEvent;
import org.openbravo.dal.core.OBContext;
import org.openbravo.materialmgmt.VariantChDescUpdateProcess;
import org.openbravo.model.common.plm.ProductCharacteristicValue;
import org.openbravo.scheduling.OBScheduler;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalConnectionProvider;

public class ProductCharacteristicValueEventHandler extends EntityPersistenceEventObserver {
  protected Logger logger = Logger.getLogger(this.getClass());
  private static Entity[] entities = { ModelProvider.getInstance().getEntity(
      ProductCharacteristicValue.ENTITY_NAME) };
  private static ThreadLocal<List<String>> prodchvalueUpdated = new ThreadLocal<List<String>>();

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  @SuppressWarnings("unused")
  public void onTransactionBegin(@Observes TransactionBeginEvent event) {
    prodchvalueUpdated.set(null);
  }

  public void onNew(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    final ProductCharacteristicValue pchv = (ProductCharacteristicValue) event.getTargetInstance();
    addProductToList(pchv);
  }

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    final ProductCharacteristicValue pchv = (ProductCharacteristicValue) event.getTargetInstance();
    addProductToList(pchv);
  }

  public void onDelete(@Observes EntityDeleteEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    final ProductCharacteristicValue pchv = (ProductCharacteristicValue) event.getTargetInstance();
    addProductToList(pchv);
  }

  public void onTransactionCompleted(@Observes TransactionCompletedEvent event) {
    List<String> productList = prodchvalueUpdated.get();
    prodchvalueUpdated.set(null);
    prodchvalueUpdated.remove();
    if (productList == null || productList.isEmpty() || event.getTransaction().wasRolledBack()) {
      return;
    }
    try {
      VariablesSecureApp vars = null;
      try {
        vars = RequestContext.get().getVariablesSecureApp();
      } catch (Exception e) {
        vars = new VariablesSecureApp(OBContext.getOBContext().getUser().getId(), OBContext
            .getOBContext().getCurrentClient().getId(), OBContext.getOBContext()
            .getCurrentOrganization().getId(), OBContext.getOBContext().getRole().getId(),
            OBContext.getOBContext().getLanguage().getLanguage());
      }

      for (String strProductId : productList) {
        ProcessBundle pb = new ProcessBundle(VariantChDescUpdateProcess.AD_PROCESS_ID, vars)
            .init(new DalConnectionProvider(false));
        HashMap<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("mProductId", strProductId);
        parameters.put("mChValueId", null);
        pb.setParams(parameters);
        OBScheduler.getInstance().schedule(pb);
      }
    } catch (Exception e) {
      logger.error("Error executing process", e);
    }
  }

  private void addProductToList(ProductCharacteristicValue pchv) {
    List<String> productList = prodchvalueUpdated.get();
    if (productList == null) {
      productList = new ArrayList<String>();
    }
    productList.add(pchv.getProduct().getId());
    prodchvalueUpdated.set(productList);
  }
}