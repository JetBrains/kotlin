/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilationToRunnableFiles
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.sources.KotlinDependencyScope
import java.io.File

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

internal class SourceSetVisibilityProvider(
    private val project: Project
) {
    private val resolvedVariantsProvider = ResolvedMppVariantsProvider.get(project)

    /**
     * Determine which source sets of the [resolvedRootMppDependency] are visible in the [visibleFrom] source set.
     *
     * This requires resolving dependencies of the compilations which [visibleFrom] takes part in, in order to find which variants the
     * [resolvedRootMppDependency] got resolved to for those compilations.
     *
     * Once the variants are known, they are checked against the [dependencyProjectStructureMetadata], and the
     * source sets of the dependency are determined that are compiled for all those variants and thus should be visible here.
     *
     * If the [resolvedRootMppDependency] is a project dependency, its project should be passed as [resolvedToOtherProject], as
     * the Gradle API for dependency variants behaves differently for project dependencies and published ones.
     */
    fun getVisibleSourceSets(
        visibleFrom: KotlinSourceSet,
        dependencyScopes: Iterable<KotlinDependencyScope>,
        resolvedRootMppDependency: ResolvedComponentResult?,
        resolvedMetadataDependency: ResolvedComponentResult,
        dependencyProjectStructureMetadata: KotlinProjectStructureMetadata,
        resolvedToOtherProject: Project?
    ): SourceSetVisibilityResult {
        val compilations = CompilationSourceSetUtil.compilationsBySourceSets(project).getValue(visibleFrom)

        val mppModuleIdentifier = ModuleIds.fromComponent(project, resolvedRootMppDependency ?: resolvedMetadataDependency)

        val firstConfigurationByVariant = mutableMapOf<String, Configuration>()

        val visiblePlatformVariantNames: Set<String?> =
            compilations
                .filter { it.target.platformType != KotlinPlatformType.common }
                .flatMapTo(mutableSetOf()) { compilation ->
                    // To find out which variant the MPP dependency got resolved for each compilation, take the resolvable configurations
                    // that we have in the compilations:
                    dependencyScopes.mapNotNull { scope -> project.resolvableConfigurationFromCompilationByScope(compilation, scope) }
                }
                .mapTo(mutableSetOf()) { configuration ->
                    val resolvedVariant = resolvedVariantsProvider.getResolvedVariantName(mppModuleIdentifier, configuration)
                        ?.let { kotlinVariantNameFromPublishedVariantName(it) }
                        ?: return@mapTo null

                    firstConfigurationByVariant.putIfAbsent(resolvedVariant, configuration)
                    resolvedVariant
                }

        if (visiblePlatformVariantNames.isEmpty()) {
            return SourceSetVisibilityResult(emptySet(), emptyMap())
        }

        val visibleSourceSetNames = dependencyProjectStructureMetadata.sourceSetNamesByVariantName
            .filterKeys { it in visiblePlatformVariantNames }
            .values.let { if (it.isEmpty()) emptySet() else it.reduce { acc, item -> acc intersect item } }

        val hostSpecificArtifactBySourceSet: Map<String, File> =
            if (resolvedToOtherProject != null) {
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
                            .filterKeys { it in firstConfigurationByVariant }
                            .filterValues { sourceSetName in it }
                            .keys.first()
                    }

                someVariantByHostSpecificSourceSet.entries.mapNotNull { (sourceSetName, variantName) ->
                    val configuration = firstConfigurationByVariant.getValue(variantName)
                    resolvedVariantsProvider.getMetadataArtifactByRootModule(mppModuleIdentifier, configuration)
                        ?.let { sourceSetName to it }
                }.toMap()
            }

        return SourceSetVisibilityResult(
            visibleSourceSetNames,
            hostSpecificArtifactBySourceSet
        )
    }
}

private fun kotlinVariantNameFromPublishedVariantName(resolvedToVariantName: String): String =
    originalVariantNameFromPublished(resolvedToVariantName) ?: resolvedToVariantName

private fun Project.resolvableConfigurationFromCompilationByScope(
    compilation: KotlinCompilation<*>,
    scope: KotlinDependencyScope
): Configuration? {
    val configurationName = when (scope) {
        KotlinDependencyScope.API_SCOPE, KotlinDependencyScope.IMPLEMENTATION_SCOPE, KotlinDependencyScope.COMPILE_ONLY_SCOPE -> compilation.compileDependencyConfigurationName

        KotlinDependencyScope.RUNTIME_ONLY_SCOPE ->
            (compilation as? KotlinCompilationToRunnableFiles<*>)?.runtimeDependencyConfigurationName
                ?: return null
    }

    return project.configurations.getByName(configurationName)
}