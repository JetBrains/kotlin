/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.yarn

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.logging.kotlinInfo

open class YarnExtension(project: Project) {
    private val gradleHome = project.gradle.gradleUserHomeDir.also {
        project.logger.kotlinInfo("Storing cached files in $it")
    }

    var installationDir = gradleHome.resolve("yarn")

    var distBaseUrl = "https://github.com/yarnpkg/yarn/releases/download"
    var version = "1.15.2"

    var command = "yarn"

    var download = true

    internal fun buildEnv() = YarnEnv(
        downloadUrl = "$distBaseUrl/v$version/yarn-v$version.tar.gz",
        home = installationDir.resolve("yarn-v$version")
    )

    companion object {
        const val YARN: String = "yarn"

        operator fun get(project: Project): YarnExtension {
            val extension = project.extensions.findByType(YarnExtension::class.java)
            if (extension != null)
                return extension

            val parentProject = project.parent
            if (parentProject != null)
                return get(parentProject)

            throw GradleException("YarnExtension is not installed")
        }
    }
}
