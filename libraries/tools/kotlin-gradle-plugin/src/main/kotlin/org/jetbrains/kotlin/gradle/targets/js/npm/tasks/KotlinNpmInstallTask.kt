/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
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
    private val nodeJs = NodeJsRootPlugin.apply(project.rootProject)
    private val resolutionManager = nodeJs.npmResolutionManager

    @Input
    val args: MutableList<String> = mutableListOf()

    @Suppress("unused")
    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    @get:InputFiles
    val packageJsonFiles: Collection<File> by lazy {
        resolutionManager.packageJsonFiles
    }

    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    @get:InputFiles
    val preparedFiles: Collection<File> by lazy {
        nodeJs.packageManager.preparedFiles(nodeJs)
    }

    @get:OutputFile
    val yarnLock: File by lazy {
        nodeJs.rootPackageDir.resolve("yarn.lock")
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