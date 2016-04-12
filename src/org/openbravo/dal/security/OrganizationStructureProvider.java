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
 * All portions are Copyright (C) 2008-2014 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.dal.security;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Query;
import org.openbravo.base.provider.OBNotSingleton;
import org.openbravo.base.util.Check;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.core.SessionHandler;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.ad.utility.Tree;
import org.openbravo.model.ad.utility.TreeNode;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.OrganizationType;

/**
 * Builds a tree of organizations to compute the accessible organizations for the current
 * organizations of a user. Is used to check if references from one object to another are correct
 * from an organization structure perspective.
 * <p/>
 * For example a city refers to a country then: an organization of the country (the refered object)
 * must be in the natural tree of the organization of the city (the referee).
 * 
 * @author mtaal
 */

public class OrganizationStructureProvider implements OBNotSingleton {

  private boolean isInitialized = false;
  private Map<String, Set<String>> naturalTreesByOrgID = new HashMap<String, Set<String>>();
  private Map<String, String> parentByOrganizationID = new HashMap<String, String>();
  private Map<String, Set<String>> childByOrganizationID = new HashMap<String, Set<String>>();
  private static final String AD_ORG_TABLE_ID = "155";
  private String clientId;

  /**
   * Set initialized to false and recompute the organization structures
   */
  public void reInitialize() {
    isInitialized = false;
    initialize();
  }

  private synchronized void initialize() {
    if (isInitialized) {
      return;
    }

    if (getClientId() == null) {
      setClientId(OBContext.getOBContext().getCurrentClient().getId());
    }

    // read all trees of all clients, bypass DAL to prevent security checks
    final String qryStr = "select t from " + Tree.class.getName() + " t where table.id='"
        + AD_ORG_TABLE_ID + "' and client.id='" + getClientId() + "'";
    final Query qry = SessionHandler.getInstance().createQuery(qryStr);
    @SuppressWarnings("unchecked")
    final List<Tree> ts = qry.list();
    final List<TreeNode> treeNodes = new ArrayList<TreeNode>();
    for (final Tree t : ts) {
      final String nodeQryStr = "select tn from " + TreeNode.class.getName()
          + " tn where tn.tree.id='" + t.getId() + "'";
      final Query nodeQry = SessionHandler.getInstance().createQuery(nodeQryStr);
      @SuppressWarnings("unchecked")
      final List<TreeNode> tns = nodeQry.list();
      treeNodes.addAll(tns);
    }

    final List<OrgNode> orgNodes = new ArrayList<OrgNode>(treeNodes.size());
    for (final TreeNode tn : treeNodes) {
      final OrgNode on = new OrgNode();
      on.setTreeNode(tn);
      orgNodes.add(on);
    }

    for (final OrgNode on : orgNodes) {
      on.resolve(orgNodes);
    }

    for (final OrgNode on : orgNodes) {
      if (on.getParent() != null) {
        parentByOrganizationID.put(on.getTreeNode().getNode(), on.getParent().getTreeNode()
            .getNode());
      }
    }

    for (final OrgNode on : orgNodes) {
      naturalTreesByOrgID.put(on.getTreeNode().getNode(), on.getNaturalTree());
      if (on.getChildren() != null) {
        Set<String> os = new HashSet<String>();
        for (OrgNode o : on.getChildren())
          os.add(o.getTreeNode().getNode());
        childByOrganizationID.put(on.getTreeNode().getNode(), os);
      }
    }
    isInitialized = true;
  }

  /**
   * Returns the natural tree of an organization.
   * 
   * @param orgId
   *          the id of the organization for which the natural tree is determined.
   * @return the natural tree of the organization.
   */
  public Set<String> getNaturalTree(String orgId) {
    initialize();
    Set<String> result;
    if (naturalTreesByOrgID.get(orgId) == null) {
      result = new HashSet<String>();
      result.add(orgId);
    } else {
      result = new HashSet<String>(naturalTreesByOrgID.get(orgId));
    }
    return result;
  }

