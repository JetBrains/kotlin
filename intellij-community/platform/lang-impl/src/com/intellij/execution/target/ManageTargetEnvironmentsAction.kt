// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.target

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.Experiments
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.DumbAwareAction

class ManageTargetEnvironmentsAction : DumbAwareAction("Manage Targets...") {
  override fun actionPerformed(e: AnActionEvent) {
    ShowSettingsUtil.getInstance().editConfigurable(e.project, TargetEnvironmentsConfigurable(e.project!!, null))
  }

  override fun update(e: AnActionEvent) {
    super.update(e)
    e.presentation.isEnabledAndVisible = Experiments.getInstance().isFeatureEnabled("runtime.environments") &&
                                         TargetEnvironmentType.EXTENSION_NAME.extensionList.isNotEmpty()
  }
}