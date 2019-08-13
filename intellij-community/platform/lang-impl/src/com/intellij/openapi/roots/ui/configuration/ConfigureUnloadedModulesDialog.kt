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
package com.intellij.openapi.roots.ui.configuration

import com.intellij.CommonBundle
import com.intellij.icons.AllIcons
import com.intellij.ide.projectView.impl.ModuleGroup
import com.intellij.ide.projectView.impl.ModuleGroupingImplementation
import com.intellij.ide.projectView.impl.ModuleGroupingTreeHelper
import com.intellij.openapi.module.ModuleDescription
import com.intellij.openapi.module.ModuleGrouper
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.module.UnloadedModuleDescription
import com.intellij.openapi.module.impl.LoadedModuleDescriptionImpl
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectBundle
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.VerticalFlowLayout
import com.intellij.openapi.util.text.NaturalComparator
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.ui.ColoredTreeCellRenderer
import com.intellij.ui.DoubleClickListener
import com.intellij.ui.TreeSpeedSearch
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.graph.*
import com.intellij.util.ui.GridBag
import com.intellij.util.ui.tree.TreeUtil
import com.intellij.xml.util.XmlStringUtil
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.event.MouseEvent
import java.util.*
import javax.swing.*
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.MutableTreeNode
import javax.swing.tree.TreePath

/**
 * @author nik
 */
class ConfigureUnloadedModulesDialog(private val project: Project, selectedModuleName: String?) : DialogWrapper(project) {
  private val loadedModulesTree = ModuleDescriptionsTree(project)
  private val unloadedModulesTree = ModuleDescriptionsTree(project)
  private val moduleDescriptions = ModuleManager.getInstance(project).allModuleDescriptions.associateBy { it.name }
  private val statusLabel = JBLabel()
  /** graph contains an edge a -> b if b depends on a */
  private val dependentsGraph by lazy { buildGraph() }
  private val initiallyFocusedTree: ModuleDescriptionsTree

  init {
    title = ProjectBundle.message("module.load.unload.dialog.title")
    loadedModulesTree.fillTree(moduleDescriptions.values.filter { it is LoadedModuleDescriptionImpl })
    unloadedModulesTree.fillTree(moduleDescriptions.values.filter { it is UnloadedModuleDescription })
    if (selectedModuleName != null) {
      initiallyFocusedTree = if (moduleDescriptions[selectedModuleName] is UnloadedModuleDescription) unloadedModulesTree else loadedModulesTree
      initiallyFocusedTree.selectNodes(setOf(selectedModuleName))
    }
    else {
      initiallyFocusedTree = loadedModulesTree
    }
    init()
  }

  private fun buildGraph(): Graph<ModuleDescription> {
    return GraphGenerator.generate(CachingSemiGraph.cache(object: InboundSemiGraph<ModuleDescription> {
      override fun getNodes(): Collection<ModuleDescription> {
        return moduleDescriptions.values
      }

      override fun getIn(node: ModuleDescription): Iterator<ModuleDescription> {
        return node.dependencyModuleNames.asIterable().mapNotNull { moduleDescriptions[it] }.iterator()
      }
    }))
  }

