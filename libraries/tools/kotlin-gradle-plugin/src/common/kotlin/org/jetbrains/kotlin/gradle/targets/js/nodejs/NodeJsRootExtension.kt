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
import org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinNpmCachesSetup
import org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinNpmInstallTask
import org.jetbrains.kotlin.gradle.targets.js.npm.tasks.RootPackageJsonTask
import org.jetbrains.kotlin.gradle.targets.js.yarn.Yarn
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnLockCopyTask
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnLockCopyTask.Companion.RESTORE_YARN_LOCK_NAME
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnLockCopyTask.Companion.STORE_YARN_LOCK_NAME
import org.jetbrains.kotlin.gradle.tasks.internal.CleanableStore
import java.io.File
import java.io.Serializable

open class NodeJsRootExtension(@Transient val rootProject: Project) : ConfigurationPhaseAware<NodeJsEnv>(), Serializable {
    init {
        check(rootProject.rootProject == rootProject)
    }

    private val logger = rootProject.logger

    private val gradleHome = rootProject.gradle.gradleUserHomeDir.also {
        logger.kotlinInfo("Storing cached files in $it")
    }

    var installationDir by Property(gradleHome.resolve("nodejs"))

    var download by Property(true)

    var nodeDownloadBaseUrl by Property("https://nodejs.org/dist")

    // Release schedule: https://github.com/nodejs/Release
    // Actual LTS and Current versions: https://nodejs.org/en/download/
    // Older versions and more information, e.g. V8 version inside: https://nodejs.org/en/download/releases/
    var nodeVersion by Property("18.12.1")

    var nodeCommand by Property("node")

    var packageManager: NpmApi by Property(Yarn())

    @Transient
    private val projectProperties = PropertiesProvider(rootProject)

    private val errorGenerateExternals = run {
        if (projectProperties.errorJsGenerateExternals != null) {
            logger.warn(
                """
                |
                |==========
                |Please note, Dukat integration in Gradle plugin does not work now, it was removed.
                |We rethink how we can integrate properly.
                |==========
                |
                """.trimMargin()
            )
        }
    }

    val taskRequirements: TasksRequirements = TasksRequirements()

    val nodeJsSetupTaskProvider: TaskProvider<out NodeJsSetupTask>
        get() = rootProject.tasks.withType(NodeJsSetupTask::class.java).named(NodeJsSetupTask.NAME)

    @Suppress(
        "UNNECESSARY_SAFE_CALL",
        "SAFE_CALL_WILL_CHANGE_NULLABILITY"
    ) // TODO: investigate this warning; fixing it breaks integration tests.
    val npmInstallTaskProvider: TaskProvider<out KotlinNpmInstallTask>?
        get() = rootProject?.tasks?.withType(KotlinNpmInstallTask::class.java)?.named(KotlinNpmInstallTask.NAME)

    val packageJsonUmbrellaTaskProvider: TaskProvider<Task>
        get() = rootProject.tasks.named(PACKAGE_JSON_UMBRELLA_TASK_NAME)

    @Suppress(
        "UNNECESSARY_SAFE_CALL",
        "SAFE_CALL_WILL_CHANGE_NULLABILITY"
    ) // TODO: investigate this warning; fixing it breaks integration tests.
    val rootPackageJsonTaskProvider: TaskProvider<RootPackageJsonTask>?
        get() = rootProject?.tasks?.withType(RootPackageJsonTask::class.java)?.named(RootPackageJsonTask.NAME)

    @Suppress(
        "UNNECESSARY_SAFE_CALL",
        "SAFE_CALL_WILL_CHANGE_NULLABILITY"
    ) // TODO: investigate this warning; fixing it breaks integration tests.
    val npmCachesSetupTaskProvider: TaskProvider<out KotlinNpmCachesSetup>?
        get() = rootProject?.tasks?.withType(KotlinNpmCachesSetup::class.java)?.named(KotlinNpmCachesSetup.NAME)

    @Suppress(
        "UNNECESSARY_SAFE_CALL",
        "SAFE_CALL_WILL_CHANGE_NULLABILITY"
    ) // TODO: investigate this warning; fixing it breaks integration tests.
    val storeYarnLockTaskProvider: TaskProvider<out YarnLockCopyTask>?
        get() = rootProject?.tasks?.withType(YarnLockCopyTask::class.java)?.named(STORE_YARN_LOCK_NAME)

    @Suppress(
        "UNNECESSARY_SAFE_CALL",
        "SAFE_CALL_WILL_CHANGE_NULLABILITY"
    ) // TODO: investigate this warning; fixing it breaks integration tests.
    val restoreYarnLockTaskProvider: TaskProvider<out YarnLockCopyTask>?
        get() = rootProject?.tasks?.withType(YarnLockCopyTask::class.java)?.named(RESTORE_YARN_LOCK_NAME)

    val rootPackageDir: File by lazy {
        rootProject.buildDir.resolve("js")
    }

    val projectPackagesDir: File
        get() = rootPackageDir.resolve("packages")

    val nodeModulesGradleCacheDir: File
        get() = rootPackageDir.resolve("packages_imported")

    override fun finalizeConfiguration(): NodeJsEnv {
        val platformHelper = PlatformHelper
        val platform = platformHelper.osName
        val architecture = platformHelper.osArch

        val nodeDirName = "node-v$nodeVersion-$platform-$architecture"
        val cleanableStore = CleanableStore[installationDir.absolutePath]
        val nodeDir = cleanableStore[nodeDirName].use()
        val isWindows = platformHelper.isWindows
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
            ivyDependency = getIvyDependency(),
            downloadBaseUrl = nodeDownloadBaseUrl
        )
    }

    internal fun executeSetup() {
        if (download) {
            val nodeJsSetupTask = nodeJsSetupTaskProvider.get()
            nodeJsSetupTask.actions.forEach {
                it.execute(nodeJsSetupTask)
            }
        }
    }

    val versions = NpmVersions()

    val npmResolutionManager = KotlinNpmResolutionManager(this)

    companion object {
        const val EXTENSION_NAME: String = "kotlinNodeJs"
    }
}
