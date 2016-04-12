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
package org.openbravo.client.application.window;

import java.util.List;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Hibernate;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.client.application.GCField;
import org.openbravo.client.application.GCSystem;
import org.openbravo.client.application.GCTab;
import org.openbravo.client.application.Parameter;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.ui.Element;
import org.openbravo.model.ad.ui.Field;
import org.openbravo.model.ad.ui.FieldTrl;
import org.openbravo.model.ad.ui.Tab;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility methods used in generating Openbravo view representations.
 * 
 * @author mtaal
 */
public class OBViewUtil {
  public static final Element createdElement;
  public static final Element createdByElement;
  public static final Element updatedElement;
  public static final Element updatedByElement;

  static {
    createdElement = OBDal.getInstance().get(Element.class, "245");
    createdByElement = OBDal.getInstance().get(Element.class, "246");
    updatedElement = OBDal.getInstance().get(Element.class, "607");
    updatedByElement = OBDal.getInstance().get(Element.class, "608");

    // force loading translations for these fields as they might be used for labels
    Hibernate.initialize(createdElement.getADElementTrlList());
    Hibernate.initialize(createdByElement.getADElementTrlList());
    Hibernate.initialize(updatedElement.getADElementTrlList());
    Hibernate.initialize(updatedByElement.getADElementTrlList());
  }

  private static Logger log = LoggerFactory.getLogger(OBViewUtil.class);

  /**
   * Method for retrieving the label of a field on the basis of the current language of the user.
   * 
   * @see #getLabel(BaseOBObject, List)
   */
  public static String getLabel(Field fld) {
    return getLabel(fld, fld.getADFieldTrlList());
  }

  /**
   * Returns parameter's title. Because the same Parameter Definition can be used in different
   * windows (some being purchases, some other ones sales), sync terminology is not enough to
   * determine its title. If this process is invoked from a window, it is required to check the
   * window itself to decide if it is sales or purchases. Note this only takes effect in case the
   * parameter is associated with an element and the parameter is centrally maintained.
   * 
   * @param parameter
   *          Parameter to get the title for
   * @param purchaseTrx
   *          Is the window for purchases or sales
   * @return Parameter's title
   */
  public static String getParameterTitle(Parameter parameter, boolean purchaseTrx) {
    if (purchaseTrx && parameter.getApplicationElement() != null
        && parameter.isCentralMaintenance()) {
      return getLabel(parameter.getApplicationElement(), parameter.getApplicationElement()
          .getADElementTrlList(), Element.PROPERTY_PURCHASEORDERNAME, Element.PROPERTY_NAME);
    }
    return getLabel(parameter, parameter.getOBUIAPPParameterTrlList());
  }

  /**
   * Generic method for computing the translated label/title. It assumes that the trlObjects have a
   * property called language and name and the owner object a property called name.
   * 
   * @param owner
   *          the owner of the trlObjects (for example Field)
   * @param trlObjects
   *          the trl objects (for example FieldTrl)
   * @return a translated name if found or otherwise the name of the owner
   */
  public static String getLabel(BaseOBObject owner, List<?> trlObjects) {
    return getLabel(owner, trlObjects, Field.PROPERTY_NAME);
  }

  public static String getLabel(BaseOBObject owner, List<?> trlObjects, String propertyName) {
    return getLabel(owner, trlObjects, propertyName, null);
  }

  /**
   * Generic method for computing the translated label/title. It assumes that the trlObjects have a
   * property called language and name and the owner object a property called name.
   * 
   * @param owner
   *          the owner of the trlObjects (for example Field)
   * @param trlObjects
   *          the trl objects (for example FieldTrl)
   * @param primaryPropertyName
   *          first property to look for, if secondaryProperty is null or value of this property is
   *          not null this one will be used
   * @param secondaryPropertyName
   *          second property to look for, if this is sent to null, primaryProperty will be always
   *          used. If this property is not null and value of primaryProperty is null, this one will
   *          be used
   * @return a translated name if found or otherwise the name of the owner
   */
  private static String getLabel(BaseOBObject owner, List<?> trlObjects,
      String primaryPropertyName, String secondaryPropertyName) {
    if (OBContext.hasTranslationInstalled()) {
      final String userLanguageId = OBContext.getOBContext().getLanguage().getId();
      for (Object o : trlObjects) {
        final BaseOBObject trlObject = (BaseOBObject) o;
        final String trlLanguageId = (String) DalUtil.getId(trlObject
            .get(FieldTrl.PROPERTY_LANGUAGE));
        if (trlLanguageId.equals(userLanguageId)) {
          if (secondaryPropertyName == null || trlObject.get(primaryPropertyName) != null) {
            return (String) trlObject.get(primaryPropertyName);
          }
          return (String) trlObject.get(secondaryPropertyName);
        }
      }
    }

    // trl not found, return owner
    if (secondaryPropertyName == null || owner.get(primaryPropertyName) != null) {
      return (String) owner.get(primaryPropertyName);
    }
    return (String) owner.get(secondaryPropertyName);
  }

