/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.dsl.topLevelExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.resolvableMetadataConfiguration
import org.jetbrains.kotlin.gradle.plugin.sources.KotlinDependencyScope
import org.jetbrains.kotlin.project.model.*

internal fun resolvableMetadataConfiguration(
    module: KotlinGradleModule
) = module.project.configurations.getByName(module.resolvableMetadataConfigurationName)

internal fun configurationToResolveMetadataDependencies(project: Project, requestingModule: KotlinModule): Configuration =
    when (project.topLevelExtension) {
        is KotlinPm20ProjectExtension -> resolvableMetadataConfiguration(requestingModule as KotlinGradleModule)
        else -> resolvableMetadataConfiguration(
            project,
            project.kotlinExtension.sourceSets, // take dependencies from all source sets; TODO introduce consistency scopes?
            KotlinDependencyScope.compileScopes
        )
    }


class GradleKotlinDependencyGraphResolver(
    private val project: Project,
    private val moduleResolver: ModuleDependencyResolver
) : KotlinDependencyGraphResolver {

    private fun configurationToResolve(requestingModule: KotlinModule): Configuration =
        configurationToResolveMetadataDependencies(project, requestingModule)

    override fun resolveDependencyGraph(requestingModule: KotlinModule): DependencyGraphResolution {
        if (requestingModule !is KotlinGradleModule)
            return DependencyGraphResolution.Unknown(requestingModule)
        return resolveAsGraph(requestingModule)
    }

    private fun resolveAsGraph(requestingModule: KotlinGradleModule): GradleDependencyGraph {
        val excludeLegacyMetadataModulesFromResult = mutableSetOf<ResolvedComponentResult>()

        val nodeByModuleId = mutableMapOf<KotlinModuleIdentifier, GradleDependencyGraphNode>()

        fun nodeFromComponent(component: ResolvedComponentResult, isRoot: Boolean /*refactor me*/): GradleDependencyGraphNode {
            val id = component.toSingleModuleIdentifier()
            return nodeByModuleId.getOrPut(id) {
                val module = if (isRoot)
                    requestingModule
                else moduleResolver.resolveDependency(requestingModule, component.toModuleDependency())
                    .takeIf { component !in excludeLegacyMetadataModulesFromResult }
                    ?: buildSyntheticModule(component, component.variants.singleOrNull()?.displayName ?: "default")

                val metadataSourceComponent =
                    (module as? ExternalImportedKotlinModule)
                        ?.takeIf { it.hasLegacyMetadataModule }
                        ?.let { (component.dependencies.singleOrNull() as? ResolvedDependencyResult)?.selected }
                        ?: component

                val dependenciesRequestedByModule =
                    module.fragments.flatMap { fragment -> fragment.declaredModuleDependencies.map { it.moduleIdentifier } }.toSet()

                val resolvedComponentDependencies = metadataSourceComponent.dependencies
                    .filterIsInstance<ResolvedDependencyResult>()
                    // This filter statement is used to only visit the dependencies of the variant(s) of the requested Kotlin module and not
                    // other variants. This prevents infinite recursion when visiting multiple Kotlin modules within one Gradle components
                    .filter { dependency -> dependency.requested.toModuleIdentifiers().any { it in dependenciesRequestedByModule } }
                    .flatMap { dependency -> dependency.requested.toModuleIdentifiers().map { id -> id to dependency.selected } }
                    .toMap()

                val fragmentDependencies = module.fragments.associateWith { it.declaredModuleDependencies }

                val nodeDependenciesMap = fragmentDependencies.mapValues { (_, deps) ->
                    deps.mapNotNull { resolvedComponentDependencies[it.moduleIdentifier] }.map { nodeFromComponent(it, isRoot = false) }
                }

                GradleDependencyGraphNode(
                    module,
                    component,
                    metadataSourceComponent,
                    nodeDependenciesMap
                )
            }
        }

        return GradleDependencyGraph(
            requestingModule,
            nodeFromComponent(configurationToResolve(requestingModule).incoming.resolutionResult.root, isRoot = true)
        )
    }
}

class GradleDependencyGraphNode(
    override val module: KotlinModule,
    val selectedComponent: ResolvedComponentResult,
    val metadataSourceComponent: ResolvedComponentResult?,
    override val dependenciesByFragment: Map<KotlinModuleFragment, Iterable<GradleDependencyGraphNode>>
) : DependencyGraphNode(module, dependenciesByFragment)

class GradleDependencyGraph(
    override val requestingModule: KotlinGradleModule,
    override val root: GradleDependencyGraphNode
) : DependencyGraphResolution.DependencyGraph(requestingModule, root)
