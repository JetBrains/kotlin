/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.IgnoreEmptyDirectories
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import org.gradle.work.NormalizeLineEndings
import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.targets.js.npm.KotlinNpmResolutionManager
import org.jetbrains.kotlin.gradle.targets.js.npm.NodeJsEnvironmentTask
import org.jetbrains.kotlin.gradle.targets.js.npm.PackageJsonFilesTask
import org.jetbrains.kotlin.gradle.targets.js.npm.UsesKotlinNpmResolutionManager

@DisableCachingByDefault
abstract class KotlinNpmInstallTask :
    DefaultTask(),
    NodeJsEnvironmentTask,
    PackageJsonFilesTask,
    UsesKotlinNpmResolutionManager {
    init {
        check(project == project.rootProject)
    }

    @Input
    val args: MutableList<String> = mutableListOf()

    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    @get:IgnoreEmptyDirectories
    @get:NormalizeLineEndings
    @get:InputFiles
    abstract val preparedFiles: ConfigurableFileCollection

    @get:OutputFiles
    abstract val additionalFiles: ConfigurableFileCollection

    // node_modules as OutputDirectory is performance problematic
    // so input will only be existence of its directory
    @get:Internal
    abstract val nodeModules: DirectoryProperty

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
