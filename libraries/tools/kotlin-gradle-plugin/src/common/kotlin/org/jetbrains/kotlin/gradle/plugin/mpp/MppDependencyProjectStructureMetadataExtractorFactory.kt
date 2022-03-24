/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.toSingleModuleIdentifier
import org.jetbrains.kotlin.project.model.KotlinModuleIdentifier

internal fun MppDependencyProjectStructureMetadataExtractor.Factory.create(
    project: Project,
    resolvedComponentResult: ResolvedComponentResult,
    configuration: Configuration,
    resolveViaAvailableAt: Boolean
): MppDependencyProjectStructureMetadataExtractor? {
    return create(
        resolvedMppVariantsProvider = ResolvedMppVariantsProvider.get(project),
        /*
        FIXME this loses information about auxiliary module deps
        TODO check how this code works with multi-capability resolutions,
         */
        moduleIdentifier = resolvedComponentResult.toSingleModuleIdentifier(),
        configuration = configuration,
        resolveViaAvailableAt = resolveViaAvailableAt,
        resolvedComponentResult = resolvedComponentResult,
        project = project
    )
}

internal fun MppDependencyProjectStructureMetadataExtractor.Factory.create(
    project: Project,
    resolvedComponentResult: ResolvedComponentResult,
    moduleIdentifier: KotlinModuleIdentifier,
    configuration: Configuration
): MppDependencyProjectStructureMetadataExtractor? {
    return create(
        resolvedMppVariantsProvider = ResolvedMppVariantsProvider.get(project),
        moduleIdentifier = moduleIdentifier,
        configuration = configuration,
        resolveViaAvailableAt = true,
        resolvedComponentResult = resolvedComponentResult,
        project = project
    )
}

private fun MppDependencyProjectStructureMetadataExtractor.Factory.create(
    resolvedMppVariantsProvider: ResolvedMppVariantsProvider,
    moduleIdentifier: KotlinModuleIdentifier,
    configuration: Configuration,
    resolveViaAvailableAt: Boolean,
    resolvedComponentResult: ResolvedComponentResult,
    project: Project
): MppDependencyProjectStructureMetadataExtractor? {
    var resolvedViaAvailableAt = false

    val metadataArtifact = resolvedMppVariantsProvider.getResolvedArtifactByPlatformModule(
        moduleIdentifier,
        configuration
    ) ?: if (resolveViaAvailableAt) {
        resolvedMppVariantsProvider.getHostSpecificMetadataArtifactByRootModule(
            moduleIdentifier, configuration
        )?.also {
            resolvedViaAvailableAt = true
        }
    } else null

    val actualComponent = if (resolvedViaAvailableAt) {
        resolvedComponentResult.dependencies.filterIsInstance<ResolvedDependencyResult>().singleOrNull()?.selected
            ?: resolvedComponentResult
    } else resolvedComponentResult

    val moduleId = actualComponent.id
    return when {
        moduleId is ProjectComponentIdentifier -> when {
            moduleId.build.isCurrentBuild ->
                ProjectMppDependencyProjectStructureMetadataExtractor(moduleIdentifier, project.project(moduleId.projectPath))
            metadataArtifact != null ->
                IncludedBuildMppDependencyProjectStructureMetadataExtractor(project, actualComponent, metadataArtifact)
            else -> null
        }
        metadataArtifact != null -> JarMppDependencyProjectStructureMetadataExtractor(metadataArtifact)
        else -> null
    }
}
