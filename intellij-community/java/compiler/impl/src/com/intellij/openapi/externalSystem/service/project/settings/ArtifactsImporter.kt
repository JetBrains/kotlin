// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.service.project.settings

import com.intellij.execution.BeforeRunTask
import com.intellij.execution.BeforeRunTaskProvider
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.externalSystem.model.project.settings.ConfigurationData
import com.intellij.openapi.externalSystem.project.PackagingModifiableModel
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider
import com.intellij.openapi.module.ModulePointer
import com.intellij.openapi.module.ModulePointerManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar
import com.intellij.packaging.artifacts.*
import com.intellij.packaging.elements.CompositePackagingElement
import com.intellij.packaging.elements.PackagingElement
import com.intellij.packaging.impl.artifacts.PlainArtifactType
import com.intellij.packaging.impl.elements.*
import com.intellij.packaging.impl.run.BuildArtifactsBeforeRunTask
import com.intellij.packaging.impl.run.BuildArtifactsBeforeRunTaskProvider
import com.intellij.util.ObjectUtils.consumeIfCast

class ArtifactsImporter: ConfigurationHandler {
  private val LOG = Logger.getInstance(ArtifactsImporter::class.java)

  override fun apply(project: Project, modelsProvider: IdeModifiableModelsProvider, configuration: ConfigurationData) {
    val artifacts = configuration.find("ideArtifacts") as? List<*> ?: return

    if (artifacts.isEmpty()) {
      return
    }

    val packagingModifiableModel = modelsProvider.getModifiableModel(PackagingModifiableModel::class.java)
    val modifiableModel = packagingModifiableModel.modifiableArtifactModel
    val postponedOps: MutableList<(ModifiableArtifactModel) -> Unit> = mutableListOf()

    artifacts.forEach { value ->
      val artifactConfig = value as? Map<*, *> ?: return@forEach
      val name = artifactConfig["name"]
      if (name !is String) return@forEach
      val type: ArtifactType = PlainArtifactType.getInstance()
      val artifact: ModifiableArtifact = modifiableModel
                                           .findArtifact(name)?.let { modifiableModel.getOrCreateModifiableArtifact(it) }
                                         ?: modifiableModel.addArtifact(name, type)
      val rootElement = type.createRootElement(name)
      populateArtifact(project, modelsProvider, rootElement, artifactConfig, postponedOps)
      artifact.rootElement = rootElement
    }

    postponedOps.forEach { it.invoke(modifiableModel) }
  }

