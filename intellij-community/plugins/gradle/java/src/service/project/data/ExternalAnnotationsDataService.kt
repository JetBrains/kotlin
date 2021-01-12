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
import org.jetbrains.plugins.gradle.model.data.GradleSourceSetData
import org.jetbrains.plugins.gradle.settings.GradleSettings
import org.jetbrains.plugins.gradle.util.GradleBundle

@Order(value = ExternalSystemConstants.UNORDERED)
class ExternalAnnotationsDataService: AbstractProjectDataService<LibraryData, Library>() {
  override fun getTargetDataKey(): Key<LibraryData> = ProjectKeys.LIBRARY

  override fun onSuccessImport(imported: MutableCollection<DataNode<LibraryData>>,
                               projectData: ProjectData?,
                               project: Project,
                               modelsProvider: IdeModelsProvider) {
    if (!shouldImportExternalAnnotations(projectData, project)) {
      return
    }

    val providedAnnotations = imported.mapNotNull {
      val libData = it.data
      val lib = modelsProvider.getLibraryByName(libData.internalName) ?: return@mapNotNull null
      lookForLocations(project, lib, libData)
    }.toMap()

    resolveProvidedAnnotations(providedAnnotations, project)
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
    if (!shouldImportExternalAnnotations(projectData, project)) {
      return
    }

    val providedAnnotations = imported
      .flatMap { ExternalSystemApiUtil.findAll(it, GradleSourceSetData.KEY) + it }
      .flatMap { moduleNode ->
      ExternalSystemApiUtil.findAll(moduleNode, ProjectKeys.LIBRARY_DEPENDENCY)
        .filter { it.data.level == LibraryLevel.MODULE }
        .mapNotNull {
          val libData = it.data.target
          val lib = (modelsProvider.findIdeModuleOrderEntry(it.data) as? LibraryOrderEntry)?.library ?: return@mapNotNull null
          lookForLocations(project, lib, libData)
        }
    }.toMap()

    resolveProvidedAnnotations(providedAnnotations, project)
  }
}


fun shouldImportExternalAnnotations(projectData: ProjectData?, project: Project): Boolean {
  if (projectData == null) {
    return false
  }

  val gradleSettings = GradleSettings.getInstance(project)
  if (gradleSettings.isOfflineWork) {
    return false
  }

  return gradleSettings
           .linkedProjectsSettings
           .find { settings -> settings.externalProjectPath == projectData.linkedExternalProjectPath }
           ?.isResolveExternalAnnotations ?: false
}

fun lookForLocations(project: Project, lib: Library, libData: LibraryData): Pair<Library, Collection<AnnotationsLocation>>? {
  val locations = AnnotationsLocationSearcher.findAnnotationsLocation(project, lib, libData.artifactId, libData.groupId, libData.version)
  return if (locations.isEmpty()) {
    null
  }
  else {
    lib to locations
  }
}

fun resolveProvidedAnnotations(providedAnnotations: Map<Library, Collection<AnnotationsLocation>>,
                               project: Project) {
  val locationsToSkip = mutableSetOf<AnnotationsLocation>()

  if (providedAnnotations.isNotEmpty()) {
    val total = providedAnnotations.map { it.value.size }.sum().toDouble()
    runBackgroundableTask(GradleBundle.message("gradle.tasks.annotations.title")) { indicator ->
      indicator.isIndeterminate = false
      val resolvers = ExternalAnnotationsArtifactsResolver.EP_NAME.extensionList
      var index = 0
      providedAnnotations.forEach { (lib, locations) ->
        indicator.text = GradleBundle.message("gradle.tasks.annotations.looking.for", lib.name)
        locations.forEach locations@ { location ->
          if (locationsToSkip.contains(location)) return@locations
          if (!resolvers.fold(false) { acc, res -> acc || res.resolve(project, lib, location) } ) {
             locationsToSkip.add(location)
          }
          index++
          indicator.fraction = index / total
        }
      }
    }
  }
}