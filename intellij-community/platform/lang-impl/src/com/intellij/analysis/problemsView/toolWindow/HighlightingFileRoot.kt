// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.analysis.problemsView.toolWindow

import com.intellij.lang.annotation.HighlightSeverity.INFORMATION
import com.intellij.openapi.editor.ex.RangeHighlighterEx
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile

internal class HighlightingFileRoot(panel: ProblemsViewPanel, val file: VirtualFile) : Root(panel) {

  private val problems = FileProblems(file)
  private val watcher = HighlightingWatcher(this, file, INFORMATION.myVal + 1)

  init {
    Disposer.register(this, watcher)
  }

  override fun getChildren(): Collection<Node> {
    return synchronized(problems) { listOf(problems.getFileNode(this)) }
  }

  override fun getChildren(file: VirtualFile): Collection<Node> {
    return synchronized(problems) { problems.getProblemNodes() }
  }

  fun findProblemNode(highlighter: RangeHighlighterEx): ProblemNode? {
    val problem = watcher.findProblem(highlighter) ?: return null
    return synchronized(problems) { problems.findProblemNode(problem) }
  }

  override fun getProblemsCount() = synchronized(problems) { problems.count() }

  override fun getProblemsCount(file: VirtualFile) = synchronized(problems) { problems.count() }

  override fun addProblem(file: VirtualFile, problem: Problem) {
    synchronized(problems) { problems.add(problem) }
    structureChanged()
  }

  override fun removeProblem(file: VirtualFile, problem: Problem) {
    synchronized(problems) { problems.remove(problem) }
    structureChanged()
  }

  override fun updateProblem(file: VirtualFile, problem: Problem) {
    synchronized(problems) { problems.findProblemNode(problem) }?.let { structureChanged() }
  }

  override fun updateProblems(file: VirtualFile, collection: Collection<Problem>) {
    synchronized(problems) { problems.update(collection) }
    structureChanged()
  }

  private fun structureChanged() {
    val model = panel.treeModel
    if (model.isRoot(this)) {
      model.structureChanged()
      panel.updateToolWindowContent()
    }
  }
}
