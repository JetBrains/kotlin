/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.work.DisableCachingByDefault
import org.gradle.work.NormalizeLineEndings
import org.jetbrains.kotlin.gradle.targets.js.nodejs.PackageManagerEnvironment
import org.jetbrains.kotlin.gradle.targets.js.npm.NodeJsEnvironment
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmProject
import org.jetbrains.kotlin.gradle.targets.js.npm.UsesKotlinNpmResolutionManager
import org.jetbrains.kotlin.gradle.targets.js.npm.resolver.KotlinCompilationNpmResolution
import org.jetbrains.kotlin.gradle.utils.getFile
import java.io.File

@DisableCachingByDefault
abstract class RootPackageJsonTask :
    DefaultTask(),
    UsesKotlinNpmResolutionManager {
    init {
        check(project == project.rootProject)
    }

    // Only in configuration phase
    // Not part of configuration caching

    @get:Internal
    internal abstract val rootPackageDirectory: DirectoryProperty

    @get:Internal
    internal abstract val projectPackagesDirectory: DirectoryProperty

    @get:Internal
    internal abstract val rootPackageManagerEnvironment: Property<PackageManagerEnvironment>

    @get:Internal
    internal abstract val rootNodeJsEnvironment: Property<NodeJsEnvironment>

    @get:Internal
    internal abstract val compilationsNpmResolution: ListProperty<KotlinCompilationNpmResolution>

    @get:OutputFile
    val rootPackageJsonFile: Provider<RegularFile> = rootPackageDirectory
        .map { it.file(NpmProject.PACKAGE_JSON) }

    @Deprecated(
        "This property is deprecated and will be removed in future. Use rootPackageJsonFile instead",
        replaceWith = ReplaceWith("rootPackageJsonFile")
    )
    @get:Internal
    val rootPackageJson: File
        get() = rootPackageJsonFile.getFile()

    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:IgnoreEmptyDirectories
    @get:NormalizeLineEndings
    @get:InputFiles
    val packageJsonFiles: List<RegularFile> by lazy {
        projectPackagesDirectory
            .zip(compilationsNpmResolution) { packagesDir, compilationsNpmResolution ->
                compilationsNpmResolution
                    .map { resolution ->
                        val name = resolution.npmProjectName
                        packagesDir.dir(name).file(NpmProject.PACKAGE_JSON)
                    }
            }
            .get()
    }

    @TaskAction
    fun resolve() {
        npmResolutionManager.get().prepare(
            logger,
            rootNodeJsEnvironment.get(),
            rootPackageManagerEnvironment.get()
        )
    }

    companion object {
        const val NAME = "rootPackageJson"
    }
}