// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.analysis.problemsView.toolWindow

import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.lang.annotation.HighlightSeverity.ERROR
import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.ex.MarkupModelEx
import com.intellij.openapi.editor.ex.RangeHighlighterEx
import com.intellij.openapi.editor.impl.DocumentMarkupModel.forDocument
import com.intellij.openapi.editor.impl.event.MarkupModelListener
import com.intellij.openapi.vfs.VirtualFile
import java.lang.ref.WeakReference

internal class HighlightingWatcher(
  private val root: Root,
  private val file: VirtualFile,
  private val level: Int = ERROR.myVal)
  : MarkupModelListener, Disposable {

  private var reference: WeakReference<MarkupModelEx>? = null

  init {
    update()
  }

  override fun dispose() = Unit

  override fun afterAdded(highlighter: RangeHighlighterEx) {
    getProblem(highlighter)?.let { root.addProblem(file, it) }
  }

  override fun beforeRemoved(highlighter: RangeHighlighterEx) {
    getProblem(highlighter)?.let { root.removeProblem(file, it) }
  }

  fun update() {
    val model = reference?.get() ?: getMarkupModel() ?: return
    val problems = mutableSetOf<Problem>()
    model.processRangeHighlightersOverlappingWith(0, model.document.textLength) { highlighter: RangeHighlighterEx ->
      getProblem(highlighter)?.let { problems += it }
      true
    }
    root.updateProblems(file, problems)
  }

  fun getProblem(info: HighlightInfo?): Problem? {
    return if (null == info?.description || info.severity.myVal < level) null else HighlightingProblem(info)
  }

  private fun getProblem(highlighter: RangeHighlighterEx): Problem? {
    val info = highlighter.errorStripeTooltip as? HighlightInfo ?: return null
    return getProblem(info)
  }

  private fun getMarkupModel(): MarkupModelEx? {
    val document = ProblemsView.getDocument(root.project, file) ?: return null
    val model = forDocument(document, root.project, true) as? MarkupModelEx ?: return null
    model.addMarkupModelListener(this, this)
    reference = WeakReference(model)
    return model
  }
}
