/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.nodejs

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.internal.ConfigurationPhaseAware
import org.jetbrains.kotlin.gradle.logging.kotlinInfo
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.targets.js.NpmVersions
import org.jetbrains.kotlin.gradle.targets.js.npm.KotlinNpmResolutionManager
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmApi
import org.jetbrains.kotlin.gradle.targets.js.npm.resolver.PACKAGE_JSON_UMBRELLA_TASK_NAME
import org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinNpmInstallTask
import org.jetbrains.kotlin.gradle.targets.js.npm.tasks.RootPackageJsonTask
import org.jetbrains.kotlin.gradle.targets.js.yarn.Yarn
import org.jetbrains.kotlin.gradle.tasks.internal.CleanableStore
import java.io.File

open class NodeJsRootExtension(val rootProject: Project) : ConfigurationPhaseAware<NodeJsEnv>() {
    init {
        check(rootProject.rootProject == rootProject)
    }

    private val gradleHome = rootProject.gradle.gradleUserHomeDir.also {
        rootProject.logger.kotlinInfo("Storing cached files in $it")
    }

    var installationDir by Property(gradleHome.resolve("nodejs"))

    var download by Property(true)

    var nodeDownloadBaseUrl by Property("https://nodejs.org/dist")
    var nodeVersion by Property("12.16.1")

    var nodeCommand by Property("node")

    var packageManager: NpmApi by Property(Yarn())

    private val projectProperties = PropertiesProvider(rootProject)

    inner class Experimental {
        val discoverTypes: Boolean
            get() = projectProperties.jsDiscoverTypes == true
    }

    val experimental = Experimental()

    val nodeJsSetupTask: NodeJsSetupTask
        get() = rootProject.tasks.getByName(NodeJsSetupTask.NAME) as NodeJsSetupTask

    val npmInstallTask: KotlinNpmInstallTask
        get() = rootProject.tasks.getByName(KotlinNpmInstallTask.NAME) as KotlinNpmInstallTask

    val packageJsonUmbrellaTaskProvider: TaskProvider<Task>
        get() = rootProject.tasks.named(PACKAGE_JSON_UMBRELLA_TASK_NAME)

    val rootPackageJsonTask: RootPackageJsonTask
        get() = rootProject.tasks.getByName(RootPackageJsonTask.NAME) as RootPackageJsonTask

    val rootPackageDir: File
        get() = rootProject.buildDir.resolve("js")

    internal val rootNodeModulesStateFile: File
        get() = rootPackageDir.resolve("node_modules.state")

    val projectPackagesDir: File
        get() = rootPackageDir.resolve("packages")

    val nodeModulesGradleCacheDir: File
        get() = rootPackageDir.resolve("packages_imported")

    override fun finalizeConfiguration(): NodeJsEnv {
        val platform = NodeJsPlatform.name
        val architecture = NodeJsPlatform.architecture

        val nodeDirName = "node-v$nodeVersion-$platform-$architecture"
        val cleanableStore = CleanableStore[installationDir.absolutePath]
        val nodeDir = cleanableStore[nodeDirName].use()
        val isWindows = NodeJsPlatform.name == NodeJsPlatform.WIN
        val nodeBinDir = if (isWindows) nodeDir else nodeDir.resolve("bin")

        fun getExecutable(command: String, customCommand: String, windowsExtension: String): String {
            val finalCommand = if (isWindows && customCommand == command) "$command.$windowsExtension" else customCommand
            return if (download) File(nodeBinDir, finalCommand).absolutePath else finalCommand
        }

        fun getIvyDependency(): String {
            val type = if (isWindows) "zip" else "tar.gz"
            return "org.nodejs:node:$nodeVersion:$platform-$architecture@$type"
        }

        return NodeJsEnv(
            cleanableStore = cleanableStore,
            nodeDir = nodeDir,
            nodeBinDir = nodeBinDir,
            nodeExecutable = getExecutable("node", nodeCommand, "exe"),
            platformName = platform,
            architectureName = architecture,
            ivyDependency = getIvyDependency()
        )
    }

    internal fun executeSetup() {
        val nodeJsEnv = requireConfigured()
        if (download) {
            if (!nodeJsEnv.nodeBinDir.isDirectory) {
                nodeJsSetupTask.exec()
            }
        }
    }

    val versions = NpmVersions()
    internal val npmResolutionManager = KotlinNpmResolutionManager(this)

    companion object {
        const val EXTENSION_NAME: String = "kotlinNodeJs"
    }
}