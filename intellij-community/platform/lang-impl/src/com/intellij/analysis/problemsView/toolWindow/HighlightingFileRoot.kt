// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.analysis.problemsView.toolWindow

import com.intellij.lang.annotation.HighlightSeverity.INFORMATION
import com.intellij.openapi.editor.ex.RangeHighlighterEx
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile

internal class HighlightingFileRoot(panel: ProblemsViewPanel, val file: VirtualFile) : Root(panel) {

  private val fileProblems = FileProblems(file)
  private val watcher = HighlightingWatcher(this, file, INFORMATION.myVal + 1)

  init {
    Disposer.register(this, watcher)
  }

  override fun getChildren(): Collection<Node> {
    return synchronized(fileProblems) { listOf(fileProblems.getFileNode(this)) }
  }

  override fun getChildren(file: VirtualFile): Collection<Node> {
    return synchronized(fileProblems) { fileProblems.getProblemNodes() }
  }

  fun findProblemNode(highlighter: RangeHighlighterEx): ProblemNode? {
    val problem = watcher.findProblem(highlighter) ?: return null
    return synchronized(fileProblems) { fileProblems.findProblemNode(problem) }
  }

  override fun getProblemsCount() = synchronized(fileProblems) { fileProblems.count() }

  override fun getProblemsCount(file: VirtualFile) = synchronized(fileProblems) { fileProblems.count() }

  override fun addProblems(file: VirtualFile, vararg problems: Problem) {
    synchronized(fileProblems) { problems.forEach { fileProblems.add(it) } }
    structureChanged()
  }

  override fun removeProblems(file: VirtualFile, vararg problems: Problem) {
    synchronized(fileProblems) { problems.forEach { fileProblems.remove(it) } }
    structureChanged()
  }

  override fun updateProblem(file: VirtualFile, problem: Problem) {
    synchronized(fileProblems) { fileProblems.findProblemNode(problem) }?.let { structureChanged() }
  }

  private fun structureChanged() {
    val model = panel.treeModel
    if (model.isRoot(this)) {
      model.structureChanged()
      panel.updateToolWindowContent()
    }
  }
}
