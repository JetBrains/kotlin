/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.yarn

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.logging.kotlinInfo
import org.jetbrains.kotlin.gradle.targets.js.AbstractSettings
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsEnv
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NpmApiExtension
import org.jetbrains.kotlin.gradle.targets.js.nodejs.Platform
import org.jetbrains.kotlin.gradle.targets.js.npm.LockCopyTask
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin.Companion.RESTORE_YARN_LOCK_NAME
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin.Companion.STORE_YARN_LOCK_NAME
import org.jetbrains.kotlin.gradle.tasks.internal.CleanableStore
import java.io.File

open class YarnRootExtension(
    val project: Project,
    val nodeJsRoot: NodeJsRootExtension,
) : AbstractSettings<YarnEnv>(), NpmApiExtension<YarnEnvironment, Yarn> {
    init {
        check(project == project.rootProject)
    }

    private val gradleHome = project.gradle.gradleUserHomeDir.also {
        project.logger.kotlinInfo("Storing cached files in $it")
    }

    override val packageManager: Yarn by lazy {
        Yarn()
    }

    override val environment: YarnEnvironment by lazy {
        requireConfigured().asYarnEnvironment
    }

    override val additionalInstallOutput: FileCollection = project.objects.fileCollection().from(
        nodeJsRoot.rootPackageDirectory.map { it.file(LockCopyTask.YARN_LOCK) }
    )

    override val preInstallTasks: ListProperty<TaskProvider<*>> = project.objects.listProperty(TaskProvider::class.java)

    override val postInstallTasks: ListProperty<TaskProvider<*>> = project.objects.listProperty(TaskProvider::class.java)

    override var installationDir by Property(gradleHome.resolve("yarn"))

    override var downloadBaseUrl: String? by Property("https://github.com/yarnpkg/yarn/releases/download")
    override var version by Property("1.22.17")

    override var command by Property("yarn")

    override var download by Property(true)
    var lockFileName by Property(LockCopyTask.YARN_LOCK)
    var lockFileDirectory: File by Property(project.rootDir.resolve(LockCopyTask.KOTLIN_JS_STORE))

    var ignoreScripts by Property(true)

    var yarnLockMismatchReport: YarnLockMismatchReport by Property(YarnLockMismatchReport.FAIL)

    var reportNewYarnLock: Boolean by Property(false)

    var yarnLockAutoReplace: Boolean by Property(false)

    val yarnSetupTaskProvider: TaskProvider<YarnSetupTask>
        get() = project.tasks
            .withType(YarnSetupTask::class.java)
            .named(YarnSetupTask.NAME)

    internal val platform: org.gradle.api.provider.Property<Platform> = project.objects.property(Platform::class.java)

    var resolutions: MutableList<YarnResolution> by Property(mutableListOf())

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

    internal val nodeJsEnvironment: org.gradle.api.provider.Property<NodeJsEnv> = project.objects.property(NodeJsEnv::class.java)

    override fun finalizeConfiguration(): YarnEnv {
        val cleanableStore = CleanableStore[installationDir.path]

        val isWindows = platform.get().isWindows()

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
            download = download,
            downloadBaseUrl = downloadBaseUrl,
            cleanableStore = cleanableStore,
            dir = home,
            executable = getExecutable("yarn", command, "cmd"),
            ivyDependency = "com.yarnpkg:yarn:$version@tar.gz",
            ignoreScripts = ignoreScripts,
            yarnLockMismatchReport = yarnLockMismatchReport,
            reportNewYarnLock = reportNewYarnLock,
            yarnLockAutoReplace = yarnLockAutoReplace,
            yarnResolutions = resolutions
        )
    }

    val restoreYarnLockTaskProvider: TaskProvider<YarnLockCopyTask>
        get() = project.tasks.withType(YarnLockCopyTask::class.java).named(RESTORE_YARN_LOCK_NAME)

    val storeYarnLockTaskProvider: TaskProvider<YarnLockStoreTask>
        get() = project.tasks.withType(YarnLockStoreTask::class.java).named(STORE_YARN_LOCK_NAME)

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