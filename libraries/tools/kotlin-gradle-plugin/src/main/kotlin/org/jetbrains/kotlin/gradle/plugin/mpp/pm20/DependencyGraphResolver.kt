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
import org.jetbrains.kotlin.gradle.plugin.mpp.resolvableMetadataConfiguration
import org.jetbrains.kotlin.gradle.plugin.sources.KotlinDependencyScope
import org.jetbrains.kotlin.project.model.*

class GradleKotlinDependencyGraphResolver(
    private val project: Project,
    private val moduleResolver: ModuleDependencyResolver,
    private val fragmentsResolver: ModuleFragmentsResolver
) : KotlinDependencyGraphResolver {

    private val configurationToResolve: Configuration
        get() = resolvableMetadataConfiguration(
            project,
            project.kotlinExtension.sourceSets, // take dependencies from all source sets; TODO introduce consistency scopes?
            KotlinDependencyScope.compileScopes
        )

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
                else moduleResolver.resolveDependency(component.toModuleDependency())
                    .takeIf { component !in excludeLegacyMetadataModulesFromResult }
                    ?: buildStubModule(component, component.variants.singleOrNull()?.displayName ?: "default")

                val resolvedComponentDependencies = component.dependencies
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
            nodeFromComponent(configurationToResolve.incoming.resolutionResult.root, isRoot = true)
        )
    }
}