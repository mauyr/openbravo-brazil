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
 * All portions are Copyright (C) 2013-2015 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 *************************************************************************
 */
package org.openbravo.financial;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.security.OrganizationStructureProvider;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBDateUtils;
import org.openbravo.model.ad.datamodel.Table;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.financialmgmt.accounting.AccountingFact;
import org.openbravo.model.financialmgmt.calendar.Period;

public class ResetAccounting {
  final static int FETCH_SIZE = 1000;
  private static final Logger log4j = Logger.getLogger(ResetAccounting.class);

  @SuppressWarnings("unchecked")
  public static HashMap<String, Integer> delete(String adClientId, String adOrgId,
      String adTableId, String recordId, String strdatefrom, String strdateto) throws OBException {
    if (recordId == null) {
      recordId = "";
    }
    long totalProcess = System.currentTimeMillis();
    long start = 0l;
    long end = 0l;
    long totalselect = 0l;
    int deleted = 0;
    int updated = 0;
    HashMap<String, Integer> results = new HashMap<String, Integer>();
    results.put("deleted", 0);
    results.put("updated", 0);
    results.put("totaldeleted", 0);
    results.put("totalupdated", 0);
    String client = adClientId;
    List<String> tables = getTables(adTableId);
    try {
      Set<String> orgIds = new OrganizationStructureProvider().getChildTree(adOrgId, true);
      for (String table : tables) {
        List<String> docbasetypes = getDocbasetypes(client, table, recordId);
        String myQuery = "select distinct e.recordID from FinancialMgmtAccountingFact e where e.organization.id in (:orgIds) and e.client.id = :clientId and e.table.id = :tableId";
        if (recordId != null && !"".equals(recordId)) {
          myQuery = myQuery + " and e.recordID = :recordId ";
        }
        for (String dbt : docbasetypes) {
          List<Date[]> periods = new ArrayList<Date[]>();
          // organizationPeriod: hashmap with organizations allow period control and their open
          // periods
          Map<String, List<Date[]>> organizationPeriod = new HashMap<String, List<Date[]>>();
          // organizationPeriodControl: hashmap with organizations and their organization allow
          // period control associated
          Map<String, String> organizationPeriodControl = new HashMap<String, String>();
          String calendarId = getCalendarId(adOrgId);
          Iterator<String> iterator = orgIds.iterator();
          while (iterator.hasNext()) {
            String organization = iterator.next();
            String myQuery1 = "select p.id from Organization p where ad_org_getperiodcontrolallow(:organization)=p.id";
            Query query1 = OBDal.getInstance().getSession().createQuery(myQuery1);
            query1.setString("organization", organization);
            query1.setMaxResults(1);
            if (query1.uniqueResult() != null) {
              String orgperiodcontrol = query1.uniqueResult().toString();
              organizationPeriodControl.put(organization, orgperiodcontrol);
              if (!organizationPeriod.keySet().contains(orgperiodcontrol)) {
                periods = getPeriodsDates(getOpenPeriods(client, dbt, orgIds, calendarId, table,
                    recordId, strdatefrom, strdateto, orgperiodcontrol));
                organizationPeriod.put(orgperiodcontrol, periods);
              }
            }
          }
          int docUpdated = 0;
          int docDeleted = 0;
          for (String organization : orgIds) {
            String orgAllow = organizationPeriodControl.get(organization);
            periods = organizationPeriod.get(orgAllow);
            for (Date[] p : periods) {
              StringBuffer consDate = new StringBuffer();
              consDate.append(" and e.documentCategory = :dbt");
              consDate.append(" and e.organization.id = :organization");
              consDate.append(" and e.accountingDate >= :dateFrom and e.accountingDate <= :dateTo");
              String exceptionsSql = myQuery + consDate.toString();
              consDate
                  .append(" and not exists (select a from FinancialMgmtAccountingFact a where a.recordID = e.recordID and a.table.id = e.table.id and (a.accountingDate < :dateFrom or a.accountingDate > :dateTo))");
              final Query query = OBDal.getInstance().getSession()
                  .createQuery(myQuery + consDate.toString());
              if (recordId != null && !"".equals(recordId)) {
                query.setString("recordId", recordId);
              }
              query.setParameterList("orgIds", orgIds);
              query.setString("clientId", client);
              query.setString("dbt", dbt);
              query.setString("tableId", table);
              query.setDate("dateFrom", p[0]);
              query.setDate("dateTo", p[1]);
              query.setString("organization", organization);
              if (recordId != null && !"".equals(recordId)) {
                query.setMaxResults(1);
              } else {
                query.setFetchSize(FETCH_SIZE);
              }
              start = System.currentTimeMillis();
              List<String> transactions = query.list();
              end = System.currentTimeMillis();
              totalselect = totalselect + end - start;
              while (transactions.size() > 0) {
                HashMap<String, Integer> partial = delete(transactions, table, client);
                deleted = deleted + partial.get("deleted");
                updated = updated + partial.get("updated");
                docUpdated = docUpdated + partial.get("updated");
                docDeleted = docDeleted + partial.get("deleted");
                start = System.currentTimeMillis();
                transactions = query.list();
                end = System.currentTimeMillis();
                totalselect = totalselect + end - start;
              }
              // Documents with postings in different periods are treated separately to validate
              // all
              // dates are within an open period
              HashMap<String, Integer> partial = treatExceptions(exceptionsSql, recordId, table,
                  orgIds, client, p[0], p[1], calendarId, strdatefrom, strdateto, dbt, orgAllow,
                  organization);
              deleted = deleted + partial.get("deleted");
              updated = updated + partial.get("updated");
              docUpdated = docUpdated + partial.get("updated");
              docDeleted = docDeleted + partial.get("deleted");
            }
          }
          log4j.debug("docBaseType: " + dbt);
          log4j.debug("updated: " + docUpdated);
          log4j.debug("deleted: " + docDeleted);
        }
      }

    } catch (OBException e) {

      throw e;
    } catch (Exception e) {
      throw new OBException("Delete failed", e);
    }
    results.put("deleted", deleted);
    results.put("updated", updated);
    log4j.debug("total totalProcess (milies): " + (System.currentTimeMillis() - totalProcess));
    if (recordId != null && !"".equals(recordId) && deleted == 0 && updated == 0) {
      if (recordId != null && !"".equals(recordId) && adTableId != null && !"".equals(adTableId)) {
        // If record exists but there is no entry in fact table then unpost record
        try {
          OBContext.setAdminMode(false);
          Table table = OBDal.getInstance().get(Table.class, adTableId);
          OBCriteria<AccountingFact> obc = OBDal.getInstance().createCriteria(AccountingFact.class);
          obc.setFilterOnReadableClients(false);
          obc.setFilterOnReadableOrganization(false);
          obc.setFilterOnActive(false);
          obc.add(Restrictions.eq(AccountingFact.PROPERTY_RECORDID, recordId));
          obc.add(Restrictions.eq(AccountingFact.PROPERTY_TABLE, table));
          obc.setMaxResults(1);
          if (obc.list().isEmpty() && !table.isView()) {
            String tableName = table.getDBTableName();
            String tableIdName = table.getDBTableName() + "_Id";
            String strUpdate = "";
            if (hasProcessingColumn(table.getId())) {
              strUpdate = "update "
                  + tableName
                  + " set posted='N', processing='N' where (posted<>'N' or posted is null or processing='N') and "
                  + tableIdName + " = :recordID ";
            } else {
              strUpdate = "update " + tableName
                  + " set posted='N' where (posted<>'N' or posted is null) and " + tableIdName
                  + " = :recordID ";
            }
            final Query update = OBDal.getInstance().getSession().createSQLQuery(strUpdate);
            update.setParameter("recordID", recordId);
            updated = update.executeUpdate();
            return results;
          }
        } finally {
          OBContext.restorePreviousMode();
        }
      }
      throw new OBException("@PeriodClosedForUnPosting@");
    }
    return results;
  }

