// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.project

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.plugins.gradle.model.Build
import org.jetbrains.plugins.gradle.model.Project
import org.jetbrains.plugins.gradle.model.ProjectImportAction
import java.util.stream.Collectors
import java.util.stream.Stream

@ApiStatus.Internal
class ToolingModelsProviderImpl(private val models: ProjectImportAction.AllModels) : ToolingModelsProvider {
  private val projectsMap: Map<Project, Build> by lazy {
    builds()
      .flatMap { build -> build.projects.stream().map { it to build } }
      .collect(Collectors.toMap<Pair<Project, Build>, Project, Build>({ it.first }) { it.second })
  }

  override fun getRootBuild(): Build = models.mainBuild
  override fun getIncludedBuilds(): List<Build> = models.includedBuilds
  override fun <T> getModel(modelClazz: Class<T>): T? = models.getModel(modelClazz)
  override fun <T> getBuildModel(build: Build, modelClazz: Class<T>): T? = models.getModel(build, modelClazz)
  override fun <T> getProjectModel(project: Project, modelClazz: Class<T>): T? = models.getModel(project, modelClazz)
  override fun builds(): Stream<Build> = Stream.builder<Build>().add(models.mainBuild).apply {
    for (it in models.includedBuilds) {
      this.add(it)
    }
  }.build()

  override fun projects(): Stream<Project> = builds().map { it.projects }.flatMap(Collection<Project>::stream)
  override fun getBuild(project: Project): Build =
    projectsMap[project] ?: error("Build can not be found for the project: '${project.name}'")
}