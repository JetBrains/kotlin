/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20

import org.gradle.api.Project
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.plugin.mpp.MetadataDependencyResolution.ChooseVisibleSourceSets.MetadataProvider.ArtifactMetadataProvider
import org.jetbrains.kotlin.project.model.*
import org.jetbrains.kotlin.utils.addToStdlib.flattenTo
import java.io.File
import java.util.*

internal class GradleKpmFragmentGranularMetadataResolver(
    private val requestingFragment: GradleKpmFragment,
    private val refinesParentResolvers: Lazy<Iterable<GradleKpmFragmentGranularMetadataResolver>>
) {
    val resolutions: Iterable<MetadataDependencyResolution> by lazy {
        doResolveMetadataDependencies()
    }

    private val project: Project
        get() = requestingFragment.containingModule.project

    private val parentResultsByModuleIdentifier: Map<KpmModuleIdentifier, List<MetadataDependencyResolution>> by lazy {
        refinesParentResolvers.value.flatMap { it.resolutions }.groupBy { it.dependency.toSingleKpmModuleIdentifier() }
    }

    private val moduleResolver = GradleKpmModuleDependencyResolver.getForCurrentBuild(project)
    private val variantResolver = KpmGradleModuleVariantResolver.getForCurrentBuild(project)
    private val fragmentResolver = KpmDefaultFragmentsResolver(variantResolver)
    private val dependencyGraphResolver = GradleKpmDependencyGraphResolver(moduleResolver)

    @Suppress("UNREACHABLE_CODE", "UNUSED_VARIABLE")
    private fun doResolveMetadataDependencies(): Iterable<MetadataDependencyResolution> {
        val configurationToResolve = configurationToResolveMetadataDependencies(requestingFragment.containingModule)
        val resolvedComponentsByModuleId =
            configurationToResolve.incoming.resolutionResult.allComponents.associateBy { it.toSingleKpmModuleIdentifier() }
        val resolvedDependenciesByModuleId =
            configurationToResolve.incoming.resolutionResult.allDependencies.filterIsInstance<ResolvedDependencyResult>()
                .flatMap { dependency -> dependency.requested.toKpmModuleIdentifiers().map { id -> id to dependency } }.toMap()

        val dependencyGraph = dependencyGraphResolver.resolveDependencyGraph(requestingFragment.containingModule)

        if (dependencyGraph is KpmDependencyGraphResolution.Unknown)
            error("unexpected failure in dependency graph resolution for $requestingFragment in $project")

        dependencyGraph as GradleKpmDependencyGraph // refactor the type hierarchy to avoid this downcast? FIXME?
        val fragmentsToInclude = requestingFragment.withRefinesClosure
        val requestedDependencies = dependencyGraph.root.dependenciesByFragment.filterKeys { it in fragmentsToInclude }.values.flatten()

        val visited = mutableSetOf<GradleKpmDependencyGraphNode>()
        val fragmentResolutionQueue = ArrayDeque<GradleKpmDependencyGraphNode>(requestedDependencies)

        val results = mutableSetOf<MetadataDependencyResolution>()

        while (fragmentResolutionQueue.isNotEmpty()) {
            val dependencyNode = fragmentResolutionQueue.removeFirst()
            if (!visited.add(dependencyNode)) {
                continue
            }

            val dependencyModule = dependencyNode.module

            val fragmentVisibility = fragmentResolver.getChosenFragments(requestingFragment, dependencyModule)
            val chosenFragments = fragmentVisibility as? KpmFragmentResolution.ChosenFragments
            val visibleFragments = chosenFragments?.visibleFragments?.toList().orEmpty()

            val visibleTransitiveDependencies =
                dependencyNode.dependenciesByFragment.filterKeys { it in visibleFragments }.values.flattenTo(mutableSetOf())

            fragmentResolutionQueue.addAll(visibleTransitiveDependencies.filter { it !in visited })

            val resolvedComponentResult = dependencyNode.selectedComponent
            val result = when (dependencyModule) {
                is GradleKpmExternalPlainModule -> {
                    MetadataDependencyResolution.KeepOriginalDependency(resolvedComponentResult)
                }

                else -> run {

                    val metadataSourceComponent = dependencyNode.run { metadataSourceComponent ?: selectedComponent }

                    val visibleFragmentNames = visibleFragments.map { it.fragmentName }.toSet()
                    val visibleFragmentNamesExcludingVisibleByParents =
                        visibleFragmentNames.minus(fragmentsNamesVisibleByParents(metadataSourceComponent.toSingleKpmModuleIdentifier()))

                    /*
                    We can safely assume that a metadata extractor can be created, because the project structure metadata already
                    had to be read in order to create the Kotlin module and infer fragment visibility.
                    */
                    val projectStructureMetadataExtractor: MppDependencyProjectStructureMetadataExtractor =
                        TODO("Implement for KPM. As it done for TCS")

                    val projectStructureMetadata = (dependencyModule as? GradleKpmExternalImportedModule)?.projectStructureMetadata
                        ?: checkNotNull(projectStructureMetadataExtractor.getProjectStructureMetadata())


                    val metadataProvider = when (projectStructureMetadataExtractor) {
                        is ProjectMppDependencyProjectStructureMetadataExtractor -> TODO("Implement ProjectStructureMetadata for KPM")

                        is JarMppDependencyProjectStructureMetadataExtractor -> ArtifactMetadataProvider(
                            CompositeMetadataArtifactImpl(
                                moduleDependencyIdentifier = ModuleIds.fromComponent(project, metadataSourceComponent),
                                moduleDependencyVersion = metadataSourceComponent.moduleVersion?.version ?: "unspecified",
                                kotlinProjectStructureMetadata = projectStructureMetadata,
                                primaryArtifactFile = projectStructureMetadataExtractor.primaryArtifactFile,
                                hostSpecificArtifactFilesBySourceSetName = if (
                                    dependencyModule is GradleKpmExternalImportedModule && chosenFragments != null
                                ) resolveHostSpecificMetadataArtifacts(dependencyModule, chosenFragments) else emptyMap(),
                            )
                        )
                    }

                    MetadataDependencyResolution.ChooseVisibleSourceSets(
                        dependency = metadataSourceComponent,
                        projectStructureMetadata = projectStructureMetadata,
                        allVisibleSourceSetNames = visibleFragmentNames,
                        visibleSourceSetNamesExcludingDependsOn = visibleFragmentNamesExcludingVisibleByParents,
                        visibleTransitiveDependencies =
                        visibleTransitiveDependencies.map { resolvedDependenciesByModuleId.getValue(it.module.moduleIdentifier) }.toSet(),
                        metadataProvider = metadataProvider
                    )
                }
            }
            results.add(result)
        }

        // FIXME this code is based on whole components; use module IDs with classifiers instead
        val resultSourceComponents = results.mapTo(mutableSetOf()) { it.dependency }
        resolvedComponentsByModuleId.values.minus(resultSourceComponents).forEach {
            results.add(MetadataDependencyResolution.Exclude.Unrequested(it))
        }

        return results
    }

    private fun fragmentsNamesVisibleByParents(kotlinModuleIdentifier: KpmModuleIdentifier): MutableSet<String> {
        val parentResolutionsForDependency = parentResultsByModuleIdentifier[kotlinModuleIdentifier].orEmpty()
        return parentResolutionsForDependency.filterIsInstance<MetadataDependencyResolution.ChooseVisibleSourceSets>()
            .flatMapTo(mutableSetOf()) { it.allVisibleSourceSetNames }
    }

    private fun resolveHostSpecificMetadataArtifacts(
        dependencyModule: GradleKpmExternalImportedModule,
        chosenFragments: KpmFragmentResolution.ChosenFragments,
    ): Map<String, File> {
        val visibleFragments = chosenFragments.visibleFragments
        val variantResolutions = chosenFragments.variantResolutions
        val hostSpecificFragments = dependencyModule.hostSpecificFragments
        return visibleFragments.intersect(hostSpecificFragments).mapNotNull { hostSpecificFragment ->
            val relevantVariantResolution = variantResolutions
                .filterIsInstance<KpmVariantResolution.KpmVariantMatch>()
                // find some of our variants that resolved a dependency's variant containing the fragment
                .find { hostSpecificFragment in it.chosenVariant.withRefinesClosure }
            // resolve the dependencies of that variant getting the host-specific metadata artifact
            @Suppress("UNREACHABLE_CODE", "UNUSED_VARIABLE")
            relevantVariantResolution?.let { resolution ->
                val configurationResolvingPlatformVariant =
                    (resolution.requestingVariant as GradleKpmVariant).compileDependenciesConfiguration
                val hostSpecificArtifact: File? = TODO("Implement host-specific lookup for KPM as it done for TCS")
                hostSpecificArtifact?.let { hostSpecificFragment.fragmentName to it }
            }
        }.toMap()
    }
}
