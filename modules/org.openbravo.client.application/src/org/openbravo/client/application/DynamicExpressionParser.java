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
 * All portions are Copyright (C) 2011-2014 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.client.application;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openbravo.base.model.domaintype.DateDomainType;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.client.application.window.ApplicationDictionaryCachedStructures;
import org.openbravo.client.kernel.KernelUtils;
import org.openbravo.client.kernel.reference.UIDefinition;
import org.openbravo.client.kernel.reference.UIDefinitionController;
import org.openbravo.client.kernel.reference.YesNoUIDefinition;
import org.openbravo.data.Sqlc;
import org.openbravo.erpCommon.utility.DimensionDisplayUtility;
import org.openbravo.model.ad.ui.AuxiliaryInput;
import org.openbravo.model.ad.ui.Field;
import org.openbravo.model.ad.ui.Tab;

/**
 * Parses a dynamic expressions and extracts information, e.g. The expression is using a field or an
 * auxiliary input, etc. <br/>
 * The transformation of @Expression@ is the following:
 * <ul>
 * <li>@ColumnName@ are transformed into property name, e.g. @DocStatus@ into <b>documentStatus</b></li>
 * <li>@AuxiliarInput@ is transformed just removes the <b>@</b>, e.g. @FinancialManagementDep@ into
 * <b>FinancialManagementDep</b></li>
 * </ul>
 * 
 */
public class DynamicExpressionParser {

  private static final String[][] COMPARATIONS = { { "==", " === " }, { "=", " === " },
      { "!", " !== " }, { "^", " !== " }, { "-", " !== " } };

  private static final String[][] UNIONS = { { "|", " || " }, { "&", " && " } };

  private static final String TOKEN_PREFIX = "context.";
  private static Map<String, String> exprToJSMap;
  static {
    exprToJSMap = new HashMap<String, String>();
    exprToJSMap.put("'Y'", "true");
    exprToJSMap.put("'N'", "false");
  }

  private List<Field> fieldsInExpression = new ArrayList<Field>();
  private List<String> otherTokensInExpression = new ArrayList<String>();
  private List<Parameter> parametersInExpression = new ArrayList<Parameter>();
  private List<AuxiliaryInput> auxInputsInExpression = new ArrayList<AuxiliaryInput>();
  private List<String> sessionAttributesInExpression = new ArrayList<String>();

  private String code;
  private Tab tab;
  private Field field;
  private StringBuffer jsCode;
  private boolean tabLevelDisplayLogic = false;
  private boolean parameterDisplayLogic = false;
  Process process;
  private Parameter parameter;

  private ApplicationDictionaryCachedStructures cachedStructures;

  public DynamicExpressionParser(String code, Process process, boolean parameterDisplayLogic) {
    this.code = code;
    this.process = process;
    this.parameterDisplayLogic = parameterDisplayLogic;
    parse();
  }

  public DynamicExpressionParser(String code, Parameter parameter, boolean parameterDisplayLogic) {
    this.code = code;
    this.parameter = parameter;
    this.process = parameter.getObuiappProcess();
    this.parameterDisplayLogic = parameterDisplayLogic;
    parse();
  }

  public DynamicExpressionParser(String code, Tab tab, boolean tabLevelDisplayLogic) {
    this.code = code;
    this.tab = tab;
    this.tabLevelDisplayLogic = tabLevelDisplayLogic;
    parse();
  }

  public DynamicExpressionParser(String code, Tab tab) {
    this.code = code;
    this.tab = tab;
    parse();
  }

  public DynamicExpressionParser(String code, Tab tab, Field field) {
    this.code = code;
    this.tab = tab;
    this.field = field;
    parse();
  }

  public DynamicExpressionParser(String code, Tab tab,
      ApplicationDictionaryCachedStructures cachedStructures) {
    this.cachedStructures = cachedStructures;
    this.code = code;
    this.tab = tab;
    parse();
  }

  public DynamicExpressionParser(String code, Tab tab,
      ApplicationDictionaryCachedStructures cachedStructures, Field field) {
    this.cachedStructures = cachedStructures;
    this.code = code;
    this.tab = tab;
    this.field = field;
    parse();
  }

