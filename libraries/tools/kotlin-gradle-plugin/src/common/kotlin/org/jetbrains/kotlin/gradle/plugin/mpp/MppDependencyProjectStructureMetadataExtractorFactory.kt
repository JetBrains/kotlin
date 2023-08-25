/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Project
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
import org.jetbrains.kotlin.gradle.plugin.extraProperties
import org.jetbrains.kotlin.gradle.utils.*
import org.jetbrains.kotlin.gradle.utils.CurrentBuildIdentifier
import org.jetbrains.kotlin.gradle.utils.buildNameCompat
import org.jetbrains.kotlin.gradle.utils.buildPathCompat
import org.jetbrains.kotlin.gradle.utils.getOrPut

internal val Project.kotlinMppDependencyProjectStructureMetadataExtractorFactory: MppDependencyProjectStructureMetadataExtractorFactory
    get() = MppDependencyProjectStructureMetadataExtractorFactory.getOrCreate(this)

internal data class ProjectPathWithBuildPath(
    val projectPath: String,
    val buildPath: String,
)

internal class MppDependencyProjectStructureMetadataExtractorFactory
private constructor(
    private val currentBuild: CurrentBuildIdentifier,
    private val includedBuildsProjectStructureMetadataProviders: Lazy<Map<ProjectPathWithBuildPath, Lazy<KotlinProjectStructureMetadata?>>>,
    private val currentBuildProjectStructureMetadataProviders: Map<String, Lazy<KotlinProjectStructureMetadata?>>,
) {
    fun create(
        metadataArtifact: ResolvedArtifactResult,
    ): MppDependencyProjectStructureMetadataExtractor {
        val moduleId = metadataArtifact.variant.owner

        return if (moduleId is ProjectComponentIdentifier) {
            if (moduleId in currentBuild) {
                val projectStructureMetadataProvider = currentBuildProjectStructureMetadataProviders[moduleId.projectPath]
                    ?: error("Project structure metadata not found for project '${moduleId.projectPath}'")

                ProjectMppDependencyProjectStructureMetadataExtractor(
                    projectPath = moduleId.projectPath,
                    projectStructureMetadataProvider = projectStructureMetadataProvider::value
                )
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
        } else {
            JarMppDependencyProjectStructureMetadataExtractor(metadataArtifact.file)
        }
    }

    companion object {
        private val extensionName = MppDependencyProjectStructureMetadataExtractorFactory::class.java.simpleName
        fun getOrCreate(project: Project): MppDependencyProjectStructureMetadataExtractorFactory =
            project.rootProject.extraProperties.getOrPut(extensionName) {
                MppDependencyProjectStructureMetadataExtractorFactory(
                    currentBuild = project.currentBuild,
                    lazy { GlobalProjectStructureMetadataStorage.getProjectStructureMetadataProvidersFromAllGradleBuilds(project) },
                    collectAllProjectStructureMetadataInCurrentBuild(project)
                )
            }
    }
}

private fun collectAllProjectStructureMetadataInCurrentBuild(project: Project): Map<String, Lazy<KotlinProjectStructureMetadata?>> =
    project.rootProject.allprojects.associate { subproject ->
        subproject.path to lazy { subproject.multiplatformExtensionOrNull?.kotlinProjectStructureMetadata }
    }
