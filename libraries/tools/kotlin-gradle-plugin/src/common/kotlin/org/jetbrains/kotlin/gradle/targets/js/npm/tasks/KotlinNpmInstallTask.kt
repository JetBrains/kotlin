/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.work.DisableCachingByDefault
import org.gradle.work.NormalizeLineEndings
import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.targets.js.npm.KotlinNpmResolutionManager
import org.jetbrains.kotlin.gradle.targets.js.npm.NodeJsEnvironmentTask
import org.jetbrains.kotlin.gradle.targets.js.npm.PackageJsonFilesTask
import org.jetbrains.kotlin.gradle.targets.js.npm.UsesKotlinNpmResolutionManager
import org.jetbrains.kotlin.gradle.utils.property

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

    @get:Internal // tracks below
    internal val withForce: Property<Boolean> = project.objects.property<Boolean>().convention(false)

    init {
        // When --force flag is set, task must re-run; when not set, use previous state
        outputs.upToDateWhen { !withForce.get() }
        outputs.doNotCacheIf("--force flag set, task must re-run") { withForce.get() }
    }

    @TaskAction
    fun resolve() {
        val args = buildList {
            addAll(args)
            if (isOffline) add("--offline")
            if (withForce.get()) add("--force")
        }

        npmResolutionManager.get()
            .installIfNeeded(
                args = args,
                logger = logger,
                nodeJsEnvironment.get(),
                packageManagerEnv.get(),
            ) ?: throw (npmResolutionManager.get().state as KotlinNpmResolutionManager.ResolutionState.Error).wrappedException
    }

    companion object {
        @InternalKotlinGradlePluginApi
        const val BASE_NAME = "npmInstall"
    }
}
