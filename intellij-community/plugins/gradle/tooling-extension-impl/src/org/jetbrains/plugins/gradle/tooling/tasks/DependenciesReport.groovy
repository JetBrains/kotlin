// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.tooling.tasks

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.result.ResolutionResult
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.diagnostics.internal.graph.nodes.RenderableDependency
import org.gradle.api.tasks.diagnostics.internal.graph.nodes.RenderableModuleResult

@CompileStatic
class DependenciesReport extends DefaultTask {

  @Input
  List<String> configurations = []
  @OutputFile
  File outputFile

  @TaskAction
  void generate() {
    List<ComponentNode> graph = []
    Gson gson = new GsonBuilder().create()
    Collection<Configuration> configurationList
    if (configurations.isEmpty()) {
      configurationList = project.configurations
    }
    else {
      configurationList = new ArrayList<>()
      for (configurationName in configurations) {
        def configuration = project.configurations.findByName(configurationName)
        if(configuration != null) {
          configurationList.add(configuration)
        }
      }
    }

    for (configuration in configurationList) {
      if (!configuration.isCanBeResolved()) continue
      ResolutionResult resolutionResult = configuration.getIncoming().getResolutionResult()
      RenderableDependency root = new RenderableModuleResult(resolutionResult.root)
      graph.add(toNode(gson, root, configuration.name, project.path, true, [:], new IdGenerator()))
    }
    outputFile.parentFile.mkdirs()
    outputFile.text = gson.toJson(graph)
  }

  static ComponentNode toNode(Gson gson,
                              RenderableDependency dependency,
                              String configurationName,
                              String projectPath,
                              boolean isConfigurationNode,
                              Map<Object, ComponentNode> added,
                              IdGenerator idGenerator) {
    def id = idGenerator.getId(dependency, configurationName)
    ComponentNode alreadySeenNode = added.get(id)
    if (alreadySeenNode != null) {
      return new ReferenceNode(id)
    }

    AbstractComponentNode node
    if (isConfigurationNode) {
      node = new ConfigurationNode(id, projectPath, configurationName)
    }
    else {
      if (dependency.id instanceof ProjectComponentIdentifier) {
        ProjectComponentIdentifier projectId = dependency.id as ProjectComponentIdentifier
        node = new ProjectComponentNode(id, projectId.projectPath)
      }
      else if (dependency.id instanceof ModuleComponentIdentifier) {
        ModuleComponentIdentifier moduleId = dependency.id as ModuleComponentIdentifier
        node = new ArtifactComponentNode(id, moduleId.group, moduleId.module, moduleId.version)
      }
      else {
        node = new BaseComponentNode(id, dependency.name)
      }
    }
    node.setState(dependency.resolutionState.name())

    added.put(id, node)
    dependency.getChildren().each { RenderableDependency child ->
      node.children.add(toNode(gson, child, configurationName, projectPath, false, added, idGenerator))
    }
    return node
  }

  static class IdGenerator {
    private Map<String, Long> idMap = new HashMap<>()
    private long value

    private long getId(RenderableDependency dependency, String configurationName) {
      def key = dependency.id.toString() + '_' + configurationName
      def id = idMap.get(key)
      if(id == null) {
        idMap[key] = ++value
        id = value
      }
      return id
    }
  }
}
