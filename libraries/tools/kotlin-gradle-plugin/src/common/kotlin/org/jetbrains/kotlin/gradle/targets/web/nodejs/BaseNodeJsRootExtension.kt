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

/**
 * Abstract base class for configuring the Node.js environment and related tasks in a Kotlin/JS project.
 */
abstract class BaseNodeJsRootExtension internal constructor(
    val project: Project,
    rootDir: String,
) : HasPlatformDisambiguator {

    init {
        check(project.rootProject == project)

        val projectProperties = PropertiesProvider.Companion(project)

        if (projectProperties.errorJsGenerateExternals != null) {
            project.logger.warn(
                """
                |The Dukat integration in Gradle plugin has been removed.
                |Remove the `kotlin.js.generate.externals` Gradle property.
                """.trimMargin()
            )
        }
    }

    val rootProjectDir
        get() = project.rootDir

    val packageManagerExtension: Property<NpmApiExtension<*, *>> = project.objects.property()

    val taskRequirements: TasksRequirements
        get() = resolver.tasksRequirements

    lateinit var resolver: KotlinRootNpmResolver

    /**
     * Root directory for the root npm project.
     * Nested inside the Gradle project's root `build/` directory.
     * Contains subdirectories of npm workspaces for each Kotlin compilation.
     */
    val rootPackageDirectory: Provider<Directory> = project.layout.buildDirectory.dir(rootDir)

    val projectPackagesDirectory: Provider<Directory>
        get() = rootPackageDirectory.map { it.dir("packages") }

    val nodeModulesGradleCacheDirectory: Provider<Directory>
        get() = rootPackageDirectory.map { it.dir("packages_imported") }

    val versions = NpmVersions()

    val npmInstallTaskProvider: TaskProvider<out KotlinNpmInstallTask>
        get() = project.tasks.withType(KotlinNpmInstallTask::class.java)
            .named(extensionName(KotlinNpmInstallTask.BASE_NAME))

    val rootPackageJsonTaskProvider: TaskProvider<RootPackageJsonTask>
        get() = project.tasks.withType(RootPackageJsonTask::class.java)
            .named(
                extensionName(
                    RootPackageJsonTask.NAME,
                    prefix = null,
                )
            )

    val packageJsonUmbrellaTaskProvider: TaskProvider<Task>
        get() = project.tasks.named(extensionName(PACKAGE_JSON_UMBRELLA_TASK_NAME))

    val npmCachesSetupTaskProvider: TaskProvider<out KotlinNpmCachesSetup>
        get() = project.tasks.withType(KotlinNpmCachesSetup::class.java)
            .named(extensionName(KotlinNpmCachesSetup.NAME))
}