  /*
   * Note: This method was partially copied from WadUtility.
   */
  public void parse() {
    jsCode = new StringBuffer();

    StringTokenizer st = new StringTokenizer(code, "|&", true);
    String token, token2;
    String strAux;
    while (st.hasMoreTokens()) {
      strAux = st.nextToken().trim();
      int i[] = getFirstElement(UNIONS, strAux);
      if (i[0] != -1) {
        strAux = strAux.substring(0, i[0]) + UNIONS[i[1]][1]
            + strAux.substring(i[0] + UNIONS[i[1]][0].length());
      }

      int pos[] = getFirstElement(COMPARATIONS, strAux);
      token = strAux;
      token2 = "";
      if (pos[0] >= 0) {
        token = strAux.substring(0, pos[0]);
        token2 = strAux.substring(pos[0] + COMPARATIONS[pos[1]][0].length(), strAux.length());
        strAux = strAux.substring(0, pos[0]) + COMPARATIONS[pos[1]][1]
            + strAux.substring(pos[0] + COMPARATIONS[pos[1]][0].length(), strAux.length());
      }

      DisplayLogicElement leftPart = getDisplayLogicText(token, false, false);
      jsCode.append(leftPart.text);

      if (pos[0] >= 0) {
        jsCode.append(COMPARATIONS[pos[1]][1]);
      }

      // The value might be transformed if the leftPart contains the string 'currentValues'
      // or if a tab level display logic is being parsed
      DisplayLogicElement rightPart = getDisplayLogicText(token2,
          leftPart.text.contains("currentValues") || tabLevelDisplayLogic, leftPart.isBoolean);
      jsCode.append(rightPart.text);
    }
    // Handle accounting dimensions special display logic
    if (jsCode.toString().contains(DimensionDisplayUtility.DIM_DISPLAYLOGIC)) {
      String parsedDisplay = null;
      if (this.parameterDisplayLogic) {
        List<String> sessionVariablesToLoad = DimensionDisplayUtility
            .getRequiredSessionVariablesForTab(this.process, this.parameter);
        for (String sv : sessionVariablesToLoad) {
          sessionAttributesInExpression.add(sv);
        }
        parsedDisplay = DimensionDisplayUtility.computeAccountingDimensionDisplayLogic(
            this.process, this.parameter);
      } else {
        List<String> sessionVariablesToLoad = DimensionDisplayUtility
            .getRequiredSessionVariablesForTab(this.tab, this.field);
        for (String sv : sessionVariablesToLoad) {
          sessionAttributesInExpression.add(sv);
        }
        parsedDisplay = DimensionDisplayUtility.computeAccountingDimensionDisplayLogic(this.tab,
            this.field);
      }

      if (!"".equals(parsedDisplay)) {
        parsedDisplay = "(" + parsedDisplay + ")";
      }
      jsCode = new StringBuffer(jsCode.toString().replace(DimensionDisplayUtility.DIM_DISPLAYLOGIC,
          parsedDisplay));
    }

  }

  /**
   * Gets a JavaScript expression based on the dynamic expression, e.g @SomeColumn@!'Y' results in
   * currentValues.someColumn !== true.<br/>
   * Note: Field comparison with <b>'Y'</b> or <b>'N'</b> are transformed in <b>true</b> or
   * <b>false</b>
   * 
   * @return A JavaScript expression
   */
  public String getJSExpression() {
    return jsCode.toString();
  }

  /**
   * @see DynamicExpressionParser#getJSExpression()
   */
  public String toString() {
    return getJSExpression();
  }

  /**
   * Returns the list of Fields used in the dynamic expression
   * 
   */
  public List<Field> getFields() {
    return fieldsInExpression;
  }

  /**
   * Returns the list of tokens that are not fields of the tab It is only used when parsing the
   * display logic of the tabs
   * 
   */
  public List<String> getOtherTokensInExpression() {
    return otherTokensInExpression;
  }

  /**
   * Returns the list of Parameters used in the dynamic expression
   * 
   */
  public List<Parameter> getParameters() {
    return parametersInExpression;
  }

