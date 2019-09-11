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
import com.intellij.openapi.util.Computable
import org.jetbrains.jps.model.java.compiler.ProcessorConfigProfile
import org.jetbrains.jps.model.java.impl.compiler.ProcessorConfigProfileImpl
import org.jetbrains.plugins.gradle.model.data.AnnotationProcessingData
import java.io.File

@Order(ExternalSystemConstants.UNORDERED)
class AnnotationProcessingDataService : AbstractProjectDataService<AnnotationProcessingData, ProcessorConfigProfile>() {
  override fun getTargetDataKey(): Key<AnnotationProcessingData> {
    return AnnotationProcessingData.KEY
  }

  override fun importData(toImport: Collection<DataNode<AnnotationProcessingData>>,
                          projectData: ProjectData?,
                          project: Project,
                          modelsProvider: IdeModifiableModelsProvider) {
    val importedData = mutableSetOf<AnnotationProcessingData>()
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

      config.configureAnnotationProcessing(ideModule, node.data, importedData)
    }
  }

  override fun computeOrphanData(toImport: MutableCollection<DataNode<AnnotationProcessingData>>,
                                 projectData: ProjectData,
                                 project: Project,
                                 modelsProvider: IdeModifiableModelsProvider): Computable<MutableCollection<ProcessorConfigProfile>> =
    Computable {
      val config = CompilerConfiguration.getInstance(project) as CompilerConfigurationImpl
      val importedProcessingProfiles = ArrayList(toImport).asSequence()
        .map { it.data }
        .distinct()
        .map { createProcessorConfigProfile(it) }
        .toList()

      val orphans = config
        .moduleProcessorProfiles
        .filter { importedProcessingProfiles.none { imported -> imported.matches(it) } }
        .toMutableList()

      orphans
    }

  override fun removeData(toRemoveComputable: Computable<MutableCollection<ProcessorConfigProfile>>,
                          toIgnore: MutableCollection<DataNode<AnnotationProcessingData>>,
                          projectData: ProjectData,
                          project: Project,
                          modelsProvider: IdeModifiableModelsProvider) {
    val toRemove = toRemoveComputable.compute()
    val config = CompilerConfiguration.getInstance(project) as CompilerConfigurationImpl
    val newProfiles = config
      .moduleProcessorProfiles
      .toMutableList()
      .apply { removeAll(toRemove) }
    config.setModuleProcessorProfiles(newProfiles)
  }

  private fun CompilerConfigurationImpl.configureAnnotationProcessing(ideModule: Module,
                                                                      data: AnnotationProcessingData,
                                                                      importedData: MutableSet<AnnotationProcessingData>) {
    val profile = findOrCreateProcessorConfigProfile(data)
    if (importedData.add(data)) {
      profile.clearModuleNames()
    }

    with(profile) {
      isEnabled = true
      isObtainProcessorsFromClasspath = false
      addModuleName(ideModule.name)
    }
  }

  private fun CompilerConfigurationImpl.findOrCreateProcessorConfigProfile(data: AnnotationProcessingData): ProcessorConfigProfile {
    val newProfile = createProcessorConfigProfile(data)
    return this.moduleProcessorProfiles
             .find { existing -> existing.matches(newProfile) }
           ?: newProfile.also { addModuleProcessorProfile(it) }
  }

  private fun createProcessorConfigProfile(annotationProcessingData: AnnotationProcessingData): ProcessorConfigProfileImpl {
    val newProfile = ProcessorConfigProfileImpl(IMPORTED_PROFILE_NAME)
    newProfile.setProcessorPath(annotationProcessingData.path.joinToString(separator = File.pathSeparator))
    annotationProcessingData.arguments
      .map { it.removePrefix("-A").split('=', limit = 2) }
      .forEach { newProfile.setOption(it[0], if (it.size > 1) it[1] else "") }
    return newProfile
  }

  private fun ProcessorConfigProfile.matches(other: ProcessorConfigProfile): Boolean {
    return this.name == other.name
           && this.processorPath == other.processorPath
           && this.processorOptions == other.processorOptions
  }

  companion object {
    private val LOG = Logger.getInstance(AnnotationProcessingDataService::class.java)
    val IMPORTED_PROFILE_NAME = "Gradle Imported"
  }
}
