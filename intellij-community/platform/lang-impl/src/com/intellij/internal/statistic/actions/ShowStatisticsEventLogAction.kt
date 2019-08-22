// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.internal.statistic.actions

import com.intellij.icons.AllIcons
import com.intellij.ide.actions.NonEmptyActionGroup
import com.intellij.ide.impl.ContentManagerWatcher
import com.intellij.internal.statistic.eventLog.getEventLogProviders
import com.intellij.internal.statistic.eventLog.validator.rules.impl.TestModeValidationRule
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowAnchor
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.wm.impl.ToolWindowImpl
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.content.ContentManager

class ShowStatisticsEventLogAction : DumbAwareAction() {

  override fun actionPerformed(event: AnActionEvent) {
    val project = event.project ?: return
    val toolWindow = getToolWindow(project)
    val contentManager = toolWindow.contentManager
    if (contentManager.contentCount == 0) {
      createNewTab(project, contentManager, "FUS")
      ContentManagerWatcher(toolWindow, contentManager)
      if (toolWindow is ToolWindowImpl) {
        val newSessionActionGroup = createNewSessionActionGroup(project)
        toolWindow.setTabActions(newSessionActionGroup)
      }
    }
    toolWindow.activate(null)
  }

  override fun update(event: AnActionEvent) {
    super.update(event)
    event.presentation.isEnabled = TestModeValidationRule.isTestModeEnabled()
  }

  private fun createNewSessionActionGroup(project: Project): NonEmptyActionGroup {
    val actionGroup = NonEmptyActionGroup()
    actionGroup.isPopup = true
    actionGroup.templatePresentation.icon = AllIcons.General.Add

    val actions = getEventLogProviders().map { logger ->
      val recorder = logger.recorderId
      NewStatisticsEventLogSession(project, recorder)
    }
    actionGroup.addAll(actions)
    return actionGroup
  }

  class NewStatisticsEventLogSession(private val project: Project, private val recorderId: String) : AnAction(recorderId) {
    override fun actionPerformed(e: AnActionEvent) {
      val toolWindow = getToolWindow(project)
      createNewTab(project, toolWindow.contentManager, recorderId)
    }
  }

  companion object {
    private fun createNewTab(
      project: Project,
      contentManager: ContentManager,
      recorderId: String
    ) {
      val eventLogToolWindow = StatisticsEventLogToolWindow(project, recorderId)
      val content = ContentFactory.SERVICE.getInstance().createContent(eventLogToolWindow.component, recorderId, true)
      content.preferredFocusableComponent = eventLogToolWindow.component
      contentManager.addContent(content)
    }

    private fun getToolWindow(project: Project): ToolWindow {
      val toolWindowManager = ToolWindowManager.getInstance(project)
      val toolWindow = toolWindowManager.getToolWindow(eventLogToolWindowsId)
      if (toolWindow != null) {
        return toolWindow
      }
      return toolWindowManager.registerToolWindow(eventLogToolWindowsId, true, ToolWindowAnchor.BOTTOM, project, true)
    }
  }

}