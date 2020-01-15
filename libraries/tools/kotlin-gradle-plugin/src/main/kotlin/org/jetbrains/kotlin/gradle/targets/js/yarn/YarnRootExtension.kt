/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.yarn

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.internal.ConfigurationPhaseAware
import org.jetbrains.kotlin.gradle.logging.kotlinInfo
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.tasks.internal.CleanableStore

open class YarnRootExtension(val project: Project) : ConfigurationPhaseAware<YarnEnv>() {
    init {
        check(project == project.rootProject)
    }

    private val gradleHome = project.gradle.gradleUserHomeDir.also {
        project.logger.kotlinInfo("Storing cached files in $it")
    }

    var installationDir by Property(gradleHome.resolve("yarn"))

    var downloadBaseUrl by Property("https://github.com/yarnpkg/yarn/releases/download")
    var version by Property("1.21.1")

    val yarnSetupTask: YarnSetupTask
        get() = project.tasks.getByName(YarnSetupTask.NAME) as YarnSetupTask

    var disableWorkspaces: Boolean by Property(false)

    val useWorkspaces: Boolean
        get() = !disableWorkspaces

    override fun finalizeConfiguration(): YarnEnv {
        val cleanableStore = CleanableStore[installationDir.path]

        return YarnEnv(
            downloadUrl = "$downloadBaseUrl/v$version/yarn-v$version.tar.gz",
            cleanableStore = cleanableStore,
            home = cleanableStore["yarn-v$version"].use()
        )
    }

    internal fun executeSetup() {
        NodeJsRootPlugin.apply(project).executeSetup()

        if (!finalizeConfiguration().home.isDirectory) {
            yarnSetupTask.setup()
        }
    }

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