/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.Directory
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.work.DisableCachingByDefault
import org.gradle.work.NormalizeLineEndings
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin.Companion.kotlinNodeJsExtension
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmProject
import org.jetbrains.kotlin.gradle.targets.js.npm.UsesKotlinNpmResolutionManager
import org.jetbrains.kotlin.gradle.targets.js.npm.asNodeJsEnvironment
import org.jetbrains.kotlin.gradle.targets.js.npm.resolver.KotlinRootNpmResolver
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

    private val nodeJs
        get() = project.rootProject.kotlinNodeJsExtension

    private val rootResolver: KotlinRootNpmResolver
        get() = nodeJs.resolver

    private val packagesDir: Provider<Directory>
        get() = nodeJs.projectPackagesDirectory

    // -----

    private val nodeJsEnvironment by lazy {
        nodeJs.requireConfigured().asNodeJsEnvironment
    }

    private val packageManagerEnv by lazy {
        nodeJs.packageManagerExtension.get().environment
    }

    @get:OutputFile
    val rootPackageJsonFile: Provider<RegularFile> =
        nodeJs.rootPackageDirectory.map { it.file(NpmProject.PACKAGE_JSON) }


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
        rootResolver.projectResolvers.values
            .flatMap { it.compilationResolvers }
            .map { it.compilationNpmResolution }
            .map { resolution ->
                val name = resolution.npmProjectName
                packagesDir.map { it.dir(name).file(NpmProject.PACKAGE_JSON) }.get()
            }
    }

    @TaskAction
    fun resolve() {
        npmResolutionManager.get().prepare(logger, nodeJsEnvironment, packageManagerEnv)
    }

    companion object {
        const val NAME = "rootPackageJson"
    }
}