  private static HashMap<String, Integer> delete(List<String> transactions, String tableId,
      String client) throws OBException {
    HashMap<String, Integer> result = new HashMap<String, Integer>();
    if (transactions.size() == 0) {
      result.put("deleted", 0);
      result.put("updated", 0);
      return result;
    }
    String tableName = "";
    String tableIdName = "";
    OBContext.setAdminMode(false);
    try {
      // First undo date balancing for those balanced entries
      String strUpdateBalanced = "update FinancialMgmtAccountingFact fact set dateBalanced = null "
          + "where fact.dateBalanced is not null "
          + "and exists (select 1 from FinancialMgmtAccountingFact f "
          + "where f.recordID in :transactions " + "and  f.table.id = :tableId "
          + "and f.client.id=:clientId and f.recordID2=fact.recordID2)";
      final Query updateBalanced = OBDal.getInstance().getSession().createQuery(strUpdateBalanced);
      updateBalanced.setString("tableId", tableId);
      updateBalanced.setParameterList("transactions", transactions);
      updateBalanced.setString("clientId", client);
      int balancedUpdated = updateBalanced.executeUpdate();
      Table table = OBDal.getInstance().get(Table.class, tableId);
      if (!table.isView()) {
        tableName = table.getDBTableName();
        tableIdName = table.getDBTableName() + "_Id";
        String strUpdate = "";
        if (hasProcessingColumn(table.getId())) {
          strUpdate = "update "
              + tableName
              + " set posted='N', processing='N' where (posted<>'N' or posted is null or processing='N') and "
              + tableIdName + " in (:transactions) ";
        } else {
          strUpdate = "update " + tableName
              + " set posted='N' where (posted<>'N' or posted is null) and " + tableIdName
              + " in (:transactions) ";
        }
        String strDelete = "delete from FinancialMgmtAccountingFact where table.id = :tableId and recordID in (:transactions) and client.id=:clientId";
        final Query update = OBDal.getInstance().getSession().createSQLQuery(strUpdate);
        update.setParameterList("transactions", transactions);
        int updated = update.executeUpdate();
        final Query delete = OBDal.getInstance().getSession().createQuery(strDelete);
        delete.setString("tableId", tableId);
        delete.setParameterList("transactions", transactions);
        delete.setString("clientId", client);
        int deleted = delete.executeUpdate();
        result.put("deleted", deleted);
        result.put("updated", updated);
        OBDal.getInstance().getConnection().commit();
        OBDal.getInstance().getSession().clear();
      }
      return result;
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      throw new OBException("Error Deleting Accounting", e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public static HashMap<String, Integer> restore(String clientId, String adOrgId, String datefrom,
      String dateto) throws OBException {
    HashMap<String, Integer> results = new HashMap<String, Integer>();
    results.put("deleted", 0);
    results.put("updated", 0);
    List<String> tableIds = getActiveTables(clientId, adOrgId);
    for (String tableId : tableIds) {
      HashMap<String, Integer> partial = restore(clientId, adOrgId, tableId, datefrom, dateto);
      results.put("deleted", results.get("deleted") + partial.get("deleted"));
      results.put("updated", results.get("updated") + partial.get("updated"));

    }
    return results;
  }

  public static HashMap<String, Integer> restore(String clientId, String adOrgId, String tableId,
      String datefrom, String dateto) throws OBException {
    HashMap<String, Integer> results = new HashMap<String, Integer>();
    results.put("deleted", 0);
    results.put("updated", 0);
    String tableName = "";
    String tableDate = "";
    OBContext.setAdminMode(false);
    try {
      Table table = OBDal.getInstance().get(Table.class, tableId);
      if (!table.isView()) {
        tableName = table.getDBTableName();
        tableDate = ModelProvider.getInstance().getEntityByTableName(table.getDBTableName())
            .getPropertyByColumnName(table.getAcctdateColumn().getDBColumnName()).getColumnName();

        String strUpdate = "";
        if (hasProcessingColumn(table.getId())) {
          strUpdate = "update "
              + tableName
              + " set posted='N', processing='N' where posted not in ('Y') and processed = 'Y' and AD_Org_ID in (:orgIds)  ";
        } else {
          strUpdate = "update "
              + tableName
              + " set posted='N' where posted not in ('Y') and processed = 'Y' and AD_Org_ID in (:orgIds)  ";
        }
        if (!("".equals(datefrom))) {
          strUpdate = strUpdate + " and " + tableDate + " >= :dateFrom ";
        }
        if (!("".equals(dateto))) {
          strUpdate = strUpdate + " and " + tableDate + " <= :dateTo ";
        }

        Query update = OBDal.getInstance().getSession().createSQLQuery(strUpdate);
        update.setParameterList("orgIds",
            new OrganizationStructureProvider().getNaturalTree(adOrgId));
        try {
          if (!("".equals(datefrom))) {
            update.setDate("dateFrom", OBDateUtils.getDate(datefrom));
          }
          if (!("".equals(dateto))) {
            update.setDate("dateTo", OBDateUtils.getDate(dateto));
          }
        } catch (ParseException e) {
          log4j.error("Restore - Error parsisng dates", e);
        }

        int updated = update.executeUpdate();
        results.put("updated", updated);
        OBDal.getInstance().getConnection().commit();
        OBDal.getInstance().getSession().clear();
      }
      return results;
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      throw new OBException("Error Reseting Accounting", e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  @SuppressWarnings("unchecked")
  private static List<String> getTables(String adTableId) {
    OBContext.setAdminMode(false);
    try {
      List<String> accountingTables = new ArrayList<String>();
      if (!"".equals(adTableId)) {
        Table myTable = OBDal.getInstance().get(Table.class, adTableId);
        accountingTables.add(myTable.getId());
        return accountingTables;
      }
      String myQuery = "select distinct e.table.id from FinancialMgmtAccountingFact e where e.table.id <> '145'";
      accountingTables = OBDal.getInstance().getSession().createQuery(myQuery).list();
      return accountingTables;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  @SuppressWarnings("unused")
  private static List<Organization> getOrganizations(Client client, Set<String> orgIds) {
    final String CLIENT_SYSTEM = "0";
    OBCriteria<Organization> obc = OBDal.getInstance().createCriteria(Organization.class);
    if (!CLIENT_SYSTEM.equals(client.getId())) {
      obc.add(Restrictions.eq(Organization.PROPERTY_CLIENT, client));
    }
    obc.add(Restrictions.in(Organization.PROPERTY_ID, orgIds));
    obc.setFilterOnReadableClients(false);
    obc.setFilterOnReadableOrganization(false);
    return obc.list();
  }

  private static List<String> getDocbasetypes(String clientId, String tableId, String recordId) {
    String myQuery = "select distinct e.documentCategory from FinancialMgmtAccountingFact e where e.client.id = :clientId and e.table.id = :tableId ";
    if (!"".equals(recordId)) {
      myQuery = myQuery + "and e.recordID=:recordId";
    }
    Query query = OBDal.getInstance().getSession().createQuery(myQuery);
    query.setString("clientId", clientId);
    query.setString("tableId", tableId);
    if (!"".equals(recordId)) {
      query.setString("recordId", recordId);
      query.setMaxResults(1);
    }
    @SuppressWarnings("unchecked")
    List<String> docbasetypes = query.list();
    return docbasetypes;
  }

  @SuppressWarnings("unchecked")
  private static List<Period> getOpenPeriods(String clientId, String docBaseType,
      Set<String> orgIds, String calendarId, String tableId, String recordId, String datefrom,
      String dateto, String orgPeriodControl) {
    if (!"".equals(recordId)) {
      List<Period> periods = new ArrayList<Period>();
      periods.add(getDocumentPeriod(clientId, tableId, recordId, docBaseType, orgPeriodControl,
          orgIds));
      return periods;

    }
    String myQuery = "select distinct p from FinancialMgmtPeriodControl e left join e.period p left join p.year y left join y.calendar c where c.id = :calendarId and e.client.id = :clientId and e.documentCategory = :docbasetype and e.periodStatus = 'O' and e.organization.id = :orgPeriodControl";

    if (!("".equals(datefrom)) && !("".equals(dateto))) {
      myQuery = myQuery + " and p.startingDate <= :dateTo";
      myQuery = myQuery + " and p.endingDate >= :dateFrom";
    } else if (!("".equals(datefrom)) && ("".equals(dateto))) {
      myQuery = myQuery + " and p.endingDate >= :dateFrom";
    } else if (("".equals(datefrom)) && !("".equals(dateto))) {
      myQuery = myQuery + " and p.startingDate <= :dateTo";
    }
    Query query = OBDal.getInstance().getSession().createQuery(myQuery);
    // TODO: Review orgIds
    // query.setParameterList("orgIds", orgIds);
    query.setString("calendarId", calendarId);
    query.setString("clientId", clientId);
    query.setString("docbasetype", docBaseType);
    query.setString("orgPeriodControl", orgPeriodControl);

    try {
      if (!("".equals(datefrom))) {
        query.setDate("dateFrom", OBDateUtils.getDate(datefrom));
      }
      if (!("".equals(dateto))) {
        query.setDate("dateTo", OBDateUtils.getDate(dateto));
      }
    } catch (ParseException e) {
      log4j.error("GetOpenPeriods - error parsing dates", e);
    }
    return query.list();
  }

  private static Period getDocumentPeriod(String clientId, String tableId, String recordId,
      String docBaseType, String orgPeriodControl, Set<String> orgIds) {
    String myQuery = "select distinct e.period from FinancialMgmtAccountingFact e , FinancialMgmtPeriodControl p where p.period=e.period and p.periodStatus = 'O' and e.client.id = :clientId and e.table.id = :tableId and e.recordID=:recordId and p.documentCategory = :docbasetype and p.organization.id  = :orgPeriodControl and e.organization.id in (:orgIds)";
    Query query = OBDal.getInstance().getSession().createQuery(myQuery);
    query.setString("clientId", clientId);
    query.setString("tableId", tableId);
    query.setString("recordId", recordId);
    query.setString("docbasetype", docBaseType);
    query.setString("orgPeriodControl", orgPeriodControl);
    query.setParameterList("orgIds", orgIds);
    query.setMaxResults(1);
    Period period = (Period) query.uniqueResult();
    if (period == null) {
      throw new OBException("@PeriodClosedForUnPosting@");
    }
    return period;
  }

  private static List<Date[]> getPeriodsDates(List<Period> periods) {
    List<Date[]> result = new ArrayList<Date[]>();
    OBContext.setAdminMode();
    try {
      for (Period period : periods) {
        Date[] dates = new Date[2];
        dates[0] = period.getStartingDate();
        dates[1] = period.getEndingDate();
        result.add(dates);
      }
    } finally {
      OBContext.restorePreviousMode();
    }
    return result;
  }

  private static String getCalendarId(String adOrgId) {
    Organization organization = OBDal.getInstance().get(Organization.class, adOrgId);
    if (organization.getCalendar() != null) {
      return organization.getCalendar().getId();
    } else {
      return getCalendarId(new OrganizationStructureProvider().getParentOrg(adOrgId));
    }
  }

  @SuppressWarnings("unchecked")
  private static List<String> getActiveTables(String clientId, String adOrgId) {
    String myQuery = "select distinct table.id from FinancialMgmtAcctSchemaTable where accountingSchema.id in (:accountingSchemaIds) and client.id = :clientId and active= true";
    Query query = OBDal.getInstance().getSession().createQuery(myQuery);
    List<String> accountingSchemaIds = getAccountingSchemaIds(clientId, adOrgId);
    query.setParameterList("accountingSchemaIds", accountingSchemaIds);
    query.setString("clientId", clientId);
    return query.list();
  }

  @SuppressWarnings("unchecked")
  private static List<String> getAccountingSchemaIds(String clientId, String orgIg) {
    String myQuery = "select distinct accountingSchema.id from OrganizationAcctSchema where client.id = :clientId and active= true and organization.id in (:orgIds)";
    Query query = OBDal.getInstance().getSession().createQuery(myQuery);
    query.setString("clientId", clientId);
    query.setParameterList("orgIds", new OrganizationStructureProvider().getNaturalTree(orgIg));
    return query.list();

  }

  private static HashMap<String, Integer> treatExceptions(String myQuery, String recordId,
      String table, Set<String> orgIds, String client, Date dateFrom, Date dateTo,
      String calendarId, String datefrom, String dateto, String dbt, String orgPeriodControl,
      String targetOrganization) {
    HashMap<String, Integer> results = new HashMap<String, Integer>();
    results.put("deleted", 0);
    results.put("updated", 0);
    final Query query = OBDal.getInstance().getSession().createQuery(myQuery);
    if (recordId != null && !"".equals(recordId)) {
      query.setString("recordId", recordId);
    }
    query.setParameterList("orgIds", orgIds);
    query.setString("clientId", client);
    query.setString("dbt", dbt);
    query.setString("tableId", table);
    query.setDate("dateFrom", dateFrom);
    query.setDate("dateTo", dateTo);
    query.setString("organization", targetOrganization);
    if (recordId != null && !"".equals(recordId)) {
      query.setMaxResults(1);
    }
    @SuppressWarnings("unchecked")
    List<String> transactions = query.list();
    for (String transaction : transactions) {
      OBCriteria<AccountingFact> factCrit = OBDal.getInstance()
          .createCriteria(AccountingFact.class);
      factCrit.add(Restrictions.eq(AccountingFact.PROPERTY_RECORDID, transaction));
      factCrit.add(Restrictions.eq(AccountingFact.PROPERTY_TABLE,
          OBDal.getInstance().get(Table.class, table)));
      factCrit.add(Restrictions.eq(AccountingFact.PROPERTY_CLIENT,
          OBDal.getInstance().get(Client.class, client)));
      List<AccountingFact> facts = factCrit.list();
      Set<Date> exceptionDates = new HashSet<Date>();
      for (AccountingFact fact : facts) {
        if (dateFrom.compareTo(fact.getAccountingDate()) != 0
            || dateTo.compareTo(fact.getAccountingDate()) != 0) {
          exceptionDates.add(fact.getAccountingDate());
        }
      }
      if (checkDates(exceptionDates, client, orgIds, facts.get(0).getDocumentCategory(),
          calendarId, datefrom, dateto, orgPeriodControl)) {
        List<String> toDelete = new ArrayList<String>();
        toDelete.add(transaction);
        results = delete(toDelete, table, client);
      } else {
        if (recordId != null && !"".equals(recordId)) {
          throw new OBException("@PeriodClosedForUnPosting@");
        }
      }
    }
    return results;
  }

  private static boolean checkDates(Set<Date> exceptionDates, String clientId, Set<String> orgIds,
      String documentCategory, String calendarId, String datefrom, String dateto,
      String orgPeriodControl) {
    List<Period> openPeriods = getOpenPeriods(clientId, documentCategory, orgIds, calendarId, "",
        "", datefrom, dateto, orgPeriodControl);
    int validDates = 0;
    for (Period period : openPeriods) {
      for (Date date : exceptionDates) {
        if (date.compareTo(period.getStartingDate()) >= 0
            && date.compareTo(period.getEndingDate()) <= 0) {
          validDates++;
        }
      }
    }
    return exceptionDates.size() == validDates;
  }

  private static boolean hasProcessingColumn(String strTableId) {
    int count = 0;
    String hql = " select count(*) from ADColumn where table.id = '" + strTableId + "' "
        + " and lower(dBColumnName) = 'processing'";
    Query query = OBDal.getInstance().getSession().createQuery(hql);
    count = ((Long) query.list().get(0)).intValue();
    return (count == 1);
  }
}
