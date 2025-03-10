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
import org.jetbrains.kotlin.gradle.targets.js.NpmVersions
import org.jetbrains.kotlin.gradle.targets.js.nodejs.*
import org.jetbrains.kotlin.gradle.targets.js.npm.resolver.KotlinRootNpmResolver
import org.jetbrains.kotlin.gradle.targets.js.npm.resolver.PACKAGE_JSON_UMBRELLA_TASK_NAME
import org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinNpmCachesSetup
import org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinNpmInstallTask
import org.jetbrains.kotlin.gradle.targets.js.npm.tasks.RootPackageJsonTask
import org.jetbrains.kotlin.gradle.targets.web.HasPlatformDisambiguator
import org.jetbrains.kotlin.gradle.utils.property
import java.io.File

/**
 * Abstract base class for configuring the Node.js environment and related tasks in a Kotlin/JS project.
 */
abstract class BaseNodeJsRootExtension internal constructor(
    val project: Project,
    private val nodeJs: () -> NodeJsEnvSpec,
    rootDir: String,
) : HasPlatformDisambiguator {

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
        "You can find this extension after applying NodeJsPlugin. Scheduled for removal in Kotlin 2.3.",
        level = DeprecationLevel.ERROR,
    )
    var installationDir: File = gradleHome.resolve("nodejs")

    @Deprecated(
        "You can find this extension after applying NodeJsPlugin. Scheduled for removal in Kotlin 2.3.",
        level = DeprecationLevel.ERROR,
    )
    var download = true

    @Suppress("DEPRECATION_ERROR")
    @Deprecated(
        "You can find this extension after applying NodeJsPlugin. Scheduled for removal in Kotlin 2.3.",
        level = DeprecationLevel.ERROR,
    )
    var nodeDownloadBaseUrl by ::downloadBaseUrl

    @Deprecated(
        "You can find this extension after applying NodeJsPlugin. Scheduled for removal in Kotlin 2.3.",
        level = DeprecationLevel.ERROR,
    )
    var downloadBaseUrl: String? = "https://nodejs.org/dist"

    @Suppress("DEPRECATION_ERROR")
    @Deprecated(
        "You can find this extension after applying NodeJsPlugin. Scheduled for removal in Kotlin 2.3.",
        level = DeprecationLevel.ERROR,
    )
    var nodeVersion by ::version

    @Deprecated(
        "You can find this extension after applying NodeJsPlugin. Scheduled for removal in Kotlin 2.3.",
        level = DeprecationLevel.ERROR,
    )
    var version = "22.13.0"

    @Deprecated(
        "You can find this extension after applying NodeJsPlugin. Scheduled for removal in Kotlin 2.3.",
        level = DeprecationLevel.ERROR,
    )
    var command = "node"

    @Suppress("DEPRECATION_ERROR")
    @Deprecated(
        "You can find this extension after applying NodeJsPlugin. Scheduled for removal in Kotlin 2.3.",
        level = DeprecationLevel.ERROR,
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
            .named(extensionName(KotlinNpmInstallTask.NAME))

    val rootPackageJsonTaskProvider: TaskProvider<RootPackageJsonTask>
        get() = project.tasks.withType(RootPackageJsonTask::class.java)
            .named(extensionName(RootPackageJsonTask.NAME))

    val packageJsonUmbrellaTaskProvider: TaskProvider<Task>
        get() = project.tasks.named(extensionName(PACKAGE_JSON_UMBRELLA_TASK_NAME))

    val npmCachesSetupTaskProvider: TaskProvider<out KotlinNpmCachesSetup>
        get() = project.tasks.withType(KotlinNpmCachesSetup::class.java)
            .named(extensionName(KotlinNpmCachesSetup.NAME))

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
