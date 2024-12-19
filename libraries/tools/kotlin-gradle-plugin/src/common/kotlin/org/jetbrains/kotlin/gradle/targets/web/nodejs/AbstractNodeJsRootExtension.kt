/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.web.nodejs

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.Directory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.logging.kotlinInfo
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.targets.web.HasPlatformDisambiguate
import org.jetbrains.kotlin.gradle.targets.js.NpmVersions
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsEnv
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsSetupTask
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NpmApiExt
import org.jetbrains.kotlin.gradle.targets.js.nodejs.TasksRequirements
import org.jetbrains.kotlin.gradle.targets.js.npm.resolver.KotlinRootNpmResolver
import org.jetbrains.kotlin.gradle.targets.js.npm.resolver.PACKAGE_JSON_UMBRELLA_TASK_NAME
import org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinNpmCachesSetup
import org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinNpmInstallTask
import org.jetbrains.kotlin.gradle.targets.js.npm.tasks.RootPackageJsonTask
import org.jetbrains.kotlin.gradle.utils.property
import java.io.File

abstract class AbstractNodeJsRootExtension(
    val project: Project,
    private val nodeJs: () -> NodeJsEnvSpec,
    rootDir: String,
) : HasPlatformDisambiguate {

    init {
        check(project.rootProject == project)

        val projectProperties = PropertiesProvider.Companion(project)

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

    private val gradleHome = project.gradle.gradleUserHomeDir.also {
        project.logger.kotlinInfo("Storing cached files in $it")
    }

    @Deprecated(
        "Use installationDir from NodeJsEnvSpec (not NodeJsRootExtension) instead. " +
                "You can find this extension after applying NodeJsPlugin. This will be removed in 2.2"
    )
    var installationDir: File = gradleHome.resolve("nodejs")

    @Deprecated(
        "Use download from NodeJsEnvSpec (not NodeJsRootExtension) instead. " +
                "You can find this extension after applying NodeJsPlugin. This will be removed in 2.2"
    )
    var download = true

    @Suppress("DEPRECATION")
    @Deprecated(
        "Use downloadBaseUrl from NodeJsEnvSpec (not NodeJsRootExtension) instead. " +
                "You can find this extension after applying NodeJsPlugin. This will be removed in 2.2"
    )
    var nodeDownloadBaseUrl by ::downloadBaseUrl

    @Deprecated(
        "Use downloadBaseUrl from NodeJsEnvSpec (not NodeJsRootExtension) instead. " +
                "You can find this extension after applying NodeJsPlugin. This will be removed in 2.2"
    )
    var downloadBaseUrl: String? = "https://nodejs.org/dist"

    @Suppress("DEPRECATION")
    @Deprecated(
        "Use version from NodeJsEnvSpec (not NodeJsRootExtension) instead. " +
                "You can find this extension after applying NodeJsPlugin. This will be removed in 2.2"
    )
    var nodeVersion by ::version

    @Deprecated(
        "Use downloadBaseUrl from NodeJsEnvSpec (not NodeJsRootExtension) instead. " +
                "You can find this extension after applying NodeJsPlugin. This will be removed in 2.2"
    )
    var version = "22.0.0"

    @Deprecated(
        "Use command from NodeJsEnvSpec (not NodeJsRootExtension) instead. " +
                "You can find this extension after applying NodeJsPlugin. This will be removed in 2.2"
    )
    var command = "node"

    @Suppress("DEPRECATION")
    @Deprecated(
        "Use command from NodeJsEnvSpec (not NodeJsRootExtension) instead. " +
                "You can find this extension after applying NodeJsPlugin. This will be removed in 2.2"
    )
    var nodeCommand by ::command

    val rootProjectDir
        get() = project.rootDir

    val packageManagerExtension: Property<NpmApiExt> = project.objects.property()

    val taskRequirements: TasksRequirements
        get() = resolver.tasksRequirements

    lateinit var resolver: KotlinRootNpmResolver

    val rootPackageDirectory: Provider<Directory> = project.layout.buildDirectory.dir(rootDir)

    val projectPackagesDirectory: Provider<Directory>
        get() = rootPackageDirectory.map { it.dir("packages") }

    val nodeModulesGradleCacheDirectory: Provider<Directory>
        get() = rootPackageDirectory.map { it.dir("packages_imported") }

    val versions = NpmVersions()

    val npmInstallTaskProvider: TaskProvider<out KotlinNpmInstallTask>
        get() = project.tasks.withType(KotlinNpmInstallTask::class.java)
            .named(extensionName(KotlinNpmInstallTask.Companion.NAME))

    val rootPackageJsonTaskProvider: TaskProvider<RootPackageJsonTask>
        get() = project.tasks.withType(RootPackageJsonTask::class.java)
            .named(extensionName(RootPackageJsonTask.Companion.NAME))

    val packageJsonUmbrellaTaskProvider: TaskProvider<Task>
        get() = project.tasks.named(extensionName(PACKAGE_JSON_UMBRELLA_TASK_NAME))

    val npmCachesSetupTaskProvider: TaskProvider<out KotlinNpmCachesSetup>
        get() = project.tasks.withType(KotlinNpmCachesSetup::class.java)
            .named(extensionName(KotlinNpmCachesSetup.Companion.NAME))

    @Deprecated(
        "Use nodeJsSetupTaskProvider from NodeJsEnvSpec (not NodeJsRootExtension) instead. " +
                "You can find this extension after applying NodeJsPlugin"
    )
    val nodeJsSetupTaskProvider: TaskProvider<out NodeJsSetupTask>
        get() = with(nodeJs()) {
            project.nodeJsSetupTaskProvider
        }

    @Deprecated("Use NodeJsEnvSpec instead. This will be removed in 2.2")
    fun requireConfigured(): NodeJsEnv {
        return nodeJs().env.get()
    }
}