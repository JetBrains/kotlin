// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.analysis.problemsView.toolWindow

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces.POPUP
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.ui.popup.JBPopupFactory

internal class ProblemsViewQuickFixAction : AnAction() {
  override fun update(event: AnActionEvent) {
    val panel = ProblemsView.getSelectedPanel(event.project)
    val node = panel?.tree?.selectionPath?.lastPathComponent as? ProblemNode
    event.presentation.isEnabled = node != null && node.problem.hasQuickFixActions()
  }

  override fun actionPerformed(event: AnActionEvent) {
    val panel = ProblemsView.getSelectedPanel(event.project) ?: return
    val node = panel.tree.selectionPath?.lastPathComponent as? ProblemNode ?: return
    val actions = node.problem.getQuickFixActions()
    if (actions.isEmpty()) return
    val group = DefaultActionGroup()
    actions.forEach { group.add(it) }
    val menu = ActionManager.getInstance().createActionPopupMenu(POPUP, DefaultActionGroup(actions.toList()))
    val point = JBPopupFactory.getInstance().guessBestPopupLocation(panel.tree)
    menu.component.show(point.component, point.point.x, point.point.y)
  }
}
