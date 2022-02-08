/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import groovy.util.Node
import groovy.util.NodeList
import org.gradle.api.Project
import org.gradle.api.XmlProvider
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.attributes.Usage
import org.gradle.api.internal.component.SoftwareComponentInternal
import org.gradle.api.internal.component.UsageContext
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilationToRunnableFiles
import org.jetbrains.kotlin.gradle.plugin.KotlinTargetComponent
import org.jetbrains.kotlin.gradle.utils.getValue

internal data class ModuleCoordinates(
    val group: String?,
    val name: String,
    val version: String?
)

internal class PomDependenciesRewriter(
    project: Project,

    @field:Transient
    private val component: KotlinTargetComponent
) {

    // Get the dependencies mapping according to the component's UsageContexts:
    private val dependenciesMappingForEachUsageContext by project.provider {
        (component as SoftwareComponentInternal).usages.mapNotNull { usage ->
            if (usage is KotlinUsageContext)
                associateDependenciesWithActualModuleDependencies(usage)
                    // We are only interested in dependencies that are mapped to some other dependencies:
                    .filter { (from, to) -> Triple(from.group, from.name, from.version) != Triple(to.group, to.name, to.version) }
            else null
        }
    }

    fun rewritePomMppDependenciesToActualTargetModules(
        pomXml: XmlProvider,
        includeOnlySpecifiedDependencies: Provider<Set<ModuleCoordinates>>? = null
    ) {
        if (component !is SoftwareComponentInternal)
            return

        val dependenciesNode = (pomXml.asNode().get("dependencies") as NodeList).filterIsInstance<Node>().singleOrNull() ?: return

        val dependencyNodes = (dependenciesNode.get("dependency") as? NodeList).orEmpty().filterIsInstance<Node>()

        val dependencyByNode = mutableMapOf<Node, ModuleCoordinates>()

        // Collect all the dependencies from the nodes:
        val dependencies = dependencyNodes.map { dependencyNode ->
            fun Node.getSingleChildValueOrNull(childName: String): String? =
                ((get(childName) as NodeList?)?.singleOrNull() as Node?)?.text()

            val groupId = dependencyNode.getSingleChildValueOrNull("groupId")
            val artifactId = dependencyNode.getSingleChildValueOrNull("artifactId")
                ?: error("unexpected dependency in POM with no artifact ID: $dependenciesNode")
            val version = dependencyNode.getSingleChildValueOrNull("version")
            (ModuleCoordinates(groupId, artifactId, version)).also { dependencyByNode[dependencyNode] = it }
        }.toSet()

        val resultDependenciesForEachUsageContext = dependencies.associate { key ->
            val map = dependenciesMappingForEachUsageContext.find { key in it }
            val value = map?.get(key) ?: key
            key to value
        }

        val includeOnlySpecifiedDependenciesSet = includeOnlySpecifiedDependencies?.get()

        // Rewrite the dependency nodes according to the mapping:
        dependencyNodes.forEach { dependencyNode ->
            val moduleDependency = dependencyByNode[dependencyNode]

            if (moduleDependency != null) {
                if (includeOnlySpecifiedDependenciesSet != null && moduleDependency !in includeOnlySpecifiedDependenciesSet) {
                    dependenciesNode.remove(dependencyNode)
                    return@forEach
                }
            }

            val mapDependencyTo = resultDependenciesForEachUsageContext.get(moduleDependency)

            if (mapDependencyTo != null) {
                fun Node.setChildNodeByName(name: String, value: String?) {
                    val childNode: Node? = (get(name) as NodeList?)?.firstOrNull() as Node?
                    if (value != null) {
                        (childNode ?: appendNode(name)).setValue(value)
                    } else {
                        childNode?.let { remove(it) }
                    }
                }

                dependencyNode.setChildNodeByName("groupId", mapDependencyTo.group)
                dependencyNode.setChildNodeByName("artifactId", mapDependencyTo.name)
                dependencyNode.setChildNodeByName("version", mapDependencyTo.version)
            }
        }
    }
}

