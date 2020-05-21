// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.analysis.problemsView.toolWindow

import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.PlatformDataKeys.CONTEXT_COMPONENT
import com.intellij.util.ui.tree.TreeUtil.getLastUserObject
import javax.swing.JTree

internal class ProblemsViewQuickFixes : ActionGroup() {
  override fun canBePerformed(context: DataContext) = false

  override fun update(event: AnActionEvent) {
    val enabled = getProblem(event)?.hasQuickFixActions()
    event.presentation.isEnabledAndVisible = enabled == true
  }

  override fun getChildren(event: AnActionEvent?): Array<AnAction> {
    val actions = getProblem(event)?.getQuickFixActions()
    return if (actions == null || actions.isEmpty()) AnAction.EMPTY_ARRAY else actions.toTypedArray()
  }

  private fun getProblem(event: AnActionEvent?): Problem? {
    val tree = event?.getData(CONTEXT_COMPONENT) as? JTree ?: return null
    return getLastUserObject(ProblemNode::class.java, tree.selectionPath)?.problem
  }
}
