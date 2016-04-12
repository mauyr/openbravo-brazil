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
 * All portions are Copyright (C) 2013-2015 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.service.datasource;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.client.application.ApplicationUtils;
import org.openbravo.client.kernel.KernelUtils;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.model.ad.datamodel.Table;
import org.openbravo.model.ad.domain.ReferencedTree;
import org.openbravo.model.ad.domain.ReferencedTreeField;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.ad.ui.Tab;
import org.openbravo.model.ad.utility.TableTree;
import org.openbravo.model.ad.utility.Tree;
import org.openbravo.model.ad.utility.TreeNode;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.service.json.DataResolvingMode;
import org.openbravo.service.json.DataToJsonConverter;
import org.openbravo.service.json.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ADTreeDatasourceService extends TreeDatasourceService {
  private static final Logger logger = LoggerFactory.getLogger(ADTreeDatasourceService.class);
  private static final String AD_MENU_TABLE_ID = "116";
  private static final String AD_ORG_TABLE_ID = "155";

  @Override
  /**
   * Creates the treenode for the new node.
   * If the tree does not exist yet, it creates it too
   */
  protected void addNewNode(JSONObject bobProperties) {
    try {
      Client client = OBContext.getOBContext().getCurrentClient();
      Organization org = OBContext.getOBContext().getCurrentOrganization();
      String bobId = bobProperties.getString("id");
      String entityName = bobProperties.getString("_entity");
      Entity entity = ModelProvider.getInstance().getEntity(entityName);
      Table table = OBDal.getInstance().get(Table.class, entity.getTableId());
      TableTree tableTree = getTableTree(table);
      if (tableTree.isHandleNodesManually()) {
        return;
      }
      Tree adTree = getTree(table, bobProperties);
      if (adTree == null) {
        // The adTree does not exists, create it
        adTree = createTree(table, bobProperties);
      }
      // Adds the node to the adTree
      TreeNode adTreeNode = OBProvider.getInstance().get(TreeNode.class);
      adTreeNode.setClient(client);
      adTreeNode.setOrganization(org);
      adTreeNode.setTree(adTree);
      adTreeNode.setNode(bobId);
      adTreeNode.setSequenceNumber(100L);
      // Added as root node
      adTreeNode.setReportSet(ROOT_NODE_DB);
      OBDal.getInstance().save(adTreeNode);
    } catch (Exception e) {
      logger.error("Error while adding the tree node", e);
    }
  }

  @Override
  /**
   * Deletes the treenode and reparents its children
   */
  protected void deleteNode(JSONObject bobProperties) {
    try {
      String bobId = bobProperties.getString("id");
      String entityName = bobProperties.getString("_entity");
      Entity entity = ModelProvider.getInstance().getEntity(entityName);
      Table table = OBDal.getInstance().get(Table.class, entity.getTableId());
      TableTree tableTree = getTableTree(table);
      if (tableTree.isHandleNodesManually()) {
        return;
      }
      Tree tree = getTree(table, bobProperties);
      OBCriteria<TreeNode> adTreeNodeCriteria = OBDal.getInstance().createCriteria(TreeNode.class);
      adTreeNodeCriteria.setFilterOnActive(false);
      adTreeNodeCriteria.add(Restrictions.eq(TreeNode.PROPERTY_TREE, tree));
      adTreeNodeCriteria.add(Restrictions.eq(TreeNode.PROPERTY_NODE, bobId));
      TreeNode treeNode = (TreeNode) adTreeNodeCriteria.uniqueResult();
      int nChildrenMoved = reparentChildrenOfDeletedNode(tree, treeNode.getReportSet(),
          treeNode.getNode());
      logger.info(nChildrenMoved + " children have been moved to another parent");
      OBDal.getInstance().remove(treeNode);
    } catch (Exception e) {
      logger.error("Error while deleting tree node: ", e);
      throw new OBException("The treenode could not be created");
    }
  }

  /**
   * Obtains the ADTree TableTree associated with the table
   * 
   * @param table
   *          table whose ADTree TableTree will be returned
   * @return the ADTree TableTree associated with the given table
   */
  private TableTree getTableTree(Table table) {
    TableTree tableTree = null;
    OBCriteria<TableTree> criteria = OBDal.getInstance().createCriteria(TableTree.class);
    criteria.add(Restrictions.eq(TableTree.PROPERTY_TABLE, table));
    criteria.add(Restrictions.eq(TableTree.PROPERTY_TREESTRUCTURE, "ADTree"));
    // There can be at most one ADTree table per table, so it is safe to use uniqueResult
    tableTree = (TableTree) criteria.uniqueResult();
    return tableTree;
  }

  /**
   * In the given tree, reparents the children of deletedNodeId, change it to newParentId
   * 
   * @return The number of nodes that have been reparented
   */
  public int reparentChildrenOfDeletedNode(Tree tree, String newParentId, String deletedNodeId) {
    int nChildrenMoved = -1;
    try {
      ConnectionProvider conn = new DalConnectionProvider(false);
      nChildrenMoved = TreeDatasourceServiceData.reparentChildrenADTree(conn, newParentId,
          tree.getId(), deletedNodeId);
    } catch (ServletException e) {
      logger.error("Error while deleting tree node: ", e);
    }
    return nChildrenMoved;
  }

  /**
   * 
   * @param parentId
   *          id of the node whose children are to be retrieved
   * @param hqlWhereClause
   *          hql where clase of the tab/selector
   * @param hqlWhereClauseRootNodes
   *          hql where clause that define what nodes are roots
   * @return A JSONArray containing all the children of the given node
   * @throws JSONException
   * @throws TooManyTreeNodesException
   *           if the number of returned nodes were to be too high
   */
  @Override
  protected JSONArray fetchNodeChildren(Map<String, String> parameters,
      Map<String, Object> datasourceParameters, String parentId, String hqlWhereClause,
      String hqlWhereClauseRootNodes) throws JSONException, TooManyTreeNodesException {

    String tabId = parameters.get("tabId");
    String treeReferenceId = parameters.get("treeReferenceId");
    Tab tab = null;
    JSONArray selectedProperties = null;
    if (tabId != null) {
      tab = OBDal.getInstance().get(Tab.class, tabId);
      String selectedPropertiesStr = parameters.get("_selectedProperties");
      selectedProperties = new JSONArray(selectedPropertiesStr);
    } else if (treeReferenceId != null) {
      ReferencedTree treeReference = OBDal.getInstance().get(ReferencedTree.class, treeReferenceId);
      treeReference.getADReferencedTreeFieldList();
      selectedProperties = new JSONArray();
      for (ReferencedTreeField treeField : treeReference.getADReferencedTreeFieldList()) {
        selectedProperties.put(treeField.getProperty());
      }
    } else {
      logger
          .error("A request to the TreeDatasourceService must include the tabId or the treeReferenceId parameter");
      return new JSONArray();
    }
    Tree tree = (Tree) datasourceParameters.get("tree");

    JSONArray responseData = new JSONArray();
    Entity entity = ModelProvider.getInstance().getEntityByTableId(tree.getTable().getId());
    final DataToJsonConverter toJsonConverter = OBProvider.getInstance().get(
        DataToJsonConverter.class);

    // Joins the ADTreeNode with the referenced table
    StringBuilder joinClause = new StringBuilder();
    joinClause.append(" as tn ");
    joinClause.append(" , " + entity.getName() + " as e ");
    joinClause.append(" where tn.node = e.id ");
    if (hqlWhereClause != null) {
      joinClause.append(" and (" + hqlWhereClause + ")");
    }
    joinClause.append(" and tn.tree.id = '" + tree.getId() + "' ");
    if (hqlWhereClauseRootNodes == null && tab != null && tab.getTabLevel() > 0) {
      // Add the criteria to filter only the records that belong to the record selected in the
      // parent tab
      Tab parentTab = KernelUtils.getInstance().getParentTab(tab);
      String parentPropertyName = ApplicationUtils.getParentProperty(tab, parentTab);
      if (parentPropertyName != null) {
        JSONArray criteria = (JSONArray) JsonUtils.buildCriteria(parameters).get("criteria");
        String parentRecordId = getParentRecordIdFromCriteria(criteria, parentPropertyName);
        if (parentRecordId != null) {
          joinClause.append(" and e." + parentPropertyName + ".id = '" + parentRecordId + "' ");
        }
      }
    }
    if (hqlWhereClauseRootNodes != null) {
      joinClause.append(" and (" + hqlWhereClauseRootNodes + ") ");
    } else {
      if (ROOT_NODE_CLIENT.equals(parentId)) {
        if (AD_ORG_TABLE_ID.equals(tree.getTable().getId())) {
          // The ad_org table needs a special treatment, since is the only table tree that has an
          // actual node ('*' organization) with node_id = ROOT_NODE_DB
          // In this table the root nodes have the parent_id property set to null
          joinClause.append(" and tn.reportSet is null");
        } else {
          // Other ad_tree nodes can have either ROOT_NODE_DB or null as parent_id
          joinClause.append(" and (tn.reportSet = '" + ROOT_NODE_DB + "' or tn.reportSet is null)");
        }
      } else {
        joinClause.append(" and tn.reportSet = '" + parentId + "' ");
      }
    }
    joinClause.append(" order by tn.sequenceNumber ");

    // Selects the relevant properties from ADTreeNode and all the properties from the referenced
    // table
    String selectClause = " tn.id as treeNodeId, tn.reportSet as parentId, tn.sequenceNumber as seqNo, tn.node as nodeId, e as entity";
    OBQuery<BaseOBObject> obq = OBDal.getInstance()
        .createQuery("ADTreeNode", joinClause.toString());
    obq.setFilterOnActive(false);
    obq.setSelectClause(selectClause);
    obq.setFilterOnReadableOrganization(false);
    int nResults = obq.count();

    OBContext context = OBContext.getOBContext();
    int nMaxResults = -1;
    try {
      nMaxResults = Integer.parseInt(Preferences.getPreferenceValue("TreeDatasourceFetchLimit",
          false, context.getCurrentClient(), context.getCurrentOrganization(), context.getUser(),
          context.getRole(), null));
    } catch (Exception e) {
      nMaxResults = 1000;
    }
    if (nResults > nMaxResults) {
      throw new TooManyTreeNodesException();
    }

    boolean fetchRoot = ROOT_NODE_CLIENT.equals(parentId);

    int PARENT_ID = 1;
    int SEQNO = 2;
    int NODE_ID = 3;
    int ENTITY = 4;
    int cont = 0;
    ScrollableResults scrollNodes = obq.createQuery().scroll(ScrollMode.FORWARD_ONLY);
    try {
      while (scrollNodes.next()) {
        Object[] node = scrollNodes.get();
        JSONObject value = null;
        BaseOBObject bob = (BaseOBObject) node[ENTITY];
        try {
          value = toJsonConverter.toJsonObject(bob, DataResolvingMode.FULL);
          value.put("nodeId", bob.getId().toString());
          if (fetchRoot) {
            value.put("parentId", ROOT_NODE_CLIENT);
          } else {
            value.put("parentId", node[PARENT_ID]);
          }
          addNodeCommonAttributes(entity, bob, value);
          value.put("seqno", node[SEQNO]);
          value
              .put("_hasChildren", (this.nodeHasChildren(entity, (String) node[NODE_ID],
                  hqlWhereClause)) ? true : false);
        } catch (JSONException e) {
          logger.error("Error while constructing JSON reponse", e);
        }
        responseData.put(value);
        if ((cont % 100) == 0) {
          OBDal.getInstance().flush();
          OBDal.getInstance().getSession().clear();
        }
        cont++;
      }
    } finally {
      scrollNodes.close();
    }
    return responseData;
  }

  @Override
  /**
   * Check if a node has children
   * 
   * @param entity
   *          the entity the node belongs to
   * @param nodeId
   *          the id of the node to be checked
   * @param hqlWhereClause
   *          the where clause to be applied to the children
   * @return
   */
  protected boolean nodeHasChildren(Entity entity, String nodeId, String hqlWhereClause) {
    StringBuilder joinClause = new StringBuilder();
    joinClause.append(" as tn ");
    joinClause.append(" , " + entity.getName() + " as e ");
    joinClause.append(" where tn.node = e.id ");
    if (hqlWhereClause != null) {
      joinClause.append(" and (" + hqlWhereClause + ")");
    }
    joinClause.append(" and tn.reportSet = ? order by tn.sequenceNumber ");
    OBQuery<BaseOBObject> obq = OBDal.getInstance()
        .createQuery("ADTreeNode", joinClause.toString());
    obq.setFilterOnActive(false);
    final List<Object> parameters = new ArrayList<Object>();
    parameters.add(nodeId);
    obq.setParameters(parameters);
    return obq.count() > 0;
  }

  /**
   * Returns the sequence number of a node that has just been moved, and recompontes the sequence
   * number of its peers when needed
   * 
   * @param tree
   *          the ADTree being modified
   * @param prevNodeId
   *          id of the node that will be placed just before the updated node after it has been
   *          moved
   * @param nextNodeId
   *          id of the node that will be placed just after the updated node after it has been moved
   * @param newParentId
   *          id of the parent node of the node whose sequence number is being calculated
   * @return The sequence number of the node that has just been reparented
   * @throws Exception
   */
  private Long calculateSequenceNumberAndRecompute(Tree tree, String prevNodeId, String nextNodeId,
      String newParentId) throws Exception {
    Long seqNo = null;
    if (prevNodeId == null && nextNodeId == null) {
      // Only child, no need to recompute sequence numbers
      seqNo = 10L;
    } else if (nextNodeId == null) {
      // Last positioned child. Pick the highest sequence number of its brothers and add 10
      // No need to recompute sequence numbers
      OBCriteria<TreeNode> maxSeqNoCriteria = OBDal.getInstance().createCriteria(TreeNode.class);
      maxSeqNoCriteria.setFilterOnActive(false);
      maxSeqNoCriteria.add(Restrictions.eq(TreeNode.PROPERTY_TREE, tree));
      maxSeqNoCriteria.add(Restrictions.eq(TreeNode.PROPERTY_REPORTSET, newParentId));
      maxSeqNoCriteria.setProjection(Projections.max(TreeNode.PROPERTY_SEQUENCENUMBER));
      Long maxSeqNo = (Long) maxSeqNoCriteria.uniqueResult();
      seqNo = maxSeqNo + 10;
    } else {
      // Sequence numbers of the nodes that are positioned after the new one needs to be recomputed
      OBCriteria<TreeNode> nextNodeCriteria = OBDal.getInstance().createCriteria(TreeNode.class);
      nextNodeCriteria.setFilterOnActive(false);
      nextNodeCriteria.add(Restrictions.eq(TreeNode.PROPERTY_TREE, tree));
      nextNodeCriteria.add(Restrictions.eq(TreeNode.PROPERTY_NODE, nextNodeId));
      TreeNode nextNode = (TreeNode) nextNodeCriteria.uniqueResult();
      seqNo = nextNode.getSequenceNumber();
      recomputeSequenceNumbers(tree, newParentId, seqNo);
    }
    return seqNo;
  }

  /**
   * Adds 10 to the seqno of all the child nodes of newParentId, if they seqNo is equals or higher
   * than the provided seqNo For the ADMenu tree, it updates the seqno of all the nodes until the
   * first node associated with a menu entry that belong to a module not in developement is reached
   */
  private void recomputeSequenceNumbers(Tree tree, String newParentId, Long seqNo) {
    StringBuilder queryStr = new StringBuilder();
    queryStr.append(" UPDATE ad_treenode ");
    queryStr.append(" SET seqno = (seqno + 10) ");
    queryStr.append(" WHERE ad_tree_id = ? ");
    if (newParentId == null) {
      queryStr.append(" AND parent_id is null ");
    } else {
      queryStr.append(" AND parent_id = ? ");
    }
    queryStr.append(" AND seqno >= ? ");

    // Menu Tree, do not update the nodes that belong to windows not in development
    int seqNoOfFirstModNotInDev = -1;
    if (tree.getTable().getId().equals(AD_MENU_TABLE_ID)) {
      seqNoOfFirstModNotInDev = getSeqNoOfFirstModNotInDev(tree.getId(), newParentId, seqNo);
      if (seqNoOfFirstModNotInDev > 0) {
        queryStr.append(" AND seqno < ? ");
      }
    }

    ConnectionProvider conn = new DalConnectionProvider(false);
    PreparedStatement st = null;
    try {
      int nParam = 1;
      st = conn.getPreparedStatement(queryStr.toString());
      st.setString(nParam++, tree.getId());
      if (newParentId != null) {
        st.setString(nParam++, newParentId);
      }
      st.setLong(nParam++, seqNo);
      if (seqNoOfFirstModNotInDev > 0) {
        st.setLong(nParam++, seqNoOfFirstModNotInDev);
      }
      int nUpdated = st.executeUpdate();
      logger.debug("Recomputing sequence numbers: " + nUpdated + " nodes updated");
    } catch (Exception e) {
      logger.error("Exception while recomputing sequence numbers: ", e);
    } finally {
      try {
        conn.releasePreparedStatement(st);
      } catch (SQLException e) {
        logger.error("Error while releasing a prepared statement", e);
      }
    }
  }

  /**
   * Obtains the lower sequence number of the tree nodes that: belong to the treeId tree, are
   * children of the parentId node, their sequence number is higher or equals to seqNo, are
   * associated to a menu entry that belongs to a module not in development
   */
  private int getSeqNoOfFirstModNotInDev(String treeId, String parentId, Long seqNo) {
    StringBuilder queryStr = new StringBuilder();
    queryStr.append(" SELECT min(tn.seqno) ");
    queryStr.append(" FROM ad_treenode tn, ad_menu me, ad_module mo ");
    queryStr.append(" WHERE tn.node_id = me.ad_menu_id ");
    queryStr.append(" AND me.ad_module_id = mo.ad_module_id ");
    queryStr.append(" AND tn.ad_tree_id = ? ");
    queryStr.append(" AND tn.parent_id = ? ");
    queryStr.append(" AND tn.seqno >= ? ");
    queryStr.append(" AND mo.isindevelopment = 'N' ");
    ConnectionProvider conn = new DalConnectionProvider(false);
    PreparedStatement st = null;
    int seq = -1;
    try {
      st = conn.getPreparedStatement(queryStr.toString());
      st.setString(1, treeId);
      st.setString(2, parentId);
      st.setLong(3, seqNo);
      ResultSet rs = st.executeQuery();
      if (rs.next()) {
        seq = rs.getInt(1);
      }
    } catch (Exception e) {
      logger.error("Exception while recomputing sequence numbers: ", e);
    } finally {
      try {
        conn.releasePreparedStatement(st);
      } catch (SQLException e) {
        // Will not happen
      }
    }
    return seq;
  }

  /**
   * Checks if a tree is ordered
   */
  private boolean isOrdered(Tree tree) {
    Table table = tree.getTable();
    List<TableTree> tableTreeList = table.getADTableTreeList();
    if (tableTreeList.size() != 1) {
      return false;
    } else {
      TableTree tableTree = tableTreeList.get(0);
      return tableTree.isOrdered();
    }
  }

  /**
   * Returns a Tree given the referencedTableId and the parentRecordId
   */
  private Tree getTree(String referencedTableId) {
    Table referencedTable = OBDal.getInstance().get(Table.class, referencedTableId);

    OBCriteria<Tree> treeCriteria = OBDal.getInstance().createCriteria(Tree.class);
    treeCriteria.setFilterOnActive(false);
    treeCriteria.add(Restrictions.eq(Tree.PROPERTY_TABLE, referencedTable));
    treeCriteria.add(Restrictions.eq(Tree.PROPERTY_CLIENT, OBContext.getOBContext()
        .getCurrentClient()));
    return (Tree) treeCriteria.uniqueResult();
  }

  /**
   * Returns a Tree given the referencedTableId and a jsonobject that contains the node properties
   * This is called from the EventHandler, because the parentRecordId is not avaiable in the
   * parameters
   */
  private Tree getTree(Table table, JSONObject bobProperties) {
    Tree tree = null;
    OBCriteria<Tree> adTreeCriteria = OBDal.getInstance().createCriteria(Tree.class);
    adTreeCriteria.setFilterOnActive(false);
    adTreeCriteria.add(Restrictions.eq(Tree.PROPERTY_TABLE, table));
    tree = (Tree) adTreeCriteria.uniqueResult();
    return tree;
  }

  /**
   * Creates a new tree (record in ADTree)
   * 
   */
  private Tree createTree(Table table, JSONObject bobProperties) {
    Client client = OBContext.getOBContext().getCurrentClient();
    Organization org = OBContext.getOBContext().getCurrentOrganization();

    Tree adTree = OBProvider.getInstance().get(Tree.class);
    adTree.setClient(client);
    adTree.setOrganization(org);
    adTree.setAllNodes(true);
    adTree.setTypeArea(table.getName());
    adTree.setTable(table);
    String name = table.getName();
    adTree.setName(name);
    OBDal.getInstance().save(adTree);
    return adTree;
  }

  /**
   * Updates the parent of a given node a returns its definition in a JSONObject and recomputes the
   * sequence number of the nodes if the tree is ordered
   */
  protected JSONObject moveNode(Map<String, String> parameters, String nodeId, String newParentId,
      String prevNodeId, String nextNodeId) throws Exception {
    String tableId = null;
    String referencedTableId = parameters.get("referencedTableId");
    String treeReferenceId = parameters.get("treeReferenceId");
    JSONArray selectedProperties = null;
    if (referencedTableId != null) {
      tableId = referencedTableId;
      String selectedPropertiesStr = parameters.get("_selectedProperties");
      selectedProperties = new JSONArray(selectedPropertiesStr);
    } else if (treeReferenceId != null) {
      ReferencedTree treeReference = OBDal.getInstance().get(ReferencedTree.class, treeReferenceId);
      treeReference.getADReferencedTreeFieldList();
      tableId = treeReference.getTable().getId();
      selectedProperties = new JSONArray();
      for (ReferencedTreeField treeField : treeReference.getADReferencedTreeFieldList()) {
        selectedProperties.put(treeField.getProperty());
      }
    } else {
      logger
          .error("A request to the TreeDatasourceService must include the tabId or the treeReferenceId parameter");
      return new JSONObject();
    }

    String parentIdDB = newParentId;
    if (parentIdDB.equals(ROOT_NODE_CLIENT)) {
      // AD_ORG is special, root nodes have parentId = null, while the other in the trees root nodes
      // have parentId = '0'
      if (AD_ORG_TABLE_ID.equals(tableId)) {
        parentIdDB = null;
      } else {
        parentIdDB = ROOT_NODE_DB;
      }
    }

    Tree tree = this.getTree(tableId);
    boolean isOrdered = this.isOrdered(tree);
    Long seqNo = null;
    if (isOrdered) {
      seqNo = this.calculateSequenceNumberAndRecompute(tree, prevNodeId, nextNodeId, parentIdDB);
    }

    OBCriteria<TreeNode> treeNodeCriteria = OBDal.getInstance().createCriteria(TreeNode.class);
    treeNodeCriteria.setFilterOnActive(false);
    treeNodeCriteria.add(Restrictions.eq(TreeNode.PROPERTY_TREE, tree));
    treeNodeCriteria.add(Restrictions.eq(TreeNode.PROPERTY_NODE, nodeId));
    TreeNode treeNode = (TreeNode) treeNodeCriteria.uniqueResult();
    treeNode.setReportSet(parentIdDB);
    if (isOrdered) {
      treeNode.setSequenceNumber(seqNo);
    }

    OBDal.getInstance().flush(); // flush in admin mode
    return null;
  }

  @Override
  protected JSONObject getJSONObjectByNodeId(Map<String, String> parameters,
      Map<String, Object> datasourceParameters, String nodeId) throws MultipleParentsException {
    // In the ADTree structure, nodeId = recordId
    return this.getJSONObjectByRecordId(parameters, datasourceParameters, nodeId);
  }

  @Override
  protected JSONObject getJSONObjectByRecordId(Map<String, String> parameters,
      Map<String, Object> datasourceParameters, String bobId) {

    String tabId = parameters.get("tabId");
    String treeReferenceId = parameters.get("treeReferenceId");
    String hqlWhereClause = null;
    if (tabId != null) {
      Tab tab = OBDal.getInstance().get(Tab.class, tabId);
      hqlWhereClause = tab.getHqlwhereclause();
    } else if (treeReferenceId != null) {
      ReferencedTree treeReference = OBDal.getInstance().get(ReferencedTree.class, treeReferenceId);
      hqlWhereClause = treeReference.getHQLSQLWhereClause();
    } else {
      logger
          .error("A request to the TreeDatasourceService must include the tabId or the treeReferenceId parameter");
      return new JSONObject();
    }
    Tree tree = (Tree) datasourceParameters.get("tree");

    if (hqlWhereClause != null) {
      hqlWhereClause = this.substituteParameters(hqlWhereClause, parameters);
    }

    Entity entity = ModelProvider.getInstance().getEntityByTableId(tree.getTable().getId());

    final DataToJsonConverter toJsonConverter = OBProvider.getInstance().get(
        DataToJsonConverter.class);

    JSONObject json = null;
    try {
      OBCriteria<TreeNode> treeNodeCriteria = OBDal.getInstance().createCriteria(TreeNode.class);
      treeNodeCriteria.setFilterOnActive(false);
      treeNodeCriteria.add(Restrictions.eq(TreeNode.PROPERTY_TREE, tree));
      treeNodeCriteria.add(Restrictions.eq(TreeNode.PROPERTY_NODE, bobId));
      TreeNode treeNode = (TreeNode) treeNodeCriteria.uniqueResult();
      BaseOBObject bob = OBDal.getInstance().get(entity.getName(), treeNode.getNode());
      json = toJsonConverter.toJsonObject(bob, DataResolvingMode.FULL);
      json.put("nodeId", bobId);
      if (treeNode.getReportSet() == null) {
        json.put("parentId", ROOT_NODE_CLIENT);
      } else {
        json.put("parentId", treeNode.getReportSet());
      }

      addNodeCommonAttributes(entity, bob, json);
      json.put("_hasChildren", this.nodeHasChildren(entity, treeNode.getNode(), hqlWhereClause));
    } catch (Exception e) {
      logger.error("Error on tree datasource", e);
    }
    return json;
  }

  /**
   * Checks if the provided node complies with the hql where clause
   */
  protected boolean nodeConformsToWhereClause(TableTree tableTree, String nodeId,
      String hqlWhereClause) {

    Entity entity = ModelProvider.getInstance().getEntityByTableId(tableTree.getTable().getId());
    StringBuilder joinClause = new StringBuilder();
    joinClause.append(" as tn ");
    joinClause.append(" , " + entity.getName() + " as e ");
    joinClause.append(" where tn.node = e.id ");
    if (hqlWhereClause != null) {
      joinClause.append(" and (" + hqlWhereClause + ")");
    }
    joinClause.append(" and tn.node = ?");
    OBQuery<BaseOBObject> obq = OBDal.getInstance()
        .createQuery("ADTreeNode", joinClause.toString());
    obq.setFilterOnActive(false);

    final List<Object> parameters = new ArrayList<Object>();
    parameters.add(nodeId);
    obq.setParameters(parameters);

    return obq.count() > 0;
  }

  @Override
  protected JSONArray fetchFilteredNodesForTreesWithMultiParentNodes(
      Map<String, String> parameters, Map<String, Object> datasourceParameters,
      TableTree tableTree, List<String> filteredNodes, String hqlTreeWhereClause,
      String hqlTreeWhereClauseRootNodes, boolean allowNotApplyingWhereClauseToChildren)
      throws MultipleParentsException, TooManyTreeNodesException {
    // Not applicable, an ADTreeNode can only have one parent node
    return new JSONArray();
  }

  @Override
  protected Map<String, Object> getDatasourceSpecificParams(Map<String, String> parameters) {
    Map<String, Object> datasourceParams = new HashMap<String, Object>();
    String tabId = parameters.get("tabId");
    String treeReferenceId = parameters.get("treeReferenceId");
    String tableId = null;
    if (tabId != null) {
      Tab tab = OBDal.getInstance().get(Tab.class, tabId);
      tableId = tab.getTable().getId();
    } else if (treeReferenceId != null) {
      ReferencedTree treeReference = OBDal.getInstance().get(ReferencedTree.class, treeReferenceId);
      tableId = treeReference.getTable().getId();
    } else {
      logger
          .error("A request to the TreeDatasourceService must include the tabId or the treeReferenceId parameter");
      return datasourceParams;
    }
    Tree tree = this.getTree(tableId);
    datasourceParams.put("tree", tree);
    return datasourceParams;
  }

}
