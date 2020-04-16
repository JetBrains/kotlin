// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.autoimport

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.externalSystem.util.ExternalSystemBundle
import com.intellij.openapi.project.DumbAware

class ToggleAutoReloadAction : ToggleAction(), DumbAware {
  override fun update(e: AnActionEvent) {
    super.update(e)
    e.presentation.description = when (isSelected(e)) {
      true -> ExternalSystemBundle.message("action.refresh.project.auto.description.disable")
      else -> ExternalSystemBundle.message("action.refresh.project.auto.description.enable")
    }
  }

  override fun isSelected(e: AnActionEvent): Boolean {
    val project = e.project ?: return false
    val projectTracker = ExternalSystemProjectTracker.getInstance(project)
    return projectTracker.isAutoReloadExternalChanges
  }

  override fun setSelected(e: AnActionEvent, state: Boolean) {
    val project = e.project ?: return
    val projectTracker = ExternalSystemProjectTracker.getInstance(project)
    projectTracker.isAutoReloadExternalChanges = state
  }

  init {
    templatePresentation.icon = AllIcons.Actions.BuildAutoReloadChanges
    templatePresentation.text = ExternalSystemBundle.message("action.refresh.project.auto.text")
    templatePresentation.description = ExternalSystemBundle.message("action.refresh.project.auto.description.disable")
  }
}
