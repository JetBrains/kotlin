// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.project.data

import com.intellij.compiler.CompilerConfiguration
import com.intellij.compiler.CompilerConfigurationImpl
import com.intellij.ide.projectView.actions.MarkRootActionBase
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.Key
import com.intellij.openapi.externalSystem.model.project.ModuleData
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider
import com.intellij.openapi.externalSystem.service.project.manage.AbstractProjectDataService
import com.intellij.openapi.externalSystem.service.project.manage.SourceFolderManager
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.externalSystem.util.ExternalSystemConstants
import com.intellij.openapi.externalSystem.util.Order
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ContentEntry
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.roots.SourceFolder
import com.intellij.openapi.util.Computable
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFileManager
import org.jetbrains.jps.model.java.JavaSourceRootType
import org.jetbrains.jps.model.java.JpsJavaExtensionService
import org.jetbrains.jps.model.java.compiler.ProcessorConfigProfile
import org.jetbrains.jps.model.java.impl.compiler.ProcessorConfigProfileImpl
import org.jetbrains.plugins.gradle.model.data.AnnotationProcessingData
import org.jetbrains.plugins.gradle.settings.GradleSettings
import java.io.File
import java.util.*
import kotlin.collections.ArrayList

@Order(ExternalSystemConstants.UNORDERED)
class AnnotationProcessingDataService : AbstractProjectDataService<AnnotationProcessingData, ProcessorConfigProfile>() {

  override fun getTargetDataKey(): Key<AnnotationProcessingData> {
    return AnnotationProcessingData.KEY
  }

  override fun importData(toImport: Collection<DataNode<AnnotationProcessingData>>,
                          projectData: ProjectData?,
                          project: Project,
                          modifiableModelsProvider: IdeModifiableModelsProvider) {
    val importedData = mutableSetOf<AnnotationProcessingData>()
    val config = CompilerConfiguration.getInstance(project) as CompilerConfigurationImpl
    val sourceFolderManager = SourceFolderManager.getInstance(project)
    for (node in toImport) {
      val moduleData = node.parent?.data as? ModuleData
      if (moduleData == null) {
        LOG.debug("Failed to find parent module data in annotation processor data. Parent: ${node.parent} ")
        continue
      }

      val ideModule = modifiableModelsProvider.findIdeModule(moduleData)
      if (ideModule == null) {
        LOG.debug("Failed to find ide module for module data: ${moduleData}")
        continue
      }

      config.configureAnnotationProcessing(ideModule, node.data, importedData)

      if (projectData != null) {
        val isDelegatedBuild = GradleSettings.getInstance(project).getLinkedProjectSettings(
          projectData.linkedExternalProjectPath)?.delegatedBuild ?: true

        clearGeneratedSourceFolders(ideModule, node, modifiableModelsProvider)
        addGeneratedSourceFolders(ideModule, node, isDelegatedBuild, modifiableModelsProvider, sourceFolderManager)
      }
    }
  }

  private fun clearGeneratedSourceFolders(ideModule: Module,
                                          node: DataNode<AnnotationProcessingData>,
                                          modelsProvider: IdeModifiableModelsProvider) {
    val gradleOutputs = ExternalSystemApiUtil.findAll(node, AnnotationProcessingData.OUTPUT_KEY)

    val pathsToRemove =
      (gradleOutputs
         .map { it.data.outputPath } +
       listOf(
         getAnnotationProcessorGenerationPath(ideModule, false, modelsProvider),
         getAnnotationProcessorGenerationPath(ideModule, true, modelsProvider)
       ))
        .filterNotNull()

    pathsToRemove.forEach { path ->
      val url = VfsUtilCore.pathToUrl(path)
      val modifiableRootModel = modelsProvider.getModifiableRootModel(ideModule)

      val (entry, folder) = findContentEntryOrFolder(modifiableRootModel, url)

      if (entry != null) {
        if (folder != null) {
          entry.removeSourceFolder(folder)
        }

        if (entry.sourceFolders.isEmpty()) {
          modifiableRootModel.removeContentEntry(entry)
        }
      }
    }
  }