  /**
   * Returns the grid configuration based on the field and tab information
   * 
   * @param tab
   *          tab whose grid configuration is to be obtained.
   * @return the grid configuration
   */
  public static JSONObject getGridConfigurationSettings(Tab tab) {
    return getGridConfigurationSettings(null, tab);
  }

  /**
   * Returns the grid configuration of a field
   * 
   * @param field
   *          field whose grid configuration is to be obtained
   * @return the grid configuration
   */
  public static JSONObject getGridConfigurationSettings(Field field) {
    return getGridConfigurationSettings(field, field.getTab());
  }

  /**
   * Returns the grid configuration based on the field and tab information
   * 
   * @param field
   *          field whose grid configuration is to be obtained it can be null
   * @param tab
   *          tab whose grid configuration is to be obtained. If the field is not null, this
   *          parameter will be the tab of the field
   * @return the grid configuration
   */
  private static JSONObject getGridConfigurationSettings(Field field, Tab tab) {
    GridConfigSettings settings = new GridConfigSettings();

    GCTab tabConf = null;
    for (GCTab t : tab.getOBUIAPPGCTabList()) {
      tabConf = t;
      break;
    }

    if (tabConf != null && field != null && field.getId() != null) {
      GCField fieldConf = null;
      for (GCField fc : tabConf.getOBUIAPPGCFieldList()) {
        // field list is cached in memory, so can be reused for all fields without the need of reach
        // DB again
        if (DalUtil.getId(fc.getField()).equals(DalUtil.getId(field))) {
          fieldConf = fc;
          break;
        }
      }

      // Trying to get parameters from "Grid Configuration (Tab/Field)" -> "Field" window
      if (fieldConf != null) {
        settings.processConfig(fieldConf);
      }
    }

    if (tabConf != null && settings.shouldContinueProcessing()) {
      // Trying to get parameters from "Grid Configuration (Tab/Field)" -> "Tab" window
      settings.processConfig(tabConf);
    }

    if (settings.shouldContinueProcessing()) {
      // Trying to get parameters from "Grid Configuration (System)" window
      List<GCSystem> sysConfs = OBDal.getInstance().createQuery(GCSystem.class, "").list();
      if (!sysConfs.isEmpty()) {
        settings.processConfig(sysConfs.get(0));
      }
    }

    return settings.processJSONResult();
  }

  private static class GridConfigSettings {
    private Boolean canSort = null;
    private Boolean canFilter = null;
    private Boolean filterOnChange = null;
    private Boolean lazyFiltering = null;
    private Boolean allowFkFilterByIdentifier = null;
    private Boolean showFkDropdownUnfiltered = null;
    private Boolean disableFkDropdown = null;
    private String operator = null;
    private Long thresholdToFilter = null;

    private boolean shouldContinueProcessing() {
      return canSort == null || canFilter == null || operator == null || filterOnChange == null
          || thresholdToFilter == null || allowFkFilterByIdentifier == null
          || showFkDropdownUnfiltered == null || disableFkDropdown == null || lazyFiltering == null;
    }

