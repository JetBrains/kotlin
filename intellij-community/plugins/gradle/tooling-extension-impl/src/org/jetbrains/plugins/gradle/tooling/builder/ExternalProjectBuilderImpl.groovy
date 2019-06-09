// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.tooling.builder

import com.google.gson.GsonBuilder
import com.intellij.openapi.externalSystem.model.project.ExternalSystemSourceType
import org.gradle.api.Action
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.ContentFilterable
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileCopyDetails
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.util.PatternFilterable
import org.gradle.plugins.ide.idea.IdeaPlugin
import org.gradle.util.GUtil
import org.gradle.util.GradleVersion
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import org.jetbrains.plugins.gradle.model.*
import org.jetbrains.plugins.gradle.tooling.ErrorMessageBuilder
import org.jetbrains.plugins.gradle.tooling.ModelBuilderService
import org.jetbrains.plugins.gradle.tooling.util.SourceSetCachedFinder
import org.jetbrains.plugins.gradle.tooling.util.resolve.DependencyResolverImpl

/**
 * @author Vladislav.Soroka
 */
class ExternalProjectBuilderImpl implements ModelBuilderService {

  private static is4OrBetter = GradleVersion.current().baseVersion >= GradleVersion.version("4.0")

  @Override
  boolean canBuild(String modelName) {
    return ExternalProject.name == modelName || ExternalProjectPreview.name == modelName
  }

  @Nullable
  @Override
  Object buildAll(final String modelName, final Project project) {
    def cache = getOrSetExt(project, 'projects cache: ' + ExternalProject.name, { new HashMap<Project, ExternalProject>()}) as Map<Project, ExternalProject>
    def tasksFactory = getOrSetExt(project , 'tasks cache: ' + ExternalProject.name,  { new TasksFactory()}) as TasksFactory
    def sourceSetFinder = getOrSetExt(project , 'sourceSets finder: ' + ExternalProject.name,  { new SourceSetCachedFinder(project)}) as SourceSetCachedFinder
    return doBuild(modelName, project, cache, tasksFactory, sourceSetFinder)
  }

  private static getOrSetExt(final Project project, String name, Closure<Object> valueProvider) {
    def rootProject = project.getRootProject()
    def extraProperties = rootProject.extensions.extraProperties
    if(!extraProperties.has(name)) {
      extraProperties.set(name, valueProvider())
    }
    return extraProperties.get(name)
  }

  @Nullable
  private static Object doBuild(final String modelName,
                                final Project project,
                                Map<Project, ExternalProject> cache,
                                TasksFactory tasksFactory,
                                SourceSetCachedFinder sourceSetFinder) {
    ExternalProject externalProject = cache[project]
    if (externalProject != null) return externalProject

    def resolveSourceSetDependencies = System.properties.'idea.resolveSourceSetDependencies' as boolean
    def isPreview = ExternalProjectPreview.name == modelName
    DefaultExternalProject defaultExternalProject = new DefaultExternalProject()
    defaultExternalProject.externalSystemId = "GRADLE"
    defaultExternalProject.name = project.name
    def qName = ":" == project.path ? project.name : project.path
    defaultExternalProject.QName = qName
    final IdeaPlugin ideaPlugin = project.getPlugins().findPlugin(IdeaPlugin.class)
    def ideaPluginModule = ideaPlugin?.model?.module
    def parentBuildRootProject = project.gradle.parent?.rootProject
    def compositePrefix = parentBuildRootProject && !project.rootProject.is(parentBuildRootProject) && ":" != project.path ?
                          (ideaPlugin?.model?.project?.name ?: project.rootProject.name) : ""
    def ideaModuleName = ideaPluginModule?.name ?: project.name
    defaultExternalProject.id = compositePrefix + (":" == project.path ? ideaModuleName : qName)
    defaultExternalProject.version = wrap(project.version)
    defaultExternalProject.description = project.description
    defaultExternalProject.buildDir = project.buildDir
    defaultExternalProject.buildFile = project.buildFile
    defaultExternalProject.group = wrap(project.group)
    defaultExternalProject.projectDir = project.projectDir
    defaultExternalProject.sourceSets = getSourceSets(project, isPreview, resolveSourceSetDependencies, sourceSetFinder)
    defaultExternalProject.tasks = getTasks(project, tasksFactory)

    defaultExternalProject.plugins = getPlugins(project)
    //defaultExternalProject.setProperties(project.getProperties())

    addArtifactsData(project, defaultExternalProject)

    final Map<String, DefaultExternalProject> childProjects = new HashMap<String, DefaultExternalProject>(project.getChildProjects().size())
    for (Map.Entry<String, Project> projectEntry : project.getChildProjects().entrySet()) {
      final Object externalProjectChild = doBuild(modelName, projectEntry.getValue(), cache, tasksFactory, sourceSetFinder)
      if (externalProjectChild instanceof DefaultExternalProject) {
        childProjects.put(projectEntry.getKey(), (DefaultExternalProject)externalProjectChild)
      }
      else if (externalProjectChild instanceof ExternalProject) {
        // convert from proxy to our model class
        childProjects.put(projectEntry.getKey(), new DefaultExternalProject((ExternalProject)externalProjectChild))
      }
    }
    defaultExternalProject.setChildProjects(childProjects)
    cache.put(project, defaultExternalProject)

    defaultExternalProject
  }