  override fun createCenterPanel(): JComponent? {
    val buttonsPanel = JPanel(VerticalFlowLayout())
    val moveToUnloadedButton = JButton(ProjectBundle.message("module.unload.button.text"))
    val moveToLoadedButton = JButton(ProjectBundle.message("module.load.button.text"))
    val moveAllToUnloadedButton = JButton(ProjectBundle.message("module.unload.all.button.text"))
    val moveAllToLoadedButton = JButton(ProjectBundle.message("module.load.all.button.text"))
    moveToUnloadedButton.addActionListener {
      moveToUnloaded()
    }
    moveToLoadedButton.addActionListener {
      moveToLoaded()
    }
    moveAllToUnloadedButton.addActionListener {
      moveAllNodes(loadedModulesTree, unloadedModulesTree)
    }
    moveAllToLoadedButton.addActionListener {
      moveAllNodes(unloadedModulesTree, loadedModulesTree)
    }
    buttonsPanel.add(moveToUnloadedButton)
    buttonsPanel.add(moveToLoadedButton)
    buttonsPanel.add(moveAllToUnloadedButton)
    buttonsPanel.add(moveAllToLoadedButton)
    loadedModulesTree.installDoubleClickListener(this::moveToUnloaded)
    unloadedModulesTree.installDoubleClickListener(this::moveToLoaded)

    val mainPanel = JPanel(BorderLayout())
    val gridBag = GridBag().setDefaultWeightX(0, 0.5).setDefaultWeightX(1, 0.0).setDefaultWeightX(2, 0.5)
    val treesPanel = JPanel(GridBagLayout())
    treesPanel.add(JBLabel(ProjectBundle.message("module.loaded.label.text")), gridBag.nextLine().next().anchor(GridBagConstraints.WEST))
    treesPanel.add(JBLabel(ProjectBundle.message("module.unloaded.label.text")), gridBag.next().next().anchor(GridBagConstraints.WEST))

    treesPanel.add(JBScrollPane(loadedModulesTree.tree), gridBag.nextLine().next().weighty(1.0).fillCell())
    treesPanel.add(buttonsPanel, gridBag.next().anchor(GridBagConstraints.CENTER))
    treesPanel.add(JBScrollPane(unloadedModulesTree.tree), gridBag.next().weighty(1.0).fillCell())
    mainPanel.add(treesPanel, BorderLayout.CENTER)
    statusLabel.text = XmlStringUtil.wrapInHtml(ProjectBundle.message("module.unloaded.explanation"))
    mainPanel.add(statusLabel, BorderLayout.SOUTH)
    //current label text looks better when it's split on 2.5 lines, so set size of the whole component accordingly
    mainPanel.preferredSize = Dimension(Math.max(treesPanel.preferredSize.width, statusLabel.preferredSize.width*2/5), treesPanel.preferredSize.height)
    return mainPanel
  }

  private fun moveToLoaded() {
    val modulesToMove = includeMissingModules(unloadedModulesTree.getSelectedModules(), loadedModulesTree.getAllModules(),
                                              GraphAlgorithms.getInstance().invertEdgeDirections(dependentsGraph),
                                              ProjectBundle.message("module.load.dependencies.dialog.title"),
                                              { selectedSize, additionalSize, additionalFirst ->
                                                ProjectBundle.message("module.load.dependencies.dialog.text", selectedSize, additionalSize,
                                                                      additionalFirst)
                                              },
                                              ProjectBundle.message("module.load.with.dependencies.button.text"),
                                              ProjectBundle.message("module.load.without.dependencies.button.text"))
    moveModules(modulesToMove, unloadedModulesTree, loadedModulesTree)
  }

  private fun moveToUnloaded() {
    val modulesToMove = includeMissingModules(loadedModulesTree.getSelectedModules(), unloadedModulesTree.getAllModules(),
                                              dependentsGraph,
                                              ProjectBundle.message("module.unload.dependents.dialog.title"),
                                              { selectedSize, additionalSize, additionalFirst ->
                                                ProjectBundle.message("module.unload.dependents.dialog.text", selectedSize, additionalSize,
                                                                      additionalFirst)
                                              },
                                              ProjectBundle.message("module.unload.with.dependents.button.text"),
                                              ProjectBundle.message("module.unload.without.dependents.button.text"))
    moveModules(modulesToMove, loadedModulesTree, unloadedModulesTree)
  }

  private fun includeMissingModules(selected: List<ModuleDescription>, availableTargetModules: List<ModuleDescription>,
                                    dependenciesGraph: Graph<ModuleDescription>,
                                    dialogTitle: String, dialogMessage: (Int, Int, String) -> String, yesButtonText: String,
                                    noButtonText: String): Collection<ModuleDescription> {
    val additional = computeDependenciesToMove(selected, availableTargetModules, dependenciesGraph)
    if (additional.isNotEmpty()) {
      val answer = Messages.showYesNoCancelDialog(project, dialogMessage(selected.size, additional.size, additional.first().name),
                                                  dialogTitle, yesButtonText, noButtonText, CommonBundle.getCancelButtonText(), null)
      if (answer == Messages.YES) {
        return selected + additional
      }
      if (answer == Messages.CANCEL) {
        return emptyList()
      }
    }
    return selected
  }

