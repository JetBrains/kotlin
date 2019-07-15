// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.projectView.actions

import com.intellij.ide.actions.OpenModuleSettingsAction
import com.intellij.ide.projectView.impl.ProjectRootsUtil
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.roots.ui.configuration.ConfigureUnloadedModulesDialog

/**
 * @author nik
 */
class LoadUnloadModulesAction : DumbAwareAction("Load/Unload Modules...") {
  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = isEnabled(e)
  }

  private fun isEnabled(e: AnActionEvent): Boolean {
    val project = e.project ?: return false
    val moduleManager = ModuleManager.getInstance(project)
    if (moduleManager.modules.size <= 1 && moduleManager.unloadedModuleDescriptions.isEmpty()) return false

    val file = e.getData(LangDataKeys.VIRTUAL_FILE)
    return !ActionPlaces.isPopupPlace(e.place) || OpenModuleSettingsAction.isModuleInContext(e)
           || file != null && ProjectRootsUtil.findUnloadedModuleByContentRoot(file, project) != null
  }

  override fun actionPerformed(e: AnActionEvent) {
    val selectedModuleName = e.getData(LangDataKeys.MODULE_CONTEXT)?.name ?: getSelectedUnloadedModuleName(e)
                             ?: e.getData(LangDataKeys.MODULE)?.name
    ConfigureUnloadedModulesDialog(e.project!!, selectedModuleName).show()
  }

  private fun getSelectedUnloadedModuleName(e: AnActionEvent): String? {
    val project = e.project ?: return null
    val file = e.getData(LangDataKeys.VIRTUAL_FILE) ?: return null
    return ProjectRootsUtil.findUnloadedModuleByFile(file, project)
  }
}