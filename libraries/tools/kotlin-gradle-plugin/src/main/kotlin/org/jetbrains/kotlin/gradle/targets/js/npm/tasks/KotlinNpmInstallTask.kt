/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.js.npm.asNpmEnvironment
import org.jetbrains.kotlin.gradle.utils.unavailableValueError
import java.io.File

open class KotlinNpmInstallTask : DefaultTask() {
    init {
        check(project == project.rootProject)

        onlyIf {
            preparedFiles.all {
                it.exists()
            }
        }
    }

    @Transient
    private val nodeJs: NodeJsRootExtension? = NodeJsRootPlugin.apply(project.rootProject)
    private val resolutionManager = (nodeJs ?: unavailableValueError("nodeJs")).npmResolutionManager

    @Input
    val args: MutableList<String> = mutableListOf()

    @get:Internal
    val nodeModulesDir: File by lazy {
        (nodeJs ?: unavailableValueError("nodeJs"))
            .rootPackageDir
            .resolve("node_modules")
    }

    init {
        outputs.upToDateWhen {
            nodeModulesDir.isDirectory
        }
    }

    @Suppress("unused")
    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    @get:IgnoreEmptyDirectories
    @get:InputFiles
    val packageJsonFiles: Collection<File> by lazy {
        resolutionManager.packageJsonFiles
    }

    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    @get:IgnoreEmptyDirectories
    @get:InputFiles
    val preparedFiles: Collection<File> by lazy {
        (nodeJs ?: unavailableValueError("nodeJs")).packageManager.preparedFiles(nodeJs.asNpmEnvironment)
    }

    @get:OutputFile
    val yarnLock: File by lazy {
        (nodeJs ?: unavailableValueError("nodeJs")).rootPackageDir.resolve("yarn.lock")
    }

    @TaskAction
    fun resolve() {
        resolutionManager.installIfNeeded(
            args = args,
            services = services,
            logger = logger
        )
    }

    companion object {
        const val NAME = "kotlinNpmInstall"
    }
}