package org.openbravo.common.datasource;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.service.json.JsonUtils;

class ResultMapCriteriaUtils {
  private static final String CRITERIA_KEY = "criteria";
  private static final String VALUE_KEY = "value";
  private static final String FIELD_NAME_KEY = "fieldName";
  private static final String OPERATOR_KEY = "operator";

  private static final String OPERATOR_AND = "and";
  private static final String OPERATOR_OR = "or";

  private static final String OPERATOR_EQUALS = "equals";
  private static final String OPERATOR_NOTEQUAL = "notEqual";
  private static final String OPERATOR_IEQUALS = "iEquals";
  private static final String OPERATOR_INOTEQUAL = "iNotEqual";
  private static final String OPERATOR_GREATERTHAN = "greaterThan";
  private static final String OPERATOR_LESSTHAN = "lessThan";
  private static final String OPERATOR_GREATEROREQUAL = "greaterOrEqual";
  private static final String OPERATOR_LESSOREQUAL = "lessOrEqual";
  private static final String OPERATOR_IGREATERTHAN = "iGreaterThan";
  private static final String OPERATOR_ILESSTHAN = "iLessThan";
  private static final String OPERATOR_IGREATEROREQUAL = "iGreaterOrEqual";
  private static final String OPERATOR_ILESSOREQUAL = "iLessOrEqual";
  private static final String OPERATOR_CONTAINS = "contains";
  private static final String OPERATOR_STARTSWITH = "startsWith";
  private static final String OPERATOR_ENDSWITH = "endsWith";
  private static final String OPERATOR_ICONTAINS = "iContains";
  private static final String OPERATOR_ISTARTSWITH = "iStartsWith";
  private static final String OPERATOR_IENDSWITH = "iEndsWith";
  private static final String OPERATOR_NOTCONTAINS = "notContains";
  private static final String OPERATOR_NOTSTARTSWITH = "notStartsWith";
  private static final String OPERATOR_NOTENDSWITH = "notEndsWith";
  private static final String OPERATOR_INOTCONTAINS = "iNotContains";
  private static final String OPERATOR_INOTSTARTSWITH = "iNotStartsWith";
  private static final String OPERATOR_INOTENDSWITH = "iNotEndsWith";
  // private static final String OPERATOR_REGEXP = "regexp";
  // private static final String OPERATOR_IREGEXP = "iregexp";
  private static final String OPERATOR_ISNULL = "isNull";
  private static final String OPERATOR_NOTNULL = "notNull";
  private static final String OPERATOR_INSET = "inSet";
  private static final String OPERATOR_NOTINSET = "notInSet";
  private static final String OPERATOR_EQUALSFIELD = "equalsField";
  private static final String OPERATOR_NOTEQUALFIELD = "notEqualField";
  private static final String OPERATOR_GREATERTHANFIElD = "greaterThanField";
  private static final String OPERATOR_LESSTHANFIELD = "lessThanField";
  private static final String OPERATOR_GREATEROREQUALFIELD = "greaterOrEqualField";
  private static final String OPERATOR_LESSOREQUALFIElD = "lessOrEqualField";
  private static final String OPERATOR_CONTAINSFIELD = "containsField";
  private static final String OPERATOR_STARTSWITHFIELD = "startsWithField";
  private static final String OPERATOR_ENDSWITHFIELD = "endsWithField";
  private static final String OPERATOR_NOT = "not";
  private static final String OPERATOR_BETWEEN = "between";
  private static final String OPERATOR_BETWEENINCLUSIVE = "betweenInclusive";
  private static final String OPERATOR_IBETWEEN = "iBetween";
  private static final String OPERATOR_IBETWEENINCLUSIVE = "iBetweenInclusive";
  private static final String OPERATOR_EXISTS = "exists";

  Map<String, Object> recordMap;
  JSONArray criteriaArray;
  boolean mainOperatorIsAnd;

  private int UTCServerMinutesTimeZoneDiff = 0;
  private int clientUTCMinutesTimeZoneDiff = 0;

