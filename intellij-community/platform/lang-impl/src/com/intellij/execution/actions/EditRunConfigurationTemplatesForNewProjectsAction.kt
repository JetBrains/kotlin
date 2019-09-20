// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.actions

import com.intellij.execution.impl.EditConfigurationsDialog
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.ProjectManager

class EditRunConfigurationTemplatesForNewProjectsAction : DumbAwareAction("Run Configuration Templates for New Projects") {
  override fun actionPerformed(e: AnActionEvent) {
    val dialog = EditConfigurationsDialog(ProjectManager.getInstance().defaultProject)
    dialog.title = e.presentation.text
    dialog.show()
  }
}