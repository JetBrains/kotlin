// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.execution

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider
import com.intellij.openapi.externalSystem.service.project.settings.RunConfigurationImporter
import com.intellij.openapi.project.Project
import com.intellij.util.ObjectUtils.consumeIfCast

class GradleRunConfigurationImporter: RunConfigurationImporter {
  override fun process(project: Project,
                       runConfiguration: RunConfiguration,
                       cfg: MutableMap<String, Any>,
                       modelsProvider: IdeModifiableModelsProvider) {

    if (runConfiguration !is GradleRunConfiguration) {
      return
    }

    val settings = runConfiguration.settings

    consumeIfCast(cfg["projectPath"], String::class.java) { settings.externalProjectPath = it }
    consumeIfCast(cfg["taskNames"], List::class.java) { settings.taskNames = it as List<String> }
    consumeIfCast(cfg["envs"], Map::class.java) { settings.env = it as Map<String, String> }
    consumeIfCast(cfg["jvmArgs"], String::class.java) { settings.vmOptions = it }
    consumeIfCast(cfg["scriptParameters"], String::class.java) { settings.scriptParameters = it }
  }

  override fun canImport(typeName: String): Boolean = "gradle" == typeName

  override fun getConfigurationFactory(): ConfigurationFactory = GradleExternalTaskConfigurationType.getInstance().factory
}