// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.analysis.problemsView.toolWindow

import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.ui.popup.JBPopupFactory
import java.util.function.Function

internal class AutoscrollToSource : ProblemsViewToggleAction({ it.autoscrollToSource })
internal class ShowPreview : ProblemsViewToggleAction({ it.showPreview })
internal class ShowErrors : ProblemsViewToggleAction({ it.showErrors })
internal class ShowWarnings : ProblemsViewToggleAction({ it.showWarnings })
internal class ShowInformation : ProblemsViewToggleAction({ it.showInformation })
internal class SortFoldersFirst : ProblemsViewToggleAction({ it.sortFoldersFirst })
internal class SortBySeverity : ProblemsViewToggleAction({ it.sortBySeverity })
internal class SortByName : ProblemsViewToggleAction({ it.sortByName })

internal open class ProblemsViewToggleAction(optionSupplier: (ProblemsViewPanel) -> Option?)
  : DumbAware, ToggleOptionAction(Function { event: AnActionEvent -> getProblemsViewPanel(event)?.let(optionSupplier) })

internal fun getProblemsViewPanel(event: AnActionEvent) = event.project?.let {
  ProblemsView.getToolWindow(it)?.contentManagerIfCreated?.selectedContent?.component as? ProblemsViewPanel
}

internal class ProblemsViewQuickFixAction : AnAction() {
  override fun update(event: AnActionEvent) {
    val panel = getProblemsViewPanel(event)
    val node = panel?.tree?.selectionPath?.lastPathComponent as? ProblemNode
    event.presentation.isEnabledAndVisible = node != null && node.problem.hasQuickFixActions()
  }

  override fun actionPerformed(event: AnActionEvent) {
    val panel = getProblemsViewPanel(event) ?: return
    val node = panel.tree.selectionPath?.lastPathComponent as? ProblemNode ?: return
    val actions = node.problem.getQuickFixActions()
    if (actions.isEmpty()) return
    val group = DefaultActionGroup()
    actions.forEach { group.add(it) }
    val menu = ActionManager.getInstance().createActionPopupMenu(ActionPlaces.POPUP, DefaultActionGroup(actions.toList()))
    val point = JBPopupFactory.getInstance().guessBestPopupLocation(panel.tree)
    menu.component.show(point.component, point.point.x, point.point.y)
  }
}
