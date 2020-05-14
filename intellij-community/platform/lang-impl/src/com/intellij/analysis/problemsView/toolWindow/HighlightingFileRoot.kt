// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.analysis.problemsView.toolWindow

import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.ex.MarkupModelEx
import com.intellij.openapi.editor.ex.RangeHighlighterEx
import com.intellij.openapi.editor.impl.DocumentMarkupModel
import com.intellij.openapi.editor.impl.event.MarkupModelListener
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.tree.LeafState

internal class HighlightingFileRoot(panel: ProblemsViewPanel, val file: VirtualFile)
  : Root(panel), LeafState.Supplier, MarkupModelListener, Disposable {

  private val problems = FileProblems(file)

  init {
    refreshChildren()
  }

  override fun getChildren(): Collection<Node> {
    return synchronized(problems) { listOf(problems.getFileNode(this)) }
  }

  override fun getChildren(file: VirtualFile): Collection<Node> {
    return synchronized(problems) { problems.getProblemNodes() }
  }

  fun findProblemNode(info: HighlightInfo?): ProblemNode? {
    val problem = getProblem(info) ?: return null
    return synchronized(problems) { problems.findProblemNode(problem) }
  }

  private fun refreshChildren() {
    val document = ProblemsView.getDocument(project, file) ?: return
    val model = DocumentMarkupModel.forDocument(document, project, true) as? MarkupModelEx ?: return
    model.addMarkupModelListener(this, this)
    model.processRangeHighlightersOverlappingWith(0, document.textLength) { highlighter: RangeHighlighterEx ->
      afterAdded(highlighter)
      true
    }
  }

  override fun afterAdded(highlighter: RangeHighlighterEx) {
    val problem = getProblem(highlighter) ?: return
    synchronized(problems) { problems.add(problem) }
    structureChanged()
  }

  override fun beforeRemoved(highlighter: RangeHighlighterEx) {
    val problem = getProblem(highlighter) ?: return
    synchronized(problems) { problems.remove(problem) }
    structureChanged()
  }

  private fun structureChanged() {
    val model = panel.treeModel
    if (model.isRoot(this)) {
      model.structureChanged()
      panel.updateDisplayName()
    }
  }

  private fun getProblem(highlighter: RangeHighlighterEx): Problem? {
    val info = highlighter.errorStripeTooltip as? HighlightInfo ?: return null
    return getProblem(info)
  }

  private fun getProblem(info: HighlightInfo?): Problem? {
    return if (info?.description == null) null else HighlightingProblem(info)
  }

  override fun getProblemsCount() = synchronized(problems) { problems.count() }

  override fun getProblemsCount(file: VirtualFile, severity: Severity) = synchronized(problems) { problems.count(severity) }
}
