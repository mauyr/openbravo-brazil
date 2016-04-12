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
 * All portions are Copyright (C) 2012 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.erpCommon.utility;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.core.SessionHandler;
import org.openbravo.model.ad.utility.Tree;
import org.openbravo.model.ad.utility.TreeNode;

public class TreeUtility {
  private static final Logger log4j = Logger.getLogger(TreeUtility.class);

  private Map<String, Set<String>> childTrees = new HashMap<String, Set<String>>();
  private Map<String, Set<String>> naturalTrees = new HashMap<String, Set<String>>();

  /**
   * Gets Natural tree for the given node
   */
  public Set<String> getNaturalTree(String nodeId, String treeType) {
    initialize(treeType);
    Set<String> result;
    if (naturalTrees.get(nodeId) == null) {
      result = new HashSet<String>();
      result.add(nodeId);
    } else {
      result = new HashSet<String>(naturalTrees.get(nodeId));
    }
    log4j.debug("Natural Tree(" + treeType + ") for the node" + nodeId + ":" + result.toString());
    return result;
  }

  /**
   * Gets the Child tree for the given node, including optionally given node
   */
  public Set<String> getChildTree(String nodeId, String treeType, boolean includeNode) {
    initialize(treeType);
    Set<String> childNode = this.getChildNode(nodeId, treeType);
    Set<String> result = new HashSet<String>();

    if (includeNode)
      result.add(nodeId);

    while (!childNode.isEmpty()) {
      for (String co : childNode) {
        result.add(co);
        childNode = this.getChildTree(co, treeType, false);
        result.addAll(childNode);
      }
    }
    return result;
  }

  /**
   * Gets Child node in the tree
   */
  public Set<String> getChildNode(String nodeId, String treeType) {
    initialize(treeType);
    if (childTrees.get(nodeId) == null) {
      return new HashSet<String>();
    } else {
      return new HashSet<String>(childTrees.get(nodeId));
    }
  }

  private void initialize(String treeType) {

    final String clientId = OBContext.getOBContext().getCurrentClient().getId();
    final String qryStr = "select t from " + Tree.class.getName() + " t where treetype='"
        + treeType + "' and client.id='" + clientId + "'";
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

    final List<Node> nodes = new ArrayList<Node>(treeNodes.size());
    for (final TreeNode tn : treeNodes) {
      final Node on = new Node();
      on.setTreeNode(tn);
      nodes.add(on);
    }

    for (final Node on : nodes) {
      on.resolve(nodes);
    }

    for (final Node on : nodes) {
      naturalTrees.put(on.getTreeNode().getNode(), on.getNaturalTree());
      if (on.getChildren() != null) {
        Set<String> os = new HashSet<String>();
        for (Node o : on.getChildren())
          os.add(o.getTreeNode().getNode());
        childTrees.put(on.getTreeNode().getNode(), os);
      }
    }
  }
}

class Node {

  private TreeNode treeNode;
  private Node parent;
  private List<Node> children = new ArrayList<Node>();

  private Set<String> naturalTreeParent = null;
  private Set<String> naturalTreeChildren = null;
  private Set<String> naturalTree = null;

  void addChild(Node child) {
    children.add(child);
  }

  public void resolve(List<Node> nodes) {
    if (treeNode.getReportSet() == null) {
      return;
    }
    for (final Node on : nodes) {
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
      for (final Node child : getChildren()) {
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
      for (final Node child : getChildren()) {
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

  public Node getParent() {
    return parent;
  }

  public void setParent(Node parent) {
    this.parent = parent;
  }

  public List<Node> getChildren() {
    return children;
  }

  public void setChildren(List<Node> children) {
    this.children = children;
  }
}
