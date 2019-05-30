/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import org.gradle.api.Project

/**
 * NodeJS package manager API
 */
interface NpmApi {
    fun setup(project: Project)

    fun resolveProject(resolvedNpmProject: NpmProjectPackage)

    /**
     * @return false if everything is already up to date
     */
    fun resolveRootProject(
        rootProject: Project,
        npmProjects: MutableList<NpmProjectPackage>
    ): Boolean

    companion object {
        fun resolveOperationDescription(packageManagerTitle: String): String =
            "Resolving NPM dependencies using $packageManagerTitle"
    }
}