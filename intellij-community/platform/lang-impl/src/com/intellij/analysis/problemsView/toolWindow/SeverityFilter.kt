// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.analysis.problemsView.toolWindow

import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.DumbAwareToggleAction
import java.util.function.Predicate

internal class SeverityFilter(val state: ProblemsViewState) : Predicate<Node?> {
  override fun test(node: Node?) = node !is ProblemNode || !state.hideBySeverity.contains(node.problem.severity)
}

internal class SeverityFiltersActionGroup : DumbAware, ActionGroup() {
  override fun getChildren(event: AnActionEvent?): Array<AnAction> {
    val project = event?.project ?: return AnAction.EMPTY_ARRAY
    if (project.isDisposed) return AnAction.EMPTY_ARRAY
    val panel = ProblemsView.getSelectedPanel(project) ?: return AnAction.EMPTY_ARRAY
    return panel.severityFilters.map { SeverityFilterAction(it.first, it.second, panel) }.toTypedArray()
  }
}

private class SeverityFilterAction(name: String, val severity: Int, val panel: ProblemsViewPanel) : DumbAwareToggleAction(name) {
  override fun isSelected(event: AnActionEvent) = !panel.state.hideBySeverity.contains(severity)
  override fun setSelected(event: AnActionEvent, selected: Boolean) {
    val changed = with(panel.state.hideBySeverity) { if (selected) remove(severity) else add(severity) }
    if (changed) {
      panel.state.intIncrementModificationCount()
      panel.treeModel.structureChanged()
    }
  }
}
