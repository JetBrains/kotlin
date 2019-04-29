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
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.composite.internal.DefaultIncludedBuild
import org.gradle.util.GradleVersion
import org.jetbrains.annotations.NotNull

/**
 * @author Vladislav.Soroka
 */
@CompileStatic
class SourceSetCachedFinder {
  private final Map<String, SourceSet> myArtifactsMap
  private final Map<String, Set<File>> mySourcesMap

  @SuppressWarnings("GrUnresolvedAccess")
  SourceSetCachedFinder(@NotNull Project project) {
    def rootProject = project.rootProject
    def extraProperties = rootProject.extensions.extraProperties
    def key = "$SourceSetCachedFinder.name${System.identityHashCode(SourceSetCachedFinder.class)}"

    if (extraProperties.has(key)) {
      def cached = extraProperties.get(key)
      if (cached instanceof SourceSetCachedFinder) {
        myArtifactsMap = (cached as SourceSetCachedFinder).myArtifactsMap
        mySourcesMap = (cached as SourceSetCachedFinder).mySourcesMap
        return
      }
    }

    def artifactsMap = new HashMap<String, SourceSet>()
    def projects = new ArrayList<Project>(rootProject.allprojects)
    def isCompositeBuildsSupported = GradleVersion.current() >= GradleVersion.version("3.1")
    if (isCompositeBuildsSupported) {
      projects = exposeIncludedBuilds(project, projects)
    }
    for (Project p : projects) {
      SourceSetContainer sourceSetContainer = getSourceSetContainer(p)
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

    myArtifactsMap = Collections.unmodifiableMap(artifactsMap)
    mySourcesMap = [:]
    extraProperties.set(key, this)
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

  private static List<Project> exposeIncludedBuilds(Project project, List<Project> projects) {
    for (IncludedBuild includedBuild : project.gradle.includedBuilds) {
      if (includedBuild instanceof DefaultIncludedBuild) {
        def build = includedBuild as DefaultIncludedBuild
        projects += build.configuredBuild.rootProject.allprojects
      }
    }
    return projects
  }

  SourceSet findByArtifact(String artifactPath) {
    myArtifactsMap[artifactPath]
  }

  static JavaPluginConvention getJavaPluginConvention(Project p) {
    p.convention.findPlugin(JavaPluginConvention)
  }

  static SourceSetContainer getSourceSetContainer(Project p) {
    getJavaPluginConvention(p)?.sourceSets
  }
}
