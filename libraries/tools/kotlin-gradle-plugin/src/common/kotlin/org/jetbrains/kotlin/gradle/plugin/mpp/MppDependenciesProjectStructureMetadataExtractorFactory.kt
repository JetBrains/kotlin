/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Project
import org.gradle.api.artifacts.component.ComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.attributes.Usage
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
        resolvedMetadataConfiguration: LazyResolvedConfiguration?,
    ): MppDependencyProjectStructureMetadataExtractor
}

internal class MppDependenciesProjectStructureMetadataExtractorFactory
private constructor(
    private val currentBuild: CurrentBuildIdentifier,
    private val includedBuildsProjectStructureMetadataProviders: Lazy<Map<ProjectPathWithBuildPath, Lazy<KotlinProjectStructureMetadata?>>>,
) : IMppDependenciesProjectStructureMetadataExtractorFactory {
    override fun create(
        metadataArtifact: ResolvedArtifactResult,
        resolvedMetadataConfiguration: LazyResolvedConfiguration?,
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

                if (isCompositeProjectContainsExtractedPsm(metadataArtifact)) {
                    /*
                    For MPP projects starting from 2.1.0, we have consumable/resolvable configurations to get PSM
                    Such an approach prevents project-isolation violations.
                    */
                    getProjectMppDependencyProjectStructureMetadataExtractorForCompositProject(resolvedMetadataConfiguration, moduleId)
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
            if (isCompositeProjectContainsExtractedPsm(metadataArtifact)) {
                getProjectMppDependencyProjectStructureMetadataExtractorForCompositProject(resolvedMetadataConfiguration, moduleId)
            } else {
                JarMppDependencyProjectStructureMetadataExtractor(metadataArtifact.file)
            }
        }
    }

    private fun isCompositeProjectContainsExtractedPsm(metadataArtifact: ResolvedArtifactResult): Boolean {
        // For some reason all attributes in variant hierarchy sometimes have value type String,
        // that is why just call .getAttribut(Usage.USAGE_ATTRIBUTE) will always return null.
        // So we need at first find the attribute with Usage.USAGE_ATTRIBUTE name and only after it get its value.
        val usageAttribute = metadataArtifact.variant
            .attributes
            .keySet()
            .singleOrNull { attribute -> attribute.name == Usage.USAGE_ATTRIBUTE.name }

        if (null == usageAttribute) {
            return false
        }

        return metadataArtifact.variant.attributes.getAttribute(usageAttribute).toString() in listOf(
            KotlinUsages.KOTLIN_PSM_METADATA,
            KotlinUsages.KOTLIN_LOCAL_METADATA
        )
    }

    private fun getProjectMppDependencyProjectStructureMetadataExtractorForCompositProject(
        resolvedMetadataConfiguration: LazyResolvedConfiguration?,
        moduleId: ComponentIdentifier,
    ): ProjectMppDependencyProjectStructureMetadataExtractor {
        val projectStructureMetadataFileForCurrentModuleId =
            getProjectStructureMetadataFileForCurrentModuleId(resolvedMetadataConfiguration, moduleId)
        return ProjectMppDependencyProjectStructureMetadataExtractor(
            projectStructureMetadataFile = projectStructureMetadataFileForCurrentModuleId
        )
    }

    private fun getProjectStructureMetadataFileForCurrentModuleId(
        resolvedMetadataConfiguration: LazyResolvedConfiguration?,
        moduleId: ComponentIdentifier?,
    ) = resolvedMetadataConfiguration?.resolvedArtifacts
        ?.filter { it.id.componentIdentifier == moduleId }
        ?.map { it.file }
        ?.singleOrNull()

    companion object {
        fun getOrCreate(project: Project): MppDependenciesProjectStructureMetadataExtractorFactory =
            MppDependenciesProjectStructureMetadataExtractorFactory(
                currentBuild = project.currentBuild,
                lazy { GlobalProjectStructureMetadataStorage.getProjectStructureMetadataProvidersFromAllGradleBuilds(project) },
            )
    }
}
