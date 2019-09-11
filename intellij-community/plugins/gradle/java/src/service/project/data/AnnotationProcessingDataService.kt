// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.project.data

import com.intellij.compiler.CompilerConfiguration
import com.intellij.compiler.CompilerConfigurationImpl
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.Key
import com.intellij.openapi.externalSystem.model.project.ModuleData
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider
import com.intellij.openapi.externalSystem.service.project.manage.AbstractProjectDataService
import com.intellij.openapi.externalSystem.util.ExternalSystemConstants
import com.intellij.openapi.externalSystem.util.Order
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import org.jetbrains.jps.model.java.compiler.ProcessorConfigProfile
import org.jetbrains.jps.model.java.impl.compiler.ProcessorConfigProfileImpl
import org.jetbrains.plugins.gradle.model.data.AnnotationProcessingData
import java.io.File

@Order(ExternalSystemConstants.UNORDERED)
class AnnotationProcessingDataService : AbstractProjectDataService<AnnotationProcessingData, ProcessorConfigProfile>() {

  val configurationProfileCache = mutableMapOf<AnnotationProcessingData, ProcessorConfigProfile>()

  override fun getTargetDataKey(): Key<AnnotationProcessingData> {
    return AnnotationProcessingData.KEY
  }

  override fun importData(toImport: Collection<DataNode<AnnotationProcessingData>>,
                          projectData: ProjectData?,
                          project: Project,
                          modelsProvider: IdeModifiableModelsProvider) {
    val config = CompilerConfiguration.getInstance(project) as CompilerConfigurationImpl
    for (node in toImport) {
      val moduleData = node.parent?.data as? ModuleData
      if (moduleData == null) {
        LOG.debug("Failed to find parent module data in annotation processor data. Parent: ${node.parent} ")
        continue
      }

      val ideModule = modelsProvider.findIdeModule(moduleData)
      if (ideModule == null) {
        LOG.debug("Failed to find ide module for module data: ${moduleData}")
        continue
      }

      config.configureAnnotationProcessing(ideModule, node.data)
    }
  }

  private fun CompilerConfigurationImpl.configureAnnotationProcessing(ideModule: Module, data: AnnotationProcessingData) {
    val profile = findOrCreateProcessorConfigProfile(data)
    with (profile) {
      isEnabled = true
      isObtainProcessorsFromClasspath = false
      addModuleName(ideModule.name)
    }
  }

  private fun CompilerConfigurationImpl.findOrCreateProcessorConfigProfile(data: AnnotationProcessingData): ProcessorConfigProfile {
    return configurationProfileCache.computeIfAbsent(data) { newData ->
      val newProfile = ProcessorConfigProfileImpl("Gradle Imported")
      newProfile.setProcessorPath(newData.path.joinToString(separator = File.pathSeparator))
      newData.arguments
        .map { it.removePrefix("-A").split('=', limit = 2) }
        .forEach { newProfile.setOption(it[0], if (it.size > 1) it[1] else "") }

      return@computeIfAbsent this.moduleProcessorProfiles
                               .find {
                                 it.processorPath == newProfile.processorPath
                                 && it.processorOptions == newProfile.processorOptions
                               } ?: newProfile.also { addModuleProcessorProfile(it) }

    }
  }


  companion object {
    private val LOG = Logger.getInstance(AnnotationProcessingDataService::class.java)
  }
}