  private SimpleDateFormat simpleDateFormat = JsonUtils.createDateFormat();
  private SimpleDateFormat simpleDateTimeFormat = JsonUtils.createJSTimeFormat();

  ResultMapCriteriaUtils(Map<String, Object> orderMap, Map<String, String> parameters) {
    JSONArray _criteriaArray = null;
    try {
      _criteriaArray = (JSONArray) JsonUtils.buildCriteria(parameters).get("criteria");
    } catch (JSONException e) {
      _criteriaArray = null;
    }
    String mainOperator = parameters.get("operator");

    this.recordMap = orderMap;
    this.criteriaArray = _criteriaArray;
    this.mainOperatorIsAnd = OPERATOR_AND.equals(mainOperator);
  }

  boolean applyFilter() throws JSONException {
    if (criteriaArray == null) {
      return true;
    }
    boolean finalResult = mainOperatorIsAnd;
    for (int i = 0; i < criteriaArray.length(); i++) {
      // Each element of the criteria array is added assuming an OR statement.
      JSONObject criteria = criteriaArray.getJSONObject(i);
      boolean critResult = parseCriteria(criteria);
      if (mainOperatorIsAnd) {
        finalResult &= critResult;
      } else {
        finalResult |= critResult;
      }
    }
    return finalResult;
  }

  private boolean parseCriteria(JSONObject jsonCriteria) throws JSONException {
    // a constructor so the content is an advanced criteria
    if (jsonCriteria.has("_constructor") || hasOrAndOperator(jsonCriteria)) {
      return parseAdvancedCriteria(jsonCriteria);
    }
    return parseSingleClause(jsonCriteria);
  }

  private boolean hasOrAndOperator(JSONObject jsonCriteria) throws JSONException {
    if (!jsonCriteria.has(OPERATOR_KEY)) {
      return mainOperatorIsAnd;
    }
    return OPERATOR_OR.equals(jsonCriteria.get(OPERATOR_KEY))
        || OPERATOR_AND.equals(jsonCriteria.get(OPERATOR_KEY));
  }

  private boolean parseSingleClause(JSONObject jsonCriteria) throws JSONException {
    String operator = jsonCriteria.getString(OPERATOR_KEY);

    if (operator.equals(OPERATOR_BETWEEN) || operator.equals(OPERATOR_BETWEENINCLUSIVE)
        || operator.equals(OPERATOR_IBETWEEN) || operator.equals(OPERATOR_IBETWEENINCLUSIVE)) {
      return parseBetween(jsonCriteria, operator, true);
    }

    Object value = jsonCriteria.has(VALUE_KEY) ? jsonCriteria.get(VALUE_KEY) : null;

    if (operator.equals(OPERATOR_EXISTS)) {
      // not supported
      return mainOperatorIsAnd;
    }

    String fieldName = jsonCriteria.getString(FIELD_NAME_KEY);

    // translate to a OR for each value
    if (value instanceof JSONArray) {
      final JSONArray jsonArray = (JSONArray) value;
      final JSONObject advancedCriteria = new JSONObject();
      advancedCriteria.put(OPERATOR_KEY, OPERATOR_OR);
      final JSONArray subCriteria = new JSONArray();
      for (int i = 0; i < jsonArray.length(); i++) {
        final JSONObject subCriterion = new JSONObject();
        subCriterion.put(OPERATOR_KEY, operator);
        subCriterion.put(FIELD_NAME_KEY, fieldName);
        subCriterion.put(VALUE_KEY, jsonArray.get(i));
        subCriteria.put(i, subCriterion);
      }
      advancedCriteria.put(CRITERIA_KEY, subCriteria);
      return parseAdvancedCriteria(advancedCriteria);
    }

    // Retrieves the UTC time zone offset of the client
    if (jsonCriteria.has("minutesTimezoneOffset")) {
      int clientMinutesTimezoneOffset = Integer.parseInt(jsonCriteria.get("minutesTimezoneOffset")
          .toString());
      Calendar now = Calendar.getInstance();
      // Obtains the UTC time zone offset of the server
      int serverMinutesTimezoneOffset = (now.get(Calendar.ZONE_OFFSET) + now
          .get(Calendar.DST_OFFSET)) / (1000 * 60);
      // Obtains the time zone offset between the server and the client
      clientUTCMinutesTimeZoneDiff = clientMinutesTimezoneOffset;
      UTCServerMinutesTimeZoneDiff = serverMinutesTimezoneOffset;
    }

    if (operator.equals(OPERATOR_ISNULL) || operator.equals(OPERATOR_NOTNULL)) {
      value = null;
    }

    // if a comparison is done on an equal date then replace
    // with a between start time and end time on that date
    if (operator.equals(OPERATOR_EQUALS) || operator.equals(OPERATOR_EQUALSFIELD)) {
      Object curValue = recordMap.get(fieldName);
      if (curValue instanceof Date) {
        if (operator.equals(OPERATOR_EQUALS)) {
          return parseSimpleClause(fieldName, OPERATOR_GREATEROREQUAL, value)
              && parseSimpleClause(fieldName, OPERATOR_LESSOREQUAL, value);

        } else {
          return parseSimpleClause(fieldName, OPERATOR_GREATEROREQUALFIELD, value)
              && parseSimpleClause(fieldName, OPERATOR_LESSOREQUALFIElD, value);
        }
      }
    }

    return parseSimpleClause(fieldName, operator, value);
  }

