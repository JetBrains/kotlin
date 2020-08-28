/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.yarn

import org.gradle.api.Action
import org.gradle.api.Incubating
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.internal.ConfigurationPhaseAware
import org.jetbrains.kotlin.gradle.logging.kotlinInfo
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.js.npm.RequiresNpmDependencies
import org.jetbrains.kotlin.gradle.targets.js.npm.resolver.implementing
import org.jetbrains.kotlin.gradle.targets.js.npm.tasks.RootPackageJsonTask
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
    var version by Property("1.22.4")

    val yarnSetupTask: YarnSetupTask
        get() = project.tasks.getByName(YarnSetupTask.NAME) as YarnSetupTask

    var disableWorkspaces: Boolean by Property(false)

    val useWorkspaces: Boolean
        get() = !disableWorkspaces

    val rootPackageJsonTaskProvider: TaskProvider<RootPackageJsonTask>
        get() = project.tasks
            .withType(RootPackageJsonTask::class.java)
            .named(RootPackageJsonTask.NAME)

    var resolutions: MutableList<YarnResolution> = mutableListOf()

    fun resolution(path: String, configure: Action<YarnResolution>) {
        resolutions.add(
            YarnResolution(path)
                .apply { configure.execute(this) }
        )
    }

    @Incubating
    fun disableGranularWorkspaces() {
        val packageJsonUmbrella = NodeJsRootPlugin.apply(project)
            .packageJsonUmbrellaTaskProvider

        rootPackageJsonTaskProvider.configure {
            it.dependsOn(packageJsonUmbrella)
        }

        project.allprojects
            .forEach {
                it.tasks.implementing(RequiresNpmDependencies::class).all {}
            }
    }

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