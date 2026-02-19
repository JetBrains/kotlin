/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.internal

import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency

internal class ProjectDependencyAccessorG88(
    private val dependency: ProjectDependency
) : ProjectDependencyAccessor {
    override fun dependencyProject(): Project = dependency.dependencyProject

    internal class Factory : ProjectDependencyAccessor.Factory {
        override fun getInstance(
            dependency: ProjectDependency,
            projectByPath: ProjectByPath,
        ): ProjectDependencyAccessor {
            return ProjectDependencyAccessorG88(dependency)
        }
    }
}
