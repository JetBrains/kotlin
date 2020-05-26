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
import com.intellij.openapi.editor.RangeMarker
import com.intellij.openapi.util.text.StringUtil.isEmpty
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import javax.swing.Icon

internal class HighlightingProblem(val info: HighlightInfo) : Problem {

  private fun getIcon(level: HighlightDisplayLevel) = if (severity >= level.severity.myVal) level.icon else null

  override val icon: Icon
    get() = HighlightDisplayLevel.find(info.severity)?.icon
            ?: getIcon(HighlightDisplayLevel.ERROR)
            ?: getIcon(HighlightDisplayLevel.WARNING)
            ?: HighlightDisplayLevel.WEAK_WARNING.icon

  override val description: String
    get() = info.description

  override val severity: Int
    get() = info.severity.myVal

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
  : AnAction(action.text, action.text, AllIcons.Actions.IntentionBulb) {

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