  private fun computeDependenciesToMove(modulesToMove: Collection<ModuleDescription>, availableModules: Collection<ModuleDescription>,
                                        graph: Graph<ModuleDescription>): Set<ModuleDescription> {
    val result = LinkedHashSet<ModuleDescription>()
    for (module in modulesToMove) {
      GraphAlgorithms.getInstance().collectOutsRecursively(graph, module, result)
    }
    result.removeAll(modulesToMove)
    result.removeAll(availableModules)
    return result
  }

  private fun moveAllNodes(from: ModuleDescriptionsTree, to: ModuleDescriptionsTree) {
    from.removeAllNodes()
    to.fillTree(moduleDescriptions.values)
    IdeFocusManager.getInstance(project).requestFocus(to.tree, true).doWhenDone {
      to.tree.selectionPath = to.tree.getPathForRow(0)
    }
  }

  private fun moveModules(modulesToMove: Collection<ModuleDescription>, from: ModuleDescriptionsTree, to: ModuleDescriptionsTree) {
    if (modulesToMove.isEmpty()) return
    val oldSelectedRow = from.tree.selectionModel.leadSelectionRow
    from.removeModules(modulesToMove)
    val modules = to.addModules(modulesToMove)
    modules.firstOrNull()?.let { TreeUtil.selectNode(to.tree, it)}
    IdeFocusManager.getInstance(project).requestFocus(from.tree, true).doWhenDone {
      from.tree.selectionModel.selectionPath = from.tree.getPathForRow(oldSelectedRow.coerceAtMost(from.tree.rowCount-1))
    }
  }

  override fun getPreferredFocusedComponent(): JComponent? {
    return initiallyFocusedTree.tree
  }

  override fun doOKAction() {
    ModuleManager.getInstance(project).setUnloadedModules(unloadedModulesTree.getAllModules().map { it.name })
    super.doOKAction()
  }
}

private class ModuleDescriptionsTree(project: Project) {
  private val root = RootNode()
  private val model = DefaultTreeModel(root)
  private val helper = createModuleDescriptionHelper(project)
  internal val tree = Tree(model)

  init {
    tree.isRootVisible = false
    tree.showsRootHandles = true
    TreeSpeedSearch(tree, { treePath -> (treePath.lastPathComponent as? ModuleDescriptionTreeNode)?.text ?: "" }, true)
    tree.cellRenderer = ModuleDescriptionTreeRenderer()
  }

  fun getSelectedModules(): List<ModuleDescription> =
    tree.selectionPaths
        ?.mapNotNull { it.lastPathComponent }
        ?.filterIsInstance<ModuleDescriptionTreeNode>()
        ?.flatMap { getAllModulesUnder(it) }
        ?: emptyList<ModuleDescription>()

  fun getAllModules() = getAllModulesUnder(root)

  fun installDoubleClickListener(action: () -> Unit) {
    object : DoubleClickListener() {
      override fun onDoubleClick(event: MouseEvent): Boolean {
        if (tree.selectionPaths?.all { (it?.lastPathComponent as? ModuleDescriptionTreeNode)?.isLeaf == true } ?: false) {
          action()
          return true
        }
        return false
      }
    }.installOn(tree)
  }

  private fun getAllModulesUnder(node: ModuleDescriptionTreeNode): List<ModuleDescription> {
    val modules = ArrayList<ModuleDescription>()
    TreeUtil.traverse(node, { node ->
      if (node is ModuleDescriptionNode) {
        modules.add(node.moduleDescription)
      }
      return@traverse true
    })
    return modules
  }

  fun fillTree(modules: Collection<ModuleDescription>) {
    removeAllNodes()
    helper.createModuleNodes(modules, root, model)
    expandRoot()
  }

  private fun expandRoot() {
    tree.expandPath(TreePath(root))
  }

  fun addModules(modules: Collection<ModuleDescription>): List<ModuleDescriptionTreeNode> {
    return modules.map { helper.createModuleNode(it, root, model) }
  }

  fun removeModules(modules: Collection<ModuleDescription>) {
    val names = modules.mapTo(HashSet<String>()) { it.name }
    val toRemove = findNodes { it.moduleDescription.name in names }
    for (node in toRemove) {
      helper.removeNode(node, root, model)
    }
    expandRoot()
  }

