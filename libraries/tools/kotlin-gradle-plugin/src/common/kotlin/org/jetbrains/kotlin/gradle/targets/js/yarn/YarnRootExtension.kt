/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.yarn

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.logging.kotlinInfo
import org.jetbrains.kotlin.gradle.targets.js.AbstractSettings
import org.jetbrains.kotlin.gradle.targets.js.nodejs.*
import org.jetbrains.kotlin.gradle.targets.js.nodejs.Platform
import org.jetbrains.kotlin.gradle.targets.js.npm.LockCopyTask
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin.Companion.RESTORE_YARN_LOCK_NAME
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin.Companion.STORE_YARN_LOCK_NAME
import org.jetbrains.kotlin.gradle.utils.property
import java.io.File

open class YarnRootExtension(
    val project: Project,
    val nodeJsRoot: NodeJsRootExtension,
    private val yarnSpec: YarnRootEnvSpec,
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

    override val installationDirectory: DirectoryProperty = project.objects.directoryProperty()
        .fileValue(gradleHome.resolve("yarn"))

    // value not convention because this property can be nullable to not add repository
    override val downloadBaseUrlProperty: org.gradle.api.provider.Property<String> = project.objects.property<String>()
        .value("https://github.com/yarnpkg/yarn/releases/download")

    override val versionProperty: org.gradle.api.provider.Property<String> = project.objects.property<String>()
        .convention("1.22.17")

    override val commandProperty: org.gradle.api.provider.Property<String> = project.objects.property<String>()
        .convention("yarn")

    override val downloadProperty: org.gradle.api.provider.Property<Boolean> = project.objects.property<Boolean>()
        .convention(true)

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
        return yarnSpec.env.get()
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