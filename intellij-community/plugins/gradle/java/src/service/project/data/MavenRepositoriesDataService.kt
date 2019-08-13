// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.project.data

import com.intellij.externalSystem.MavenRepositoryData
import com.intellij.jarRepository.RemoteRepositoriesConfiguration
import com.intellij.jarRepository.RemoteRepositoryDescription
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.Key
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.service.project.IdeModelsProvider
import com.intellij.openapi.externalSystem.service.project.manage.AbstractProjectDataService
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.gradle.settings.GradleSettings

class MavenRepositoriesDataService: AbstractProjectDataService<MavenRepositoryData, Void>() {
  override fun getTargetDataKey(): Key<MavenRepositoryData> = MavenRepositoryData.KEY

  override fun onSuccessImport(imported: MutableCollection<DataNode<MavenRepositoryData>>,
                               projectData: ProjectData?,
                               project: Project,
                               modelsProvider: IdeModelsProvider) {

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

    val repositoriesConfiguration = RemoteRepositoriesConfiguration.getInstance(project)

    val urlToConfig = repositoriesConfiguration.repositories.groupBy { it.url }.toMutableMap()

    imported.forEach {
      urlToConfig.putIfAbsent(it.data.url, listOf(RemoteRepositoryDescription(it.data.name, it.data.name, it.data.url)))
    }

    repositoriesConfiguration.repositories = urlToConfig.values.flatten()

    super.onSuccessImport(imported, projectData, project, modelsProvider)
  }
}