/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.internal

import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.VariantImplementationFactories
import org.jetbrains.kotlin.gradle.plugin.variantImplementationFactory

typealias ProjectByPath = (String) -> Project

interface ProjectDependencyAccessor {
    fun dependencyProject(): Project

    interface Factory : VariantImplementationFactories.VariantImplementationFactory {
        fun getInstance(
            dependency: ProjectDependency,
            projectByPath: ProjectByPath,
        ): ProjectDependencyAccessor
    }
}

internal class DefaultProjectDependencyAccessor(
    private val dependency: ProjectDependency,
    private val projectByPath: ProjectByPath,
) : ProjectDependencyAccessor {

    override fun dependencyProject(): Project {
        return projectByPath(dependency.path)
    }

    internal class Factory : ProjectDependencyAccessor.Factory {
        override fun getInstance(
            dependency: ProjectDependency,
            projectByPath: ProjectByPath,
        ): ProjectDependencyAccessor {
            return DefaultProjectDependencyAccessor(dependency, projectByPath)
        }
    }
}

internal fun ProjectDependency.compatAccessor(project: Project) = project
    .variantImplementationFactory<ProjectDependencyAccessor.Factory>()
    .getInstance(this, project::project)

internal fun ProjectDependency.compatAccessor(
    variantFactory: Provider<ProjectDependencyAccessor.Factory>,
    projectByPath: ProjectByPath,
) = variantFactory
    .get()
    .getInstance(this, projectByPath)
