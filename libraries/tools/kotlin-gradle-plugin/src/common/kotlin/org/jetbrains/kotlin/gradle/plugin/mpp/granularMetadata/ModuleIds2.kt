/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.granularMetadata

import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.component.*
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.file.Directory
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Provider
import org.gradle.api.publish.maven.MavenPublication
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.dsl.topLevelExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.PublishedModuleCoordinatesProvider
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.hasKpmModel
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.kpmModelContainer
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.kpmModules
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.ComputedCapability
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.currentBuildId
import org.jetbrains.kotlin.gradle.tasks.KotlinCompileCommon
import org.jetbrains.kotlin.gradle.utils.getValue
import org.jetbrains.kotlin.gradle.utils.withType
import org.jetbrains.kotlin.project.model.KpmLocalModuleIdentifier
import org.jetbrains.kotlin.project.model.KpmMavenModuleIdentifier
import org.jetbrains.kotlin.project.model.KpmModuleIdentifier

data class ProjectData(
    val name: String,
    val group: String,
    val hasMppExtension: Boolean,
    val publicationDelegate: MavenPublication?,
    val sourceSetsMetadataOutputs: Provider<Map<String, Directory?>>
) {
    constructor(project: Project) : this(
        name = project.name,
        group = project.group.toString(),
        hasMppExtension = project.multiplatformExtensionOrNull != null,
        publicationDelegate = project.multiplatformExtensionOrNull?.rootSoftwareComponent?.publicationDelegate,
        sourceSetsMetadataOutputs = project.multiplatformExtensionOrNull?.sourceSetsMetadataOutputs() ?: project.provider { emptyMap() }
    )
}

private fun KotlinMultiplatformExtension.sourceSetsMetadataOutputs(): Provider<Map<String, Directory?>> {
    return project.provider {
        val commonTarget = targets.withType<KotlinMetadataTarget>().singleOrNull() ?: return@provider emptyMap()

        val compilations = commonTarget.compilations

        sourceSets.associate { sourceSet ->
            val task = compilations.findByName(sourceSet.name)?.compileKotlinTask as? KotlinCompileCommon?
            Pair(sourceSet.name, task?.destinationDirectory?.get())
        }
    }
}

internal class ModuleIds2(
    private val projectData: Map<String, ProjectData>
) {
    fun fromDependency(dependency: DependencyId): ModuleDependencyIdentifier = when (dependency) {
        is DependencyId.ProjectDependency -> idOfRootModule(projectData[dependency.projectId.path] ?: error("Unknown project $dependency"))
        is DependencyId.ExternalDependency -> ModuleDependencyIdentifier(dependency.group, dependency.name)
    }

    fun fromComponentSelector(
        componentSelector: ComponentSelector
    ): ModuleDependencyIdentifier = when (componentSelector) {
        is ProjectComponentSelector -> idOfRootModuleByProjectPath(componentSelector.projectPath)
        is ModuleComponentSelector -> ModuleDependencyIdentifier(componentSelector.group, componentSelector.module)
        else -> idFromName(componentSelector.displayName)
    }

    fun fromComponentId(
        componentIdentifier: ComponentIdentifier
    ): ModuleDependencyIdentifier =
        when (componentIdentifier) {
            is ProjectComponentIdentifier -> idOfRootModuleByProjectPath(componentIdentifier.projectPath)
            is ModuleComponentIdentifier -> ModuleDependencyIdentifier(componentIdentifier.group, componentIdentifier.module)
            else -> idFromName(componentIdentifier.displayName)
        }

    fun fromComponent(component: ResolvedComponentResult) =
        // If the project component comes from another build, we can't extract anything from it, so just use the module coordinates:
        if ((component.id as? ProjectComponentIdentifier)?.build?.isCurrentBuild == false)
            ModuleDependencyIdentifier(component.moduleVersion?.group ?: "unspecified", component.moduleVersion?.name ?: "unspecified")
        else
            fromComponentId(component.id)

    private fun idOfRootModule(project: ProjectData): ModuleDependencyIdentifier =
        if (project.hasMppExtension) {
            val rootPublication = project.publicationDelegate
            val group = rootPublication?.groupId ?: project.group
            val name = rootPublication?.artifactId ?: project.name
            ModuleDependencyIdentifier(group, name)
        } else {
            ModuleDependencyIdentifier(project.group, project.name)
        }

    private fun idFromName(name: String) =
        ModuleDependencyIdentifier(null, name)

    private fun idOfRootModuleByProjectPath(projectPath: String): ModuleDependencyIdentifier =
        idOfRootModule(projectData[projectPath] ?: error("Project not found by path: $projectPath"))

    // FIXME use capabilities to point to auxiliary modules
    fun lossyFromModuleIdentifier(thisProject: Project, moduleIdentifier: KpmModuleIdentifier): ModuleDependencyIdentifier {
        when (moduleIdentifier) {
            is KpmLocalModuleIdentifier -> {
                check(moduleIdentifier.buildId == thisProject.currentBuildId().name)
                val dependencyProject = thisProject.project(moduleIdentifier.projectId)
                val topLevelExtension = dependencyProject.topLevelExtension
                val getRootPublication: () -> MavenPublication? = when {
                    dependencyProject.hasKpmModel -> {
                        { dependencyProject.kpmModelContainer.rootPublication }
                    }
                    topLevelExtension is KotlinMultiplatformExtension -> {
                        { topLevelExtension.rootSoftwareComponent.publicationDelegate }
                    }
                    else -> error("unexpected top-level extension $topLevelExtension")
                }
                val capabilities = when {
                    dependencyProject.hasKpmModel -> listOfNotNull(
                        ComputedCapability.capabilityStringFromModule(
                        dependencyProject.kpmModules.single { it.moduleIdentifier == moduleIdentifier }
                    ))
                    topLevelExtension is KotlinMultiplatformExtension -> emptyList()
                    else -> error("unexpected top-level extension $topLevelExtension")
                }
                val coordinatesProvider = MavenPublicationCoordinatesProvider(
                    dependencyProject,
                    getRootPublication,
                    defaultModuleSuffix = null,
                    capabilities = capabilities
                )
                return ChangingModuleDependencyIdentifier({ coordinatesProvider.group }, { coordinatesProvider.name })
            }
            is KpmMavenModuleIdentifier -> {
                return ModuleDependencyIdentifier(moduleIdentifier.group, moduleIdentifier.name)
            }
            else -> error("unexpected module identifier $moduleIdentifier")
        }
    }
}

//open class MavenPublicationCoordinatesProvider(
//    project: Project,
//    val getPublication: () -> MavenPublication?,
//    defaultModuleSuffix: String?,
//    override val capabilities: Iterable<String> = emptyList()
//) : PublishedModuleCoordinatesProvider {
//
//    override val group: String by project.provider {
//        getPublication()?.groupId ?: project.group.toString()
//    }
//
//    override val name: String by project.provider {
//        getPublication()?.artifactId ?: project.name.plus(defaultModuleSuffix?.let { "-$it" }.orEmpty())
//    }
//
//    override val version: String by project.provider {
//        getPublication()?.version ?: project.version.toString()
//    }
//}