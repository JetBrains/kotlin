// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.action.task

import com.intellij.execution.Executor
import com.intellij.execution.ExecutorRegistry
import com.intellij.execution.actions.RunContextAction
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.externalSystem.model.ExternalSystemDataKeys
import com.intellij.openapi.externalSystem.statistics.ExternalSystemActionsCollector
import com.intellij.openapi.project.DumbAware

class ExternalSystemTaskMenu : DefaultActionGroup(), DumbAware {

  override fun update(e: AnActionEvent) {
    val project = AnAction.getEventProject(e) ?: return

    childActionsOrStubs
      .filter { it is MyDelegatingAction }
      .forEach { remove(it) }

    ExecutorRegistry.getInstance().registeredExecutors
      .filter { it.isApplicable(project) }
      .reversed()
      .forEach { add(wrap(RunContextAction(it), it), Constraints.FIRST) }
  }

  private interface MyDelegatingAction

  private class DelegatingActionGroup internal constructor(action: ActionGroup, private val executor: Executor) :
    EmptyAction.MyDelegatingActionGroup(action), MyDelegatingAction {

    override fun getChildren(e: AnActionEvent?): Array<AnAction> {
      val children = super.getChildren(e)
      return children.map { wrap(it, executor) }.toTypedArray()
    }

    override fun actionPerformed(e: AnActionEvent) {
      reportUsage(e, executor)
      super.actionPerformed(e)
    }
  }

  private class DelegatingAction internal constructor(action: AnAction, private val executor: Executor) :
    EmptyAction.MyDelegatingAction(action), MyDelegatingAction {

    override fun actionPerformed(e: AnActionEvent) {
      reportUsage(e, executor)
      super.actionPerformed(e)
    }
  }

  companion object {
    private fun wrap(action: AnAction, executor: Executor): AnAction = if (action is ActionGroup) DelegatingActionGroup(action, executor)
    else DelegatingAction(action, executor)

    private fun reportUsage(e: AnActionEvent, executor: Executor) {
      val project = e.project
      val systemId = ExternalSystemDataKeys.EXTERNAL_SYSTEM_ID.getData(e.dataContext)
      ExternalSystemActionsCollector.trigger(project, systemId, ExternalSystemActionsCollector.ActionId.RunExternalSystemTaskAction, e,
                                             executor)
    }
  }


}