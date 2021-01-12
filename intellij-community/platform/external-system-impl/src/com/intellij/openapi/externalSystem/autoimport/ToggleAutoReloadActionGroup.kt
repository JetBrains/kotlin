// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.autoimport

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CheckedActionGroup
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.Toggleable
import com.intellij.openapi.externalSystem.autoimport.ExternalSystemProjectTrackerSettings.AutoReloadType
import com.intellij.openapi.externalSystem.util.ExternalSystemBundle
import com.intellij.openapi.project.DumbAware

class ToggleAutoReloadActionGroup : DefaultActionGroup(), Toggleable, CheckedActionGroup, DumbAware {
  override fun update(e: AnActionEvent) {
    val settings = e.projectTrackerSettings
    e.presentation.isEnabledAndVisible = settings != null
    val isDisabledAutoReload = settings?.autoReloadType == AutoReloadType.NONE
    Toggleable.setSelected(e.presentation, !isDisabledAutoReload)
  }

  init {
    isPopup = true
    templatePresentation.icon = AllIcons.Actions.BuildAutoReloadChanges
    templatePresentation.text = ExternalSystemBundle.message("action.refresh.project.auto.text")
  }

  companion object {
    internal val AnActionEvent.projectTrackerSettings: ExternalSystemProjectTrackerSettings?
      get() = project?.let { ExternalSystemProjectTrackerSettings.getInstance(it) }
  }
}
