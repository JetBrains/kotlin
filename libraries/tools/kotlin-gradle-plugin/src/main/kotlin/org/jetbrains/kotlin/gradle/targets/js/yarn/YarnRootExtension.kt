/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.yarn

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.logging.kotlinInfo
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlugin

open class YarnRootExtension(val project: Project) {
    private val gradleHome = project.gradle.gradleUserHomeDir.also {
        project.logger.kotlinInfo("Storing cached files in $it")
    }

    var installationDir = gradleHome.resolve("yarn")

    var downloadBaseUrl = "https://github.com/yarnpkg/yarn/releases/download"
    var version = "1.15.2"

    val yarnSetupTask: YarnSetupTask
        get() = project.tasks.getByName(YarnSetupTask.NAME) as YarnSetupTask

    var useWorkspaces: Boolean = false

    internal fun checkUseWorkspace(): Boolean = if (useWorkspaces) {
        check(NodeJsPlugin[project].manageNodeModules) {
            "Yarn workspace can be used only with `manageNodeModules`: (add `nodeJs { manageNodeModules = true }` to your root project)"
        }

        true
    } else false

    internal fun executeSetup() {
        NodeJsPlugin[project].executeSetup()

        val env = buildEnv()
        if (!env.home.isDirectory) {
            yarnSetupTask.setup()
        }
    }

    internal fun buildEnv() = YarnEnv(
        downloadUrl = "$downloadBaseUrl/v$version/yarn-v$version.tar.gz",
        home = installationDir.resolve("yarn-v$version")
    )

    companion object {
        const val YARN: String = "yarn"

        operator fun get(project: Project): YarnRootExtension {
            val extension = project.extensions.findByType(YarnRootExtension::class.java)
            if (extension != null)
                return extension

            val parentProject = project.parent
            if (parentProject != null)
                return get(parentProject)

            throw GradleException("YarnRootExtension is not installed")
        }
    }
}
