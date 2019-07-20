/*
 * Copyright 2000-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.plugins.gradle.tooling.util

import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.api.initialization.IncludedBuild
import org.gradle.api.invocation.Gradle
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.composite.internal.DefaultIncludedBuild
import org.gradle.util.GradleVersion
import org.jetbrains.annotations.NotNull
import org.jetbrains.plugins.gradle.tooling.ModelBuilderContext
import org.jetbrains.plugins.gradle.tooling.internal.ExtraModelBuilder

import static org.jetbrains.plugins.gradle.tooling.ModelBuilderContext.DataProvider

/**
 * @author Vladislav.Soroka
 */
@CompileStatic
class SourceSetCachedFinder {
  private static final DataProvider<Map<String, SourceSet>> ARTIFACTS_PROVIDER = new DataProvider<Map<String, SourceSet>>() {
    @NotNull
    @Override
    Map<String, SourceSet> create(@NotNull Gradle gradle) {
      return createArtifactsMap(gradle)
    }
  }
  private static final DataProvider<Map<String, Set<File>>> SOURCES_DATA_KEY = new DataProvider<Map<String, Set<File>>>() {
    @NotNull
    @Override
    Map<String, Set<File>> create(@NotNull Gradle gradle) {
      return new HashMap<String, Set<File>>()
    }
  }

  private Map<String, SourceSet> myArtifactsMap
  private Map<String, Set<File>> mySourcesMap

  @Deprecated
  SourceSetCachedFinder(@NotNull Project project) {
    def context = ExtraModelBuilder.CURRENT_CONTEXT.get()
    if (context != null) {
      init(context)
    }
    else {
      def extraProperties = project.rootProject.extensions.extraProperties
      def key = "$SourceSetCachedFinder.name${System.identityHashCode(SourceSetCachedFinder.class)}"
      if (extraProperties.has(key)) {
        def cached = extraProperties.get(key)
        if (cached instanceof SourceSetCachedFinder) {
          myArtifactsMap = (cached as SourceSetCachedFinder).myArtifactsMap
          mySourcesMap = (cached as SourceSetCachedFinder).mySourcesMap
          return
        }
      }
      myArtifactsMap = Collections.unmodifiableMap(createArtifactsMap(project.gradle))
      mySourcesMap = [:]
      extraProperties.set(key, this)
    }
  }

  SourceSetCachedFinder(@NotNull ModelBuilderContext context) {
    init(context)
  }

  private void init(@NotNull ModelBuilderContext context) {
    myArtifactsMap = context.getData(ARTIFACTS_PROVIDER)
    mySourcesMap = context.getData(SOURCES_DATA_KEY)
  }

  Set<File> findSourcesByArtifact(String path) {
    def sources = mySourcesMap[path]
    if (sources == null) {
      def sourceSet = myArtifactsMap[path]
      if (sourceSet != null) {
        sources = sourceSet.getAllJava().getSrcDirs()
        mySourcesMap[path] = sources
      }
    }
    return sources
  }

  SourceSet findByArtifact(String artifactPath) {
    myArtifactsMap[artifactPath]
  }

  private static HashMap<String, SourceSet> createArtifactsMap(@NotNull Gradle gradle) {
    def artifactsMap = new HashMap<String, SourceSet>()
    def projects = new ArrayList<Project>(gradle.rootProject.allprojects)
    def isCompositeBuildsSupported = GradleVersion.current() >= GradleVersion.version("3.1")
    if (isCompositeBuildsSupported) {
      projects = exposeIncludedBuilds(gradle, projects)
    }
    for (Project p : projects) {
      SourceSetContainer sourceSetContainer = JavaPluginUtil.getSourceSetContainer(p)
      if (sourceSetContainer == null || sourceSetContainer.isEmpty()) continue

      for (SourceSet sourceSet : sourceSetContainer) {
        def task = p.tasks.findByName(sourceSet.getJarTaskName())
        if (task instanceof AbstractArchiveTask) {
          AbstractArchiveTask jarTask = (AbstractArchiveTask)task
          def archivePath = jarTask?.getArchivePath()
          if (archivePath) {
            artifactsMap[archivePath.path] = sourceSet
          }
        }
      }
    }
    return artifactsMap
  }

  private static List<Project> exposeIncludedBuilds(Gradle gradle, List<Project> projects) {
    for (IncludedBuild includedBuild : gradle.includedBuilds) {
      if (includedBuild instanceof DefaultIncludedBuild) {
        def build = includedBuild as DefaultIncludedBuild
        projects += build.configuredBuild.rootProject.allprojects
      }
    }
    return projects
  }
}