  /**
   * Checks if an organization (org2) is in the natural tree of another organization (org1).
   * 
   * @param org1
   *          the natural tree of this organization is used to check if org2 is present
   * @param org2
   *          the organization checked in the natural tree of org1
   * @return true if org2 is in the natural tree of org1, false otherwise
   */
  public boolean isInNaturalTree(Organization org1, Organization org2) {
    initialize();
    final String id1 = (String) DalUtil.getId(org1);
    final String id2 = (String) DalUtil.getId(org2);

    // org 0 is in everyones natural tree, and the other way around
    if (id2 != null && id2.equals("0")) {
      return true;
    }
    if (id1 != null && id1.equals("0")) {
      return true;
    }

    final Set<String> ids = getNaturalTree(id1);
    Check.isNotNull(ids, "Organization with id " + id1
        + " does not have a computed natural tree, does this organization exist?");
    return ids.contains(id2);
  }

  /**
   * Returns the parent organization tree of an organization.
   * 
   * @param orgId
   *          the id of the organization for which the parent organization tree is determined.
   * @param includeOrg
   *          if true, returns also the given organization as part of the tree
   * @return the parent organization tree of the organization.
   */
  public Set<String> getParentTree(String orgId, boolean includeOrg) {
    initialize();
    String parentOrg = this.getParentOrg(orgId);
    Set<String> result = new HashSet<String>();

    if (includeOrg) {
      result.add(orgId);
    }

    while (parentOrg != null) {
      result.add(parentOrg);
      parentOrg = this.getParentOrg(parentOrg);
    }
    return result;
  }

  /**
   * Returns an ordered list of parents of an organization. The parents are listed from the
   * organization and up (so parent before grand parent).
   * 
   * @param orgId
   *          the id of the organization for which the parent organization tree is determined.
   * @param includeOrg
   *          if true, returns also the given organization as part of the tree
   * @return the parent organization tree of the organization.
   */
  public List<String> getParentList(String orgId, boolean includeOrg) {
    initialize();
    String parentOrg = this.getParentOrg(orgId);
    List<String> result = new ArrayList<String>();

    if (includeOrg) {
      result.add(orgId);
    }

    while (parentOrg != null) {
      result.add(parentOrg);
      parentOrg = this.getParentOrg(parentOrg);
    }
    return result;
  }

  /**
   * Returns the parent organization of an organization.
   * 
   * @param orgId
   *          the id of the organization for which the parent organization is determined.
   * @return the parent organization.
   */
  public String getParentOrg(String orgId) {
    initialize();
    return parentByOrganizationID.get(orgId);
  }

  /**
   * Returns the parent organization of an organization.
   * 
   * @param org
   *          the organization for which the parent organization is determined.
   * @return the parent organization.
   */
  public Organization getParentOrg(Organization org) {
    initialize();
    return OBDal.getInstance().get(Organization.class, parentByOrganizationID.get(org.getId()));
  }

  /**
   * Returns the child organization tree of an organization.
   * 
   * @param orgId
   *          the id of the organization for which the child organization tree is determined.
   * @param includeOrg
   *          if true, returns also the given organization as part of the tree
   * @return the child organization tree of the organization.
   */
  public Set<String> getChildTree(String orgId, boolean includeOrg) {
    initialize();
    Set<String> childOrg = this.getChildOrg(orgId);
    Set<String> result = new HashSet<String>();

    if (includeOrg)
      result.add(orgId);

    while (!childOrg.isEmpty()) {
      for (String co : childOrg) {
        result.add(co);
        childOrg = this.getChildTree(co, false);
        result.addAll(childOrg);
      }

    }
    return result;
  }

  /**
   * Returns the child organizations of an organization.
   * 
   * @param orgId
   *          the id of the organization for which the child organizations are determined.
   * @return the child organizations
   */
  public Set<String> getChildOrg(String orgId) {
    initialize();
    if (childByOrganizationID.get(orgId) == null) {
      reInitialize();
    }
    if (childByOrganizationID.get(orgId) == null) {
      return new HashSet<String>();
    } else {
      return new HashSet<String>(childByOrganizationID.get(orgId));
    }
  }

  class OrgNode {

    private TreeNode treeNode;
    private OrgNode parent;
    private List<OrgNode> children = new ArrayList<OrgNode>();

    private Set<String> naturalTreeParent = null;
    private Set<String> naturalTreeChildren = null;
    private Set<String> naturalTree = null;

    void addChild(OrgNode child) {
      children.add(child);
    }

    public void resolve(List<OrgNode> nodes) {
      if (treeNode.getReportSet() == null) {
        return;
      }
      for (final OrgNode on : nodes) {
        if (on.getTreeNode().getNode().equals(treeNode.getReportSet())) {
          on.addChild(this);
          setParent(on);
          break;
        }
      }
    }

