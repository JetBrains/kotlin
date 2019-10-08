/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ResolvedDependency
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilationToRunnableFiles
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.sources.KotlinDependencyScope
import org.jetbrains.kotlin.gradle.targets.metadata.getPublishedPlatformCompilations

internal class SourceSetVisibilityProvider(
    private val project: Project
) {
    /**
     * Determine which source sets of the [resolvedMppDependency] are visible in the [visibleFrom] source set.
     *
     * This requires resolving dependencies of the compilations which [visibleFrom] takes part in, in order to find which variants the
     * [resolvedMppDependency] got resolved to for those compilations. The [resolvedMppDependency] should therefore be the dependency
     * on the 'root' module of the MPP (such as 'com.example:lib-foo', not 'com.example:lib-foo-metadata').
     *
     * Once the variants are known, they are checked against the [dependencyProjectStructureMetadata], and the
     * source sets of the dependency are determined that are compiled for all those variants and thus should be visible here.
     *
     * If the [resolvedMppDependency] is a project dependency, its project should be passed as [resolvedToOtherProject], as
     * the Gradle API for dependency variants behaves differently for project dependencies and published ones.
     */
    fun getVisibleSourceSetNames(
        visibleFrom: KotlinSourceSet,
        dependencyScopes: Iterable<KotlinDependencyScope>,
        resolvedMppDependency: ResolvedDependency,
        dependencyProjectStructureMetadata: KotlinProjectStructureMetadata,
        resolvedToOtherProject: Project?
    ): Set<String> {
        val compilations = CompilationSourceSetUtil.compilationsBySourceSets(project).getValue(visibleFrom)

        var visiblePlatformVariantNames: Set<String> =
            compilations
                .filter { it.target.platformType != KotlinPlatformType.common }
                .flatMapTo(mutableSetOf()) { compilation ->
                    val configurations = dependencyScopes.mapNotNullTo(mutableSetOf()) { scope ->
                        project.resolvableConfigurationFromCompilationByScope(compilation, scope)
                    }
                    configurations.mapNotNull { configuration ->
                        // Resolve the configuration but don't trigger artifacts download, only download component metadata:
                        configuration.incoming.resolutionResult.allComponents
                            .find {
                                it.moduleVersion?.group == resolvedMppDependency.moduleGroup &&
                                        it.moduleVersion?.name == resolvedMppDependency.moduleName
                            }
                            ?.variant?.displayName
                    }
                }

        if (visiblePlatformVariantNames.isEmpty()) {
            return emptySet()
        }

        if (resolvedToOtherProject != null) {
            val publishedVariants = getPublishedPlatformCompilations(resolvedToOtherProject).keys

            visiblePlatformVariantNames = visiblePlatformVariantNames.mapTo(mutableSetOf()) { configurationName ->
                publishedVariants.first { it.dependencyConfigurationName == configurationName }.name
            }
        }

        return dependencyProjectStructureMetadata.sourceSetNamesByVariantName
            .filterKeys { it in visiblePlatformVariantNames }
            .values.let { if (it.isEmpty()) emptySet() else it.reduce { acc, item -> acc intersect item } }
    }
}

internal fun Project.resolvableConfigurationFromCompilationByScope(
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

