// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.analysis.problemsView.toolWindow

import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.lang.annotation.HighlightSeverity.ERROR
import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.ex.MarkupModelEx
import com.intellij.openapi.editor.ex.RangeHighlighterEx
import com.intellij.openapi.editor.impl.DocumentMarkupModel.forDocument
import com.intellij.openapi.editor.impl.event.MarkupModelListener
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import java.lang.ref.WeakReference

internal class HighlightingWatcher(
  private val root: Root,
  private val file: VirtualFile,
  private val level: Int = ERROR.myVal)
  : MarkupModelListener, Disposable {

  private val problems = mutableMapOf<RangeHighlighterEx, Problem>()
  private var reference: WeakReference<MarkupModelEx>? = null

  init {
    Disposer.register(root, this)
    update()
  }

  override fun dispose() = Unit

  override fun afterAdded(highlighter: RangeHighlighterEx) {
    getProblem(highlighter)?.let { root.addProblems(file, it) }
  }

  override fun beforeRemoved(highlighter: RangeHighlighterEx) {
    getProblem(highlighter)?.let { root.removeProblems(file, it) }
  }

  override fun attributesChanged(highlighter: RangeHighlighterEx, renderersChanged: Boolean, fontStyleOrColorChanged: Boolean) {
    findProblem(highlighter)?.let { root.updateProblem(file, it) }
  }

  fun update() {
    val model = reference?.get() ?: getMarkupModel() ?: return
    model.processRangeHighlightersOverlappingWith(0, model.document.textLength) { highlighter: RangeHighlighterEx ->
      afterAdded(highlighter)
      true
    }
  }

  fun getProblems(): Collection<Problem> = synchronized(problems) { problems.values }

  fun findProblem(highlighter: RangeHighlighterEx) = synchronized(problems) { problems[highlighter] }

  private fun getProblem(highlighter: RangeHighlighterEx) = when {
    !isValid(highlighter) -> null
    else -> synchronized(problems) {
      problems.computeIfAbsent(highlighter) { HighlightingProblem(highlighter) }
    }
  }

  private fun isValid(highlighter: RangeHighlighterEx): Boolean {
    val info = highlighter.errorStripeTooltip as? HighlightInfo ?: return false
    return info.description != null && info.severity.myVal >= level
  }

  private fun getMarkupModel(): MarkupModelEx? {
    val document = ProblemsView.getDocument(root.project, file) ?: return null
    val model = forDocument(document, root.project, true) as? MarkupModelEx ?: return null
    model.addMarkupModelListener(this, this)
    reference = WeakReference(model)
    return model
  }
}
