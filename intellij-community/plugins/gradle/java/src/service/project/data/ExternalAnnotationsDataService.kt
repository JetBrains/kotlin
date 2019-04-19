// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.project.data

import com.intellij.codeInsight.ExternalAnnotationsArtifactsResolver
import com.intellij.codeInsight.externalAnnotation.location.AnnotationsLocation
import com.intellij.codeInsight.externalAnnotation.location.AnnotationsLocationSearcher
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.Key
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.project.LibraryData
import com.intellij.openapi.externalSystem.model.project.LibraryLevel
import com.intellij.openapi.externalSystem.model.project.ModuleData
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.service.project.IdeModelsProvider
import com.intellij.openapi.externalSystem.service.project.manage.AbstractProjectDataService
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.externalSystem.util.ExternalSystemConstants
import com.intellij.openapi.externalSystem.util.Order
import com.intellij.openapi.progress.runBackgroundableTask
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.LibraryOrderEntry
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.util.registry.Registry
import org.jetbrains.plugins.gradle.model.data.GradleSourceSetData
import org.jetbrains.plugins.gradle.settings.GradleSettings

@Order(value = ExternalSystemConstants.UNORDERED)
class ExternalAnnotationsDataService: AbstractProjectDataService<LibraryData, Library>() {
  override fun getTargetDataKey(): Key<LibraryData> = ProjectKeys.LIBRARY

  override fun onSuccessImport(imported: MutableCollection<DataNode<LibraryData>>,
                               projectData: ProjectData?,
                               project: Project,
                               modelsProvider: IdeModelsProvider) {

    val resolver = ExternalAnnotationsArtifactsResolver.EP_NAME.extensionList.firstOrNull() ?: return
    val providedAnnotations = imported.mapNotNull {
      val libData = it.data
      val lib = modelsProvider.getLibraryByName(libData.internalName) ?: return@mapNotNull null
      lookForLocations(lib, libData)
    }.toMap()

    resolveProvidedAnnotations(providedAnnotations, resolver, project)

    if (!Registry.`is`("external.system.import.resolve.annotations")) {
      return
    }
    if (imported.isEmpty()) {
      return
    }

    projectData?.apply {
      val importRepositories =
        GradleSettings
          .getInstance(project)
          .linkedProjectsSettings
          .find { settings -> settings.externalProjectPath == linkedExternalProjectPath }
          ?.isResolveExternalAnnotations ?: false

      if (!importRepositories) {
        return
      }
    }

    val totalSize = imported.size

    runBackgroundableTask("Resolving external annotations", project) { indicator ->
      indicator.isIndeterminate = false
      imported.forEachIndexed { index, dataNode ->
        if (indicator.isCanceled) {
          return@runBackgroundableTask
        }
        val libraryData = dataNode.data
        val libraryName = libraryData.internalName
        val library = modelsProvider.getLibraryByName(libraryName)
        if (library != null) {
          indicator.text = "Looking for annotations for '$libraryName'"
          val mavenId = "${libraryData.groupId}:${libraryData.artifactId}:${libraryData.version}"
          resolver.resolve(project, library, mavenId)
        }
        indicator.fraction = (index + 1) / totalSize.toDouble()
      }
    }
  }
  companion object {
    val LOG = Logger.getInstance(ExternalAnnotationsDataService::class.java)
  }
}


class ExternalAnnotationsModuleLibrariesService: AbstractProjectDataService<ModuleData, Library>() {
  override fun getTargetDataKey(): Key<ModuleData> = ProjectKeys.MODULE

  override fun onSuccessImport(imported: MutableCollection<DataNode<ModuleData>>,
                               projectData: ProjectData?,
                               project: Project,
                               modelsProvider: IdeModelsProvider) {
    val resolver = ExternalAnnotationsArtifactsResolver.EP_NAME.extensionList.firstOrNull() ?: return

    val providedAnnotations = imported
      .flatMap { ExternalSystemApiUtil.findAll(it, GradleSourceSetData.KEY) + it }
      .flatMap { moduleNode ->
      ExternalSystemApiUtil.findAll(moduleNode, ProjectKeys.LIBRARY_DEPENDENCY)
        .filter { it.data.level == LibraryLevel.MODULE }
        .mapNotNull {
          val libData = it.data.target
          val lib = (modelsProvider.findIdeModuleOrderEntry(it.data) as? LibraryOrderEntry)?.library ?: return@mapNotNull null
          lookForLocations(lib, libData)
        }
    }.toMap()

    resolveProvidedAnnotations(providedAnnotations, resolver, project)
  }
}


fun lookForLocations(lib: Library, libData: LibraryData): Pair<Library, Collection<AnnotationsLocation>>? {
  val locations = AnnotationsLocationSearcher.findAnnotationsLocation(lib, libData.artifactId, libData.groupId, libData.version)
  return if (locations.isEmpty()) {
    null
  }
  else {
    lib to locations
  }
}

fun resolveProvidedAnnotations(providedAnnotations: Map<Library, Collection<AnnotationsLocation>>,
                               resolver: ExternalAnnotationsArtifactsResolver,
                               project: Project) {
  if (providedAnnotations.isNotEmpty()) {
    val total = providedAnnotations.map { it.value.size }.sum().toDouble()
    runBackgroundableTask("Resolving known external annotations") { indicator ->
      indicator.isIndeterminate = false
      var index = 0
      providedAnnotations.forEach { (lib, locations) ->
        indicator.text = "Looking for annotations for '${lib.name}'"
        locations.forEach { location ->
          resolver.resolve(project, lib, location)
          index++
          indicator.fraction = index / total
        }
      }
    }
  }
}