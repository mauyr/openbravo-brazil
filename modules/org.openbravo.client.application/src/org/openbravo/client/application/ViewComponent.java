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
 * All portions are Copyright (C) 2010-2015 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.client.application;

import java.util.Date;
import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import org.apache.commons.codec.digest.DigestUtils;
import org.hibernate.Query;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.util.OBClassLoader;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.client.application.window.ParameterWindowComponent;
import org.openbravo.client.application.window.StandardWindowComponent;
import org.openbravo.client.kernel.BaseComponent;
import org.openbravo.client.kernel.BaseTemplateComponent;
import org.openbravo.client.kernel.KernelConstants;
import org.openbravo.client.kernel.OBUserException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.obps.ActivationKey;
import org.openbravo.erpCommon.obps.ActivationKey.FeatureRestriction;
import org.openbravo.model.ad.module.Module;
import org.openbravo.model.ad.ui.Tab;
import org.openbravo.model.ad.ui.Window;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads the view and generates it.
 * 
 * @author mtaal
 */
@RequestScoped
public class ViewComponent extends BaseComponent {
  private static Logger log = LoggerFactory.getLogger(ViewComponent.class);

  @Inject
  private StandardWindowComponent standardWindowComponent;

  @Inject
  private ParameterWindowComponent parameterWindowComponent;

  @Inject
  private WeldUtils weldUtils;

  @Override
  public String generate() {
    long t = System.currentTimeMillis();
    final String viewId = getParameter("viewId");
    if (viewId == null) {
      throw new IllegalArgumentException("viewId parameter not found, it is mandatory");
    }

    try {
      OBContext.setAdminMode();

      final Window window = OBDal.getInstance().get(Window.class, correctViewId(viewId));

      if (window != null) {
        FeatureRestriction featureRestriction = ActivationKey.getInstance().hasLicenseAccess("MW",
            window.getId());
        if (featureRestriction != FeatureRestriction.NO_RESTRICTION) {
          throw new OBUserException(featureRestriction.toString());
        }
        return generateWindow(window);
      } else if (viewId.startsWith("processDefinition_")) {
        String processId = viewId.substring("processDefinition_".length());
        Process process = OBDal.getInstance().get(Process.class, processId);
        if (process == null) {
          throw new IllegalArgumentException("Not found process definition with ID " + processId);
        }
        return generateProcess(process);
      } else {
        return generateView(viewId);
      }
    } finally {
      OBContext.restorePreviousMode();
      log.debug("View {} generated in {} ms", viewId, System.currentTimeMillis() - t);
    }
  }

  protected String generateWindow(Window window) {
    standardWindowComponent.setWindow(window);
    standardWindowComponent.setParameters(getParameters());
    final String jsCode = standardWindowComponent.generate();
    return jsCode;
  }

  protected String generateView(String viewName) {
    OBUIAPPViewImplementation viewImpDef = getView(viewName);

    final BaseTemplateComponent component;
    if (viewImpDef.getJavaClassName() != null) {
      try {
        @SuppressWarnings("unchecked")
        final Class<BaseTemplateComponent> clz = (Class<BaseTemplateComponent>) OBClassLoader
            .getInstance().loadClass(viewImpDef.getJavaClassName());
        component = weldUtils.getInstance(clz);
      } catch (Exception e) {
        throw new OBException(e);
      }
    } else {
      component = weldUtils.getInstance(BaseTemplateComponent.class);
      if (viewImpDef.getTemplate() == null) {
        throw new IllegalStateException("No class and no template defined for view " + viewName);
      }
    }
    component.setId(viewImpDef.getId());
    component.setComponentTemplate(viewImpDef.getTemplate());
    component.setParameters(getParameters());

    final String jsCode = component.generate();
    return jsCode;
  }

  protected String generateProcess(Process process) {
    parameterWindowComponent.setProcess(process);
    parameterWindowComponent.setParameters(getParameters());
    parameterWindowComponent.setPoup(false);
    return parameterWindowComponent.generate();
  }

