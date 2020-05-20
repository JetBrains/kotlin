// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.analysis.problemsView.toolWindow

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces.POPUP
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.PlatformDataKeys.CONTEXT_COMPONENT
import com.intellij.openapi.ui.popup.JBPopupFactory
import java.awt.event.MouseEvent
import javax.swing.SwingUtilities.isDescendingFrom

internal class ProblemsViewQuickFixAction : AnAction() {
  override fun update(event: AnActionEvent) {
    val problem = ProblemsView.getSelectedPanel(event.project)?.selectedProblem
    event.presentation.isEnabled = problem != null && problem.hasQuickFixActions()
  }

  override fun actionPerformed(event: AnActionEvent) {
    val panel = ProblemsView.getSelectedPanel(event.project) ?: return // no problems view
    val problem = panel.selectedProblem ?: return // no selected problem
    if (event.inputEvent !is MouseEvent) {
      val context = event.getData(CONTEXT_COMPONENT) ?: return // no context component
      if (!isDescendingFrom(context, panel)) return // panel is out of the context
    }
    val actions = problem.getQuickFixActions()
    if (actions.isEmpty()) return // no quick fixes
    val menu = ActionManager.getInstance().createActionPopupMenu(POPUP, DefaultActionGroup(actions.toList()))
    val point = JBPopupFactory.getInstance().guessBestPopupLocation(panel.tree)
    menu.component.show(point.component, point.point.x, point.point.y)
  }
}
