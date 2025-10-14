/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Project
import org.gradle.api.artifacts.component.ComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.attributes.Usage
import org.gradle.api.logging.Logger
import org.jetbrains.kotlin.gradle.utils.*

internal val Project.kotlinProjectStructureMetadataExtractorFactory: KotlinProjectStructureMetadataExtractorFactory
    get() = KotlinProjectStructureMetadataExtractorFactory.getOrCreate(this)

internal data class ProjectPathWithBuildPath(
    val projectPath: String,
    val buildPath: String,
)

internal sealed interface IKotlinProjectStructureMetadataExtractorFactory

internal class KotlinProjectStructureMetadataExtractorFactory
private constructor(
    private val logger: Logger,
) : IKotlinProjectStructureMetadataExtractorFactory {
    fun create(
        dependency: ResolvedDependencyResult,
        resolvedPsmConfiguration: LazyResolvedConfigurationWithArtifacts?,
    ): MppDependencyProjectStructureMetadataExtractor? {
        checkNotNull(resolvedPsmConfiguration) { "KotlinProjectStructureMetadataExtractorFactory must not receive null psmConfiguration" }

        val moduleId = dependency.selected.id

        val psmFile = findPsmFileOrNull(resolvedPsmConfiguration, moduleId)
        if (psmFile == null) {
            logger.warn("Could not find Kotlin project structure metadata for module $moduleId; Please report this: http://kotl.in/issue")
            return null
        }

        return ProjectStructureMetadataFileExtractor(
            projectStructureMetadataFile = psmFile
        )
    }

    private fun findPsmFileOrNull(
        resolvedPsmConfiguration: LazyResolvedConfigurationWithArtifacts,
        moduleId: ComponentIdentifier,
    ) = resolvedPsmConfiguration.getArtifacts(moduleId)
        .filter { it.variant.attributes.getAttribute(Usage.USAGE_ATTRIBUTE)?.name == KotlinUsages.KOTLIN_PSM_METADATA }
        .filter { it.file.name != EMPTY_PROJECT_STRUCTURE_METADATA_FILE_NAME }
        .map { it.file }
        .singleOrNull()

    companion object {
        fun getOrCreate(project: Project): KotlinProjectStructureMetadataExtractorFactory =
            KotlinProjectStructureMetadataExtractorFactory(
                logger = project.logger,
            )
    }
}