  private fun populateArtifact(project: Project,
                               modelsProvider: IdeModifiableModelsProvider,
                               element: CompositePackagingElement<*>,
                               config: Map<*, *>,
                               postponedOps: MutableList<(ModifiableArtifactModel) -> Unit>) {
    fun createModulePointer(moduleName: String): ModulePointer {
      val module = modelsProvider.findIdeModule(moduleName)
      return if (module != null) {
        ModulePointerManager.getInstance(project).create(module)
      } else {
        LOG.warn("Artifact `${element.name}`: unable to find module `$moduleName`");
        ModulePointerManager.getInstance(project).create(moduleName)
      }
    }

    fun addModuleElement(config: Map<*, *>, provider: (ModulePointer) -> PackagingElement<*>) {
      val moduleName = config["moduleName"] as? String
      if (moduleName == null) {
        LOG.warn("Artifact `${element.name}`: moduleName missed for $config")
        return
      }

      val modulePointer = createModulePointer(moduleName)
      val child = provider(modulePointer)
      element.addOrFindChild(child)
    }

    consumeIfCast(config["children"], List::class.java) { children ->
      children.forEach { child ->
        consumeIfCast(child, Map::class.java) child@{ config ->
          val type = config["type"] as? String ?: return@child
          when (type) {
            "ARTIFACT" -> {  }

            "DIR" -> {
              val directory = DirectoryPackagingElement(config["name"] as? String ?: "no_name")
              populateArtifact(project, modelsProvider, directory, config, postponedOps)
              element.addOrFindChild(directory)
            }

            "ARCHIVE" -> {
              val archive = ArchivePackagingElement(config["name"] as? String ?: "no_name")
              populateArtifact(project, modelsProvider, archive, config, postponedOps)
              element.addOrFindChild(archive)
            }

            "LIBRARY_FILES" -> {
              consumeIfCast(config["libraries"], List::class.java) {
                it.forEach { libraryCoordinates ->
                  consumeIfCast(libraryCoordinates, Map::class.java) { library ->
                    val nameSuffix = "${library["group"]}:${library["artifact"]}:${library["version"]}"
                    project.findLibraryByNameSuffix(nameSuffix)?.let {
                      val libraryClasses = LibraryPackagingElement(LibraryTablesRegistrar.PROJECT_LEVEL, it.name, null)
                      element.addOrFindChild(libraryClasses)
                    }
                  }
                }
              }
            }

            "MODULE_OUTPUT" -> addModuleElement(config) { ProductionModuleOutputPackagingElement(project, it) }

            "MODULE_TEST_OUTPUT" -> addModuleElement(config) { TestModuleOutputPackagingElement(project, it) }

            "MODULE_SRC" ->  addModuleElement(config) { ProductionModuleSourcePackagingElement(project, it) }

            "FILE" -> {
              consumeIfCast(config["sourceFiles"], List::class.java) {
                it.filterIsInstance(String::class.java).forEach { path ->
                  val fileCopy = FileCopyPackagingElement(path)
                  element.addOrFindChild(fileCopy)
                }
              }
            }

            "DIR_CONTENT" ->  {
              consumeIfCast(config["sourceFiles"], List::class.java) {
                it.filterIsInstance(String::class.java).forEach { path ->
                  val dirCopy = DirectoryCopyPackagingElement(path)
                  element.addOrFindChild(dirCopy)
                }
              }
            }

            "EXTRACTED_DIR" ->  {
              consumeIfCast(config["sourceFiles"], List::class.java) {
                it.filterIsInstance(String::class.java).forEach { path ->
                  val fileCopy = ExtractedDirectoryPackagingElement(path,"/")
                  element.addOrFindChild(fileCopy)
                }
              }
            }

            "ARTIFACT_REF"  -> {
              val artifactName = config["artifactName"] as? String ?: return@child
              postponedOps.add { model ->
                model.findArtifact(artifactName)?.let {
                  val pointer = ArtifactPointerManager.getInstance(project).createPointer(it)
                  val artifactRef = ArtifactPackagingElement(project, pointer)
                  element.addOrFindChild(artifactRef)
                }
              }
            }

            else -> LOG.warn("Artifact `${element.name}`: unsupported artifact type `$type` in $config")
          }
        }
      }
    }
  }

  private fun Project.findLibraryByNameSuffix(nameSuffix: String): Library? {
    val libraryTable = LibraryTablesRegistrar.getInstance().getLibraryTable(this@findLibraryByNameSuffix)
    return libraryTable.libraries.find { it.name?.endsWith(nameSuffix) ?: false }
  }
}

class BuildArtifactsTaskImporter: BeforeRunTaskImporter {
  override fun process(project: Project,
                       modelsProvider: IdeModifiableModelsProvider,
                       runConfiguration: RunConfiguration,
                       beforeRunTasks: MutableList<BeforeRunTask<*>>,
                       cfg: MutableMap<String, Any>): MutableList<BeforeRunTask<*>> {

    consumeIfCast(cfg["artifactName"], String::class.java) { artifactName ->
      val artifact = ArtifactManager.getInstance(project).findArtifact(artifactName)
      val hasTask = beforeRunTasks
        .filterIsInstance<BuildArtifactsBeforeRunTask>()
        .any { it.artifactPointers.any { it.artifactName == artifactName } }

      if (!hasTask && artifact != null) {
        val provider = BeforeRunTaskProvider.getProvider(project, BuildArtifactsBeforeRunTaskProvider.ID) ?: return@consumeIfCast
        val task = provider.createTask(runConfiguration) ?: return@consumeIfCast
        task.addArtifact(artifact)
        beforeRunTasks.add(task)
      }
    }
    return beforeRunTasks
  }

  override fun canImport(typeName: String): Boolean = "buildArtifact" == typeName

}