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

        fun getKotlinModuleFromComponentResult(component: ResolvedComponentResult): KotlinModule =
            moduleResolver.resolveDependency(requestingModule, component.toModuleDependency())
                ?: buildSyntheticPlainModule(
                    component,
                    component.variants.singleOrNull()?.displayName ?: "default",
                    requestingModule.project
                )

        fun nodeFromModule(componentResult: ResolvedComponentResult, kotlinModule: KotlinModule): GradleDependencyGraphNode {
            val id = kotlinModule.moduleIdentifier
            return nodeByModuleId.getOrPut(id) {
                val metadataSourceComponent =
                    (kotlinModule as? ExternalImportedKotlinModule)
                        ?.takeIf { it.hasLegacyMetadataModule }
                        ?.let { (componentResult.dependencies.singleOrNull() as? ResolvedDependencyResult)?.selected }
                        ?: componentResult

                val dependenciesRequestedByModule =
                    kotlinModule.fragments.flatMap { fragment -> fragment.declaredModuleDependencies.map { it.moduleIdentifier } }.toSet()

                val resolvedComponentDependencies = metadataSourceComponent.dependencies
                    .filterIsInstance<ResolvedDependencyResult>()
                    // This filter statement is used to only visit the dependencies of the variant(s) of the requested Kotlin module and not
                    // other variants. This prevents infinite recursion when visiting multiple Kotlin modules within one Gradle components
                    .filter { dependency -> dependency.requested.toModuleIdentifiers().any { it in dependenciesRequestedByModule } }
                    .flatMap { dependency -> dependency.requested.toModuleIdentifiers().map { id -> id to dependency.selected } }
                    .toMap()

                val fragmentDependencies = kotlinModule.fragments.associateWith { it.declaredModuleDependencies }

                val nodeDependenciesMap = fragmentDependencies.mapValues { (_, deps) ->
                    deps.mapNotNull { resolvedComponentDependencies[it.moduleIdentifier] }.map {
                        val dependencyModule = getKotlinModuleFromComponentResult(it)
                        nodeFromModule(it, dependencyModule)
                    }
                }

                GradleDependencyGraphNode(
                    kotlinModule,
                    componentResult,
                    metadataSourceComponent,
                    nodeDependenciesMap
                )
            }
        }

        return GradleDependencyGraph(
            requestingModule,
            nodeFromModule(configurationToResolve(requestingModule).incoming.resolutionResult.root, requestingModule)
        )
    }
}

class GradleDependencyGraphNode(
    override val module: KotlinModule,
    val selectedComponent: ResolvedComponentResult,
    /** If the Kotlin module description was provided by a different component, such as with legacy publishing layout using *-metadata
     * modules, then this property points to the other component. */
    val metadataSourceComponent: ResolvedComponentResult?,
    override val dependenciesByFragment: Map<KotlinModuleFragment, Iterable<GradleDependencyGraphNode>>
) : DependencyGraphNode(module, dependenciesByFragment)

class GradleDependencyGraph(
    override val requestingModule: KotlinGradleModule,
    override val root: GradleDependencyGraphNode
) : DependencyGraphResolution.DependencyGraph(requestingModule, root)
