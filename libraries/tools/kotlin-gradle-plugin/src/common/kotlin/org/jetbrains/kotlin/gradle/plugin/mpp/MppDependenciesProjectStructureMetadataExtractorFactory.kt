/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Project
import org.gradle.api.artifacts.component.ComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.attributes.Usage
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.PreparedKotlinToolingDiagnosticsCollector
import org.jetbrains.kotlin.gradle.utils.*

internal val Project.kotlinMppDependencyProjectStructureMetadataExtractorFactory: MppDependenciesProjectStructureMetadataExtractorFactory
    get() = MppDependenciesProjectStructureMetadataExtractorFactory.getOrCreate(this)

internal data class ProjectPathWithBuildPath(
    val projectPath: String,
    val buildPath: String,
)

internal interface IMppDependenciesProjectStructureMetadataExtractorFactory {
    fun create(
        metadataArtifact: ResolvedArtifactResult,
        dependency: ResolvedDependencyResult,
        diagnosticsCollector: PreparedKotlinToolingDiagnosticsCollector,
        resolvedPsmConfiguration: LazyResolvedConfiguration?,
    ): MppDependencyProjectStructureMetadataExtractor?
}

internal class MppDependenciesProjectStructureMetadataExtractorFactory
private constructor(
    private val currentBuild: CurrentBuildIdentifier,
) : IMppDependenciesProjectStructureMetadataExtractorFactory {
    override fun create(
        metadataArtifact: ResolvedArtifactResult,
        dependency: ResolvedDependencyResult,
        diagnosticsCollector: PreparedKotlinToolingDiagnosticsCollector,
        resolvedPsmConfiguration: LazyResolvedConfiguration?,
    ): MppDependencyProjectStructureMetadataExtractor? {
        checkNotNull(resolvedPsmConfiguration) { "MppDependenciesProjectStructureMetadataExtractorFactory must not receive null psmConfiguration" }

        val moduleId = metadataArtifact.variant.owner

        val psmFile = findPsmFileOrNull(resolvedPsmConfiguration, moduleId)

        // Dependency was resolved to Included Build of old Kotlin
        if (psmFile == null && moduleId is ProjectComponentIdentifier && moduleId !in currentBuild) {
            diagnosticsCollector.report(
                KotlinToolingDiagnostics.ProjectIsolationIncompatibleWithIncludedBuildsWithOldKotlinVersion(
                    dependency = dependency.requested.toString(),
                    includedProjectPath = moduleId.buildTreePathCompat
                ),
                reportOnce = true,
                key = dependency.requested.toString()
            )
            return null
        }

        //  Dependency was resolved to local project
        if (psmFile == null && moduleId is ProjectComponentIdentifier && moduleId in currentBuild) {
            error("Project structure metadata not found for project '${moduleId.projectPath}'")
        }

        /** If, for some reason, [org.jetbrains.kotlin.gradle.plugin.mpp.internal.ProjectStructureMetadataTransformAction]
         * didn't work, Fallback to JAR */
        if (psmFile == null) {
            return JarMppDependencyProjectStructureMetadataExtractor(metadataArtifact.file)
        }

        return ProjectMppDependencyProjectStructureMetadataExtractor(
            projectStructureMetadataFile = psmFile
        )
    }

    private fun findPsmFileOrNull(
        resolvedPsmConfiguration: LazyResolvedConfiguration,
        moduleId: ComponentIdentifier,
    ) = resolvedPsmConfiguration.getArtifacts(moduleId)
        .filter { it.variant.attributes.getAttribute(Usage.USAGE_ATTRIBUTE)?.name == KotlinUsages.KOTLIN_PSM_METADATA }
        .map { it.file }
        .singleOrNull()

    companion object {
        fun getOrCreate(project: Project): MppDependenciesProjectStructureMetadataExtractorFactory =
            MppDependenciesProjectStructureMetadataExtractorFactory(
                currentBuild = project.currentBuild
            )
    }
}
