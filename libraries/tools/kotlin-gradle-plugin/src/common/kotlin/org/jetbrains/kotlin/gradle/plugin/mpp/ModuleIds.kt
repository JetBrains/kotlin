/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.component.*
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.publish.maven.MavenPublication
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.PublishedModuleCoordinatesProvider
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.ComputedCapability
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.currentBuildId
import org.jetbrains.kotlin.gradle.utils.getValue
import org.jetbrains.kotlin.gradle.utils.isProjectComponentIdentifierInCurrentBuild
import org.jetbrains.kotlin.project.model.KpmLocalModuleIdentifier
import org.jetbrains.kotlin.project.model.KpmMavenModuleIdentifier
import org.jetbrains.kotlin.project.model.KpmModuleIdentifier

internal object ModuleIds {
    fun fromDependency(dependency: Dependency): ModuleDependencyIdentifier = when (dependency) {
        is ProjectDependency -> idOfRootModule(dependency.dependencyProject)
        else -> ModuleDependencyIdentifier(dependency.group, dependency.name)
    }

    fun fromComponentSelector(
        thisProject: Project,
        componentSelector: ComponentSelector
    ): ModuleDependencyIdentifier = when (componentSelector) {
        is ProjectComponentSelector -> idOfRootModuleByProjectPath(thisProject, componentSelector.projectPath)
        is ModuleComponentSelector -> ModuleDependencyIdentifier(componentSelector.group, componentSelector.module)
        else -> idFromName(componentSelector.displayName)
    }

    private fun fromComponentId(
        thisProject: Project,
        componentIdentifier: ComponentIdentifier
    ): ModuleDependencyIdentifier =
        when (componentIdentifier) {
            is ProjectComponentIdentifier -> idOfRootModuleByProjectPath(thisProject, componentIdentifier.projectPath)
            is ModuleComponentIdentifier -> ModuleDependencyIdentifier(componentIdentifier.group, componentIdentifier.module)
            else -> idFromName(componentIdentifier.displayName)
        }

    fun fromComponent(thisProject: Project, component: ResolvedComponentResult) =
        // If the project component comes from another build, we can't extract anything from it, so just use the module coordinates:
        if (!component.id.isProjectComponentIdentifierInCurrentBuild)
            ModuleDependencyIdentifier(component.moduleVersion?.group ?: "unspecified", component.moduleVersion?.name ?: "unspecified")
        else
            fromComponentId(thisProject, component.id)

    fun idOfRootModule(project: Project): ModuleDependencyIdentifier =
        if (project.multiplatformExtensionOrNull != null) {
            val rootPublication = project.multiplatformExtension.rootSoftwareComponent.publicationDelegate
            val group = rootPublication?.groupId ?: project.group.toString()
            val name = rootPublication?.artifactId ?: project.name
            ModuleDependencyIdentifier(group, name)
        } else {
            ModuleDependencyIdentifier(project.group.toString(), project.name)
        }

    private fun idFromName(name: String) =
        ModuleDependencyIdentifier(null, name)

    private fun idOfRootModuleByProjectPath(thisProject: Project, projectPath: String): ModuleDependencyIdentifier =
        idOfRootModule(thisProject.project(projectPath))

    // FIXME use capabilities to point to auxiliary modules
    fun lossyFromModuleIdentifier(thisProject: Project, moduleIdentifier: KpmModuleIdentifier): ModuleDependencyIdentifier {
        when (moduleIdentifier) {
            is KpmLocalModuleIdentifier -> {
                check(moduleIdentifier.buildId == thisProject.currentBuildId().name)
                val dependencyProject = thisProject.project(moduleIdentifier.projectId)
                val topLevelExtension = dependencyProject.topLevelExtension
                val getRootPublication: () -> MavenPublication? = when {
                    dependencyProject.pm20ExtensionOrNull != null -> {
                        { dependencyProject.pm20Extension.kpmModelContainer.rootPublication }
                    }
                    topLevelExtension is KotlinMultiplatformExtension -> {
                        { topLevelExtension.rootSoftwareComponent.publicationDelegate }
                    }
                    else -> error("unexpected top-level extension $topLevelExtension")
                }
                val capabilities = when {
                    dependencyProject.pm20ExtensionOrNull != null -> listOfNotNull(ComputedCapability.capabilityStringFromModule(
                        dependencyProject.pm20Extension.modules.single { it.moduleIdentifier == moduleIdentifier }
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

open class MavenPublicationCoordinatesProvider(
    project: Project,
    val getPublication: () -> MavenPublication?,
    defaultModuleSuffix: String?,
    override val capabilities: Iterable<String> = emptyList()
) : PublishedModuleCoordinatesProvider {

    override val group: String by project.provider {
        getPublication()?.groupId ?: project.group.toString()
    }

    override val name: String by project.provider {
        getPublication()?.artifactId ?: project.name.plus(defaultModuleSuffix?.let { "-$it" }.orEmpty())
    }

    override val version: String by project.provider {
        getPublication()?.version ?: project.version.toString()
    }
}
