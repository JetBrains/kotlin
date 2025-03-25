/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.targets.js.NpmPackageVersion
import org.jetbrains.kotlin.gradle.targets.js.npm.NodeJsEnvironmentTask
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmProject
import org.jetbrains.kotlin.gradle.targets.js.npm.PackageJson
import org.jetbrains.kotlin.gradle.utils.getFile
import java.nio.channels.FileChannel
import java.nio.file.StandardOpenOption


/**
 * A task designed to install Kotlin tooling dependencies through Node.js.
 *
 * This task ensures the required npm dependencies for Kotlin tooling are installed
 * in the specified output directory. If the required dependencies are already available,
 * the installation process is skipped.
 *
 * The task generates a `package.json` file with the necessary dependencies and then triggers
 * the Node.js packaging manager to perform the installation.
 *
 */
@DisableCachingByDefault
abstract class KotlinToolingInstallTask
internal constructor() :
    DefaultTask(),
    NodeJsEnvironmentTask {

    @get:Input
    internal abstract val versionsHash: Property<String>

    @get:Nested
    internal abstract val tools: ListProperty<NpmPackageVersion>

    @get:OutputDirectory
    abstract val destination: DirectoryProperty

    @get:Input
    abstract val args: ListProperty<String>

    // node_modules as OutputDirectory is performance problematic
    // so input will only be existence of its directory
    @get:Internal
    abstract val nodeModules: DirectoryProperty

    @TaskAction
    fun install() {
        val destinationDir = destination.getFile()
        val lockFile = destinationDir.resolve("lock")
        FileChannel.open(
            lockFile.toPath(),
            StandardOpenOption.CREATE, StandardOpenOption.WRITE
        ).use { channel ->
            channel.lock().use { _ ->
                val packageJsonFile = destinationDir.resolve(NpmProject.PACKAGE_JSON)
                if (nodeModules.getFile().exists()) return // return from install
                val toolingPackageJson = PackageJson(
                    KOTLIN_NPM_TOOLING_NAME,
                    versionsHash.get()
                ).apply {
                    private = true
                    dependencies.putAll(
                        tools.get().map { it.name to it.version }
                    )
                }

                toolingPackageJson.saveTo(packageJsonFile)

                nodeJsEnvironment.get().packageManager.prepareTooling(destinationDir)

                nodeJsEnvironment.get().packageManager.packageManagerExec(
                    logger = logger,
                    nodeJs = nodeJsEnvironment.get(),
                    environment = packageManagerEnv.get(),
                    dir = destination.locationOnly.map { it.asFile },
                    description = "Installation of tooling install",
                    args = args.get(),
                )
            }
        }
    }

    companion object {
        const val NAME = "kotlinToolingInstall"

        const val KOTLIN_NPM_TOOLING_NAME = "kotlin-npm-tooling"
    }
}