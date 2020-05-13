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

internal class HighlightingFileRoot(panel: ProblemsViewPanel, file: VirtualFile)
  : Root(panel), LeafState.Supplier, MarkupModelListener, Disposable {

  private val myProblems = FileProblems()
  private val myFileNode = FileNode(this, file)

  init {
    refreshChildren()
  }

  val file: VirtualFile
    get() = myFileNode.file

  override fun getChildren(): Collection<Node> {
    return listOf(myFileNode)
  }

  override fun getChildren(file: VirtualFile): Collection<Node> {
    return synchronized(myProblems) { myProblems.getNodes(myFileNode).toList() }
  }

  fun findProblemNode(info: HighlightInfo?): ProblemNode? {
    val problem = getProblem(info) ?: return null
    return synchronized(myProblems) { myProblems.findNode(problem) }
  }

  private fun refreshChildren() {
    val document = ProblemsView.getDocument(project, myFileNode.file) ?: return
    val model = DocumentMarkupModel.forDocument(document, project, true) as? MarkupModelEx ?: return
    model.addMarkupModelListener(this, this)
    model.processRangeHighlightersOverlappingWith(0, document.textLength) { highlighter: RangeHighlighterEx ->
      afterAdded(highlighter)
      true
    }
  }

  override fun afterAdded(highlighter: RangeHighlighterEx) {
    val problem = getProblem(highlighter) ?: return
    synchronized(myProblems) { myProblems.add(problem) }
    structureChanged()
  }

  override fun beforeRemoved(highlighter: RangeHighlighterEx) {
    val problem = getProblem(highlighter) ?: return
    synchronized(myProblems) { myProblems.remove(problem) }
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
    return if (info == null || null == info.description) null else HighlightingProblem(info)
  }

  override fun getProblemsCount() = synchronized(myProblems) { myProblems.count() }

  override fun getProblemsCount(file: VirtualFile, severity: Severity) = synchronized(myProblems) { myProblems.count(severity) }
}