  private fun findNodes(condition: (ModuleDescriptionNode) -> Boolean): List<ModuleDescriptionNode> {
    val result = ArrayList<ModuleDescriptionNode>()
    TreeUtil.traverse(root, { node ->
      if (node is ModuleDescriptionNode && condition(node)) {
        result.add(node)
      }
      return@traverse true
    })
    return result
  }

  fun removeAllNodes() {
    helper.removeAllNodes(root, model)
  }

  fun selectNodes(moduleNames: Set<String>) {
    val toSelect = findNodes { it.moduleDescription.name in moduleNames }
    val paths = toSelect.map { TreeUtil.getPath(root, it) }
    paths.forEach { tree.expandPath(it) }
    tree.selectionModel.selectionPaths = paths.toTypedArray()
    if (paths.isNotEmpty()) {
      TreeUtil.showRowCentered(tree, tree.getRowForPath(paths.first()), false, true)
    }
  }
}

private class ModuleDescriptionTreeRenderer : ColoredTreeCellRenderer() {
  override fun customizeCellRenderer(tree: JTree, value: Any?, selected: Boolean, expanded: Boolean, leaf: Boolean, row: Int, hasFocus: Boolean) {
    if (value is ModuleDescriptionTreeNode) {
      icon = value.icon
      append(value.text)
    }
  }
}

private fun createModuleDescriptionHelper(project: Project): ModuleGroupingTreeHelper<ModuleDescription, ModuleDescriptionTreeNode> {
  val moduleGrouper = ModuleGrouper.instanceFor(project)
  return ModuleGroupingTreeHelper.forEmptyTree(true, ModuleDescriptionGrouping(moduleGrouper),
                                               ::ModuleGroupNode, {ModuleDescriptionNode(it, moduleGrouper)}, nodeComparator)
}

private class ModuleDescriptionGrouping(private val moduleGrouper: ModuleGrouper) : ModuleGroupingImplementation<ModuleDescription> {
  override val compactGroupNodes: Boolean
    get() = moduleGrouper.compactGroupNodes

  override fun getGroupPath(m: ModuleDescription): List<String> {
    return moduleGrouper.getGroupPath(m)
  }

  override fun getModuleAsGroupPath(m: ModuleDescription): List<String>? {
    return moduleGrouper.getModuleAsGroupPath(m)
  }
}

private val nodeComparator = compareBy(NaturalComparator.INSTANCE) { node: ModuleDescriptionTreeNode -> node.text }
private interface ModuleDescriptionTreeNode : MutableTreeNode {
  val text: String
  val icon: Icon
  val group: ModuleGroup?
}

private class ModuleDescriptionNode(val moduleDescription: ModuleDescription, val moduleGrouper: ModuleGrouper) : DefaultMutableTreeNode(), ModuleDescriptionTreeNode {
  override val text: String
    get() = moduleGrouper.getShortenedNameByFullModuleName(moduleDescription.name, (parent as? ModuleDescriptionTreeNode)?.group?.qualifiedName)

  override val icon: Icon
    get() = AllIcons.Nodes.Module

  override val group: ModuleGroup?
    get() = moduleGrouper.getModuleAsGroupPath(moduleDescription)?.let {ModuleGroup(it)}
}

private class ModuleGroupNode(override val group: ModuleGroup) : DefaultMutableTreeNode(), ModuleDescriptionTreeNode {
  override val text: String
    get() {
      val parentGroupPath = (parent as? ModuleDescriptionTreeNode)?.group?.groupPathList
      if (parentGroupPath != null && ContainerUtil.startsWith(group.groupPathList, parentGroupPath)) {
        return group.groupPathList.drop(parentGroupPath.size).joinToString(".")
      }
      return group.groupPathList.last()
    }

  override val icon: Icon
    get() = AllIcons.Nodes.ModuleGroup
}

private class RootNode: DefaultMutableTreeNode(), ModuleDescriptionTreeNode {
  override val text: String
    get() = "<root>"

  override val icon: Icon
    get() = AllIcons.Nodes.ModuleGroup

  override val group: ModuleGroup?
    get() = null
}