  static void addArtifactsData(final Project project, DefaultExternalProject externalProject) {
    final List<File> artifacts = new ArrayList<File>()
    for (Task task : project.getTasks()) {
      if (task instanceof Jar) {
        Jar jar = (Jar)task
        try {
          // TODO use getArchiveFile method since Gradle 5.1
          artifacts.add(jar.getArchivePath())
        }
        catch (e) {
          // TODO add reporting for such issues
          project.getLogger().error("warning: [task $jar.path] $e.message")
        }
      }
    }
    externalProject.setArtifacts(artifacts)

    def configurationsByName = project.getConfigurations().getAsMap()
    Map<String, Set<File>> artifactsByConfiguration = new HashMap<String, Set<File>>()
    for (Map.Entry<String, Configuration> configurationEntry : configurationsByName.entrySet()) {
      Set<File> files = configurationEntry.getValue().getArtifacts().getFiles().getFiles()
      artifactsByConfiguration.put(configurationEntry.getKey(), new LinkedHashSet<>(files))
    }
    externalProject.setArtifactsByConfiguration(artifactsByConfiguration)
  }

  static Map<String, ExternalPlugin> getPlugins(Project project) {
    def result = [:] as Map<String, ExternalPlugin>
    project.convention.plugins.each { key, value ->
      ExternalPlugin externalPlugin = new DefaultExternalPlugin()
      externalPlugin.id = key
      result.put(key, externalPlugin)
    }

    result
  }

  static Map<String, DefaultExternalTask> getTasks(Project project, TasksFactory tasksFactory) {
    def result = [:] as Map<String, DefaultExternalTask>

    tasksFactory.getTasks(project).each { Task task ->
      DefaultExternalTask externalTask = result.get(task.name)
      if (externalTask == null) {
        externalTask = new DefaultExternalTask()
        externalTask.name = task.name
        externalTask.QName = task.name
        externalTask.description = task.description
        externalTask.group = task.group ?: "other"
        def ext = task.getExtensions()?.extraProperties
        externalTask.test = (task instanceof Test) || (ext?.has("idea.internal.test") && Boolean.valueOf(ext.get("idea.internal.test")))
        externalTask.type = ProjectExtensionsDataBuilderImpl.getType(task)
        result.put(externalTask.name, externalTask)
      }

      def projectTaskPath = (project.path == ':' ? ':' : project.path + ':') + task.name
      if (projectTaskPath.equals(task.path)) {
        externalTask.QName = task.path
      }
    }
    result
  }

