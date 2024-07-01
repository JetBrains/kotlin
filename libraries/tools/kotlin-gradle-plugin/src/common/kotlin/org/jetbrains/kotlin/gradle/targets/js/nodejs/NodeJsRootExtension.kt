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
    private val nodeJs: NodeJsExtension
) {

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

    @Deprecated("Use installationDir from NodeJsPlugin (not NodeJsRootPlugin) instead")
    var installationDir: File by nodeJs::installationDir

    @Deprecated("Use download from NodeJsPlugin (not NodeJsRootPlugin) instead")
    var download by nodeJs::download

    @Deprecated("Use downloadBaseUrl from NodeJsPlugin (not NodeJsRootPlugin) instead")
    var nodeDownloadBaseUrl by nodeJs::downloadBaseUrl

    var downloadBaseUrl: String? by nodeJs::downloadBaseUrl

    @Deprecated("Use version from NodeJsPlugin (not NodeJsRootPlugin) instead")
    var nodeVersion by nodeJs::version

    var version by nodeJs::version

    @Deprecated("Use command from NodeJsPlugin (not NodeJsRootPlugin) instead")
    var command by nodeJs::command

    @Deprecated("Use command from NodeJsPlugin (not NodeJsRootPlugin) instead")
    var nodeCommand by nodeJs::command

    val rootProjectDir
        get() = project.rootDir

    val packageManagerExtension: org.gradle.api.provider.Property<NpmApiExt> = project.objects.property()

    val taskRequirements: TasksRequirements
        get() = resolver.tasksRequirements

    lateinit var resolver: KotlinRootNpmResolver

    val rootPackageDirectory: Provider<Directory> = project.layout.buildDirectory.dir("js")

    val projectPackagesDirectory: Provider<Directory>
        get() = rootPackageDirectory.map { it.dir("packages") }

    val nodeModulesGradleCacheDirectory: Provider<Directory>
        get() = rootPackageDirectory.map { it.dir("packages_imported") }

    val versions = NpmVersions()

    val npmInstallTaskProvider: TaskProvider<out KotlinNpmInstallTask>
        get() = project.tasks.withType(KotlinNpmInstallTask::class.java).named(KotlinNpmInstallTask.NAME)

    val rootPackageJsonTaskProvider: TaskProvider<RootPackageJsonTask>
        get() = project.tasks.withType(RootPackageJsonTask::class.java).named(RootPackageJsonTask.NAME)

    val packageJsonUmbrellaTaskProvider: TaskProvider<Task>
        get() = project.tasks.named(PACKAGE_JSON_UMBRELLA_TASK_NAME)

    val npmCachesSetupTaskProvider: TaskProvider<out KotlinNpmCachesSetup>
        get() = project.tasks.withType(KotlinNpmCachesSetup::class.java).named(KotlinNpmCachesSetup.NAME)

    @Deprecated("Use nodeJsSetupTaskProvider from NodeJsPlugin (not NodeJsRootPlugin) instead")
    val nodeJsSetupTaskProvider: TaskProvider<out NodeJsSetupTask> by nodeJs::nodeJsSetupTaskProvider

    @Deprecated("Use NodeJsExtension instead")
    fun requireConfigured(): NodeJsEnv {
        return nodeJs.requireConfigured()
    }

    companion object {
        const val EXTENSION_NAME: String = "kotlinNodeJsRoot"
    }
}
