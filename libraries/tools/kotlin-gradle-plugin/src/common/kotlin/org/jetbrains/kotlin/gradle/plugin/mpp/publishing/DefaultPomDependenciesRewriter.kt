/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.publishing

import org.gradle.api.Project
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ModuleComponentSelector
import org.gradle.api.artifacts.component.ProjectComponentSelector
import org.jetbrains.kotlin.gradle.plugin.KotlinTargetComponent
import org.jetbrains.kotlin.gradle.plugin.internal.KotlinProjectSharedDataProvider
import org.jetbrains.kotlin.gradle.plugin.internal.kotlinSecondaryVariantsDataSharing
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmAndroidCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsageContext.MavenScope
import org.jetbrains.kotlin.gradle.plugin.mpp.internal
import org.jetbrains.kotlin.gradle.utils.lastExternalVariantOrSelf

internal fun Project.createDefaultPomDependenciesRewriterForTargetComponent(
    kotlinComponent: KotlinTargetComponent,
): DefaultPomDependenciesRewriter {
    val projectCoordinatesProviders = collectProjectsPublicationCoordinatesFromDependencies(project, kotlinComponent)
    return DefaultPomDependenciesRewriter(projectCoordinatesProviders)
}

internal class DefaultPomDependenciesRewriter(
    private val projectCoordinatesProviders: List<KotlinProjectSharedDataProvider<TargetPublicationCoordinates>>,
) : PomDependenciesRewriter() {
    override val taskDependencies: Any?
        get() = projectCoordinatesProviders.map { it.files }.takeIf { it.isNotEmpty() }

    override fun createDependenciesMappingForEachUsageContext(): List<Map<ModuleCoordinates, ModuleCoordinates>> {
        return projectCoordinatesProviders.map {
            associateDependenciesWithActualModuleDependencies(it)
                .filter { (from, to) -> from != to }
        }
    }

    private fun associateDependenciesWithActualModuleDependencies(
        projectCoordinatesProvider: KotlinProjectSharedDataProvider<TargetPublicationCoordinates>,
    ): Map<ModuleCoordinates, ModuleCoordinates> {
        val result = mutableMapOf<ModuleCoordinates, ModuleCoordinates>()
        for (resolvedDependency in projectCoordinatesProvider.allResolvedDependencies) {
            if (resolvedDependency.isConstraint) continue

            val projectCoordinates = projectCoordinatesProvider.getProjectDataFromDependencyOrNull(resolvedDependency)
            val requested = resolvedDependency.requested

            val from: ModuleCoordinates
            val to: ModuleCoordinates
            if (projectCoordinates != null) {
                // Project dependencies + Included Build
                from = when (requested) {
                    is ProjectComponentSelector -> projectCoordinates.rootPublicationCoordinates.moduleCoordinates
                    is ModuleComponentSelector -> ModuleCoordinates(requested.group, requested.module, requested.version)
                    else -> continue
                }
                to = projectCoordinates.targetPublicationCoordinates.moduleCoordinates
            } else {
                // External Maven Dependencies
                if (requested !is ModuleComponentSelector) continue
                if (!resolvedDependency.resolvedVariant.externalVariant.isPresent) continue

                val resolved = resolvedDependency
                    .resolvedVariant
                    .lastExternalVariantOrSelf()
                    .owner as? ModuleComponentIdentifier ?: continue

                from = ModuleCoordinates(requested.group, requested.module, requested.version)
                to = ModuleCoordinates(resolved.group, resolved.module, resolved.version)
            }

            result[from] = to
        }

        return result
    }
}

private val TargetPublicationCoordinates.GAV.moduleCoordinates
    get() = ModuleCoordinates(
        moduleGroup = group,
        moduleName = artifactId,
        moduleVersion = version
    )

internal fun collectProjectsPublicationCoordinatesFromDependencies(
    project: Project,
    component: KotlinTargetComponent,
): List<KotlinProjectSharedDataProvider<TargetPublicationCoordinates>> {
    val projectDataSharingService = project.kotlinSecondaryVariantsDataSharing

    return component.internal.usages.mapNotNull { usage ->
        val mavenScope = usage.mavenScope ?: return@mapNotNull null
        val compilation = usage.compilation
        val dependenciesConfiguration = project.configurations.getByName(
            when (compilation) {
                is KotlinJvmAndroidCompilation -> {
                    // TODO handle Android configuration names in a general way once we drop AGP < 3.0.0
                    val variantName = compilation.name
                    when (mavenScope) {
                        MavenScope.COMPILE -> variantName + "CompileClasspath"
                        MavenScope.RUNTIME -> variantName + "RuntimeClasspath"
                    }
                }
                else -> when (mavenScope) {
                    MavenScope.COMPILE -> compilation.compileDependencyConfigurationName
                    MavenScope.RUNTIME -> compilation.runtimeDependencyConfigurationName ?: return@mapNotNull null
                }
            }
        )

        return@mapNotNull projectDataSharingService.consumeTargetPublicationCoordinates(dependenciesConfiguration)
    }
}