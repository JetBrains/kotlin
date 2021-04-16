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
    private val requestingFragment: KotlinGradleFragment,
    private val refinesParentResolvers: Lazy<Iterable<FragmentGranularMetadataResolver>>
) {
    val resolutions: Iterable<MetadataDependencyResolution> by lazy {
        doResolveMetadataDependencies()
    }

    private val project: Project
        get() = requestingFragment.containingModule.project

    private val parentResultsByModuleIdentifier: Map<KotlinModuleIdentifier, List<MetadataDependencyResolution>> by lazy {
        refinesParentResolvers.value.flatMap { it.resolutions }.groupBy { it.dependency.toSingleModuleIdentifier() }
    }

    private val moduleResolver = GradleModuleDependencyResolver.getForCurrentBuild(project)
    private val variantResolver = GradleModuleVariantResolver.getForCurrentBuild(project)
    private val fragmentResolver = DefaultModuleFragmentsResolver(variantResolver)
    private val dependencyGraphResolver = GradleKotlinDependencyGraphResolver(moduleResolver)

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

        dependencyGraph as GradleDependencyGraph // refactor the type hierarchy to avoid this downcast? FIXME?
        val fragmentsToInclude = requestingFragment.refinesClosure
        val requestedDependencies = dependencyGraph.root.dependenciesByFragment.filterKeys { it in fragmentsToInclude }.values.flatten()

        val visited = mutableSetOf<GradleDependencyGraphNode>()
        val fragmentResolutionQueue = ArrayDeque<GradleDependencyGraphNode>(requestedDependencies)

        val results = mutableSetOf<MetadataDependencyResolution>()

        while (fragmentResolutionQueue.isNotEmpty()) {
            val dependencyNode = fragmentResolutionQueue.removeFirst()
            visited.add(dependencyNode)

            val dependencyModule = dependencyNode.module

            val fragmentVisibility = fragmentResolver.getChosenFragments(requestingFragment, dependencyModule)
            val chosenFragments = fragmentVisibility as? FragmentResolution.ChosenFragments
            val visibleFragments = chosenFragments?.visibleFragments?.toList().orEmpty()

            val visibleTransitiveDependencies =
                dependencyNode.dependenciesByFragment.filterKeys { it in visibleFragments }.values.flattenTo(mutableSetOf())

            fragmentResolutionQueue.addAll(visibleTransitiveDependencies.filter { it !in visited })

            val resolvedComponentResult = dependencyNode.selectedComponent
            val isResolvedAsProject = resolvedComponentResult.toProjectOrNull(project)
            val result = when (dependencyModule) {
                is ExternalPlainKotlinModule -> {
                    MetadataDependencyResolution.KeepOriginalDependency(resolvedComponentResult, isResolvedAsProject)
                }
                else -> run {
                    val metadataSourceComponent = dependencyNode.run { metadataSourceComponent ?: selectedComponent }

                    val metadataExtractor = getMetadataExtractor(project, resolvedComponentResult, configurationToResolve, true)

                    if (dependencyModule is ExternalImportedKotlinModule &&
                        metadataExtractor is JarArtifactMppDependencyMetadataExtractor &&
                        chosenFragments != null
                    ) {
                        resolveHostSpecificMetadataArtifacts(dependencyModule, chosenFragments, metadataExtractor)
                    }

                    val projectStructureMetadata = (dependencyModule as? ExternalImportedKotlinModule)?.projectStructureMetadata
                        ?: checkNotNull(metadataExtractor?.getProjectStructureMetadata())

                    val visibleFragmentNames = visibleFragments.map { it.fragmentName }.toSet()
                    val visibleFragmentNamesExcludingVisibleByParents =
                        visibleFragmentNames
                            .minus(fragmentsNamesVisibleByParents(metadataSourceComponent.toSingleModuleIdentifier()))

                    ChooseVisibleSourceSetsImpl(
                        metadataSourceComponent,
                        isResolvedAsProject,
                        projectStructureMetadata,
                        visibleFragmentNames,
                        visibleFragmentNamesExcludingVisibleByParents,
                        visibleTransitiveDependencies.map { resolvedDependenciesByModuleId.getValue(it.module.moduleIdentifier) }.toSet(),
                        checkNotNull(metadataExtractor)
                    )
                }
            }
            results.add(result)
        }

        // FIXME this code is based on whole components; use module IDs with classifiers instead
        val resultSourceComponents = results.mapTo(mutableSetOf()) { it.dependency }
        resolvedComponentsByModuleId.values.minus(resultSourceComponents).forEach {
            results.add(MetadataDependencyResolution.ExcludeAsUnrequested(it, it.toProjectOrNull(project)))
        }

        return results
    }

    private fun fragmentsNamesVisibleByParents(kotlinModuleIdentifier: KotlinModuleIdentifier): MutableSet<String> {
        val parentResolutionsForDependency = parentResultsByModuleIdentifier[kotlinModuleIdentifier].orEmpty()
        return parentResolutionsForDependency.filterIsInstance<ChooseVisibleSourceSetsImpl>()
            .flatMapTo(mutableSetOf()) { it.allVisibleSourceSetNames }
    }

    private fun resolveHostSpecificMetadataArtifacts(
        dependencyModule: ExternalImportedKotlinModule,
        chosenFragments: FragmentResolution.ChosenFragments,
        metadataExtractor: JarArtifactMppDependencyMetadataExtractor
    ) {
        val visibleFragments = chosenFragments.visibleFragments
        val variantResolutions = chosenFragments.variantResolutions
        val hostSpecificFragments = dependencyModule.hostSpecificFragments
        val hostSpecificFragmentToArtifact = visibleFragments.intersect(hostSpecificFragments).mapNotNull { hostSpecificFragment ->
            val relevantVariantResolution = variantResolutions
                .filterIsInstance<VariantResolution.VariantMatch>()
                // find some of our variants that resolved a dependency's variant containing the fragment
                .find { hostSpecificFragment in it.chosenVariant.refinesClosure }
            // resolve the dependencies of that variant getting the host-specific metadata artifact
            relevantVariantResolution?.let { resolution ->
                val configurationResolvingPlatformVariant =
                    (resolution.requestingVariant as KotlinGradleVariant).compileDependencyConfiguration
                val hostSpecificArtifact = ResolvedMppVariantsProvider.get(project)
                    .getHostSpecificMetadataArtifactByRootModule(
                        dependencyModule.moduleIdentifier,
                        configurationResolvingPlatformVariant
                    )
                hostSpecificArtifact?.let { hostSpecificFragment.fragmentName to it }
            }
        }
        metadataExtractor.metadataArtifactBySourceSet.putAll(hostSpecificFragmentToArtifact)
    }
}