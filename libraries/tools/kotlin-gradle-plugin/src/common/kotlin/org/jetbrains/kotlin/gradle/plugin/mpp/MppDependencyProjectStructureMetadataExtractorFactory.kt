/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Project
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
import org.jetbrains.kotlin.gradle.plugin.extraProperties
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.toSingleKpmModuleIdentifier
import org.jetbrains.kotlin.gradle.utils.getOrPut

internal val Project.kotlinMppDependencyProjectStructureMetadataExtractorFactory: MppDependencyProjectStructureMetadataExtractorFactory
    get() = MppDependencyProjectStructureMetadataExtractorFactory.getOrCreate(this)

internal data class ProjectPathWithBuildName(
    val projectPath: String,
    val buildName: String
)

internal class MppDependencyProjectStructureMetadataExtractorFactory
private constructor(
    private val allProjectStructureMetadataProviders: Lazy<Map<ProjectPathWithBuildName, Lazy<KotlinProjectStructureMetadata?>>>
) {
    fun create(
        metadataArtifact: ResolvedArtifactResult
    ): MppDependencyProjectStructureMetadataExtractor {
        val moduleId = metadataArtifact.variant.owner

        return if (moduleId is ProjectComponentIdentifier) {
            val key = ProjectPathWithBuildName(moduleId.projectPath, moduleId.build.name)
            val projectStructureMetadataProvider = allProjectStructureMetadataProviders.value[key]
                ?: error("Project structure metadata not found for project $key")

            if (moduleId.build.isCurrentBuild) {
                ProjectMppDependencyProjectStructureMetadataExtractor(
                    moduleIdentifier = metadataArtifact.variant.toSingleKpmModuleIdentifier(),
                    projectPath = moduleId.projectPath,
                    projectStructureMetadataProvider = projectStructureMetadataProvider::value
                )
            } else {
                IncludedBuildMppDependencyProjectStructureMetadataExtractor(
                    componentId = moduleId,
                    primaryArtifact = metadataArtifact.file,
                    projectStructureMetadataProvider = projectStructureMetadataProvider::value
                )
            }
        } else {
            JarMppDependencyProjectStructureMetadataExtractor(metadataArtifact.file)
        }
    }

    companion object {
        private val extensionName = MppDependencyProjectStructureMetadataExtractorFactory::class.java.simpleName
        fun getOrCreate(project: Project): MppDependencyProjectStructureMetadataExtractorFactory =
            project.extraProperties.getOrPut(extensionName) {
                val allProjectStructureMetadataProviders = lazy {
                    GlobalProjectStructureMetadataStorage
                        .getAllProjectStructureMetadata(project)
                }

                MppDependencyProjectStructureMetadataExtractorFactory(allProjectStructureMetadataProviders)
            }
    }
}
