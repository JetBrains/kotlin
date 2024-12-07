/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Project
import org.gradle.api.artifacts.component.ComponentIdentifier
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.SourceSetVisibilityProvider.PlatformCompilationData
import org.jetbrains.kotlin.gradle.utils.LazyResolvedConfiguration
import org.jetbrains.kotlin.gradle.utils.dependencyArtifactsOrNull
import org.jetbrains.kotlin.gradle.utils.projectStoredProperty
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
    val hostSpecificMetadataArtifactBySourceSet: Map<String, File>,
)

private val Project.allPlatformCompilationData: List<PlatformCompilationData> by projectStoredProperty {
    collectAllPlatformCompilationData()
}

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
        val hostSpecificMetadataConfiguration: LazyResolvedConfiguration?,
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
        resolvedToOtherProject: Boolean,
    ): SourceSetVisibilityResult {
        val resolvedRootMppDependencyId = resolvedRootMppDependency.selected.id

        val platformCompilationsByResolvedVariantName = mutableMapOf<String, PlatformCompilationData>()

        val visiblePlatformVariantNames: List<Set<String>> = platformCompilations
            .filter { visibleFromSourceSet in it.allSourceSets }
            .mapNotNull { platformCompilationData ->
                val resolvedPlatformDependencies = platformCompilationData
                    .resolvedDependenciesConfiguration
                    .allResolvedDependencies
                    .filter { it.selected.id isEqualsIgnoringVersion resolvedRootMppDependencyId }
                    /*
                    Returning null if we can't find the given dependency in a certain platform compilations dependencies.
                    This is not expected, since this means the dependency does not support the given targets which will
                    lead to a dependency resolution error.

                    Esoteric cases can still get into this branch: e.g. broken publications (or broken .m2 and mavenLocal()).
                    In this case we just return null, effectively ignoring this situation for this algorithm.

                    Ignoring this will still lead to a more graceful behaviour in the IDE.
                    A broken publication will potentially lead to 'too many' source sets being visible, which is
                    more desirable than having none.
                    */
                    .ifEmpty { return@mapNotNull null }

                resolvedPlatformDependencies.map { resolvedPlatformDependency ->
                    val resolvedVariant = kotlinVariantNameFromPublishedVariantName(
                        resolvedPlatformDependency.resolvedVariant.displayName
                    )

                    if (resolvedVariant !in platformCompilationsByResolvedVariantName) {
                        platformCompilationsByResolvedVariantName[resolvedVariant] = platformCompilationData
                    }

                    resolvedVariant
                }.toSet()
            }

        if (visiblePlatformVariantNames.isEmpty()) {
            return SourceSetVisibilityResult(emptySet(), emptyMap())
        }

        val visibleSourceSetNames = visiblePlatformVariantNames
            .mapNotNull { platformVariants ->
                platformVariants
                    .map { dependencyProjectStructureMetadata.sourceSetNamesByVariantName[it].orEmpty() }
                    // join together visible source sets from multiple variants of the same platform
                    .fold(emptySet<String>()) { acc, item -> acc union item }
                    .ifEmpty { null }
            }
            // intersect visible variants from different platforms
            .ifEmpty { listOf(emptySet()) } // to avoid calling reduce on an empty list
            .reduce { acc, item -> acc intersect item }

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

                    // It can happen that host-specific artifact is mentioned in resolve but it doesn't exist physically
                    // then again gracefully return null
                    val metadataArtifactFile = metadataArtifact.file
                    if (!metadataArtifactFile.exists()) return@mapNotNull null

                    sourceSetName to metadataArtifact.file
                }.toMap()
            }

        /**
         * Sort from more to less target specific source sets.
         * So that actuals will be first in the library path.
         * e.g. linuxMain, nativeMain, commonMain.
         */
        val sortedVisibleSourceSets = sortSourceSetsByDependsOnRelation(
            visibleSourceSetNames,
            dependencyProjectStructureMetadata.sourceSetsDependsOnRelation
        )

        return SourceSetVisibilityResult(
            sortedVisibleSourceSets.toSet(),
            hostSpecificArtifactBySourceSet
        )
    }
}

/**
 * Sorts the source sets based on the dependsOn relation from [KotlinProjectStructureMetadata]
 *
 * @param sourceSetsDependsOnRelation should contain direct dependsOn edges.
 *
 * For example, given this "dependsOn" closure: linuxMain -> nativeMain -> commonMain
 * [sourceSetsDependsOnRelation] would have the following values:
 *
 * ```kotlin
 * mapOf(
 *   linuxMain to setOf(nativeMain),
 *   nativeMain to setOf(commonMain),
 *   commonMain to emptySet()
 * )
 * ```
 *
 * Then calling [sortSourceSetsByDependsOnRelation] with [sourceSets] as listOf(nativeMain, commonMain, linuxMain) should
 * result to listOf(linuxMain, nativeMain, commonMain)
 *
 * And for [sortSourceSetsByDependsOnRelation] with this structure: jvmAndJs -> commonMain; linuxMain -> nativeMain -> commonMain;
 * the result can be one of the following lists:
 * * linuxMain, nativeMain, jvmAndJs, commonMain
 * * linuxMain, jvmAndJs, nativeMain, commonMain
 * * jvmAndJs, linuxMain, nativeMain, commonMain
 *
 * Because jvmAndJs has no dependsOn relation with linuxMain and nativeMain they can be treated equally.
 *
 * Implementation uses an algorithm for Topological Sorting with DFS.
 */
internal fun sortSourceSetsByDependsOnRelation(
    sourceSets: Set<String>,
    sourceSetsDependsOnRelation: Map<String, Set<String>>,
): List<String> {
    val visited = mutableSetOf<String>()
    val result = mutableListOf<String>()
    for (sourceSet in sourceSets) {
        if (!visited.add(sourceSet)) continue

        fun dfs(sourceSet: String) {
            val children = sourceSetsDependsOnRelation[sourceSet].orEmpty()
            for (child in children) {
                if (!visited.add(child)) continue
                dfs(child)
            }
            // We're only interested in input source sets
            if (sourceSet in sourceSets) result.add(sourceSet)
        }
        dfs(sourceSet)
    }

    return result.reversed()
}

internal fun kotlinVariantNameFromPublishedVariantName(resolvedToVariantName: String): String =
    originalVariantNameFromPublished(resolvedToVariantName) ?: resolvedToVariantName

/**
 * Returns true when two components identifiers are from the same maven module (group + name)
 * Gradle projects can't be resolved into multiple versions since there is only one version of a project in gradle build
 */
private infix fun ComponentIdentifier.isEqualsIgnoringVersion(that: ComponentIdentifier): Boolean {
    if (this is ProjectComponentIdentifier && that is ProjectComponentIdentifier) return this == that
    if (this is ModuleComponentIdentifier && that is ModuleComponentIdentifier) return this.moduleIdentifier == that.moduleIdentifier
    return false
}
