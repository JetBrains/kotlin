// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.analysis.problemsView.toolWindow

import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInsight.intention.impl.ShowIntentionActionsHandler.chooseActionAndInvoke
import com.intellij.icons.AllIcons.Actions.IntentionBulb
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.editor.RangeMarker
import com.intellij.openapi.util.text.StringUtil.isEmpty
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile

internal class HighlightingProblem(val info: HighlightInfo) : Problem {

  override val description: String
    get() = info.description

  override val severity: Severity
    get() = when (info.severity) {
      HighlightSeverity.ERROR -> Severity.ERROR
      HighlightSeverity.WARNING -> Severity.WARNING
      else -> Severity.INFORMATION
    }

  override val offset: Int
    get() = info.actualStartOffset

  override fun hashCode() = info.hashCode()

  override fun equals(other: Any?) = other is HighlightingProblem && other.info == info

  override fun hasQuickFixActions(): Boolean {
    val markers = info.quickFixActionMarkers ?: return false
    return markers.any { !isEmpty(it.first.action.text) && it.second.isValid }
  }

  override fun getQuickFixActions(): Collection<AnAction> {
    val markers = info.quickFixActionMarkers ?: return emptyList()
    return markers.filter { !isEmpty(it.first.action.text) && it.second.isValid }.map { QuickFixAction(it.first.action, it.second) }
  }
}

private class QuickFixAction(val action: IntentionAction, val marker: RangeMarker)
  : AnAction(action.text, action.text, IntentionBulb) {

  override fun update(event: AnActionEvent) {
    event.presentation.isEnabledAndVisible = getTopLevelFile(event) != null
  }

  override fun actionPerformed(event: AnActionEvent) {
    val file = getTopLevelFile(event) ?: return
    chooseActionAndInvoke(file, null, action, action.text)
  }

  private fun getTopLevelFile(event: AnActionEvent): PsiFile? {
    if (!marker.isValid) return null
    val project = event.project ?: return null
    val file = PsiDocumentManager.getInstance(project).getPsiFile(marker.document) ?: return null
    return InjectedLanguageManager.getInstance(project).getTopLevelFile(file)
  }
}
