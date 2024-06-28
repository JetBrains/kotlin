/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.nodejs

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.logging.kotlinInfo
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.targets.js.AbstractSettings
import org.jetbrains.kotlin.gradle.targets.js.NpmVersions
import org.jetbrains.kotlin.gradle.targets.js.npm.resolver.KotlinRootNpmResolver
import org.jetbrains.kotlin.gradle.targets.js.npm.resolver.PACKAGE_JSON_UMBRELLA_TASK_NAME
import org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinNpmCachesSetup
import org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinNpmInstallTask
import org.jetbrains.kotlin.gradle.targets.js.npm.tasks.RootPackageJsonTask
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnLockStoreTask
import org.jetbrains.kotlin.gradle.targets.js.yarn.yarn
import org.jetbrains.kotlin.gradle.tasks.internal.CleanableStore
import org.jetbrains.kotlin.gradle.utils.getFile
import org.jetbrains.kotlin.gradle.utils.property
import java.io.File

open class NodeJsExtension(
    val project: Project,
) : AbstractSettings<NodeJsEnv>() {

    private val gradleHome = project.gradle.gradleUserHomeDir.also {
        project.logger.kotlinInfo("Storing cached files in $it")
    }

    override var installationDir by Property(gradleHome.resolve("nodejs"))

    override var download by Property(true)

    override var downloadBaseUrl: String? by Property("https://nodejs.org/dist")

    @Deprecated("Use downloadBaseUrl instead", ReplaceWith("downloadBaseUrl"))
    var nodeDownloadBaseUrl by ::downloadBaseUrl

    // Release schedule: https://github.com/nodejs/Release
    // Actual LTS and Current versions: https://nodejs.org/en/download/
    // Older versions and more information, e.g. V8 version inside: https://nodejs.org/en/download/releases/
    override var version by Property("22.0.0")

    @Deprecated("Use version instead", ReplaceWith("version"))
    var nodeVersion by ::version

    override var command by Property("node")

    @Deprecated("Use command instead", ReplaceWith("command"))
    var nodeCommand by ::command

    internal val platform: org.gradle.api.provider.Property<Platform> = project.objects.property<Platform>()

    override fun finalizeConfiguration(): NodeJsEnv {
        val name = platform.get().name
        val architecture = platform.get().arch

        val nodeDirName = "node-v$version-$name-$architecture"
        val cleanableStore = CleanableStore[installationDir.absolutePath]
        val nodeDir = cleanableStore[nodeDirName].use()
        val isWindows = platform.get().isWindows()
        val nodeBinDir = if (isWindows) nodeDir else nodeDir.resolve("bin")

        fun getExecutable(command: String, customCommand: String, windowsExtension: String): String {
            val finalCommand = if (isWindows && customCommand == command) "$command.$windowsExtension" else customCommand
            return if (download) File(nodeBinDir, finalCommand).absolutePath else finalCommand
        }

        fun getIvyDependency(): String {
            val type = if (isWindows) "zip" else "tar.gz"
            return "org.nodejs:node:$version:$name-$architecture@$type"
        }

        return NodeJsEnv(
            download = download,
            cleanableStore = cleanableStore,
            dir = nodeDir,
            nodeBinDir = nodeBinDir,
            executable = getExecutable("node", command, "exe"),
            platformName = name,
            architectureName = architecture,
            ivyDependency = getIvyDependency(),
            downloadBaseUrl = downloadBaseUrl,
        )
    }

    val nodeJsSetupTaskProvider: TaskProvider<out NodeJsSetupTask>
        get() = project.tasks.withType(NodeJsSetupTask::class.java).named(NodeJsSetupTask.NAME)

    companion object {
        const val EXTENSION_NAME: String = "kotlinNodeJs"
    }
}