  private boolean parseBetween(JSONObject jsonCriteria, String operator, boolean inclusive)
      throws JSONException {
    final String fieldName = jsonCriteria.getString(FIELD_NAME_KEY);
    final Object start = jsonCriteria.get("start");
    final Object end = jsonCriteria.get("end");
    final boolean leftClause = parseSimpleClause(fieldName, getBetweenOperator(operator, false),
        start);
    final boolean rightClause = parseSimpleClause(fieldName, getBetweenOperator(operator, true),
        end);
    return leftClause && rightClause;
  }

  private boolean parseSimpleClause(String fieldName, String operator, Object value)
      throws JSONException {

    String hqlOperator = getHqlOperator(operator);
    String strField = fieldName;
    boolean filterIdentifier = false;
    // if (strField.endsWith("$_identifier")) {
    // strField = strField.substring(0, strField.length() - 12);
    // filterIdentifier = true;
    // }
    Object mapValue = recordMap.get(strField);
    if (operator.equals(OPERATOR_NOTNULL)) {
      return mapValue != null;
    } else if (operator.equals(OPERATOR_ISNULL)) {
      return mapValue == null;
    }

    Object localValue = value;
    if (ignoreCase(mapValue, operator)) {
      localValue = localValue.toString().toUpperCase();
    }

    boolean returnVal = mainOperatorIsAnd;

    if ("id".equals(strField)) {
      // ID is always equals
      returnVal = mapValue != JSONObject.NULL && ((String) localValue).equals(mapValue);
    } else if (filterIdentifier) {
      BaseOBObject currentVal = (BaseOBObject) mapValue;
      String strCurrValIdentifier = currentVal.getIdentifier();
      String strCurrentFilter = (String) localValue;
      returnVal = StringUtils.containsIgnoreCase(strCurrValIdentifier, strCurrentFilter);
    } else if (mapValue instanceof Boolean) {
      returnVal = (Boolean) mapValue == (Boolean) localValue;
    } else if (mapValue instanceof BigDecimal) {
      BigDecimal filterValue = new BigDecimal((Integer) localValue);
      int compare = filterValue.compareTo((BigDecimal) mapValue);
      if ("=".equals(hqlOperator)) {
        returnVal = compare == 0;
      } else if ("!=".equals(hqlOperator)) {
        returnVal = compare != 0;
      } else if (">".equals(hqlOperator)) {
        returnVal = compare > 0;
      } else if (">=".equals(hqlOperator)) {
        returnVal = compare >= 0;
      } else if ("<".equals(hqlOperator)) {
        returnVal = compare < 0;
      } else if ("<=".equals(hqlOperator)) {
        returnVal = compare <= 0;
      }

    } else if (mapValue instanceof Date) {
      try {
        Date filterValue;
        try {
          filterValue = simpleDateTimeFormat.parse(localValue.toString());
        } catch (ParseException e) {
          // When a DateTime column is filtered, plan Date values are used
          // See issue https://issues.openbravo.com/view.php?id=23203
          filterValue = simpleDateFormat.parse(localValue.toString());
        }
        final Calendar calendar = Calendar.getInstance();
        calendar.setTime(filterValue);
        // Applies the time zone offset difference of the client
        calendar.add(Calendar.MINUTE, clientUTCMinutesTimeZoneDiff);
        // move the date to the beginning of the day
        if (isGreaterOperator(operator)) {
          calendar.set(Calendar.HOUR, 0);
          calendar.set(Calendar.MINUTE, 0);
          calendar.set(Calendar.SECOND, 0);
          calendar.set(Calendar.MILLISECOND, 0);
        } else if (isLesserOperator(operator)) {
          // move the data to the end of the day
          calendar.set(Calendar.HOUR, 23);
          calendar.set(Calendar.MINUTE, 59);
          calendar.set(Calendar.SECOND, 59);
          calendar.set(Calendar.MILLISECOND, 999);
        }
        // Applies the time zone offset difference of the server
        calendar.add(Calendar.MINUTE, -UTCServerMinutesTimeZoneDiff);
        Date mapDate = (Date) mapValue;
        boolean isBefore = mapDate.before(calendar.getTime());
        boolean isAfter = mapDate.after(calendar.getTime());
        boolean sameDate = mapDate.compareTo(calendar.getTime()) == 0;
        if ("=".equals(hqlOperator)) {
          returnVal = sameDate;
        } else if ("!=".equals(hqlOperator)) {
          returnVal = !sameDate;
        } else if (">".equals(hqlOperator)) {
          returnVal = isAfter;
        } else if (">=".equals(hqlOperator)) {
          returnVal = isAfter || sameDate;
        } else if ("<".equals(hqlOperator)) {
          returnVal = isBefore;
        } else if ("<=".equals(hqlOperator)) {
          returnVal = isBefore || sameDate;
        }
      } catch (Exception e) {
        throw new IllegalArgumentException(e);
      }
    } else if (mapValue instanceof String) {
      String strCurrentValue = (String) mapValue;
      String strCurrentFilter = (String) localValue;
      returnVal = StringUtils.containsIgnoreCase(strCurrentValue, strCurrentFilter);
    }

    if (isNot(operator)) {
      return !returnVal;
    } else {
      return returnVal;
    }
  }

