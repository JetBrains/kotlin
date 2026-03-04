/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.*
import org.gradle.work.DisableCachingByDefault
import org.gradle.work.NormalizeLineEndings
import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.targets.js.npm.KotlinNpmResolutionManager
import org.jetbrains.kotlin.gradle.targets.js.npm.NodeJsEnvironmentTask
import org.jetbrains.kotlin.gradle.targets.js.npm.PackageJsonFilesTask
import org.jetbrains.kotlin.gradle.targets.js.npm.UsesKotlinNpmResolutionManager

/**
 * Prepares the root npm project, if necessary.
 *
 * This task is required by subprojects with JS and WasmJS Kotlin targets.
 * This task must run before subprojects run tasks that depend on the root npm project.
 *
 * The specific package manager used to resolve npm dependencies is configurable.
 * https://kotl.in/js-project-setup/npm-dependencies
 *
 * This task **must** only be registered once for a Gradle build,
 * to ensure the root npm project is only prepared once.
 * Therefore, it must be registered in the root project.
 */
// KT-80311: This will be refactored as part of npm dependencies project isolation support.
@DisableCachingByDefault
abstract class KotlinNpmInstallTask :
    DefaultTask(),
    NodeJsEnvironmentTask,
    PackageJsonFilesTask,
    UsesKotlinNpmResolutionManager {

    init {
        check(project == project.rootProject) {
            "KotlinNpmInstallTask must be registered in the root project. " +
                    "It is not allowed to register it in subproject ${project.path}."
        }
    }

    /**
     * Additional input arguments that are passed to the npm package manager.
     */
    @Input
    val args: MutableList<String> = mutableListOf()

    /**
     * Input files required by the npm package manager.
     *
     * @see org.jetbrains.kotlin.gradle.targets.js.npm.NpmApiExecution.preparedFiles
     */
    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    @get:IgnoreEmptyDirectories
    @get:NormalizeLineEndings
    @get:InputFiles
    abstract val preparedFiles: ConfigurableFileCollection

    /**
     * Output files produced by the npm package managers
     * after they have installed the npm dependencies into [nodeModules].
     *
     * For example, lock files.
     */
    @get:OutputFiles
    abstract val additionalFiles: ConfigurableFileCollection

    /**
     * The `node_modules/` directory where npm dependencies are resolved.
     *
     * Used as a custom up-to-date check:
     * if the directory exists, assume this task is up-to-date.
     */
    // node_modules as OutputDirectory is performance problematic
    // so input will only be existence of its directory
    @get:Internal
    abstract val nodeModules: DirectoryProperty

    /**
     * If Gradle is running in offline mode, pass the `--offline` flag to the npm package manager.
     */
    private val isOffline = project.gradle.startParameter.isOffline()

    @TaskAction
    fun resolve() {
        val args = buildList {
            addAll(args)
            if (isOffline) add("--offline")
        }

        npmResolutionManager.get()
            .installIfNeeded(
                args = args,
                services = services,
                logger = logger,
                nodeJsEnvironment.get(),
                packageManagerEnv.get(),
            ) ?: throw (npmResolutionManager.get().state as KotlinNpmResolutionManager.ResolutionState.Error).wrappedException
    }

    companion object {
        @Deprecated(
            "Use npmInstallTaskProvider from corresponding NodeJsRootExtension or WasmNodeJsRootExtension instead. " +
                    "Scheduled for removal in Kotlin 2.4.",
            level = DeprecationLevel.ERROR
        )
        const val NAME = "kotlinNpmInstall"

        @InternalKotlinGradlePluginApi
        const val BASE_NAME = "npmInstall"
    }
}