  private static Map<String, ExternalSourceSet> getSourceSets(Project project, boolean isPreview, boolean resolveSourceSetDependencies, SourceSetCachedFinder sourceSetFinder) {
    final IdeaPlugin ideaPlugin = project.getPlugins().findPlugin(IdeaPlugin.class)
    def ideaPluginModule = ideaPlugin?.model?.module
    boolean inheritOutputDirs = ideaPluginModule?.inheritOutputDirs ?: false
    def ideaPluginOutDir = ideaPluginModule?.outputDir
    def ideaPluginTestOutDir = ideaPluginModule?.testOutputDir
    def generatedSourceDirs = null
    def ideaSourceDirs = null
    def ideaResourceDirs = null
    def ideaTestSourceDirs = null
    def ideaTestResourceDirs = null
    def downloadJavadoc = false
    def downloadSources = true

    if (ideaPluginModule) {
      generatedSourceDirs = ideaPluginModule.hasProperty("generatedSourceDirs") ? new LinkedHashSet<>(ideaPluginModule.generatedSourceDirs) : null
      ideaSourceDirs = new LinkedHashSet<>(ideaPluginModule.sourceDirs)
      ideaResourceDirs = ideaPluginModule.hasProperty("resourceDirs") ? new LinkedHashSet<>(ideaPluginModule.resourceDirs) : []
      ideaTestSourceDirs = new LinkedHashSet<>(ideaPluginModule.testSourceDirs)
      ideaTestResourceDirs = ideaPluginModule.hasProperty("testResourceDirs") ? new LinkedHashSet<>(ideaPluginModule.testResourceDirs) : []
      downloadJavadoc = ideaPluginModule.downloadJavadoc
      downloadSources = ideaPluginModule.downloadSources
    }

    def projectSourceCompatibility
    def projectTargetCompatibility

    //noinspection GrUnresolvedAccess
    if(project.hasProperty('sourceCompatibility') && project.sourceCompatibility instanceof JavaVersion) {
      //noinspection GrUnresolvedAccess
      projectSourceCompatibility = project.sourceCompatibility.toString()
    }
    //noinspection GrUnresolvedAccess
    if(project.hasProperty('targetCompatibility') && project.targetCompatibility instanceof JavaVersion) {
      //noinspection GrUnresolvedAccess
      projectTargetCompatibility = project.targetCompatibility.toString()
    }

    def result = [:] as Map<String, ExternalSourceSet>
    //noinspection GrUnresolvedAccess
    if (!project.hasProperty("sourceSets") || !(project.sourceSets instanceof SourceSetContainer)) {
      return result
    }
    //noinspection GrUnresolvedAccess
    def sourceSets = project.sourceSets as SourceSetContainer

    // ignore inherited source sets from parent project
    def parentProject = project.parent
    if (parentProject && parentProject.hasProperty("sourceSets") && parentProject.sourceSets instanceof SourceSetContainer) {
      if(sourceSets.is(parentProject.sourceSets)){
        return result
      }
    }

    def (resourcesIncludes, resourcesExcludes, filterReaders) = getFilters(project, 'processResources')
    def (testResourcesIncludes, testResourcesExcludes, testFilterReaders) = getFilters(project, 'processTestResources')
    //def (javaIncludes,javaExcludes) = getFilters(project,'compileJava')

    def additionalIdeaGenDirs = [] as Collection<File>
    if(generatedSourceDirs && !generatedSourceDirs.isEmpty()) {
      additionalIdeaGenDirs.addAll(generatedSourceDirs)
    }
    sourceSets.all { SourceSet sourceSet ->
      ExternalSourceSet externalSourceSet = new DefaultExternalSourceSet()
      externalSourceSet.name = sourceSet.name

      def javaCompileTask = project.tasks.findByName(sourceSet.compileJavaTaskName)
      if(javaCompileTask instanceof JavaCompile) {
        externalSourceSet.sourceCompatibility = javaCompileTask.sourceCompatibility ?: projectSourceCompatibility
        externalSourceSet.targetCompatibility = javaCompileTask.targetCompatibility ?: projectTargetCompatibility
      } else {
        externalSourceSet.sourceCompatibility = projectSourceCompatibility
        externalSourceSet.targetCompatibility = projectTargetCompatibility
      }

      def jarTask = project.tasks.findByName(sourceSet.jarTaskName)
      if(jarTask instanceof AbstractArchiveTask) {
        externalSourceSet.artifacts = [jarTask.archivePath]
      }

      def sources = [:] as Map<ExternalSystemSourceType, DefaultExternalSourceDirectorySet>
      ExternalSourceDirectorySet resourcesDirectorySet = new DefaultExternalSourceDirectorySet()
      resourcesDirectorySet.name = sourceSet.resources.name
      resourcesDirectorySet.srcDirs = sourceSet.resources.srcDirs
      if(ideaPluginOutDir && SourceSet.MAIN_SOURCE_SET_NAME == sourceSet.name) {
        resourcesDirectorySet.addGradleOutputDir(ideaPluginOutDir)
      }
      if (ideaPluginTestOutDir && SourceSet.TEST_SOURCE_SET_NAME == sourceSet.name) {
        resourcesDirectorySet.addGradleOutputDir(ideaPluginTestOutDir)
      }
      if (is4OrBetter) {
        if (sourceSet.output.resourcesDir) {
          resourcesDirectorySet.addGradleOutputDir(sourceSet.output.resourcesDir)
        }
        else {
          for (File outDir : sourceSet.output.classesDirs.files) {
            resourcesDirectorySet.addGradleOutputDir(outDir)
          }
          if (resourcesDirectorySet.gradleOutputDirs.isEmpty()) {
            resourcesDirectorySet.addGradleOutputDir(project.buildDir)
          }
        }
      }
      else {
        resourcesDirectorySet.addGradleOutputDir(chooseNotNull(
          sourceSet.output.resourcesDir, sourceSet.output.classesDir, project.buildDir))
      }

      def ideaOutDir = new File(project.projectDir, "out/" + (SourceSet.MAIN_SOURCE_SET_NAME == sourceSet.name ||
                                                              (!resolveSourceSetDependencies && SourceSet.TEST_SOURCE_SET_NAME !=
                                                               sourceSet.name) ? "production" : GUtil.toLowerCamelCase(sourceSet.name)))
      resourcesDirectorySet.outputDir = new File(ideaOutDir, "resources")
      resourcesDirectorySet.inheritedCompilerOutput = inheritOutputDirs

      ExternalSourceDirectorySet javaDirectorySet = new DefaultExternalSourceDirectorySet()
      javaDirectorySet.name = sourceSet.allJava.name
      javaDirectorySet.srcDirs = sourceSet.allJava.srcDirs
      if(ideaPluginOutDir && SourceSet.MAIN_SOURCE_SET_NAME == sourceSet.name) {
        javaDirectorySet.addGradleOutputDir(ideaPluginOutDir)
      }
      if (ideaPluginTestOutDir && SourceSet.TEST_SOURCE_SET_NAME == sourceSet.name) {
        javaDirectorySet.addGradleOutputDir(ideaPluginTestOutDir)
      }
      if (is4OrBetter) {
        for (File outDir : sourceSet.output.classesDirs.files) {
          javaDirectorySet.addGradleOutputDir(outDir)
        }
        if (javaDirectorySet.gradleOutputDirs.isEmpty()) {
          javaDirectorySet.addGradleOutputDir(project.buildDir)
        }
      }
      else {
        javaDirectorySet.addGradleOutputDir(chooseNotNull(sourceSet.output.classesDir, project.buildDir))
      }

      javaDirectorySet.outputDir = new File(ideaOutDir, "classes")
      javaDirectorySet.inheritedCompilerOutput = inheritOutputDirs
//      javaDirectorySet.excludes = javaExcludes + sourceSet.java.excludes;
//      javaDirectorySet.includes = javaIncludes + sourceSet.java.includes;

      ExternalSourceDirectorySet generatedDirectorySet = null
      def hasExplicitlyDefinedGeneratedSources = generatedSourceDirs && !generatedSourceDirs.isEmpty()
      FileCollection generatedSourcesOutput = sourceSet.output.hasProperty("generatedSourcesDirs") ? sourceSet.output.generatedSourcesDirs : null
      def hasAnnotationProcessorClasspath = sourceSet.hasProperty("annotationProcessorPath") && !isEmpty(sourceSet.annotationProcessorPath)
      if (hasExplicitlyDefinedGeneratedSources || hasAnnotationProcessorClasspath) {

        def files = new HashSet<File>()
        if (hasAnnotationProcessorClasspath && generatedSourcesOutput != null) {
          files.addAll(generatedSourcesOutput.files)
        }
        for(File file : generatedSourceDirs) {
          if(javaDirectorySet.srcDirs.contains(file)) {
            files.add(file)
          }
        }

        if (!files.isEmpty()) {
          javaDirectorySet.srcDirs.removeAll(files)
          generatedDirectorySet = new DefaultExternalSourceDirectorySet()
          generatedDirectorySet.name = "generated " + javaDirectorySet.name
          generatedDirectorySet.srcDirs = files
          for (file in javaDirectorySet.gradleOutputDirs) {
            generatedDirectorySet.addGradleOutputDir(file)
          }
          generatedDirectorySet.outputDir = javaDirectorySet.outputDir
          generatedDirectorySet.inheritedCompilerOutput = javaDirectorySet.isCompilerOutputPathInherited()
        }
        additionalIdeaGenDirs.removeAll(files)
      }

      if (SourceSet.TEST_SOURCE_SET_NAME == sourceSet.name) {
        if (!inheritOutputDirs && ideaPluginTestOutDir != null) {
          javaDirectorySet.outputDir = ideaPluginTestOutDir
          resourcesDirectorySet.outputDir = ideaPluginTestOutDir
        }
        resourcesDirectorySet.excludes = testResourcesExcludes + sourceSet.resources.excludes
        resourcesDirectorySet.includes = testResourcesIncludes + sourceSet.resources.includes
        resourcesDirectorySet.filters = testFilterReaders
        sources.put(ExternalSystemSourceType.TEST, javaDirectorySet)
        sources.put(ExternalSystemSourceType.TEST_RESOURCE, resourcesDirectorySet)
        if (generatedDirectorySet) {
          sources.put(ExternalSystemSourceType.TEST_GENERATED, generatedDirectorySet)
        }
      }
      else {
        boolean isTestSourceSet = false
        if (!inheritOutputDirs && resolveSourceSetDependencies && SourceSet.MAIN_SOURCE_SET_NAME != sourceSet.name
          && ideaTestSourceDirs && (ideaTestSourceDirs as Collection).containsAll(javaDirectorySet.srcDirs)) {
          javaDirectorySet.outputDir = ideaPluginTestOutDir ?: new File(project.projectDir, "out/test/classes")
          resourcesDirectorySet.outputDir = ideaPluginTestOutDir ?: new File(project.projectDir, "out/test/resources")
          sources.put(ExternalSystemSourceType.TEST, javaDirectorySet)
          sources.put(ExternalSystemSourceType.TEST_RESOURCE, resourcesDirectorySet)
          isTestSourceSet = true
        }
        else if (!inheritOutputDirs && ideaPluginOutDir != null) {
          javaDirectorySet.outputDir = ideaPluginOutDir
          resourcesDirectorySet.outputDir = ideaPluginOutDir
        }

        resourcesDirectorySet.excludes = resourcesExcludes + sourceSet.resources.excludes
        resourcesDirectorySet.includes = resourcesIncludes + sourceSet.resources.includes
        resourcesDirectorySet.filters = filterReaders
        if(!isTestSourceSet) {
          sources.put(ExternalSystemSourceType.SOURCE, javaDirectorySet)
          sources.put(ExternalSystemSourceType.RESOURCE, resourcesDirectorySet)
        }

        if(!resolveSourceSetDependencies && ideaTestSourceDirs) {
          def testDirs = javaDirectorySet.srcDirs.intersect(ideaTestSourceDirs as Collection)
          if(!testDirs.isEmpty()) {
            javaDirectorySet.srcDirs.removeAll(ideaTestSourceDirs)

            def testDirectorySet = new DefaultExternalSourceDirectorySet()
            testDirectorySet.name = javaDirectorySet.name
            testDirectorySet.srcDirs = testDirs
            testDirectorySet.addGradleOutputDir(javaDirectorySet.outputDir)
            testDirectorySet.outputDir = ideaPluginTestOutDir ?: new File(project.projectDir, "out/test/classes")
            testDirectorySet.inheritedCompilerOutput = javaDirectorySet.isCompilerOutputPathInherited()
            sources.put(ExternalSystemSourceType.TEST, testDirectorySet)
          }

          def testResourcesDirs = resourcesDirectorySet.srcDirs.intersect(ideaTestSourceDirs as Collection)
          if(!testResourcesDirs.isEmpty()) {
            resourcesDirectorySet.srcDirs.removeAll(ideaTestSourceDirs)

            def testResourcesDirectorySet = new DefaultExternalSourceDirectorySet()
            testResourcesDirectorySet.name = resourcesDirectorySet.name
            testResourcesDirectorySet.srcDirs = testResourcesDirs
            testResourcesDirectorySet.addGradleOutputDir(resourcesDirectorySet.outputDir)
            testResourcesDirectorySet.outputDir = ideaPluginTestOutDir ?: new File(project.projectDir, "out/test/resources")
            testResourcesDirectorySet.inheritedCompilerOutput = resourcesDirectorySet.isCompilerOutputPathInherited()
            sources.put(ExternalSystemSourceType.TEST_RESOURCE, testResourcesDirectorySet)
          }
        }

        if (generatedDirectorySet) {
          sources.put(ExternalSystemSourceType.SOURCE_GENERATED, generatedDirectorySet)
          if(!resolveSourceSetDependencies && ideaTestSourceDirs) {
            def testGeneratedDirs = generatedDirectorySet.srcDirs.intersect(ideaTestSourceDirs as Collection)
            if(!testGeneratedDirs.isEmpty()) {
              generatedDirectorySet.srcDirs.removeAll(ideaTestSourceDirs)

              def testGeneratedDirectorySet = new DefaultExternalSourceDirectorySet()
              testGeneratedDirectorySet.name = generatedDirectorySet.name
              testGeneratedDirectorySet.srcDirs = testGeneratedDirs
              testGeneratedDirectorySet.addGradleOutputDir(generatedDirectorySet.outputDir)
              testGeneratedDirectorySet.outputDir = generatedDirectorySet.outputDir
              testGeneratedDirectorySet.inheritedCompilerOutput = generatedDirectorySet.isCompilerOutputPathInherited()

              sources.put(ExternalSystemSourceType.TEST_GENERATED, testGeneratedDirectorySet)
            }
          }
        }

        if (ideaPluginModule && SourceSet.MAIN_SOURCE_SET_NAME != sourceSet.name && SourceSet.TEST_SOURCE_SET_NAME != sourceSet.name) {
          sources.values().each {
            ideaSourceDirs.removeAll(it.srcDirs)
            ideaResourceDirs.removeAll(it.srcDirs)
            ideaTestSourceDirs.removeAll(it.srcDirs)
            ideaTestResourceDirs.removeAll(it.srcDirs)
          }
        }
      }

      if(resolveSourceSetDependencies) {
        def dependencies = new DependencyResolverImpl(project, isPreview, downloadJavadoc, downloadSources, sourceSetFinder).resolveDependencies(sourceSet)
        externalSourceSet.dependencies.addAll(dependencies)
      }

      externalSourceSet.sources = sources
      result[sourceSet.name] = externalSourceSet
    }

    def mainSourceSet = result[SourceSet.MAIN_SOURCE_SET_NAME]
    if(ideaPluginModule && mainSourceSet && ideaSourceDirs && !ideaSourceDirs.isEmpty()) {
      def mainGradleSourceSet = sourceSets.findByName(SourceSet.MAIN_SOURCE_SET_NAME)
      if(mainGradleSourceSet) {
        def mainSourceDirectorySet = mainSourceSet.sources[ExternalSystemSourceType.SOURCE]
        if(mainSourceDirectorySet) {
          mainSourceDirectorySet.srcDirs.addAll(ideaSourceDirs - (mainGradleSourceSet.resources.srcDirs + generatedSourceDirs))
        }
        def mainResourceDirectorySet = mainSourceSet.sources[ExternalSystemSourceType.RESOURCE]
        if(mainResourceDirectorySet) {
          mainResourceDirectorySet.srcDirs.addAll(ideaResourceDirs)
        }

        if (!additionalIdeaGenDirs.isEmpty()) {
          def mainAdditionalGenDirs = additionalIdeaGenDirs.intersect(ideaSourceDirs)
          def mainGenSourceDirectorySet = mainSourceSet.sources[ExternalSystemSourceType.SOURCE_GENERATED]
          if (mainGenSourceDirectorySet) {
            mainGenSourceDirectorySet.srcDirs.addAll(mainAdditionalGenDirs)
          }
          else {
            def generatedDirectorySet = new DefaultExternalSourceDirectorySet()
            generatedDirectorySet.name = "generated " + mainSourceSet.name
            generatedDirectorySet.srcDirs.addAll(mainAdditionalGenDirs)
            generatedDirectorySet.addGradleOutputDir(mainSourceDirectorySet.outputDir)
            generatedDirectorySet.outputDir = mainSourceDirectorySet.outputDir
            generatedDirectorySet.inheritedCompilerOutput = mainSourceDirectorySet.isCompilerOutputPathInherited()
            mainSourceSet.sources.put(ExternalSystemSourceType.SOURCE_GENERATED, generatedDirectorySet)
          }
        }
      }
    }

    def testSourceSet = result[SourceSet.TEST_SOURCE_SET_NAME]
    if(ideaPluginModule && testSourceSet && ideaTestSourceDirs && !ideaTestSourceDirs.isEmpty()) {
      def testGradleSourceSet = sourceSets.findByName(SourceSet.TEST_SOURCE_SET_NAME)
      if(testGradleSourceSet) {
        def testSourceDirectorySet = testSourceSet.sources[ExternalSystemSourceType.TEST]
        if(testSourceDirectorySet) {
          testSourceDirectorySet.srcDirs.addAll(ideaTestSourceDirs - (testGradleSourceSet.resources.srcDirs + generatedSourceDirs))
        }
        def testResourceDirectorySet = testSourceSet.sources[ExternalSystemSourceType.TEST_RESOURCE]
        if(testResourceDirectorySet) {
          testResourceDirectorySet.srcDirs.addAll(ideaTestResourceDirs)
        }

        if (!additionalIdeaGenDirs.isEmpty()) {
          def testAdditionalGenDirs = additionalIdeaGenDirs.intersect(ideaTestSourceDirs)
          def testGenSourceDirectorySet = testSourceSet.sources[ExternalSystemSourceType.TEST_GENERATED]
          if (testGenSourceDirectorySet) {
            testGenSourceDirectorySet.srcDirs.addAll(testAdditionalGenDirs)
          }
          else {
            def generatedDirectorySet = new DefaultExternalSourceDirectorySet()
            generatedDirectorySet.name = "generated " + testSourceSet.name
            generatedDirectorySet.srcDirs.addAll(testAdditionalGenDirs)
            generatedDirectorySet.addGradleOutputDir(testSourceDirectorySet.outputDir)
            generatedDirectorySet.outputDir = testSourceDirectorySet.outputDir
            generatedDirectorySet.inheritedCompilerOutput = testSourceDirectorySet.isCompilerOutputPathInherited()
            testSourceSet.sources.put(ExternalSystemSourceType.TEST_GENERATED, generatedDirectorySet)
          }
        }
      }
    }

    cleanupSharedSourceFolders(result)

    result
  }

