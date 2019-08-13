/*
 * Copyright 2000-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 * Copyright 2000-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.ide.projectView.impl

import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleGrouper
import com.intellij.openapi.util.Pair
import com.intellij.util.containers.BidirectionalMap
import com.intellij.util.ui.tree.TreeUtil
import org.jetbrains.annotations.TestOnly
import java.util.*
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.MutableTreeNode
import javax.swing.tree.TreeNode

/**
 * Provides methods to build trees where nodes are grouped by modules (and optionally by module groups). Type parameter M specified class
 * of modules (may be [Module] if real modules are shown, or [com.intellij.openapi.module.ModuleDescription] if loaded and unloaded modules are shown.
 *
 * @author nik
 */
class ModuleGroupingTreeHelper<M: Any, N: MutableTreeNode> private constructor(
  private val groupingEnabled: Boolean,
  private val grouping: ModuleGroupingImplementation<M>,
  private val moduleGroupNodeFactory: (ModuleGroup) -> N,
  private val moduleNodeFactory: (M) -> N,
  private val nodeComparator: Comparator<in N>
) {
  private val nodeForGroup = HashMap<ModuleGroup, N>()
  /**
   * maps group which contains only one module/subgroup (and therefore doesn't have its own node) to that module/subgroup node;
   * for example if modules 'a.b.c.d.e' and 'a.b.c.d2' are grouped accordingly to their qualified names there will be the following entries in the map:
   * 'a' group to 'a.b.c' node, 'a.b' group to 'a.b.c' node and 'a.b.c.d' group to 'a.b.c.d.e' node
   */
  private val virtualGroupToChildNode = BidirectionalMap<ModuleGroup, N>()
  private val nodeData = HashMap<N, ModuleTreeNodeData<M>>()

  companion object {
    @JvmStatic
    fun <M: Any, N : MutableTreeNode> forEmptyTree(groupingEnabled: Boolean, grouping: ModuleGroupingImplementation<M>,
                                                   moduleGroupNodeFactory: (ModuleGroup) -> N, moduleNodeFactory: (M) -> N,
                                                   nodeComparator: Comparator<in N>): ModuleGroupingTreeHelper<M, N> =
      ModuleGroupingTreeHelper(groupingEnabled, grouping, moduleGroupNodeFactory, moduleNodeFactory, nodeComparator)

    @JvmStatic
    fun <M: Any, N : MutableTreeNode> forTree(rootNode: N, moduleGroupByNode: (N) -> ModuleGroup?, moduleByNode: (N) -> M?,
                                              groupingEnabled: Boolean, grouping: ModuleGroupingImplementation<M>,
                                              moduleGroupNodeFactory: (ModuleGroup) -> N, moduleNodeFactory: (M) -> N,
                                              nodeComparator: Comparator<in N>, nodeToBeMovedFilter: (N) -> Boolean): ModuleGroupingTreeHelper<M, N> {
      val helper = ModuleGroupingTreeHelper(groupingEnabled, grouping, moduleGroupNodeFactory, moduleNodeFactory, nodeComparator)
      TreeUtil.treeNodeTraverser(rootNode).forEach { node ->
        @Suppress("UNCHECKED_CAST")
        val group = moduleGroupByNode(node as N)
        val module = moduleByNode(node)
        if (group != null) {
          helper.nodeForGroup[group] = node
        }
        if (group != null || module != null) {
          helper.nodeData[node] = ModuleTreeNodeData(module, group)
        }
      }
      if (groupingEnabled && grouping.compactGroupNodes) {
        helper.nodeData.entries.forEach { (node, data) ->
          if (data.module != null) {
            val groups = grouping.getGroupPath(data.module)
            var lastNode = node
            for (end in groups.size downTo 1) {
              val parentGroup = ModuleGroup(groups.subList(0, end))
              val groupNode = helper.nodeForGroup[parentGroup]
              if (groupNode != null) {
                lastNode = groupNode
              }
              else if (!nodeToBeMovedFilter(lastNode) || parentGroup !in helper.virtualGroupToChildNode) {
                helper.virtualGroupToChildNode[parentGroup] = lastNode
              }
            }
          }
        }
      }
      return helper
    }

    @JvmStatic
    fun createDefaultGrouping(grouper: ModuleGrouper): ModuleGroupingImplementation<Module> = object : ModuleGroupingImplementation<Module> {
      override val compactGroupNodes: Boolean
        get() = grouper.compactGroupNodes

      override fun getGroupPath(m: Module) = grouper.getGroupPath(m)
      override fun getModuleAsGroupPath(m: Module) = grouper.getModuleAsGroupPath(m)
    }
  }

  fun createModuleNodes(modules: Collection<M>, rootNode: N, model: DefaultTreeModel): List<N> {
    val nodes = modules.map { createModuleNode(it, rootNode, model, true) }
    TreeUtil.sortRecursively(rootNode, nodeComparator)
    model.nodeStructureChanged(rootNode)
    return nodes
  }

  fun createModuleNode(module: M, rootNode: N, model: DefaultTreeModel): N {
    return createModuleNode(module, rootNode, model, false)
  }

  private fun createModuleNode(module: M, rootNode: N, model: DefaultTreeModel, bulkOperation: Boolean): N {
    val group = ModuleGroup(grouping.getGroupPath(module))
    val moduleNode = moduleNodeFactory(module)
    val parentNode = getOrCreateModuleGroupNode(group, rootNode, moduleNode, model, bulkOperation)
    insertModuleNode(moduleNode, parentNode, module, model, bulkOperation)
    return moduleNode
  }

  /**
   * If [bulkOperation] is true, no events will be fired and new node will be added into arbitrary place in the children list
   */
  private fun insertModuleNode(moduleNode: N, parentNode: N, module: M, model: DefaultTreeModel, bulkOperation: Boolean) {
    val moduleAsGroup = moduleAsGroup(module)
    if (moduleAsGroup != null) {
      val oldModuleGroupNode = nodeForGroup[moduleAsGroup]
      if (oldModuleGroupNode != null) {
        moveChildren(oldModuleGroupNode, moduleNode, model)
        model.removeNodeFromParent(oldModuleGroupNode)
        removeNodeData(oldModuleGroupNode)
      }
      val childNodeOfVirtualGroup = virtualGroupToChildNode.remove(moduleAsGroup)
      if (childNodeOfVirtualGroup != null) {
        detachNode(childNodeOfVirtualGroup, model, bulkOperation)
        insertNode(childNodeOfVirtualGroup, moduleNode, model, bulkOperation)
        convertVirtualGroupToRealNode(moduleAsGroup, childNodeOfVirtualGroup, moduleNode)
      }
      nodeForGroup[moduleAsGroup] = moduleNode
      nodeData[moduleNode] = ModuleTreeNodeData(module, moduleAsGroup)
    }
    else {
      nodeData[moduleNode] = ModuleTreeNodeData(module, null)
    }

    insertNode(moduleNode, parentNode, model, bulkOperation)
    compactMiddleGroupNodesWithSingleChild(parentNode, model, bulkOperation)
  }

  private fun moduleAsGroup(module: M) = grouping.getModuleAsGroupPath(module)?.let(::ModuleGroup)

  private fun moveChildren(fromNode: N, toNode: N, model: DefaultTreeModel) {
    val children = TreeUtil.listChildren(fromNode)
    moveChildren(children, toNode, model)
  }

  private fun moveChildren(children: List<TreeNode>, toNode: N, model: DefaultTreeModel) {
    TreeUtil.addChildrenTo(toNode, children)
    TreeUtil.sortChildren(toNode, nodeComparator)
    model.nodeStructureChanged(toNode)
  }

  /**
   * If [bulkOperation] is true, no events will be fired and new node will be added into arbitrary place in the children list
   */
  private fun getOrCreateModuleGroupNode(group: ModuleGroup, rootNode: N, childNode: N, model: DefaultTreeModel, bulkOperation: Boolean): N {
    val path = group.groupPathList
    if (!groupingEnabled || path.isEmpty()) return rootNode

    val existingNode = nodeForGroup[group]
    if (existingNode != null) return existingNode

    val parentGroup = ModuleGroup(path.subList(0, path.size - 1))

    val node: N
    if (!grouping.compactGroupNodes) {
      node = moduleGroupNodeFactory(group)
      val parentNode = getOrCreateModuleGroupNode(parentGroup, rootNode, node, model, bulkOperation)
      insertNode(node, parentNode, model, bulkOperation)
    }
    else {
      val nodeFromVirtualGroup = virtualGroupToChildNode.remove(group)
      if (nodeFromVirtualGroup == null) {
        virtualGroupToChildNode[group] = childNode
        return getOrCreateModuleGroupNode(parentGroup, rootNode, childNode, model, bulkOperation)
      }

      node = moduleGroupNodeFactory(group)
      val parentNode = findNearestGroupNode(parentGroup, rootNode)
      detachNode(nodeFromVirtualGroup, model, bulkOperation)
      insertNode(node, parentNode, model, bulkOperation)
      insertNode(nodeFromVirtualGroup, node, model, bulkOperation)
      convertVirtualGroupToRealNode(group, nodeFromVirtualGroup, node)
    }
    nodeForGroup[group] = node
    nodeData[node] = ModuleTreeNodeData<M>(null, group)
    return node
  }

  private fun findNearestGroupNode(group: ModuleGroup, rootNode: N): N {
    val pathList = group.groupPathList
    for (i in pathList.size downTo 1) {
      val node = nodeForGroup[ModuleGroup(pathList.subList(0, i))]
      if (node != null) return node
    }
    return rootNode
  }

  private fun insertNode(node: N, parentNode: N, model: DefaultTreeModel, bulkOperation: Boolean) {
    if (bulkOperation) {
      parentNode.insert(node, parentNode.childCount)
    }
    else {
      TreeUtil.insertNode(node, parentNode, model, nodeComparator)
    }
  }

  fun moveAllModuleNodesToProperGroups(rootNode: N, model: DefaultTreeModel) {
    val modules = nodeData.values.map { it.module }.filterNotNull()
    nodeData.keys.forEach { it.removeFromParent() }
    nodeData.clear()
    nodeForGroup.clear()
    virtualGroupToChildNode.clear()
    createModuleNodes(modules, rootNode, model)
  }

  fun moveModuleNodesToProperGroup(nodes: List<Pair<N, M>>, rootNode: N, model: DefaultTreeModel, tree: JTree) {
    nodes.forEach {
      moveModuleNodeToProperGroup(it.first, it.second, rootNode, model, tree)
    }
  }

  fun moveModuleNodeToProperGroup(node: N, module: M, rootNode: N, model: DefaultTreeModel, tree: JTree): N {
    val actualGroup = ModuleGroup(grouping.getGroupPath(module))
    val parent = node.parent
    val nodeAsGroup = nodeData[node]?.group
    val expectedParent = if (groupingEnabled && !actualGroup.groupPathList.isEmpty()) {
      nodeForGroup[actualGroup] ?: if (virtualGroupToChildNode[actualGroup] == node) parent else null
    } else rootNode
    if (expectedParent == parent && nodeAsGroup == moduleAsGroup(module)) {
      return node
    }

    val selectionPath = tree.selectionPath
    val wasSelected = selectionPath?.lastPathComponent == node

    removeNode(node, rootNode, model)

    val newNode = moduleNodeFactory(module)
    val newParent = getOrCreateModuleGroupNode(actualGroup, rootNode, newNode, model, false)
    insertModuleNode(newNode, newParent, module, model, false)

    if (wasSelected) {
      tree.expandPath(TreeUtil.getPath(rootNode, newParent))
      tree.selectionPath = TreeUtil.getPath(rootNode, newNode)
    }
    return newNode
  }

  private fun detachNode(node: N, model: DefaultTreeModel, bulkOperation: Boolean) {
    if (bulkOperation) {
      node.removeFromParent()
    }
    else {
      model.removeNodeFromParent(node)
    }
  }

  fun removeNode(node: N, rootNode: N, model: DefaultTreeModel, bulkOperation: Boolean = false) {
    val parent = node.parent
    val nodeAsGroup = nodeData[node]?.group
    detachNode(node, model, bulkOperation)
    removeNodeData(node)
    if (nodeAsGroup != null) {
      val childrenToKeep = TreeUtil.listChildren(node).filter { it in nodeData }
      if (childrenToKeep.isNotEmpty()) {
        childrenToKeep.forEach {
          @Suppress("UNCHECKED_CAST")
          val moduleNode = it as N
          val newGroupNode = getOrCreateModuleGroupNode(nodeAsGroup, rootNode, moduleNode, model, bulkOperation)
          detachNode(moduleNode, model, bulkOperation)
          insertNode(moduleNode, newGroupNode, model, bulkOperation)
        }
      }
    }

    removeEmptySyntheticModuleGroupNodes(parent, model, bulkOperation)
    compactMiddleGroupNodesWithSingleChild(parent, model, bulkOperation)
  }

  private fun removeEmptySyntheticModuleGroupNodes(parentNode: TreeNode?, model: DefaultTreeModel, bulkOperation: Boolean) {
    var parent = parentNode
    while (parent is MutableTreeNode && parent in nodeData && nodeData[parent]?.module == null && parent.childCount == 0) {
      val grandParent = parent.parent
      @Suppress("UNCHECKED_CAST")
      detachNode(parent as N, model, bulkOperation)
      removeNodeData(parent)
      parent = grandParent
    }
  }

  @Suppress("UNCHECKED_CAST")
  private fun compactMiddleGroupNodesWithSingleChild(parentNode: TreeNode?, model: DefaultTreeModel, bulkOperation: Boolean) {
    if (!grouping.compactGroupNodes) return

    var parent = parentNode
    while (parent is MutableTreeNode && parent in nodeData && nodeData[parent]?.module == null && parent.childCount == 1) {
      val grandParent = parent.parent
      val singleChild = parent.children().nextElement() as N
      detachNode(parent as N, model, bulkOperation)
      insertNode(singleChild, grandParent as N, model, bulkOperation)
      val group = nodeData[parent]?.group
      removeNodeData(parent)
      if (group != null) {
        virtualGroupToChildNode[group] = singleChild
      }
      parent = grandParent
    }
  }

  private fun removeNodeData(node: N) {
    val group = nodeData.remove(node)?.group
    if (group != null) {
      nodeForGroup.remove(group)
      virtualGroupToChildNode.remove(group)
      virtualGroupToChildNode.removeValue(node)
    }
  }

  private fun convertVirtualGroupToRealNode(group: ModuleGroup, oldChildNode: N, newGroupNode: N) {
    val parentGroups = virtualGroupToChildNode.getKeysByValue(oldChildNode)?.filter { it.groupPathList.size < group.groupPath.size }
    parentGroups?.forEach {
      virtualGroupToChildNode[it] = newGroupNode
    }
  }

  fun removeAllNodes(root: DefaultMutableTreeNode, model: DefaultTreeModel) {
    nodeData.clear()
    nodeForGroup.clear()
    virtualGroupToChildNode.clear()
    root.removeAllChildren()
    model.nodeStructureChanged(root)
  }

  @TestOnly
  fun getNodeForGroupMap(): MutableMap<ModuleGroup, N>? = Collections.unmodifiableMap(nodeForGroup)

  @TestOnly
  fun getVirtualGroupToChildNodeMap(): MutableMap<ModuleGroup, N>? = Collections.unmodifiableMap(virtualGroupToChildNode)

  @TestOnly
  fun getModuleByNodeMap(): Map<N, M?> = nodeData.mapValues { it.value.module }.filterValues { it != null }

  @TestOnly
  fun getGroupByNodeMap(): Map<N, ModuleGroup?> = nodeData.mapValues { it.value.group }.filterValues { it != null }

  @TestOnly
  fun isGroupingEnabled(): Boolean = groupingEnabled
}

private class ModuleTreeNodeData<M>(val module: M?, val group: ModuleGroup?)

interface ModuleGroupingImplementation<M: Any> {
  fun getGroupPath(m: M): List<String>
  fun getModuleAsGroupPath(m: M): List<String>?
  val compactGroupNodes: Boolean
}