  /**
   * Returns the list of session attribute names used in the dynamic expression
   * 
   */
  public List<String> getSessionAttributes() {
    return sessionAttributesInExpression;
  }

  /**
   * Transform values into JavaScript equivalent, e.g. <b>'Y'</b> into <b>true</b>, based in a
   * defined map. Often used in dynamic expression comparisons
   * 
   * If the value is enclosed between brackets, it is extracted, translated and enclosed again
   * 
   * There is a junit test in the class: DynamicExpressionParserTest
   * 
   * @param value
   *          A string expression like <b>'Y'</b>
   * @return A equivalent value in JavaScript or the same string if has no mapping value
   */
  private String transformValue(String value) {
    if (value == null) {
      return null;
    }
    String removeBracketsRegExp = "[\\[\\(]*(.*?)[\\)\\]]*";
    Pattern pattern = Pattern.compile(removeBracketsRegExp);
    Matcher matcher = pattern.matcher(value);
    String transformedValueWithBrackets = null;
    // It is always matched: zero or plus opening brackets, followed by any string, follow by zero
    // or plus closing brackets
    if (matcher.matches()) {
      // Extracts the value
      String valueWithoutBrackets = matcher.group(1);
      // Transforms the value
      String transformedValueWithoutBrackets = exprToJSMap.get(valueWithoutBrackets) != null ? exprToJSMap
          .get(valueWithoutBrackets) : valueWithoutBrackets;
      // Re-encloses the value
      transformedValueWithBrackets = value.replace(valueWithoutBrackets,
          transformedValueWithoutBrackets);
    }
    return transformedValueWithBrackets;
  }

  /*
   * This method was partially copied from WadUtility.
   */
  private DisplayLogicElement getDisplayLogicText(String token, boolean transformValue,
      boolean boolLeftToken) {
    StringBuffer strOut = new StringBuffer();
    String localToken = token;
    boolean boolToken = false;
    int i = localToken.indexOf("@");
    while (i != -1) {
      strOut.append(localToken.substring(0, i));
      localToken = localToken.substring(i + 1);
      i = localToken.indexOf("@");
      if (i != -1) {
        String strAux = localToken.substring(0, i);
        localToken = localToken.substring(i + 1);
        DisplayLogicElement displayLogicElement = getDisplayLogicTextTranslate(strAux);
        // It needn't boolean transformation as it is a token like @column@
        strOut.append(displayLogicElement.text);
        boolToken = boolToken || displayLogicElement.isBoolean;
      }
      i = localToken.indexOf("@");
    }
    // Do boolean transformation in case comparison left member is a boolean column
    strOut.append(transformValue && boolLeftToken ? transformValue(localToken) : localToken);
    return new DisplayLogicElement(strOut.toString(), boolToken);
  }

