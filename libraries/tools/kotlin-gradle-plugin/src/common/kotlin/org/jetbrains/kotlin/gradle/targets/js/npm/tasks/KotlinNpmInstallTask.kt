/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.work.DisableCachingByDefault
import org.gradle.work.NormalizeLineEndings
import org.jetbrains.kotlin.gradle.targets.js.nodejs.PackageManagerEnvironment
import org.jetbrains.kotlin.gradle.targets.js.npm.KotlinNpmResolutionManager
import org.jetbrains.kotlin.gradle.targets.js.npm.NodeJsEnvironment
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmApiExecution
import org.jetbrains.kotlin.gradle.targets.js.npm.UsesKotlinNpmResolutionManager
import org.jetbrains.kotlin.gradle.utils.getFile
import java.io.File

@DisableCachingByDefault
abstract class KotlinNpmInstallTask :
    DefaultTask(),
    UsesKotlinNpmResolutionManager {
    init {
        check(project == project.rootProject)
    }

    @get:Internal
    internal abstract val rootNodeJsEnvironment: Property<NodeJsEnvironment>

    @get:Internal
    internal abstract val rootPackageManagerEnvironment: Property<PackageManagerEnvironment>

    @get:Internal
    internal abstract val rootPackageManager: Property<NpmApiExecution<*>>

    @get:Internal
    internal abstract val rootPackagesDirectory: DirectoryProperty

    @get:Internal
    internal abstract val packageJsonFilesProperty: ListProperty<RegularFile>

    @get:Internal
    internal abstract val additionalInstallOutput: ConfigurableFileCollection

    @Input
    val args: MutableList<String> = mutableListOf()

    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    @get:IgnoreEmptyDirectories
    @get:NormalizeLineEndings
    @get:InputFiles
    val preparedFiles: Collection<File> by lazy {
        rootPackageManager
            .zip(rootNodeJsEnvironment) { manager, environment ->
                manager.preparedFiles(environment)
            }
            .get()
    }

    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:IgnoreEmptyDirectories
    @get:NormalizeLineEndings
    @get:InputFiles
    val packageJsonFiles: List<RegularFile> by lazy {
        packageJsonFilesProperty.get()
    }

    @get:OutputFiles
    val additionalFiles: FileCollection
        get() = additionalInstallOutput

    @Deprecated(
        "This property is deprecated and will be removed in future. Use additionalFiles instead",
        replaceWith = ReplaceWith("additionalFiles")
    )
    @get:Internal
    val yarnLockFile: Provider<RegularFile> = rootPackagesDirectory.map { it.file("yarn.lock") }

    @Suppress("DEPRECATION")
    @Deprecated(
        "This property is deprecated and will be removed in future. Use additionalFiles instead",
        replaceWith = ReplaceWith("additionalFiles")
    )
    @get:Internal
    val yarnLock: File
        get() = yarnLockFile.getFile()

    // node_modules as OutputDirectory is performance problematic
    // so input will only be existence of its directory
    @get:Internal
    val nodeModules: Provider<Directory> = rootPackagesDirectory.map { it.dir("node_modules") }

    @TaskAction
    fun resolve() {
        npmResolutionManager.get()
            .installIfNeeded(
                args = args,
                services = services,
                logger = logger,
                rootNodeJsEnvironment.get(),
                rootPackageManagerEnvironment.get(),
            ) ?: throw (npmResolutionManager.get().state as KotlinNpmResolutionManager.ResolutionState.Error).wrappedException
    }

    companion object {
        const val NAME = "kotlinNpmInstall"
    }
}