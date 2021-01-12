// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.service.project.manage

import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.project.ModuleData
import com.intellij.openapi.externalSystem.model.project.ModuleSdkData
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.model.project.ProjectSdkData
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider
import com.intellij.openapi.externalSystem.util.DisposeAwareProjectChange
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.externalSystem.util.ExternalSystemBundle
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.roots.ProjectRootManager

class ProjectSdkDataService : AbstractProjectDataService<ProjectSdkData, Project?>() {
  override fun getTargetDataKey() = ProjectSdkData.KEY

  override fun importData(
    toImport: MutableCollection<DataNode<ProjectSdkData>>,
    projectData: ProjectData?,
    project: Project,
    modelsProvider: IdeModifiableModelsProvider
  ) {
    if (toImport.isEmpty() || projectData == null) return
    require(toImport.size == 1) { String.format("Expected to get a single project but got %d: %s", toImport.size, toImport) }
    if (!ExternalSystemApiUtil.isOneToOneMapping(project, projectData, modelsProvider.modules)) return
    for (sdkDataNode in toImport) {
      ExternalSystemApiUtil.executeProjectChangeAction(object : DisposeAwareProjectChange(project) {
        override fun execute() {
          importProjectSdk(project, sdkDataNode.data)
        }
      })
    }
  }

  private fun importProjectSdk(project: Project, sdkData: ProjectSdkData) {
    val sdkName = sdkData.sdkName ?: return
    val projectJdkTable = ProjectJdkTable.getInstance()
    val sdk = projectJdkTable.findJdk(sdkName)
    val projectRootManager = ProjectRootManager.getInstance(project)
    val projectSdk = projectRootManager.projectSdk
    if (projectSdk == null) {
      projectRootManager.projectSdk = sdk
    }
  }
}

class ModuleSdkDataService : AbstractProjectDataService<ModuleSdkData, Project?>() {
  override fun getTargetDataKey() = ModuleSdkData.KEY

  override fun importData(
    toImport: MutableCollection<DataNode<ModuleSdkData>>,
    projectData: ProjectData?,
    project: Project,
    modelsProvider: IdeModifiableModelsProvider
  ) {
    if (toImport.isEmpty() || projectData == null) return
    val useDefaultsIfCan = ExternalSystemApiUtil.isOneToOneMapping(project, projectData, modelsProvider.modules)
    for (sdkDataNode in toImport) {
      val moduleNode = sdkDataNode.getParent(ModuleData::class.java) ?: continue
      val module = moduleNode.getUserData(AbstractModuleDataService.MODULE_KEY) ?: continue

      importModuleSdk(module, sdkDataNode.data, modelsProvider, useDefaultsIfCan)
    }
  }


  private fun importModuleSdk(
    module: Module,
    sdkData: ModuleSdkData,
    modelsProvider: IdeModifiableModelsProvider,
    useDefaultsIfCan: Boolean
  ) {
    val moduleSdkName = sdkData.sdkName
    val projectJdkTable = ProjectJdkTable.getInstance()
    val sdk = moduleSdkName?.let { projectJdkTable.findJdk(moduleSdkName) }
    val modifiableRootModel = modelsProvider.getModifiableRootModel(module)
    if (modifiableRootModel.sdk != null) return
    val projectRootManager = ProjectRootManager.getInstance(module.project)
    val projectSdk = projectRootManager.projectSdk
    when {
      useDefaultsIfCan && sdk == projectSdk -> modifiableRootModel.inheritSdk()
      moduleSdkName == null && sdk == null -> modifiableRootModel.inheritSdk()
      sdk == null -> modifiableRootModel.setInvalidSdk(moduleSdkName, ExternalSystemBundle.message("unknown.sdk.type"))
      else -> modifiableRootModel.sdk = sdk
    }
  }
}