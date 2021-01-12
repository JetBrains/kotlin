// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.tooling.tasks

import com.google.gson.GsonBuilder
import com.intellij.openapi.externalSystem.model.project.dependencies.*
import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.Describable
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.FileCollectionDependency
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.result.ResolutionResult
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.diagnostics.internal.graph.nodes.RenderableDependency
import org.gradle.api.tasks.diagnostics.internal.graph.nodes.RenderableModuleResult
import org.gradle.util.GradleVersion

@CompileStatic
class DependenciesReport extends DefaultTask {

  @Input
  List<String> configurations = []
  @OutputFile
  File outputFile

  @TaskAction
  void generate() {
    Collection<Configuration> configurationList
    if (configurations.isEmpty()) {
      configurationList = project.configurations
    }
    else {
      configurationList = new ArrayList<>()
      for (configurationName in configurations) {
        def configuration = project.configurations.findByName(configurationName)
        if (configuration != null) {
          configurationList.add(configuration)
        }
      }
    }

    def projectNameFunction = new ProjectNameFunction()
    List<DependencyScopeNode> graph = []
    for (configuration in configurationList) {
      if (!configuration.isCanBeResolved()) continue
      graph.add(doBuildDependenciesGraph(configuration, project, projectNameFunction))
    }
    outputFile.parentFile.mkdirs()
    outputFile.text = new GsonBuilder().create().toJson(graph)
  }

  static DependencyScopeNode buildDependenciesGraph(Configuration configuration, Project project) {
    return doBuildDependenciesGraph(configuration, project, new ProjectNameFunction())
  }

  private static DependencyScopeNode doBuildDependenciesGraph(Configuration configuration,
                                                              Project project,
                                                              ProjectNameFunction projectNameFunction) {
    if (!project.configurations.contains(configuration)) {
      throw new IllegalArgumentException("configurations of the project should be used")
    }
    ResolutionResult resolutionResult = configuration.getIncoming().getResolutionResult()
    RenderableDependency root = new RenderableModuleResult(resolutionResult.root)
    String configurationName = configuration.name
    IdGenerator idGenerator = new IdGenerator()
    long id = idGenerator.getId(root, configurationName)
    String scopeDisplayName = "project " + project.path + " (" + configurationName + ")"
    DependencyScopeNode node = new DependencyScopeNode(id, configurationName, scopeDisplayName, configuration.getDescription())
    node.setResolutionState(root.resolutionState.name())
    for (Dependency dependency : configuration.getAllDependencies()) {
      if (dependency instanceof FileCollectionDependency) {
        FileCollection fileCollection = ((FileCollectionDependency)dependency).getFiles();
        if (fileCollection instanceof Configuration) continue;
        def files = fileCollection.files
        if (files.isEmpty()) continue

        String displayName = null
        if (fileCollection instanceof Describable) {
          displayName = ((Describable)fileCollection).displayName
        } else {
          def string = fileCollection.toString()
          if ("file collection" != string) {
            displayName = string
          }
        }

        if (displayName != null) {
          long fileDepId = idGenerator.getId(displayName, configurationName)
          node.dependencies.add(new FileCollectionDependencyNodeImpl(fileDepId, displayName, fileCollection.getAsPath()))
        }
        else {
          for (File file : files) {
            long fileDepId = idGenerator.getId(file.path, configurationName)
            node.dependencies.add(new FileCollectionDependencyNodeImpl(fileDepId, file.name, file.path))
          }
        }
      }
    }

    Map<Object, DependencyNode> added = [:]
    for (RenderableDependency child in root.getChildren()) {
      node.dependencies.add(toNode(child, configurationName, added, idGenerator, projectNameFunction))
    }
    return node
  }

  static private DependencyNode toNode(RenderableDependency dependency,
                                       String configurationName,
                                       Map<Object, DependencyNode> added,
                                       IdGenerator idGenerator,
                                       ProjectNameFunction projectNameFunction) {
    long id = idGenerator.getId(dependency, configurationName)
    DependencyNode alreadySeenNode = added.get(id)
    if (alreadySeenNode != null) {
      return new ReferenceNode(id)
    }

    AbstractDependencyNode node
    if (dependency.id instanceof ProjectComponentIdentifier) {
      ProjectComponentIdentifier projectId = dependency.id as ProjectComponentIdentifier
      node = new ProjectDependencyNodeImpl(id, projectNameFunction.fun(projectId))
    }
    else if (dependency.id instanceof ModuleComponentIdentifier) {
      ModuleComponentIdentifier moduleId = dependency.id as ModuleComponentIdentifier
      node = new ArtifactDependencyNodeImpl(id, moduleId.group, moduleId.module, moduleId.version)
    }
    else {
      node = new UnknownDependencyNode(id, dependency.name)
    }
    node.setResolutionState(dependency.resolutionState.name())
    added.put(id, node)
    Iterator<? extends RenderableDependency> iterator = dependency.getChildren().iterator()
    while (iterator.hasNext()) {
      RenderableDependency child = iterator.next()
      node.dependencies.add(toNode(child, configurationName, added, idGenerator, projectNameFunction))
    }
    return node
  }

  static class ProjectNameFunction {
    def is45OrNewer = GradleVersion.current() >= GradleVersion.version("4.5")

    String fun(ProjectComponentIdentifier identifier) {
      return is45OrNewer ? identifier.projectName : identifier.projectPath
    }
  }

  private static class IdGenerator {
    private Map<String, Long> idMap = new HashMap<>()
    private long value

    private long getId(String prefix, String configurationName) {
      def key = prefix + '_' + configurationName
      def id = idMap.get(key)
      if (id == null) {
        idMap[key] = ++value
        id = value
      }
      return id
    }

    private long getId(RenderableDependency dependency, String configurationName) {
      return getId(dependency.id.toString(), configurationName)
    }
  }
}
