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
import org.jetbrains.kotlin.gradle.dsl.pm20ExtensionOrNull
import org.jetbrains.kotlin.gradle.plugin.mpp.resolvableMetadataConfiguration
import org.jetbrains.kotlin.gradle.plugin.sources.KotlinDependencyScope
import org.jetbrains.kotlin.project.model.*

internal fun resolvableMetadataConfiguration(
    module: GradleKpmModule
) = module.project.configurations.getByName(module.resolvableMetadataConfigurationName)

internal fun configurationToResolveMetadataDependencies(project: Project, requestingModule: KpmModule): Configuration =
    when {
        project.pm20ExtensionOrNull != null -> resolvableMetadataConfiguration(requestingModule as GradleKpmModule)
        else -> resolvableMetadataConfiguration(
            project,
            project.kotlinExtension.sourceSets, // take dependencies from all source sets; TODO introduce consistency scopes?
            KotlinDependencyScope.compileScopes
        )
    }


class GradleKpmDependencyGraphResolver(
    private val moduleResolver: KpmModuleDependencyResolver
) : KpmDependencyGraphResolver {

    private fun configurationToResolve(requestingModule: GradleKpmModule): Configuration =
        configurationToResolveMetadataDependencies(requestingModule.project, requestingModule)

    override fun resolveDependencyGraph(requestingModule: KpmModule): KpmDependencyGraphResolution {
        if (requestingModule !is GradleKpmModule)
            return KpmDependencyGraphResolution.Unknown(requestingModule)
        return resolveAsGraph(requestingModule)
    }

    private fun resolveAsGraph(requestingModule: GradleKpmModule): GradleKpmDependencyGraph {
        val nodeByModuleId = mutableMapOf<KpmModuleIdentifier, GradleKpmDependencyGraphNode>()

        fun getKotlinModuleFromComponentResult(component: ResolvedComponentResult): KpmModule =
            moduleResolver.resolveDependency(requestingModule, component.toKpmModuleDependency())
                ?: buildSyntheticPlainModule(
                    component,
                    component.variants.singleOrNull()?.displayName ?: "default",
                )

        fun nodeFromModule(componentResult: ResolvedComponentResult, kpmModule: KpmModule): GradleKpmDependencyGraphNode {
            val id = kpmModule.moduleIdentifier
            return nodeByModuleId.getOrPut(id) {
                val metadataSourceComponent =
                    (kpmModule as? GradleKpmExternalImportedModule)
                        ?.takeIf { it.hasLegacyMetadataModule }
                        ?.let { (componentResult.dependencies.singleOrNull() as? ResolvedDependencyResult)?.selected }
                        ?: componentResult

                val dependenciesRequestedByModule =
                    kpmModule.fragments.flatMap { fragment -> fragment.declaredModuleDependencies.map { it.moduleIdentifier } }.toSet()

                val resolvedComponentDependencies = metadataSourceComponent.dependencies
                    .filterIsInstance<ResolvedDependencyResult>()
                    // This filter statement is used to only visit the dependencies of the variant(s) of the requested Kotlin module and not
                    // other variants. This prevents infinite recursion when visiting multiple Kotlin modules within one Gradle components
                    .filter { dependency -> dependency.requested.toKpmModuleIdentifiers().any { it in dependenciesRequestedByModule } }
                    .flatMap { dependency -> dependency.requested.toKpmModuleIdentifiers().map { id -> id to dependency.selected } }
                    .toMap()

                val fragmentDependencies = kpmModule.fragments.associateWith { it.declaredModuleDependencies }

                val nodeDependenciesMap = fragmentDependencies.mapValues { (_, deps) ->
                    deps.mapNotNull { resolvedComponentDependencies[it.moduleIdentifier] }.map {
                        val dependencyModule = getKotlinModuleFromComponentResult(it)
                        nodeFromModule(it, dependencyModule)
                    }
                }

                GradleKpmDependencyGraphNode(
                    kpmModule,
                    componentResult,
                    metadataSourceComponent,
                    nodeDependenciesMap
                )
            }
        }

        return GradleKpmDependencyGraph(
            requestingModule,
            nodeFromModule(configurationToResolve(requestingModule).incoming.resolutionResult.root, requestingModule)
        )
    }
}

class GradleKpmDependencyGraphNode(
    override val module: KpmModule,
    val selectedComponent: ResolvedComponentResult,
    /** If the Kotlin module description was provided by a different component, such as with legacy publishing layout using *-metadata
     * modules, then this property points to the other component. */
    val metadataSourceComponent: ResolvedComponentResult?,
    override val dependenciesByFragment: Map<KpmFragment, Iterable<GradleKpmDependencyGraphNode>>
) : KpmDependencyGraphNode(module, dependenciesByFragment)

class GradleKpmDependencyGraph(
    override val requestingModule: GradleKpmModule,
    override val root: GradleKpmDependencyGraphNode
) : KpmDependencyGraphResolution.KpmDependencyGraph(requestingModule, root)
