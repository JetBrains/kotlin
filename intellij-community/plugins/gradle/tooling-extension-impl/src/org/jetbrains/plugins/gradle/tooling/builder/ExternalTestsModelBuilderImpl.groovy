// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.tooling.builder

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.testing.Test
import org.jetbrains.annotations.NotNull
import org.jetbrains.plugins.gradle.model.tests.DefaultExternalTestSourceMapping
import org.jetbrains.plugins.gradle.model.tests.DefaultExternalTestsModel
import org.jetbrains.plugins.gradle.model.tests.ExternalTestSourceMapping
import org.jetbrains.plugins.gradle.model.tests.ExternalTestsModel
import org.jetbrains.plugins.gradle.tooling.ErrorMessageBuilder
import org.jetbrains.plugins.gradle.tooling.ModelBuilderService
import org.jetbrains.plugins.gradle.tooling.util.JavaPluginUtil

import static org.codehaus.groovy.runtime.StringGroovyMethods.capitalize

@CompileStatic
class ExternalTestsModelBuilderImpl implements ModelBuilderService {
  @Override
  boolean canBuild(String modelName) {
    return ExternalTestsModel.name == modelName
  }

  @Override
  Object buildAll(String modelName, Project project) {
    def defaultTestsModel = new DefaultExternalTestsModel()
    if (javaPluginIsApplied(project)) {
      defaultTestsModel.sourceTestMappings = getMapping(project)
    }
    return defaultTestsModel
  }

  private static boolean javaPluginIsApplied(Project project) {
    return JavaPluginUtil.getJavaPluginConvention(project) != null
  }

  private static List<ExternalTestSourceMapping> getMapping(Project project) {
    def taskToClassesDirs = new LinkedHashMap<Test, Set<String>>()
    for (task in project.tasks.withType(Test.class)) {
      taskToClassesDirs.put(task, getClassesDirs(task))
    }

    def sourceSetContainer = JavaPluginUtil.getSourceSetContainer(project)
    if (sourceSetContainer == null) return Collections.emptyList()
    def classesDirToSourceDirs = new LinkedHashMap<String, Set<String>>()
    for (sourceSet in sourceSetContainer) {
      def sourceDirectorySet = sourceSet.allSource
      def sourceFolders = sourceDirectorySet.srcDirs.collect { it -> it.absolutePath }
      for (classDirectory in getPaths(sourceSet.output)) {
        def storedSourceFolders = classesDirToSourceDirs.get(classDirectory)
        if (storedSourceFolders == null) {
          storedSourceFolders = new LinkedHashSet<String>()
        }
        storedSourceFolders.addAll(sourceFolders)
        classesDirToSourceDirs.put(classDirectory, storedSourceFolders)
      }
    }
    def testSourceMappings = new ArrayList<ExternalTestSourceMapping>()
    for (entry in taskToClassesDirs.entrySet()) {
      def sourceFolders = new LinkedHashSet<String>()
      for (classDirectory in entry.value) {
        def storedSourceFolders = classesDirToSourceDirs.get(classDirectory)
        if (storedSourceFolders == null) continue
        for (folder in storedSourceFolders) sourceFolders.add(folder)
      }
      def task = entry.key
      def taskProjectPath = task.project.path == ":" ? "" : task.project.path
      def cleanTestTaskName = "clean" + capitalize(task.name)
      def defaultExternalTestSourceMapping = new DefaultExternalTestSourceMapping()
      defaultExternalTestSourceMapping.testName = task.name
      defaultExternalTestSourceMapping.testTaskPath = task.path
      defaultExternalTestSourceMapping.cleanTestTaskPath = taskProjectPath + ":" + cleanTestTaskName
      defaultExternalTestSourceMapping.sourceFolders = sourceFolders
      testSourceMappings.add(defaultExternalTestSourceMapping)
    }
    return testSourceMappings
  }

  @CompileDynamic
  private static Set<String> getClassesDirs(Test test) {
    def testClassesDirs = new LinkedHashSet()
    if (test.hasProperty("testClassesDirs")) {
      testClassesDirs.addAll(getPaths(test.testClassesDirs))
    }
    if (test.hasProperty("testClassesDir")) {
      def testClassesDir = test.testClassesDir?.absolutePath
      if (testClassesDir != null) {
        testClassesDirs.add(testClassesDir)
      }
    }
    return testClassesDirs
  }

  private static Set<String> getPaths(FileCollection files) {
    def paths = new LinkedHashSet<String>()
    for (def file : files.files) {
      paths.add(file.absolutePath)
    }
    return paths
  }

  @NotNull
  @Override
  ErrorMessageBuilder getErrorMessageBuilder(@NotNull Project project, @NotNull Exception e) {
    return ErrorMessageBuilder.create(project, e, "Tests model errors")
  }
}
