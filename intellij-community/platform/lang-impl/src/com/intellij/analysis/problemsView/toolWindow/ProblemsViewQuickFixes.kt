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
  private fun getProblem(context: DataContext?): Problem? {
    val tree = context?.getData(CONTEXT_COMPONENT) as? JTree ?: return null
    return getLastUserObject(ProblemNode::class.java, tree.selectionPath)?.problem
  }

  override fun canBePerformed(context: DataContext): Boolean {
    val problem = getProblem(context) ?: return true
    return !problem.hasQuickFixActions()
  }

  override fun getChildren(event: AnActionEvent?): Array<AnAction> {
    val actions = getProblem(event?.dataContext)?.getQuickFixActions()
    return if (actions == null || actions.isEmpty()) AnAction.EMPTY_ARRAY else actions.toTypedArray()
  }
}
