// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.analysis.problemsView.toolWindow

import com.intellij.codeInsight.daemon.impl.SeverityRegistrar
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.DumbAwareToggleAction
import com.intellij.profile.codeInspection.ui.SingleInspectionProfilePanel.renderSeverity

internal class ProblemFilter(val state: ProblemsViewState) : (Problem) -> Boolean {
  override fun invoke(problem: Problem) = !state.hideBySeverity.contains(problem.severity)
}

internal class SeverityFiltersActionGroup : DumbAware, ActionGroup() {
  override fun getChildren(event: AnActionEvent?): Array<AnAction> {
    val project = event?.project ?: return AnAction.EMPTY_ARRAY
    if (project.isDisposed) return AnAction.EMPTY_ARRAY
    val panel = ProblemsView.getSelectedPanel(project) as? HighlightingPanel ?: return AnAction.EMPTY_ARRAY
    return SeverityRegistrar.getSeverityRegistrar(project).allSeverities.reversed()
      .filter { it != HighlightSeverity.INFO && it > HighlightSeverity.INFORMATION && it < HighlightSeverity.ERROR }
      .map { Pair(ProblemsViewBundle.message("problems.view.highlighting.severity.show", renderSeverity(it)), it.myVal) }
      .map { SeverityFilterAction(it.first, it.second, panel) }
      .toTypedArray()
  }
}

private class SeverityFilterAction(name: String, val severity: Int, val panel: HighlightingPanel) : DumbAwareToggleAction(name) {
  override fun isSelected(event: AnActionEvent) = !panel.state.hideBySeverity.contains(severity)
  override fun setSelected(event: AnActionEvent, selected: Boolean) {
    val changed = with(panel.state.hideBySeverity) { if (selected) remove(severity) else add(severity) }
    if (changed) {
      panel.state.intIncrementModificationCount()
      panel.treeModel.structureChanged()
      panel.requestStatusUpdating()
    }
  }
}