  private boolean isGreaterOperator(String operator) {
    return operator != null
        && (operator.equals(OPERATOR_GREATERTHAN) || operator.equals(OPERATOR_GREATEROREQUAL)
            || operator.equals(OPERATOR_IGREATERTHAN) || operator.equals(OPERATOR_IGREATEROREQUAL)
            || operator.equals(OPERATOR_GREATERTHANFIElD) || operator
              .equals(OPERATOR_GREATEROREQUALFIELD));
  }

  private boolean isLesserOperator(String operator) {
    return operator != null
        && (operator.equals(OPERATOR_LESSTHAN) || operator.equals(OPERATOR_LESSOREQUAL)
            || operator.equals(OPERATOR_ILESSTHAN) || operator.equals(OPERATOR_ILESSOREQUAL)
            || operator.equals(OPERATOR_LESSTHANFIELD) || operator
              .equals(OPERATOR_LESSOREQUALFIElD));
  }

  private boolean parseAdvancedCriteria(JSONObject advancedCriteria) throws JSONException {
    final String operator = advancedCriteria.getString(OPERATOR_KEY);
    if (operator.equals(OPERATOR_NOT)) {
      final boolean clause = parseStructuredClause(advancedCriteria.getJSONArray(CRITERIA_KEY),
          OPERATOR_OR);
      return !clause;
    }
    if (operator.equals(OPERATOR_AND)) {
      return parseStructuredClause(advancedCriteria.getJSONArray(CRITERIA_KEY), OPERATOR_AND);
    }
    if (operator.equals(OPERATOR_OR)) {
      final boolean value = parseStructuredClause(advancedCriteria.getJSONArray(CRITERIA_KEY),
          OPERATOR_OR);
      return value;
    }
    return parseSingleClause(advancedCriteria);
  }

