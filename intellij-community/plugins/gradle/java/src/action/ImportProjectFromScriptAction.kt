// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.action

import com.intellij.ide.actions.ImportModuleAction.createFromWizard
import com.intellij.ide.actions.ImportModuleAction.createImportWizard
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.externalSystem.action.ExternalSystemAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.projectImport.ProjectImportProvider
import org.jetbrains.plugins.gradle.settings.GradleSettings
import org.jetbrains.plugins.gradle.util.GradleConstants

class ImportProjectFromScriptAction: ExternalSystemAction() {

  override fun isEnabled(e: AnActionEvent): Boolean = true

  override fun isVisible(e: AnActionEvent): Boolean {
    val virtualFile = e.getData<VirtualFile>(CommonDataKeys.VIRTUAL_FILE) ?: return false
    val project = e.getData<Project>(CommonDataKeys.PROJECT) ?: return false

    if (virtualFile.name  != GradleConstants.DEFAULT_SCRIPT_NAME
        && virtualFile.name != GradleConstants.KOTLIN_DSL_SCRIPT_NAME) {
      return false
    }

    return GradleSettings.getInstance(project).getLinkedProjectSettings(virtualFile.parent.path) == null
  }

  override fun actionPerformed(e: AnActionEvent) {
    val virtualFile = e.getData<VirtualFile>(CommonDataKeys.VIRTUAL_FILE) ?: return
    val project = e.getData<Project>(CommonDataKeys.PROJECT) ?: return

    val wizard = createImportWizard(project, null, virtualFile,
                                    *ProjectImportProvider.PROJECT_IMPORT_PROVIDER.extensions)
    if (wizard != null && (wizard.stepCount <= 0 || wizard.showAndGet())) {
      createFromWizard(project, wizard)
    }
  }
}