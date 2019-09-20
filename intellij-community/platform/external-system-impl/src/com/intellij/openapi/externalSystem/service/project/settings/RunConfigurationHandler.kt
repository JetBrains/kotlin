// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.service.project.settings

import com.intellij.execution.RunManagerEx
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.externalSystem.model.project.settings.ConfigurationData
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project

internal val LOG: Logger = logger<RunConfigurationHandler>()

internal class RunConfigurationHandler : ConfigurationHandler {
  private fun Any?.isTrue(): Boolean = this != null && this is Boolean && this

  override fun apply(module: Module, modelsProvider: IdeModifiableModelsProvider, configuration: ConfigurationData) {}

  override fun apply(project: Project, modelsProvider: IdeModifiableModelsProvider, configuration: ConfigurationData) {
    val runCfgMap = configuration.find("runConfigurations")
    if (runCfgMap !is List<*>) {
      return
    }

    val runManager = RunManagerEx.getInstanceEx(project)
    for (cfg in runCfgMap
      .filterIsInstance<Map<*, *>>()
      .sortedByDescending { it["defaults"].isTrue() }) {
      val name = cfg["name"] as? String ?: ""
      val typeName = cfg["type"] as? String
      if (typeName == null) {
        LOG.warn("Missing type for run configuration: '${name}', skipping")
        continue
      }

      val importer = handlerForType(typeName)
      if (importer == null) {
        LOG.warn("No importers for run configuration '${name}' with type '$typeName', skipping")
        continue
      }

      val isDefaults = cfg["defaults"].isTrue()

      val runnerAndConfigurationSettings = when {
        isDefaults -> runManager.getConfigurationTemplate(importer.configurationFactory)
        else -> runManager.createConfiguration(name, importer.configurationFactory)
      }

      try {
        importer.process(project, runnerAndConfigurationSettings.configuration, cfg as Map<String, *>, modelsProvider)
        if (!isDefaults) {
          runManager.addConfiguration(runnerAndConfigurationSettings)
        }

        (cfg["beforeRun"] as? List<*>)?.let {
          var tasksList = runnerAndConfigurationSettings.configuration.getBeforeRunTasks()
          for (beforeRunConfig in it.filterIsInstance(Map::class.java)) {
            val typeName = beforeRunConfig["type"] as? String ?: continue
            importerForType(typeName)?.let { importer ->
              tasksList = importer.process(project,
                                           modelsProvider,
                                           runnerAndConfigurationSettings.configuration,
                                           tasksList,
                                           beforeRunConfig as MutableMap<String, *>)
            }
          }
          runManager.setBeforeRunTasks(runnerAndConfigurationSettings.configuration, tasksList)
        }
      }
      catch (e: Exception) {
        LOG.warn("Error occurred when importing run configuration ${name}: ${e.message}", e)
      }
    }
  }
}

private fun handlerForType(typeName: String): RunConfigurationImporter? {
  return RunConfigurationImporter.EP_NAME.extensionList.firstOrNull { it.canImport(typeName) }
}

private fun importerForType(typeName: String): BeforeRunTaskImporter? {
  return BeforeRunTaskImporter.EP_NAME.extensionList.firstOrNull { it.canImport(typeName) }
}