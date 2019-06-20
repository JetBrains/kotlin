// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.tooling.tasks

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.result.ResolutionResult
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.diagnostics.internal.graph.nodes.RenderableDependency
import org.gradle.api.tasks.diagnostics.internal.graph.nodes.RenderableModuleResult

@CompileStatic
class DependenciesReport extends DefaultTask {
  public static final String ANY_CONFIGURATION = "*"

  @Input
  String configurationName
  @OutputFile
  File outputFile

  @TaskAction
  void generate() {
    List<DependencyNode> graph = []
    Gson gson = new GsonBuilder().create()
    Collection<Configuration> configurations = ANY_CONFIGURATION == configurationName ? project.configurations.asList() :
                                               Collections.singleton(project.configurations.getByName(configurationName))
    for (configuration in configurations) {
      if (!configuration.isCanBeResolved()) continue
      ResolutionResult resolutionResult = configuration.getIncoming().getResolutionResult()
      RenderableDependency root = new RenderableModuleResult(resolutionResult.root)
      graph.add(toNode(gson, root, configuration.name, true, [:]))
    }
    outputFile.parentFile.mkdirs()
    outputFile.text = gson.toJson(graph)
  }

  static DependencyNode toNode(Gson gson,
                               RenderableDependency dependency,
                               String configurationName,
                               boolean isConfigurationNode,
                               Map<Object, DependencyNode> added) {
    def id = "${gson.toJson(dependency.id)}_$configurationName".hashCode()
    DependencyNode node = added.get(id)
    if (node != null) {
      def dependencyNode = new DependencyNode(id)
      dependencyNode.setName(dependency.name + " (*)")
      return dependencyNode
    }

    def nodeName = isConfigurationNode ? dependency.name + " (" + configurationName + ")" : dependency.name
    node = new DependencyNode(id)
    node.setName(nodeName)
    node.setState(dependency.resolutionState.name())

    added.put(id, node)
    dependency.getChildren().each { RenderableDependency child ->
      node.children.add(toNode(gson, child, configurationName, false, added))
    }
    return node
  }
}
