// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.service.project.manage

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.externalSystem.model.ProjectKeys.CONTENT_ROOT
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProviderImpl
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity

class ReprocessContentRootDataActivity : StartupActivity, DumbAware {

  override fun runActivity(project: Project) {
    if (ApplicationManager.getApplication().isUnitTestMode) {
      return
    }
    val dataManager = ProjectDataManager.getInstance()
    val service = ContentRootDataService()

    val externalProjectsManager = ExternalProjectsManagerImpl.getInstance(project)
    externalProjectsManager.init()
    externalProjectsManager.runWhenInitialized {
      val haveModulesToProcess = ModuleManager.getInstance(project).modules.isNotEmpty()
      if (!haveModulesToProcess) {
        return@runWhenInitialized
      }

      val modifiableModelsProvider = lazy { IdeModifiableModelsProviderImpl(project) }

      try {
        ExternalSystemApiUtil.getAllManagers()
          .flatMap { dataManager.getExternalProjectsData(project, it.getSystemId()) }
          .mapNotNull { it.externalProjectStructure }
          .map { ExternalSystemApiUtil.findAllRecursively(it, CONTENT_ROOT) }
          .forEach {
            service.importData(it, null, project, modifiableModelsProvider.value)
          }
      }
      finally {
        if (modifiableModelsProvider.isInitialized()) {
          ExternalSystemApiUtil.doWriteAction {
            if (!project.isDisposed) {
              modifiableModelsProvider.value.commit()
            } else {
              modifiableModelsProvider.value.dispose()
            }
          }
        }
      }
    }
  }
}
