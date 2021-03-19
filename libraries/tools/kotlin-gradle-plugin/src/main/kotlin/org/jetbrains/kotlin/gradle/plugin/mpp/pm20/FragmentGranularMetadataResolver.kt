/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20

import org.gradle.api.Project
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.plugin.mpp.ChooseVisibleSourceSetsImpl
import org.jetbrains.kotlin.gradle.plugin.mpp.MetadataDependencyResolution
import org.jetbrains.kotlin.gradle.plugin.mpp.getMetadataExtractor
import org.jetbrains.kotlin.gradle.plugin.mpp.getProjectStructureMetadata
import org.jetbrains.kotlin.project.model.*
import org.jetbrains.kotlin.utils.addToStdlib.flattenTo
import java.util.ArrayDeque

internal class FragmentGranularMetadataResolver(
    val requestingFragment: KotlinGradleFragment,
    val refinesParentResolvers: Lazy<Iterable<FragmentGranularMetadataResolver>>
) {
    val resolutions: Iterable<MetadataDependencyResolution> by lazy {
        doResolveMetadataDependencies()
    }

    private val project: Project
        get() = requestingFragment.containingModule.project

    private val parentResultsByModuleIdentifier: Map<KotlinModuleIdentifier, List<MetadataDependencyResolution>> by lazy {
        refinesParentResolvers.value.flatMap { it.resolutions }.groupBy { it.dependency.toSingleModuleIdentifier() }
    }

    private val metadataModuleBuilder = ProjectStructureMetadataModuleBuilder()
    private val projectModuleBuilder = GradleProjectModuleBuilder(true)
    private val moduleResolver = GradleModuleDependencyResolver(project, metadataModuleBuilder, projectModuleBuilder)
    private val variantResolver = GradleModuleVariantResolver(project)
    private val fragmentResolver = DefaultModuleFragmentsResolver(variantResolver)
    private val dependencyGraphResolver = GradleKotlinDependencyGraphResolver(project, moduleResolver)

    private fun doResolveMetadataDependencies(): Iterable<MetadataDependencyResolution> {
        val configurationToResolve = configurationToResolveMetadataDependencies(project, requestingFragment.containingModule)
        val resolvedComponentsByModuleId =
            configurationToResolve.incoming.resolutionResult.allComponents.associateBy { it.toSingleModuleIdentifier() }
        val resolvedDependenciesByModuleId =
            configurationToResolve.incoming.resolutionResult.allDependencies.filterIsInstance<ResolvedDependencyResult>()
                .flatMap { dependency -> dependency.requested.toModuleIdentifiers().map { id -> id to dependency } }.toMap()

        val dependencyGraph = dependencyGraphResolver.resolveDependencyGraph(requestingFragment.containingModule)

        if (dependencyGraph is DependencyGraphResolution.Unknown)
            error("unexpected failure in dependency graph resolution for $requestingFragment in $project")

        dependencyGraph as DependencyGraphResolution.DependencyGraph
        val fragmentsToInclude = requestingFragment.refinesClosure
        val requestedDependencies = dependencyGraph.root.dependenciesByFragment.filterKeys { it in fragmentsToInclude }.values.flatten()

        val visited = mutableSetOf<DependencyGraphNode>()
        val fragmentResolutionQueue = ArrayDeque<DependencyGraphNode>().apply {
            addAll(requestedDependencies)
        }

        val results = mutableSetOf<MetadataDependencyResolution>()

        while (fragmentResolutionQueue.isNotEmpty()) {
            val dependencyNode = fragmentResolutionQueue.removeFirst()
            visited.add(dependencyNode)

            val dependencyModule = dependencyNode.module

            val fragmentVisibility = fragmentResolver.getChosenFragments(requestingFragment, dependencyModule)
            val visibleFragments = (fragmentVisibility as? FragmentResolution.ChosenFragments)?.visibleFragments?.toList().orEmpty()
            val variantResolutions =
                (fragmentVisibility as? FragmentResolution.ChosenFragments)?.variantResolutions?.associateBy { it.requestingVariant }

            val visibleTransitiveDependencies =
                dependencyNode.dependenciesByFragment.filterKeys { it in visibleFragments }.values.flattenTo(mutableSetOf())

            //FIXME host-specific fragments

            fragmentResolutionQueue.addAll(visibleTransitiveDependencies.filter { it !in visited })

            val resolvedComponentResult = resolvedComponentsByModuleId.getValue(dependencyModule.moduleIdentifier)
            val isResolvedAsProject = resolvedComponentResult.toProjectOrNull(project)
            val result = when (dependencyModule) {
                is ExternalSyntheticKotlinModule -> {
                    MetadataDependencyResolution.KeepOriginalDependency(resolvedComponentResult, isResolvedAsProject)
                }
                else -> run {
                    val projectStructureMetadata = (dependencyModule as? ExternalImportedKotlinModule)?.projectStructureMetadata
                        ?: checkNotNull(getProjectStructureMetadata(project, resolvedComponentResult, configurationToResolve))

                    // FIXME host-specific metadata!!!

                    val metadataSourceComponent = when {
                        dependencyModule is ExternalImportedKotlinModule && dependencyModule.hasLegacyMetadataModule ->
                            resolvedComponentResult.dependencies.filterIsInstance<ResolvedDependencyResult>().singleOrNull()?.selected
                                ?: resolvedComponentResult
                        else -> resolvedComponentResult
                    }

                    val parentResolutionsForDependency =
                        parentResultsByModuleIdentifier[metadataSourceComponent.toSingleModuleIdentifier()].orEmpty()
                    val fragmentsVisibleByParents =
                        parentResolutionsForDependency.filterIsInstance<ChooseVisibleSourceSetsImpl>()
                            .flatMapTo(mutableSetOf()) { it.allVisibleSourceSetNames }
                    val visibleFragmentNames = visibleFragments.map { it.fragmentName }.toSet()

                    ChooseVisibleSourceSetsImpl(
                        metadataSourceComponent,
                        isResolvedAsProject,
                        projectStructureMetadata,
                        visibleFragmentNames,
                        visibleFragmentNames.minus(fragmentsVisibleByParents),
                        visibleTransitiveDependencies.map { resolvedDependenciesByModuleId.getValue(it.module.moduleIdentifier) }.toSet(),
                        checkNotNull(getMetadataExtractor(project, resolvedComponentResult, configurationToResolve, true))
                    )
                }
            }
            results.add(result)
            fragmentResolutionQueue.addAll(visibleTransitiveDependencies.filterNot(visited::contains))
        }

        val resultSourceComponents = results.mapTo(mutableSetOf()) { it.dependency }
        resolvedComponentsByModuleId.values.minus(resultSourceComponents).forEach {
            results.add(MetadataDependencyResolution.ExcludeAsUnrequested(it, it.toProjectOrNull(project)))
        }

        return results
    }
}