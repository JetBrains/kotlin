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
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlatform
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.js.npm.RequiresNpmDependencies
import org.jetbrains.kotlin.gradle.targets.js.npm.resolver.implementing
import org.jetbrains.kotlin.gradle.targets.js.npm.tasks.RootPackageJsonTask
import org.jetbrains.kotlin.gradle.tasks.internal.CleanableStore

open class YarnRootExtension(
    @Transient
    val project: Project
) : ConfigurationPhaseAware<YarnEnv>() {
    init {
        check(project == project.rootProject)
    }

    private val gradleHome = project.gradle.gradleUserHomeDir.also {
        project.logger.kotlinInfo("Storing cached files in $it")
    }

    var installationDir by Property(gradleHome.resolve("yarn"))

    var downloadBaseUrl by Property("https://github.com/yarnpkg/yarn/releases/download")
    var version by Property("1.22.17")

    var command by Property("yarn")

    var download by Property(true)

    val yarnSetupTaskProvider: TaskProvider<YarnSetupTask>
        get() = project.tasks
            .withType(YarnSetupTask::class.java)
            .named(YarnSetupTask.NAME)

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

    fun resolution(path: String, version: String) {
        resolution(path, Action {
            it.include(version)
        })
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

        val isWindows = NodeJsPlatform.name == NodeJsPlatform.WIN

        val home = cleanableStore["yarn-v$version"].use()

        fun getExecutable(command: String, customCommand: String, windowsExtension: String): String {
            val finalCommand = if (isWindows && customCommand == command) "$command.$windowsExtension" else customCommand
            return if (download)
                home
                    .resolve("bin/yarn.js").absolutePath
            else
                finalCommand
        }
        return YarnEnv(
            downloadUrl = downloadBaseUrl,
            cleanableStore = cleanableStore,
            home = home,
            executable = getExecutable("yarn", command, "cmd"),
            standalone = !download,
            ivyDependency = "com.yarnpkg:yarn:$version@tar.gz"
        )
    }

    internal fun executeSetup() {
        NodeJsRootPlugin.apply(project).executeSetup()

        if (!download) return

        val yarnSetupTask = yarnSetupTaskProvider.get()
        yarnSetupTask.actions.forEach {
            it.execute(yarnSetupTask)
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