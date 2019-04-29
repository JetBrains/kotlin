// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInspection.ex

import com.intellij.openapi.components.BaseState
import com.intellij.openapi.util.text.StringUtil
import com.intellij.profile.codeInspection.ui.inspectionsTree.InspectionConfigTreeNode
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.ui.tree.TreeUtil
import com.intellij.util.xmlb.annotations.Tag
import com.intellij.util.xmlb.annotations.XCollection
import java.util.*
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreePath

@Tag("profile-state")
internal class VisibleTreeState : BaseState() {
  @get:XCollection(elementName = "expanded", valueAttributeName = "path", propertyElementName = "expanded-state")
  var expandedNodes by property(TreeSet<State>())

  @get:XCollection(elementName = "selected", valueAttributeName = "path", propertyElementName = "selected-state")
  var selectedNodes by property(TreeSet<State>())

  fun expandNode(node: InspectionConfigTreeNode) {
    expandedNodes.add(getState(node))
  }

  fun restoreVisibleState(tree: Tree) {
    val pathsToExpand = ArrayList<TreePath>()
    val toSelect = ArrayList<TreePath>()
    traverseNodes(tree.model.root as DefaultMutableTreeNode, pathsToExpand, toSelect)
    TreeUtil.restoreExpandedPaths(tree, pathsToExpand)
    if (toSelect.isEmpty()) {
      TreeUtil.selectFirstNode(tree)
    }
    else {
      toSelect.forEach { TreeUtil.selectPath(tree, it) }
    }
  }

  private fun traverseNodes(root: DefaultMutableTreeNode, pathsToExpand: MutableList<TreePath>, toSelect: MutableList<TreePath>) {
    val state = getState(root as InspectionConfigTreeNode)
    val rootPath = root.getPath()
    if (selectedNodes.contains(state)) {
      toSelect.add(TreePath(rootPath))
    }
    if (expandedNodes.contains(state)) {
      pathsToExpand.add(TreePath(rootPath))
    }
    for (i in 0 until root.getChildCount()) {
      traverseNodes(root.getChildAt(i) as DefaultMutableTreeNode, pathsToExpand, toSelect)
    }
  }

  fun saveVisibleState(tree: Tree) {
    expandedNodes.clear()
    val rootNode = tree.model.root as DefaultMutableTreeNode
    val expanded = tree.getExpandedDescendants(TreePath(rootNode.path))
    if (expanded != null) {
      while (expanded.hasMoreElements()) {
        val treePath = expanded.nextElement()
        val node = treePath.lastPathComponent as InspectionConfigTreeNode
        expandedNodes.add(getState(node))
      }
    }

    setSelectionPaths(tree.selectionPaths)
    incrementModificationCount()
  }

  private fun getState(_node: InspectionConfigTreeNode): State {
    var node = _node
    val expandedNode: State
    if (node is InspectionConfigTreeNode.Tool) {
      expandedNode = State(node.key.toString())
    }
    else {
      val buf = StringBuilder()
      while (node.parent != null) {
        buf.append((node as InspectionConfigTreeNode.Group).groupName)
        node = node.getParent() as InspectionConfigTreeNode
      }
      expandedNode = State(buf.toString())
    }
    return expandedNode
  }

  fun setSelectionPaths(selectionPaths: Array<TreePath>?) {
    selectedNodes.clear()
    if (selectionPaths != null) {
      for (selectionPath in selectionPaths) {
        val node = selectionPath.lastPathComponent as InspectionConfigTreeNode
        selectedNodes.add(getState(node))
      }
    }
    incrementModificationCount()
  }

  internal class State @JvmOverloads constructor(key: String? = null): Comparable<State>, BaseState() {
    @get:Tag("id")
    var key by string()

    init {
      this.key = key
    }

    override fun compareTo(other: State) = StringUtil.compare(key, other.key, false)
  }
}
