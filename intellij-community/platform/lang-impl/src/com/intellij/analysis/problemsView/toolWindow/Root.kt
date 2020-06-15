// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.analysis.problemsView.toolWindow

import com.intellij.ide.projectView.PresentationData
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.tree.LeafState
import com.intellij.ui.tree.TreeVisitor.ByTreePath
import com.intellij.util.ui.tree.TreeUtil.promiseExpand

internal open class Root(val panel: ProblemsViewPanel, private val filter: ProblemFilter? = null)
  : Node(panel.project), Disposable {

  private val allProblems = mutableMapOf<VirtualFile, FileProblems>()

  override fun dispose() = Unit

  override fun getLeafState() = LeafState.NEVER

  override fun getName() = panel.displayName

  override fun update(project: Project, presentation: PresentationData) = Unit

  override fun getChildren(): Collection<Node> = synchronized(allProblems) {
    allProblems.values
      .filter { it.count(filter) > 0 }
      .map { it.getFileNode(getParentNode(it.file)) }
  }

  open fun getChildren(file: VirtualFile): Collection<Node> = synchronized(allProblems) {
    allProblems[file]?.getProblemNodes(filter) ?: emptyList()
  }

  open fun getProblemsCount(): Int = synchronized(allProblems) {
    allProblems.values.sumBy { it.count(filter) }
  }

  open fun getProblemsCount(file: VirtualFile): Int = synchronized(allProblems) {
    allProblems[file]?.count(filter) ?: 0
  }

  open fun addProblems(file: VirtualFile, vararg problems: Problem) {
    val exist = synchronized(allProblems) { allProblems.contains(file) }
    val node = add(file, problems) ?: return
    onValidThread {
      panel.updateToolWindowContent()
      panel.treeModel.structureChanged(node)
      if (!exist) synchronized(allProblems) {
        allProblems[file]?.getFileNode(getParentNode(file))
      }?.let {
        promiseExpand(panel.tree, ByTreePath(it.getPath()) { any: Any? -> any })
      }
    }
  }

  open fun removeProblems(file: VirtualFile, vararg problems: Problem) {
    val node = remove(file, problems) ?: return
    onValidThread {
      panel.updateToolWindowContent()
      panel.treeModel.structureChanged(node)
    }
  }

  open fun updateProblem(file: VirtualFile, problem: Problem) {
    val node = findProblemNode(file, problem) ?: return
    onValidThread {
      if (node.update()) panel.treeModel.nodeChanged(node)
    }
  }

  fun findProblemNode(file: VirtualFile, problem: Problem) = synchronized(allProblems) {
    allProblems[file]?.findProblemNode(problem)
  }

  private fun onValidThread(task: () -> Unit) {
    panel.treeModel.invoker.invoke {
      if (panel.treeModel.isRoot(this)) task()
    }
  }

  private fun add(file: VirtualFile, problems: Array<out Problem>): Node? = synchronized(allProblems) {
    val fileProblems = allProblems.computeIfAbsent(file) { FileProblems(it) }
    val count = fileProblems.count()
    problems.forEach { fileProblems.add(it) }
    if (count == fileProblems.count()) return null
    val parent = getParentNode(file)
    if (count > 0) return fileProblems.getFileNode(parent)
    return parent
  }

  private fun remove(file: VirtualFile, problems: Array<out Problem>): Node? = synchronized(allProblems) {
    val fileProblems = allProblems[file] ?: return null
    val count = fileProblems.count()
    problems.forEach { fileProblems.remove(it) }
    if (count == fileProblems.count()) return null
    val parent = getParentNode(file)
    if (fileProblems.count() > 0) return fileProblems.getFileNode(parent)
    allProblems.remove(file)
    return parent
  }

  @Suppress("UNUSED_PARAMETER") // TODO: support file hierarchy
  private fun getParentNode(file: VirtualFile): Node = this
}