  private fun addGeneratedSourceFolders(ideModule: Module,
                                        node: DataNode<AnnotationProcessingData>,
                                        delegatedBuild: Boolean,
                                        modelsProvider: IdeModifiableModelsProvider,
                                        sourceFolderManager: SourceFolderManager) {

    if (delegatedBuild) {
      val outputs = ExternalSystemApiUtil.findAll(node, AnnotationProcessingData.OUTPUT_KEY)
      outputs.forEach {
        val outputPath = it.data.outputPath
        val isTestSource = it.data.isTestSources
        addGeneratedSourceFolder(ideModule, outputPath, isTestSource, modelsProvider, sourceFolderManager)
      }
    }
    else {
      val outputPath = getAnnotationProcessorGenerationPath(ideModule, false, modelsProvider)
      if (outputPath != null) {
        addGeneratedSourceFolder(ideModule, outputPath, false, modelsProvider, sourceFolderManager)
      }

      val testOutputPath = getAnnotationProcessorGenerationPath(ideModule, true, modelsProvider)
      if (testOutputPath != null) {
        addGeneratedSourceFolder(ideModule, testOutputPath, true, modelsProvider, sourceFolderManager)
      }
    }
  }


  private fun addGeneratedSourceFolder(ideModule: Module,
                                       path: String,
                                       isTest: Boolean,
                                       modelsProvider: IdeModifiableModelsProvider,
                                       sourceFolderManager: SourceFolderManager) {
    val type = if (isTest) JavaSourceRootType.TEST_SOURCE else JavaSourceRootType.SOURCE
    val url = VfsUtilCore.pathToUrl(path)
    val vf = LocalFileSystem.getInstance().refreshAndFindFileByPath(path)
    if (vf == null || !vf.exists()) {
      sourceFolderManager.addSourceFolder(ideModule, url, type)
      sourceFolderManager.setSourceFolderGenerated(url, true)
    } else {
      val modifiableRootModel = modelsProvider.getModifiableRootModel(ideModule)
      val contentEntry = MarkRootActionBase.findContentEntry(modifiableRootModel, vf)
                         ?: modifiableRootModel.addContentEntry(url)
      val properties = JpsJavaExtensionService.getInstance().createSourceRootProperties("", true)
      contentEntry.addSourceFolder(url, type, properties)
    }
  }

  private fun findContentEntryOrFolder(modifiableRootModel: ModifiableRootModel,
                                       url: String): Pair<ContentEntry?, SourceFolder?> {
    var entryVar: ContentEntry? = null
    var folderVar: SourceFolder? = null
    modifiableRootModel.contentEntries.forEach search@{ ce ->
      ce.sourceFolders.forEach { sf ->
        if (sf.url == url) {
          entryVar = ce
          folderVar = sf
          return@search
        }
      }
      if (ce.url == url) {
        entryVar = ce
      }
    }
    return entryVar to folderVar
  }

  private fun getAnnotationProcessorGenerationPath(ideModule: Module, forTests: Boolean,
                                                   modelsProvider: IdeModifiableModelsProvider): String? {
    val config = CompilerConfiguration.getInstance(ideModule.project).getAnnotationProcessingConfiguration(ideModule)
    val sourceDirName = config.getGeneratedSourcesDirectoryName(forTests)
      val roots = modelsProvider.getModifiableRootModel(ideModule).contentRootUrls
      if (roots.isEmpty()) {
        return null
      }
      if (roots.size > 1) {
        Arrays.sort(roots)
      }
      return if (StringUtil.isEmpty(sourceDirName)) VirtualFileManager.extractPath(roots[0])
      else VirtualFileManager.extractPath(roots[0]) + "/" + sourceDirName
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
      isOutputRelativeToContentRoot = true
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
    const val IMPORTED_PROFILE_NAME = "Gradle Imported"
  }
}
