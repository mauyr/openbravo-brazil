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
 * All portions are Copyright (C) 2010-2011 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.client.kernel;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openbravo.base.model.Entity;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.access.WindowAccess;
import org.openbravo.model.ad.datamodel.Table;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.ad.ui.Window;
import org.openbravo.model.common.enterprise.Organization;

/**
 * The component responsible for generating the dynamic part of the application js file.
 * 
 * @author mtaal
 */
public class ApplicationDynamicComponent extends BaseTemplateComponent {

  public Set<Entity> getAccessibleEntities() {
    final Set<Entity> entities = OBContext.getOBContext().getEntityAccessChecker()
        .getReadableEntities();
    entities.addAll(OBContext.getOBContext().getEntityAccessChecker().getWritableEntities());
    return removeInaccessibleEntities(entities);
  }

  // entities may contain entities not accessible by the current role, this function removes them
  // see issue 20530
  private Set<Entity> removeInaccessibleEntities(Set<Entity> entities) {
    Role role = OBContext.getOBContext().getRole();
    Role initializedRole = OBDal.getInstance().get(Role.class, role.getId());
    List<WindowAccess> windowAccessList = initializedRole.getADWindowAccessList();
    Set<Entity> accessibleEntities = new HashSet<Entity>();
    for (Entity entity : entities) {
      String tableId = entity.getTableId();
      if ("800018".equals(tableId) || "203".equals(tableId)) {
        // Special cases, may not link to its window/poWindow
        // It is safer not to try to remove them
        // See getTabId@ReferencedLink.java
        accessibleEntities.add(entity);
        continue;
      }
      Table table = OBDal.getInstance().get(Table.class, tableId);
      Window window = table.getWindow();
      Window poWindow = table.getPOWindow();
      if (windowAccessible(windowAccessList, window)
          || windowAccessible(windowAccessList, poWindow)) {
        accessibleEntities.add(entity);
      }
    }
    return accessibleEntities;
  }

  private boolean windowAccessible(List<WindowAccess> windowAccessList, Window window) {
    if (window == null) {
      return false;
    }
    for (WindowAccess wa : windowAccessList) {
      if (wa.getWindow().getId().equals(window.getId())) {
        return true;
      }
    }
    return false;
  }

  @Override
  protected Template getComponentTemplate() {
    return OBDal.getInstance().get(Template.class, KernelConstants.APPLICATION_DYNAMIC_TEMPLATE_ID);
  }

  public User getUser() {
    return OBContext.getOBContext().getUser();
  }

  public Client getClient() {
    return OBContext.getOBContext().getCurrentClient();
  }

  public Organization getOrganization() {
    return OBContext.getOBContext().getCurrentOrganization();
  }

  public Role getRole() {
    return OBContext.getOBContext().getRole();
  }

  @Override
  public String getETag() {
    OBContext c = OBContext.getOBContext();
    return super.getETag() + "_" + c.getRole().getId() + "_" + c.getUser().getId() + "_"
        + c.getCurrentClient().getId() + "_" + c.getCurrentOrganization().getId();
  }
}