  private OBUIAPPViewImplementation getView(String viewName) {
    OBCriteria<OBUIAPPViewImplementation> obc = OBDal.getInstance().createCriteria(
        OBUIAPPViewImplementation.class);
    obc.add(Restrictions.or(Restrictions.eq(OBUIAPPViewImplementation.PROPERTY_NAME, viewName),
        Restrictions.eq(OBUIAPPViewImplementation.PROPERTY_ID, viewName)));

    if (obc.list().size() > 0) {
      return obc.list().get(0);
    } else {
      throw new IllegalArgumentException("No view found using id/name " + viewName);
    }
  }

  @Override
  public Module getModule() {
    final String id = getParameter("viewId");
    final Window window = OBDal.getInstance().get(Window.class, correctViewId(id));
    if (window != null) {
      return window.getModule();
    } else if (id.startsWith("processDefinition_")) {
      String processId = id.substring("processDefinition_".length());
      Process process = OBDal.getInstance().get(Process.class, processId);
      if (process == null) {
        throw new IllegalArgumentException("Not found process definition with ID " + processId);
      }
      return process.getModule();
    } else {
      OBUIAPPViewImplementation view = getView(id);
      if (view != null) {
        return view.getModule();
      } else {
        return super.getModule();
      }
    }
  }

  protected String correctViewId(String viewId) {
    // the case if a window is in development and has a unique making postfix
    // see the StandardWindowComponent.getWindowClientClassName method
    // changes made here should also be done there
    String correctedViewId = (viewId.startsWith(KernelConstants.ID_PREFIX) ? viewId.substring(1)
        : viewId);
    // if in consultants mode, do another conversion
    if (correctedViewId.contains(KernelConstants.ID_PREFIX)) {
      final int index = correctedViewId.indexOf(KernelConstants.ID_PREFIX);
      correctedViewId = correctedViewId.substring(0, index);
    }
    return correctedViewId;
  }

  @Override
  public Object getData() {
    return this;
  }

  @Override
  public String getETag() {
    String etag = super.getETag();

    return etag + "_" + getViewVersionHash();
  }

  /**
   * This function returns the last grid configuration change made into a window at any level (at
   * whole system level or just a for particuar tab or field).
   * 
   * This value is needed for the eTag calculation, so, if there has been any grid configuration
   * change, the eTag should change in order to load again the view definition.
   * 
   * @param window
   *          the window to obtain its last grid configuration change
   * @return a String with the last grid configuration change
   */
  private String getLastGridConfigurationChange(Window window) {
    Date lastModification = new Date(0);

    List<GCSystem> sysConfs = OBDal.getInstance().createQuery(GCSystem.class, "").list();
    if (!sysConfs.isEmpty()) {
      if (lastModification.compareTo(sysConfs.get(0).getUpdated()) < 0) {
        lastModification = sysConfs.get(0).getUpdated();
      }
    }

    String tabHql = "select max(updated) from OBUIAPP_GC_Tab where tab.window.id = :windowId";
    Query qryTabData = OBDal.getInstance().getSession().createQuery(tabHql);
    qryTabData.setParameter("windowId", window.getId());
    Date tabUpdated = (Date) qryTabData.uniqueResult();
    if (tabUpdated != null && lastModification.compareTo(tabUpdated) < 0) {
      lastModification = tabUpdated;
    }

    String fieldHql = "select max(updated) from OBUIAPP_GC_Field where obuiappGcTab.tab.window.id = :windowId";
    Query qryFieldData = OBDal.getInstance().getSession().createQuery(fieldHql);
    qryFieldData.setParameter("windowId", window.getId());
    Date fieldUpdated = (Date) qryFieldData.uniqueResult();
    if (fieldUpdated != null && lastModification.compareTo(fieldUpdated) < 0) {
      lastModification = fieldUpdated;
    }

    return lastModification.toString();
  }

  private synchronized String getViewVersionHash() {
    String viewVersionHash = "";
    String viewVersions = "";
    final String viewId = getParameter("viewId");
    OBContext.setAdminMode();
    try {
      Window window = OBDal.getInstance().get(Window.class, correctViewId(viewId));
      if (window == null) {
        return viewVersionHash;
      }
      for (Tab t : window.getADTabList()) {
        viewVersions += t.getTable().isFullyAudited() + "|";
      }
      viewVersions += getLastGridConfigurationChange(window) + "|";
      viewVersionHash = DigestUtils.md5Hex(viewVersions);
    } finally {
      OBContext.restorePreviousMode();
    }
    return viewVersionHash;
  }
}
