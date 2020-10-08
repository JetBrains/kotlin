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
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull

internal object ModuleIds {
    fun fromDependency(dependency: Dependency) = when (dependency) {
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
        if ((component.id as? ProjectComponentIdentifier)?.build?.isCurrentBuild == false)
            ModuleDependencyIdentifier(component.moduleVersion?.group ?: "unspecified", component.moduleVersion?.name ?: "unspecified")
        else
            fromComponentId(thisProject, component.id)

    private fun idOfRootModule(project: Project): ModuleDependencyIdentifier =
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
}