    private Boolean convertBoolean(BaseOBObject gcItem, String property) {
      Boolean isPropertyEnabled = true;
      Class<? extends BaseOBObject> itemClass = gcItem.getClass();
      try {
        if (gcItem instanceof GCSystem) {
          if (gcItem.get(itemClass.getField(property).get(gcItem).toString()).equals(true)) {
            isPropertyEnabled = true;
          } else if (gcItem.get(itemClass.getField(property).get(gcItem).toString()).equals(false)) {
            isPropertyEnabled = false;
          }
        } else {
          if ("Y".equals(gcItem.get(itemClass.getField(property).get(gcItem).toString()))) {
            isPropertyEnabled = true;
          } else if ("N".equals(gcItem.get(itemClass.getField(property).get(gcItem).toString()))) {
            isPropertyEnabled = false;
          } else if ("D".equals(gcItem.get(itemClass.getField(property).get(gcItem).toString()))) {
            isPropertyEnabled = null;
          }
        }
      } catch (Exception e) {
        log.error("Error while converting a value to boolean", e);
      }
      return isPropertyEnabled;
    }

    private void processConfig(BaseOBObject gcItem) {
      Class<? extends BaseOBObject> itemClass = gcItem.getClass();
      try {
        if (canSort == null) {
          canSort = convertBoolean(gcItem, "PROPERTY_SORTABLE");
        }
        if (canFilter == null) {
          canFilter = convertBoolean(gcItem, "PROPERTY_FILTERABLE");
        }
        if (operator == null) {
          if (gcItem.get(itemClass.getField("PROPERTY_TEXTFILTERBEHAVIOR").get(gcItem).toString()) != null
              && !"D".equals(gcItem.get(itemClass.getField("PROPERTY_TEXTFILTERBEHAVIOR")
                  .get(gcItem).toString()))) {
            operator = (String) gcItem.get(itemClass.getField("PROPERTY_TEXTFILTERBEHAVIOR")
                .get(gcItem).toString());
          }
        }
        if (filterOnChange == null) {
          filterOnChange = convertBoolean(gcItem, "PROPERTY_FILTERONCHANGE");
        }
        if (allowFkFilterByIdentifier == null) {
          allowFkFilterByIdentifier = convertBoolean(gcItem, "PROPERTY_ALLOWFILTERBYIDENTIFIER");
        }
        if (showFkDropdownUnfiltered == null) {
          showFkDropdownUnfiltered = convertBoolean(gcItem, "PROPERTY_ISFKDROPDOWNUNFILTERED");
        }
        if (disableFkDropdown == null) {
          disableFkDropdown = convertBoolean(gcItem, "PROPERTY_DISABLEFKCOMBO");
        }
        if (thresholdToFilter == null) {
          thresholdToFilter = (Long) gcItem.get(itemClass.getField("PROPERTY_THRESHOLDTOFILTER")
              .get(gcItem).toString());
        }
        if (lazyFiltering == null && !(gcItem instanceof GCField)) {
          lazyFiltering = convertBoolean(gcItem, "PROPERTY_ISLAZYFILTERING");
        }
      } catch (Exception e) {
        log.error("Error while getting the properties of " + gcItem, e);
      }
    }

    public JSONObject processJSONResult() {
      if (operator != null) {
        if ("IC".equals(operator)) {
          operator = "iContains";
        } else if ("IS".equals(operator)) {
          operator = "iStartsWith";
        } else if ("IE".equals(operator)) {
          operator = "iEquals";
        } else if ("C".equals(operator)) {
          operator = "contains";
        } else if ("S".equals(operator)) {
          operator = "startsWith";
        } else if ("E".equals(operator)) {
          operator = "equals";
        }
      }

      JSONObject result = new JSONObject();
      try {
        if (canSort != null) {
          result.put("canSort", canSort);
        }
        if (canFilter != null) {
          result.put("canFilter", canFilter);
        }
        if (operator != null) {
          result.put("operator", operator);
        }
        // If the tab uses lazy filtering, the fields should not filter on change
        if (Boolean.TRUE.equals(lazyFiltering)) {
          filterOnChange = false;
        }
        if (filterOnChange != null) {
          result.put("filterOnChange", filterOnChange);
        }
        if (thresholdToFilter != null) {
          result.put("thresholdToFilter", thresholdToFilter);
        }
        if (allowFkFilterByIdentifier != null) {
          result.put("allowFkFilterByIdentifier", allowFkFilterByIdentifier);
        }
        if (showFkDropdownUnfiltered != null) {
          result.put("showFkDropdownUnfiltered", showFkDropdownUnfiltered);
        }
        if (disableFkDropdown != null) {
          result.put("disableFkDropdown", disableFkDropdown);
        }
      } catch (JSONException e) {
        log.error("Couldn't get field property value", e);
      }

      return result;
    }
  }
}
