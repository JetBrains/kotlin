/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import groovy.util.Node
import groovy.util.NodeList
import org.gradle.api.XmlProvider
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.internal.component.SoftwareComponentInternal
import org.gradle.api.internal.component.UsageContext
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
import org.jetbrains.kotlin.gradle.plugin.KotlinTargetComponent
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsageContext.MavenScope
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toUpperCaseAsciiOnly

internal data class ModuleCoordinates(
    val group: String?,
    val name: String,
    val version: String?
)

internal data class ModuleCoordinatesWithMavenScope(
    val coordinates: ModuleCoordinates,
    val mavenScope: MavenScope
)

internal suspend fun createDependenciesMappings(component: KotlinTargetComponent): Map<ModuleCoordinatesWithMavenScope, ModuleCoordinates> {
    val usages = component.awaitKotlinUsagesOrEmpty()
    val result = mutableMapOf<ModuleCoordinatesWithMavenScope, ModuleCoordinates>()

    for (usage in usages) {
        val mavenScope = usage.mavenScope ?: continue
        val mappingsForUsage = usage.createDependenciesMappings(mavenScope)
        for ((from, to) in mappingsForUsage) {
            // We are only interested in dependencies that are mapped to some other dependencies:
            if (from == to) continue

            result[ModuleCoordinatesWithMavenScope(from, mavenScope)] = to
        }
    }

    return result
}

private fun KotlinUsageContext.createDependenciesMappings(mavenScope: MavenScope): Map<ModuleCoordinates, ModuleCoordinates> {
    val project = compilation.target.project

    val targetDependenciesConfiguration = project.configurations.getByName(
        when (compilation) {
            is KotlinJvmAndroidCompilation -> {
                // TODO handle Android configuration names in a general way once we drop AGP < 3.0.0
                val variantName = compilation.name
                when (mavenScope) {
                    MavenScope.COMPILE -> variantName + "CompileClasspath"
                    MavenScope.RUNTIME -> variantName + "RuntimeClasspath"
                }
            }
            else -> when (mavenScope) {
                MavenScope.COMPILE -> compilation.compileDependencyConfigurationName
                MavenScope.RUNTIME -> compilation.runtimeDependencyConfigurationName ?: return emptyMap()
            }
        }
    )

    // TODO: Find a way to not resolve during configuration time
    val resolvedDependencies: Map<ModuleCoordinates, ResolvedDependency> =
        targetDependenciesConfiguration.resolvedConfiguration.lenientConfiguration.allModuleDependencies.associateBy {
            ModuleCoordinates(it.moduleGroup, it.moduleName, it.moduleVersion)
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

                    // Non-default publication layouts are not supported for pom rewriting
                    if (!dependencyProject.kotlinPropertiesProvider.createDefaultMultiplatformPublications)
                        return@associate noMapping

                    val resolved = resolvedDependencies[coordinates]
                        ?: return@associate noMapping

                    val resolvedToConfiguration = resolved.configuration
                    val dependencyTargetComponent: KotlinTargetComponent = run {
                        dependencyProjectKotlinExtension.targets.withType(InternalKotlinTarget::class.java).forEach { target ->
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
                    val resolvedDependency = resolvedDependencies[coordinates]
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

internal fun rewritePomMppDependenciesToActualTargetModules(
    mappings: Map<ModuleCoordinatesWithMavenScope, ModuleCoordinates>,
    pomXml: XmlProvider,
    includeOnlySpecifiedDependencies: Provider<Set<ModuleCoordinates>>? = null,
) {
    val dependenciesNode = (pomXml.asNode().get("dependencies") as NodeList).filterIsInstance<Node>().singleOrNull() ?: return

    val dependencyNodes = (dependenciesNode.get("dependency") as? NodeList).orEmpty().filterIsInstance<Node>()

    val dependencyByNode = mutableMapOf<Node, ModuleCoordinatesWithMavenScope>()

    // Collect all the dependencies from the nodes:
    val dependencies = dependencyNodes.mapNotNull { dependencyNode ->
        fun Node.getSingleChildValueOrNull(childName: String): String? =
            ((get(childName) as NodeList?)?.singleOrNull() as Node?)?.text()

        val groupId = dependencyNode.getSingleChildValueOrNull("groupId")
        val artifactId = dependencyNode.getSingleChildValueOrNull("artifactId")
            ?: error("unexpected dependency in POM with no artifact ID: $dependenciesNode")
        val version = dependencyNode.getSingleChildValueOrNull("version")
        val mavenScopeString = dependencyNode.getSingleChildValueOrNull("scope")
            ?: return@mapNotNull null // Map only scoped dependencies
        val mavenScope = MavenScope.valueOf(mavenScopeString.toUpperCaseAsciiOnly())
        val coordinates = ModuleCoordinates(groupId, artifactId, version)
        ModuleCoordinatesWithMavenScope(coordinates, mavenScope).also { dependencyByNode[dependencyNode] = it }
    }.toSet()

    val resultDependenciesForEachUsageContext = dependencies.associateWith { key -> mappings[key] ?: key.coordinates }

    val includeOnlySpecifiedDependenciesSet = includeOnlySpecifiedDependencies?.get()

    // Rewrite the dependency nodes according to the mapping:
    dependencyNodes.forEach { dependencyNode ->
        val moduleDependency = dependencyByNode[dependencyNode]

        if (moduleDependency != null) {
            if (includeOnlySpecifiedDependenciesSet != null && moduleDependency.coordinates !in includeOnlySpecifiedDependenciesSet) {
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

private fun KotlinTargetComponent.findUsageContext(configurationName: String): UsageContext? {
    val usageContexts = when (this) {
        is SoftwareComponentInternal -> usages
        else -> emptySet()
    }
    return usageContexts.find { usageContext ->
        if (usageContext !is KotlinUsageContext) return@find false
        val compilation = usageContext.compilation
        val outgoingConfigurations = mutableListOf(
            compilation.target.apiElementsConfigurationName,
            compilation.target.runtimeElementsConfigurationName
        )
        if (compilation is KotlinJvmAndroidCompilation) {
            val androidVariant = compilation.androidVariant
            outgoingConfigurations += listOf(
                "${androidVariant.name}ApiElements",
                "${androidVariant.name}RuntimeElements",
            )
        }
        configurationName in outgoingConfigurations
    }
}