  /*
   * This method is a different reimplementation of an equivalent method in WadUtility
   */
  private DisplayLogicElement getDisplayLogicTextTranslate(String token) {
    if (token == null || token.trim().equals(""))
      return new DisplayLogicElement("", false);
    List<Field> fields;
    List<AuxiliaryInput> auxIns;
    if (parameterDisplayLogic) {
      List<Parameter> parameters = process.getOBUIAPPParameterList();
      for (Parameter parameter : parameters) {
        if (token.equalsIgnoreCase(parameter.getDBColumnName())) {
          parametersInExpression.add(parameter);
          UIDefinition uiDef = UIDefinitionController.getInstance().getUIDefinition(
              parameter.getReference());
          if (uiDef.getDomainType() instanceof DateDomainType) {
            return new DisplayLogicElement(
                "OB.Utilities.Date.JSToOB(OB.Utilities.getValue(currentValues,'" + token
                    + "'),OB.Format.date)", uiDef instanceof YesNoUIDefinition);
          }
          return new DisplayLogicElement("OB.Utilities.getValue(currentValues,'" + token + "')",
              uiDef instanceof YesNoUIDefinition);
        }
      }
    } else {
      try {
        if (cachedStructures == null) {
          cachedStructures = WeldUtils
              .getInstanceFromStaticBeanManager(ApplicationDictionaryCachedStructures.class);
        }
        fields = cachedStructures.getFieldsOfTab(tab.getId());
        auxIns = cachedStructures.getAuxiliarInputList(tab.getId());
      } catch (NullPointerException e) {
        fields = tab.getADFieldList();
        auxIns = tab.getADAuxiliaryInputList();
      }
      for (Field field : fields) {
        if (field.getColumn() == null) {
          continue;
        }
        if (token.equalsIgnoreCase(field.getColumn().getDBColumnName()) && !tabLevelDisplayLogic) {
          fieldsInExpression.add(field);
          final String fieldName = KernelUtils.getInstance()
              .getPropertyFromColumn(field.getColumn()).getName();

          UIDefinition uiDef = UIDefinitionController.getInstance().getUIDefinition(
              field.getColumn().getId());
          if (uiDef.getDomainType() instanceof DateDomainType) {
            return new DisplayLogicElement(
                "OB.Utilities.Date.JSToOB(OB.Utilities.getValue(currentValues,'" + fieldName
                    + "'),OB.Format.date)", uiDef instanceof YesNoUIDefinition);
          }

          return new DisplayLogicElement(
              "OB.Utilities.getValue(currentValues,'" + fieldName + "')",
              uiDef instanceof YesNoUIDefinition);
        } else if (tabLevelDisplayLogic) {
          if (!otherTokensInExpression.contains(token)) {
            otherTokensInExpression.add(token);
          }
        }
      }
      for (AuxiliaryInput auxIn : auxIns) {
        if (token.equalsIgnoreCase(auxIn.getName())) {
          auxInputsInExpression.add(auxIn);
          return new DisplayLogicElement(TOKEN_PREFIX + auxIn.getName(), false);
        }
      }
    }

    String convertedToken = token;
    boolean isBoolean = false;

    // Session attributes (#sessionAttributeName) must not be converted to the inp format, and they
    // do not need to be treated as a boolean
    if (tabLevelDisplayLogic && !convertedToken.startsWith("#")) {
      Field ancestorField = lookForFieldInAncestorTabs(token);
      if (ancestorField != null) {
        // If the token is the name of an ancestor tab field, it must to converted to its inp format
        convertedToken = "inp" + Sqlc.TransformaNombreColumna(token);
        UIDefinition uiDef = UIDefinitionController.getInstance().getUIDefinition(
            ancestorField.getColumn().getId());
        // ... in that case, the left part is a boolean if that field is a YesNoUIDefinition
        isBoolean = (uiDef instanceof YesNoUIDefinition);
      }

    }
    sessionAttributesInExpression.add(convertedToken);
    return new DisplayLogicElement(TOKEN_PREFIX
        + (convertedToken.startsWith("#") ? convertedToken.replace("#", "_") : convertedToken),
        isBoolean);
  }

  private Field lookForFieldInAncestorTabs(String fieldName) {
    Field aField = null;
    Tab parentTab = KernelUtils.getInstance().getParentTab(tab);
    while (parentTab != null && aField == null) {
      aField = searchForFieldInTab(parentTab, fieldName);
      parentTab = KernelUtils.getInstance().getParentTab(parentTab);
    }
    return aField;
  }

  private Field searchForFieldInTab(Tab tab, String fieldName) {
    List<Field> fields = tab.getADFieldList();
    for (Field aField : fields) {
      if (aField.getColumn() == null) {
        continue;
      }
      if (fieldName.equalsIgnoreCase(aField.getColumn().getDBColumnName())) {
        return aField;
      }
    }
    return null;
  }

  /*
   * This method was partially copied from WadUtility.
   */
  private static int[] getFirstElement(String[][] array, String token) {
    int min[] = { -1, -1 }, aux;
    for (int i = 0; i < array.length; i++) {
      aux = token.indexOf(array[i][0]);
      if (aux != -1 && (aux < min[0] || min[0] == -1)) {
        min[0] = aux;
        min[1] = i;
      }
    }
    return min;
  }

  private class DisplayLogicElement {
    boolean isBoolean;
    String text;

    public DisplayLogicElement(String text, boolean isBoolean) {
      this.text = text;
      this.isBoolean = isBoolean;
    }
  }
}
