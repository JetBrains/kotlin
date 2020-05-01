// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.autoimport

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Toggleable
import com.intellij.openapi.externalSystem.autoimport.ExternalSystemProjectTrackerSettings.AutoReloadType
import com.intellij.openapi.externalSystem.autoimport.ToggleAutoReloadActionGroup.Companion.projectTrackerSettings
import com.intellij.openapi.externalSystem.util.ExternalSystemBundle
import com.intellij.openapi.project.DumbAwareAction

class UseAllAutoReloadTypeAction : AutoReloadTypeAction(AutoReloadType.ALL)

class UseSelectiveAutoReloadTypeAction : AutoReloadTypeAction(AutoReloadType.SELECTIVE)

class UseNoneAutoReloadTypeAction : AutoReloadTypeAction(AutoReloadType.NONE)

abstract class AutoReloadTypeAction(val type: AutoReloadType) : DumbAwareAction(), Toggleable {
  override fun update(e: AnActionEvent) {
    val settings = e.projectTrackerSettings
    e.presentation.isEnabledAndVisible = settings != null
    val isSelected = settings?.autoReloadType == type
    Toggleable.setSelected(e.presentation, isSelected)
  }

  override fun actionPerformed(e: AnActionEvent) {
    val settings = e.projectTrackerSettings ?: return
    settings.autoReloadType = type
  }

  init {
    templatePresentation.text = when (type) {
      AutoReloadType.ALL -> ExternalSystemBundle.message("action.refresh.project.auto.text.all")
      AutoReloadType.SELECTIVE -> ExternalSystemBundle.message("action.refresh.project.auto.text.selective")
      AutoReloadType.NONE -> ExternalSystemBundle.message("action.refresh.project.auto.text.none")
    }
    templatePresentation.description = when (type) {
      AutoReloadType.ALL -> ExternalSystemBundle.message("action.refresh.project.auto.description.all")
      AutoReloadType.SELECTIVE -> ExternalSystemBundle.message("action.refresh.project.auto.description.selective")
      AutoReloadType.NONE -> ExternalSystemBundle.message("action.refresh.project.auto.description.none")
    }
  }
}