  private boolean parseStructuredClause(JSONArray clauses, String hqlOperator) throws JSONException {
    boolean doOr = OPERATOR_OR.equals(hqlOperator);
    boolean doAnd = OPERATOR_AND.equals(hqlOperator);
    for (int i = 0; i < clauses.length(); i++) {
      final JSONObject clause = clauses.getJSONObject(i);
      if (clause.has(VALUE_KEY) && clause.get(VALUE_KEY) != null
          && clause.getString(VALUE_KEY).equals("")) {
        continue;
      }
      final boolean clauseResult = parseCriteria(clause);
      if (doOr && clauseResult) {
        return true;
      }
      if (doAnd && !clauseResult) {
        return false;
      }
    }
    if (doOr) {
      return false;
    } else if (doAnd) {
      return true;
    }
    return mainOperatorIsAnd;
  }

  private String getBetweenOperator(String operator, boolean rightClause) {
    if (operator.equals(OPERATOR_IBETWEEN)) {
      if (rightClause) {
        return OPERATOR_ILESSTHAN;
      } else {
        return OPERATOR_IGREATERTHAN;
      }
    }
    if (operator.equals(OPERATOR_BETWEEN)) {
      if (rightClause) {
        return OPERATOR_LESSTHAN;
      } else {
        return OPERATOR_GREATERTHAN;
      }
    }
    if (operator.equals(OPERATOR_IBETWEENINCLUSIVE)) {
      if (rightClause) {
        return OPERATOR_ILESSOREQUAL;
      } else {
        return OPERATOR_IGREATEROREQUAL;
      }
    }
    if (operator.equals(OPERATOR_BETWEENINCLUSIVE)) {
      if (rightClause) {
        return OPERATOR_LESSOREQUAL;
      } else {
        return OPERATOR_GREATEROREQUAL;
      }
    }
    throw new IllegalArgumentException("Operator not supported " + operator);
  }

  private boolean ignoreCase(Object mapValue, String operator) {
    if (mapValue instanceof BigDecimal || mapValue instanceof Date) {
      return false;
    }
    return operator.equals(OPERATOR_IEQUALS) || operator.equals(OPERATOR_INOTEQUAL)
        || operator.equals(OPERATOR_CONTAINS) || operator.equals(OPERATOR_ENDSWITH)
        || operator.equals(OPERATOR_STARTSWITH) || operator.equals(OPERATOR_ICONTAINS)
        || operator.equals(OPERATOR_INOTSTARTSWITH) || operator.equals(OPERATOR_INOTENDSWITH)
        || operator.equals(OPERATOR_NOTSTARTSWITH) || operator.equals(OPERATOR_NOTCONTAINS)
        || operator.equals(OPERATOR_INOTCONTAINS) || operator.equals(OPERATOR_NOTENDSWITH)
        || operator.equals(OPERATOR_IENDSWITH) || operator.equals(OPERATOR_ISTARTSWITH)
        || operator.equals(OPERATOR_IBETWEEN) || operator.equals(OPERATOR_IGREATEROREQUAL)
        || operator.equals(OPERATOR_ILESSOREQUAL) || operator.equals(OPERATOR_IGREATERTHAN)
        || operator.equals(OPERATOR_ILESSTHAN) || operator.equals(OPERATOR_IBETWEENINCLUSIVE);
  }

