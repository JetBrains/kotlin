/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.yarn

import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.logging.kotlinInfo
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.tasks.locateTask
import org.jetbrains.kotlin.gradle.utils.deprecatedBecauseNoConfigAvoidanceUseProvider
import org.jetbrains.kotlin.gradle.utils.warnAccessToDeprecatedNoConfigAvoidanceSymbol

open class YarnRootExtension(val project: Project) {
    init {
        check(project == project.rootProject)
    }

    private val gradleHome = project.gradle.gradleUserHomeDir.also {
        project.logger.kotlinInfo("Storing cached files in $it")
    }

    var installationDir = gradleHome.resolve("yarn")

    var downloadBaseUrl = "https://github.com/yarnpkg/yarn/releases/download"
    var version = "1.15.2"

    val yarnSetupTaskProvider: TaskProvider<YarnSetupTask>
        get() = project.locateTask(YarnSetupTask.NAME)!!

    @Deprecated(deprecatedBecauseNoConfigAvoidanceUseProvider, ReplaceWith("yarnSetupTaskProvider"))
    val yarnSetupTask: YarnSetupTask
        get() = yarnSetupTaskProvider.get().also {
            project.logger.warnAccessToDeprecatedNoConfigAvoidanceSymbol("yarnSetupTask")
        }

    var disableWorkspaces: Boolean = false

    val useWorkspaces: Boolean
        get() = !disableWorkspaces

    internal fun executeSetup() {
        NodeJsRootPlugin.apply(project).executeSetup()

        val env = environment
        if (!env.home.isDirectory) {
            yarnSetupTaskProvider.get().setup()
        }
    }

    internal val environment
        get() = YarnEnv(
            downloadUrl = "$downloadBaseUrl/v$version/yarn-v$version.tar.gz",
            home = installationDir.resolve("yarn-v$version")
        )

    companion object {
        const val YARN: String = "kotlinYarn"

        operator fun get(project: Project): YarnRootExtension {
            val rootProject = project.rootProject
            rootProject.plugins.apply(YarnPlugin::class.java)
            return rootProject.extensions.getByName(YARN) as YarnRootExtension
        }
    }
}

val Project.yarn: YarnRootExtension
    get() = YarnRootExtension[this]