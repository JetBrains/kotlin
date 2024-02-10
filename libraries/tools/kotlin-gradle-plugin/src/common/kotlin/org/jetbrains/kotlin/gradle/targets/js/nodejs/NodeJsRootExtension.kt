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

open class NodeJsRootExtension(
    val project: Project,
) : AbstractSettings<NodeJsEnv>() {

    init {
        check(project.rootProject == project)

        val projectProperties = PropertiesProvider(project)

        if (projectProperties.errorJsGenerateExternals != null) {
            project.logger.warn(
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

    val rootProjectDir
        get() = project.rootDir

    private val gradleHome = project.gradle.gradleUserHomeDir.also {
        project.logger.kotlinInfo("Storing cached files in $it")
    }

    override var installationDir by Property(gradleHome.resolve("nodejs"))

    override var download by Property(true)

    @Deprecated("Use downloadBaseUrl instead", ReplaceWith("downloadBaseUrl"))
    var nodeDownloadBaseUrl
        get() = downloadBaseUrl
        set(value) {
            downloadBaseUrl = value
        }

    override var downloadBaseUrl: String? by Property("https://nodejs.org/dist")

    @Deprecated("Use version instead", ReplaceWith("version"))
    var nodeVersion
        get() = version
        set(value) {
            version = value
        }

    // Release schedule: https://github.com/nodejs/Release
    // Actual LTS and Current versions: https://nodejs.org/en/download/
    // Older versions and more information, e.g. V8 version inside: https://nodejs.org/en/download/releases/
    override var version by Property("20.10.0")

    override var command by Property("node")

    @Deprecated("Use command instead", ReplaceWith("command"))
    var nodeCommand
        get() = command
        set(value) {
            command = value
        }

    val packageManagerExtension: org.gradle.api.provider.Property<NpmApiExt> = project.objects.property()

    val taskRequirements: TasksRequirements
        get() = resolver.tasksRequirements

    lateinit var resolver: KotlinRootNpmResolver

    val rootPackageDirectory: Provider<Directory> = project.layout.buildDirectory.dir("js")

    @Deprecated(
        "This property is deprecated and will be removed in future. Use rootPackageDirectory instead",
        replaceWith = ReplaceWith("rootPackageDirectory")
    )
    val rootPackageDir: File
        get() = rootPackageDirectory.getFile()

    val projectPackagesDirectory: Provider<Directory>
        get() = rootPackageDirectory.map { it.dir("packages") }

    @Deprecated(
        "This property is deprecated and will be removed in future. Use projectPackagesDirectory instead",
        replaceWith = ReplaceWith("projectPackagesDirectory")
    )
    val projectPackagesDir: File
        get() = projectPackagesDirectory.getFile()

    val nodeModulesGradleCacheDirectory: Provider<Directory>
        get() = rootPackageDirectory.map { it.dir("packages_imported") }

    @Deprecated(
        "This property is deprecated and will be removed in future. Use nodeModulesGradleCacheDirectory instead",
        replaceWith = ReplaceWith("nodeModulesGradleCacheDirectory")
    )
    val nodeModulesGradleCacheDir: File
        get() = nodeModulesGradleCacheDirectory.getFile()

    internal val platform: org.gradle.api.provider.Property<Platform> = project.objects.property<Platform>()

    val versions = NpmVersions()

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

        packageManagerExtension.disallowChanges()

        return NodeJsEnv(
            download = download,
            cleanableStore = cleanableStore,
            rootPackageDir = rootPackageDirectory.getFile(),
            dir = nodeDir,
            nodeBinDir = nodeBinDir,
            executable = getExecutable("node", command, "exe"),
            platformName = name,
            architectureName = architecture,
            ivyDependency = getIvyDependency(),
            downloadBaseUrl = downloadBaseUrl,
            packageManager = packageManagerExtension.get().packageManager
        )
    }

    val nodeJsSetupTaskProvider: TaskProvider<out NodeJsSetupTask>
        get() = project.tasks.withType(NodeJsSetupTask::class.java).named(NodeJsSetupTask.NAME)

    val npmInstallTaskProvider: TaskProvider<out KotlinNpmInstallTask>
        get() = project.tasks.withType(KotlinNpmInstallTask::class.java).named(KotlinNpmInstallTask.NAME)

    val rootPackageJsonTaskProvider: TaskProvider<RootPackageJsonTask>
        get() = project.tasks.withType(RootPackageJsonTask::class.java).named(RootPackageJsonTask.NAME)

    val packageJsonUmbrellaTaskProvider: TaskProvider<Task>
        get() = project.tasks.named(PACKAGE_JSON_UMBRELLA_TASK_NAME)

    val npmCachesSetupTaskProvider: TaskProvider<out KotlinNpmCachesSetup>
        get() = project.tasks.withType(KotlinNpmCachesSetup::class.java).named(KotlinNpmCachesSetup.NAME)

    @Deprecated("This is deprecated and will be removed. Use corresponding property from YarnRootExtension")
    val storeYarnLockTaskProvider: TaskProvider<YarnLockStoreTask>
        get() = project.yarn.storeYarnLockTaskProvider

    companion object {
        const val EXTENSION_NAME: String = "kotlinNodeJs"
    }
}
