/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Project
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.extraProperties
import org.jetbrains.kotlin.gradle.plugin.mpp.SourceSetVisibilityProvider.PlatformCompilationData
import org.jetbrains.kotlin.gradle.utils.LazyResolvedConfiguration
import org.jetbrains.kotlin.gradle.utils.dependencyArtifactsOrNull
import org.jetbrains.kotlin.gradle.utils.getOrPut
import java.io.File

private typealias KotlinSourceSetName = String

internal data class SourceSetVisibilityResult(
    /**
     * Names of source sets that the consumer sees from the requested dependency.
     */
    val visibleSourceSetNames: Set<String>,

    /**
     * For some of the [visibleSourceSetNames], additional artifacts may be present that
     * the consumer should read the compiled source set metadata from.
     */
    val hostSpecificMetadataArtifactBySourceSet: Map<String, File>
)

private val Project.allPlatformCompilationData: List<PlatformCompilationData>
    get() = extraProperties
    .getOrPut("all${PlatformCompilationData::class.java.simpleName}") { collectAllPlatformCompilationData() }

private fun Project.collectAllPlatformCompilationData(): List<PlatformCompilationData> {
    val multiplatformExtension = multiplatformExtensionOrNull ?: return emptyList()
    return multiplatformExtension
        .targets
        .filter { it.platformType != KotlinPlatformType.common }
        .flatMap { target -> target.compilations.map { it.toPlatformCompilationData() } }
}

private fun KotlinCompilation<*>.toPlatformCompilationData() = PlatformCompilationData(
    allSourceSets = allKotlinSourceSets.map { it.name }.toSet(),
    resolvedDependenciesConfiguration = LazyResolvedConfiguration(internal.configurations.compileDependencyConfiguration),
    hostSpecificMetadataConfiguration = internal
        .configurations
        .hostSpecificMetadataConfiguration
        ?.let(::LazyResolvedConfiguration)
)

internal class SourceSetVisibilityProvider(
    private val platformCompilations: List<PlatformCompilationData>,
) {
    constructor(project: Project) : this(
        platformCompilations = project.allPlatformCompilationData
    )

    class PlatformCompilationData(
        val allSourceSets: Set<KotlinSourceSetName>,
        val resolvedDependenciesConfiguration: LazyResolvedConfiguration,
        val hostSpecificMetadataConfiguration: LazyResolvedConfiguration?
    )

    /**
     * Determine which source sets of the [resolvedRootMppDependency] are visible in the [visibleFromSourceSet] source set.
     *
     * This requires resolving dependencies of the compilations which [visibleFromSourceSet] takes part in, in order to find which variants the
     * [resolvedRootMppDependency] got resolved to for those compilations.
     *
     * Once the variants are known, they are checked against the [dependencyProjectStructureMetadata], and the
     * source sets of the dependency are determined that are compiled for all those variants and thus should be visible here.
     *
     * If the [resolvedRootMppDependency] is a project dependency, its project should be passed as [resolvedToOtherProject], as
     * the Gradle API for dependency variants behaves differently for project dependencies and published ones.
     */
    fun getVisibleSourceSets(
        visibleFromSourceSet: KotlinSourceSetName,
        resolvedRootMppDependency: ResolvedDependencyResult,
        dependencyProjectStructureMetadata: KotlinProjectStructureMetadata,
        resolvedToOtherProject: Boolean
    ): SourceSetVisibilityResult {
        val resolvedRootMppDependencyId = resolvedRootMppDependency.selected.id

        val platformCompilationsByResolvedVariantName = mutableMapOf<String, PlatformCompilationData>()

        val visiblePlatformVariantNames: Set<String?> = platformCompilations
            .filter { visibleFromSourceSet in it.allSourceSets }
            .map { platformCompilationData ->
                val resolvedPlatformDependency = platformCompilationData
                    .resolvedDependenciesConfiguration
                    .allResolvedDependencies
                    .find { it.selected.id == resolvedRootMppDependencyId }
                /*
                Returning null if we can't find the given dependency in a certain platform compilations dependencies.
                This is not expected, since this means the dependency does not support the given targets which will
                lead to a dependency resolution error.

                Esoteric cases can still get into this branch: e.g. broken publications (or broken .m2 and mavenLocal()).
                In this case we just return null, effectively ignoring this situation for this algorithm.

                Ignoring this will still lead to a more graceful behaviour in the IDE.
                A broken publication will potentially lead to 'too many' source sets being visible, which is
                more desirable than having none.
                 */ ?: return@map null

                val resolvedVariant = kotlinVariantNameFromPublishedVariantName(
                    resolvedPlatformDependency.resolvedVariant.displayName
                )

                if (resolvedVariant !in platformCompilationsByResolvedVariantName) {
                    platformCompilationsByResolvedVariantName[resolvedVariant] = platformCompilationData
                }

                resolvedVariant
            }.toSet()

        if (visiblePlatformVariantNames.isEmpty()) {
            return SourceSetVisibilityResult(emptySet(), emptyMap())
        }

        val visibleSourceSetNames = dependencyProjectStructureMetadata.sourceSetNamesByVariantName
            .filterKeys { it in visiblePlatformVariantNames }
            .values.let { if (it.isEmpty()) emptySet() else it.reduce { acc, item -> acc intersect item } }

        val hostSpecificArtifactBySourceSet: Map<String, File> =
            if (resolvedToOtherProject) {
                /**
                 * When a dependency resolves to a project, we don't need any artifacts from it, we can
                 * instead use the compilation outputs directly:
                 */
                emptyMap()
            } else {
                val hostSpecificSourceSets = visibleSourceSetNames.intersect(dependencyProjectStructureMetadata.hostSpecificSourceSets)

                /**
                 * As all of the variants normally contain the same metadata for each of the relevant host-specific source sets,
                 * any of the variants that we resolved can be used, so choose the first one that satisfies both:
                 *
                 *  - it contains the host-specific source set, and
                 *  - we have resolved it for some compilation
                 */
                val someVariantByHostSpecificSourceSet =
                    hostSpecificSourceSets.associate { sourceSetName ->
                        sourceSetName to dependencyProjectStructureMetadata.sourceSetNamesByVariantName
                            .filterKeys { it in platformCompilationsByResolvedVariantName }
                            .filterValues { sourceSetName in it }
                            .keys.first()
                    }

                someVariantByHostSpecificSourceSet.entries.mapNotNull { (sourceSetName, variantName) ->
                    val resolvedHostSpecificMetadataConfiguration = platformCompilationsByResolvedVariantName
                        .getValue(variantName)
                        .hostSpecificMetadataConfiguration
                        ?: return@mapNotNull null

                    val dependency = resolvedHostSpecificMetadataConfiguration
                        .allResolvedDependencies
                        .find { it.selected.id == resolvedRootMppDependencyId }
                        ?: return@mapNotNull null

                    val metadataArtifact = resolvedHostSpecificMetadataConfiguration
                        // it can happen that related host-specific metadata artifact doesn't exist
                        // for example on linux machines, then just gracefully return null
                        .dependencyArtifactsOrNull(dependency)
                        ?.singleOrNull()
                        ?: return@mapNotNull null

                    sourceSetName to metadataArtifact.file
                }.toMap()
            }

        return SourceSetVisibilityResult(
            visibleSourceSetNames,
            hostSpecificArtifactBySourceSet
        )
    }
}

internal fun kotlinVariantNameFromPublishedVariantName(resolvedToVariantName: String): String =
    originalVariantNameFromPublished(resolvedToVariantName) ?: resolvedToVariantName
