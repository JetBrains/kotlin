/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.yarn

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.internal.execWithProgress
import org.jetbrains.kotlin.gradle.internal.operation
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlugin
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmApi
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmProjectLayout
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmResolver
import org.jetbrains.kotlin.gradle.targets.js.npm.PackageJson
import org.slf4j.LoggerFactory

object Yarn : NpmApi {
    private val log = LoggerFactory.getLogger("org.jetbrains.kotlin.gradle.targets.js.yarn.Yarn")

    override fun setup(project: Project) {
        YarnPlugin[project].executeSetup()
    }

    fun yarnExec(
        project: Project,
        description: String,
        vararg args: String,
        npmProjectLayout: NpmProjectLayout = NpmProjectLayout[project]
    ) {
        val nodeJsEnv = NodeJsPlugin[project].buildEnv()
        val yarnEnv = YarnPlugin[project].buildEnv()

        project.execWithProgress(description) { exec ->
            exec.executable = nodeJsEnv.nodeExec
            exec.args = listOf(yarnEnv.home.resolve("bin/yarn.js").absolutePath) + args
            exec.workingDir = npmProjectLayout.nodeWorkDir
        }
    }

    @Suppress("EXPOSED_PARAMETER_TYPE")
    override fun resolveProject(npmPackage: NpmResolver.NpmPackage) {
        val project = npmPackage.project
        if (!YarnPlugin[project].checkUseWorkspace()) {
            yarnExec(project, NpmApi.resolveOperationDescription("yarn for ${project.path}"))
        }
    }

    @Suppress("EXPOSED_PARAMETER_TYPE")
    override fun hookRootPackage(rootProject: Project, rootPackageJson: PackageJson, allWorkspaces: Collection<NpmResolver.NpmPackage>) {
        if (YarnPlugin[rootProject].checkUseWorkspace()) {
            rootPackageJson.private = true
            rootPackageJson.workspaces = allWorkspaces
                .filter { it.project != rootProject }
                .map { it.project.rootDir.relativeTo(rootProject.rootDir).path }
        }
    }

    @Suppress("EXPOSED_PARAMETER_TYPE")
    override fun resolveRootProject(
        rootProject: Project,
        subprojects: MutableList<NpmResolver.NpmPackage>
    ) {
        check(rootProject == rootProject.rootProject)

        if (YarnPlugin[rootProject].checkUseWorkspace()) {
            yarnExec(rootProject, NpmApi.resolveOperationDescription("yarn"))
        } else {
            if (subprojects.any { it.project != rootProject }) {
                // todo: proofread message
                rootProject.logger.warn(
                    "Build contains sub projects with NPM dependencies. " +
                            "It is recommended to enable yarn workspaces to store common NPM dependencies in root project. " +
                            "To enable it add this to your root project: \n" +
                            "nodeJs { manageNodeModules = true } \n" +
                            "yarn { useWorkspaces = true } \n" +
                            "Note: with `manageNodeModules` enabled, your `node_modules` and `package.json` files will be managed by " +
                            "Gradle, will be overridden during build and should be ignored in VCS."
                )
            }
        }
    }
}