  private boolean isNot(String operator) {
    return operator.equals(OPERATOR_NOTCONTAINS) || operator.equals(OPERATOR_NOTENDSWITH)
        || operator.equals(OPERATOR_NOTSTARTSWITH) || operator.equals(OPERATOR_INOTCONTAINS)
        || operator.equals(OPERATOR_INOTENDSWITH) || operator.equals(OPERATOR_INOTSTARTSWITH)
        || operator.equals(OPERATOR_NOT) || operator.equals(OPERATOR_NOTINSET);
  }

  private String getHqlOperator(String operator) {
    if (operator.equals(OPERATOR_EQUALS)) {
      return "=";
    } else if (operator.equals(OPERATOR_INSET)) {
      return "in";
    } else if (operator.equals(OPERATOR_NOTINSET)) {
      return "in";
    } else if (operator.equals(OPERATOR_NOTEQUAL)) {
      return "!=";
    } else if (operator.equals(OPERATOR_IEQUALS)) {
      return "=";
    } else if (operator.equals(OPERATOR_INOTEQUAL)) {
      return "!=";
    } else if (operator.equals(OPERATOR_GREATERTHAN)) {
      return ">";
    } else if (operator.equals(OPERATOR_LESSTHAN)) {
      return "<";
    } else if (operator.equals(OPERATOR_GREATEROREQUAL)) {
      return ">=";
    } else if (operator.equals(OPERATOR_LESSOREQUAL)) {
      return "<=";
    } else if (operator.equals(OPERATOR_IGREATERTHAN)) {
      return ">";
    } else if (operator.equals(OPERATOR_ILESSTHAN)) {
      return "<";
    } else if (operator.equals(OPERATOR_IGREATEROREQUAL)) {
      return ">=";
    } else if (operator.equals(OPERATOR_ILESSOREQUAL)) {
      return "<=";
    } else if (operator.equals(OPERATOR_CONTAINS)) {
      return "like";
    } else if (operator.equals(OPERATOR_STARTSWITH)) {
      return "like";
    } else if (operator.equals(OPERATOR_ENDSWITH)) {
      return "like";
    } else if (operator.equals(OPERATOR_ICONTAINS)) {
      return "like";
    } else if (operator.equals(OPERATOR_ISTARTSWITH)) {
      return "like";
    } else if (operator.equals(OPERATOR_IENDSWITH)) {
      return "like";
    } else if (operator.equals(OPERATOR_NOTCONTAINS)) {
      return "like";
    } else if (operator.equals(OPERATOR_NOTSTARTSWITH)) {
      return "like";
    } else if (operator.equals(OPERATOR_NOTENDSWITH)) {
      return "like";
    } else if (operator.equals(OPERATOR_INOTCONTAINS)) {
      return "like";
    } else if (operator.equals(OPERATOR_INOTSTARTSWITH)) {
      return "like";
    } else if (operator.equals(OPERATOR_INOTENDSWITH)) {
      return "like";
    } else if (operator.equals(OPERATOR_EQUALSFIELD)) {
      return "=";
    } else if (operator.equals(OPERATOR_NOTEQUALFIELD)) {
      return "!=";
    } else if (operator.equals(OPERATOR_GREATERTHANFIElD)) {
      return ">";
    } else if (operator.equals(OPERATOR_LESSTHANFIELD)) {
      return "<";
    } else if (operator.equals(OPERATOR_GREATEROREQUALFIELD)) {
      return ">=";
    } else if (operator.equals(OPERATOR_LESSOREQUALFIElD)) {
      return "<=";
    } else if (operator.equals(OPERATOR_CONTAINSFIELD)) {
      return "like";
    } else if (operator.equals(OPERATOR_STARTSWITHFIELD)) {
      return "like";
    } else if (operator.equals(OPERATOR_ENDSWITHFIELD)) {
      return "like";
    } else if (operator.equals(OPERATOR_ISNULL)) {
      return "is";
    } else if (operator.equals(OPERATOR_NOTNULL)) {
      return "is not";
    } else if (operator.equals(OPERATOR_EXISTS)) {
      return "exists";
    }
    // todo throw exception
    return null;
  }
}
