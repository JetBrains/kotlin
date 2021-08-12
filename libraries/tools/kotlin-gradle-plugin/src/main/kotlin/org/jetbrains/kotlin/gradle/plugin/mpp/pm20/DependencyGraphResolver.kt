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
    private val moduleResolver: ModuleDependencyResolver
) : KotlinDependencyGraphResolver {

    private fun configurationToResolve(requestingModule: KotlinGradleModule): Configuration =
        configurationToResolveMetadataDependencies(requestingModule.project, requestingModule)

    override fun resolveDependencyGraph(requestingModule: KotlinModule): DependencyGraphResolution {
        if (requestingModule !is KotlinGradleModule)
            return DependencyGraphResolution.Unknown(requestingModule)
        return resolveAsGraph(requestingModule)
    }

    private fun resolveAsGraph(requestingModule: KotlinGradleModule): GradleDependencyGraph {
        val nodeByModuleId = mutableMapOf<KotlinModuleIdentifier, GradleDependencyGraphNode>()

        fun getKotlinModuleFromDependencyResult(resolvedDependency: ResolvedDependencyResult): KotlinModule =
            moduleResolver.resolveDependency(requestingModule, resolvedDependency.requested.toModuleDependency())
                ?: buildSyntheticPlainModule(resolvedDependency, resolvedDependency.resolvedVariant.displayName, requestingModule.project)

        fun moduleDependenciesByFragments(
            kotlinModule: KotlinModule,
            gradleDependencies: Iterable<ResolvedDependencyResult>
        ): Map<KotlinModuleFragment, List<ResolvedDependencyResult>> {
            val dependenciesRequestedByModule =
                kotlinModule.fragments.flatMap { fragment -> fragment.declaredModuleDependencies.map { it.moduleIdentifier } }.toSet()

            val resolvedComponentDependencies = gradleDependencies
                // This filter statement is used to only visit the dependencies of the variant(s) of the requested Kotlin module and not
                // other variants. This prevents infinite recursion when visiting multiple Kotlin modules within one Gradle components
                .filter { dependency -> dependency.toModuleIdentifier() in dependenciesRequestedByModule }
                .associate { dependency -> dependency.toModuleIdentifier() to dependency }
                .toMap()

            val fragmentDependencies = kotlinModule.fragments.associateWith { it.declaredModuleDependencies }

            val nodeDependenciesMap = fragmentDependencies.mapValues { (_, deps) ->
                deps.mapNotNull { resolvedComponentDependencies[it.moduleIdentifier] }
            }
            return nodeDependenciesMap
        }

        fun resolveDependenciesToGraphNodes(
            gradleDependenciesByFragment: Map<KotlinModuleFragment, Iterable<ResolvedDependencyResult>>,
            resolveOneDependency: (ResolvedDependencyResult) -> GradleDependencyGraphNode
        ) = gradleDependenciesByFragment.mapValues { (_, dependencies) ->
            dependencies.map(resolveOneDependency)
        }

        fun nodeFromModule(dependency: ResolvedDependencyResult): GradleDependencyGraphNode {
            val kotlinModule = getKotlinModuleFromDependencyResult(dependency)
            val id = kotlinModule.moduleIdentifier
            return nodeByModuleId.getOrPut(id) {
                val metadataModuleDependency =
                    (kotlinModule as? ExternalImportedKotlinModule)
                        ?.takeIf { it.hasLegacyMetadataModule }
                        // With the legacy publishing layout, the root module will have a single dependency (available-at) on the metadata
                        ?.let { dependency.resolvedDependencies.singleOrNull() }

                val metadataSource: ResolvedDependencyResult = metadataModuleDependency ?: dependency
                val gradleDependencyResultsByFragment = moduleDependenciesByFragments(kotlinModule, metadataSource.resolvedDependencies)
                val edgesToDependenciesByFragment = resolveDependenciesToGraphNodes(gradleDependencyResultsByFragment, ::nodeFromModule)

                GradleDependencyGraphNode(
                    kotlinModule,
                    edgesToDependenciesByFragment,
                    dependency,
                    metadataModuleDependency
                )
            }
        }

        val gradleGraphRoot = configurationToResolve(requestingModule).incoming.resolutionResult.root
        val gradleDependenciesByRootFragment = moduleDependenciesByFragments(requestingModule, gradleGraphRoot.resolvedDependencies)
        val allKotlinDependenciesByRootFragment = resolveDependenciesToGraphNodes(gradleDependenciesByRootFragment, ::nodeFromModule)
        val root = GradleDependencyGraphRoot(requestingModule, allKotlinDependenciesByRootFragment)

        return GradleDependencyGraph(requestingModule, root)
    }
}

/** This hierarchy of sealed classes is only needed because the root of the dependency graph is special as it does not come from a
 * Gradle dependency. */
sealed class AbstractGradleDependencyGraphNode(
    override val module: KotlinModule,
    final override val dependenciesByFragment: Map<KotlinModuleFragment, Iterable<GradleDependencyGraphNode>>
) : DependencyGraphNode(module, dependenciesByFragment)

class GradleDependencyGraphRoot(
    module: KotlinModule,
    dependenciesByFragment: Map<KotlinModuleFragment, Iterable<GradleDependencyGraphNode>>
) : AbstractGradleDependencyGraphNode(
    module, dependenciesByFragment
)

class GradleDependencyGraphNode(
    module: KotlinModule,
    dependenciesByFragment: Map<KotlinModuleFragment, Iterable<GradleDependencyGraphNode>>,
    val gradleDependency: ResolvedDependencyResult,
    /** If the PSM was provided by a different dependency, such as with legacy publishing layout using *-metadata
     * modules, then this property points to the PSM-providing dependency. */
    val metadataDependency: ResolvedDependencyResult?
) : AbstractGradleDependencyGraphNode(module, dependenciesByFragment)

class GradleDependencyGraph(
    override val requestingModule: KotlinGradleModule,
    override val root: AbstractGradleDependencyGraphNode
) : DependencyGraphResolution.DependencyGraph(requestingModule, root)
