// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.internal.statistic.actions

import com.intellij.ide.impl.ContentManagerWatcher
import com.intellij.internal.statistic.eventLog.validator.rules.impl.TestModeValidationRule
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowAnchor
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.content.ContentFactory

class ShowStatisticsEventLogAction : DumbAwareAction() {
  override fun actionPerformed(event: AnActionEvent) {
    val project = event.project ?: return
    val toolWindow = getToolWindow(project)
    val contentManager = toolWindow.contentManager
    if (contentManager.contentCount == 0) {
      val eventLogToolWindow = StatisticsEventLogToolWindow(project)
      val content = ContentFactory.SERVICE.getInstance().createContent(eventLogToolWindow.component, "", true)
      content.preferredFocusableComponent = eventLogToolWindow.component
      contentManager.addContent(content)
      ContentManagerWatcher(toolWindow, contentManager)
    }
    toolWindow.activate(null)
  }

  override fun update(event: AnActionEvent) {
    super.update(event)
    event.presentation.isEnabled = TestModeValidationRule.isTestModeEnabled()
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