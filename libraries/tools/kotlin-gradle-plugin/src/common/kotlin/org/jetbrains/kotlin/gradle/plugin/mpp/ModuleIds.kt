/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.component.ComponentIdentifier
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
import org.jetbrains.kotlin.gradle.plugin.mpp.publishing.kotlinMultiplatformRootPublication
import org.jetbrains.kotlin.gradle.utils.currentBuild
import org.jetbrains.kotlin.gradle.utils.future

internal object ModuleIds {
    fun fromDependency(project: Project, dependency: Dependency): ModuleDependencyIdentifier = when (dependency) {
        is ProjectDependency -> {
            val dependencyProject = if (GradleVersion.current().baseVersion < GradleVersion.version("9.0")) {
                // This API was removed in 9.0-M4 - https://github.com/gradle/gradle/commit/3a27d546a713568343413f622c872b2f052ea757
                @Suppress("DEPRECATION") dependency.dependencyProject
            } else {
                project.project(dependency.path)
            }
            @Suppress("DEPRECATION_ERROR")
            idOfRootModule(dependencyProject)
        }
        else -> ModuleDependencyIdentifier(dependency.group, dependency.name)
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
        if (component is ProjectComponentIdentifier && component !in thisProject.currentBuild)
            ModuleDependencyIdentifier(component.moduleVersion?.group ?: "unspecified", component.moduleVersion?.name ?: "unspecified")
        else
            fromComponentId(thisProject, component.id)

    // TODO KT-62911: Replace unsafe idOfRootModule with suspendable version idRootModule
    @Deprecated(
        "Use suspendable version if possible. Scheduled for removal in Kotlin 2.3.",
        replaceWith = ReplaceWith("idOfRootModuleSafe(project)"),
        level = DeprecationLevel.ERROR
    )
    fun idOfRootModule(project: Project): ModuleDependencyIdentifier = project.future { idOfRootModuleSafe(project) }.getOrThrow()

    suspend fun idOfRootModuleSafe(project: Project): ModuleDependencyIdentifier =
        if (project.multiplatformExtensionOrNull != null) {
            val rootPublication = project.kotlinMultiplatformRootPublication.await()
            val group = rootPublication?.groupId ?: project.group.toString()
            val name = rootPublication?.artifactId ?: project.name
            ModuleDependencyIdentifier(group, name)
        } else {
            ModuleDependencyIdentifier(project.group.toString(), project.name)
        }

    private fun idFromName(name: String) =
        ModuleDependencyIdentifier(null, name)

    private fun idOfRootModuleByProjectPath(thisProject: Project, projectPath: String): ModuleDependencyIdentifier =
        @Suppress("DEPRECATION_ERROR")
        idOfRootModule(thisProject.project(projectPath))
}