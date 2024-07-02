/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Project
import org.gradle.api.artifacts.component.ComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.jetbrains.kotlin.gradle.utils.*

internal val Project.kotlinMppDependencyProjectStructureMetadataExtractorFactory: MppDependencyProjectStructureMetadataMppDependenciesProjectStucureMetadataExtractorFactory
    get() = MppDependencyProjectStructureMetadataMppDependenciesProjectStucureMetadataExtractorFactory.getOrCreate(this)

internal data class ProjectPathWithBuildPath(
    val projectPath: String,
    val buildPath: String,
)
internal interface IMppDependenciesProjectStucureMetadataExtractorFactory {
    fun create(
        metadataArtifact: ResolvedArtifactResult,
        resolvedMetadataConfiguration: LazyResolvedConfiguration,
    ): MppDependencyProjectStructureMetadataExtractor
}
internal class MppDependencyProjectStructureMetadataMppDependenciesProjectStucureMetadataExtractorFactory
private constructor(
    private val currentBuild: CurrentBuildIdentifier,
    private val includedBuildsProjectStructureMetadataProviders: Lazy<Map<ProjectPathWithBuildPath, Lazy<KotlinProjectStructureMetadata?>>>,
): IMppDependenciesProjectStucureMetadataExtractorFactory {
    override fun create(
        metadataArtifact: ResolvedArtifactResult,
        resolvedMetadataConfiguration: LazyResolvedConfiguration,
    ): MppDependencyProjectStructureMetadataExtractor {
        val moduleId = metadataArtifact.variant.owner

        return if (moduleId is ProjectComponentIdentifier) {
            if (moduleId in currentBuild) {
                val projectStructureMetadataFileForCurrentModuleId =
                    getProjectStructureMetadataFileForCurrentModuleId(resolvedMetadataConfiguration, moduleId)
                        ?: error("Project structure metadata not found for project '${moduleId.projectPath}'")

                ProjectMppDependencyProjectStructureMetadataExtractor(
                    projectPath = moduleId.projectPath,
                    projectStructureMetadataFile = projectStructureMetadataFileForCurrentModuleId
                )
            } else {
                /*
                    For MPP projects strarting from 2.0.20 we are consumable/resolvable configurations to get PSM
                    Such approach prevents project-isolation violations.
                 */
                val projectStructureMetadataFileForCurrentModuleId =
                    getProjectStructureMetadataFileForCurrentModuleId(resolvedMetadataConfiguration, moduleId)
                if (projectStructureMetadataFileForCurrentModuleId != null) {
                    IncludedBuildMppDependencyProjectStructureMetadataExtractor(
                        primaryArtifact = metadataArtifact.file,
                        projectStructureMetadataProvider = { null },
                        projectStructureMetadataFile = projectStructureMetadataFileForCurrentModuleId)
                } else {
                    /*
                    We switched to using 'buildPath' instead of 'buildName' in 1.9.20,
                    (See: https://youtrack.jetbrains.com/issue/KT-58157/)

                    In order for 1.9.20 projects to consume included builds with lesser KGP versions,
                    we will still query this 'legacy key' which is the key we expect older KGP versions to use.
                    */
                    val pre1920Key = ProjectPathWithBuildPath(moduleId.projectPath, moduleId.build.buildNameCompat)
                    val key = ProjectPathWithBuildPath(moduleId.projectPath, moduleId.build.buildPathCompat)

                    IncludedBuildMppDependencyProjectStructureMetadataExtractor(
                        primaryArtifact = metadataArtifact.file,
                        projectStructureMetadataProvider = {
                            includedBuildsProjectStructureMetadataProviders.value[key]?.value
                                ?: includedBuildsProjectStructureMetadataProviders.value[pre1920Key]?.value
                        }
                    )
                }
            }
        } else {
            JarMppDependencyProjectStructureMetadataExtractor(metadataArtifact.file)
        }
    }

    private fun getProjectStructureMetadataFileForCurrentModuleId(
        resolvedMetadataConfiguration: LazyResolvedConfiguration,
        moduleId: ComponentIdentifier?,
    ) = resolvedMetadataConfiguration.resolvedArtifacts
        .filter { it.id.componentIdentifier == moduleId }
        .map { it.file }
        .singleOrNull()

    companion object {
        fun getOrCreate(project: Project): MppDependencyProjectStructureMetadataMppDependenciesProjectStucureMetadataExtractorFactory =
            MppDependencyProjectStructureMetadataMppDependenciesProjectStucureMetadataExtractorFactory(
                currentBuild = project.currentBuild,
                lazy { GlobalProjectStructureMetadataStorage.getProjectStructureMetadataProvidersFromAllGradleBuilds(project) },
            )
    }
}