private fun associateDependenciesWithActualModuleDependencies(
    usageContext: KotlinUsageContext
): Map<ModuleCoordinates, ModuleCoordinates> {
    val compilation = usageContext.compilation
    val project = compilation.target.project

    val targetDependenciesConfiguration = project.configurations.getByName(
        when (compilation) {
            is KotlinJvmAndroidCompilation -> {
                // TODO handle Android configuration names in a general way once we drop AGP < 3.0.0
                val variantName = compilation.name
                when (usageContext.usage.name) {
                    Usage.JAVA_API, "java-api-jars" -> variantName + "CompileClasspath"
                    Usage.JAVA_RUNTIME_JARS -> variantName + "RuntimeClasspath"
                    else -> error("Unexpected Usage for usage context: ${usageContext.usage}")
                }
            }
            else -> when (usageContext.usage.name) {
                Usage.JAVA_API, "java-api-jars" -> compilation.compileDependencyConfigurationName
                Usage.JAVA_RUNTIME_JARS -> (compilation as KotlinCompilationToRunnableFiles).runtimeDependencyConfigurationName
                else -> error("Unexpected Usage for usage context: ${usageContext.usage}")
            }
        }
    )

    val resolvedDependencies: Map<Triple<String?, String, String?>, ResolvedDependency> by lazy {
        // don't resolve if no project dependencies on MPP projects are found
        targetDependenciesConfiguration.resolvedConfiguration.lenientConfiguration.allModuleDependencies.associateBy {
            Triple(it.moduleGroup, it.moduleName, it.moduleVersion)
        }
    }

    return targetDependenciesConfiguration
        .allDependencies.withType(ModuleDependency::class.java)
        .associate { dependency ->
            val coordinates = ModuleCoordinates(dependency.group, dependency.name, dependency.version)
            val noMapping = coordinates to coordinates
            when (dependency) {
                is ProjectDependency -> {
                    val dependencyProject = dependency.dependencyProject
                    val dependencyProjectKotlinExtension = dependencyProject.multiplatformExtensionOrNull
                        ?: return@associate noMapping

                    val resolved = resolvedDependencies[Triple(dependency.group!!, dependency.name, dependency.version!!)]
                        ?: return@associate noMapping

                    val resolvedToConfiguration = resolved.configuration
                    val dependencyTargetComponent: KotlinTargetComponent = run {
                        dependencyProjectKotlinExtension.targets.withType(AbstractKotlinTarget::class.java).forEach { target ->
                            target.kotlinComponents.forEach { component ->
                                if (component.findUsageContext(resolvedToConfiguration) != null)
                                    return@run component
                            }
                        }
                        // Failed to find a matching component:
                        return@associate noMapping
                    }

                    val targetModulePublication = (dependencyTargetComponent as? KotlinTargetComponentWithPublication)?.publicationDelegate
                    val rootModulePublication = dependencyProjectKotlinExtension.rootSoftwareComponent.publicationDelegate

                    // During Gradle POM generation, a project dependency is already written as the root module's coordinates. In the
                    // dependencies mapping, map the root module to the target's module:

                    val rootModule = ModuleCoordinates(
                        rootModulePublication?.groupId ?: dependency.group,
                        rootModulePublication?.artifactId ?: dependencyProject.name,
                        rootModulePublication?.version ?: dependency.version
                    )

                    rootModule to ModuleCoordinates(
                        targetModulePublication?.groupId ?: dependency.group,
                        targetModulePublication?.artifactId ?: dependencyTargetComponent.defaultArtifactId,
                        targetModulePublication?.version ?: dependency.version
                    )
                }
                else -> {
                    val resolvedDependency = resolvedDependencies[Triple(dependency.group, dependency.name, dependency.version)]
                        ?: return@associate noMapping

                    if (resolvedDependency.moduleArtifacts.isEmpty() && resolvedDependency.children.size == 1) {
                        // This is a dependency on a module that resolved to another module; map the original dependency to the target module
                        val targetModule = resolvedDependency.children.single()
                        coordinates to ModuleCoordinates(
                            targetModule.moduleGroup,
                            targetModule.moduleName,
                            targetModule.moduleVersion
                        )

                    } else {
                        noMapping
                    }
                }
            }
        }
}

private fun KotlinTargetComponent.findUsageContext(configurationName: String): UsageContext? {
    val usageContexts = when (this) {
        is SoftwareComponentInternal -> usages
        else -> emptySet()
    }
    return usageContexts.find { usageContext ->
        if (usageContext !is KotlinUsageContext) return@find false
        val compilation = usageContext.compilation
        configurationName in compilation.relatedConfigurationNames ||
                configurationName == compilation.target.apiElementsConfigurationName ||
                configurationName == compilation.target.runtimeElementsConfigurationName ||
                configurationName == compilation.target.defaultConfigurationName
    }
}