/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.targets.js.npm.resolved.KotlinCompilationNpmResolution

/**
 * NodeJS package manager API
 */
interface NpmApi {
    fun setup(project: Project)

    fun resolveProject(resolvedNpmProject: KotlinCompilationNpmResolution)

    enum class Result {
        upToDate, executed
    }

    fun resolveRootProject(
        rootProject: Project,
        subProjects: Collection<KotlinCompilationNpmResolution>
    ): Result

    companion object {
        fun resolveOperationDescription(packageManagerTitle: String): String =
            "Resolving NPM dependencies using $packageManagerTitle"
    }
}