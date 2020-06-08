// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.analysis.problemsView.toolWindow

import com.intellij.codeHighlighting.HighlightDisplayLevel
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInsight.intention.impl.ShowIntentionActionsHandler.chooseActionAndInvoke
import com.intellij.icons.AllIcons
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.RangeMarker
import com.intellij.openapi.editor.ex.RangeHighlighterEx
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import javax.swing.Icon

internal class HighlightingProblem(private val highlighter: RangeHighlighterEx) : Problem {

  private fun getIcon(level: HighlightDisplayLevel) = if (severity >= level.severity.myVal) level.icon else null

  private val info: HighlightInfo?
    get() = HighlightInfo.fromRangeHighlighter(highlighter)

  override val icon: Icon
    get() = HighlightDisplayLevel.find(info?.severity)?.icon
            ?: getIcon(HighlightDisplayLevel.ERROR)
            ?: getIcon(HighlightDisplayLevel.WARNING)
            ?: HighlightDisplayLevel.WEAK_WARNING.icon

  override val description: String
    get() = info?.description ?: "Invalid"

  override val severity: Int
    get() = info?.severity?.myVal ?: -1

  override val offset: Int
    get() = info?.actualStartOffset ?: -1

  override fun hashCode() = highlighter.hashCode()

  override fun equals(other: Any?) = other is HighlightingProblem && other.highlighter == highlighter

  override fun hasQuickFixActions(): Boolean {
    val markers = info?.quickFixActionMarkers ?: return false
    return markers.any { it.second.isValid }
  }

  override fun getQuickFixActions(): Collection<AnAction> {
    val markers = info?.quickFixActionMarkers ?: return emptyList()
    return markers.filter { it.second.isValid }.map { QuickFixAction(it.first.action, it.second) }
  }
}

private class QuickFixAction(val action: IntentionAction, val marker: RangeMarker) : AnAction(AllIcons.Actions.IntentionBulb) {

  override fun update(event: AnActionEvent) {
    event.presentation.isEnabledAndVisible = false
    val panel = ProblemsView.getSelectedPanel(event.project) ?: return
    val file = getTopLevelFile(panel) ?: return
    if (!isAvailable(file, panel.preview.findEditor(marker.document))) return
    val text = action.text // may throw an exception if action.isAvailable is not invoked
    event.presentation.text = text
    event.presentation.isEnabledAndVisible = text.isNotEmpty()
  }

  override fun actionPerformed(event: AnActionEvent) {
    val panel = ProblemsView.getSelectedPanel(event.project) ?: return
    val file = getTopLevelFile(panel) ?: return
    chooseActionAndInvoke(file, panel.preview.findEditor(marker.document), action, action.text)
  }

  private fun getTopLevelFile(panel: ProblemsViewPanel): PsiFile? {
    if (!marker.isValid) return null
    val file = PsiDocumentManager.getInstance(panel.project).getPsiFile(marker.document) ?: return null
    return InjectedLanguageManager.getInstance(panel.project).getTopLevelFile(file)
  }

  private fun isAvailable(file: PsiFile, editor: Editor?) = try {
    action.isAvailable(file.project, editor, file)
  }
  catch (exception: Exception) {
    logger<HighlightingProblem>().warn(exception)
    false
  }
}
