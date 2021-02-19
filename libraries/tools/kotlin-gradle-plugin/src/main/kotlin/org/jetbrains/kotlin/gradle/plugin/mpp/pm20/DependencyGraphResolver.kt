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
        if (!requestingModule.representsProject(project))
            return DependencyGraphResolution.Unknown(requestingModule)

        val excludeLegacyMetadataModulesFromResult = mutableSetOf<ResolvedComponentResult>()

        val nodeByModuleId = mutableMapOf<KotlinModuleIdentifier, DependencyGraphNode>()

        fun nodeFromComponent(component: ResolvedComponentResult, isRoot: Boolean /*refactor*/): DependencyGraphNode {
            val id = component.toModuleIdentifier()
            return nodeByModuleId.getOrPut(id) {
                val module = if (isRoot)
                    requestingModule
                else moduleResolver.resolveDependency(requestingModule, component.toModuleDependency())
                    .takeIf { component !in excludeLegacyMetadataModulesFromResult }
                    ?: buildSyntheticModule(component, component.variants.singleOrNull()?.displayName ?: "default")

                val componentContainingTransitiveDependencies =
                    (module as? ExternalImportedKotlinModule)
                        ?.takeIf { it.hasLegacyMetadataModule }
                        ?.let { (component.dependencies.singleOrNull() as? ResolvedDependencyResult)?.selected }
                        ?: component

                val resolvedComponentDependencies = componentContainingTransitiveDependencies.dependencies
                    .filterIsInstance<ResolvedDependencyResult>()
                    .flatMap { dependency -> dependency.requested.toModuleIdentifiers().map { id -> id to dependency.selected } }
                    .toMap()

                val fragmentDependencies = module.fragments.associateWith { it.declaredModuleDependencies }

                val nodeDependenciesMap = fragmentDependencies.mapValues { (_, deps) ->
                    deps.mapNotNull { resolvedComponentDependencies[it.moduleIdentifier] }.map { nodeFromComponent(it, isRoot = false) }
                }

                DependencyGraphNode(module, nodeDependenciesMap)
            }
        }

        return DependencyGraphResolution.DependencyGraph(
            requestingModule,
            nodeFromComponent(configurationToResolve(requestingModule).incoming.resolutionResult.root, isRoot = true)
        )
    }
}