    public Set<String> getNaturalTree() {
      if (naturalTree == null) {
        naturalTree = new HashSet<String>();
        naturalTree.add(getTreeNode().getNode());
        if (getParent() != null) {
          getParent().getParentPath(naturalTree);
        }
        for (final OrgNode child : getChildren()) {
          child.getChildPath(naturalTree);
        }
      }
      return naturalTree;
    }

    public void getParentPath(Set<String> theNaturalTree) {
      if (naturalTreeParent == null) {
        naturalTreeParent = new HashSet<String>();
        naturalTreeParent.add(getTreeNode().getNode());
        if (getParent() != null) {
          getParent().getParentPath(naturalTreeParent);
        }
      }
      theNaturalTree.addAll(naturalTreeParent);
    }

    public void getChildPath(Set<String> theNaturalTree) {
      if (naturalTreeChildren == null) {
        naturalTreeChildren = new HashSet<String>();
        naturalTreeChildren.add(getTreeNode().getNode());
        for (final OrgNode child : getChildren()) {
          child.getChildPath(naturalTreeChildren);
        }
      }
      theNaturalTree.addAll(naturalTreeChildren);
    }

    public TreeNode getTreeNode() {
      return treeNode;
    }

    public void setTreeNode(TreeNode treeNode) {
      this.treeNode = treeNode;
    }

    public OrgNode getParent() {
      return parent;
    }

    public void setParent(OrgNode parent) {
      this.parent = parent;
    }

    public List<OrgNode> getChildren() {
      return children;
    }

    public void setChildren(List<OrgNode> children) {
      this.children = children;
    }
  }

  public String getClientId() {
    return clientId;
  }

  public void setClientId(String clientId) {
    this.clientId = clientId;
  }

  /*
   * Returns the legal entities of the client.
   */
  public List<Organization> getLegalEntitiesList() {
    StringBuffer where = new StringBuffer();
    where.append(" as org");
    where.append(" join org." + Organization.PROPERTY_ORGANIZATIONTYPE + " as orgType");
    where.append(" where org." + Organization.PROPERTY_CLIENT + ".id = :client");
    where.append("   and orgType." + OrganizationType.PROPERTY_LEGALENTITY + " = true");
    OBQuery<Organization> orgQry = OBDal.getInstance().createQuery(Organization.class,
        where.toString());
    orgQry.setFilterOnReadableClients(false);
    orgQry.setFilterOnReadableOrganization(false);
    orgQry.setNamedParameter("client", clientId);
    return orgQry.list();
  }

  /**
   * Returns the legal entity of the given organization
   * 
   * @param org
   *          organization to get its legal entity
   * @return legal entity (with or without accounting) organization or null if not found
   */
  public Organization getLegalEntity(final Organization org) {
    // Admin mode needed to get the Organization type.
    OBContext.setAdminMode(true);
    try {
      for (final String orgId : getParentList(org.getId(), true)) {
        final Organization parentOrg = OBDal.getInstance().get(Organization.class, orgId);
        if (parentOrg.getOrganizationType().isLegalEntity()) {
          return parentOrg;
        }
      }
      return null;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Returns the legal entity or Business Unit of the given organization
   * 
   * @param org
   *          organization to get its legal entity or business unit
   * @return legal entity (with or without accounting) organization or null if not found
   */
  public Organization getLegalEntityOrBusinessUnit(final Organization org) {
    // Admin mode needed to get the Organization type.
    OBContext.setAdminMode(true);
    try {
      for (final String orgId : getParentList(org.getId(), true)) {
        final Organization parentOrg = OBDal.getInstance().get(Organization.class, orgId);
        if (parentOrg.getOrganizationType().isLegalEntity()
            || parentOrg.getOrganizationType().isBusinessUnit()) {
          return parentOrg;
        }
      }
      return null;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Returns the organization that is period control allowed for the org Organization. If no
   * organization is found, it returns NULL.
   * 
   * @param org
   *          Organization to get its period control allowed organization.
   */
  public Organization getPeriodControlAllowedOrganization(final Organization org) {
    if (org.isAllowPeriodControl()) {
      return org;
    }
    for (final String orgId : getParentList(org.getId(), false)) {
      final Organization parentOrg = OBDal.getInstance().get(Organization.class, orgId);
      if (parentOrg.isAllowPeriodControl()) {
        return parentOrg;
      }
    }
    return null;
  }
}