  private static boolean isEmpty(FileCollection collection) {
    try {
      return collection.isEmpty()
    } catch (Throwable ignored) {
    }
    return true
  }

  private static void cleanupSharedSourceFolders(Map<String, ExternalSourceSet> map) {
    def mainSourceSet = map[SourceSet.MAIN_SOURCE_SET_NAME]
    cleanupSharedSourceFolders(map, mainSourceSet, null)
    cleanupSharedSourceFolders(map, map[SourceSet.TEST_SOURCE_SET_NAME], mainSourceSet)
  }

  private static void cleanupSharedSourceFolders(Map<String, ExternalSourceSet> result, ExternalSourceSet sourceSet, ExternalSourceSet toIgnore) {
    if(!sourceSet) return

    result.entrySet().each {
      if (!it.value.is(sourceSet) && !it.value.is(toIgnore)) {
        def customSourceSet = it.value
        ExternalSystemSourceType.values().each {
          def customSourceDirectorySet = customSourceSet.sources[it] as ExternalSourceDirectorySet
          if (customSourceDirectorySet) {
            def mainSourcesMap = sourceSet.sources
            mainSourcesMap.values().each {
              customSourceDirectorySet.srcDirs.removeAll(it.srcDirs)
            }
          }
        }
      }
    }
  }

