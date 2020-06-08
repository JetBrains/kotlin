// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.analysis.problemsView.toolWindow

import com.intellij.ide.projectView.PresentationData
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.tree.LeafState
import com.intellij.util.containers.ContainerUtil.mapNotNull

internal open class Root(val panel: ProblemsViewPanel) : Node(panel.project), Disposable {

  private val allProblems = mutableMapOf<VirtualFile, FileProblems>()

  override fun dispose() = Unit

  override fun getLeafState() = LeafState.NEVER

  override fun getName() = panel.displayName

  override fun update(project: Project, presentation: PresentationData) = Unit

  override fun getChildren(): Collection<Node> = synchronized(allProblems) {
    mapNotNull(allProblems.values) { it.getFileNode(getParentNode(it.file)) }
  }

  open fun getChildren(file: VirtualFile): Collection<Node> = synchronized(allProblems) {
    allProblems[file]?.getProblemNodes() ?: return emptyList()
  }

  open fun getProblemsCount(): Int = synchronized(allProblems) {
    allProblems.values.sumBy { it.count() }
  }

  open fun getProblemsCount(file: VirtualFile): Int = synchronized(allProblems) {
    allProblems[file]?.count() ?: 0
  }

  open fun addProblem(file: VirtualFile, problem: Problem) {
    synchronized(allProblems) { add(file, problem) }?.let { structureChanged(it) }
  }

  open fun removeProblem(file: VirtualFile, problem: Problem) {
    synchronized(allProblems) { remove(file, problem) }?.let { structureChanged(it) }
  }

  open fun updateProblem(file: VirtualFile, problem: Problem) {
    val node = synchronized(allProblems) { allProblems[file]?.findProblemNode(problem) } ?: return
    node.update()
    structureChanged(node)
  }

  open fun removeProblems(file: VirtualFile) {
    synchronized(allProblems) { removeAll(file) }?.let { structureChanged(it) }
  }

  open fun updateProblems(file: VirtualFile, collection: Collection<Problem>) {
    synchronized(allProblems) { update(file, collection) }?.let { structureChanged(it) }
  }

  private fun structureChanged(node: Node) {
    val model = panel.treeModel
    if (model.isRoot(this)) {
      model.structureChanged(node)
      panel.updateToolWindowContent()
    }
  }

  private fun add(file: VirtualFile, problem: Problem): Node? {
    val fileProblems = allProblems.computeIfAbsent(file) { FileProblems(it) }
    if (!fileProblems.add(problem)) return null
    val parent = getParentNode(file)
    if (fileProblems.count() > 1) return fileProblems.getFileNode(parent)
    return parent
  }

  private fun remove(file: VirtualFile, problem: Problem): Node? {
    val fileProblems = allProblems[file] ?: return null
    if (!fileProblems.remove(problem)) return null
    val parent = getParentNode(file)
    if (fileProblems.count() > 0) return fileProblems.getFileNode(parent)
    allProblems.remove(file)
    return parent
  }

  private fun update(file: VirtualFile, collection: Collection<Problem>): Node? {
    val fileProblems = allProblems[file] ?: return addAll(file, collection)
    if (collection.isEmpty()) return removeAll(file)
    fileProblems.update(collection)
    val parent = getParentNode(file)
    return fileProblems.getFileNode(parent)
  }

  private fun addAll(file: VirtualFile, collection: Collection<Problem>): Node? {
    if (collection.isEmpty()) return null
    val fileProblems = FileProblems(file)
    fileProblems.update(collection)
    allProblems[file] = fileProblems
    return getParentNode(file)
  }

  private fun removeAll(file: VirtualFile): Node? {
    val fileProblems = allProblems.remove(file) ?: return null
    fileProblems.clear()
    return getParentNode(file)
  }

  @Suppress("UNUSED_PARAMETER") // TODO: support file hierarchy
  private fun getParentNode(file: VirtualFile): Node = this
}
