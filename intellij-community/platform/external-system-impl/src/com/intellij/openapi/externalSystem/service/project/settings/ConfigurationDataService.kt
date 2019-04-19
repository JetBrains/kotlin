// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.service.project.settings

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.externalSystem.model.ConfigurationDataImpl
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.Key
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.model.project.settings.ConfigurationData
import com.intellij.openapi.externalSystem.service.project.IdeModelsProvider
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider
import com.intellij.openapi.externalSystem.service.project.manage.AbstractModuleDataService.MODULE_KEY
import com.intellij.openapi.externalSystem.service.project.manage.AbstractProjectDataService
import com.intellij.openapi.externalSystem.util.*
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.registry.Registry
import org.jetbrains.annotations.ApiStatus

/**
 * @author Vladislav.Soroka
 */
@ApiStatus.Experimental
@Order(ExternalSystemConstants.BUILTIN_SERVICE_ORDER)
class ConfigurationDataService : AbstractProjectDataService<ConfigurationData, Void>() {

  override fun getTargetDataKey(): Key<ConfigurationData> {
    return ProjectKeys.CONFIGURATION
  }

  override fun importData(toImport: Collection<DataNode<ConfigurationData>>,
                          projectData: ProjectData?,
                          project: Project,
                          modelsProvider: IdeModifiableModelsProvider) {
    withConfigurationData(toImport, projectData,
                          project, modelsProvider,
                          ConfigurationHandler::apply,
                          ConfigurationHandler::apply)
  }

  override fun onSuccessImport(imported: Collection<DataNode<ConfigurationData>>,
                               projectData: ProjectData?,
                               project: Project,
                               modelsProvider: IdeModelsProvider) {
    withConfigurationData(imported, projectData,
                          project, modelsProvider,
                          ConfigurationHandler::onSuccessImport,
                          ConfigurationHandler::onSuccessImport)
  }

  companion object {
    private val LOG = Logger.getInstance(ConfigurationDataService::class.java)
    const val EXTERNAL_SYSTEM_CONFIGURATION_IMPORT_ENABLED = "external.system.configuration.import.enabled"

    private fun <ModelsProvider : IdeModelsProvider> withConfigurationData(
      configurationData: Collection<DataNode<ConfigurationData>>,
      projectData: ProjectData?,
      project: Project,
      modelsProvider: ModelsProvider,
      acceptProject: ConfigurationHandler.(Project, ProjectData?, ModelsProvider, ConfigurationData) -> Unit,
      acceptModule: ConfigurationHandler.(Module, ModelsProvider, ConfigurationData) -> Unit
    ) {
      if (configurationData.isEmpty() || !Registry.`is`(EXTERNAL_SYSTEM_CONFIGURATION_IMPORT_ENABLED)) {
        LOG.debug("Configuration data is" +
                  (if (!configurationData.isEmpty()) " not " else " ") +
                  "empty, Registry flag is " +
                  Registry.`is`(EXTERNAL_SYSTEM_CONFIGURATION_IMPORT_ENABLED))
        return
      }

      val javaProjectDataNode = configurationData.iterator().next()
      val projectDataNode = ExternalSystemApiUtil.findParent(javaProjectDataNode, ProjectKeys.PROJECT)!!

      val projectConfigurationNode = ExternalSystemApiUtil.find(projectDataNode, ProjectKeys.CONFIGURATION)
      if (projectConfigurationNode != null) {

        val data = projectConfigurationNode.data
        if (LOG.isDebugEnabled && data is ConfigurationDataImpl) {
          LOG.debug("Importing project configuration: " + data.jsonString)
        }

        if (!ExternalSystemApiUtil.isOneToOneMapping(project, projectDataNode.data)) {
          LOG.warn(
            "This external project are not the only project in the current IDE workspace, " +
            "found project level configuration can override the configuration came from other external projects.")
        }

        for (handler in ConfigurationHandler.EP_NAME.extensions) {
          handler.acceptProject(project, projectData, modelsProvider, data)
        }
      }

      for (node in configurationData) {
        if (node === projectConfigurationNode) continue

        val moduleDataNode = ExternalSystemApiUtil.findParent(node, ProjectKeys.MODULE)
        if (moduleDataNode != null) {
          var module = moduleDataNode.getUserData<Module>(MODULE_KEY)
          module = module ?: modelsProvider.findIdeModule(moduleDataNode.data)

          if (module == null) {
            LOG.warn(String.format(
              "Can't import module level configuration. Reason: target module (%s) is not found at the ide", moduleDataNode))
            continue
          }

          val data = node.data
          if (LOG.isDebugEnabled && data is ConfigurationDataImpl) {
            LOG.debug("Importing module configuration: " + data.jsonString)
          }

          for (handler in ConfigurationHandler.EP_NAME.extensions) {
            handler.acceptModule(module, modelsProvider, data)
          }
        }
      }
    }
  }
}