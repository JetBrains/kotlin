/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import org.gradle.api.Project

interface NpmApi {
    fun setup(project: Project)

    @Suppress("EXPOSED_PARAMETER_TYPE")
    fun resolveProject(npmPackage: NpmResolver.NpmPackage)

    @Suppress("EXPOSED_PARAMETER_TYPE")
    fun resolveRootProject(
        rootProject: Project,
        subprojects: MutableList<NpmResolver.NpmPackage>
    )

    @Suppress("EXPOSED_PARAMETER_TYPE")
    fun hookRootPackage(rootProject: Project, rootPackageJson: PackageJson, allWorkspaces: Collection<NpmResolver.NpmPackage>) {

    }

    open val hoistGradleNodeModules
        get() = false

    fun cleanProject(project: Project) {
        val npmProject = project.npmProject

        npmProject.nodeModulesDir.deleteRecursively()
        npmProject.packageJsonFile.delete()
    }

    companion object {
        fun resolveOperationDescription(packageManagerTitle: String): String =
            "Resolving NPM dependencies using $packageManagerTitle"
    }
}