  static <T> T chooseNotNull(T ... params) {
    //noinspection GrUnresolvedAccess
    params.findResult("", { it })
  }

  static getFilters(Project project, String taskName) {
    def includes = []
    def excludes = []
    def filterReaders = [] as List<ExternalFilter>
    def filterableTask = project.tasks.findByName(taskName)
    if (filterableTask instanceof PatternFilterable) {
      includes += filterableTask.includes
      excludes += filterableTask.excludes
    }

    if(System.getProperty('idea.disable.gradle.resource.filtering', 'false').toBoolean()) {
      return [includes, excludes, filterReaders]
    }

    try {
      if (filterableTask instanceof ContentFilterable && filterableTask.metaClass.respondsTo(filterableTask, "getMainSpec")) {
        //noinspection GrUnresolvedAccess
        def properties = filterableTask.getMainSpec().properties
        def copyActions = properties?.allCopyActions ?: properties?.copyActions

        if(copyActions) {
          copyActions.each { Action<? super FileCopyDetails> action ->
            if (action.hasProperty('val$filterType')) {
              //noinspection GrUnresolvedAccess
              def filterType = (action?.val$filterType as Class).name
              def filter = [filterType: filterType] as DefaultExternalFilter

              if(action.hasProperty('val$properties')) {
                //noinspection GrUnresolvedAccess
                def props = action?.val$properties
                if (props) {
                  if ('org.apache.tools.ant.filters.ExpandProperties' == filterType && props['project']) {
                    if (props['project']) filter.propertiesAsJsonMap = new GsonBuilder().create().toJson(props['project'].properties)
                  }
                  else {
                    filter.propertiesAsJsonMap = new GsonBuilder().create().toJson(props)
                  }
                }
              }
              filterReaders << filter
            }
            else if (action.class.simpleName == 'RenamingCopyAction' && action.hasProperty('transformer')) {
              //noinspection GrUnresolvedAccess
              if (action.transformer.hasProperty('matcher') && action?.transformer?.hasProperty('replacement')) {
                //noinspection GrUnresolvedAccess
                String pattern = action?.transformer?.matcher?.pattern()?.pattern
                //noinspection GrUnresolvedAccess
                String replacement = action?.transformer?.replacement
                def filter = [filterType: 'RenamingCopyFilter'] as DefaultExternalFilter
                if(pattern && replacement){
                  filter.propertiesAsJsonMap = new GsonBuilder().create().toJson([pattern: pattern, replacement: replacement])
                  filterReaders << filter
                }
              }
            }
//          else {
//            project.logger.error(
//              ErrorMessageBuilder.create(project, "Resource configuration errors")
//                .withDescription("Unsupported copy action found: " + action.class.name).build())
//          }
          }
        }
      }
    }
    catch (Exception ignore) {
//      project.logger.error(
//        ErrorMessageBuilder.create(project, e, "Resource configuration errors")
//          .withDescription("Unable to resolve resources filtering configuration").build())
    }

    return [includes, excludes, filterReaders]
  }


  private static String wrap(Object o) {
    return o instanceof CharSequence ? o.toString() : ""
  }

  @NotNull
  @Override
  ErrorMessageBuilder getErrorMessageBuilder(@NotNull Project project, @NotNull Exception e) {
    return ErrorMessageBuilder.create(
      project, e, "Project resolve errors"
    ).withDescription("Unable to resolve additional project configuration.")
  }
}
