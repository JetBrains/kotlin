// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.daemon.impl

import com.intellij.codeInsight.daemon.DaemonBundle.message
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.options.ex.ConfigurableExtensionPointUtil.createProjectConfigurableForProvider
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.profile.codeInspection.ui.ErrorsConfigurableProvider

class ConfigureInspectionsAction : DumbAwareAction(message("popup.action.configure.inspections")) {

  override fun update(event: AnActionEvent) {
    event.presentation.isEnabled = event.project != null
  }

  override fun actionPerformed(event: AnActionEvent) {
    val project = event.project ?: return
    val provider = createProjectConfigurableForProvider(project, ErrorsConfigurableProvider::class.java) ?: return
    ShowSettingsUtil.getInstance().editConfigurable(project, provider)
  }
}
